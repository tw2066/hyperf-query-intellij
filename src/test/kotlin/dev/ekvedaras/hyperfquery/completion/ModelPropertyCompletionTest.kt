package dev.ekvedaras.hyperfquery.completion

import com.intellij.database.util.DasUtil
import com.intellij.testFramework.TestDataFile
import dev.ekvedaras.hyperfquery.BaseTestCase

internal class ModelPropertyCompletionTest : BaseTestCase() {
    fun testCompletesColumnsInFillableProperty() {
        assertCompletesUsersColumns("model/modelFillableProperty.php")
    }

    fun testCompletesColumnsInGuardedProperty() {
        assertCompletesUsersColumns("model/modelGuardedProperty.php")
    }

    fun testCompletesColumnsInCastsKey() {
        assertCompletesUsersColumns("model/modelCastsKeyProperty.php")
    }

    fun testCompletesColumnsInCastsPlainValue() {
        // $casts 中尚未写成 key => 的裸字符串视为输入中的键,提示列名而非 cast 类型
        assertCompletesUsersColumns("model/modelCastsPlainValueProperty.php")

        assertNoCompletion("integer", "datetime", "decimal:")
    }

    fun testDoesNotCompleteColumnsInCastsValue() {
        myFixture.configureByFile("model/modelCastsValueProperty.php")

        myFixture.completeBasic()
        assertNoCompletion(*usersColumns().toTypedArray())
    }

    fun testCompletesCastTypesInCastsValue() {
        myFixture.configureByFile("model/modelCastsValueProperty.php")

        myFixture.completeBasic()
        assertCompletion(
            "int", "integer", "real", "float", "double", "string",
            "bool", "boolean", "object", "array", "json", "collection",
            "date", "datetime", "timestamp",
            "decimal:", "date:", "datetime:",
        )
    }

    fun testDoesNotCompleteCastTypesOutsideModel() {
        myFixture.configureByFile("model/notModelCastsValueProperty.php")

        myFixture.completeBasic()
        assertNoCompletion("integer", "datetime", "decimal:")
    }

    fun testDoesNotCompleteColumnsOutsideModel() {
        myFixture.configureByFile("model/notModelFillableProperty.php")

        myFixture.completeBasic()
        assertNoCompletion(*usersColumns().toTypedArray())
    }

    fun testCompletesTablesAndSchemasInTableProperty() {
        myFixture.configureByFile("model/modelTableProperty.php")

        myFixture.completeBasic()
        assertCompletion(*schemasAndTables.toTypedArray())
    }

    fun testDoesNotCompleteTablesInTablePropertyOutsideModel() {
        myFixture.configureByFile("model/notModelTableProperty.php")

        myFixture.completeBasic()
        assertNoCompletion(*schemasAndTables.toTypedArray())
    }

    fun testCompletesOnlyConnectionTablesInTableProperty() {
        addDatabasesConfig()
        myFixture.configureByFile("model/modelTablePropertyOnConnection.php")

        myFixture.completeBasic()
        assertCompletion("failed_jobs", "migrations", "jc_goods")
        assertNoCompletion("users", "customers")
    }

    private fun assertCompletesUsersColumns(@TestDataFile filePath: String) {
        myFixture.configureByFile(filePath)

        val usersColumns = usersColumns()
        val otherTable = DasUtil.getTables(dataSource())
            .filterNot { it.name == "users" }
            .lastOrNull() ?: return fail("Did not find any tables.")
        val otherColumns = DasUtil.getColumns(otherTable)
            .filterNot { usersColumns.contains(it.name) }
            .map { it.name }
            .toList()

        myFixture.completeBasic()
        assertCompletion(*usersColumns.toTypedArray())
        assertNoCompletion(*otherColumns.toTypedArray())
        assertNoCompletion(*schemas.toTypedArray())
    }

    private fun usersColumns(): List<String> {
        val table = DasUtil.getTables(dataSource())
            .firstOrNull { it.name == "users" }
        if (table == null) {
            fail("Did not find users table.")
            return listOf()
        }
        return DasUtil.getColumns(table).map { it.name }.toList()
    }
}
