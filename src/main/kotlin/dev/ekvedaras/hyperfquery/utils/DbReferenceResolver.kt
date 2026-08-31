package dev.ekvedaras.hyperfquery.utils

import com.intellij.database.model.DasColumn
import com.intellij.database.model.DasForeignKey
import com.intellij.database.model.DasIndex
import com.intellij.database.model.DasNamespace
import com.intellij.database.model.DasTable
import com.intellij.database.model.DasTableKey
import com.intellij.openapi.progress.ProgressManager
import dev.ekvedaras.hyperfquery.models.DbReferenceExpression
import dev.ekvedaras.hyperfquery.utils.DatabaseUtils.Companion.columns
import dev.ekvedaras.hyperfquery.utils.DatabaseUtils.Companion.dbDataSources
import dev.ekvedaras.hyperfquery.utils.DatabaseUtils.Companion.foreignKeys
import dev.ekvedaras.hyperfquery.utils.DatabaseUtils.Companion.indexes
import dev.ekvedaras.hyperfquery.utils.DatabaseUtils.Companion.keys
import dev.ekvedaras.hyperfquery.utils.DatabaseUtils.Companion.nameWithoutPrefix
import dev.ekvedaras.hyperfquery.utils.DatabaseUtils.Companion.schemas
import dev.ekvedaras.hyperfquery.utils.DatabaseUtils.Companion.tablesSequential

class DbReferenceResolver(private val reference: DbReferenceExpression) {
    fun resolve() {
        when (reference.type) {
            DbReferenceExpression.Companion.Type.Table ->
                ResolverForTableMethods(reference, reference.schema, reference.table).resolve()
            DbReferenceExpression.Companion.Type.Column ->
                ResolverForColumnMethods(reference, reference.schema, reference.table, reference.column).resolve()
            DbReferenceExpression.Companion.Type.Index ->
                ResolverForIndexMethods(reference, reference.index).resolve()
            DbReferenceExpression.Companion.Type.Key ->
                ResolverForKeyMethods(reference, reference.key).resolve()
            DbReferenceExpression.Companion.Type.ForeignKey ->
                ResolverForForeignKeyMethods(reference, reference.foreignKey).resolve()
        }
    }
}

private class ResolverForTableMethods(
    private val reference: DbReferenceExpression,
    private val schemas: MutableList<DasNamespace>,
    private val tables: MutableList<DasTable>
) {
    fun resolve() {
        resolveSchemes()

        ProgressManager.checkCanceled()

        when (reference.parts.size) {
            1 -> resolveTables()
            else -> resolveSchemaTables()
        }
    }

    /**
     * 'schema'
     * 'schema.table'
     */
    private fun resolveSchemes() {
        reference.project.dbDataSources().forEach { dataSource ->
            dataSource.schemas()
                .filter { it.name == reference.parts.first() }
                .forEach { schemas.add(it) }
        }
    }

    /**
     * 'table'
     */
    private fun resolveTables() {
        reference.project.dbDataSources().forEach { dataSource ->
            ProgressManager.checkCanceled()

            dataSource.tablesSequential(reference.connectionSchema, reference.connectionPrefix).forEach { table ->
                ProgressManager.checkCanceled()

                if (table.nameWithoutPrefix(reference.project, reference.connectionPrefix) == reference.parts.last()) {
                    tables.add(table)
                } else if (reference.tablesAndAliases[reference.parts.last()]?.first == table.nameWithoutPrefix(reference.project, reference.connectionPrefix)) {
                    tables.add(table)
                    reference.alias = table.nameWithoutPrefix(reference.project, reference.connectionPrefix)
                }
            }
        }
    }

    /**
     * 'schema.table'
     */
    private fun resolveSchemaTables() {
        reference.project.dbDataSources().forEach { dataSource ->
            ProgressManager.checkCanceled()

            dataSource.schemas()
                .filter { schemas.contains(it) }
                .forEach { schema ->
                    ProgressManager.checkCanceled()

                    dataSource.tablesSequential(null, reference.connectionPrefix)
                        .filter { it.dasParent?.name == schema.name }
                        .filter { it.nameWithoutPrefix(reference.project, reference.connectionPrefix) == reference.parts.last() }
                        .forEach { tables.add(it) }
                }
        }
    }
}

