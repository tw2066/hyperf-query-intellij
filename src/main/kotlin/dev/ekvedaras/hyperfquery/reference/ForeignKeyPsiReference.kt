package dev.ekvedaras.hyperfquery.reference

import com.intellij.database.util.DbUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceBase
import dev.ekvedaras.hyperfquery.models.DbReferenceExpression
import dev.ekvedaras.hyperfquery.services.HyperfQuerySettings
import dev.ekvedaras.hyperfquery.utils.DatabaseUtils.Companion.nameWithoutPrefix

class ForeignKeyPsiReference(element: PsiElement) : PsiReferenceBase<PsiElement>(element) {
    override fun resolve(): PsiElement? {
        val target = DbReferenceExpression(element, DbReferenceExpression.Companion.Type.ForeignKey)
        val tables = target.tablesAndAliases.values.map { it.first }

        rangeInElement = target.ranges.last()

        DbUtil.getDataSources(element.project).filter {
            HyperfQuerySettings.getInstance(element.project).interestedIn(it)
        }.forEach { dataSource ->
            val dbForeignKey = dataSource.findElement(target.foreignKey.find {
                tables.contains(it.table?.nameWithoutPrefix(element.project))
            })
            if (dbForeignKey != null) {
                return dbForeignKey
            }
        }

        return null
    }
}
