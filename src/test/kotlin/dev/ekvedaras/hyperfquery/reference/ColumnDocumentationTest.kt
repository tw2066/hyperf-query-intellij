package dev.ekvedaras.hyperfquery.reference

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
        assertTrue("doc should contain column name", doc.contains("id"))
        assertTrue("doc should contain type row", doc.contains("类型"))
        assertTrue("id is primary key, not nullable", doc.contains("非空: 是"))
    }

    fun testDocForSelectArrayValue() {
        val doc = checkNotNull(docAt("<?php (new \\Hyperf\\Database\\Query\\Builder())->from('testProject1.users')->select(['email<caret>']);")) {
            "No documentation generated"
        }
        assertTrue("doc should contain column name", doc.contains("email"))
        assertTrue("email is nullable", doc.contains("可空: 是"))
    }

    fun testDocForQualifiedColumn() {
        val doc = checkNotNull(docAt("<?php (new \\Hyperf\\Database\\Query\\Builder())->from('testProject1.users')->where('users.id<caret>', 1);")) {
            "No documentation generated"
        }
        assertTrue("doc should contain column name", doc.contains("id"))
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
