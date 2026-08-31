package dev.ekvedaras.hyperfquery.utils

import com.intellij.openapi.project.Project
import com.jetbrains.php.PhpIndex
import com.jetbrains.php.lang.psi.elements.Field
import com.jetbrains.php.lang.psi.elements.PhpClass
import com.jetbrains.php.lang.psi.elements.impl.PhpClassAliasImpl
import com.jetbrains.php.lang.psi.elements.impl.PhpClassImpl

private fun String.pluralize(): String {
    val lower = this.lowercase()
    // Words that typically take 'es'
    if (lower.endsWith("ch") || lower.endsWith("sh") || lower.endsWith("s") || lower.endsWith("x") || lower.endsWith("z")) {
        return this + "es"
    }
    // Consonant + y -> ies
    if (lower.endsWith("y") && length > 1) {
        val beforeY = lower[lower.length - 2]
        if (beforeY !in charArrayOf('a', 'e', 'i', 'o', 'u')) {
            return this.dropLast(1) + "ies"
        }
    }
    return this + "s"
}

class ClassUtils private constructor() {
    companion object {
        @JvmStatic
        fun PhpClassImpl.isChildOf(clazz: PhpClass): Boolean {
            if (this.fqn == clazz.fqn) {
                return true
            }

            if (superClass == null) {
                return false
            }

            if (superClass is PhpClassAliasImpl) {
                val original = (superClass as PhpClassAliasImpl).original ?: return false
                return (original as PhpClassImpl).isChildOf(clazz)
            }

            return superClass != null && (superClass as PhpClassImpl).isChildOf(clazz)
        }

        @JvmStatic
        fun PhpClassImpl.isChildOf(clazz: String, depth: Int = 1): Boolean {
            // 目标类只解析一次,沿继承链比较 fqn;原先每层递归都重复查 PhpIndex
            val targetFqn = clazz.asPhpClass(project)?.fqn ?: return false

            var current: PhpClass? = this
            var level = depth
            while (current != null) {
                if (current is PhpClassAliasImpl) {
                    current = current.original as? PhpClassImpl ?: return false
                    continue
                }

                if (current.fqn == targetFqn) {
                    return true
                }

                if (level > 20) {
                    return false
                }

                current = (current as? PhpClassImpl)?.superClass
                level++
            }

            return false
        }

        @JvmStatic
        fun String.asPhpClass(project: Project): PhpClass? {
            return PhpIndex.getInstance(project).getAnyByFQN(this).firstOrNull()
        }

        @JvmStatic
        fun PhpClass.asTableName(): String {
            val table = this.name.fold(StringBuilder(this.name.length)) { acc, c ->
                if (c in 'A'..'Z') (if (acc.isNotEmpty()) acc.append('_') else acc).append(c + ('a' - 'A'))
                else acc.append(c)
            }.toString()

            val parts = table.split("_")

            if (parts.size == 1) {
                return table.pluralize()
            }

            val last = parts[parts.size - 1]
            return parts.subList(0, parts.size - 1).joinToString("_") + "_" + last.pluralize()
        }

        fun fieldHasDefaultValue(field: Field?) = field != null && field.defaultValue != null
    }
}
