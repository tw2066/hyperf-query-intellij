package dev.ekvedaras.hyperfquery.completion

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.database.model.DasTable
import com.intellij.database.model.ObjectKind
import com.intellij.database.util.DbUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.psi.util.parentOfType
import com.intellij.util.ProcessingContext
import com.jetbrains.php.lang.psi.elements.Method
import com.jetbrains.php.lang.psi.elements.MethodReference
import com.jetbrains.php.lang.psi.elements.PhpClass
import com.jetbrains.php.lang.psi.elements.Variable
import com.jetbrains.php.lang.psi.elements.impl.VariableImpl
import com.jetbrains.rd.util.first
import dev.ekvedaras.hyperfquery.models.DbReferenceExpression
import dev.ekvedaras.hyperfquery.services.HyperfQuerySettings
import dev.ekvedaras.hyperfquery.utils.BlueprintMethod.Companion.createsTable
import dev.ekvedaras.hyperfquery.utils.BlueprintMethod.Companion.dbIcon
import dev.ekvedaras.hyperfquery.utils.BlueprintMethod.Companion.getColumnDefinitionReference
import dev.ekvedaras.hyperfquery.utils.BlueprintMethod.Companion.getColumnName
import dev.ekvedaras.hyperfquery.utils.BlueprintMethod.Companion.getColumns
import dev.ekvedaras.hyperfquery.utils.BlueprintMethod.Companion.getIndexName
import dev.ekvedaras.hyperfquery.utils.BlueprintMethod.Companion.hasIndex
import dev.ekvedaras.hyperfquery.utils.BlueprintMethod.Companion.hasUniqueIndex
import dev.ekvedaras.hyperfquery.utils.BlueprintMethod.Companion.isColumnDefinition
import dev.ekvedaras.hyperfquery.utils.BlueprintMethod.Companion.isId
import dev.ekvedaras.hyperfquery.utils.BlueprintMethod.Companion.isInsideUpMigration
import dev.ekvedaras.hyperfquery.utils.BlueprintMethod.Companion.isPrimary
import dev.ekvedaras.hyperfquery.utils.BlueprintMethod.Companion.isSoftDeletes
import dev.ekvedaras.hyperfquery.utils.BlueprintMethod.Companion.isTimestamps
import dev.ekvedaras.hyperfquery.utils.BlueprintMethod.Companion.wantsColumn
import dev.ekvedaras.hyperfquery.utils.BlueprintMethod.Companion.wantsColumnForIndexes
import dev.ekvedaras.hyperfquery.utils.DatabaseUtils.Companion.tables
import dev.ekvedaras.hyperfquery.utils.HyperfUtils.Companion.isBlueprintMethod
import dev.ekvedaras.hyperfquery.utils.HyperfUtils.Companion.isBuilderMethodForIndexes
import dev.ekvedaras.hyperfquery.utils.HyperfUtils.Companion.isBuilderMethodForKeys
import dev.ekvedaras.hyperfquery.utils.HyperfUtils.Companion.isBuilderMethodForUniqueIndexes
import dev.ekvedaras.hyperfquery.utils.HyperfUtils.Companion.isInsideRegularFunction
import dev.ekvedaras.hyperfquery.utils.MethodUtils
import dev.ekvedaras.hyperfquery.utils.MethodUtils.Companion.isSameInSameFile
import dev.ekvedaras.hyperfquery.utils.PsiUtils.Companion.references
import dev.ekvedaras.hyperfquery.utils.SchemaMethod.Companion.blueprintTableParam
import dev.ekvedaras.hyperfquery.utils.SchemaMethod.Companion.statementsForTable
import icons.DatabaseIcons

class NewMigrationCompletionProvider : CompletionProvider<CompletionParameters>() {
    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        val method = MethodUtils.resolveMethodReference(parameters.position) ?: return
        val project = method.project

        if (shouldNotComplete(project, method, parameters)) {
            return
        }

        val target = DbReferenceExpression.create(parameters.position, DbReferenceExpression.Companion.Type.Column)
        if (target.tablesAndAliases.isEmpty()) {
            return
        }

        val items = mutableListOf<LookupElement>()

        var table: DasTable? = null;

        DbUtil.getDataSources(project).filter {
            HyperfQuerySettings.getInstance(project).interestedIn(it)
        }.forEach { dataSource ->
            val dasTable = dataSource.tables().firstOrNull { it.name == target.tablesAndAliases.first().key }
            if (dasTable != null) {
                table = dasTable
                return@forEach
            }
        }

        val columns = table?.getDasChildren(ObjectKind.COLUMN)?.map { it.name } ?: listOf<String>()
//        val indexes = table?.getDasChildren(ObjectKind.INDEX)?.map { it.name } ?: listOf<String>()
//        val keys = table?.getDasChildren(ObjectKind.KEY)?.map { it.name } ?: listOf<String>()
//        val foreignKeys = table?.getDasChildren(ObjectKind.FOREIGN_KEY)?.map { it.name } ?: listOf<String>()

