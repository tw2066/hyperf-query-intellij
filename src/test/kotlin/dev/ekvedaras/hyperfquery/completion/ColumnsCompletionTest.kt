package dev.ekvedaras.hyperfquery.completion

import com.intellij.codeInsight.completion.CompletionType
import com.intellij.database.model.ObjectKind
import com.intellij.database.util.DasUtil
import dev.ekvedaras.hyperfquery.BaseTestCase
import dev.ekvedaras.hyperfquery.utils.HyperfUtils

internal class ColumnsCompletionTest : BaseTestCase() {
    private fun completeFor(
        from: String,
        prefix: String,
        method: String,
        argument: Int,
        completionType: CompletionType = CompletionType.BASIC
    ) {
        configureQueryBuilderMethod(from, prefix, method, argument)
        myFixture.complete(completionType)
    }

    fun testCompletesSchemasAndTables() {
        val table = DasUtil.getTables(dataSource())
            .filter { !it.isSystem }
            .firstOrNull() ?: return fail("Did not find any tables.")
        val columns = DasUtil.getColumns(table)

        val expected = columns.map { it.name } + // All selected table columns
            listOf(
                table.name, // Table itself
                table.dasParent?.name ?: return fail("Failed to load table schema") // Table schema
            )

        val notExpected = schemas.filter { it != table.dasParent?.name ?: it } + // All other schemas
            schemaTables.values.flatten().filter { it != table.name } // All other tables

        HyperfUtils.BuilderTableColumnsParams.entries.distinctBy { it.value }.forEach { entry ->
            entry.value.forEach { param ->
                completeFor(table.name, "", entry.key, param)

                assertCompletion(*expected.toList().toTypedArray())
                assertNoCompletion(*notExpected.toList().toTypedArray())
            }
        }
    }

    fun testCompletesSchemaTables() {
        val schema = schemas.first()
        val table = schemaTables[schema]?.first() ?: return fail("Failed to find first table")
        val expected = schemaTables[schema] ?: return fail("Failed to find schema tables")

        val notExpected = schemas.filterNot { it == schema } + // All other schemas
            schemaTables.entries.filterNot { it.key == schema }.map { it.value }
                .flatten() // Tables of other schemas

        HyperfUtils.BuilderTableColumnsParams.entries.distinctBy { it.value }.forEach { entry ->
            entry.value.forEach { param ->
                completeFor(table, "$schema.", entry.key, param)

                assertCompletion(*expected.toList().toTypedArray())
                assertNoCompletion(*notExpected.toList().toTypedArray())
            }
        }
    }

    fun testCompletesTableColumns() {
        val table = DasUtil.getTables(dataSource())
            .filterNot { it.isSystem }
            .firstOrNull() ?: return fail("Did not find any tables.")
        val columns = DasUtil.getColumns(table).map { it.name }
        val lastTable = DasUtil.getTables(dataSource())
            .filterNot { it.isSystem }
            .lastOrNull() ?: return fail("Did not find any tables.")

        val notExpected =
            schemas.filterNot { it == table.dasParent?.name } + // All other schemas
                schemaTables.entries.filterNot { it.key == table.dasParent?.name }.map { it.value }
                    .flatten() + // Tables of other schemas
                DasUtil.getColumns(lastTable)
                    .filterNot { columns.contains(it.name) }
                    .map { it.name } // Columns of other table

        HyperfUtils.BuilderTableColumnsParams.entries.distinctBy { it.value }.forEach { entry ->
            entry.value.forEach { param ->
                completeFor(table.name, "${table.name}.", entry.key, param)

                assertCompletion(*columns.toList().toTypedArray())
                assertNoCompletion(*notExpected.toList().toTypedArray())
            }
        }
    }

