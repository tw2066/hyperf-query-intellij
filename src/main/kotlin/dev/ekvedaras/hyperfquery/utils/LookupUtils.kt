package dev.ekvedaras.hyperfquery.utils

import com.intellij.codeInsight.AutoPopupController
import com.intellij.codeInsight.completion.PrioritizedLookupElement
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.database.model.DasColumn
import com.intellij.database.model.DasForeignKey
import com.intellij.database.model.DasIndex
import com.intellij.database.model.DasNamespace
import com.intellij.database.model.DasObject
import com.intellij.database.model.DasTable
import com.intellij.database.model.DasTableKey
import com.intellij.database.psi.DbDataSource
import com.intellij.openapi.project.Project
import com.intellij.sql.symbols.DasPsiWrappingSymbol
import dev.ekvedaras.hyperfquery.completion.DeclarativeInsertHandler
import dev.ekvedaras.hyperfquery.utils.DatabaseUtils.Companion.nameWithoutPrefix
import dev.ekvedaras.hyperfquery.utils.DatabaseUtils.Companion.tableNameWithoutPrefix
import dev.ekvedaras.hyperfquery.utils.DatabaseUtils.Companion.withoutTablePrefix
import icons.DatabaseIcons

class LookupUtils private constructor() {
    companion object {
        private const val NamespacePriority = 1
        private const val TablePriority = 2
        private const val AliasPriority = 3
        private const val ColumnPriority = 4

        fun DasNamespace.buildLookup(project: Project, dataSource: DbDataSource): LookupElement =
            PrioritizedLookupElement.withGrouping(
                PrioritizedLookupElement.withPriority(
                    LookupElementBuilder
                        .create(this, this.name)
                        .withIcon(this.getIcon(project))
                        .withTypeText(dataSource.name, true)
                        .withInsertHandler(project, true),
                    NamespacePriority.toDouble()
                ),
                NamespacePriority
            )

        fun DasTable.buildLookup(
            project: Project,
            withTablePrefix: Boolean = false,
            triggerCompletion: Boolean = false,
            connectionPrefix: String? = null,
        ): LookupElement =
            PrioritizedLookupElement.withGrouping(
                PrioritizedLookupElement.withPriority(
                    LookupElementBuilder
                        .create(this, this.nameWithoutPrefix(project, connectionPrefix))
                        .withLookupString("${this.dasParent?.name}.${this.nameWithoutPrefix(project, connectionPrefix)}")
                        .withTypeText(this.dasParent?.name ?: "", true)
                        .withIcon(this.getIcon(project))
                        .withInsertHandler(
                            project,
                            triggerCompletion,
                            if (withTablePrefix) {
                                this.dasParent?.name ?: ""
                            } else {
                                ""
                            }
                        ),
                    TablePriority.toDouble()
                ),
                TablePriority
            )

        fun DasColumn.buildLookup(
            project: Project,
            withTablePrefix: Boolean = false,
            withSchemaPrefix: Boolean = false,
            alias: String? = null,
            connectionPrefix: String? = null,
            /** raw SQL 片段(selectRaw / Db::raw 等): 插入时逐字保留光标前已输入的前缀(含逗号分段/带前缀别名),不再拼表名 */
            verbatimInsertPrefix: String? = null,
        ): LookupElement {
            val prefix = if (withSchemaPrefix) {
                this.table?.dasParent?.name ?: ""
            } else {
                ""
            } + "." + if (withTablePrefix) {
                this.tableNameWithoutPrefix(project, connectionPrefix)
            } else {
                ""
            }

            return PrioritizedLookupElement.withGrouping(
                PrioritizedLookupElement.withPriority(
                    LookupElementBuilder
                        .create(this, this.name)
                        .withIcon(this.getIcon(project))
                        .withTailText(
                            "  ${this.dataType}${if (this.default != null) " = ${this.default}" else ""}",
                            true
                        )
                        .withTypeText("${this.comment ?: ""} ${this.tableNameWithoutPrefix(project, connectionPrefix)}", true)
                        .withLookupString("${alias ?: "${this.table?.dasParent?.name}.${this.tableNameWithoutPrefix(project, connectionPrefix)}"}.${this.name}")
                        .withLookupString("${this.tableNameWithoutPrefix(project, connectionPrefix)}.${this.name}")
                        .let { builder ->
                            // raw 段内已输入的前缀(可能是带表前缀的别名 jc_a.): 让匹配器能命中
                            if (verbatimInsertPrefix.isNullOrEmpty()) {
                                builder
                            } else {
                                builder.withLookupString("$verbatimInsertPrefix${this.name}")
                            }
                        }
                        .withInsertHandler(
                            project,
                            false,
                            alias ?: prefix.trim('.'),
                            verbatimInsertPrefix
                        ),
                    ColumnPriority.toDouble()
                ),
                ColumnPriority
            )
        }

        fun DasIndex.buildLookup(project: Project): LookupElement {
            return LookupElementBuilder
                .create(this, this.name)
                .withIcon(this.getIcon(project))
                .withTypeText(
                    "  ${if (this.isUnique) " unique" else ""} ${if (this.isFunctionBased) " function" else ""}",
                    true
                )
                .withTailText("${this.comment ?: ""} ${this.columnsRef.names().joinToString(", ")}", true)
                .withInsertHandler(
                    project,
                    false,
                )
        }

        fun DasTableKey.buildLookup(project: Project): LookupElement {
            return LookupElementBuilder
                .create(this, this.name)
                .withIcon(this.getIcon(project))
                .withTypeText(
                    "  ${if (this.isPrimary) " primary" else ""}",
                    true
                )
                .withTailText("${this.comment ?: ""} ${this.columnsRef.names().joinToString(", ")}", true)
                .withInsertHandler(
                    project,
                    false,
                )
        }

        fun DasForeignKey.buildLookup(project: Project): LookupElement {
            return LookupElementBuilder
                .create(this, this.name)
                .withIcon(this.getIcon(project))
                .withTypeText(
                    "  ${this.refTableName.withoutTablePrefix(project)}: ${this.columnsRef.names().joinToString(", ")}",
                    true
                )
                .withTailText(this.comment ?: "", true)
                .withInsertHandler(
                    project,
                    false,
                )
        }

        fun buildForAliasOrTable(
            project: Project,
            tableAlias: Map.Entry<String, Pair<String, String?>>,
            dataSource: DbDataSource,
            table: DasTable?
        ): LookupElement =
            PrioritizedLookupElement.withGrouping(
                PrioritizedLookupElement.withPriority(
                    LookupElementBuilder
                        .create(tableAlias.key)
                        .withIcon(table?.getIcon(project) ?: DatabaseIcons.Synonym)
                        .withTailText(
                            if (tableAlias.value.second != null) "  (${tableAlias.value.second})" else "",
                            true
                        )
                        .withTypeText(dataSource.name, true)
                        .withInsertHandler(
                            DeclarativeInsertHandler.Builder()
                                .disableOnCompletionChars(".")
                                .insertOrMove(".")
                                .triggerAutoPopup()
                                .build()
                        ),
                    AliasPriority.toDouble()
                ),
                AliasPriority
            )

        private fun DasObject.getIcon(project: Project) =
            DasPsiWrappingSymbol(this, project).getIcon(false)

        private fun LookupElementBuilder.withInsertHandler(
            project: Project,
            triggerCompletion: Boolean = false,
            prefix: String = "",
            verbatimPrefix: String? = null,
        ): LookupElementBuilder {
            val lookupPrefix = verbatimPrefix ?: if (prefix.isNotEmpty()) {
                "$prefix."
            } else {
                ""
            }

            val suffix = if (triggerCompletion) {
                "."
            } else {
                ""
            }

            return this.withInsertHandler { context, lookup ->
                context.document.deleteString(context.startOffset, context.tailOffset)
                context.document.insertString(
                    context.startOffset, "${lookupPrefix}${lookup.lookupString}$suffix"
                )
                context.editor.caretModel.moveCaretRelatively(
                    lookupPrefix.length + lookup.lookupString.length + suffix.length,
                    0,
                    false,
                    false,
                    true
                )

                if (triggerCompletion) {
                    AutoPopupController.getInstance(project).scheduleAutoPopup(context.editor)
                }
            }
        }
    }
}
