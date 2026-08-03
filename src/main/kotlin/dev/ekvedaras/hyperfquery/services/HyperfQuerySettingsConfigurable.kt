package dev.ekvedaras.hyperfquery.services

import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.openapi.project.Project
import dev.ekvedaras.hyperfquery.services.forms.HyperfQuerySettingsForm
import javax.swing.JComponent
import org.jetbrains.annotations.Nls

class HyperfQuerySettingsConfigurable(val project: Project) : SearchableConfigurable {
    var settingsForm: HyperfQuerySettingsForm? = null

    override fun createComponent(): JComponent? {
        settingsForm = settingsForm ?: HyperfQuerySettingsForm(project)
        return settingsForm?.component()
    }

    override fun isModified(): Boolean = settingsForm?.isModified ?: false

    @Throws(ConfigurationException::class)
    override fun apply() {
        val settings = HyperfQuerySettings.getInstance(project)
        settings.filterDataSources = settingsForm?.shouldFilterDataSources() ?: false
        settings.filteredDataSources = settingsForm?.filteredDataSources() ?: setOf()
        settings.tablePrefix = settingsForm?.tablePrefix()?.trim() ?: ""
    }

    override fun reset() {
        settingsForm?.loadSettings()
    }

    override fun disposeUIResources() {
        settingsForm = null
    }

    @Nls
    override fun getDisplayName() = "Hyperf Query"

    override fun getId(): String = ID

    companion object {
        const val ID = "preferences.ekvedaras.hyperfquery"
    }
}
