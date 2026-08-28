package dev.ekvedaras.hyperfquery.completion

import com.intellij.database.util.DasUtil
import dev.ekvedaras.hyperfquery.BaseTestCase

internal class ConnectionFilterCompletionTest : BaseTestCase() {
    fun testTableCompletionFilteredByConnection() {
        addDatabasesConfig()
        myFixture.configureByText(
            "test.php",
            "<?php \\Hyperf\\DbConnection\\Db::connection('goods')->table('<caret>');"
        )
        myFixture.completeBasic()

        assertCompletion("failed_jobs", "migrations")
        assertNoCompletion("users", "customers", "testProject1")
    }

    fun testColumnCompletionFilteredByConnection() {
        addDatabasesConfig()
        myFixture.configureByText(
            "test.php",
            "<?php \\Hyperf\\DbConnection\\Db::connection('goods')->table('failed_jobs')->where('<caret>');"
        )
        myFixture.completeBasic()

        assertCompletion("id", "connection", "queue", "payload", "exception", "failed_at")
        assertNoCompletion("email", "first_name", "last_name")
    }

    fun testTableCompletionFallsBackToAllForUnknownConnection() {
        addDatabasesConfig()
        myFixture.configureByText(
            "test.php",
            "<?php \\Hyperf\\DbConnection\\Db::connection('missing')->table('<caret>');"
        )
        myFixture.completeBasic()

        assertCompletion("users", "customers", "failed_jobs", "migrations")
    }

    fun testTableCompletionUsesDefaultConnectionWhenNoneSpecified() {
        addDatabasesConfig()
        myFixture.configureByText("test.php", "<?php \\Hyperf\\DbConnection\\Db::table('<caret>');")
        myFixture.completeBasic()

        assertCompletion("users", "customers")
        assertNoCompletion("failed_jobs", "migrations")
    }

    fun testTableCompletionNotFilteredWithoutConfigFile() {
        myFixture.configureByText("test.php", "<?php \\Hyperf\\DbConnection\\Db::table('<caret>');")
        myFixture.completeBasic()

        assertCompletion("users", "customers", "failed_jobs", "migrations")
    }

    fun testModelQueryColumnCompletionFilteredByConnectionProperty() {
        addDatabasesConfig()
        myFixture.configureByText(
            "test.php",
            """
            <?php
            class FailedJob extends \Hyperf\Database\Model\Model
            {
                protected ?string ${'$'}connection = 'goods';
            }

            FailedJob::query()->where('<caret>');
            """.trimIndent()
        )
        myFixture.completeBasic()

        assertCompletion("id", "connection", "queue", "payload", "exception", "failed_at")
        assertNoCompletion("email", "first_name", "last_name")
    }

    fun testModelQueryColumnCompletionUsesDefaultConnectionWithoutProperty() {
        addDatabasesConfig()
        myFixture.configureByText(
            "test.php",
            """
            <?php
            class User extends \Hyperf\Database\Model\Model
            {
            }

            User::query()->where('<caret>');
            """.trimIndent()
        )
        myFixture.completeBasic()

        assertCompletion("email", "first_name", "last_name")
        assertNoCompletion("connection", "queue", "payload")
    }

    fun testModelFillableCompletionFilteredByConnectionProperty() {
        addDatabasesConfig()
        myFixture.configureByText(
            "test.php",
            """
            <?php
            class FailedJob extends \Hyperf\Database\Model\Model
            {
                protected ?string ${'$'}connection = 'goods';
                protected ${'$'}fillable = ['<caret>'];
            }
            """.trimIndent()
        )
        myFixture.completeBasic()

        assertCompletion("id", "connection", "queue", "payload", "exception", "failed_at")
        assertNoCompletion("email", "first_name", "last_name")
    }

    fun testSchemaConnectionTableCompletionFiltered() {
        addDatabasesConfig()
        myFixture.configureByText(
            "test.php",
            "<?php \\Hyperf\\DbConnection\\Schema::connection('goods')->table('<caret>', function (\\Hyperf\\Database\\Schema\\Blueprint ${'$'}table) {});"
        )
        myFixture.completeBasic()

        assertCompletion("failed_jobs", "migrations")
        assertNoCompletion("users", "customers")
    }

    fun testTableCompletionAppliesConnectionPrefix() {
        addDatabasesConfig(
            """
            <?php
            return [
                'goods' => ['database' => 'testProject2', 'prefix' => 'failed_'],
            ];
            """.trimIndent()
        )
        myFixture.configureByText(
            "test.php",
            "<?php \\Hyperf\\DbConnection\\Db::connection('goods')->table('<caret>');"
        )
        myFixture.completeBasic()

        assertCompletion("jobs")
        assertNoCompletion("failed_jobs", "migrations", "users", "customers")
    }

    fun testColumnCompletionAppliesConnectionPrefix() {
        addDatabasesConfig(
            """
            <?php
            return [
                'goods' => ['database' => 'testProject2', 'prefix' => 'failed_'],
            ];
            """.trimIndent()
        )
        myFixture.configureByText(
            "test.php",
            "<?php \\Hyperf\\DbConnection\\Db::connection('goods')->table('jobs')->where('<caret>');"
        )
        myFixture.completeBasic()

        assertCompletion("id", "connection", "queue", "payload", "exception", "failed_at")
        assertNoCompletion("email", "first_name", "last_name")
    }

    fun testConnectionWithoutPrefixKeyFallsBackToGlobalTablePrefix() {
        useTablePrefix("failed_")
        addDatabasesConfig()
        myFixture.configureByText(
            "test.php",
            "<?php \\Hyperf\\DbConnection\\Db::connection('goods')->table('<caret>');"
        )
        myFixture.completeBasic()

        // goods 连接未配置 prefix -> 回退全局 tablePrefix
        assertCompletion("jobs")
        assertNoCompletion("failed_jobs", "migrations")
    }

    fun testConnectionEmptyPrefixOverridesGlobalTablePrefix() {
        useTablePrefix("failed_")
        addDatabasesConfig(
            """
            <?php
            return [
                'goods' => ['database' => 'testProject2', 'prefix' => ''],
            ];
            """.trimIndent()
        )
        myFixture.configureByText(
            "test.php",
            "<?php \\Hyperf\\DbConnection\\Db::connection('goods')->table('<caret>');"
        )
        myFixture.completeBasic()

        // 显式空前缀覆盖全局 tablePrefix
        assertCompletion("failed_jobs", "migrations")
        assertNoCompletion("jobs")
    }
}