        if (ApplicationManager.getApplication().isReadAccessAllowed) {
            ApplicationManager.getApplication().runReadAction {
                method.parentOfType<PhpClass>()?.ownMethods?.forEach { migrationMethod ->
                    if (migrationMethod.name == "up") {
                        scabMigrationMethod(migrationMethod, target, method, columns, items)
                    }

                    if (shouldScanDownMethod(migrationMethod, method)) {
                        scabMigrationMethod(migrationMethod, target, method, columns, items)
                    }
                }
            }
        }

        result.addAllElements(
            items.distinctBy { it.lookupString }
        )

        result.stopHere()
    }

    private fun shouldScanDownMethod(
        migrationMethod: Method,
        method: MethodReference
    ) = migrationMethod.name == "down" &&
        method.parentOfType<Method>()?.name == "down" &&
        (
            method.isBuilderMethodForIndexes() ||
                method.isBuilderMethodForKeys() ||
                method.isBuilderMethodForUniqueIndexes()
            )

    private fun scabMigrationMethod(
        migrationMethod: Method,
        target: DbReferenceExpression,
        method: MethodReference,
        columns: Iterable<String>,
        items: MutableList<LookupElement>
    ) {
        migrationMethod.statementsForTable(target.tablesAndAliases.first().key).forEach { statementMethod ->
            statementMethod.blueprintTableParam()?.references()?.forEach referenceLoop@{ reference ->
                val referenceMethod = (reference.element as Variable).parent as? MethodReference ?: return@referenceLoop

                if (referenceMethod.isSameInSameFile(method)) {
                    return@referenceLoop
                }

                if (method.isInsideUpMigration() && method.createsTable() && method.isColumnDefinition()) {
                    return@referenceLoop
                }

                if (method.wantsColumn()) {
                    if (method.wantsColumnForIndexes()) {
                        addColumnForIndex(method, referenceMethod, items, target)
                    } else {
                        addColumn(referenceMethod, columns, items, target)
                    }
                } else {
                    if (!referenceMethod.isColumnDefinition()) {
                        addIndexFromIndexMethod(method, referenceMethod, items, target)

                        return@referenceLoop
                    }

                    addIndexFromColumnMethod(method, referenceMethod, items, target)
                }
            }
        }
    }

    private fun addIndexFromColumnMethod(
        method: MethodReference,
        referenceMethod: MethodReference,
        items: MutableList<LookupElement>,
        target: DbReferenceExpression
    ) {
        if (method.isBuilderMethodForKeys() && referenceMethod.isPrimary()) {
            items.add(
                LookupElementBuilder
                    .create("${target.tablesAndAliases.first().key}_${referenceMethod.getColumnName() ?: "?"}_primary")
                    .withIcon(DatabaseIcons.GoldKey)
                    .withTailText("  " + referenceMethod.name)
                    .withTypeText(target.tablesAndAliases.first().key)
                    .withPsiElement(referenceMethod)
            )
        } else if (method.isBuilderMethodForIndexes() && referenceMethod.hasIndex()) {
            items.add(
                LookupElementBuilder
                    .create("${target.tablesAndAliases.first().key}_${referenceMethod.getColumnName() ?: "?"}_index")
                    .withIcon(DatabaseIcons.Index)
                    .withTailText("  " + referenceMethod.name)
                    .withTypeText(target.tablesAndAliases.first().key)
                    .withPsiElement(referenceMethod)
            )
        } else if (method.isBuilderMethodForUniqueIndexes() && referenceMethod.hasUniqueIndex()) {
            items.add(
                LookupElementBuilder
                    .create("${target.tablesAndAliases.first().key}_${referenceMethod.getColumnName() ?: "?"}_unique")
                    .withIcon(DatabaseIcons.BlueKey)
                    .withTailText("  " + referenceMethod.name)
                    .withTypeText(target.tablesAndAliases.first().key)
                    .withPsiElement(referenceMethod)
            )
        }
    }

    private fun addIndexFromIndexMethod(
        method: MethodReference,
        referenceMethod: MethodReference,
        items: MutableList<LookupElement>,
        target: DbReferenceExpression
    ) {
        if (method.isBuilderMethodForKeys() && referenceMethod.name == "primary") {
            items.add(
                LookupElementBuilder
                    .create(referenceMethod.getIndexName(target.tablesAndAliases.first().key))
                    .withIcon(DatabaseIcons.GoldKey)
                    .withTailText("  " + referenceMethod.name)
                    .withTypeText(target.tablesAndAliases.first().key)
                    .withPsiElement(referenceMethod)
            )
        } else if (method.isBuilderMethodForIndexes() && referenceMethod.name == "index") {
            items.add(
                LookupElementBuilder
                    .create(referenceMethod.getIndexName(target.tablesAndAliases.first().key))
                    .withIcon(DatabaseIcons.Index)
                    .withTailText("  " + referenceMethod.name)
                    .withTypeText(target.tablesAndAliases.first().key)
                    .withPsiElement(referenceMethod)
            )
        } else if (method.isBuilderMethodForUniqueIndexes() && referenceMethod.name == "unique") {
            items.add(
                LookupElementBuilder
                    .create(referenceMethod.getIndexName(target.tablesAndAliases.first().key))
                    .withIcon(DatabaseIcons.BlueKey)
                    .withTailText("  " + referenceMethod.name)
                    .withTypeText(target.tablesAndAliases.first().key)
                    .withPsiElement(referenceMethod)
            )
        }
    }

    private fun addColumn(
        referenceMethod: MethodReference,
        columns: Iterable<String>,
        items: MutableList<LookupElement>,
        target: DbReferenceExpression
    ) {
        if (referenceMethod.isId() && !columns.contains("id")) {
            items.add(
                LookupElementBuilder
                    .create("id")
                    .withIcon(DatabaseIcons.ColGoldKey)
                    .withTailText("  primary")
                    .withTypeText(target.tablesAndAliases.first().key)
                    .withPsiElement(referenceMethod)
            )
        } else if (referenceMethod.isTimestamps()) {
            if (!columns.contains("created_at")) {
                items.add(
                    LookupElementBuilder
                        .create("created_at")
                        .withIcon(DatabaseIcons.ColDot)
                        .withTailText("  " + referenceMethod.name)
                        .withTypeText(target.tablesAndAliases.first().key)
                        .withPsiElement(referenceMethod)
                )
            }

            if (!columns.contains("updated_at")) {
                items.add(
                    LookupElementBuilder
                        .create("updated_at")
                        .withIcon(DatabaseIcons.ColDot)
                        .withTailText("  " + referenceMethod.name)
                        .withTypeText(target.tablesAndAliases.first().key)
                        .withPsiElement(referenceMethod)
                )
            }
        } else if (referenceMethod.isSoftDeletes() && !columns.contains("deleted_at")) {
            items.add(
                LookupElementBuilder
                    .create("deleted_at")
                    .withIcon(DatabaseIcons.Col)
                    .withTailText("  timestamp")
                    .withTypeText(target.tablesAndAliases.first().key)
                    .withPsiElement(referenceMethod)
            )
        } else if (referenceMethod.isColumnDefinition() && !columns.contains(referenceMethod.getColumnName())) {
            items.add(
                LookupElementBuilder
                    .create(referenceMethod.getColumnName() ?: '?')
                    .withIcon(referenceMethod.dbIcon())
                    .withTailText("  " + referenceMethod.name)
                    .withTypeText(target.tablesAndAliases.first().key)
                    .withPsiElement(referenceMethod.getColumnDefinitionReference())
            )
        }
    }

    private fun addColumnForIndex(
        method: MethodReference,
        referenceMethod: MethodReference,
        items: MutableList<LookupElement>,
        target: DbReferenceExpression
    ) {
        if (method.isBuilderMethodForKeys() && (referenceMethod.isPrimary() || referenceMethod.isBuilderMethodForKeys())) {
            referenceMethod.getColumns().forEach { column ->
                items.add(
                    LookupElementBuilder
                        .create(column)
                        .withIcon(referenceMethod.dbIcon()) // TODO: find column definition method and use that here
//                        .withTailText("  " + referenceMethod.name) // TODO: find column definition method and use that here
                        .withTypeText(target.tablesAndAliases.first().key)
                        .withPsiElement(referenceMethod.getColumnDefinitionReference())
                )
            }
        } else if (method.isBuilderMethodForIndexes() && (referenceMethod.hasIndex() || referenceMethod.isBuilderMethodForIndexes())) {
            referenceMethod.getColumns().forEach { column ->
                items.add(
                    LookupElementBuilder
                        .create(column)
                        .withIcon(referenceMethod.dbIcon()) // TODO: find column definition method and use that here
//                        .withTailText("  " + referenceMethod.name) // TODO: find column definition method and use that here
                        .withTypeText(target.tablesAndAliases.first().key)
                        .withPsiElement(referenceMethod.getColumnDefinitionReference())
                )
            }
        } else if (method.isBuilderMethodForUniqueIndexes() && (referenceMethod.hasUniqueIndex() || referenceMethod.isBuilderMethodForUniqueIndexes())) {
            referenceMethod.getColumns().forEach { column ->
                items.add(
                    LookupElementBuilder
                        .create(column)
                        .withIcon(referenceMethod.dbIcon()) // TODO: find column definition method and use that here
//                        .withTailText("  " + referenceMethod.name) // TODO: find column definition method and use that here
                        .withTypeText(target.tablesAndAliases.first().key)
                        .withPsiElement(referenceMethod.getColumnDefinitionReference())
                )
            }
        }
    }

    private fun shouldNotComplete(project: Project, method: MethodReference, parameters: CompletionParameters) =
        !ApplicationManager.getApplication().isReadAccessAllowed ||
            !method.isBlueprintMethod(project) ||
            parameters.isInsideRegularFunction() ||
            method.firstPsiChild !is VariableImpl
}
