package dev.ekvedaras.hyperfquery.reference

import com.intellij.database.model.ObjectKind
import com.intellij.database.util.DasUtil
import com.intellij.database.util.DbImplUtil
import com.intellij.database.util.DbUtil
import com.intellij.testFramework.UsefulTestCase
import dev.ekvedaras.hyperfquery.BaseTestCase
import junit.framework.TestCase

internal class ModelPropertyReferenceTest : BaseTestCase() {
    fun testResolvesColumnReferenceInFillableProperty() {
        myFixture.configureByFile("model/modelFillablePropertyReference.php")

        val column = DasUtil.getTables(dataSource())
            .first { it.name == "users" }
            .getDasChildren(ObjectKind.COLUMN)
            .first { it.name == "email" }
        val dbColumn = DbImplUtil.findElement(DbUtil.getDataSources(project).first(), column)
            ?: return fail("Failed to resolve DB column")

        val usages = myFixture.findUsages(dbColumn)

        UsefulTestCase.assertSize(1, usages)
        TestCase.assertEquals(ColumnPsiReference::class.java, usages.first().referenceClass)
        TestCase.assertTrue(usages.first().element?.textMatches("'email'") ?: false)
    }
}
