package dev.ekvedaras.hyperfquery.utils

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.search.ProjectScope
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.util.elementType
import com.intellij.psi.util.parentOfType
import com.intellij.util.Query
import com.jetbrains.php.lang.psi.elements.Parameter
import com.jetbrains.php.lang.psi.elements.Statement
import com.jetbrains.php.lang.psi.elements.Variable
import org.jetbrains.annotations.NotNull

@Suppress("MagicNumber")
object ElementTypes {
    val PhpArray = listOf(1386, 1889)
    const val ArrayValue = "Array value"
    const val ArrayKey = "Array key"
}

class PsiUtils private constructor() {
    companion object {
        fun PsiElement.containsVariable(): Boolean = this.textContains('$')
        fun CompletionParameters.containsVariable(): Boolean = this.position.containsVariable()
        fun String.containsAlias(): Boolean = this.contains(" as ", true)
        fun PsiElement.isPhpArray(): Boolean = ElementTypes.PhpArray.contains(this.typeAsInt())
        fun PsiElement.isArrayValue(): Boolean = this.elementType.toString() === ElementTypes.ArrayValue
        fun PsiElement.isArrayKey(): Boolean = this.elementType.toString() === ElementTypes.ArrayKey
        fun String.unquoteAndCleanup() = this.replace("IntellijIdeaRulezzz", "").trim('\'', '"').trim()

        private val SimpleColumnExpression = Regex("^[\\w$]+(\\.[\\w$]+){0,2}(\\s+[aA][sS]\\s+[\\w$]+)?$")
        private val CompletableColumnExpression = Regex("^[\\w$]*(\\.[\\w$]*){0,2}$")

        /**
         * raw SQL 片段(selectRaw / Db::raw 等)按 ',' 切段,返回其中符合简单列表达式
         * (col / t.col / schema.t.col / as 别名)的段在原文中的 TextRange(已 trim)。
         * count(*) 等复杂段被跳过,避免误判为列。
         */
        fun String.simpleColumnExpressionSegments(): List<TextRange> {
            val start = if (firstOrNull() == '\'' || firstOrNull() == '"') 1 else 0
            val end = if (length - 1 > start && (last() == '\'' || last() == '"')) length - 1 else length

            val segments = mutableListOf<TextRange>()
            var offset = start
            substring(start, end).split(",").forEach { segment ->
                val segmentStart = offset
                offset += segment.length + 1

                val trimmed = segment.trim()
                if (trimmed.matches(SimpleColumnExpression)) {
                    val leading = segment.indexOf(trimmed)
                    segments.add(TextRange(segmentStart + leading, segmentStart + leading + trimmed.length))
                }
            }
            return segments
        }

        /**
         * 补全场景: 光标(IntellijIdeaRulezzz 占位符)所在段若为简单列表达式前缀
         * (允许空段、结尾的 .)则返回该段范围,否则返回 null。
         */
        fun String.completableCaretColumnSegment(): TextRange? {
            val start = if (firstOrNull() == '\'' || firstOrNull() == '"') 1 else 0
            val end = if (length - 1 > start && (last() == '\'' || last() == '"')) length - 1 else length
            val caret = (indexOf("IntellijIdeaRulezzz").takeIf { it >= 0 } ?: length) - start

            var offset = 0
            substring(start, end).split(",").forEach { segment ->
                val segmentEnd = offset + segment.length
                if (caret <= segmentEnd) {
                    val cleaned = segment.replace("IntellijIdeaRulezzz", "").trim()
                    if (!cleaned.matches(CompletableColumnExpression)) {
                        return null
                    }
                    return TextRange(
                        start + offset + (segment.length - segment.trimStart().length),
                        start + segmentEnd
                    )
                }
                offset = segmentEnd + 1
            }
            return null
        }
        fun Variable.references(): @NotNull Query<PsiReference> =
            ReferencesSearch.search(this.originalElement, ProjectScope.getProjectScope(this.project), false)
        fun Parameter.references(): @NotNull Query<PsiReference> =
            ReferencesSearch.search(this.originalElement, ProjectScope.getProjectScope(this.project), false)

        fun PsiReference.statementFirstPsiChild(): PsiElement? = this.element.parentOfType<Statement>()?.firstPsiChild
        private fun PsiElement.typeAsInt(): Int = this.elementType?.index?.toInt() ?: 0

        fun PsiElement.nextSiblingInTreeWithText(text: String): PsiElement? {
            if (this.nextSibling == null) {
                if (this.parent.parent !is Statement) {
                    return this.parent.parent.firstChild.nextSiblingInTreeWithText(text)
                }

                return null
            }

            if (this.nextSibling.textMatches(text)) {
                return this.nextSibling
            }

            return this.nextSibling.nextSiblingInTreeWithText(text)
        }
    }
}
