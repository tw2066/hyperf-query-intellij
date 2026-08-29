package dev.ekvedaras.hyperfquery.utils

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.TreeElement
import com.intellij.psi.util.parentOfType
import com.jetbrains.php.lang.psi.elements.ArrayCreationExpression
import com.jetbrains.php.lang.psi.elements.ArrayHashElement
import com.jetbrains.php.lang.psi.elements.ConcatenationExpression
import com.jetbrains.php.lang.psi.elements.Field
import com.jetbrains.php.lang.psi.elements.FunctionReference
import com.jetbrains.php.lang.psi.elements.MethodReference
import com.jetbrains.php.lang.psi.elements.PhpClass
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression
import com.jetbrains.php.lang.psi.elements.impl.ArrayHashElementImpl
import com.jetbrains.php.lang.psi.elements.impl.MethodReferenceImpl
import com.jetbrains.php.lang.psi.elements.impl.PhpClassImpl
import dev.ekvedaras.hyperfquery.utils.ClassUtils.Companion.asTableName
import dev.ekvedaras.hyperfquery.utils.ClassUtils.Companion.isChildOf
import dev.ekvedaras.hyperfquery.utils.PsiUtils.Companion.isArrayKey
import dev.ekvedaras.hyperfquery.utils.PsiUtils.Companion.isArrayValue
import dev.ekvedaras.hyperfquery.utils.PsiUtils.Companion.isArrayKey as isPhpArrayKey
import dev.ekvedaras.hyperfquery.utils.PsiUtils.Companion.isArrayValue as isPhpArrayValue
import dev.ekvedaras.hyperfquery.utils.PsiUtils.Companion.isPhpArray
import dev.ekvedaras.hyperfquery.utils.PsiUtils.Companion.unquoteAndCleanup
import org.eclipse.xtend2.lib.StringConcatenation

object HyperfClasses {
    const val QueryBuilder = "\\Hyperf\\Database\\Query\\Builder"
    const val EloquentBuilder = "\\Hyperf\\Database\\Model\\Builder"
    const val SchemaBuilder = "\\Hyperf\\Database\\Schema\\Builder"
    const val Blueprint = "\\Hyperf\\Database\\Schema\\Blueprint"
    const val JoinClause = "\\Hyperf\\Database\\Query\\JoinClause"
    const val Relation = "\\Hyperf\\Database\\Model\\Relations\\Relation"
    const val Model = "\\Hyperf\\Database\\Model\\Model"
    const val DbFacade = "\\Hyperf\\DbConnection\\Db"
    const val SchemaFacade = "\\Hyperf\\Database\\Schema\\Schema"
    const val ColumnDefinition = "\\Hyperf\\Database\\Schema\\ColumnDefinition"
    const val Connection = "\\Hyperf\\Database\\Connection"
    const val ConnectionInterface = "\\Hyperf\\Database\\ConnectionInterface"
}