private class ResolverForColumnMethods(
    private val reference: DbReferenceExpression,
    private val schemas: MutableList<DasNamespace>,
    private val tables: MutableList<DasTable>,
    private val columns: MutableList<DasColumn>
) {
    fun resolve() {
        when (reference.parts.size) {
            1 -> withOnePart()
            2 -> withTwoParts()
            else -> withThreeParts()
        }
    }

    /**
     * raw SQL 写的是带前缀的真实表名: 除无前缀名外还允许精确表名匹配(jc_goods)。
     */
    private fun DasTable.matchesTablePart(part: String): Boolean =
        nameWithoutPrefix(reference.project, reference.connectionPrefix) == part ||
            (reference.isRawExpression && name == part)

    /**
     * 别名匹配;raw SQL 中的别名带表前缀(jc_a),剥掉前缀后再查声明的别名(a)。
     */
    private fun matchesAlias(aliasPart: String, tableUnprefixed: String): Boolean {
        if (reference.tablesAndAliases[aliasPart]?.first == tableUnprefixed) {
            return true
        }
        val prefix = reference.tablePrefix
        return reference.isRawExpression && prefix.isNotEmpty() && aliasPart.startsWith(prefix) &&
            reference.tablesAndAliases[aliasPart.removePrefix(prefix)]?.first == tableUnprefixed
    }

    /**
     * 'column'
     * 'table'
     * 'schema'
     * 'alias'
     */
    private fun withOnePart() {
        reference.project.dbDataSources().forEach { dataSource ->
            ProgressManager.checkCanceled()

            dataSource.schemas()
                .filter { it.name == reference.parts.first() }
                .forEach { schemas.add(it) }

            dataSource.tablesSequential(reference.connectionSchema, reference.connectionPrefix).forEach { dasTable ->
                ProgressManager.checkCanceled()

                val unprefixed = dasTable.nameWithoutPrefix(reference.project, reference.connectionPrefix)

                if (dasTable.matchesTablePart(reference.parts.first()) ||
                    matchesAlias(reference.parts.first(), unprefixed)
                ) {
                    tables.add(dasTable)
                }

                // 已有命中后只剩别名相关表的列对调用方有意义(inspection 只判存在性,
                // 引用跳转只取别名表内的列),其余表的列枚举全部跳过
                val aliasRelevant = reference.tablesAndAliases.values.any { it.first == unprefixed }
                if (columns.isEmpty() || aliasRelevant) {
                    dasTable.columns()
                        .filter { it.name == reference.parts.first() }
                        .forEach { columns.add(it) }
                }
            }
        }
    }

    /**
     * 'table.column'
     * 'schema.table'
     * 'alias.column'
     */
    private fun withTwoParts() {
        reference.project.dbDataSources().forEach { dataSource ->
            ProgressManager.checkCanceled()

            dataSource.schemas()
                .filter { it.name == reference.parts.first() }
                .forEach { schemas.add(it) }

            // 显式写了 schema 前缀时不按连接过滤(允许跨 schema 引用)
            dataSource.tablesSequential(reference.connectionSchema.takeIf { schemas.isEmpty() }, reference.connectionPrefix).forEach { table ->
                ProgressManager.checkCanceled()

                if (schemas.isEmpty() || schemas.contains(table.dasParent)) {
                    addTablesAndTheirColumns(table)
                }
            }
        }
    }

    private fun addTablesAndTheirColumns(table: DasTable) {
        val unprefixed = table.nameWithoutPrefix(reference.project, reference.connectionPrefix)

        if (table.matchesTablePart(reference.parts.first()) || table.matchesTablePart(reference.parts.last())) {
            tables.add(table)

            table.columns()
                .filter { it.name == reference.parts.last() }
                .forEach { columns.add(it) }
        } else if (schemas.isEmpty() &&
            (matchesAlias(reference.parts.first(), unprefixed) || matchesAlias(reference.parts.last(), unprefixed))
        ) {
            tables.add(table)

            table.columns()
                .filter { it.name == reference.parts.last() }
                .forEach { columns.add(it) }
        }
    }

    /**
     * schema.table.column
     */
    private fun withThreeParts() {
        reference.project.dbDataSources().forEach { dataSource ->
            ProgressManager.checkCanceled()

            dataSource.schemas()
                .filter { it.name == reference.parts.first() }
                .forEach { schemas.add(it) }

            ProgressManager.checkCanceled()

            dataSource.tablesSequential(null, reference.connectionPrefix)
                .filter { schemas.contains(it.dasParent) }
                .forEach { addTableAndItsColumns(it) }
        }
    }

    private fun addTableAndItsColumns(table: DasTable) {
        val unprefixed = table.nameWithoutPrefix(reference.project, reference.connectionPrefix)

        if (table.matchesTablePart(reference.parts[1]) || matchesAlias(reference.parts[1], unprefixed)) {
            tables.add(table)

            table.columns()
                .filter { it.name == reference.parts.last() }
                .forEach { columns.add(it) }
        }
    }
}

