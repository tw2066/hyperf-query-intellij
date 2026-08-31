package dev.ekvedaras.hyperfquery.inspection

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElementVisitor
import com.jetbrains.php.lang.inspections.PhpInspection
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression
import com.jetbrains.php.lang.psi.visitors.PhpElementVisitor
import dev.ekvedaras.hyperfquery.MyBundle
import dev.ekvedaras.hyperfquery.utils.DatabasesConfig.Companion.databaseConnections
import dev.ekvedaras.hyperfquery.utils.HyperfUtils.Companion.isConnectionParam
import dev.ekvedaras.hyperfquery.utils.HyperfUtils.Companion.isModelConnectionProperty
import dev.ekvedaras.hyperfquery.utils.PsiUtils.Companion.containsVariable

/**
 * 未知连接名检查:Db::connection() / Schema::connection() 第一个参数、Model $connection 属性默认值。
 * 连接名取自 config/autoload/databases.php;项目中没有该配置文件时不告警(无从判断)。
 */
class UnknownConnectionInspection : PhpInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : PhpElementVisitor() {
            override fun visitPhpStringLiteralExpression(expression: StringLiteralExpression?) {
                expression ?: return
                if (!ApplicationManager.getApplication().isReadAccessAllowed || expression.containsVariable()) {
                    return
                }

                val project = expression.project
                if (!expression.isConnectionParam(project) && !expression.isModelConnectionProperty()) {
                    return
                }

                val name = expression.contents
                if (name.isEmpty()) {
                    return
                }

                val connections = project.databaseConnections()
                if (connections.isEmpty() || connections.containsKey(name)) {
                    return
                }

                holder.registerProblem(
                    expression,
                    MyBundle.message("unknownConnectionDescription"),
                    ProblemHighlightType.WARNING,
                    TextRange.from(1, name.length)
                )
            }
        }
    }
}
