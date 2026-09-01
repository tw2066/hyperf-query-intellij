package dev.ekvedaras.hyperfquery.completion

import dev.ekvedaras.hyperfquery.BaseTestCase

internal class ConnectionCompletionTest : BaseTestCase() {
    fun testCompletesConnectionsInDbConnectionMethod() {
        addDatabasesConfig()
        myFixture.configureByText("test.php", "<?php \\Hyperf\\DbConnection\\Db::connection('<caret>');")
        myFixture.completeBasic()

        assertCompletion("default", "goods")
    }

    fun testCompletesConnectionsInSchemaConnectionMethod() {
        addDatabasesConfig()
        myFixture.configureByText("test.php", "<?php \\Hyperf\\DbConnection\\Schema::connection('<caret>');")
        myFixture.completeBasic()

        assertCompletion("default", "goods")
    }

    fun testCompletesConnectionsInModelConnectionProperty() {
        addDatabasesConfig()
        myFixture.configureByText(
            "test.php",
            """
            <?php
            class User extends \Hyperf\Database\Model\Model
            {
                protected ?string ${'$'}connection = '<caret>';
            }
            """.trimIndent()
        )
        myFixture.completeBasic()

        assertCompletion("default", "goods")
    }

    fun testDoesNotCompleteConnectionsInTableMethod() {
        addDatabasesConfig()
        myFixture.configureByText("test.php", "<?php \\Hyperf\\DbConnection\\Db::table('<caret>');")
        myFixture.completeBasic()

        assertNoCompletion("default", "goods")
    }

    fun testDoesNotCompleteConnectionsWithoutConfigFile() {
        myFixture.configureByText("test.php", "<?php \\Hyperf\\DbConnection\\Db::connection('<caret>');")
        myFixture.completeBasic()

        assertNoCompletion("default", "goods")
    }
}
