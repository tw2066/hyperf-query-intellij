package dev.ekvedaras.hyperfquery.reference

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceBase
import dev.ekvedaras.hyperfquery.utils.DatabasesConfig.Companion.databasesPhp
import dev.ekvedaras.hyperfquery.utils.DatabasesConfig.Companion.parseDatabaseConnections
import dev.ekvedaras.hyperfquery.utils.PsiUtils.Companion.unquoteAndCleanup

/**
 * 连接名字符串 -> databases.php 中对应数组键,支持 Ctrl+Click 跳转。
 */
class ConnectionPsiReference(element: PsiElement) : PsiReferenceBase<PsiElement>(element) {
    init {
        rangeInElement = TextRange.from(1, (element.textLength - 2).coerceAtLeast(0))
    }

    override fun resolve(): PsiElement? {
        val name = element.text.unquoteAndCleanup()
        if (name.isEmpty()) {
            return null
        }

        return element.project.databasesPhp()
            ?.parseDatabaseConnections()
            ?.keys
            ?.firstOrNull { it.contents == name }
    }
}
