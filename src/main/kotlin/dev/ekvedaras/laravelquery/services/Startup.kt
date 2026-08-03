package dev.ekvedaras.laravelquery.services

import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import dev.ekvedaras.laravelquery.notifications.ConfigureSettingsNotification
import dev.ekvedaras.laravelquery.utils.ClassUtils.Companion.asPhpClass
import dev.ekvedaras.laravelquery.utils.HyperfClasses

class Startup : StartupActivity {
    override fun runActivity(project: Project) {
        val settings = HyperfQuerySettings.getInstance(project)

        if (!settings.filterDataSources && !settings.ignoreSettings && HyperfClasses.QueryBuilder.asPhpClass(project) != null) {
            ConfigureSettingsNotification().notify(
                project,
                "Hyperf query now allows you to configure which schemas to inspect and reduce the noise!"
            )
        }
    }
}
