package dev.ekvedaras.hyperfquery.reference

import com.intellij.database.psi.DbElement
import com.intellij.database.psi.documentation.DbDocumentationProvider
import com.intellij.lang.documentation.AbstractDocumentationProvider
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression
import dev.ekvedaras.hyperfquery.utils.HyperfUtils.Companion.isBuilderMethodForColumns
import dev.ekvedaras.hyperfquery.utils.HyperfUtils.Companion.isColumnIn
import dev.ekvedaras.hyperfquery.utils.HyperfUtils.Companion.isDbFacadeSqlBindingMethod
import dev.ekvedaras.hyperfquery.utils.HyperfUtils.Companion.isInteresting
import dev.ekvedaras.hyperfquery.utils.MethodUtils
import dev.ekvedaras.hyperfquery.utils.PsiUtils.Companion.containsVariable

/**
 * 悬停/快速文档：把查询构造器方法中列名参数解析到数据库列，并渲染其元信息。
 *
 * <p>字符串字面量本身不是可解析引用，默认实现拿不到悬停目标，必须通过
 * {@link #getCustomDocumentationElement} 把字符串元素直接返回给文档框架，
 * 再在 {@link #generateDoc} 里复用 {@link ColumnPsiReference}（即 Ctrl+Click 的
 * 引用解析链）解析出 {@link DbElement}，再交给 DataGrip 自带的
 * {@link DbDocumentationProvider} 生成原生文档。
 */
class ColumnDocumentationProvider : AbstractDocumentationProvider() {

    override fun getCustomDocumentationElement(
        editor: Editor,
        file: PsiFile,
        contextElement: PsiElement?,
        targetOffset: Int
    ): PsiElement? {
        if (contextElement == null) {
            return null
        }
        val literal = contextElement.parent as? StringLiteralExpression ?: return null
        return literal.takeIf { isColumnString(literal) }
    }

    override fun generateDoc(element: PsiElement, originalElement: PsiElement?): String? {
        val literal = element as? StringLiteralExpression
            ?: originalElement?.parent as? StringLiteralExpression
            ?: return null
        if (!isColumnString(literal)) {
            return null
        }

        // 复用 Ctrl+Click 的列引用解析，再交给 DataGrip 原生 provider 渲染。
        // 不直接调用 DbElement.getDocumentation(boolean)：该方法在 262 平台已被删除。
        val resolved = ColumnPsiReference(literal).resolve()
        val dbElement = resolved as? DbElement ?: return null
        val provider = DbDocumentationProvider()
        return provider.generateHoverDoc(dbElement, literal)
            ?: provider.generateDoc(dbElement, literal)
    }

    /**
     * 判断字符串是否为查询构造器方法中应该提示的列名参数
     * （复用补全/检查的同一套判定逻辑，保证行为一致）。
     */
    private fun isColumnString(literal: StringLiteralExpression): Boolean {
        if (literal.containsVariable()) {
            return false
        }
        val method = MethodUtils.resolveMethodReference(literal) ?: return false
        val project = method.project

        return method.isInteresting(project) &&
            method.isBuilderMethodForColumns() &&
            literal.isColumnIn(method, allowArray = method.name?.startsWith("where") == true) &&
            !method.isDbFacadeSqlBindingMethod(project)
    }
}
