package dev.ekvedaras.hyperfquery.completion

import dev.ekvedaras.hyperfquery.BaseTestCase

internal class DbBindingsCompletionTest : BaseTestCase() {
    fun testCompletesPlaceholdersForInlineSql() {
        myFixture.configureByText(
            "test.php",
            "<?php \\Hyperf\\DbConnection\\Db::select('select * from goods where id = :id and name = :name', ['<caret>' => 1]);"
        )
        myFixture.completeBasic()
        assertCompletion("id", "name")
        assertNoCompletion(":id", ":name", "email", "first_name", "last_name", "select", "from")
    }

    fun testCompletesColonFormsForColonPrefix() {
        myFixture.configureByText(
            "test.php",
            "<?php \\Hyperf\\DbConnection\\Db::select('select * from goods where id = :id and name = :name', [':<caret>' => 1]);"
        )
        myFixture.completeBasic()
        assertCompletion(":id", ":name")
        assertNoCompletion("id", "name", "email", "first_name")
    }

    fun testCompletesPlaceholdersForDoubleQuotedKey() {
        myFixture.configureByText(
            "test.php",
            "<?php \\Hyperf\\DbConnection\\Db::select('select * from goods where id = :id and name = :name', [\"<caret>\" => 1]);"
        )
        myFixture.completeBasic()
        assertCompletion("id", "name")
        assertNoCompletion(":id", ":name", "email", "first_name")
    }

    fun testCompletesPlaceholdersForVariableSql() {
        myFixture.configureByText(
            "test.php",
            "<?php " +
                "\$sql = 'select * from goods where id = :id and name = :name';" +
                "\\Hyperf\\DbConnection\\Db::select(\$sql, ['<caret>' => 1]);"
        )
        myFixture.completeBasic()
        assertCompletion("id", "name")
        assertNoCompletion(":id", ":name", "email", "first_name")
    }

    fun testSkipsPlaceholdersAlreadyUsedAsKeys() {
        myFixture.configureByText(
            "test.php",
            "<?php " +
                "\\Hyperf\\DbConnection\\Db::select('select * from goods where id = :id and name = :name', ['id' => 1, '<caret>' => 2]);"
        )
        myFixture.completeBasic()
        assertCompletion("name")
        assertNoCompletion("id", ":id", ":name")
    }

    fun testSkipsPlaceholdersUsedAsColonKeys() {
        myFixture.configureByText(
            "test.php",
            "<?php " +
                "\\Hyperf\\DbConnection\\Db::select('select * from goods where id = :id and name = :name', [':id' => 1, '<caret>' => 2]);"
        )
        myFixture.completeBasic()
        assertCompletion("name")
        assertNoCompletion("id", ":id", ":name")
    }

    fun testCompletesForOtherWhitelistedMethods() {
        myFixture.configureByText(
            "test.php",
            "<?php " +
                "\\Hyperf\\DbConnection\\Db::update('update goods set name = :name where id = :id', ['<caret>' => 1]);"
        )
        myFixture.completeBasic()
        assertCompletion("id", "name")
        assertNoCompletion(":id", ":name", "email", "first_name")
    }

    fun testCompletesForBareEmptyElement() {
        myFixture.configureByText(
            "test.php",
            "<?php \\Hyperf\\DbConnection\\Db::select('select * from goods where id = :id and name = :name', ['<caret>']);"
        )
        myFixture.completeBasic()
        assertCompletion("id", "name")
        assertNoCompletion(":id", ":name", "email", "first_name")
    }

    fun testNoCompletionOnBindingValueSide() {
        myFixture.configureByText(
            "test.php",
            "<?php " +
                "\\Hyperf\\DbConnection\\Db::select('select * from goods where id = :id', ['x' => '<caret>']);"
        )
        myFixture.completeBasic()
        assertEmpty(myFixture.lookupElementStrings?.toList() ?: listOf<String>())
    }

    fun testNoCompletionWhenSqlHasNoPlaceholders() {
        myFixture.configureByText(
            "test.php",
            "<?php " +
                "\\Hyperf\\DbConnection\\Db::select('select * from goods where id = 1', ['<caret>' => 2]);"
        )
        myFixture.completeBasic()
        assertEmpty(myFixture.lookupElementStrings?.toList() ?: listOf<String>())
    }

    fun testNoCompletionWhenSqlIsNotResolvable() {
        myFixture.configureByText(
            "test.php",
            "<?php " +
                "\\Hyperf\\DbConnection\\Db::select(\$unknown, ['<caret>' => 2]);"
        )
        myFixture.completeBasic()
        assertEmpty(myFixture.lookupElementStrings?.toList() ?: listOf<String>())
    }

    fun testStillCompletesColumnsOnQueryBuilderSelect() {
        myFixture.configureByText(
            "test.php",
            "<?php (new \\Hyperf\\Database\\Query\\Builder())->from('users')->select('first_name', '<caret>');"
        )
        myFixture.completeBasic()
        assertCompletion("email", "first_name", "last_name")
    }

    fun testStillCompletesColumnsOnDbFacadeTableWhere() {
        myFixture.configureByText(
            "test.php",
            "<?php \\Hyperf\\DbConnection\\Db::table('users')->where('<caret>');"
        )
        myFixture.completeBasic()
        assertCompletion("first_name", "last_name")
        assertNoCompletion(":id")
    }
}
