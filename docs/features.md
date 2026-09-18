# 功能详解

每项功能均附 PHP 示例。文中"补全"指在字符串字面量中触发 IDE 补全,"跳转"指 Ctrl+Click(或 Ctrl+B)导航,"悬停"指鼠标悬停/Quick Documentation 显示文档。

## 目录

- [1. 表 / 视图 / Schema 补全](#1-表--视图--schema-补全)
- [2. 列名补全](#2-列名补全)
- [3. 模型属性补全](#3-模型属性补全)
- [4. Migration(Blueprint)补全](#4-migrationblueprint补全)
- [5. 数据库连接感知](#5-数据库连接感知)
- [6. Raw SQL 片段支持](#6-raw-sql-片段支持)
- [7. SQL 命名占位符绑定](#7-sql-命名占位符绑定)
- [8. 未知元素检查(Inspection)](#8-未知元素检查inspection)
- [9. Ctrl+Click 跳转](#9-ctrlclick-跳转)
- [10. 悬停文档](#10-悬停文档)

---

## 1. 表 / 视图 / Schema 补全

在查询构建器与 Schema 构建器方法的**第一个参数**中补全表名、视图名;`createDatabase` / `dropDatabaseIfExists` 补全 schema 名。

```php
use Hyperf\DbConnection\Db;
use Hyperf\Database\Schema\Schema;

Db::table('users');                      // 表/视图补全
Db::connection('default')->table('users');

User::query()->from('users');

Schema::table('users', function (Blueprint $table) { ... });
Schema::create('users', function (Blueprint $table) { ... });
Schema::createDatabase('shop');          // 补全 schema 名
```

已识别的表名方法包括:`from`、`table`、`join` / `leftJoin` / `rightJoin` / `crossJoin` / `joinWhere` 系列、`hasTable`、`getColumnListing`、`hasColumn(s)`、`getColumnType`、`create`、`drop`、`dropIfExists`、`rename`、`createDatabase`、`dropDatabaseIfExists` 等(完整清单见[适配性](./compatibility.md#支持方法清单))。

**表别名**会被识别,后续链式调用按别名解析列:

```php
Db::table('users as u')->where('u.status', 1);   // 'u.' 后补全 users 的列
```

## 2. 列名补全

覆盖 100+ 个查询构建器方法的列参数,字符串参数、数组值、数组键三种形态都支持:

```php
User::query()
    ->select(['id', 'name'])            // 数组值
    ->where('status', 1)                // 字符串参数
    ->whereIn('id', [1, 2])
    ->whereBetweenColumns('a', 'b')
    ->orderBy('created_at')
    ->groupBy('type')
    ->pluck('name', 'id')
    ->update(['status' => 1])           // 数组键
    ->insert([['name' => 'x']]);        // 批量 insert 的嵌套数组键
```

**表名解析链**支持以下入口,补全的列都来自解析出的那张表:

```php
// Db 门面
Db::table('users')->where('status', 1);

// 模型:$table 属性优先,否则按类名约定(User → users)
User::query()->where('status', 1);

// join:第二张表的列同样可以补全
Db::table('users')
    ->join('orders', 'users.id', '=', 'orders.user_id')
    ->where('orders.status', 1);

// 模型 relation 闭包
User::query()->with(['orders' => function ($q) {
    $q->where('status', 1);             // 解析为 orders 表的列
}]);

// when 闭包(即使 when 之前已链式调用了其他方法)
User::query()->where('status', 1)->when($x, function ($q) {
    $q->where('name', 'a');
});
```

其他规则:

- `create()` / `update()` / `fill()` / `insert()` / `insertGetId()` / `insertOrIgnore()` 的**数组键**只补全列名;
- `increment()` / `decrement()` / `find()` / `paginate()` 等聚合与分页方法的列参数也已覆盖;
- `select('*')` 这类含 `*` 的参数不触发列补全。

## 3. 模型属性补全

在 Hyperf 模型类内部,下列属性的**数组值**按模型表解析列名,支持补全、跳转与悬停:

```php
class User extends Model
{
    protected $table = 'users';                    // 表名补全 + Ctrl+Click 跳转 + 未知表检查

    protected array $fillable = ['name', 'email']; // 补全 users 表的列名
    protected array $guarded = ['id'];
    protected array $hidden = ['password'];
    protected array $visible = ['name'];
    protected array $dates = ['birthday'];

    protected array $casts = [
        'status' => 'integer',  // 键:补全列名;值:补全 cast 类型
    ];
}
```

`$casts` 的**值**补全 Hyperf 属性 cast 类型:`int`、`integer`、`real`、`float`、`double`、`string`、`bool`、`boolean`、`object`、`array`、`json`、`collection`、`date`、`datetime`、`timestamp`,以及带参数形式 `decimal:<digits>`、`date:<format>`、`datetime:<format>`。

`$table` 属性的表名字符串本身支持补全、跳转与未知表检查,支持 `schema.table` 两段式写法;声明了 `$connection` 的模型会限定到该连接 `database` 配置对应的 schema,并应用连接表前缀。

模型表名解析规则:优先读取 `$table` 属性(**含父类继承**,如集中声明在 `BaseModel`),否则按类名约定推导。通过构造函数/方法动态设置表名的模型无法解析。

## 4. Migration(Blueprint)补全

在 migration 文件的 `Schema::table/create(..., function (Blueprint $table) { ... })` 闭包内:

```php
Schema::table('users', function (Blueprint $table) {
    $table->string('nickname');          // 列名参数补全
    $table->integer('age')->nullable();
    $table->index('age');                // 列参数;第二参数补全索引名
    $table->unique('email');
    $table->foreign('role_id');          // 外键列参数
    $table->renameColumn('nickname', 'nick');
    $table->dropIndex('users_age_index'); // 补全已存在的索引名
    $table->after('id', 'nickname');
});
```

- 列定义方法:`string()`、`integer()`、`id()`、`timestamps()`、`softDeletes()`、`json()` 等全部 Blueprint 列类型的列名参数;
- 索引:`index()`、`unique()`、`primary()`、`spatialIndex()`、`foreign()` 的列参数与索引名参数;
- 删除索引:`dropIndex()`、`dropUnique()`、`dropPrimary()`、`dropForeign()`、`dropSpatialIndex()` 补全**已存在**的索引名;
- 其他:`renameColumn()`、`dropColumn()`、`dropColumns()`、`after()`、`removeColumn()` 等。

## 5. 数据库连接感知

连接名从项目的 `config/autoload/databases.php` 中读取:

```php
Db::connection('readonly')->table('goods');      // 连接名补全 + Ctrl+Click 跳转
Schema::connection('readonly')->hasTable('goods');

class Goods extends Model
{
    protected ?string $connection = 'readonly';  // 属性默认值同样补全连接名
}
```

具体能力:

- **连接名补全**:`Db::connection()` / `Schema::connection()` 的第一个参数、模型 `$connection` 属性默认值,补全 `databases.php` 中定义的连接名;`env('KEY', 'default')` 形式的值解析为其**默认值**;
- **跳转**:Ctrl+Click 连接名跳到 `databases.php` 中对应的数组键;
- **未知连接检查**:不在 `databases.php` 中的连接名给出 "Unknown database connection" 警告;项目中没有该配置文件时不告警(无从判断);
- **作用域隔离**:指定了连接的链式调用(或声明了 `$connection` 的模型),其表/列补全、引用解析、未知元素检查都**只针对该连接配置的 `database` schema**;未指定连接时使用 `default` 连接;

```php
// databases.php 中 'readonly' => ['database' => 'shop', ...]
// 下面这条只在 shop schema 中解析 goods 表及其列:
Db::connection('readonly')->table('goods')->where('status', 1);
```

- **表前缀**:连接配置了 `'prefix' => 'pre_'` 时,`table('goods')` 解析为 `pre_goods`;连接未配置 `prefix` 时使用插件设置中的全局表前缀;显式 `'prefix' => ''` 可为该连接关闭前缀;
- **回退行为**:连接无法解析时(缺少配置、`env()` 无默认值、schema 不在 IDE 数据源中),回退为扫描全部数据源。

## 6. Raw SQL 片段支持

`selectRaw()`、`whereRaw()`、`orWhereRaw()`、`havingRaw()`、`orHavingRaw()`、`orderByRaw()`、`groupByRaw()` 以及 `Db::raw('...')` 包裹的字符串中,**简单列表达式**支持补全、跳转与未知列检查:

```php
User::query()
    ->selectRaw('id, name')              // 逗号分隔列表
    ->whereRaw('status = 1')
    ->orderByRaw('created_at desc');

Db::table('users as u')->selectRaw('u.id, u.name');      // 别名引用
Db::table('users')->selectRaw('shop.users.id');          // schema.table.column 三段式
```

- 支持 `column`、`table.column`、`schema.table.column`、`... as alias` 形式;
- 带连接表前缀的表/别名引用也能解析(前缀为 `pre_` 时 `pre_a.id` 可解析别名 `a`);
- **复杂 SQL(函数、算术运算、子查询)会被跳过**,不报误警,例如 `selectRaw('COUNT(*) as c')`、`whereRaw('a + b > 1')`;
- Raw 片段**不提供悬停文档**(有意为之)。

## 7. SQL 命名占位符绑定

针对 `Db` 门面带 `$bindings` 参数的方法(`select`、`selectOne`、`insert`、`update`、`delete`、`statement`、`affectingStatement`):

```php
$sql = 'SELECT * FROM users WHERE id = :id AND status = :status';
Db::select($sql, ['id' => 1, ':status' => 1]);
//               ^^^^ 补全占位符名;Ctrl+Click 键名跳转到 SQL 中的 :id
```

- 绑定数组的键根据 SQL 字符串中的 `:name` 占位符补全,`name` 与 `:name` 两种形式都会建议;已绑定的键不再重复建议;
- SQL 字符串可以是内联字面量,也可以是变量赋值(`$sql = '...'`,取调用点之前**最近的**一次赋值);
- Ctrl+Click 绑定键跳转到 SQL 字符串中对应的占位符;
- 这些原生 SQL 方法上**不会**触发列/表补全与未知元素检查(避免把 SQL 文本误报为列名);
- 位置占位符 `?` 不支持,仅支持 `:name` 命名占位符。

## 8. 未知元素检查(Inspection)

三个默认开启的检查,位于 <kbd>Settings</kbd> > <kbd>Editor</kbd> > <kbd>Inspections</kbd> > <kbd>PHP</kbd> > <kbd>Database</kbd>:

| 检查 | 作用 | 示例 |
| --- | --- | --- |
| **Unknown table or view** | 表/视图名在数据源中不存在时告警(含模型 `$table` 属性) | `Db::table('user')` → 警告: Unknown table 'user' |
| **Unknown column** | 列名在已解析的表中不存在时告警 | `->where('stats', 1)` → 警告: Unknown column 'stats' |
| **Unknown database connection** | 连接名不在 `config/autoload/databases.php` 中时告警 | `Db::connection('readony')` → 警告: Unknown database connection |

检查与补全共用同一套解析逻辑,连接感知、表前缀、别名、raw 简单表达式等规则全部生效。可在 Inspections 设置中调整严重级别或关闭。

## 9. Ctrl+Click 跳转

以下字符串字面量可以 Ctrl+Click(或 Ctrl+B)直接跳转:

- 表/视图名 → Database 工具窗口中的表(含模型 `$table` 属性);
- 列名 → 对应的列定义(含 join 第二张表的列、relation 闭包内的列);
- Schema 名、索引名、外键名;
- 连接名 → `config/autoload/databases.php` 中的配置项;
- 绑定数组键 → SQL 字符串中的 `:placeholder`;
- 模型属性(`$fillable` 等)中的列名 → 模型表对应的列。

## 10. 悬停文档

- **列名悬停**(`where()`、`select()`、`orderBy()` 等方法的列参数上):显示该列的类型、是否可空、默认值与注释;
- **表名悬停**:显示轻量摘要(schema 限定名 + 表注释),**不渲染完整 DDL**(宽表上太慢);完整 DDL 可 Ctrl+Click 进入 Database 工具窗口查看;
- Raw SQL 片段中的列不提供悬停(有意为之)。

---

相关阅读:[相关配置](./configuration.md) | [适配性与已知限制](./compatibility.md)
