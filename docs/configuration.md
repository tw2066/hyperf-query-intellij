# 相关配置

## 设置面板

位置:<kbd>Settings/Preferences</kbd> > <kbd>PHP</kbd> > <kbd>Hyperf Query</kbd>

配置**按项目保存**,持久化在 `.idea/hyperf-query-settings.xml`(可随项目提交,团队共享)。

### Enable Hyperf Query

全局开关,**默认开启**。关闭后插件的补全、检查、Ctrl+Click 跳转与悬停文档全部停用,面板中其余设置项联动禁用。停用期间也不会再弹出配置引导通知。

### Table prefix

全局表前缀。代码中写 `table('users')` 时,按 `前缀 + users` 去数据源中解析(例如前缀 `pre_` 时解析 `pre_users`)。

**优先级**:连接配置(`config/autoload/databases.php` 中该连接的 `prefix`)**优先于**全局前缀,二者**不会叠加**:

| 连接 `prefix` 配置 | 全局 Table prefix | 实际生效 |
| --- | --- | --- |
| `'prefix' => 'pre_'` | 任意 | `pre_`(连接配置优先) |
| 未配置(无 `prefix` 键) | `g_` | `g_`(回退全局) |
| `'prefix' => ''` | `g_` | 无前缀(显式关闭) |

### Filter data sources

勾选后启用下方的数据源/schema 列表,**只有被勾选的 schema** 参与补全、检查与跳转。

- 项目连了多个数据源时强烈建议开启:否则补全列表会包含所有数据源的所有 schema,既慢又干扰;
- 列表按 `数据源 / schema` 两级展示,勾选父节点等于勾选其下全部 schema;
- 注意与[连接作用域](./features.md#5-数据库连接感知)的关系:连接配置决定"解析到哪个 schema",数据源过滤决定"哪些 schema 可见",二者叠加生效——连接指向的 schema 若未勾选,同样不可见。

## 与 databases.php 的互动

插件会读取 `config/autoload/databases.php` 中的连接定义,用于连接名补全、作用域隔离与表前缀:

```php
return [
    'default' => [
        'database' => env('DB_DATABASE', 'test'),  // 决定表/列解析的 schema
        'prefix' => 'pre_',                        // 该连接的表前缀
    ],
    'readonly' => [ ... ],
];
```

- **连接名来源**:`Db::connection()` / `Schema::connection()` / 模型 `$connection` 的补全候选就是这里的数组键;
- **`database` 决定 schema 作用域**:指定连接的链式调用只在该 schema 内解析;未指定连接时回退 `default` 连接;
- **`env()` 只取默认值**:`env('DB_DATABASE', 'test')` 解析为 `'test'`,插件**不读取真实 `.env` 文件**。线上/本地 schema 不同时,以默认值为准,或把配置写死;
- **回退行为**:连接无法解析时(缺少配置、`env()` 无默认值、schema 不在 IDE 数据源中),回退为扫描全部(被过滤勾选的)数据源;
- 项目中没有 `databases.php` 时,连接名检查不告警(无从判断)。

## Inspection 开关与严重级别

三个检查均默认开启,位于 <kbd>Settings</kbd> > <kbd>Editor</kbd> > <kbd>Inspections</kbd> > <kbd>PHP</kbd> > <kbd>Database</kbd>:

- **Unknown table or view**
- **Unknown column**
- **Unknown database connection**

可在此调整严重级别(Error / Warning / Weak Warning 等)或单独关闭。详见[功能详解 - 未知元素检查](./features.md#8-未知元素检查inspection)。

## 启动通知

检测到 Hyperf 项目(存在 `Hyperf\Database\Query\Builder`)且尚未开启数据源过滤时,插件会弹出通知引导进入设置面板。该通知**每个项目最多显示一次**;插件被全局停用期间不再弹出。
