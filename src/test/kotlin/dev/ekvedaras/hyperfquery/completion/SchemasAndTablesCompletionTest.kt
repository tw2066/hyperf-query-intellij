package dev.ekvedaras.hyperfquery.completion

import dev.ekvedaras.hyperfquery.BaseTestCase
import dev.ekvedaras.hyperfquery.utils.HyperfUtils

internal class SchemasAndTablesCompletionTest : BaseTestCase() {
    private fun completeAllFor(method: String) {
        myFixture.configureByText(
            "test.php",
            "<?php (new Hyperf\\Database\\Query\\Builder())->$method('<caret>')"
        )
        myFixture.completeBasic()
    }

    private fun completeTablesFor(schema: String, method: String) {
        myFixture.configureByText(
            "test.php",
            "<?php (new Hyperf\\Database\\Query\\Builder())->$method('$schema.<caret>')"
        )
        myFixture.completeBasic()
    }

    fun testCompletesSchemasAndTables() {
        HyperfUtils.BuilderTableMethods.forEach { method ->
            completeAllFor(method)

            if (HyperfUtils.BuilderSchemaMethods.contains(method)) {
                assertEquals(schemas.size, myFixture.lookupElementStrings?.size)
                assertCompletion(*schemas.toTypedArray())
            } else {
                assertEquals(schemasAndTables.size, myFixture.lookupElementStrings?.size)
                assertCompletion(*schemasAndTables.toTypedArray())
            }
        }
    }

    fun testItShowsOnlyTablesOfSchema() {
        schemas.forEach { schema ->
            HyperfUtils.BuilderTableMethods.forEach {
                completeTablesFor(schema, it)

                assertEquals(schemaTables[schema]?.size, myFixture.lookupElementStrings?.size)
                assertCompletion(*schemaTables[schema]?.toTypedArray() ?: return fail("Failed to get schema tables"))
            }
        }
    }

}

