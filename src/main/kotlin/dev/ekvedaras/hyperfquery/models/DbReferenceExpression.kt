package dev.ekvedaras.hyperfquery.models

import com.intellij.database.model.DasColumn
import com.intellij.database.model.DasForeignKey
import com.intellij.database.model.DasIndex
import com.intellij.database.model.DasNamespace
import com.intellij.database.model.DasTable
import com.intellij.database.model.DasTableKey
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiTreeChangeAdapter
import com.intellij.psi.PsiTreeChangeEvent
import dev.ekvedaras.hyperfquery.utils.DatabaseUtils.Companion.dbDataSources
import dev.ekvedaras.hyperfquery.utils.DatabaseUtils.Companion.schemas
import dev.ekvedaras.hyperfquery.utils.DatabasesConfig.Companion.databaseConnections
import dev.ekvedaras.hyperfquery.utils.DatabasesConnection
import dev.ekvedaras.hyperfquery.utils.DbReferenceResolver
import dev.ekvedaras.hyperfquery.utils.PsiUtils.Companion.unquoteAndCleanup
import dev.ekvedaras.hyperfquery.utils.TableAndAliasCollector

class DbReferenceExpression(val expression: PsiElement, val type: Type) {
    companion object {
        enum class Type {
            Table,
            Column,
            Index,
            Key,
            ForeignKey,
        }
        
        private val LOG = Logger.getInstance(DbReferenceExpression::class.java)
        private const val TIMEOUT_SECONDS = 5L
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

    val parts = mutableListOf<String>()
    val ranges = mutableListOf<TextRange>()

    init {
        parts.addAll(
            expression.text.unquoteAndCleanup()
                .substringBefore("->") // strip out json fields
                .split(".")
                .map { it.substringBefore(" as").substringBefore(" AS").trim() }
        )

        for (part in parts) {
            ranges.add(TextRange.from(if (ranges.isNotEmpty()) ranges.last().endOffset + 1 else 1, part.length))
        }

        if (!DumbService.isDumb(project)) {
            val expressionDisposable = Disposer.newDisposable()
            PsiManager.getInstance(project).addPsiTreeChangeListener(object : PsiTreeChangeAdapter() {
                override fun childrenChanged(event: PsiTreeChangeEvent) {
                    expressionDisposable.dispose()
                }
            }, expressionDisposable)
            
            try {
                ReadAction.nonBlocking<Unit> {
                    try {
                        TableAndAliasCollector(this).collect()
                        DbReferenceResolver(this).resolve()
                    } catch (_: ProcessCanceledException) {
                        // Process canceled, skip resolution
                    }
                }
                .inSmartMode(project)
                .expireWith(expressionDisposable)
                .executeSynchronously()
            } catch (e: IllegalStateException) {
                // Handle inSmartMode constraint failure (issue #120)
                if (e.message?.contains("inSmartMode") == true) {
                    LOG.debug("Cannot satisfy inSmartMode constraint, skipping DB reference resolution")
                } else {
                    LOG.warn("Unexpected error during DB reference resolution", e)
                }
            } catch (_: ProcessCanceledException) {
                // Process was canceled, skip resolution
                LOG.debug("Process canceled during DB reference resolution")
            }
        }
    }
}
