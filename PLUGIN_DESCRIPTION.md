This plugin provides database‑integration for the Hyperf query builder. When used together with the IDE’s data‑source tool (DataGrip database tool), it delivers auto‑completion, unknown‑element validation, Ctrl‑Click navigation and hover documentation for schemas, tables, views and columns.

本插件为 **Hyperf** 查询构建器提供数据库集成,配合 IDE 的数据源(DataGrip 数据库工具)为 schema、表、视图与列提供自动补全、未知元素检查、Ctrl+Click 跳转与悬停文档。

[文档](https://github.com/tw2066/hyperf-query-intellij)

## 功能特性

* 查询构建器与 Schema 构建器方法中的 schema、表、视图与列名补全
* 迁移(migration)文件内的补全
* 未知数据库元素检查
* 表别名支持
* 构建器方法可从模型解析表名
* 模型 relation 闭包方法的关联表名解析
* 与数据库元素建立文本链接,支持跳转与重构
* 查询构建器列参数的悬停/快速文档 —— 在 `where()`、`select()`、`orderBy()` 等方法的列名上悬停,可查看解析出的列类型、是否可空、默认值与注释;表名悬停显示轻量摘要(schema + 表注释),而非完整 DDL
* 模型属性的列名补全、Ctrl+Click 跳转与悬停文档 —— `$fillable`、`$guarded`、`$hidden`、`$visible`、`$dates` 数组值与 `$casts` 数组键基于模型表(来自 `$table` 属性或类名约定)解析;`$casts` 数组值补全 Hyperf 属性转换类型(`integer`、`datetime`、`decimal:<digits>`、`date:<format>` 等)
* 模型 `$table` 属性的表名补全、Ctrl+Click 跳转与未知表检查,作用域限定在模型的 `$connection`(遵守 schema 与表前缀)
* `Db::select()`、`Db::update()` 等 `Hyperf\DbConnection\Db` 带 `$bindings` 方法的 SQL 命名占位符(`:name`)补全与跳转 —— 绑定数组键基于查询字符串中的占位符补全(`name` 与 `:name` 两种写法均可),Ctrl+Click 键名可跳转到 SQL 中的占位符
* `config/autoload/databases.php` 中的数据库连接名在 `Db::connection()` / `Schema::connection()` 与模型 `$connection` 属性中补全,支持 Ctrl+Click 跳转到配置项与未知连接名检查告警;使用指定连接的链式调用(或模型)上的表/列补全、引用解析与检查都限定在该连接配置的 `database` schema 内(未指定时回退 `default` 连接),表名遵守连接的 `prefix`
* 原生 SQL 片段(`selectRaw()`、`whereRaw()`、`orderByRaw()`、`havingRaw()`、`groupByRaw()`、`Db::raw()` 等)中的简单列表达式获得列补全、跳转与检查 —— 支持逗号分隔列表与带连接表前缀的表/别名引用;复杂 SQL(函数、算术运算)保持跳过,避免误报
* 可配置表前缀与数据源过滤

## 注意事项

1. **必须先配置 IDE 数据源**:所有补全信息来自 Database 工具窗口中的数据源元数据。没有配置数据源,或数据源未刷新加载 schema,插件不会有任何补全。
2. **vendor 必须被索引**:插件通过 PSI 类型解析判断调用链是否属于 `Hyperf\Database\*`,如果 vendor 目录被排除或依赖未安装,所有功能失效。
3. **表前缀别重复设置**:连接配置(`databases.php` 的 `prefix`)优先于插件全局 Table prefix,二者不会叠加;全局前缀只作用于未配置 `prefix` 的连接。
4. **`env()` 解析有限**:连接配置中 `env('DB_DATABASE', 'test')` 只取**默认值**,插件不会读取真实 `.env` 文件。线上/本地 schema 不同时,以默认值为准或让配置写死。
5. **多数据源项目建议开过滤**:否则补全列表会包含所有数据源的所有 schema,既慢又干扰。
6. **表悬停只看摘要**:宽表的完整 DDL 渲染慢,所以悬停只显示表注释;需要 DDL 请 Ctrl+Click 跳转到 Database 工具窗口。
7. **修改数据源后**:补全基于 IDE 缓存,数据库结构变更后记得在数据源上 <kbd>Refresh</kbd>。

