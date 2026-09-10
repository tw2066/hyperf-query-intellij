package dev.ekvedaras.hyperfquery.services

import com.intellij.ide.SaveAndSyncHandler
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import dev.ekvedaras.hyperfquery.notifications.ConfigureSettingsNotification
import dev.ekvedaras.hyperfquery.utils.ClassUtils.Companion.asPhpClass
import dev.ekvedaras.hyperfquery.utils.HyperfClasses

class Startup : StartupActivity {
    override fun runActivity(project: Project) {
        val settings = HyperfQuerySettings.getInstance(project)

        if (settings.enabled &&
            !settings.filterDataSources &&
            !settings.ignoreSettings &&
            !settings.configureSettingsNotificationShown &&
            HyperfClasses.QueryBuilder.asPhpClass(project) != null
        ) {
            settings.configureSettingsNotificationShown = true
            SaveAndSyncHandler
                .getInstance()
                .scheduleSave(SaveAndSyncHandler.SaveTask(project, true))
            ConfigureSettingsNotification().notify(
                project,
                "Hyperf query now allows you to configure which schemas to inspect and reduce the noise!"
            )
        }
    }
}
