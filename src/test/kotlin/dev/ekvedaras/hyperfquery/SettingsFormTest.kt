package dev.ekvedaras.hyperfquery

import dev.ekvedaras.hyperfquery.models.SettingsSchema
import dev.ekvedaras.hyperfquery.services.HyperfQuerySettings
import dev.ekvedaras.hyperfquery.services.HyperfQuerySettingsConfigurable
import java.awt.Component
import java.awt.Container
import javax.swing.JCheckBox
import javax.swing.JTable

internal class SettingsFormTest : BaseTestCase() {
    fun testApplyingSettingsFormEnablesFiltering() {
        val configurable = HyperfQuerySettingsConfigurable(project)
        val component = configurable.createComponent()!!

        val master = findComponent(component, JCheckBox::class.java)!!
        master.isSelected = true

        val table = findComponent(component, JTable::class.java)!!
        var ticked = 0
        for (row in 0 until table.rowCount) {
            val schema = table.getValueAt(row, 1) as SettingsSchema
            if (schema.schema.name == "testProject1") {
                table.setValueAt(true, row, 0)
                ticked++
            }
        }
        assertEquals(1, ticked)

        assertTrue(configurable.isModified)
        configurable.apply()

        val settings = HyperfQuerySettings.getInstance(project)
        assertTrue(settings.filterDataSources)
        assertEquals(
            setOf(SettingsSchema.keyFor("testProject1", dataSource().uniqueId)),
            settings.filteredDataSources
        )

        myFixture.configureByText(
            "test.php",
            "<?php (new Hyperf\\Database\\Query\\Builder())->from('<caret>')"
        )
        myFixture.completeBasic()

        assertEquals(schemaTables["testProject1"]!!.size + 1, myFixture.lookupElementStrings?.size)
        assertCompletion(*(schemaTables["testProject1"]!! + "testProject1").toTypedArray())
    }

    private fun <T> findComponent(root: Component, cls: Class<T>): T? {
        if (cls.isInstance(root)) return cls.cast(root)
        if (root is Container) {
            root.components.forEach { child ->
                findComponent(child, cls)?.let { return it }
            }
        }
        return null
    }
}
