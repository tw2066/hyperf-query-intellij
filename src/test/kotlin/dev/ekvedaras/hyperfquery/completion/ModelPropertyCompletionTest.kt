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

    fun testDoesNotCompleteColumnsInCastsValue() {
        myFixture.configureByFile("model/modelCastsValueProperty.php")

        myFixture.completeBasic()
        assertNoCompletion(*usersColumns().toTypedArray())
    }

    fun testDoesNotCompleteColumnsOutsideModel() {
        myFixture.configureByFile("model/notModelFillableProperty.php")

        myFixture.completeBasic()
        assertNoCompletion(*usersColumns().toTypedArray())
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
