package dev.ekvedaras.hyperfquery.reference

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceBase
import com.jetbrains.php.lang.psi.elements.impl.StringLiteralExpressionImpl
import dev.ekvedaras.hyperfquery.utils.extractPlaceholders

class BindingsPsiReference(
    literal: StringLiteralExpressionImpl,
    private val sqlLiteral: StringLiteralExpressionImpl,
) : PsiReferenceBase<StringLiteralExpressionImpl>(literal) {
    private val keyText = literal.contents.removePrefix(":")
    private val keyLength = literal.contents.length

    init {
        rangeInElement = TextRange.from(1, keyLength)
    }

    override fun resolve(): PsiElement? =
        if (keyText.isNotEmpty() && extractPlaceholders(sqlLiteral.contents).contains(keyText)) {
            sqlLiteral
        } else {
            null
        }
}
