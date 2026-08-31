package dev.ekvedaras.hyperfquery.models

import com.intellij.database.model.DasColumn
import com.intellij.database.model.DasForeignKey
import com.intellij.database.model.DasIndex
import com.intellij.database.model.DasNamespace
import com.intellij.database.model.DasTable
import com.intellij.database.model.DasTableKey
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import dev.ekvedaras.hyperfquery.services.HyperfQuerySettings
import dev.ekvedaras.hyperfquery.utils.DatabaseUtils.Companion.dbDataSources
import dev.ekvedaras.hyperfquery.utils.DatabaseUtils.Companion.schemas
import dev.ekvedaras.hyperfquery.utils.DatabasesConfig.Companion.databaseConnections
import dev.ekvedaras.hyperfquery.utils.DatabasesConnection
import dev.ekvedaras.hyperfquery.utils.DbReferenceResolver
import dev.ekvedaras.hyperfquery.utils.PsiUtils.Companion.unquoteAndCleanup
import dev.ekvedaras.hyperfquery.utils.TableAndAliasCollector
import java.util.Collections

class DbReferenceExpression(
    val expression: PsiElement,
    val type: Type,
    /** raw SQL 片段(selectRaw / Db::raw 等)中按 ',' 切出的段在 expression.text 中的范围;null 表示整串解析 */
    private val segment: TextRange? = null,
) {
    companion object {
        enum class Type {
            Table,
            Column,
            Index,
            Key,
            ForeignKey,
        }

        private val ResolutionCacheKey = Key<CachedValue<MutableMap<String, DbReferenceExpression>>>(
            "hyperfquery.db.reference.resolutions"
        )

        /**
         * 按 (字符串元素, 引用类型, raw 分段) 缓存解析结果,任意 PSI 变更后整体失效。
         * 同一字符串的 inspection / reference / completion 共享一次解析,避免重复扫描。
         * 数据库模型变更不会触发失效(回退到下一次 PSI 变更时刷新),与既有行为一致可接受。
         */
        @JvmStatic
        fun create(expression: PsiElement, type: Type, segment: TextRange? = null): DbReferenceExpression {
            // dumb mode 下解析结果必然为空,直接返回不缓存,避免索引就绪后残留空结果
            if (DumbService.isDumb(expression.project)) {
                return DbReferenceExpression(expression, type, segment)
            }

            val resolutions = CachedValuesManager.getManager(expression.project).getCachedValue(
                expression,
                ResolutionCacheKey,
                {
                    CachedValueProvider.Result.create(
                        Collections.synchronizedMap(mutableMapOf<String, DbReferenceExpression>()),
                        PsiManager.getInstance(expression.project).modificationTracker,
                    )
                },
                false,
            )

            val key = "${type.name}:${segment?.startOffset ?: -1}:${segment?.endOffset ?: -1}"
            return synchronized(resolutions) {
                resolutions.getOrPut(key) { DbReferenceExpression(expression, type, segment) }
            }
        }
    }

    val project: Project = expression.project

    val tablesAndAliases = mutableMapOf<String, Pair<String, String?>>()
    val aliases = mutableMapOf<String, Pair<String, PsiElement>>()

    var schema = mutableListOf<DasNamespace>()
    var table = mutableListOf<DasTable>()
    var column = mutableListOf<DasColumn>()
    var index = mutableListOf<DasIndex>()
    var key = mutableListOf<DasTableKey>()
    var foreignKey = mutableListOf<DasForeignKey>()
    var alias: String? = null

    /** 链上 connection('name') 或模型 $connection 声明的连接名,由 TableAndAliasCollector 填充 */
    var connectionName: String? = null

    /** 当前生效的连接配置;无配置文件或连接配置缺失时为 null */
    private val selectedConnection: DatabasesConnection? by lazy(LazyThreadSafetyMode.NONE) {
        val connections = project.databaseConnections()
        when {
            // 显式连接: 配置中查不到 → null(全量回退, 不按 default 过滤)
            connectionName != null -> connections[connectionName]
            // 无显式连接: 优先 default 连接(Hyperf 运行时语义)
            else -> connections["default"]
        }
    }

    /**
     * 连接配置的 database(schema) 名;null 表示不过滤(回退旧逻辑)。
     * IDE 数据源中无同名 schema 时归一为 null。
     */
    val connectionSchema: String? by lazy(LazyThreadSafetyMode.NONE) {
        selectedConnection?.database?.takeIf { schema ->
            project.dbDataSources().anyMatch { dataSource ->
                dataSource.schemas().anyMatch { it.name == schema }
            }
        }
    }

    /**
     * 连接配置的表前缀;连接未配置 prefix 时为 null(回退全局 tablePrefix)。
     * 配置 'prefix' => '' 可显式覆盖全局前缀。
     */
    val connectionPrefix: String? by lazy(LazyThreadSafetyMode.NONE) {
        selectedConnection?.prefix
    }

    /** 生效的表前缀: 连接 prefix 优先, 否则全局 tablePrefix 设置 */
    val tablePrefix: String by lazy(LazyThreadSafetyMode.NONE) {
        connectionPrefix ?: HyperfQuerySettings.getInstance(project).tablePrefix
    }

    /** raw SQL 片段(selectRaw / Db::raw 等)中的逗号分段;raw 里的表名/别名是带前缀的真实 SQL 写法 */
    val isRawExpression: Boolean get() = segment != null

    val parts = mutableListOf<String>()
    val ranges = mutableListOf<TextRange>()

    init {
        val text = segment?.let { expression.text.substring(it.startOffset, it.endOffset) } ?: expression.text
        val baseOffset = segment?.startOffset ?: 1

        parts.addAll(
            text.unquoteAndCleanup()
                .substringBefore("->") // strip out json fields
                .split(".")
                .map { it.substringBefore(" as").substringBefore(" AS").trim() }
        )

        for (part in parts) {
            ranges.add(TextRange.from(if (ranges.isNotEmpty()) ranges.last().endOffset + 1 else baseOffset, part.length))
        }

        if (!DumbService.isDumb(project)) {
            // ProcessCanceledException 向上抛出: 取消时不缓存半成品结果,由平台重试
            ReadAction.compute<Unit, RuntimeException> {
                TableAndAliasCollector(this).collect()
                DbReferenceResolver(this).resolve()
            }
        }
    }
}