    fun testCompletesSchemaTableColumns() {
        val table = DasUtil.getTables(dataSource())
            .filterNot { it.isSystem }
            .firstOrNull() ?: return fail("Did not find any tables.")
        val columns = DasUtil.getColumns(table).map { it.name }
        val lastTable = DasUtil.getTables(dataSource())
            .filterNot { it.isSystem }
            .lastOrNull() ?: return fail("Did not find any tables.")

        val notExpected =
            schemas.filterNot { it == table.dasParent?.name } + // All other schemas
                schemaTables.entries.filterNot { it.key == table.dasParent?.name }.map { it.value }
                    .flatten() + // Tables of other schemas
                DasUtil.getColumns(lastTable)
                    .filterNot { columns.contains(it.name) }
                    .map { it.name } // Columns of other table

        HyperfUtils.BuilderTableColumnsParams.entries.distinctBy { it.value }.forEach { entry ->
            entry.value.forEach { param ->
                completeFor(table.name, "${table.dasParent?.name}.${table.name}.", entry.key, param)

                assertCompletion(*columns.toList().toTypedArray())
                assertNoCompletion(*notExpected.toList().toTypedArray())
            }
        }
    }

    fun testCompletesAliasColumns() {
        val table = DasUtil.getTables(dataSource())
            .filterNot { it.isSystem }
            .firstOrNull() ?: return fail("Did not find any tables.")
        val columns = DasUtil.getColumns(table).map { it.name }
        val lastTable = DasUtil.getTables(dataSource())
            .filterNot { it.isSystem }
            .lastOrNull() ?: return fail("Did not find any tables.")
        val alias = "${table.name}_alias"

        val notExpected =
            schemas.filterNot { it == table.dasParent?.name } + // All other schemas
                schemaTables.entries.filterNot { it.key == table.dasParent?.name }.map { it.value }
                    .flatten() + // Tables of other schemas
                DasUtil.getColumns(lastTable)
                    .filterNot { columns.contains(it.name) }
                    .map { it.name } // Columns of other table

        HyperfUtils.BuilderTableColumnsParams.entries.distinctBy { it.value }.forEach { entry ->
            entry.value.forEach { param ->
                completeFor("${table.name} as $alias", "$alias.", entry.key, param)

                assertCompletion(*columns.toList().toTypedArray())
                assertNoCompletion(*notExpected.toList().toTypedArray())
            }
        }
    }

    fun testCompletesColumnsAndSchemasTablesAfterSmartSearch() {
        val schema = schemas.first()
        val table = schemaTables[schema]?.first() ?: return fail("Failed to find first table")
        val expected = schemasAndTables + DasUtil.getTables(dataSource()).first { it.name == table }
            .getDasChildren(ObjectKind.COLUMN).map { it.name }

        val notExpected = listOf("failed_at")

        HyperfUtils.BuilderTableColumnsParams.entries.distinctBy { it.value }.forEach { entry ->
            entry.value.forEach { param ->
                completeFor(table, "", entry.key, param, CompletionType.SMART)

                assertCompletion(*expected.toList().toTypedArray())
                assertNoCompletion(*notExpected.toList().toTypedArray())
            }
        }
    }

    fun testCompletesColumnsInSelectRaw() {
        val table = DasUtil.getTables(dataSource())
            .filterNot { it.isSystem }
            .firstOrNull() ?: return fail("Did not find any tables.")
        val columns = DasUtil.getColumns(table).map { it.name }

        completeFor(table.name, "", "selectRaw", 0)

        assertCompletion(*columns.toList().toTypedArray())
    }

    fun testCompletesTableColumnsInSelectRaw() {
        val table = DasUtil.getTables(dataSource())
            .filterNot { it.isSystem }
            .firstOrNull() ?: return fail("Did not find any tables.")
        val columns = DasUtil.getColumns(table).map { it.name }

        completeFor(table.name, "${table.name}.", "selectRaw", 0)

        assertCompletion(*columns.toList().toTypedArray())
    }

    fun testCompletesColumnsInRawExpressionMethods() {
        val table = DasUtil.getTables(dataSource())
            .filterNot { it.isSystem }
            .firstOrNull() ?: return fail("Did not find any tables.")
        val columns = DasUtil.getColumns(table).map { it.name }

        listOf("selectRaw", "whereRaw", "orWhereRaw", "havingRaw", "orHavingRaw", "orderByRaw", "groupByRaw")
            .forEach { method ->
                completeFor(table.name, "", method, 0)
                assertCompletion(*columns.toList().toTypedArray())
            }
    }

