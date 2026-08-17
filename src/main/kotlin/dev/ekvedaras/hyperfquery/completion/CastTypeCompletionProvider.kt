package dev.ekvedaras.hyperfquery.completion

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.icons.AllIcons
import com.intellij.openapi.application.ApplicationManager
import com.intellij.util.ProcessingContext
import dev.ekvedaras.hyperfquery.utils.HyperfUtils.Companion.ModelCastTypes
import dev.ekvedaras.hyperfquery.utils.HyperfUtils.Companion.ModelParameterizedCastTypes
import dev.ekvedaras.hyperfquery.utils.HyperfUtils.Companion.modelCastsValueClass
import dev.ekvedaras.hyperfquery.utils.PsiUtils.Companion.containsVariable

/**
 * $casts 数组值位置的 cast 类型补全(integer/datetime/decimal:<digits> 等)。
 */
class CastTypeCompletionProvider : CompletionProvider<CompletionParameters>() {
    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        if (!ApplicationManager.getApplication().isReadAccessAllowed || parameters.containsVariable()) {
            return
        }
        parameters.position.modelCastsValueClass() ?: return

        ModelCastTypes.forEach { castType ->
            result.addElement(
                LookupElementBuilder
                    .create(castType)
                    .withIcon(AllIcons.Nodes.Type)
                    .withTypeText("cast", true)
            )
        }

        ModelParameterizedCastTypes.forEach { (presentable, inserted) ->
            result.addElement(
                LookupElementBuilder
                    .create(inserted)
                    .withPresentableText(presentable)
                    .withIcon(AllIcons.Nodes.Type)
                    .withTypeText("cast", true)
            )
        }
    }
}
