<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# Hyperf Query 更新日志

## 1.0.6 Unreleased

### 变更
- 性能：数据库引用解析（`DbReferenceExpression`）现在按字符串字面量缓存，并在检查（inspection）、引用解析与补全之间共享——此前同一个 `'users.email'` 字符串在每轮高亮中最多被重复解析 5 次，每次都要重新走一遍方法链收集并重新扫描所有数据源的表/列。缓存在任意 PSI 变更后失效
- 性能：追踪查询构建器链时的变量引用搜索范围从全项目缩小到当前文件
- 性能：方法类型解析（`isInteresting`、`isBlueprintMethod` 等）现在按方法引用缓存；类继承检查不再在每一层继承上重复查询 PHP 索引
- 性能：单列名解析在已有命中后，对与查询别名无关的表跳过列枚举，不再枚举每个数据源中每张表的所有列
- 性能：移除了补全/高亮线程上的 `parallelStream` 使用（这些工作此前跑在读锁之外的共享 common pool 上）；补全现在每个数据源只物化一次表列表，不再按表别名逐个重复获取

### 新增
- 数据库连接名补全：为 `Db::connection()` / `Schema::connection()` 的第一个参数以及模型 `$connection` 属性默认值提供补全，数据来源于项目的 `config/autoload/databases.php`（`env('KEY', 'default')` 形式的值解析为 env 默认值）
- 支持从连接名 Ctrl+Click 跳转到 `config/autoload/databases.php` 中对应的数组键
- 连接感知的表/列补全、引用解析与未知元素检查：使用 `Db::connection('name')` / `Schema::connection('name')` 的链以及声明了 `$connection` 的模型，会限定到该连接 `database` 配置对应的 schema。未指定连接时使用 `default` 连接的 schema。若连接或其 schema 无法解析（缺少配置、`env()` 无默认值、IDE 数据源中不存在该 schema），则回退为原先扫描全部数据源的行为
- 按连接的表前缀支持：在该连接上解析和补全表名时会使用 `config/autoload/databases.php` 中该连接的 `prefix`（例如 `table('goods')` 解析为 `pre_goods`）。连接未定义 `prefix` 时使用全局表前缀设置；可设置 `'prefix' => ''` 为该连接显式关闭前缀
- 原生 SQL 片段中简单列表达式的列补全、Ctrl+Click 跳转与未知列检查：覆盖 `selectRaw()`、`whereRaw()`、`orWhereRaw()`、`havingRaw()`、`orHavingRaw()`、`orderByRaw()`、`groupByRaw()` 以及 `Db::raw()` 包裹的表达式。匹配 `column`、`table.column`、`schema.table.column` 或 `... as alias` 形式的字符串会基于查询的表上下文解析，支持逗号分隔列表（`selectRaw('id, jc_a.id')`）以及带连接表前缀的表/别名引用（连接前缀为 `jc_` 时，`jc_a.id` 解析别名 `a`）；复杂 SQL（函数、运算、子查询）会被跳过，以避免误报"未知列"警告。原生片段有意不提供悬停文档

### 修复
- 设置面板修改表前缀或数据源过滤后，已缓存的表/列/索引解析结果立即失效，不再等到下一次文件编辑才刷新
- 设置面板："Filter data sources" 下的数据源复选框现在可以直接点击勾选。此前该单元格同时注册了布尔编辑器和行点击监听器，每次点击切换两次，导致永远无法显示为已勾选
- 表名悬停不再渲染完整的 `CREATE TABLE` DDL（宽表上很慢），改为显示轻量摘要：带 schema 限定的表名和表注释。完整 DDL 仍可通过 Ctrl+Click 进入数据库工具窗口查看
- 通过 `Db::connection(...)`（`Hyperf\Database\ConnectionInterface`）的查询构建器链现在可以被补全和检查识别

## 1.0.5 - 2026-08-10

