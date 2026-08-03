package dev.ekvedaras.laravelquery.utils

import com.intellij.psi.util.parentOfType
import com.jetbrains.php.lang.psi.elements.Function
import com.jetbrains.php.lang.psi.elements.MethodReference
import dev.ekvedaras.laravelquery.models.DbReferenceExpression
import dev.ekvedaras.laravelquery.utils.HyperfUtils.Companion.isBlueprintMethod
import dev.ekvedaras.laravelquery.utils.HyperfUtils.Companion.isColumnDefinitionMethod

class SchemaTableResolver(private val reference: DbReferenceExpression) {
    fun resolve(methods: MutableList<MethodReference>, method: MethodReference) {
        if (!method.isBlueprintMethod(reference.project) && !method.isColumnDefinitionMethod(reference.project)) {
            return
        }

        methods.add(
            method.parentOfType<Function>()?.parentOfType() ?: return
        )
    }
}
