package dev.ekvedaras.hyperfquery.completion

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.util.ProcessingContext
import com.jetbrains.php.lang.psi.elements.MethodReference
import dev.ekvedaras.hyperfquery.models.DbReferenceExpression
import dev.ekvedaras.hyperfquery.utils.DatabaseUtils.Companion.dbDataSources
import dev.ekvedaras.hyperfquery.utils.DatabaseUtils.Companion.schemas
import dev.ekvedaras.hyperfquery.utils.DatabaseUtils.Companion.tables
import dev.ekvedaras.hyperfquery.utils.HyperfUtils.Companion.isBuilderMethodForTableByName
import dev.ekvedaras.hyperfquery.utils.HyperfUtils.Companion.isDbFacadeSqlBindingMethod
import dev.ekvedaras.hyperfquery.utils.HyperfUtils.Companion.isEloquentModel
import dev.ekvedaras.hyperfquery.utils.HyperfUtils.Companion.isInsideRegularFunction
import dev.ekvedaras.hyperfquery.utils.HyperfUtils.Companion.isInteresting
import dev.ekvedaras.hyperfquery.utils.HyperfUtils.Companion.isTableParam
import dev.ekvedaras.hyperfquery.utils.HyperfUtils.Companion.modelTablePropertyClass
import dev.ekvedaras.hyperfquery.utils.HyperfUtils.Companion.shouldCompleteOnlyColumns
import dev.ekvedaras.hyperfquery.utils.HyperfUtils.Companion.shouldCompleteOnlySchemas
import dev.ekvedaras.hyperfquery.utils.HyperfUtils.Companion.shouldCompleteSchemas
import dev.ekvedaras.hyperfquery.utils.LookupUtils.Companion.buildLookup
import dev.ekvedaras.hyperfquery.utils.MethodUtils
import dev.ekvedaras.hyperfquery.utils.PsiUtils.Companion.containsVariable
import dev.ekvedaras.hyperfquery.utils.isJoinOrRelation

class TableOrViewCompletionProvider : CompletionProvider<CompletionParameters>() {
    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        val method = MethodUtils.resolveMethodReference(parameters.position)
        if (method == null) {
            addModelTablePropertyCompletions(parameters, result)
            return
        }
        val project = method.project

        if (shouldNotComplete(project, method, parameters)) {
            return
        }

        val target = DbReferenceExpression.create(parameters.position, DbReferenceExpression.Companion.Type.Table)
        val items = mutableListOf<LookupElement>()

        if (ApplicationManager.getApplication().isReadAccessAllowed) {
            ApplicationManager.getApplication().runReadAction {
                when (target.parts.size) {
                    1 -> populateWithOnePart(project, method, target, items)
                    else -> populateWithTwoParts(project, target, items)
                }
            }
        }

        result.addAllElements(
            items.distinctBy { it.lookupString }
        )

//        result.stopHere()
    }

    /**
     * Model $table 属性默认值补全:无方法调用上下文,提示 schema 和表,效果同 Db::table('...')。
     */
    private fun addModelTablePropertyCompletions(
        parameters: CompletionParameters,
        result: CompletionResultSet
    ) {
        if (!ApplicationManager.getApplication().isReadAccessAllowed || parameters.containsVariable()) {
            return
        }
        val project = parameters.position.project
        parameters.position.modelTablePropertyClass() ?: return

        val target = DbReferenceExpression.create(parameters.position, DbReferenceExpression.Companion.Type.Table)
        val items = mutableListOf<LookupElement>()

        ApplicationManager.getApplication().runReadAction {
            when (target.parts.size) {
                1 -> project.dbDataSources().forEach { dataSource ->
                    dataSource.schemas(target.connectionSchema).forEach { schema ->
                        items.add(schema.buildLookup(project, dataSource))
                    }
                    dataSource.tables(target.connectionSchema, target.connectionPrefix).forEach { table ->
                        items.add(table.buildLookup(project, connectionPrefix = target.connectionPrefix))
                    }
                }
                else -> populateWithTwoParts(project, target, items)
            }
        }

        result.addAllElements(items.distinctBy { it.lookupString })
    }

    private fun populateWithOnePart(
        project: Project,
        method: MethodReference,
        target: DbReferenceExpression,
        result: MutableList<LookupElement>
    ) {
        project.dbDataSources().forEach dataSources@{ dataSource ->
            if (method.shouldCompleteSchemas(project)) {
                dataSource.schemas(target.connectionSchema).forEach { schema ->
                    result.add(schema.buildLookup(project, dataSource))
                }

                if (method.shouldCompleteOnlySchemas()) {
                    return@dataSources
                }
            }

            dataSource.tables(target.connectionSchema, target.connectionPrefix).forEach { table ->
                result.add(table.buildLookup(project, connectionPrefix = target.connectionPrefix))
            }
        }
    }

    private fun populateWithTwoParts(
        project: Project,
        target: DbReferenceExpression,
        result: MutableList<LookupElement>,
    ) {
        target.schema.forEach { schema ->
            schema.tables(project, target.connectionPrefix).forEach { table ->
                result.add(table.buildLookup(project, true, connectionPrefix = target.connectionPrefix))
            }
        }
    }

    private fun shouldNotComplete(project: Project, method: MethodReference, parameters: CompletionParameters) =
        !ApplicationManager.getApplication().isReadAccessAllowed ||
            method.isDbFacadeSqlBindingMethod(project) ||
            !method.isBuilderMethodForTableByName() ||
            !parameters.isTableParam() ||
            (
                (method.isEloquentModel(project) || method.isJoinOrRelation(project)) &&
                    method.shouldCompleteOnlyColumns()
                ) ||
            parameters.isInsideRegularFunction() ||
            !method.isInteresting(project)
}
