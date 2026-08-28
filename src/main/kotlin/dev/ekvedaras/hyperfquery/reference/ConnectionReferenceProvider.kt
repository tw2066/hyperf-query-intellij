package dev.ekvedaras.hyperfquery.reference

import com.intellij.openapi.application.ApplicationManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceProvider
import com.intellij.util.ProcessingContext
import dev.ekvedaras.hyperfquery.utils.HyperfUtils.Companion.isConnectionParam
import dev.ekvedaras.hyperfquery.utils.HyperfUtils.Companion.isModelConnectionProperty

class ConnectionReferenceProvider : PsiReferenceProvider() {
    override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
        if (!ApplicationManager.getApplication().isReadAccessAllowed) {
            return PsiReference.EMPTY_ARRAY
        }

        if (!element.isConnectionParam(element.project) && !element.isModelConnectionProperty()) {
            return PsiReference.EMPTY_ARRAY
        }

        return arrayOf(ConnectionPsiReference(element))
    }
}
