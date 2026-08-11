package dev.ekvedaras.hyperfquery.reference

import com.intellij.database.model.DasColumn
import com.intellij.database.model.DasDataSource
import com.intellij.database.model.DasNamespace
import com.intellij.database.model.DasTable
import com.intellij.database.psi.DbElement
import com.intellij.lang.documentation.AbstractDocumentationProvider
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.jetbrains.php.lang.psi.elements.MethodReference
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression
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
 * 再在 {@link #generateDoc} 里复用 {@link ColumnPsiReference}（即 Ctrl+Click 的
 * 引用解析链）解析出 {@link com.intellij.database.model.DasColumn}，用稳定的数据库模型
 * API 渲染与 DataGrip 原生一致的文档（数据源/架构/表/列 + DDL 片段）。
 *
 * <p>注意：不调用 {@link DbElement#getDocumentation} 等 DataGrip 内部方法——它的签名
 * 在不同平台版本间不稳定，会导致运行时 {@code NoSuchMethodError}。这里只用
 * {@code DasColumn}/{@code DasTable}/{@code DasNamespace}/{@code DasDataSource} 的
 * 稳定 getter。
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

        // 复用 Ctrl+Click 的列引用解析（ColumnPsiReference.resolve()），保证悬停与跳转解析一致
        val resolved = ColumnPsiReference(literal).resolve()
        val column = (resolved as? DbElement)?.delegate as? DasColumn ?: return null
        return render(column, literal.project)
    }

    private fun render(column: DasColumn, project: Project): String {
        val table = column.table
        val schema = table?.dasParent as? DasNamespace
        val dataSource = schema?.dasParent as? DasDataSource
        val tableName = table?.nameWithoutPrefix(project)
        val comment = column.comment
        val defaultValue = column.default

        val sb = StringBuilder()
        sb.append("<html><body>")
        sb.append("<b>Data Source:</b> ").append(escape(dataSource?.name ?: ""))
        sb.append("<br/><b>Schema:</b> ").append(escape(schema?.name ?: ""))
        sb.append("<br/><b>Table:</b> ").append(escape(tableName ?: ""))
        sb.append("<br/><b>Column:</b> ").append(escape(column.name))
        if (!comment.isNullOrEmpty()) {
            sb.append("<br/><br>").append(escape(comment))
        }
        sb.append("<br><br><code><pre>")
        sb.append("alter table ").append(escape(tableName ?: column.name))
        sb.append("<br>    add ").append(escape(column.name))
        sb.append(' ').append(escape(column.dataType?.toString() ?: ""))
        sb.append(if (column.isNotNull) " not null" else " null")
        if (defaultValue != null) {
            sb.append(" default ").append(escape(defaultValue))
        }
        if (!comment.isNullOrEmpty()) {
            sb.append(" comment '").append(escape(comment)).append('\'')
        }
        sb.append(';')
        sb.append("</pre></code>")
        sb.append("</body></html>")
        return sb.toString()
    }

    private fun escape(s: String): String =
        s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

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
}
