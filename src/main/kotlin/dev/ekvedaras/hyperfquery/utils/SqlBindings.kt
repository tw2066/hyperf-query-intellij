package dev.ekvedaras.hyperfquery.utils

import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.TreeElement
import com.intellij.psi.util.parentOfType
import com.intellij.util.ArrayUtil
import com.jetbrains.php.lang.psi.elements.ArrayHashElement
import com.jetbrains.php.lang.psi.elements.MethodReference
import com.jetbrains.php.lang.psi.elements.ParameterList
import com.jetbrains.php.lang.psi.elements.PhpPsiElement
import com.jetbrains.php.lang.psi.elements.Statement
import com.jetbrains.php.lang.psi.elements.impl.ArrayCreationExpressionImpl
import com.jetbrains.php.lang.psi.elements.impl.AssignmentExpressionImpl
import com.jetbrains.php.lang.psi.elements.impl.StringLiteralExpressionImpl
import com.jetbrains.php.lang.psi.elements.impl.VariableImpl
import dev.ekvedaras.hyperfquery.utils.PsiUtils.Companion.isArrayKey
import dev.ekvedaras.hyperfquery.utils.PsiUtils.Companion.isArrayValue

private val PLACEHOLDER_PATTERN = Regex(":([a-zA-Z_][a-zA-Z0-9_]*)")
private val QUOTED_SQL_PATTERN = Regex("'[^']*'|\"[^\"]*\"")
private val SQL_LINE_COMMENT_PATTERN = Regex("--[^\n]*")

/** Named placeholders in SQL, ordered, unique, skipping quoted strings and `--` line comments. */
fun extractPlaceholders(sqlText: String): List<String> =
    PLACEHOLDER_PATTERN.findAll(
        sqlText.replace(QUOTED_SQL_PATTERN, " ").replace(SQL_LINE_COMMENT_PATTERN, " ")
    ).map { it.groupValues[1] }.distinct().toList()

/**
 * The string literal holding the SQL for this call: param 0 as an inline literal,
 * or the nearest preceding `$sql = '...'` assignment in the same scope, found by
 * walking backwards through sibling statements. A reference search is deliberately
 * not used here — it would run this plugin's own reference provider on candidate
 * elements, recursing until a StackOverflowError.
 */
fun MethodReference.sqlLiteral(): StringLiteralExpressionImpl? {
    val param = this.getParameter(0) ?: return null
    if (param is StringLiteralExpressionImpl) {
        return param
    }

    val variable = param as? VariableImpl ?: return null
    val targetName = variable.name?.removePrefix("$") ?: return null

    var statement: PsiElement? = this.parentOfType<Statement>()
    while (statement != null) {
        val expression = (statement as? PhpPsiElement)?.firstPsiChild
        if (expression is AssignmentExpressionImpl) {
            val lhs = expression.getVariable()
            if (lhs is VariableImpl && lhs.name?.removePrefix("$") == targetName) {
                return expression.getValue() as? StringLiteralExpressionImpl
            }
        }
        statement = statement.prevSibling
    }

    return null
}

/**
 * True when this string literal is (or is being typed as) a KEY of the array at
 * param index 1 of [method]. A bare array element without a preceding `=>` (e.g.
 * `['']`) counts as a key being typed; a literal that follows `=>` is a value.
 */
fun StringLiteralExpressionImpl.isBindingsArrayKey(method: MethodReference): Boolean {
    if (this.parent?.isArrayKey() != true && this.parent?.isArrayValue() != true) {
        return false
    }

    val array = this.parentOfType<ArrayCreationExpressionImpl>() ?: return false
    val parameterList = array.parent as? ParameterList ?: return false
    if (parameterList.parent != method || ArrayUtil.indexOf(parameterList.parameters, array) != 1) {
        return false
    }

    return this.parent?.isArrayKey() == true || !this.isAfterArrow()
}

private fun StringLiteralExpressionImpl.isAfterArrow(): Boolean {
    var sibling: PsiElement? = this.parent
    while (sibling != null) {
        if (sibling is TreeElement && sibling.textMatches("=>")) {
            return true
        }
        sibling = sibling.prevSibling
    }
    return false
}

/** Existing binding keys (both `id` and `:id` forms) normalized to plain names. */
fun MethodReference.bindingsKeys(): Set<String> {
    val array = this.getParameter(1) as? ArrayCreationExpressionImpl ?: return emptySet()
    return array.children.filterIsInstance<ArrayHashElement>().mapNotNull { hash ->
        (hash.key as? StringLiteralExpressionImpl)?.contents?.removePrefix(":")
    }.toSet()
}
