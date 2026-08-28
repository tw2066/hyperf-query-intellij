package dev.ekvedaras.hyperfquery.reference

import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression
import dev.ekvedaras.hyperfquery.BaseTestCase

internal class TableDocumentationTest : BaseTestCase() {
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
        return TableDocumentationProvider().generateDoc(literal, null)
    }

    fun testDocForTable() {
        val doc = checkNotNull(docAt("<?php \\Hyperf\\DbConnection\\Db::table('use<caret>rs');")) {
            "No documentation generated"
        }
        assertTrue("doc should show the schema", doc.contains("testProject1"))
        assertTrue("doc should show the table", doc.contains("users"))
        assertFalse("doc should be summary-only, no counts", doc.contains("columns:"))
    }

    fun testDocForTableOnConnectionChain() {
        addDatabasesConfig()
        val doc = checkNotNull(
            docAt("<?php \\Hyperf\\DbConnection\\Db::connection('goods')->table('failed_<caret>jobs');")
        ) { "No documentation generated" }
        assertTrue("doc should resolve to the connection's schema", doc.contains("testProject2"))
        assertTrue("doc should show the table", doc.contains("failed_jobs"))
    }

    fun testDocForSchemaQualifiedTable() {
        val doc = checkNotNull(
            docAt("<?php \\Hyperf\\DbConnection\\Db::table('testProject2.migrati<caret>ons');")
        ) { "No documentation generated" }
        assertTrue("doc should show the schema", doc.contains("testProject2"))
        assertTrue("doc should show the table", doc.contains("migrations"))
    }

    fun testNoDocForUnknownTable() {
        val doc = docAt("<?php \\Hyperf\\DbConnection\\Db::table('missing_<caret>table');")
        assertNull("No documentation for an unknown table", doc)
    }

    fun testNoDocForColumnArgument() {
        val doc = docAt("<?php \\Hyperf\\DbConnection\\Db::table('users')->where('i<caret>d', 1);")
        assertNull("No table documentation for a column argument", doc)
    }
}