### 新增
- 查询构建器方法列参数的悬停/快速文档（`where()`、`select()`、`orderBy()` 等）：悬停在列名上时显示解析到的数据库列的类型、是否可空、默认值和注释
- Hyperf 模型属性的列名补全、Ctrl+Click 跳转与悬停文档：`$fillable`/`$guarded`/`$hidden`/`$visible`/`$dates` 的数组值以及 `$casts` 的数组键（包括尚未写成 `key =>` 的裸字符串），基于模型对应的表解析（读取 `$table` 属性或从类名推导）
- `$casts` 数组值的属性 cast 类型补全：`int`、`integer`、`real`、`float`、`double`、`string`、`bool`、`boolean`、`object`、`array`、`json`、`collection`、`date`、`datetime`、`timestamp`，以及带参数形式 `decimal:<digits>`、`date:<format>`、`datetime:<format>`

## 1.0.4 - 2026-08-08

### 新增
- 为 `\Hyperf\DbConnection\Db` 带 `$bindings` 数组参数的方法（`select`、`selectOne`、`insert`、`update`、`delete`、`statement`、`affectingStatement`）补全 SQL 命名占位符：绑定数组的键会根据查询字符串中的 `:name` 占位符补全，同时提供 `name` 和 `:name` 两种形式。已绑定的键会被跳过。查询字符串可以是内联字面量，也可以是变量赋值（`$sql = '...'`）
- 支持从绑定数组键（`'id'` 或 `':id'`）跳转到上述 `Db` 方法查询字符串中对应的占位符
- 在这些 `Db` SQL 方法上，抑制列/表补全、引用和未知元素检查——此前查询字符串和绑定键会被误认为数据库列

### 修复
- 补全形式跟随已输入的前缀：空键（`['' => ...]`）只提示普通 `name` 形式，输入冒号前缀（`[':' => ...]`）时只提示 `:name` 形式
- 补全现在也会在绑定数组中的空元素（`['']`）上触发，而不仅是在显式写出 `=>` 键时
- 变量被重复赋值时，跳转和补全解析到调用点之前最近的 `$sql = '...'` 赋值，而不是总取第一个赋值
- 修复后台高亮器中的 `StackOverflowError`：此前通过引用搜索解析 SQL 变量的赋值时会递归触发本插件自己的引用提供者。现在赋值查找改为从调用点沿 PSI 树向前回溯，既无递归，也能解析到最近的在前赋值

## 1.0.3 - 2026-08-05

### 修复
- 列补全和引用解析现在可以在 `when()` 风格的闭包内正常工作，即使 `when()` 之前还链式调用了其他构建器方法（例如 `Model::query()->where(...)->when(..., function (Builder $query) { $query->where('<caret>'); })`）。此前只有当 `when()` 直接跟在 `Model::query()` 之后时闭包才会被识别

### 新增
- `insert()`、`insertGetId()` 和 `insertOrIgnore()` 数组键的列补全，包括批量（嵌套数组）`insert()` 调用。键只补全解析到的模型/表的列，与已有的 `update()` 行为一致
- `first()` 列数组值的列补全（例如 `Model::query()->first(['id', '<caret>'])`），与已有的 `get()` 行为一致

## 1.0.2 - 2026-08-04

### 修复
- 修复设置面板在 PhpStorm 2023+ 上显示占位文案的问题：将 `projectConfigurable` 的 parent 从 `language` 改为 `reference.webide.settings.project.settings.php`，使 "Hyperf Query" 设置面板正常渲染

### 变更
- 插件适配 Hyperf 3.2：识别目标从 `Illuminate\Database\*` 改为 `Hyperf\Database\*` 的查询构建器、模型和 schema 构建器类
- `LaravelUtils` 更名为 `HyperfUtils`，设置类更名为 `HyperfQuerySettings`
- 插件显示名改为 "Hyperf Query"

### 移除
- 移除了 Hyperf 中不存在的 Laravel `Illuminate\Foundation\Testing\TestCase` 数据库断言支持（`assertDatabaseHas`、`assertDeleted` 等）
- 移除了 Hyperf 中不存在的 `\DB` 和 `\Schema` 门面别名
