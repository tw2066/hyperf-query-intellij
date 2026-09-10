package dev.ekvedaras.hyperfquery

import com.intellij.openapi.util.JDOMUtil
import com.intellij.util.xmlb.XmlSerializer
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

        val master = findComponents(component, JCheckBox::class.java).first { it.text == "Filter data sources" }
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

    fun testPluginEnabledByDefaultAndCanBeDisabled() {
        val configurable = HyperfQuerySettingsConfigurable(project)
        val component = configurable.createComponent()!!

        val enableSwitch = findComponents(component, JCheckBox::class.java).first { it.text == "Enable Hyperf Query" }
        val filterCheckbox = findComponents(component, JCheckBox::class.java).first { it.text == "Filter data sources" }

        assertTrue(enableSwitch.isSelected)
        assertFalse(configurable.isModified)

        enableSwitch.isSelected = false
        assertFalse(filterCheckbox.isEnabled)
        assertTrue(configurable.isModified)

        configurable.apply()
        assertFalse(HyperfQuerySettings.getInstance(project).enabled)
    }

    fun testDisabledPluginSkipsCompletion() {
        HyperfQuerySettings.getInstance(project).enabled = false

        myFixture.configureByText(
            "test.php",
            "<?php (new Hyperf\\Database\\Query\\Builder())->from('<caret>')"
        )
        myFixture.completeBasic()

        assertNoCompletion(*schemasAndTables.toTypedArray())
    }

    fun testSettingsFlagsSerializeRoundTrip() {
        val settings = HyperfQuerySettings()
        val initialModificationCount = settings.modificationCount
        settings.enabled = false
        settings.ignoreSettings = true
        settings.configureSettingsNotificationShown = true

        assertEquals(initialModificationCount + 3, settings.modificationCount)

        val element = XmlSerializer.serialize(settings)
        val xml = JDOMUtil.writeElement(element)

        assertTrue(xml.contains("""name="enabled" value="false""""))
        assertTrue(xml.contains("""name="ignoreSettings" value="true""""))
        assertTrue(xml.contains("""name="configureSettingsNotificationShown" value="true""""))

        val restored = HyperfQuerySettings()
        XmlSerializer.deserializeInto(restored, element)
        assertFalse(restored.enabled)
        assertTrue(restored.ignoreSettings)
        assertTrue(restored.configureSettingsNotificationShown)
    }

    private fun <T> findComponent(root: Component, cls: Class<T>): T? =
        findComponents(root, cls).firstOrNull()

    private fun <T> findComponents(root: Component, cls: Class<T>): List<T> {
        val found = mutableListOf<T>()
        if (cls.isInstance(root)) found.add(cls.cast(root))
        if (root is Container) {
            root.components.forEach { child ->
                found.addAll(findComponents(child, cls))
            }
        }
        return found
    }
}
