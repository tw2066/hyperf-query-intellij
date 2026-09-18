# 适配性

## 运行环境

| 项目 | 要求 |
| --- | --- |
| IDE | **PhpStorm 2022.2+**(插件兼容自 build 222 起,不设上限);或 **IntelliJ IDEA Ultimate** 并安装 PHP 插件 |
| 必需 IDE 插件 | **PHP**(`com.jetbrains.php`)、**Database Tools and SQL**(`com.intellij.database`),两者均被声明为硬依赖,缺失时插件无法加载 |
| 框架 | **Hyperf**(`Hyperf\Database\*`、`Hyperf\DbConnection\Db` 等类须可被 IDE 索引,即 vendor 正常加载) |
| 数据库 | 取决于 IDE 数据源支持(MySQL、PostgreSQL 等均可)。插件本身**不直连数据库、不读 `.env`**,一切元数据来自 IDE 数据源缓存 |
| 操作系统 | 与 IDE 一致(Windows / macOS / Linux) |

> Community 版 IDE 没有 Database Tools 插件,无法使用本插件。

## 识别的 Hyperf 类

插件通过 PSI 类型解析判断调用链是否属于以下类(含子类),只有命中才会触发补全/检查/跳转:

| 类 | 作用 |
| --- | --- |
| `Hyperf\Database\Query\Builder` | 查询构建器 |
| `Hyperf\Database\Model\Builder` | 模型构建器(`Model::query()`) |
| `Hyperf\Database\Query\JoinClause` | join 子句 |
| `Hyperf\Database\Model\Relations\Relation` | 模型关联 |
| `Hyperf\Database\Model\Model` | 模型基类 |
| `Hyperf\DbConnection\Db` | Db 门面 |
| `Hyperf\Database\Schema\Builder` / `Schema` | Schema 构建器与门面 |
| `Hyperf\Database\Schema\Blueprint` / `ColumnDefinition` | migration 蓝图与列定义 |
| `Hyperf\Database\Connection` / `ConnectionInterface` | 连接(Db 绑定方法识别) |

## 支持方法清单

以下为插件识别的方法白名单(按功能分类;`-` 后数字为生效参数位置,"任意"表示所有参数位置)。

### 表名 / Schema 名

| 类别 | 方法 |
| --- | --- |
| 表/视图(第一个参数) | `from`、`table`、`join`、`joinWhere`、`leftJoin`、`leftJoinWhere`、`rightJoin`、`rightJoinWhere`、`crossJoin`、`hasTable`、`getColumnListing`、`hasColumn`、`hasColumns`、`getColumnType`、`create`、`drop`、`dropIfExists`、`dropColumns`、`rename`、`createDatabase`、`dropDatabaseIfExists` |
| Schema 名 | `createDatabase`、`dropDatabaseIfExists` |
| 别名定义(第二参数) | `from`、`fromSub`、`selectSub`、`table`(如 `from('users as u')` / `from($q, 'u')`) |

### 列名参数

| 类别 | 方法 |
| --- | --- |
| 查询投影 | `select`(任意)、`addSelect`、`get`(任意)、`first`(任意) |
| 条件 | `where`、`whereNot`、`orWhere`、`orWhereNot`、`whereColumn`、`orWhereColumn`、`whereIn`、`orWhereIn`、`whereNotIn`、`orWhereNotIn`、`whereIntegerInRaw`、`orWhereIntegerInRaw`、`orWhereIntegerNotInRaw`、`whereNull`、`orWhereNull`、`whereNotNull`、`orWhereNotNull`、`whereBetween` 系列、`whereDate/Day/Month/Time/Year` 及 `orWhere*` 变体、`whereRowValues`、`orWhereRowValues`、`whereJsonContains`、`whereJsonDoesntContain`、`whereJsonLength` 及各自 `orWhere*` 变体 |
| join 列参数 | `join`(1-3)、`joinWhere`(1)、`joinSub`(2-4) 及 `leftJoin*` / `rightJoin*` / `crossJoin` 对应位置、`on`(0-2) |
| 分组/排序 | `groupBy`(任意)、`having`、`orHaving`、`havingBetween`、`orderBy`、`orderByDesc`、`latest`、`oldest`、`reorder`、`forPageBeforeId`(2)、`forPageAfterId`(2) |
| 读取/聚合/分页 | `find`(1)、`value`、`paginate`(1)、`simplePaginate`(1)、`getCountForPagination`、`pluck`(0-1)、`implode`、`count`、`min`、`max`、`sum`、`avg`、`average`、`aggregate`(1)、`numericAggregate`(1)、`insertUsing`(1)、`increment`、`decrement` |
| 写入(数组键只补全列名) | `create`、`fill`、`update`、`updateOrInsert`、`updateOrCreate`、`insert`、`insertGetId`、`insertOrIgnore` |
| Schema 列操作 | `hasColumn`(1)、`hasColumns`(1)、`getColumnType`(1)、`dropColumn`、`dropColumns`(1)、`dropConstrainedForeignId`、`renameColumn`、`dropSoftDeletes`、`dropSoftDeletesTz`、`after`、`removeColumn` |
| Raw 片段(仅简单列表达式) | `selectRaw`、`whereRaw`、`orWhereRaw`、`havingRaw`、`orHavingRaw`、`orderByRaw`、`groupByRaw`、`raw` |

