package dev.ekvedaras.hyperfquery.reference

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceProvider
import com.intellij.util.ProcessingContext
import com.jetbrains.php.lang.psi.elements.MethodReference
import dev.ekvedaras.hyperfquery.utils.HyperfUtils.Companion.isBuilderMethodForForeignKeys
import dev.ekvedaras.hyperfquery.utils.HyperfUtils.Companion.isBuilderMethodForIndexes
import dev.ekvedaras.hyperfquery.utils.HyperfUtils.Companion.isBuilderMethodForKeys
import dev.ekvedaras.hyperfquery.utils.HyperfUtils.Companion.isBuilderMethodForUniqueIndexes
import dev.ekvedaras.hyperfquery.utils.HyperfUtils.Companion.isForeignKeyIn
import dev.ekvedaras.hyperfquery.utils.HyperfUtils.Companion.isIndexIn
import dev.ekvedaras.hyperfquery.utils.HyperfUtils.Companion.isInsidePhpArrayOrValue
import dev.ekvedaras.hyperfquery.utils.HyperfUtils.Companion.isInsideRegularFunction
import dev.ekvedaras.hyperfquery.utils.HyperfUtils.Companion.isInteresting
import dev.ekvedaras.hyperfquery.utils.HyperfUtils.Companion.isKeyIn
import dev.ekvedaras.hyperfquery.utils.HyperfUtils.Companion.isUniqueIndexIn
import dev.ekvedaras.hyperfquery.utils.MethodUtils
import dev.ekvedaras.hyperfquery.utils.PsiUtils.Companion.containsVariable

class IndexReferenceProvider : PsiReferenceProvider() {

    override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
        val method = MethodUtils.resolveMethodReference(element) ?: return PsiReference.EMPTY_ARRAY
        val project = method.project

        if (shouldNotInspect(project, method, element)) {
            return PsiReference.EMPTY_ARRAY
        }

        var references = arrayOf<PsiReference>()

        if (method.isBuilderMethodForIndexes()) {
            references += IndexPsiReference(element)
        } else if (method.isBuilderMethodForKeys() || method.isBuilderMethodForUniqueIndexes()) {
            references += KeyPsiReference(element)
        } else if (method.isBuilderMethodForForeignKeys()) {
            references += ForeignKeyPsiReference(element)
        }

        return references
    }

    private fun shouldNotInspect(project: Project, method: MethodReference, element: PsiElement) =
        !ApplicationManager.getApplication().isReadAccessAllowed ||
            element.containsVariable() ||
            (
                !method.isBuilderMethodForIndexes() &&
                    !method.isBuilderMethodForUniqueIndexes() &&
                    !method.isBuilderMethodForKeys() &&
                    !method.isBuilderMethodForForeignKeys()
                ) ||
            (
                !element.isIndexIn(method) &&
                    !element.isUniqueIndexIn(method) &&
                    !element.isKeyIn(method) &&
                    !element.isForeignKeyIn(method)
                ) ||
            element.isInsideRegularFunction() ||
            element.isInsidePhpArrayOrValue() ||
            !method.isInteresting(project)
}
