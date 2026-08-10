package dev.ekvedaras.hyperfquery.reference

import com.intellij.lang.documentation.AbstractDocumentationProvider
import com.intellij.lang.documentation.DocumentationMarkup
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.jetbrains.php.lang.psi.elements.MethodReference
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression
import dev.ekvedaras.hyperfquery.models.DbReferenceExpression
import dev.ekvedaras.hyperfquery.utils.DatabaseUtils.Companion.nameWithoutPrefix
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
 * 再在 {@link #generateDoc} 里复用列引用解析链（{@link DbReferenceExpression}）取出
 * {@link com.intellij.database.model.DasColumn} 渲染类型/默认值/注释等信息。
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

        val reference = DbReferenceExpression(literal, DbReferenceExpression.Companion.Type.Column)
        val column = reference.column.firstOrNull() ?: return null
        val table = column.table?.nameWithoutPrefix(literal.project)
        val dataType = column.dataType?.toString() ?: "-"
        val enumValues = column.dataType?.enumValues?.takeIf { it.isNotEmpty() }

        val sb = StringBuilder()
        sb.append(DocumentationMarkup.DEFINITION_START)
        sb.append(escape(column.name))
        sb.append("<font color='#707070'>&nbsp;&nbsp;").append(escape(dataType)).append("</font>")
        if (!table.isNullOrEmpty()) {
            sb.append("<font color='#707070'>&nbsp;&nbsp;").append(escape(table)).append("</font>")
        }
        sb.append(DocumentationMarkup.DEFINITION_END)
        sb.append(DocumentationMarkup.CONTENT_START)
        val rows = mutableListOf<String>()
        rows += "类型: $dataType"
        if (column.default != null) {
            rows += "默认值: ${column.default}"
        }
        rows += if (column.isNotNull) "非空: 是" else "可空: 是"
        if (enumValues != null) {
            rows += "枚举: ${enumValues.joinToString(", ")}"
        }
        if (!column.comment.isNullOrEmpty()) {
            rows += "注释: ${column.comment}"
        }
        sb.append(rows.joinToString("<br>"))
        sb.append(DocumentationMarkup.CONTENT_END)
        return sb.toString()
    }

    /**
     * 判断字符串是否为查询构造器方法中应该提示的列名参数
     * （复用补全/检查的同一套判定逻辑，保证行为一致）。
     */
    private fun isColumnString(literal: StringLiteralExpression): Boolean {
        if (literal.containsVariable()) {
            return false
        }
        val method = MethodUtils.resolveMethodReference(literal) as? MethodReference ?: return false
        val project = method.project

        return method.isInteresting(project) &&
            method.isBuilderMethodForColumns() &&
            literal.isColumnIn(method, allowArray = method.name?.startsWith("where") == true) &&
            !method.isDbFacadeSqlBindingMethod(project)
    }

    private fun escape(s: String): String =
        s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
}
