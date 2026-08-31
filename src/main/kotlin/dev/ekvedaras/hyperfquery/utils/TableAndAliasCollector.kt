package dev.ekvedaras.hyperfquery.utils

import com.intellij.openapi.progress.ProgressManager
import com.intellij.psi.PsiReference
import com.intellij.psi.util.parentOfType
import com.jetbrains.php.lang.psi.elements.MethodReference
import com.jetbrains.php.lang.psi.elements.PhpClass
import com.jetbrains.php.lang.psi.elements.PhpTypedElement
import com.jetbrains.php.lang.psi.elements.Statement
import com.jetbrains.php.lang.psi.elements.impl.AssignmentExpressionImpl
import com.jetbrains.php.lang.psi.elements.impl.ClassReferenceImpl
import com.jetbrains.php.lang.psi.elements.impl.ParenthesizedExpressionImpl
import com.jetbrains.php.lang.psi.elements.impl.PhpClassImpl
import com.jetbrains.php.lang.psi.elements.impl.StringLiteralExpressionImpl
import com.jetbrains.php.lang.psi.elements.impl.VariableImpl
import com.jetbrains.rd.util.addUnique
import com.jetbrains.rd.util.lifetime.Lifetime
import dev.ekvedaras.hyperfquery.models.DbReferenceExpression
import dev.ekvedaras.hyperfquery.utils.ClassUtils.Companion.isChildOf
import dev.ekvedaras.hyperfquery.utils.DatabaseUtils.Companion.dbDataSources
import dev.ekvedaras.hyperfquery.utils.DatabaseUtils.Companion.nameWithoutPrefix
import dev.ekvedaras.hyperfquery.utils.DatabaseUtils.Companion.tables
import dev.ekvedaras.hyperfquery.utils.HyperfUtils.Companion.connectionName
import dev.ekvedaras.hyperfquery.utils.HyperfUtils.Companion.isConnectionCall
import dev.ekvedaras.hyperfquery.utils.HyperfUtils.Companion.isInteresting
import dev.ekvedaras.hyperfquery.utils.HyperfUtils.Companion.modelColumnPropertyClass
import dev.ekvedaras.hyperfquery.utils.HyperfUtils.Companion.modelTablePropertyClass
import dev.ekvedaras.hyperfquery.utils.HyperfUtils.Companion.tableName
import dev.ekvedaras.hyperfquery.utils.PsiUtils.Companion.containsAlias
import dev.ekvedaras.hyperfquery.utils.PsiUtils.Companion.statementFirstPsiChild
import dev.ekvedaras.hyperfquery.utils.PsiUtils.Companion.unquoteAndCleanup

class TableAndAliasCollector(private val reference: DbReferenceExpression) {
    private val aliasCollector = AliasCollector(reference)
    private val relationResolver = ModelRelationResolver(reference, this)
    private val schemaTableResolver = SchemaTableResolver(reference)

    fun collect() {
        val method = MethodUtils.resolveMethodReference(reference.expression)
        if (method == null) {
            // Model 属性数组($fillable/$guarded/$casts 等)没有方法调用上下文,直接注入模型表名
            reference.expression.modelColumnPropertyClass()?.let {
                resolveTableName(it)
                reference.connectionName = it.connectionName()
            }
            // Model $table 属性默认值: 字符串自身即表名,只需模型 $connection 声明做连接过滤
            reference.expression.modelTablePropertyClass()?.let {
                reference.connectionName = it.connectionName()
            }
            return
        }
        val methods = mutableListOf<MethodReference>()

        ProgressManager.checkCanceled()

        collectMethodsAcrossVariableReferences(methods, method)
        collectMethodsInCurrentTree(methods, method)

        ProgressManager.checkCanceled()

        relationResolver.resolveModelAndRelationTables(methods, method)

        ProgressManager.checkCanceled()

        // 模型语境(Model::query() 链、作用域方法等)下取模型 $connection 声明的连接
        reference.connectionName = resolveModelReference(methods)
            ?.let { (it as? PhpClass) ?: it.getClass(reference.project) }
            ?.connectionName()

        // 链上显式 connection('name') 优先于模型声明
        methods.firstOrNull { it.isConnectionCall(reference.project) }
            ?.let { (it.getParameter(0) as? StringLiteralExpressionImpl)?.contents }
            ?.let { reference.connectionName = it }

        ProgressManager.checkCanceled()

        schemaTableResolver.resolve(methods, method)

        methods
            .filter { HyperfUtils.BuilderTableMethods.contains(it.name) }
            .forEach {
                ProgressManager.checkCanceled()
                scanMethodReference(it)
            }
    }

    private fun collectMethodsAcrossVariableReferences(methods: MutableList<MethodReference>, method: MethodReference) {
        val variable = method.parentOfType<Statement>()?.firstPsiChild?.firstPsiChild
        if (variable !is VariableImpl) return

        val declaration = variable.resolve()
        if (declaration?.reference != null) {
            collectMethodsInVariableReference(declaration.reference as PsiReference, methods)
        }

        ProgressManager.checkCanceled()

        variable.references.forEach {
            ProgressManager.checkCanceled()
            collectMethodsInVariableReference(it, methods)
        }
    }

