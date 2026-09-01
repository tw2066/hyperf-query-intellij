package dev.ekvedaras.hyperfquery.reference

import com.intellij.database.util.DbUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceBase
import dev.ekvedaras.hyperfquery.models.DbReferenceExpression
import dev.ekvedaras.hyperfquery.services.HyperfQuerySettings
import dev.ekvedaras.hyperfquery.utils.DatabaseUtils.Companion.nameWithoutPrefix

class IndexPsiReference(element: PsiElement) : PsiReferenceBase<PsiElement>(element) {
    override fun resolve(): PsiElement? {
        val target = DbReferenceExpression.create(element, DbReferenceExpression.Companion.Type.Index)
        val tables = target.tablesAndAliases.values.map { it.first }

        rangeInElement = target.ranges.last()

        DbUtil.getDataSources(element.project).filter {
            HyperfQuerySettings.getInstance(element.project).interestedIn(it)
        }.forEach { dataSource ->
            val dbIndex = dataSource.findElement(target.index.find {
                tables.contains(it.table?.nameWithoutPrefix(element.project, target.connectionPrefix))
            })
            if (dbIndex != null) {
                return dbIndex
            }
        }

        return null
    }
}
