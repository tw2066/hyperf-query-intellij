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
    var filterDataSources = false
    var filteredDataSources = setOf<String>()
    var ignoreSettings = false
    var tablePrefix = ""

    private val modificationCounter = AtomicLong(0L)

    /** 设置修改计数,作为 CachedValue 依赖:设置变更后使数据库引用解析缓存失效 */
    @Transient
    override fun getModificationCount(): Long = modificationCounter.get()

    /** 设置被写入后调用(Configurable.apply / loadState) */
    fun touch() {
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