private class ResolverForIndexMethods(
    private val reference: DbReferenceExpression,
    private val indexes: MutableList<DasIndex>,
) {
    fun resolve() {
        reference.project.dbDataSources().forEach { dataSource ->
            ProgressManager.checkCanceled()

            dataSource.tablesSequential(null, reference.connectionPrefix).filter {
                reference.tablesAndAliases.containsKey(it.nameWithoutPrefix(reference.project, reference.connectionPrefix))
            }.filter {
                (reference.tablesAndAliases[it.nameWithoutPrefix(reference.project, reference.connectionPrefix)]?.second ?: it.dasParent?.name) == it.dasParent?.name
            }.forEach { table ->
                ProgressManager.checkCanceled()

                table.indexes()
                    .filter { it.name == reference.parts[0] }
                    .forEach { indexes.add(it) }
            }
        }
    }
}

private class ResolverForKeyMethods(
    private val reference: DbReferenceExpression,
    private val keys: MutableList<DasTableKey>,
) {
    fun resolve() {
        reference.project.dbDataSources().forEach { dataSource ->
            ProgressManager.checkCanceled()

            dataSource.tablesSequential(null, reference.connectionPrefix).filter {
                reference.tablesAndAliases.containsKey(it.nameWithoutPrefix(reference.project, reference.connectionPrefix))
            }.filter {
                (reference.tablesAndAliases[it.nameWithoutPrefix(reference.project, reference.connectionPrefix)]?.second ?: it.dasParent?.name) == it.dasParent?.name
            }.forEach { table ->
                ProgressManager.checkCanceled()

                table.keys()
                    .filter { it.name == reference.parts[0] }
                    .forEach { keys.add(it) }
            }
        }
    }
}

private class ResolverForForeignKeyMethods(
    private val reference: DbReferenceExpression,
    private val foreignKeys: MutableList<DasForeignKey>,
) {
    fun resolve() {
        reference.project.dbDataSources().forEach { dataSource ->
            ProgressManager.checkCanceled()

            dataSource.tablesSequential(null, reference.connectionPrefix).filter {
                reference.tablesAndAliases.containsKey(it.nameWithoutPrefix(reference.project, reference.connectionPrefix))
            }.filter {
                (reference.tablesAndAliases[it.nameWithoutPrefix(reference.project, reference.connectionPrefix)]?.second ?: it.dasParent?.name) == it.dasParent?.name
            }.forEach { table ->
                ProgressManager.checkCanceled()

                table.foreignKeys()
                    .filter { it.name == reference.parts[0] }
                    .forEach { foreignKeys.add(it) }
            }
        }
    }
}
