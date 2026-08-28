package dev.ekvedaras.hyperfquery.completion

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.application.ApplicationManager
import com.intellij.util.ProcessingContext
import dev.ekvedaras.hyperfquery.utils.DatabasesConfig.Companion.databaseConnections
import dev.ekvedaras.hyperfquery.utils.HyperfUtils.Companion.isConnectionParam
import dev.ekvedaras.hyperfquery.utils.HyperfUtils.Companion.isModelConnectionProperty
import dev.ekvedaras.hyperfquery.utils.PsiUtils.Companion.containsVariable
import icons.DatabaseIcons

/**
 * 连接名补全:Db::connection('...') / Schema::connection('...') 参数、
 * Model 子类 $connection 属性默认值。候选取自 config/autoload/databases.php。
 */
class ConnectionCompletionProvider : CompletionProvider<CompletionParameters>() {
    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        if (!ApplicationManager.getApplication().isReadAccessAllowed || parameters.containsVariable()) {
            return
        }

        val project = parameters.position.project
        if (!parameters.position.isConnectionParam(project) && !parameters.position.isModelConnectionProperty()) {
            return
        }

        project.databaseConnections().forEach { (name, connection) ->
            result.addElement(
                LookupElementBuilder
                    .create(name)
                    .withIcon(DatabaseIcons.Synonym)
                    .withTypeText(connection.database ?: "connection", true)
            )
        }
    }
}
