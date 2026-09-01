package dev.ekvedaras.hyperfquery.reference

import com.jetbrains.php.lang.psi.elements.StringLiteralExpression
import dev.ekvedaras.hyperfquery.BaseTestCase

internal class ConnectionReferenceTest : BaseTestCase() {
    fun testResolvesConnectionReferenceToConfigKey() {
        addDatabasesConfig()
        val file = myFixture.configureByText(
            "test.php",
            "<?php \\Hyperf\\DbConnection\\Db::connection('go<caret>ods');"
        )

        val reference = file.findReferenceAt(myFixture.caretOffset)
            ?: return fail("No reference at caret")
        assertEquals(ConnectionPsiReference::class.java, reference.javaClass)

        val resolved = reference.resolve() ?: return fail("Failed to resolve connection reference")
        assertTrue(resolved is StringLiteralExpression)
        assertEquals("goods", (resolved as StringLiteralExpression).contents)
        assertEquals("databases.php", resolved.containingFile.name)
    }

    fun testResolvesModelConnectionPropertyReference() {
        addDatabasesConfig()
        val file = myFixture.configureByText(
            "test.php",
            """
            <?php
            class User extends \Hyperf\Database\Model\Model
            {
                protected ?string ${'$'}connection = 'def<caret>ault';
            }
            """.trimIndent()
        )

        val reference = file.findReferenceAt(myFixture.caretOffset)
            ?: return fail("No reference at caret")
        assertEquals(ConnectionPsiReference::class.java, reference.javaClass)

        val resolved = reference.resolve() ?: return fail("Failed to resolve connection reference")
        assertTrue(resolved is StringLiteralExpression)
        assertEquals("default", (resolved as StringLiteralExpression).contents)
    }

    fun testDoesNotResolveUnknownConnection() {
        addDatabasesConfig()
        val file = myFixture.configureByText(
            "test.php",
            "<?php \\Hyperf\\DbConnection\\Db::connection('mis<caret>sing');"
        )

        val reference = file.findReferenceAt(myFixture.caretOffset)
            ?: return fail("No reference at caret")
        assertNull(reference.resolve())
    }

    fun testNoConnectionReferenceWithoutConfigFile() {
        val file = myFixture.configureByText(
            "test.php",
            "<?php \\Hyperf\\DbConnection\\Db::connection('go<caret>ods');"
        )

        val reference = file.findReferenceAt(myFixture.caretOffset)
            ?: return fail("No reference at caret")
        assertNull(reference.resolve())
    }
}