    private fun collectMethodsInVariableReference(
        variableReference: PsiReference,
        methods: MutableList<MethodReference>
    ) {
        val element = variableReference.statementFirstPsiChild() ?: return

        // $var = query()->table();
        if (element is AssignmentExpressionImpl && element.lastChild is MethodReference) {
            MethodUtils.findMethodsInTree(element.lastChild).forEach { methods.addUnique(Lifetime.Eternal, it) }
            return
        }

        ProgressManager.checkCanceled()

        // $var->where()
        if (element is MethodReference) {
            MethodUtils.findMethodsInTree(
                // $var->where(['relation' => function (Relation $relation) { $relation->where() }])
                // $var->join('table', function (JoinClause $join) { $join->on() })
                if (element.isJoinOrRelation(reference.project)) {
                    element.parent?.parentOfType<Statement>()?.parentOfType<Statement>() ?: return
                } else {
                    element.parent
                }
            ).forEach { methods.addUnique(Lifetime.Eternal, it) }
        }
    }

    private fun collectMethodsInCurrentTree(methods: MutableList<MethodReference>, method: MethodReference) {
        // $var->where(['relation' => function (Relation $relation) { $relation->where() }])
        // $var->join('table', function (JoinClause $join) { $join->on() })
        if (method.isJoinOrRelation(reference.project)) {
            val firstAttempt = method.parentOfType<Statement>()
                ?.parentOfType<Statement>()
                ?.parentOfType<Statement>()
                ?.firstPsiChild

            MethodUtils.findMethodsInTree(
                if (firstAttempt is MethodReference) {
                    firstAttempt
                } else {
                    method.parentOfType<Statement>()?.firstPsiChild // $var->relation()->create()
                }
            ).forEach { methods.addUnique(Lifetime.Eternal, it) }
        } else {
            MethodUtils.findMethodsInTree(method.firstChildOfParentStatement()).forEach {
                methods.addUnique(Lifetime.Eternal, it)
            }
        }

        ProgressManager.checkCanceled()

        // Mode::when(true, function (Builder $query) { $query->where(''); });
        if (method.isInsideModelQueryClosure(reference.project)) {
            MethodUtils.findMethodsInTree(method.getParentOfClosure()).forEach {
                methods.addUnique(Lifetime.Eternal, it)
            }
        }
    }

    fun resolveTableName(model: PhpClass) {
        val name = model.tableName()!! // This will be without prefix as expected in reference.tablesAndAliases
        reference.tablesAndAliases[name] = name to null
    }

    fun resolveModelReference(methods: MutableList<MethodReference>): PhpTypedElement? {
        if (!methods.none { it.name == "from" }) return null

        // TODO can this be improved with methods like firstPsiChild, nextPsiSibling ?
        val method = methods.find { isModelReference(it) }?.firstChild as? PhpTypedElement
            ?: methods.find { isNewModelInstance(it) }
                ?.firstChild
                ?.firstChild
                ?.nextSibling
                ?.firstChild
                ?.nextSibling
                ?.nextSibling as? PhpTypedElement
            ?: methods.find { // Inside scope method inside model
                it.isInteresting(it.project) &&
                    it.parentOfType<PhpClassImpl>()?.isChildOf(HyperfClasses.Model) ?: false
            }?.parentOfType<PhpClassImpl>() as? PhpTypedElement

        if (method is VariableImpl) {
            return method.getClass(method.project)
        }

        return method
    }

    private fun isNewModelInstance(methodReference: MethodReference): Boolean {
        val classReference = methodReference
            .firstChild
            ?.firstChild
            ?.nextSibling
            ?.firstChild
            ?.nextSibling
            ?.nextSibling as? ClassReferenceImpl

        val isModel = classReference?.getClass(reference.project)?.isChildOf(HyperfClasses.Model) == true

        return methodReference.firstChild is ParenthesizedExpressionImpl && isModel
    }

    private fun isModelReference(methodReference: MethodReference): Boolean {
        return when (methodReference.firstPsiChild) {
            is ClassReferenceImpl -> (methodReference.firstChild as ClassReferenceImpl)
                .getClass(reference.project)
                ?.isChildOf(HyperfClasses.Model) ?: false
            is VariableImpl -> (methodReference.firstChild as VariableImpl)
                .getClass(reference.project)
                ?.isChildOf(HyperfClasses.Model) ?: false
            else -> false
        }
    }

    private fun scanMethodReference(method: MethodReference) {
        if (method.getParameter(0) !is StringLiteralExpressionImpl) {
            return
        }

        var (referencedTable: String, referencedSchema: String?) = extractTableAndSchema(method)

        if (referencedTable.containsAlias()) {
            aliasCollector.extractAliasFromString(method, referencedTable, referencedSchema)
            return
        }

        if (referencedSchema == null) {
            reference.project.dbDataSources().forEach { dataSource ->
                dataSource.tables(reference.connectionSchema, reference.connectionPrefix).firstOrNull {
                    it.nameWithoutPrefix(reference.project, reference.connectionPrefix) == referencedTable
                }?.let {
                    referencedSchema = it.dasParent?.name
                }
            }
        }

        aliasCollector.collectAliasFromMethodReference(method, referencedTable, referencedSchema)
    }

    private fun extractTableAndSchema(method: MethodReference): Pair<String, String?> {
        val definition = (method.getParameter(0) as StringLiteralExpressionImpl).contents.trim()

        var referencedTable: String = definition
        var referencedSchema: String? = null

        if (!definition.contains(".")) return Pair(referencedTable, referencedSchema)

        for (part in definition.split(".").reversed()) {
            if (referencedTable == definition) {
                referencedTable = part.unquoteAndCleanup()
            } else {
                referencedSchema = part.unquoteAndCleanup()
            }
        }

        return Pair(referencedTable, referencedSchema)
    }
}
