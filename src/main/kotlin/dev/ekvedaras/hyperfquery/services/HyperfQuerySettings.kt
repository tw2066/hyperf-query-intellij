package dev.ekvedaras.hyperfquery.services

import com.intellij.database.model.DasNamespace
import com.intellij.database.model.DasTable
import com.intellij.database.psi.DbDataSource
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ModificationTracker
import com.intellij.util.xmlb.XmlSerializerUtil.copyBean
import com.intellij.util.xmlb.annotations.Transient
import dev.ekvedaras.hyperfquery.models.SettingsSchema
import org.jetbrains.annotations.Nullable
import java.util.concurrent.atomic.AtomicLong

@State(name = "HyperfQuerySettings", storages = [Storage("hyperf-query-settings.xml")])
class HyperfQuerySettings : PersistentStateComponent<HyperfQuerySettings>, ModificationTracker {
    private val modificationCounter = AtomicLong(0L)

    var enabled = true
        set(value) {
            if (field != value) {
                field = value
                touch()
            }
        }
    var filterDataSources = false
        set(value) {
            if (field != value) {
                field = value
                touch()
            }
        }
    var filteredDataSources = setOf<String>()
        set(value) {
            if (field != value) {
                field = value
                touch()
            }
        }
    var ignoreSettings = false
        set(value) {
            if (field != value) {
                field = value
                touch()
            }
        }
    var configureSettingsNotificationShown = false
        set(value) {
            if (field != value) {
                field = value
                touch()
            }
        }
    var tablePrefix = ""
        set(value) {
            if (field != value) {
                field = value
                touch()
            }
        }

    /** 设置修改计数,作为 CachedValue 依赖:设置变更后使数据库引用解析缓存失效 */
    @Transient
    override fun getModificationCount(): Long = modificationCounter.get()

    /** 持久化字段变化时同时通知状态存储与 CachedValue 依赖 */
    private fun touch() {
        modificationCounter.incrementAndGet()
    }

    @Nullable
    override fun getState() = this

    override fun loadState(state: HyperfQuerySettings) {
        copyBean(state, this)
        modificationCounter.incrementAndGet()
    }

    companion object {
        fun getInstance(project: Project): HyperfQuerySettings {
            return project.service()
        }
    }

    fun interestedIn(dataSource: DbDataSource) =
        !filterDataSources || filteredDataSources.any { it.startsWith(SettingsSchema.keyFor("", dataSource.uniqueId)) }

    fun interestedIn(namespace: DasNamespace, dataSource: DbDataSource) =
        !filterDataSources || filteredDataSources.contains(SettingsSchema.keyFor(namespace, dataSource))

    fun interestedIn(table: DasTable, dataSource: DbDataSource) =
        interestedIn(table.dasParent as DasNamespace, dataSource)
}
