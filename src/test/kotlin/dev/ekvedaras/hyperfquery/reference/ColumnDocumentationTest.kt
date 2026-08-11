package dev.ekvedaras.hyperfquery.reference

import com.intellij.database.psi.DbElement
import com.intellij.database.psi.documentation.DbDocumentationProvider
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression
import dev.ekvedaras.hyperfquery.BaseTestCase

internal class ColumnDocumentationTest : BaseTestCase() {
    private fun docAt(caret: String): String? {
        myFixture.configureByText("test.php", caret)
        val offset = myFixture.editor.caretModel.offset
        val literal = checkNotNull(
            PsiTreeUtil.findElementOfClassAtOffset(
                myFixture.file,
                offset,
                StringLiteralExpression::class.java,
                false
            )
        ) { "Caret is not inside a string literal" }
        return ColumnDocumentationProvider().generateDoc(literal, null)
    }

    fun testDocForWhereColumn() {
        val doc = checkNotNull(docAt("<?php (new \\Hyperf\\Database\\Query\\Builder())->from('testProject1.users')->where('id<caret>', 1);")) {
            "No documentation generated"
        }
        // DataGrip 原生风格文档：数据源/架构/表/列 + DDL
        assertTrue("doc should have Data Source header", doc.contains("Data Source:"))
        assertTrue("doc should have Column header", doc.contains("Column:"))
        assertTrue("doc should contain column name", doc.contains("id"))
        // from('users') 里悬停 id 必须解析到 users 表，而不是其他表的同名列
        assertTrue("doc should show the queried table", doc.contains("users"))
        assertTrue("doc should NOT show another table", !doc.contains("customers"))
    }

    fun testDocMatchesDataGripNativeHoverDocumentation() {
        myFixture.configureByText(
            "test.php",
            "<?php (new \\Hyperf\\Database\\Query\\Builder())->from('testProject1.users')->where('id<caret>', 1);"
        )
        val offset = myFixture.editor.caretModel.offset
        val literal = checkNotNull(
            PsiTreeUtil.findElementOfClassAtOffset(
                myFixture.file,
                offset,
                StringLiteralExpression::class.java,
                false
            )
        )
        val dbElement = checkNotNull(ColumnPsiReference(literal).resolve() as? DbElement)
        val expected = DbDocumentationProvider().generateHoverDoc(dbElement, literal)
        val actual = ColumnDocumentationProvider().generateDoc(literal, null)

        assertEquals("hover documentation should be rendered by DataGrip itself", expected, actual)
    }

    fun testDocForSelectArrayValue() {
        val doc = checkNotNull(docAt("<?php (new \\Hyperf\\Database\\Query\\Builder())->from('testProject1.users')->select(['email<caret>']);")) {
            "No documentation generated"
        }
        assertTrue("doc should contain column name", doc.contains("email"))
        assertTrue("doc should show the table", doc.contains("users"))
    }

    fun testDocForQualifiedColumn() {
        val doc = checkNotNull(docAt("<?php (new \\Hyperf\\Database\\Query\\Builder())->from('testProject1.users')->where('users.id<caret>', 1);")) {
            "No documentation generated"
        }
        assertTrue("doc should contain column name", doc.contains("id"))
        assertTrue("doc should show the table", doc.contains("users"))
    }

    fun testDocForModelFirstResolvesToModelTable() {
        // users 和 customers 都有 id 列；模型 User → users 表，悬停 id 必须解析到 users 而非 customers
        val doc = checkNotNull(
            docAt(
                "<?php namespace App { class User extends \\Hyperf\\Database\\Model\\Model {} }" +
                    "\\App\\User::query()->first(['id<caret>', 'email']);"
            )
        ) { "No documentation generated" }
        assertTrue("doc should contain column name", doc.contains("id"))
        assertTrue("doc should show the model's table", doc.contains("users"))
        assertTrue("doc should NOT show customers table", !doc.contains("customers"))
    }

    fun testNoDocForVariableValue() {
        myFixture.configureByText(
            "test.php",
            "<?php (new \\Hyperf\\Database\\Query\\Builder())->from('testProject1.users')->where('id', \$data<caret>['id']);"
        )
        val offset = myFixture.editor.caretModel.offset
        val literal = PsiTreeUtil.findElementOfClassAtOffset(
            myFixture.file,
            offset,
            StringLiteralExpression::class.java,
            false
        )
        // value position is not a column argument; no doc expected
        assertTrue(
            "No documentation should be generated for a where() value",
            literal == null || ColumnDocumentationProvider().generateDoc(literal, null) == null
        )
    }

    fun testNoDocForUnknownColumn() {
        val doc = docAt("<?php (new \\Hyperf\\Database\\Query\\Builder())->from('testProject1.users')->where('missing_col<caret>', 1);")
        assertNull("No documentation for an unknown column", doc)
    }

    fun testNoDocOutsideBuilderMethods() {
        val doc = docAt("<?php \$x = 'id<caret>';")
        assertNull("No documentation outside builder methods", doc)
    }
}
