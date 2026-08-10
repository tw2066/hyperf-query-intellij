package dev.ekvedaras.hyperfquery.reference

import com.intellij.openapi.application.ApplicationManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceProvider
import com.intellij.util.ProcessingContext
import com.jetbrains.php.lang.psi.elements.impl.StringLiteralExpressionImpl
import dev.ekvedaras.hyperfquery.utils.HyperfUtils.Companion.isDbFacadeSqlBindingMethod
import dev.ekvedaras.hyperfquery.utils.MethodUtils
import dev.ekvedaras.hyperfquery.utils.isBindingsArrayKey
import dev.ekvedaras.hyperfquery.utils.sqlLiteral

class BindingsReferenceProvider : PsiReferenceProvider() {
    override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
        val literal = element as? StringLiteralExpressionImpl ?: return PsiReference.EMPTY_ARRAY
        val method = MethodUtils.resolveMethodReference(literal) ?: return PsiReference.EMPTY_ARRAY
        val project = method.project

        if (!ApplicationManager.getApplication().isReadAccessAllowed) {
            return PsiReference.EMPTY_ARRAY
        }

        if (!method.isDbFacadeSqlBindingMethod(project)) {
            return PsiReference.EMPTY_ARRAY
        }

        if (!literal.isBindingsArrayKey(method)) {
            return PsiReference.EMPTY_ARRAY
        }

        val sqlLiteral = method.sqlLiteral() ?: return PsiReference.EMPTY_ARRAY

        return arrayOf(BindingsPsiReference(literal, sqlLiteral))
    }
}