@Suppress("TooManyFunctions")
class HyperfUtils private constructor() {
    companion object {
        // <editor-fold desc="\Hyperf\Database query builder classes" defaultstate="collapsed">
        @JvmStatic
        val InterestingClasses = listOf(
            HyperfClasses.QueryBuilder,
            HyperfClasses.EloquentBuilder,
            HyperfClasses.JoinClause,
            HyperfClasses.Relation,
            HyperfClasses.Model,
            HyperfClasses.DbFacade,
            HyperfClasses.SchemaBuilder,
            HyperfClasses.SchemaFacade,
            HyperfClasses.Blueprint,
            HyperfClasses.ColumnDefinition,
            HyperfClasses.Connection,
            HyperfClasses.ConnectionInterface,
        )
        // </editor-fold>

        // <editor-fold desc="\Hyperf\Database schema builder classes" defaultstate="collapsed">
        @JvmStatic
        val SchemaBuilderClasses = listOf(
            HyperfClasses.SchemaBuilder,
            HyperfClasses.SchemaFacade,
        )
        // </editor-fold>

        // <editor-fold desc="Model properties whose array values are column names" defaultstate="collapsed">
        @JvmStatic
        val ModelColumnArrayProperties = listOf(
            "fillable", "guarded", "hidden", "visible", "dates",
        )
        // </editor-fold>

        // <editor-fold desc="Model properties whose array keys are column names" defaultstate="collapsed">
        @JvmStatic
        val ModelColumnHashKeyProperties = listOf(
            "casts",
        )
        // </editor-fold>

        // <editor-fold desc="\Hyperf\Database\Model\Concerns\HasAttributes cast types ($casts values)" defaultstate="collapsed">
        @JvmStatic
        val ModelCastTypes = listOf(
            "int", "integer",
            "real", "float", "double",
            "string",
            "bool", "boolean",
            "object",
            "array", "json",
            "collection",
            "date", "datetime", "timestamp",
        )

        /** 带参数的 cast 形式:decimal:<digits>、date:<format>、datetime:<format> */
        @JvmStatic
        val ModelParameterizedCastTypes = mapOf(
            "decimal:<digits>" to "decimal:",
            "date:<format>" to "date:",
            "datetime:<format>" to "datetime:",
        )
        // </editor-fold>

        // <editor-fold desc="Query builder methods where table name should be completed" defaultstate="collapsed">
        @JvmStatic
        val BuilderTableMethods = listOf(
            "from",
            "join", "joinWhere",
            "leftJoin", "leftJoinWhere",
            "rightJoin", "rightJoinWhere",
            "crossJoin",
            "table", "hasTable", "getColumnListing",
            "hasColumn", "hasColumns", "getColumnType",
            "table", "create", "drop", "dropIfExists",
            "dropColumns", "rename", "createDatabase", "dropDatabaseIfExists",
        )
        // </editor-fold>

        // <editor-fold desc="Schema builder methods where schema name should be completed" defaultstate="collapsed">
        @JvmStatic
        val BuilderSchemaMethods = listOf(
            "createDatabase", "dropDatabaseIfExists"
        )
        // </editor-fold>

        // <editor-fold desc="Table methods param indexes where table alias is defined" defaultstate="collapsed">
        @JvmStatic
        val BuilderTableAliasParams = hashMapOf(
            "from" to 1,
            "fromSub" to 1,
            "selectSub" to 1,
            "table" to 1,
        )
        // </editor-fold>

        // <editor-fold desc="Methods and params where columns should be completed" defaultstate="collapsed">
        @JvmStatic
        val BuilderTableColumnsParams = mapOf(
            "select" to listOf(-1),
            "selectRaw" to listOf(0),
            "addSelect" to listOf(0),
            "whereRaw" to listOf(0),
            "orWhereRaw" to listOf(0),
            "havingRaw" to listOf(0),
            "orHavingRaw" to listOf(0),
            "orderByRaw" to listOf(0),
            "groupByRaw" to listOf(0),
            "raw" to listOf(0),
            "join" to listOf(1, 2, 3),
            "joinWhere" to listOf(1),
            "joinSub" to listOf(2, 3, 4),
            "leftJoin" to listOf(1, 2, 3),
            "leftJoinWhere" to listOf(1),
            "leftJoinSub" to listOf(2, 3, 4),
            "rightJoin" to listOf(1, 2, 3),
            "rightJoinWhere" to listOf(1),
            "rightJoinSub" to listOf(2, 3, 4),
            "crossJoin" to listOf(1, 2, 3),
            "where" to listOf(0),
            "whereNot" to listOf(0),
            "orWhere" to listOf(0),
            "orWhereNot" to listOf(0),
            "whereColumn" to listOf(0, 1, 2),
            "orWhereColumn" to listOf(0, 1, 2),
            "whereIn" to listOf(0),
            "orWhereIn" to listOf(0),
            "whereNotIn" to listOf(0),
            "orWhereNotIn" to listOf(0),
            "whereIntegerInRaw" to listOf(0),
            "orWhereIntegerInRaw" to listOf(0),
            "orWhereIntegerNotInRaw" to listOf(0),
            "whereNull" to listOf(0),
            "orWhereNull" to listOf(0),
            "whereNotNull" to listOf(0),
            "whereBetween" to listOf(0),
            "whereBetweenColumns" to listOf(0, 1),
            "orWhereBetween" to listOf(0),
            "orWhereBetweenColumns" to listOf(0, 1),
            "whereNotBetween" to listOf(0),
            "whereNotBetweenColumns" to listOf(0, 1),
            "orWhereNotBetween" to listOf(0),
            "orWhereNotBetweenColumns" to listOf(0, 1),
            "orWhereNotNull" to listOf(0),
            "whereDate" to listOf(0),
            "orWhereDate" to listOf(0),
            "whereTime" to listOf(0),
            "orWhereTime" to listOf(0),
            "whereDay" to listOf(0),
            "orWhereDay" to listOf(0),
            "whereMonth" to listOf(0),
            "orWhereMonth" to listOf(0),
            "whereYear" to listOf(0),
            "orWhereYear" to listOf(0),
            "whereRowValues" to listOf(0),
            "orWhereRowValues" to listOf(0),
            "whereJsonContains" to listOf(0),
            "orWhereJsonContains" to listOf(0),
            "whereJsonDoesntContain" to listOf(0),
            "orWhereJsonDoesntContain" to listOf(0),
            "whereJsonLength" to listOf(0),
            "orWhereJsonLength" to listOf(0),
            "groupBy" to listOf(-1),
            "having" to listOf(0),
            "orHaving" to listOf(0),
            "havingBetween" to listOf(0),
            "orderBy" to listOf(0),
            "orderByDesc" to listOf(0),
            "latest" to listOf(0),
            "oldest" to listOf(0),
            "forPageBeforeId" to listOf(2),
            "forPageAfterId" to listOf(2),
            "reorder" to listOf(0),
            "find" to listOf(1),
            "value" to listOf(0),
            "get" to listOf(-1),
            "first" to listOf(-1),
            "paginate" to listOf(1),
            "simplePaginate" to listOf(1),
            "getCountForPagination" to listOf(0),
            "pluck" to listOf(0, 1),
            "implode" to listOf(0),
            "count" to listOf(0),
            "min" to listOf(0),
            "max" to listOf(0),
            "sum" to listOf(0),
            "avg" to listOf(0),
            "average" to listOf(0),
            "aggregate" to listOf(1),
            "numericAggregate" to listOf(1),
            "insertUsing" to listOf(1),
            "insertUsing" to listOf(1),
            "increment" to listOf(0),
            "decrement" to listOf(0),
            "updateOrInsert" to listOf(0, 1),
            "update" to listOf(0),
            "on" to listOf(0, 1, 2),
            "hasColumn" to listOf(1),
            "hasColumns" to listOf(1),
            "getColumnType" to listOf(1),
            "dropColumn" to listOf(0),
            "dropColumns" to listOf(1),
            "dropConstrainedForeignId" to listOf(0),
            "renameColumn" to listOf(0),
            "dropSoftDeletes" to listOf(0),
            "dropSoftDeletesTz" to listOf(0),
            "unique" to listOf(0),
            "index" to listOf(0),
            "spatialIndex" to listOf(0),
            "foreign" to listOf(0),
            "indexCommand" to listOf(1),
            "createIndexName" to listOf(1),
            "after" to listOf(0),
            "removeColumn" to listOf(0),
            "primary" to listOf(0),
            "unique" to listOf(0),
            "index" to listOf(0),
            "dropIndex" to listOf(0),
            "dropUnique" to listOf(0),
            "dropPrimary" to listOf(0),
            "dropForeign" to listOf(0),
            "dropSpatialIndex" to listOf(0),
            "foreign" to listOf(0),
            "id" to listOf(0),
            "increments" to listOf(0),
            "integerIncrements" to listOf(0),
            "tinyIncrements" to listOf(0),
            "mediumIncrements" to listOf(0),
            "bigIncrements" to listOf(0),
            "char" to listOf(0),
            "string" to listOf(0),
            "text" to listOf(0),
            "mediumText" to listOf(0),
            "longText" to listOf(0),
            "integer" to listOf(0),
            "tinyInteger" to listOf(0),
            "smallInteger" to listOf(0),
            "mediumInteger" to listOf(0),
            "bigInteger" to listOf(0),
            "unsignedInteger" to listOf(0),
            "unsignedTinyInteger" to listOf(0),
            "unsignedSmallInteger" to listOf(0),
            "unsignedMediumInteger" to listOf(0),
            "unsignedBigInteger" to listOf(0),
            "foreignId" to listOf(0),
            "foreignIdFor" to listOf(1),
            "float" to listOf(0),
            "double" to listOf(0),
            "decimal" to listOf(0),
            "unsignedFloat" to listOf(0),
            "unsignedDouble" to listOf(0),
            "unsignedDecimal" to listOf(0),
            "boolean" to listOf(0),
            "enum" to listOf(0),
            "set" to listOf(0),
            "json" to listOf(0),
            "jsonb" to listOf(0),
            "date" to listOf(0),
            "dateTime" to listOf(0),
            "dateTimeTz" to listOf(0),
            "time" to listOf(0),
            "timeTz" to listOf(0),
            "timestamp" to listOf(0),
            "timestampTz" to listOf(0),
            "softDeletes" to listOf(0),
            "softDeletesTz" to listOf(0),
            "year" to listOf(0),
            "binary" to listOf(0),
            "uuid" to listOf(0),
            "foreignUuid" to listOf(0),
            "ipAddress" to listOf(0),
            "macAddress" to listOf(0),
            "geometry" to listOf(0),
            "point" to listOf(0),
            "lineString" to listOf(0),
            "polygon" to listOf(0),
            "geometryCollection" to listOf(0),
            "multiPoint" to listOf(0),
            "multiLineString" to listOf(0),
            "multiPolygon" to listOf(0),
            "multiPolygonZ" to listOf(0),
            "computed" to listOf(0),
            "create" to listOf(0),
            "update" to listOf(0),
            "fill" to listOf(0),
            "updateOrCreate" to listOf(0, 1),
            "updateOrInsert" to listOf(0, 1),
            "insert" to listOf(0),
            "insertGetId" to listOf(0),
            "insertOrIgnore" to listOf(0),
        )
        // </editor-fold>

        // <editor-fold desc="Methods whose string param is a raw SQL fragment (only simple column expressions are resolved)" defaultstate="collapsed">
        @JvmStatic
        val BuilderRawExpressionMethods = listOf(
            "selectRaw",
            "whereRaw", "orWhereRaw",
            "havingRaw", "orHavingRaw",
            "orderByRaw", "groupByRaw",
            "raw",
        )
        // </editor-fold>

        // <editor-fold desc="Methods and params where indexes should be completed" defaultstate="collapsed">
        @JvmStatic
        val BuilderTableIndexesParams = mapOf(
            "index" to listOf(1),
            "dropIndex" to listOf(0),
        )
        // </editor-fold>

        // <editor-fold desc="Methods and params where unique indexes should be completed" defaultstate="collapsed">
        @JvmStatic
        val BuilderTableUniqueIndexesParams = mapOf(
            "unique" to listOf(1),
            "dropUnique" to listOf(0),
        )
        // </editor-fold>

        // <editor-fold desc="Methods and params where keys should be completed" defaultstate="collapsed">
        @JvmStatic
        val BuilderTableKeysParams = mapOf(
            "primary" to listOf(1),
            "dropPrimary" to listOf(0),
        )
        // </editor-fold>

        // <editor-fold desc="Methods and params where foreign keys should be completed" defaultstate="collapsed">
        @JvmStatic
        val BuilderTableForeignKeysParams = mapOf(
            "foreign" to listOf(0),
            "dropForeign" to listOf(0),
        )
        // </editor-fold>

        // <editor-fold desc="Methods where params may accept columns as array values" defaultstate="collapsed">
        @JvmStatic
        private val BuilderMethodsWithTableColumnsInArrayValues = listOf(
            "get", "select", "first",
            "whereBetweenColumns", "orWhereBetweenColumns",
            "whereNotBetweenColumns", "orWhereNotBetweenColumns",
            "hasColumns", "dropColumns", "dropColumns",
            "primary", "unique", "index", "spatialIndex", "foreign",
            "dropPrimary", "dropUnique", "dropIndex", "dropSpatialIndex", "dropForeign",
            "indexCommand", "createIndexName",
        )
        // </editor-fold>

        // <editor-fold desc="Methods where only columns should be completed" defaultstate="collapsed">
        @JvmStatic
        private val MethodsWhereOnlyColumnsShouldBeCompleted = listOf(
            "create", "update", "fill", "insert", "insertGetId", "insertOrIgnore",
        )
        // </editor-fold>

        // <editor-fold desc="Methods where params only accept columns as array values" defaultstate="collapsed">
        @JvmStatic
        private val BuilderMethodsWithTableColumnsOnlyInArrayValues = listOf(
            "dropPrimary", "dropUnique", "dropIndex", "dropSpatialIndex", "dropForeign",
        )
        // </editor-fold>

        // <editor-fold desc="Methods that work with indexes" defaultstate="collapsed">
        @JvmStatic
        private val BlueprintMethodsForIndexes = listOf(
            "index", "spacialIndex",
            "dropIndex", "dropSpatialIndex",
        )
        // </editor-fold>

        // <editor-fold desc="Methods that work with indexes" defaultstate="collapsed">
        @JvmStatic
        private val BlueprintMethodsForUniqueIndexes = listOf(
            "unique", "dropUnique",
        )
        // </editor-fold>

        // <editor-fold desc="Methods that work with keys" defaultstate="collapsed">
        @JvmStatic
        private val BlueprintMethodsForKeys = listOf(
            "primary",
            "dropPrimary",
        )
        // </editor-fold>

        // <editor-fold desc="Methods that work with foreign keys" defaultstate="collapsed">
        @JvmStatic
        private val BlueprintMethodsForForeignKeys = listOf(
            "foreign",
            "dropForeign",
        )
        // </editor-fold>

        // <editor-fold desc="Possible operators" defaultstate="collapsed">
        @JvmStatic
        private val Operators = listOf<CharSequence>(
            "=", "<", ">", "<=", ">=", "<>", "!=", "<=>",
            "like", "like binary", "not like", "like",
            "&", "|", "^", "<<", ">>",
            "rlike", "not rlike", "regexp", "not regexp",
            "~", "~*", "!~", "!~*", "similar to",
            "not similar to", "not ilike", "~~*", "!~~*", "distinct from",
        )

        @JvmStatic
        private val OperatorPositions = listOf(1, 2)
        // </editor-fold>

        fun MethodReference.isInteresting(project: Project): Boolean =
            MethodUtils.resolveMethodClasses(this, project).any { clazz ->
                InterestingClasses.any {
                    clazz.isChildOf(it)
                }
            }

        fun MethodReference.isEloquentModel(project: Project): Boolean =
            MethodUtils.resolveMethodClasses(this, project).any { clazz ->
                clazz.isChildOf(HyperfClasses.Model)
            }

        // <editor-fold desc="Db facade methods with (query, bindings) signature" defaultstate="collapsed">
        @JvmStatic
        val DbFacadeSqlBindingMethods = listOf(
            "select", "selectOne", "insert", "update", "delete", "statement", "affectingStatement",
        )
        // </editor-fold>

        fun MethodReference.isDbFacadeSqlBindingMethod(project: Project): Boolean =
            DbFacadeSqlBindingMethods.contains(this.name) &&
                MethodUtils.resolveMethodClasses(this, project).any { clazz ->
                    clazz.isChildOf(HyperfClasses.DbFacade) ||
                        clazz.isChildOf(HyperfClasses.Connection) ||
                        clazz.isChildOf(HyperfClasses.ConnectionInterface)
                }

        fun MethodReference.shouldCompleteSchemas(project: Project): Boolean =
            this.shouldCompleteOnlySchemas() || !this.isSchemaBuilderMethod(project)

        fun MethodReference.isSchemaBuilderMethod(project: Project): Boolean =
            MethodUtils.resolveMethodClasses(this, project).any { clazz ->
                SchemaBuilderClasses.any {
                    clazz.isChildOf(it)
                }
            }

        fun MethodReference.shouldCompleteOnlySchemas(): Boolean =
            BuilderSchemaMethods.contains(this.name)

        fun MethodReference.shouldCompleteOnlyColumns(): Boolean =
            MethodsWhereOnlyColumnsShouldBeCompleted.contains(this.name)

        fun MethodReference.isBlueprintMethod(project: Project): Boolean =
            MethodUtils.resolveMethodClasses(this, project).any { clazz ->
                clazz.isChildOf(HyperfClasses.Blueprint)
            }

        fun MethodReference.isColumnDefinitionMethod(project: Project): Boolean =
            MethodUtils.resolveMethodClasses(this, project).any { clazz ->
                clazz.isChildOf(HyperfClasses.ColumnDefinition)
            }

        fun PhpClass.tableName(resolveFromName: Boolean = true): String? {
            val tableField = this.fields.find { it.name == "table" }

            if (ClassUtils.fieldHasDefaultValue(tableField)) {
                val defaultName = tableField?.defaultValue?.text?.unquoteAndCleanup()
                if (defaultName != null) {
                    return defaultName
                }

                if (!resolveFromName) {
                    return defaultName
                }
            }

            if (this.parent is PhpClass) {
                val defaultName = (this.parent as PhpClass).tableName(false)
                if (defaultName != null) {
                    return defaultName
                }

                if (!resolveFromName) {
                    return defaultName
                }
            }

            return this.asTableName()
        }

        /**
         * 读取模型 $connection 属性声明的连接名,沿父类上溯(基类 BaseModel 常集中声明)。
         */
        fun PhpClass.connectionName(depth: Int = 1): String? {
            val connectionField = this.fields.find { it.name == "connection" }

            if (ClassUtils.fieldHasDefaultValue(connectionField)) {
                val name = connectionField?.defaultValue?.text?.unquoteAndCleanup()
                if (!name.isNullOrEmpty() && !name.equals("null", ignoreCase = true)) {
                    return name
                }
            }

            if (depth > 20) {
                return null
            }

            return (this.superClass as? PhpClass)?.connectionName(depth + 1)
        }

        /**
         * Db::connection('...') / Schema::connection('...') 调用。
         */
        fun MethodReference.isConnectionCall(project: Project): Boolean =
            this.name == "connection" &&
                MethodUtils.resolveMethodClasses(this, project).any { clazz ->
                    clazz.isChildOf(HyperfClasses.DbFacade) ||
                        SchemaBuilderClasses.any { clazz.isChildOf(it) }
                }

        /**
         * 该元素是否是 connection() 的第一个参数。
         */
        fun PsiElement.isConnectionParam(project: Project): Boolean {
            val method = MethodUtils.resolveMethodReference(this) ?: return false
            return method.isConnectionCall(project) && this.findParamIndex() == 0
        }

        /**
         * 该元素是否是 Model 子类 $connection 属性的默认值。
         */
        fun PsiElement.isModelConnectionProperty(): Boolean {
            val literal = when (this) {
                is StringLiteralExpression -> this
                else -> this.parent as? StringLiteralExpression ?: return false
            }

            val field = literal.parent as? Field ?: return false
            if (field.name != "connection") {
                return false
            }

            val clazz = field.containingClass as? PhpClassImpl ?: return false
            return clazz.isChildOf(HyperfClasses.Model)
        }

        fun PsiElement.isInsideRelationClosure(): Boolean =
            this is ArrayHashElementImpl && this.parentOfType<MethodReferenceImpl>()?.name == "with"

        /**
         * 若该元素是 Hyperf Model 子类属性数组中的列名字符串（$fillable/$guarded 等的
         * 数组值、$casts 的数组键），返回所属模型类，否则返回 null。
         */
        fun PsiElement.modelColumnPropertyClass(): PhpClass? {
            val (array, kind) = modelPropertyArrayEntry() ?: return null
            val propertyNames = when (kind) {
                // $casts 中尚未写成 key => 的裸字符串也是输入中的键
                PropertyEntryKind.ArrayValue -> ModelColumnArrayProperties + ModelColumnHashKeyProperties
                PropertyEntryKind.HashKey -> ModelColumnHashKeyProperties
                else -> return null
            }
            return modelClassOfPropertyArray(array, propertyNames)
        }

        /**
         * 若该元素是 Hyperf Model 子类 $casts 数组的哈希值（cast 类型位置），
         * 返回所属模型类，否则返回 null。
         */
        fun PsiElement.modelCastsValueClass(): PhpClass? {
            val (array, kind) = modelPropertyArrayEntry() ?: return null
            if (kind != PropertyEntryKind.HashValue) {
                return null
            }
            return modelClassOfPropertyArray(array, ModelColumnHashKeyProperties)
        }

        private enum class PropertyEntryKind { ArrayValue, HashKey, HashValue }

        /**
         * 解析字符串所处的模型属性数组及位置类型。
         * PHP PSI 中数组值/键都有 "Array value"/"Array key" 包装元素。
         */
        private fun PsiElement.modelPropertyArrayEntry(): Pair<ArrayCreationExpression, PropertyEntryKind>? {
            val literal = when (this) {
                is StringLiteralExpression -> this
                else -> this.parent as? StringLiteralExpression ?: return null
            }

            val wrapper = literal.parent ?: return null

            return when {
                wrapper.isPhpArrayValue() && wrapper.parent is ArrayCreationExpression ->
                    (wrapper.parent as ArrayCreationExpression) to PropertyEntryKind.ArrayValue
                wrapper.isPhpArrayKey() && wrapper.parent is ArrayHashElement ->
                    (wrapper.parent.parent as? ArrayCreationExpression ?: return null) to PropertyEntryKind.HashKey
                wrapper.isPhpArrayValue() && wrapper.parent is ArrayHashElement ->
                    (wrapper.parent.parent as? ArrayCreationExpression ?: return null) to PropertyEntryKind.HashValue
                else -> null
            }
        }

        private fun modelClassOfPropertyArray(
            array: ArrayCreationExpression,
            propertyNames: List<String>
        ): PhpClass? {
            val field = array.parent as? Field ?: return null
            if (!propertyNames.contains(field.name)) {
                return null
            }

            val clazz = field.containingClass as? PhpClassImpl ?: return null
            return clazz.takeIf { it.isChildOf(HyperfClasses.Model) }
        }

        fun PhpClassImpl.isJoinOrRelation(): Boolean =
            this.isChildOf(HyperfClasses.JoinClause) || this.isChildOf(HyperfClasses.Relation)

        fun MethodReference.isBuilderMethodForTableByName(): Boolean =
            BuilderTableMethods.contains(this.name)

        fun MethodReference.isBuilderMethodForColumns(): Boolean =
            BuilderTableColumnsParams.containsKey(this.name)

        fun MethodReference.isRawExpressionMethod(): Boolean =
            BuilderRawExpressionMethods.contains(this.name)

        fun MethodReference.isBuilderMethodForIndexes(): Boolean =
            BuilderTableIndexesParams.containsKey(this.name)

        fun MethodReference.isBuilderMethodForUniqueIndexes(): Boolean =
            BuilderTableUniqueIndexesParams.containsKey(this.name)

        fun MethodReference.isBuilderMethodForKeys(): Boolean =
            BuilderTableKeysParams.containsKey(this.name)

        fun MethodReference.isBuilderMethodForForeignKeys(): Boolean =
            BuilderTableForeignKeysParams.containsKey(this.name)

        fun MethodReference.isColumnParam(position: PsiElement, allowArray: Boolean): Boolean =
            this.isColumnParam(position.findParamIndex(allowArray))

        fun MethodReference.isIndexParam(position: PsiElement): Boolean =
            this.isIndexParam(position.findParamIndex())

        fun MethodReference.isUniqueIndexParam(position: PsiElement): Boolean =
            this.isUniqueIndexParam(position.findParamIndex())

        fun MethodReference.isKeyParam(position: PsiElement): Boolean =
            this.isKeyParam(position.findParamIndex())

        fun MethodReference.isForeignKeyParam(position: PsiElement): Boolean =
            this.isForeignKeyParam(position.findParamIndex())

        fun MethodReference.hasColumnsInAllParams(): Boolean =
            this.isColumnParam(-1)

        fun MethodReference.hasIndexesInAllParams(): Boolean =
            this.isIndexParam(-1)

        fun MethodReference.hasUniqueIndexesInAllParams(): Boolean =
            this.isUniqueIndexParam(-1)

        fun MethodReference.hasKeysInAllParams(): Boolean =
            this.isKeyParam(-1)

        fun MethodReference.hasForeignKeysInAllParams(): Boolean =
            this.isForeignKeyParam(-1)

        fun MethodReference.isColumnParam(index: Int): Boolean =
            BuilderTableColumnsParams[this.name]?.contains(index) ?: false

        fun MethodReference.isIndexParam(index: Int): Boolean =
            BuilderTableIndexesParams[this.name]?.contains(index) ?: false

        fun MethodReference.isUniqueIndexParam(index: Int): Boolean =
            BuilderTableUniqueIndexesParams[this.name]?.contains(index) ?: false

        fun MethodReference.isKeyParam(index: Int): Boolean =
            BuilderTableKeysParams[this.name]?.contains(index) ?: false

        fun MethodReference.isForeignKeyParam(index: Int): Boolean =
            BuilderTableForeignKeysParams[this.name]?.contains(index) ?: false

        fun MethodReference.canHaveAliasParam(): Boolean =
            BuilderTableAliasParams.containsKey(this.name)

        fun CompletionParameters.isColumnIn(method: MethodReference, allowArray: Boolean): Boolean =
            this.position.isColumnIn(method, allowArray)

        fun PsiElement.isColumnIn(method: MethodReference, allowArray: Boolean): Boolean =
            method.isColumnParam(this, allowArray) || method.hasColumnsInAllParams()

        fun CompletionParameters.isIndexIn(method: MethodReference): Boolean =
            this.position.isIndexIn(method)

        fun PsiElement.isIndexIn(method: MethodReference): Boolean =
            method.isIndexParam(this) || method.hasIndexesInAllParams()

        fun CompletionParameters.isUniqueIndexIn(method: MethodReference): Boolean =
            this.position.isUniqueIndexIn(method)

        fun PsiElement.isUniqueIndexIn(method: MethodReference): Boolean =
            method.isUniqueIndexParam(this) || method.hasUniqueIndexesInAllParams()

        fun CompletionParameters.isKeyIn(method: MethodReference): Boolean =
            this.position.isKeyIn(method)

        fun PsiElement.isKeyIn(method: MethodReference): Boolean =
            method.isKeyParam(this) || method.hasKeysInAllParams()

        fun CompletionParameters.isForeignKeyIn(method: MethodReference): Boolean =
            this.position.isForeignKeyIn(method)

        fun PsiElement.isForeignKeyIn(method: MethodReference): Boolean =
            method.isForeignKeyParam(this) || method.hasForeignKeysInAllParams()

        fun MethodReference.canHaveColumnsInArrayValues(): Boolean =
            BuilderMethodsWithTableColumnsInArrayValues.contains(this.name)

        fun MethodReference.canOnlyHaveColumnsInArrayValues(): Boolean =
            BuilderMethodsWithTableColumnsOnlyInArrayValues.contains(this.name)

        fun MethodReference.isForIndexes(): Boolean =
            BlueprintMethodsForIndexes.contains(this.name)

        fun MethodReference.isForUniqueIndexes(): Boolean =
            BlueprintMethodsForUniqueIndexes.contains(this.name)

        fun MethodReference.isForKeys(): Boolean =
            BlueprintMethodsForKeys.contains(this.name)

        fun MethodReference.isForForeignKeys(): Boolean =
            BlueprintMethodsForForeignKeys.contains(this.name)

        fun CompletionParameters.isInsideRegularFunction(): Boolean =
            this.position.isInsideRegularFunction()

        fun PsiElement.isInsideRegularFunction(): Boolean =
            (this.parent?.parent is FunctionReference && this.parent?.parent !is MethodReference) ||
                (this.parent?.parent?.parent is FunctionReference && this.parent?.parent?.parent !is MethodReference)

        fun PsiElement.isOperatorParam(allowArray: Boolean = false): Boolean =
            OperatorPositions.contains(this.findParamIndex(allowArray)) && Operators.any {
                this.textMatches("'$it'") || this.textMatches("\"$it\"")
            }

        fun CompletionParameters.isInsidePhpArrayOrValue(): Boolean =
            this.position.isInsidePhpArrayOrValue()

        fun CompletionParameters.isArrayKey(): Boolean =
            this.position.parent?.parent?.isArrayKey() ?: false

        fun CompletionParameters.isArrayValue(): Boolean =
            this.position.parent?.parent?.isArrayValue() ?: false

        fun PsiElement.isInsidePhpArrayOrValue(): Boolean =
            if (this.parent?.parent is ConcatenationExpression) {
                this.parent.isInsidePhpArrayOrValue()
            } else {
                (this.parent?.parent?.isPhpArray() ?: false) ||
                    (this.parent?.parent?.isArrayValue() ?: false) ||
                    this.parent?.parent is ArrayHashElementImpl?
            }

        fun PsiElement.isAssocArrayValue(): Boolean =
            (
                this.parent?.parent?.prevSibling is TreeElement &&
                    (this.parent?.parent?.prevSibling as TreeElement).textMatches("=>")
                ) ||
                (
                    this.parent?.parent?.prevSibling?.prevSibling is TreeElement &&
                        (this.parent?.parent?.prevSibling?.prevSibling as TreeElement).textMatches("=>")
                    )

        fun PsiElement.selectsAllColumns(): Boolean =
            this.textContains('*')

        fun CompletionParameters.isTableParam(): Boolean =
            this.position.isTableParam()

        fun PsiElement.isTableParam(): Boolean =
            this.findParamIndex() == 0 // So far all functions accept table as the first argument
    }
}
