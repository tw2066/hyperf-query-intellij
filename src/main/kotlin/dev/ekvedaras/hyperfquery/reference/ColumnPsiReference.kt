package dev.ekvedaras.hyperfquery.reference

import com.intellij.database.util.DbUtil
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceBase
import dev.ekvedaras.hyperfquery.models.DbReferenceExpression
import dev.ekvedaras.hyperfquery.services.HyperfQuerySettings
import dev.ekvedaras.hyperfquery.utils.DatabaseUtils.Companion.nameWithoutPrefix
import dev.ekvedaras.hyperfquery.utils.DatabaseUtils.Companion.tableNameWithoutPrefix

class ColumnPsiReference(element: PsiElement, private val segment: TextRange? = null) :
    PsiReferenceBase<PsiElement>(element) {
    override fun resolve(): PsiElement? {
        val target = DbReferenceExpression.create(element, DbReferenceExpression.Companion.Type.Column, segment)
        val tables = target.tablesAndAliases.values.map { it.first }

        rangeInElement = target.ranges.last()

        DbUtil.getDataSources(element.project).filter {
            HyperfQuerySettings.getInstance(element.project).interestedIn(it)
        }.forEach { dataSource ->
            val dbColumn = dataSource.findElement(target.column.find {
                tables.contains(it.tableNameWithoutPrefix(element.project, target.connectionPrefix))
            })
            if (dbColumn != null) {
                return dbColumn
            }
        }

        return null
    }
}
