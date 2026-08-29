package dev.ekvedaras.hyperfquery.completion

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.database.model.DasNamespace
import com.intellij.database.psi.DbDataSource
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.util.ProcessingContext
import com.jetbrains.php.lang.psi.elements.MethodReference
import dev.ekvedaras.hyperfquery.models.DbReferenceExpression
import dev.ekvedaras.hyperfquery.utils.BlueprintMethod.Companion.createsTable
import dev.ekvedaras.hyperfquery.utils.BlueprintMethod.Companion.isColumnDefinition
import dev.ekvedaras.hyperfquery.utils.BlueprintMethod.Companion.isInsideUpMigration
import dev.ekvedaras.hyperfquery.utils.DatabaseUtils.Companion.columnsInParallel
import dev.ekvedaras.hyperfquery.utils.DatabaseUtils.Companion.dbDataSourcesInParallel
import dev.ekvedaras.hyperfquery.utils.DatabaseUtils.Companion.nameWithoutPrefix
import dev.ekvedaras.hyperfquery.utils.DatabaseUtils.Companion.schemasInParallel
import dev.ekvedaras.hyperfquery.utils.DatabaseUtils.Companion.tables
import dev.ekvedaras.hyperfquery.utils.DatabaseUtils.Companion.tablesInParallel
import dev.ekvedaras.hyperfquery.utils.HyperfUtils.Companion.canHaveColumnsInArrayValues
import dev.ekvedaras.hyperfquery.utils.HyperfUtils.Companion.canOnlyHaveColumnsInArrayValues
import dev.ekvedaras.hyperfquery.utils.HyperfUtils.Companion.isAssocArrayValue
import dev.ekvedaras.hyperfquery.utils.HyperfUtils.Companion.isBlueprintMethod
import dev.ekvedaras.hyperfquery.utils.HyperfUtils.Companion.isBuilderMethodForColumns
import dev.ekvedaras.hyperfquery.utils.HyperfUtils.Companion.isColumnDefinitionMethod
import dev.ekvedaras.hyperfquery.utils.HyperfUtils.Companion.isColumnIn
import dev.ekvedaras.hyperfquery.utils.HyperfUtils.Companion.isDbFacadeSqlBindingMethod
import dev.ekvedaras.hyperfquery.utils.HyperfUtils.Companion.isEloquentModel
import dev.ekvedaras.hyperfquery.utils.HyperfUtils.Companion.isInsidePhpArrayOrValue
import dev.ekvedaras.hyperfquery.utils.HyperfUtils.Companion.isInsideRegularFunction
import dev.ekvedaras.hyperfquery.utils.HyperfUtils.Companion.isInteresting
import dev.ekvedaras.hyperfquery.utils.HyperfUtils.Companion.isRawExpressionMethod
import dev.ekvedaras.hyperfquery.utils.HyperfUtils.Companion.modelColumnPropertyClass
import dev.ekvedaras.hyperfquery.utils.HyperfUtils.Companion.shouldCompleteOnlyColumns
import dev.ekvedaras.hyperfquery.utils.LookupUtils
import dev.ekvedaras.hyperfquery.utils.LookupUtils.Companion.buildLookup
import dev.ekvedaras.hyperfquery.utils.MethodUtils
import dev.ekvedaras.hyperfquery.utils.PsiUtils.Companion.completableCaretColumnSegment
import dev.ekvedaras.hyperfquery.utils.PsiUtils.Companion.containsVariable
import java.util.Collections

