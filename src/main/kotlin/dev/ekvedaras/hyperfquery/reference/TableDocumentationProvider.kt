package dev.ekvedaras.hyperfquery.reference

import com.intellij.database.psi.DbElement
import com.intellij.lang.documentation.AbstractDocumentationProvider
import com.intellij.lang.documentation.DocumentationMarkup
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression
import dev.ekvedaras.hyperfquery.models.DbReferenceExpression
import dev.ekvedaras.hyperfquery.utils.HyperfUtils.Companion.isBuilderMethodForTableByName
import dev.ekvedaras.hyperfquery.utils.HyperfUtils.Companion.isDbFacadeSqlBindingMethod
import dev.ekvedaras.hyperfquery.utils.HyperfUtils.Companion.isInteresting
import dev.ekvedaras.hyperfquery.utils.HyperfUtils.Companion.isInsideRegularFunction
import dev.ekvedaras.hyperfquery.utils.HyperfUtils.Companion.isTableParam
import dev.ekvedaras.hyperfquery.utils.MethodUtils
import dev.ekvedaras.hyperfquery.utils.PsiUtils.Companion.containsVariable

/**
 * 表名悬停/快速文档：轻量摘要(schema.表名 + 注释)。
 *
 * <p>不走 DataGrip 原生 DDL 渲染：宽表下生成整段建表语句(所有列/索引/外键)
 * 会明显卡顿。完整 DDL 可通过 Ctrl+Click 跳转到数据库工具窗口查看。
 */
class TableDocumentationProvider : AbstractDocumentationProvider() {

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
        return literal.takeIf { isTableString(literal) }
    }

    override fun generateDoc(element: PsiElement, originalElement: PsiElement?): String? {
        val literal = element as? StringLiteralExpression
            ?: originalElement?.parent as? StringLiteralExpression
            ?: return null
        if (!isTableString(literal)) {
            return null
        }

        val db = TableOrViewPsiReference(literal, DbReferenceExpression.Companion.Type.Table)
            .resolve() as? DbElement
            ?: return null

        return buildString {
            append(DocumentationMarkup.DEFINITION_START)
            (db.dasParent as? DbElement)?.name?.let { append(it).append('.') }
            append("<b>").append(db.name).append("</b>")
            append(DocumentationMarkup.DEFINITION_END)

            db.comment?.takeIf { it.isNotBlank() }?.let {
                append(DocumentationMarkup.CONTENT_START)
                append(it)
                append(DocumentationMarkup.CONTENT_END)
            }
        }
    }

    /**
     * 判断字符串是否为查询构造器方法中应该提示的表名参数
     * （与 TableOrViewReferenceProvider 的判定保持一致）。
     */
    private fun isTableString(literal: StringLiteralExpression): Boolean {
        if (literal.containsVariable()) {
            return false
        }
        val method = MethodUtils.resolveMethodReference(literal) ?: return false
        val project = method.project

        return method.isInteresting(project) &&
            method.isBuilderMethodForTableByName() &&
            literal.isTableParam() &&
            !method.isDbFacadeSqlBindingMethod(project) &&
            !literal.isInsideRegularFunction()
    }
}
