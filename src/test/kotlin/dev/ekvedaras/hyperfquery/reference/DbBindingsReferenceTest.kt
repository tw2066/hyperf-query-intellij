package dev.ekvedaras.hyperfquery.reference

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiReference
import com.intellij.psi.util.parentOfType
import com.intellij.testFramework.UsefulTestCase
import com.jetbrains.php.lang.psi.elements.impl.StringLiteralExpressionImpl
import dev.ekvedaras.hyperfquery.BaseTestCase

internal class DbBindingsReferenceTest : BaseTestCase() {
    private fun referenceAtCaret(): PsiReference =
        myFixture.file.findReferenceAt(myFixture.caretOffset)
            ?: error("Expected binding reference at caret")

    private fun sqlLiteralElement(file: PsiFile): StringLiteralExpressionImpl? {
        val offset = file.text.indexOf("select * from goods where id = :id")
        return file.findElementAt(offset + 1)?.parentOfType<StringLiteralExpressionImpl>()
    }

    fun testResolvesPlainKeyToSqlPlaceholder() {
        val file = myFixture.configureByText(
            "test.php",
            "<?php \\Hyperf\\DbConnection\\Db::select('select * from goods where id = :id', ['<caret>id' => 2]);"
        )

        val ref = referenceAtCaret()
        UsefulTestCase.assertEquals(sqlLiteralElement(file), ref.resolve())
        UsefulTestCase.assertEquals(TextRange.from(1, 2), ref.rangeInElement)
    }

    fun testResolvesColonKeyToSqlPlaceholder() {
        val file = myFixture.configureByText(
            "test.php",
            "<?php \\Hyperf\\DbConnection\\Db::select('select * from goods where id = :id', ['<caret>:id' => 2]);"
        )

        val ref = referenceAtCaret()
        UsefulTestCase.assertEquals(sqlLiteralElement(file), ref.resolve())
        UsefulTestCase.assertEquals(TextRange.from(1, 3), ref.rangeInElement)
    }

    fun testResolvesKeyWithVariableSql() {
        val file = myFixture.configureByText(
            "test.php",
            "<?php " +
                "\$sql = 'select * from goods where id = :id';" +
                "\\Hyperf\\DbConnection\\Db::select(\$sql, ['<caret>id' => 2]);"
        )

        val ref = referenceAtCaret()
        UsefulTestCase.assertEquals(sqlLiteralElement(file), ref.resolve())
    }

    fun testResolvesToNearestAssignmentBeforeCall() {
        val file = myFixture.configureByText(
            "test.php",
            "<?php " +
                "\$sql = 'select * from goods where id = :id';" +
                "\$sql = 'select * from goods where id = :id';" +
                "\\Hyperf\\DbConnection\\Db::select(\$sql, ['<caret>id' => 2]);"
        )

        val ref = referenceAtCaret()
        val secondLiteral = file.text.indexOf(
            "select * from goods where id = :id",
            file.text.indexOf("select * from goods where id = :id") + 1
        )
        val secondElement = file.findElementAt(secondLiteral + 1)
            ?.parentOfType<StringLiteralExpressionImpl>()
        UsefulTestCase.assertEquals(secondElement, ref.resolve())
    }

    fun testDoesNotResolveUnknownKey() {
        myFixture.configureByText(
            "test.php",
            "<?php \\Hyperf\\DbConnection\\Db::select('select * from goods where id = :id', ['<caret>unknown' => 2]);"
        )

        val ref = referenceAtCaret()
        UsefulTestCase.assertNull(ref.resolve())
    }

    fun testNoBindingReferenceOnQueryBuilderSelect() {
        myFixture.configureByText(
            "test.php",
            "<?php (new \\Hyperf\\Database\\Query\\Builder())->select('id');"
        )

        val ref = myFixture.file.findReferenceAt(myFixture.caretOffset)
        UsefulTestCase.assertTrue(ref !is BindingsPsiReference)
    }
}
