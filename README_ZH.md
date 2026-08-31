# Hyperf Query 中文文档

> English: [README.md](README.md)

**Hyperf Query** 是一款 JetBrains IDE 插件(PhpStorm / 带 PHP 与 Database 插件的 IntelliJ IDEA),为 **Hyperf 3.2** 的查询构建器、模型、Schema 构建器提供数据库级别的代码补全、未知元素检查、Ctrl+Click 跳转与悬停文档。

本项目 fork 自 [ekvedaras/laravel-query-intellij](https://github.com/ekvedaras/laravel-query-intellij)(MIT 协议),识别目标已从 `Illuminate\Database\*` 全面迁移到 `Hyperf\Database\*`。

---

## 目录

- [环境要求](#环境要求)
- [安装](#安装)
- [快速开始](#快速开始)
- [功能详解](#功能详解)
  - [表 / 视图 / Schema 补全](#1-表--视图--schema-补全)
  - [列名补全](#2-列名补全)
  - [模型属性补全](#3-模型属性补全)
  - [Migration(Blueprint)补全](#4-migrationblueprint补全)
  - [数据库连接感知](#5-数据库连接感知)
  - [Raw SQL 片段支持](#6-raw-sql-片段支持)
  - [SQL 命名占位符绑定](#7-sql-命名占位符绑定)
  - [未知表 / 列检查](#8-未知表--列检查inspection)
  - [Ctrl+Click 跳转](#9-ctrlclick-跳转)
  - [悬停文档](#10-悬停文档)
- [插件设置](#插件设置)
- [注意事项](#注意事项)
- [已知限制与未实现功能](#已知限制与未实现功能)
- [与上游 laravel 版本的差异](#与上游-laravel-版本的差异)
- [开发与构建](#开发与构建)

---

## 环境要求

| 依赖 | 说明 |
| --- | --- |
| IDE | PhpStorm 2022.2 及更高版本(或安装了 PHP 插件 + Database Tools 插件的 IntelliJ IDEA Ultimate) |
| 数据库插件 | 必须启用 **Database Tools and SQL** 插件,并在 **Database 工具窗口**中配置好数据源 —— 插件的所有表/列信息都来自 IDE 数据源,**不会**读取项目里的 `.env` 去连数据库 |
| 项目 | Hyperf 3.2(`Hyperf\Database\*`、`Hyperf\DbConnection\Db` 等类可被 IDE 索引到,即 vendor 目录正常加载) |

## 安装

- **IDE 内安装(推荐)**:
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> > 搜索 **"Hyperf Query"** > <kbd>Install</kbd>

- **手动安装**:
  从 [Releases](https://github.com/tw2066/hyperf-query-intellij/releases/latest) 下载最新 zip,然后
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install Plugin from Disk...</kbd>

安装后重启 IDE。

## 快速开始

1. **配置数据源**:打开 IDE 右侧的 <kbd>Database</kbd> 工具窗口,添加你的 MySQL/PostgreSQL 等数据源,并确保对应的 schema 已被加载(数据源上右键 > <kbd>Refresh</kbd>)。
2. **打开 Hyperf 项目**:插件启动时会检测项目中是否存在 `Hyperf\Database\Query\Builder`,若存在且你尚未配置数据源过滤,会弹出通知引导你进入设置。
3. **开始使用**:在 `Db::table('...')`、`Model::query()->where('...')`、`Schema::table('...')` 等方法的字符串参数中输入,即可获得补全;Ctrl+Click 可跳转到数据库元素;鼠标悬停可查看列定义。

设置面板位置:<kbd>Settings</kbd> > <kbd>PHP</kbd> > <kbd>Hyperf Query</kbd>(详见[插件设置](#插件设置))。

## 功能详解

### 1. 表 / 视图 / Schema 补全

在以下方法的第一个参数中补全表名、视图名:

```php
Db::table('users');                    // 表/视图
Db::connection('default')->table('users');
User::query()->from('users');
Schema::table('users', ...);
Schema::create('users', ...);
Schema::createDatabase('...');         // schema 名
```

已识别的表名方法包括:`from`、`table`、`join` / `leftJoin` / `rightJoin` / `crossJoin` / `joinWhere` 系列、`hasTable`、`getColumnListing`、`hasColumn(s)`、`getColumnType`、`create`、`drop`、`dropIfExists`、`rename`、`createDatabase`、`dropDatabaseIfExists` 等。

- 支持 `Schema::table('users', ...)` 这类 Schema Builder 调用
- `fromSub()` / `selectSub()` / `table()` 的**表别名**会被识别,后续 `where('别名.列')` 可以正确解析

### 2. 列名补全

覆盖 100+ 个查询构建器方法的列参数,典型场景:

```php
User::query()
    ->select(['id', 'name'])           // 数组值
    ->where('status', 1)               // 字符串参数
    ->whereIn('id', [1, 2])
    ->whereBetweenColumns('a', 'b')
    ->orderBy('created_at')
    ->groupBy('type')
    ->pluck('name', 'id')
    ->update(['status' => 1])          // 数组键
    ->insert([['name' => 'x']]);       // 批量 insert 的嵌套数组键
```

- **表名解析链**支持:`Db::table('users')`、`Model::query()`(从 `$table` 属性或类名约定推导表名)、join、模型 relation 闭包(`with(function ($q) { $q->where(...) })`)、`when()` 闭包(即使 `when()` 之前已链式调用了其他方法)
- `create()` / `update()` / `fill()` / `insert()` / `insertGetId()` / `insertOrIgnore()` 的**数组键**只补全列名
- `increment()` / `decrement()` / `find()` / `paginate()` 等聚合与分页方法的列参数也已覆盖

### 3. 模型属性补全

在 Hyperf 模型类内部,以下属性的**数组值**按模型表解析列名,支持补全、跳转与悬停:

```php
class User extends Model
{
    protected array $fillable = ['name', 'email'];   // 补全列名
    protected array $guarded = ['id'];
    protected array $hidden = ['password'];
    protected array $visible = ['name'];
    protected array $dates = ['birthday'];

    protected array $casts = [
        'status' => 'integer',   // 键:补全列名;值:补全 cast 类型
    ];
}
```

`$casts` 的**值**补全 Hyperf 属性 cast 类型:`int`、`integer`、`real`、`float`、`double`、`string`、`bool`、`boolean`、`object`、`array`、`json`、`collection`、`date`、`datetime`、`timestamp`,以及带参数形式 `decimal:<digits>`、`date:<format>`、`datetime:<format>`。

模型表名解析规则:优先读取 `$table` 属性(含父类继承),否则按类名约定推导。

### 4. Migration(Blueprint)补全

在 migration 文件的 `Schema::table/create(..., function (Blueprint $table) { ... })` 闭包内:

- 列定义方法:`$table->string('...')`、`$table->integer('...')`、`id()`、`timestamps()` 等全部 Blueprint 列类型的列名参数
- 索引:`index()`、`unique()`、`primary()`、`spatialIndex()`、`foreign()` 的列参数与索引名参数
- 删除索引:`dropIndex()`、`dropUnique()`、`dropPrimary()`、`dropForeign()`、`dropSpatialIndex()`(补全已存在的索引名)
- `renameColumn()`、`dropColumn()`、`dropColumns()`、`after()`、`removeColumn()` 等

### 5. 数据库连接感知

连接名从项目的 `config/autoload/databases.php` 中读取:

```php
Db::connection('readonly')->table('goods');      // 连接名补全 + 跳转
Schema::connection('readonly')->hasTable('goods');

class Goods extends Model
{
    protected ?string $connection = 'readonly';  // 属性默认值同样支持补全
}
```

- **连接名补全**:`Db::connection()` / `Schema::connection()` 的第一个参数、模型 `$connection` 属性默认值,补全 `databases.php` 中定义的连接名;`env('KEY', 'default')` 形式的值解析为其默认值
- **Ctrl+Click** 连接名可跳转到 `databases.php` 中对应的数组键
- **作用域隔离**:指定了连接的链式调用(或声明了 `$connection` 的模型),其表/列补全、引用解析、未知元素检查都**只针对该连接配置的 `database` schema**;未指定连接时使用 `default` 连接
- **表前缀**:连接的 `prefix` 配置会被遵守,例如连接配置 `'prefix' => 'pre_'` 时,`table('goods')` 解析为 `pre_goods`;连接未配置 `prefix` 时使用插件设置中的全局表前缀,显式设置 `'prefix' => ''` 可为该连接关闭前缀
- **回退行为**:连接无法解析时(缺少配置、`env()` 无默认值、schema 不在 IDE 数据源中),回退为扫描全部数据源

### 6. Raw SQL 片段支持

`selectRaw()`、`whereRaw()`、`orWhereRaw()`、`havingRaw()`、`orHavingRaw()`、`orderByRaw()`、`groupByRaw()` 以及 `Db::raw('...')` 包裹的字符串中,**简单列表达式**支持补全、Ctrl+Click 跳转与未知列检查:

```php
User::query()
    ->selectRaw('id, name')
    ->whereRaw('status = 1')
    ->orderByRaw('created_at desc');
```

- 支持 `column`、`table.column`、`schema.table.column`、`... as alias` 形式,以及逗号分隔的列表(`selectRaw('id, pre_a.id')`)
- 带连接表前缀的表/别名引用也能解析(如前缀为 `pre_` 时 `pre_a.id` 可解析别名 `a`)
- **复杂 SQL(函数、算术运算、子查询)会被跳过**,不报误警
- Raw 片段**不提供悬停文档**(有意为之)

### 7. SQL 命名占位符绑定

针对 `Db` 门面带 `$bindings` 参数的方法(`select`、`selectOne`、`insert`、`update`、`delete`、`statement`、`affectingStatement`):

```php
$sql = 'SELECT * FROM users WHERE id = :id AND status = :status';
Db::select($sql, ['id' => 1, ':status' => 1]);
//               ^^^^ 补全占位符名;Ctrl+Click 跳转到 SQL 中的 :id
```

- 绑定数组的键根据 SQL 字符串中的 `:name` 占位符补全,`name` 与 `:name` 两种形式都会建议;已绑定的键不再重复建议
- SQL 字符串可以是内联字面量,也可以是变量赋值(`$sql = '...'`,取调用点之前**最近的**一次赋值)
- Ctrl+Click 绑定键跳转到 SQL 字符串中对应的占位符
- 这些原生 SQL 方法上**不会**触发列/表补全与未知元素检查(避免把 SQL 文本误报为列名)

### 8. 未知表 / 列检查(Inspection)

两个默认开启的检查,位于 <kbd>Settings</kbd> > <kbd>Editor</kbd> > <kbd>Inspections</kbd> > <kbd>PHP</kbd> > <kbd>Database</kbd>:

| 检查 | 作用 |
| --- | --- |
| **Unknown table or view** | 表/视图名在数据源中不存在时告警 |
| **Unknown column** | 列名在已解析的表中不存在时告警 |

检查与补全共用同一套解析逻辑,连接感知、表前缀、别名、raw 简单表达式等规则全部生效。可在 Inspections 设置中调整严重级别或关闭。

### 9. Ctrl+Click 跳转

以下字符串字面量可以 Ctrl+Click(或 Ctrl+B)直接跳转:

- 表/视图名 → Database 工具窗口中的表
- 列名 → 对应的列定义
- Schema 名、索引名、外键名
- 连接名 → `config/autoload/databases.php` 中的配置项
- 绑定数组键 → SQL 字符串中的 `:placeholder`
- 模型属性(`$fillable` 等)中的列名 → 模型表对应的列

### 10. 悬停文档

- **列名悬停**(在 `where()`、`select()`、`orderBy()` 等方法的列参数上):显示该列的类型、是否可空、默认值与注释
- **表名悬停**:显示轻量摘要(schema 限定名 + 表注释),**不渲染完整 DDL**(宽表上太慢);完整 DDL 可 Ctrl+Click 进入 Database 工具窗口查看
- Raw SQL 片段中的列不提供悬停

## 插件设置

位置:<kbd>Settings/Preferences</kbd> > <kbd>PHP</kbd> > <kbd>Hyperf Query</kbd>(按项目保存,存储于 `.idea/hyperf-query-settings.xml`)

| 选项 | 说明 |
| --- | --- |
| **Table prefix** | 全局表前缀。代码中写 `table('users')` 时按 `前缀 + users` 去数据源中解析。连接配置里定义了 `prefix` 时以连接配置为准 |
| **Filter data sources** | 勾选后启用下方的数据源/schema 列表,只有被勾选的 schema 参与补全与检查。项目连了多个数据源时建议开启,避免候选列表混入无关库 |

首次打开项目时,如果检测到 Hyperf 且未开启数据源过滤,会弹出通知提示配置;点击 **Ignore** 后不再提醒。

## 注意事项

1. **必须先配置 IDE 数据源**:所有补全信息来自 Database 工具窗口中的数据源元数据。没有配置数据源,或数据源未刷新加载 schema,插件不会有任何补全。
2. **vendor 必须被索引**:插件通过 PSI 类型解析判断调用链是否属于 `Hyperf\Database\*`,如果 vendor 目录被排除或依赖未安装,所有功能失效。
3. **表前缀别重复设置**:连接配置(`databases.php` 的 `prefix`)优先于插件全局 Table prefix,二者不会叠加;全局前缀只作用于未配置 `prefix` 的连接。
4. **`env()` 解析有限**:连接配置中 `env('DB_DATABASE', 'test')` 只取**默认值**,插件不会读取真实 `.env` 文件。线上/本地 schema 不同时,以默认值为准或让配置写死。
5. **多数据源项目建议开过滤**:否则补全列表会包含所有数据源的所有 schema,既慢又干扰。
6. **表悬停只看摘要**:宽表的完整 DDL 渲染慢,所以悬停只显示表注释;需要 DDL 请 Ctrl+Click 跳转到 Database 工具窗口。
7. **修改数据源后**:补全基于 IDE 缓存,数据库结构变更后记得在数据源上 <kbd>Refresh</kbd>。

## 已知限制与未实现功能

以下功能目前**不支持**(部分为有意为之,部分待后续版本):

- **复杂 raw SQL 不解析**:`selectRaw('COUNT(*) as c')`、`whereRaw('a + b > 1')` 等含函数/运算/子查询的字符串直接跳过,不报未知列也不补全其中标识符
- **Raw 片段无悬停文档**:raw 中的列只有补全/跳转/检查,没有 hover
- **位置占位符 `?` 不支持**:`Db::select('... WHERE id = ?', [1])` 的位置绑定没有补全与跳转(仅支持 `:name` 命名占位符)
- **子查询的表推导有限**:`fromSub`/`selectSub`/`joinSub` 支持别名提取,但闭包/构建器实例子查询内部的列**不会**向外层 SELECT 传播
- **无索引/外键的未知检查**:检查只有 Unknown table/view 与 Unknown column 两项;写错索引名(如 `dropIndex('xxx')`)目前不告警
- **Laravel 专属能力已移除**(Hyperf 无对应物):`assertDatabaseHas` / `assertDeleted` 等测试断言补全、`\DB` / `\Schema` 全局 facade 别名
- **不读取 `.env` / `config` 运行时值**:除 `databases.php` 的连接名/`database`/`prefix` 外,不做其他配置文件的动态求值
- **模型表名只识别静态声明**:`$table` 属性或类名约定;通过构造函数/方法动态设置表名的模型无法解析

## 与上游 laravel 版本的差异

| 差异点 | 说明 |
| --- | --- |
| 识别目标 | `Hyperf\Database\*` / `Hyperf\DbConnection\Db`(上游为 `Illuminate\Database\*`) |
| 连接感知 | 新增:解析 `config/autoload/databases.php`,支持连接级 schema 隔离与表前缀(上游无) |
| Cast 类型 | 适配 Hyperf `HasAttributes` 的 cast 类型列表 |
| 已移除 | Laravel 测试断言补全、`\DB`/`\Schema` 别名 |
| 目标框架 | Hyperf 3.2 |

## 开发与构建

```bash
./gradlew build          # 完整构建(含插件校验)
./gradlew test           # 跑全部测试(基于 H2/MySQL dump 初始化测试数据源,较慢)
./gradlew runIde         # 启动带插件的调试 IDE
./gradlew buildPlugin    # 打包,产物在 build/distributions/
```

更详细的架构说明见 [CLAUDE.md](CLAUDE.md)。

## 反馈

问题与建议请提交 [GitHub Issues](https://github.com/tw2066/hyperf-query-intellij/issues)。
