package dev.ekvedaras.hyperfquery

import com.intellij.codeInspection.InspectionProfileEntry
import com.intellij.database.Dbms
import com.intellij.database.dataSource.LocalDataSource
import com.intellij.database.dataSource.LocalDataSourceManager
import com.intellij.database.model.DasDataSource
import com.intellij.database.model.ObjectKind
import com.intellij.database.util.DasUtil
import com.intellij.psi.PsiFile
import com.intellij.sql.database.SqlCommonTestUtils
import com.intellij.testFramework.TestDataFile
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import dev.ekvedaras.hyperfquery.services.HyperfQuerySettings
import java.io.File
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

@Suppress("UnnecessaryAbstractClass", "Deprecation")
internal abstract class BaseTestCase : BasePlatformTestCase() {
    override fun getTestDataPath(): String = "src/test/resources"
    lateinit var db: LocalDataSource
    lateinit var schemas: List<String>
    var schemaTables = mutableMapOf<String, List<String>>()
    lateinit var schemasAndTables: List<String>

    override fun setUp() {
        super.setUp()

        myFixture.copyFileToProject("stubs.php")

        HyperfQuerySettings.getInstance(project).filterDataSources = false

        db = SqlCommonTestUtils.createDataSourceFromSql(
            project,
            Dbms.MYSQL,
            false,
            File("./src/test/resources/test-db.sql").readText(Charsets.UTF_8)
        )

        schemas = DasUtil.getSchemas(db).map { schema ->
            schemaTables[schema.name] = schema.getDasChildren(ObjectKind.TABLE).map { it.name }.toList()

            schema.name
        }.toList()

        schemasAndTables = schemaTables.values.flatten() + schemas
    }

    override fun tearDown() {
        HyperfQuerySettings.getInstance(project).tablePrefix = ""
        LocalDataSourceManager.getInstance(project).removeDataSource(db)

        super.tearDown()
    }

    protected fun dataSource(): DasDataSource = db

    /**
     * 注入 Hyperf 连接配置:默认 default -> testProject1,goods -> testProject2。
     */
    protected fun addDatabasesConfig(config: String? = null) =
        myFixture.addFileToProject(
            "config/autoload/databases.php",
            config ?: """
            <?php
            return [
                'default' => ['driver' => 'pdo', 'database' => 'testProject1'],
                'goods' => ['driver' => 'pdo', 'database' => 'testProject2'],
            ];
            """.trimIndent()
        )

    protected fun useTablePrefix(prefix: String): String {
        HyperfQuerySettings.getInstance(project).tablePrefix = prefix
        return prefix
    }

    private fun caretAfterArgs(at: Int, prefix: String = ""): String {
        var args = ""

        repeat((0 until at).count()) { args += "''," }

        args += "'$prefix<caret>'"

        return args
    }

    protected fun configureQueryBuilderMethod(from: String, prefix: String, method: String, argument: Int): PsiFile? {
        return myFixture.configureByText(
            "test.php",
            run {
                val args = caretAfterArgs(argument, prefix)
                "<?php (new Hyperf\\Database\\Query\\Builder())->from('$from')->$method($args);"
            }
        )
    }

    protected fun assertCompletion(vararg shouldContain: String) {
        val strings = myFixture.lookupElementStrings ?: return fail("Empty completion result")

        assertContainsElements(strings, shouldContain.asList())
    }

    protected fun assertNoCompletion(vararg shouldNotContain: String) {
        val strings = myFixture.lookupElementStrings ?: return

        assertDoesntContain(strings, shouldNotContain.asList())
    }

    protected fun assertInspection(@TestDataFile filePath: String, inspection: InspectionProfileEntry) {
        myFixture.enableInspections(inspection)

        // Delay is required otherwise tests randomly fail due to PSI tree changes during highlighting 🤷‍
        runBlocking { delay(500L) }

        myFixture.testHighlighting(filePath)
    }

    protected fun assertInspection(file: PsiFile, inspection: InspectionProfileEntry) {
        myFixture.enableInspections(inspection)

        // Delay is required otherwise tests randomly fail due to PSI tree changes during highlighting 🤷‍
        runBlocking { delay(500L) }

        myFixture.testHighlighting(true, false, false, file.virtualFile)
    }
}
