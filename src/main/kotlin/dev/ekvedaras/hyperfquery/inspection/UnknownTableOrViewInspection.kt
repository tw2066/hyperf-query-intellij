package dev.ekvedaras.hyperfquery.inspection

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElementVisitor
import com.jetbrains.php.lang.inspections.PhpInspection
import com.jetbrains.php.lang.psi.elements.MethodReference
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression
import com.jetbrains.php.lang.psi.elements.impl.ArrayIndexImpl
import com.jetbrains.php.lang.psi.visitors.PhpElementVisitor
import dev.ekvedaras.hyperfquery.MyBundle
import dev.ekvedaras.hyperfquery.models.DbReferenceExpression
import dev.ekvedaras.hyperfquery.utils.HyperfUtils.Companion.isBlueprintMethod
import dev.ekvedaras.hyperfquery.utils.HyperfUtils.Companion.isBuilderMethodForTableByName
import dev.ekvedaras.hyperfquery.utils.HyperfUtils.Companion.isDbFacadeSqlBindingMethod
import dev.ekvedaras.hyperfquery.utils.HyperfUtils.Companion.isInsideRegularFunction
import dev.ekvedaras.hyperfquery.utils.HyperfUtils.Companion.isInteresting
import dev.ekvedaras.hyperfquery.utils.HyperfUtils.Companion.isSchemaBuilderMethod
import dev.ekvedaras.hyperfquery.utils.HyperfUtils.Companion.isTableParam
import dev.ekvedaras.hyperfquery.utils.HyperfUtils.Companion.modelTablePropertyClass
import dev.ekvedaras.hyperfquery.utils.HyperfUtils.Companion.shouldCompleteOnlyColumns
import dev.ekvedaras.hyperfquery.utils.MethodUtils
import dev.ekvedaras.hyperfquery.utils.PsiUtils.Companion.containsVariable

class UnknownTableOrViewInspection : PhpInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : PhpElementVisitor() {
            override fun visitPhpStringLiteralExpression(expression: StringLiteralExpression?) {
                expression ?: return
                val method = MethodUtils.resolveMethodReference(expression)
                if (method == null) {
                    inspectModelTableProperty(expression)
                    return
                }
                val project = method.project

                if (shouldNotInspect(project, method, expression)) {
                    return
                }

                val target = DbReferenceExpression.create(expression, DbReferenceExpression.Companion.Type.Table)

                if (target.table.isEmpty()) {
                    holder.registerProblem(
                        expression,
                        MyBundle.message("unknownTableOrViewDescription"),
                        ProblemHighlightType.WARNING,
                        target.ranges.last()
                    )
                }

                if (target.parts.size > 1 && target.schema.isEmpty()) {
                    holder.registerProblem(
                        expression,
                        MyBundle.message("unknownSchemaDescription"),
                        ProblemHighlightType.WARNING,
                        target.ranges.first()
                    )
                }
            }

            /**
             * Model $table 属性默认值:与方法参数同规则检查表/schema 是否存在。
             */
            private fun inspectModelTableProperty(expression: StringLiteralExpression) {
                if (!ApplicationManager.getApplication().isReadAccessAllowed || expression.containsVariable()) {
                    return
                }
                expression.modelTablePropertyClass() ?: return

                val target = DbReferenceExpression.create(expression, DbReferenceExpression.Companion.Type.Table)

                if (target.table.isEmpty()) {
                    holder.registerProblem(
                        expression,
                        MyBundle.message("unknownTableOrViewDescription"),
                        ProblemHighlightType.WARNING,
                        target.ranges.last()
                    )
                }

                if (target.parts.size > 1 && target.schema.isEmpty()) {
                    holder.registerProblem(
                        expression,
                        MyBundle.message("unknownSchemaDescription"),
                        ProblemHighlightType.WARNING,
                        target.ranges.first()
                    )
                }
            }

            private fun shouldNotInspect(
                project: Project,
                method: MethodReference,
                expression: StringLiteralExpression
            ) =
                !ApplicationManager.getApplication().isReadAccessAllowed ||
                    method.isDbFacadeSqlBindingMethod(project) ||
                    expression.parent is ArrayIndexImpl ||
                    !method.isBuilderMethodForTableByName() ||
                    method.shouldCompleteOnlyColumns() ||
                    !expression.isTableParam() ||
                    expression.isInsideRegularFunction() ||
                    !method.isInteresting(project) ||
                    method.isSchemaBuilderMethod(project) ||
                    method.isBlueprintMethod(project)
        }
    }
}