class ColumnCompletionProvider(private val shouldCompleteAll: Boolean = false) :
    CompletionProvider<CompletionParameters>() {
    private var onlyColumns = false

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        val method = MethodUtils.resolveMethodReference(parameters.position)
        if (method == null) {
            addModelPropertyCompletions(parameters, result)
            return
        }
        val project = method.project

        if (shouldNotComplete(project, method, parameters)) {
            return
        }

        // raw SQL 片段: 只解析光标所在的逗号分段。匹配前缀绑定到当前段已输入文本
        // (IntelliJ 默认把整串内容当前缀,逗号分段后无法匹配); 插入时保留段内表名/别名前缀。
        var rawInsertionPrefix: String? = null
        var resultSet = result
        val rawSegment = if (method.isRawExpressionMethod()) {
            val positionText = parameters.position.text
            val caretAt = positionText.indexOf("IntellijIdeaRulezzz").takeIf { it >= 0 } ?: positionText.length
            val segmentTyped = positionText.substring(0, caretAt)
                .trimStart('\'', '"')
                .substringAfterLast(',')
                .trimStart()
            rawInsertionPrefix = segmentTyped.substring(0, segmentTyped.lastIndexOf('.') + 1)
            resultSet = result.withPrefixMatcher(segmentTyped)
            parameters.position.text.completableCaretColumnSegment()
        } else {
            null
        }
        val target = DbReferenceExpression(parameters.position, DbReferenceExpression.Companion.Type.Column, rawSegment)
        val items = Collections.synchronizedList(mutableListOf<LookupElement>())

        if (ApplicationManager.getApplication().isReadAccessAllowed) {
            ApplicationManager.getApplication().runReadAction {
                when (target.parts.size) {
                    1 -> completeForOnePart(project, target, items, method, resultSet, rawInsertionPrefix)
                    2 -> completeForTwoParts(project, target, items, rawInsertionPrefix)
                    else -> completeForThreeParts(project, target, items, rawInsertionPrefix)
                }
            }
        }

        resultSet.addAllElements(
            items.distinctBy { it.lookupString }
        )
    }

    /**
     * Model 属性数组($fillable/$guarded/$casts 等)中的列名补全:只提示模型表中的列。
     */
    private fun addModelPropertyCompletions(
        parameters: CompletionParameters,
        result: CompletionResultSet
    ) {
        if (!ApplicationManager.getApplication().isReadAccessAllowed || parameters.containsVariable()) {
            return
        }
        val project = parameters.position.project
        parameters.position.modelColumnPropertyClass() ?: return

        val target = DbReferenceExpression(parameters.position, DbReferenceExpression.Companion.Type.Column)
        if (target.parts.size != 1) {
            return
        }

        val items = Collections.synchronizedList(mutableListOf<LookupElement>())
        ApplicationManager.getApplication().runReadAction {
            project.dbDataSourcesInParallel().forEach { dataSource ->
                target.tablesAndAliases.forEach { tableAlias ->
                    dataSource.tables(target.connectionSchema, target.connectionPrefix).firstOrNull { dasTable ->
                        dasTable.nameWithoutPrefix(project, target.connectionPrefix) == tableAlias.value.first &&
                            (tableAlias.value.second == null || dasTable.dasParent?.name == tableAlias.value.second)
                    }?.columnsInParallel()?.forEach { column ->
                        items.add(column.buildLookup(project, connectionPrefix = target.connectionPrefix))
                    }
                }
            }
        }

        result.addAllElements(items.distinctBy { it.lookupString })
    }

    private fun completeForOnePart(
        project: Project,
        target: DbReferenceExpression,
        items: MutableList<LookupElement>,
        method: MethodReference,
        result: CompletionResultSet,
        rawInsertionPrefix: String? = null,
    ) {
        val schemas = target.tablesAndAliases.map { it.value.second }.filterNotNull().distinct()
        onlyColumns = method.isBlueprintMethod(project) ||
            method.isColumnDefinitionMethod(project) ||
            method.shouldCompleteOnlyColumns()

        project.dbDataSourcesInParallel().forEach { dataSource ->
            if (!onlyColumns) {
                dataSource.schemasInParallel(target.connectionSchema).filter {
                    shouldCompleteAll || schemas.isEmpty() || schemas.contains(it.name)
                }.forEach { schema ->
                    addSchemaAndItsTables(items, schema, project, dataSource, target)
                }
            }

            if (target.tablesAndAliases.isNotEmpty()) {
                addTablesAndAliases(result, target, dataSource, project, items, rawInsertionPrefix)
            }
        }
    }

    private fun addSchemaAndItsTables(
        items: MutableList<LookupElement>,
        schema: DasNamespace,
        project: Project,
        dataSource: DbDataSource,
        target: DbReferenceExpression
    ) {
        items.add(schema.buildLookup(project, dataSource))

        if (shouldCompleteAll || target.tablesAndAliases.isEmpty()) {
            schema.tablesInParallel(project, target.connectionPrefix).forEach { table ->
                items.add(
                    table.buildLookup(
                        project,
                        withTablePrefix = false,
                        triggerCompletion = true,
                        connectionPrefix = target.connectionPrefix
                    )
                )
            }
        }
    }

    private fun addTablesAndAliases(
        result: CompletionResultSet,
        target: DbReferenceExpression,
        dataSource: DbDataSource,
        project: Project,
        items: MutableList<LookupElement>,
        rawInsertionPrefix: String? = null,
    ) {
        result.addLookupAdvertisement("CTRL(CMD) + SHIFT + Space to see all options")
        target.tablesAndAliases.forEach { tableAlias ->
            val table = dataSource.tables(target.connectionSchema, target.connectionPrefix).firstOrNull { dasTable ->
                dasTable.nameWithoutPrefix(project, target.connectionPrefix) == tableAlias.value.first &&
                    (tableAlias.value.second == null || dasTable.dasParent?.name == tableAlias.value.second)
            }

            if (!onlyColumns) {
                items.add(
                    LookupUtils.buildForAliasOrTable(project, tableAlias, dataSource, table)
                )
            }

            table?.columnsInParallel()?.forEach { column ->
                items.add(
                    column.buildLookup(project, connectionPrefix = target.connectionPrefix, verbatimInsertPrefix = rawInsertionPrefix)
                )
            }
        }
    }

    private fun completeForTwoParts(
        project: Project,
        target: DbReferenceExpression,
        result: MutableList<LookupElement>,
        rawInsertionPrefix: String? = null,
    ) {
        project.dbDataSourcesInParallel().forEach {
            if (target.schema.isNotEmpty()) {
                addTables(target, result, project)
            } else {
                addTableColumns(target, result, project, rawInsertionPrefix)
            }
        }
    }

    private fun addTableColumns(
        target: DbReferenceExpression,
        result: MutableList<LookupElement>,
        project: Project,
        rawInsertionPrefix: String? = null,
    ) {
        target.table.parallelStream().forEach { table ->
            // raw 段里插入文本保留光标前已输入的内容,列名前不再加表名/别名前缀
            val alias = if (target.isRawExpression) {
                null
            } else {
                target.tablesAndAliases.entries
                    .filter { it.value.first != it.key }
                    .firstOrNull { it.value.first == table.nameWithoutPrefix(project, target.connectionPrefix) }?.key
            }

            table.columnsInParallel().forEach { column ->
                result.add(
                    column.buildLookup(
                        project,
                        withTablePrefix = !target.isRawExpression,
                        withSchemaPrefix = false,
                        alias = alias,
                        connectionPrefix = target.connectionPrefix,
                        verbatimInsertPrefix = rawInsertionPrefix
                    )
                )
            }
        }
    }

    private fun addTables(
        target: DbReferenceExpression,
        result: MutableList<LookupElement>,
        project: Project
    ) {
        target.schema.parallelStream().forEach { schema ->
            schema.tablesInParallel(project, target.connectionPrefix).forEach { table ->
                result.add(
                    table.buildLookup(
                        project,
                        withTablePrefix = true,
                        triggerCompletion = true,
                        connectionPrefix = target.connectionPrefix
                    )
                )
            }
        }
    }

    private fun completeForThreeParts(
        project: Project,
        target: DbReferenceExpression,
        result: MutableList<LookupElement>,
        rawInsertionPrefix: String? = null,
    ) {
        target.table.parallelStream().forEach { table ->
            table.columnsInParallel().forEach { column ->
                result.add(
                    column.buildLookup(
                        project,
                        withTablePrefix = !target.isRawExpression,
                        withSchemaPrefix = !target.isRawExpression,
                        connectionPrefix = target.connectionPrefix,
                        verbatimInsertPrefix = rawInsertionPrefix
                    )
                )
            }
        }
    }

    @Suppress("ComplexMethod")
    private fun shouldNotComplete(
        project: Project,
        method: MethodReference,
        parameters: CompletionParameters
    ): Boolean {
        val allowArray = !(method.name?.equals("whereIn") ?: false) &&
            (method.name?.startsWith("where") ?: false)

        return !ApplicationManager.getApplication().isReadAccessAllowed ||
            parameters.containsVariable() ||
            method.isDbFacadeSqlBindingMethod(project) ||
            !method.isBuilderMethodForColumns() ||
            (method.isRawExpressionMethod() && parameters.position.text.completableCaretColumnSegment() == null) ||
            (method.isBlueprintMethod(project) && method.isColumnDefinition() && method.isInsideUpMigration() && method.createsTable()) ||
            !parameters.isColumnIn(method, allowArray) ||
            parameters.isInsideRegularFunction() ||
            (
                parameters.isInsidePhpArrayOrValue() &&
                    parameters.position.isAssocArrayValue() &&
                    !method.canHaveColumnsInArrayValues()
                ) ||
            (!parameters.isInsidePhpArrayOrValue() && method.canOnlyHaveColumnsInArrayValues()) ||
            (
                parameters.position.isAssocArrayValue() &&
                    method.shouldCompleteOnlyColumns() &&
                    method.isEloquentModel(project)
                ) ||
            !method.isInteresting(project)

    }
}