    fun testCompletesColumnsInsideDbRaw() {
        val table = DasUtil.getTables(dataSource())
            .filterNot { it.isSystem }
            .firstOrNull() ?: return fail("Did not find any tables.")
        val columns = DasUtil.getColumns(table).map { it.name }

        myFixture.configureByText(
            "test.php",
            "<?php (new Hyperf\\Database\\Query\\Builder())->from('${table.name}')" +
                "->select(Hyperf\\DbConnection\\Db::raw('<caret>'));"
        )
        myFixture.complete(CompletionType.BASIC)

        assertCompletion(*columns.toList().toTypedArray())
    }

    fun testCompletesColumnsInSelectRawCommaSegments() {
        val table = DasUtil.getTables(dataSource())
            .filterNot { it.isSystem }
            .firstOrNull() ?: return fail("Did not find any tables.")
        val columns = DasUtil.getColumns(table).map { it.name }

        myFixture.configureByText(
            "test.php",
            "<?php (new Hyperf\\Database\\Query\\Builder())->from('${table.name}')->selectRaw('id,<caret>');"
        )
        myFixture.complete(CompletionType.BASIC)

        assertCompletion(*columns.toList().toTypedArray())
    }

    fun testCompletesColumnsInsideDbRawCommaSegments() {
        myFixture.configureByText(
            "test.php",
            "<?php (new Hyperf\\Database\\Query\\Builder())->from('testProject1.users')" +
                "->select(Hyperf\\DbConnection\\Db::raw('id,<caret>'));"
        )
        myFixture.complete(CompletionType.BASIC)

        assertCompletion("email", "first_name", "last_name")
    }

    fun testCompletesPrefixedAliasColumnsInSelectRaw() {
        addPrefixedGoodsConfig()

        myFixture.configureByText(
            "test.php",
            "<?php \\Hyperf\\DbConnection\\Db::connection('goods')->table('goods as a')->selectRaw('jc_a.<caret>');"
        )
        myFixture.complete(CompletionType.BASIC)

        assertCompletion("id", "number", "name")
        assertNoCompletion("email", "failed_jobs")
    }

    fun testCompletingSelectRawCommaSegmentDoesNotDuplicatePreviousSegments() {
        myFixture.configureByText(
            "test.php",
            "<?php (new Hyperf\\Database\\Query\\Builder())->from('testProject1.users')->selectRaw('id,<caret>')"
        )
        myFixture.completeBasic()
        myFixture.type("email")
        myFixture.finishLookup('\n')

        myFixture.checkResult(
            "<?php (new Hyperf\\Database\\Query\\Builder())->from('testProject1.users')->selectRaw('id,email')"
        )
    }

    fun testCompletingPrefixedAliasColumnInSelectRawKeepsTypedPrefix() {
        addPrefixedGoodsConfig()

        myFixture.configureByText(
            "test.php",
            "<?php \\Hyperf\\DbConnection\\Db::connection('goods')->table('goods as a')->selectRaw('jc_a.<caret>');"
        )
        myFixture.completeBasic()
        myFixture.type("nu")
        myFixture.finishLookup('\n')

        myFixture.checkResult(
            "<?php \\Hyperf\\DbConnection\\Db::connection('goods')->table('goods as a')->selectRaw('jc_a.number');"
        )
    }

    fun testDoesNotCompleteColumnsInComplexSelectRawExpression() {
        myFixture.configureByText(
            "test.php",
            "<?php (new Hyperf\\Database\\Query\\Builder())->from('testProject1.users')->selectRaw('count(<caret>)');"
        )
        myFixture.complete(CompletionType.BASIC)

        assertNoCompletion("first_name", "last_name", "deleted_at")
    }

    fun testCompletesColumnsInsideJoinClause() {
        val tables = DasUtil.getTables(dataSource()).filter {
            it.name == "users" || it.name == "customers"
        }

        val expected = tables.map { it.name } +
            (tables.first()?.getDasChildren(ObjectKind.COLUMN)?.map { it.name } ?: listOf<String>()) +
            (tables.last()?.getDasChildren(ObjectKind.COLUMN)?.map { it.name } ?: listOf<String>())

        val notExpected = listOf("failed_jobs", "migrations", "testProject2")

        myFixture.configureByFile("inspection/joinColumns.php")
        myFixture.completeBasic()

        assertCompletion(*expected.toList().toTypedArray())
        assertNoCompletion(*notExpected.toList().toTypedArray())
    }
}

