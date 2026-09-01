package dev.ekvedaras.hyperfquery.utils

import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.php.lang.psi.elements.ArrayCreationExpression
import com.jetbrains.php.lang.psi.elements.ArrayHashElement
import com.jetbrains.php.lang.psi.elements.FunctionReference
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression
import com.jetbrains.php.lang.psi.elements.impl.PhpReturnImpl

private val DatabasesConfigPaths = listOf(
    "config/autoload/databases.php",
    "config/databases.php",
)

private val ConnectionsCacheKey = Key<CachedValue<Map<String, DatabasesConnection>>>(
    "hyperfquery.databases.connections"
)

/**
 * databases.php 中单个连接的配置。字段不可静态解析时为 null。
 */
data class DatabasesConnection(val database: String?, val prefix: String?)

class DatabasesConfig private constructor() {
    companion object {
        @Suppress("DEPRECATION")
        fun Project.databasesPhp(): PsiFile? {
            // 轻量测试夹具中 addFileToProject 写到内容根(/src),与 baseDir 不同,两者都要试
            val roots = ProjectRootManager.getInstance(this).contentRoots.toList() + listOfNotNull(this.baseDir)
            val virtualFile = roots.distinct().firstNotNullOfOrNull { root ->
                DatabasesConfigPaths.firstNotNullOfOrNull { root.findFileByRelativePath(it) }
            } ?: return null
            return PsiManager.getInstance(this).findFile(virtualFile)
        }

        /**
         * 连接名 -> 连接配置(database / prefix)。
         */
        fun Project.databaseConnections(): Map<String, DatabasesConnection> {
            val project = this
            return CachedValuesManager.getManager(project).getCachedValue(
                project,
                ConnectionsCacheKey,
                {
                    // 文件查找必须在 provider 内部进行:CV 失效后由同一 provider 重算
                    val file = project.databasesPhp()
                    val connections = file
                        ?.parseDatabaseConnections()
                        ?.mapKeys { (key, _) -> key.contents }
                        ?: emptyMap()
                    CachedValueProvider.Result.create(
                        connections,
                        PsiManager.getInstance(project).modificationTracker,
                    )
                },
                false,
            )
        }

        /**
         * 解析 return 数组：连接名 key 元素 -> 连接配置。
         * 不在此处缓存 PSI，调用方按需使用 key 元素（如引用跳转）。
         */
        fun PsiFile.parseDatabaseConnections(): Map<StringLiteralExpression, DatabasesConnection> {
            val config = PsiTreeUtil.findChildrenOfType(this, PhpReturnImpl::class.java)
                .firstOrNull()
                ?.argument as? ArrayCreationExpression
                ?: return emptyMap()

            return config.children
                .filterIsInstance<ArrayHashElement>()
                .mapNotNull { element ->
                    val key = element.key as? StringLiteralExpression ?: return@mapNotNull null
                    key to DatabasesConnection(
                        database = entryStringValue(element.value, "database"),
                        prefix = entryStringValue(element.value, "prefix"),
                    )
                }
                .toMap()
        }

        private fun entryStringValue(connectionConfig: PsiElement?, entryKey: String): String? {
            val entries = (connectionConfig as? ArrayCreationExpression)
                ?.children
                ?.filterIsInstance<ArrayHashElement>()
                ?: return null

            return when (
                val value = entries
                    .firstOrNull { (it.key as? StringLiteralExpression)?.contents == entryKey }
                    ?.value
            ) {
                is StringLiteralExpression -> value.contents
                // env('DB_DATABASE', 'hyperf') 取默认值
                is FunctionReference ->
                    if (value.name == "env") {
                        (value.getParameter(1) as? StringLiteralExpression)?.contents
                    } else {
                        null
                    }
                else -> null
            }
        }
    }
}
