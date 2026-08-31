package dev.ekvedaras.hyperfquery.reference

import com.intellij.database.util.DbUtil
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceBase
import dev.ekvedaras.hyperfquery.models.DbReferenceExpression
import dev.ekvedaras.hyperfquery.services.HyperfQuerySettings

class TableOrViewPsiReference(
    element: PsiElement,
    private val type: DbReferenceExpression.Companion.Type,
    private val segment: TextRange? = null,
) : PsiReferenceBase<PsiElement>(element) {
    override fun resolve(): PsiElement? {
        val target = DbReferenceExpression.create(element, type, segment)

        rangeInElement = if (target.schema.isNotEmpty() && target.ranges.size > 1) {
            target.ranges[1]
        } else {
            target.ranges.first()
        }

        DbUtil.getDataSources(element.project).filter {
            HyperfQuerySettings.getInstance(element.project).interestedIn(it)
        }.forEach { dataSource ->
            val dbTable = dataSource.findElement(target.table.firstOrNull())
            if (dbTable != null) {
                return dbTable
            }
        }

        return null
    }
}
