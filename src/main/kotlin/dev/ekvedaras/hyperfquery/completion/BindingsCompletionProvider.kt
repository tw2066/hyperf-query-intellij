package dev.ekvedaras.hyperfquery.completion

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.util.ProcessingContext
import com.intellij.psi.util.parentOfType
import com.jetbrains.php.lang.psi.elements.impl.StringLiteralExpressionImpl
import dev.ekvedaras.hyperfquery.utils.HyperfUtils.Companion.isDbFacadeSqlBindingMethod
import dev.ekvedaras.hyperfquery.utils.MethodUtils
import dev.ekvedaras.hyperfquery.utils.bindingsKeys
import dev.ekvedaras.hyperfquery.utils.extractPlaceholders
import dev.ekvedaras.hyperfquery.utils.isBindingsArrayKey
import dev.ekvedaras.hyperfquery.utils.sqlLiteral

class BindingsCompletionProvider : CompletionProvider<CompletionParameters>() {
    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        val method = MethodUtils.resolveMethodReference(parameters.position) ?: return
        val project = method.project

        if (!method.isDbFacadeSqlBindingMethod(project)) {
            return
        }

        val literal = parameters.position.parentOfType<StringLiteralExpressionImpl>()
            ?: parameters.position as? StringLiteralExpressionImpl
            ?: return
        if (!literal.isBindingsArrayKey(method)) {
            return
        }

        val sqlLiteral = method.sqlLiteral() ?: return
        val placeholders = extractPlaceholders(sqlLiteral.contents)
        if (placeholders.isEmpty()) {
            return
        }

        val used = method.bindingsKeys()

        // Suggest the `name` form for a plain prefix and the `:name` form once the
        // user typed a leading colon, so `''` yields `id` and `':'` yields `:id`.
        val typed = literal.containingFile.text
            .substring(literal.textRange.startOffset + 1, parameters.offset)
        val withColon = typed.startsWith(":")

        result.addAllElements(
            placeholders.filterNot { it in used }.map { name ->
                LookupElementBuilder
                    .create(if (withColon) ":$name" else name)
                    .withTypeText("binding", true)
            }
        )
    }
}
