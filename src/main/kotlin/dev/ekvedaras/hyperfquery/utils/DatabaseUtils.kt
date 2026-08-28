package dev.ekvedaras.hyperfquery.utils

import com.intellij.database.model.DasColumn
import com.intellij.database.model.DasForeignKey
import com.intellij.database.model.DasIndex
import com.intellij.database.model.DasNamespace
import com.intellij.database.model.DasTable
import com.intellij.database.model.DasTableKey
import com.intellij.database.model.ObjectKind
import com.intellij.database.psi.DbDataSource
import com.intellij.database.util.DasUtil
import com.intellij.database.util.DbUtil
import com.intellij.openapi.project.Project
import dev.ekvedaras.hyperfquery.services.HyperfQuerySettings
import dev.ekvedaras.hyperfquery.utils.DatabaseUtils.Companion.tables
import java.util.stream.Stream

private val SchemasToSkip = listOf(
    "sys", "information_schema", "mysql", "performance_schema",
)

class DatabaseUtils private constructor() {
    companion object {
        fun Project.dbDataSourcesInParallel(): Stream<out DbDataSource> =
            DbUtil.getDataSources(this).toList().parallelStream().filter {
                HyperfQuerySettings.getInstance(this).interestedIn(it)
            }

        fun Project.dbDataSources(): Stream<out DbDataSource> =
            DbUtil.getDataSources(this).toList().stream().filter {
                HyperfQuerySettings.getInstance(this).interestedIn(it)
            }

        fun DbDataSource.schemasInParallel(onlySchema: String? = null): Stream<out DasNamespace> =
            DasUtil.getSchemas(this).toList().parallelStream().filter {
                HyperfQuerySettings.getInstance(this.project).interestedIn(it, this)
            }.filter { !SchemasToSkip.contains(it.name) }
                .filter { onlySchema == null || it.name == onlySchema }

        fun DbDataSource.schemas(onlySchema: String? = null): Stream<out DasNamespace> =
            DasUtil.getSchemas(this).toList().stream().filter {
                HyperfQuerySettings.getInstance(this.project).interestedIn(it, this)
            }.filter { !SchemasToSkip.contains(it.name) }
                .filter { onlySchema == null || it.name == onlySchema }

        fun DbDataSource.tables(onlySchema: String? = null, connectionPrefix: String? = null) =
            DasUtil.getTables(this).filter {
                HyperfQuerySettings.getInstance(this.project).interestedIn(it, this)
            }.filter { !it.isSystem && !SchemasToSkip.contains(it.dasParent?.name) }.filter {
                it.isPrefixed(this.project, connectionPrefix)
            }.filter { onlySchema == null || it.dasParent?.name == onlySchema }

        fun DbDataSource.tablesInParallel(onlySchema: String? = null, connectionPrefix: String? = null): Stream<out DasTable> =
            DasUtil.getTables(this).toList().parallelStream().filter {
                HyperfQuerySettings.getInstance(this.project).interestedIn(it, this)
            }.filter {
                !it.isSystem && !SchemasToSkip.contains(it.dasParent?.name)
            }.filter { it.isPrefixed(this.project, connectionPrefix) }
                .filter { onlySchema == null || it.dasParent?.name == onlySchema }

        fun DbDataSource.tablesSequential(onlySchema: String? = null, connectionPrefix: String? = null): Stream<out DasTable> =
            DasUtil.getTables(this).toList().stream().filter {
                HyperfQuerySettings.getInstance(this.project).interestedIn(it, this)
            }.filter {
                !it.isSystem && !SchemasToSkip.contains(it.dasParent?.name)
            }.filter { it.isPrefixed(this.project, connectionPrefix) }
                .filter { onlySchema == null || it.dasParent?.name == onlySchema }

        fun DasNamespace.tablesInParallel(project: Project, connectionPrefix: String? = null): Stream<out DasTable> =
            this.getDasChildren(ObjectKind.TABLE).toList().parallelStream()
                .map { it as DasTable }
                .filter { !it.isSystem }
                .filter { it.isPrefixed(project, connectionPrefix) }

        fun DasNamespace.tables(project: Project, connectionPrefix: String? = null): Stream<out DasTable> =
            this.getDasChildren(ObjectKind.TABLE).toList().stream()
                .map { it as DasTable }
                .filter { !it.isSystem }
                .filter { it.isPrefixed(project, connectionPrefix) }

        fun DasTable.columnsInParallel(): Stream<out DasColumn> =
            this.getDasChildren(ObjectKind.COLUMN).toList().parallelStream().map { it as DasColumn }

        fun DasTable.columns(): Stream<out DasColumn> =
            this.getDasChildren(ObjectKind.COLUMN).toList().stream().map { it as DasColumn }

        fun DasTable.indexesInParallel(): Stream<out DasIndex> =
            this.getDasChildren(ObjectKind.INDEX).toList().parallelStream().map { it as DasIndex }

        fun DasTable.indexes(): Stream<out DasIndex> =
            this.getDasChildren(ObjectKind.INDEX).toList().stream().map { it as DasIndex }

        fun DasTable.keysInParallel(): Stream<out DasTableKey> =
            this.getDasChildren(ObjectKind.KEY).toList().parallelStream().map { it as DasTableKey }

        fun DasTable.keys(): Stream<out DasTableKey> =
            this.getDasChildren(ObjectKind.KEY).toList().stream().map { it as DasTableKey }

        fun DasTable.foreignKeysInParallel(): Stream<out DasForeignKey> =
            this.getDasChildren(ObjectKind.FOREIGN_KEY).toList().parallelStream().map { it as DasForeignKey }

        fun DasTable.foreignKeys(): Stream<out DasForeignKey> =
            this.getDasChildren(ObjectKind.FOREIGN_KEY).toList().stream().map { it as DasForeignKey }

        /** 连接配置的 prefix 优先于设置面板的全局 tablePrefix */
        private fun effectivePrefix(project: Project, connectionPrefix: String?): String =
            connectionPrefix ?: HyperfQuerySettings.getInstance(project).tablePrefix

        private fun DasTable.isPrefixed(project: Project, connectionPrefix: String? = null): Boolean =
            this.name.startsWith(effectivePrefix(project, connectionPrefix))

        fun DasTable.nameWithoutPrefix(project: Project, connectionPrefix: String? = null): String =
            this.name.substringAfter(effectivePrefix(project, connectionPrefix))

        fun DasColumn.tableNameWithoutPrefix(project: Project, connectionPrefix: String? = null): String =
            this.tableName.substringAfter(effectivePrefix(project, connectionPrefix))

        fun String.withoutTablePrefix(project: Project): String =
            this.substringAfter(HyperfQuerySettings.getInstance(project).tablePrefix)
    }
}