### Blueprint(migration)列与索引

| 类别 | 方法 |
| --- | --- |
| 列类型(列名参数) | `id`、`increments` 系列、`char`、`string`、`text` 系列、`integer` 系列(含 unsigned)、`foreignId`、`foreignIdFor`(1)、`float`、`double`、`decimal` 系列、`boolean`、`enum`、`set`、`json`、`jsonb`、`date`、`dateTime(Tz)`、`time(Tz)`、`timestamp(Tz)`、`softDeletes(Tz)`、`year`、`binary`、`uuid`、`foreignUuid`、`ipAddress`、`macAddress`、`geometry` 系列、`computed` 等全部 Blueprint 列类型方法 |
| 索引(列参数 0,索引名参数 1) | `index`、`unique`、`primary`、`spatialIndex`、`foreign`、`indexCommand`(1)、`createIndexName`(1) |
| 删除索引(补全已存在索引名) | `dropIndex`、`dropUnique`、`dropPrimary`、`dropForeign`、`dropSpatialIndex` |

### Db 原生 SQL 绑定(命名占位符)

`select`、`selectOne`、`insert`、`update`、`delete`、`statement`、`affectingStatement` —— 绑定数组键按 SQL 中的 `:name` 占位符补全与跳转,详见[功能详解](./features.md#7-sql-命名占位符绑定)。

### 模型属性

| 属性 | 支持 |
| --- | --- |
| `$fillable` / `$guarded` / `$hidden` / `$visible` / `$dates` | 数组值补全列名 + 跳转 + 悬停 |
| `$casts` | 键补全列名;值补全 cast 类型 |
| `$table` | 表名补全 + 跳转 + 未知表检查(支持 `schema.table`) |
| `$connection` | 连接名补全 + 跳转 + 未知连接检查 |

**`$casts` 值可补全的 cast 类型**:`int`、`integer`、`real`、`float`、`double`、`string`、`bool`、`boolean`、`object`、`array`、`json`、`collection`、`date`、`datetime`、`timestamp`,以及带参数形式 `decimal:<digits>`、`date:<format>`、`datetime:<format>`。

## 已知限制

以下功能目前**不支持**(部分为有意为之,部分待后续版本):

- **复杂 raw SQL 不解析**:`selectRaw('COUNT(*) as c')`、`whereRaw('a + b > 1')` 等含函数/运算/子查询的字符串直接跳过,不报未知列也不补全其中标识符;
- **Raw 片段无悬停文档**:raw 中的列只有补全/跳转/检查,没有 hover;
- **位置占位符 `?` 不支持**:`Db::select('... WHERE id = ?', [1])` 的位置绑定没有补全与跳转(仅支持 `:name` 命名占位符);
- **子查询的表推导有限**:`fromSub`/`selectSub`/`joinSub` 支持别名提取,但闭包/构建器实例子查询内部的列**不会**向外层 SELECT 传播;
- **无索引/外键的未知检查**:检查有 Unknown table/view、Unknown column 与 Unknown database connection 三项;写错索引名(如 `dropIndex('xxx')`)目前不告警;
- **不读取 `.env` / `config` 运行时值**:除 `databases.php` 的连接名/`database`/`prefix` 外,不做其他配置文件的动态求值;`env('KEY', 'default')` 只取默认值;
- **模型表名只识别静态声明**:`$table` 属性或类名约定;通过构造函数/方法动态设置表名的模型无法解析;
- **表悬停只看摘要**:宽表的完整 DDL 渲染慢,悬停只显示 schema 限定名 + 表注释;需要 DDL 请 Ctrl+Click 跳转到 Database 工具窗口。

## 与上游 laravel-query-intellij 的差异

| 差异点 | 说明 |
| --- | --- |
| 识别目标 | `Hyperf\Database\*` / `Hyperf\DbConnection\Db`(上游为 `Illuminate\Database\*`) |
| 连接感知 | 新增:解析 `config/autoload/databases.php`,支持连接级 schema 隔离与表前缀(上游无) |
| Cast 类型 | 适配 Hyperf `HasAttributes` 的 cast 类型列表 |
| 已移除 | Laravel 测试断言补全(`assertDatabaseHas` 等)、`\DB` / `\Schema` 全局 facade 别名(Hyperf 无对应物) |
| 目标框架 | Hyperf |

---

返回[文档首页](./README.md)
