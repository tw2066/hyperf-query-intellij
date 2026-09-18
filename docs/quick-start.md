# 快速入门

本指南带你在几分钟内完成安装并验证插件生效。

## 1. 确认环境

| 依赖 | 要求 |
| --- | --- |
| IDE | PhpStorm 2022.2 或更高版本(或装了 PHP 插件 + Database Tools 插件的 IntelliJ IDEA Ultimate) |
| IDE 插件 | 必须启用 **Database Tools and SQL** 插件 |
| 项目 | Hyperf,且 `vendor` 目录被 IDE 正常索引 |

完整的版本与依赖矩阵见[适配性](./compatibility.md)。

## 2. 安装插件

- **IDE 内安装(推荐)**
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> > 搜索 **"Hyperf Query"** > <kbd>Install</kbd>

- **手动安装**
  从 [Releases](https://github.com/tw2066/hyperf-query-intellij/releases/latest) 下载最新 zip,然后
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install Plugin from Disk...</kbd>

安装后**重启 IDE**。

## 3. 配置数据源

插件不读取项目里的 `.env`,所有数据库元数据均来自 IDE 数据源:

1. 打开 IDE 右侧的 <kbd>Database</kbd> 工具窗口;
2. 点击 <kbd>+</kbd> 添加数据源(如 MySQL),填写连接信息并测试连通;
3. 在数据源上右键 > <kbd>Refresh</kbd>(或按 <kbd>Ctrl+F5</kbd>),确保目标 schema 已加载出表结构。

> 项目连了多个数据源时,建议随后开启[数据源过滤](./configuration.md#filter-data-sources),避免补全列表混入无关库。

## 4. 打开 Hyperf 项目

插件启动时检测项目中是否存在 `Hyperf\Database\Query\Builder`(即 Hyperf 依赖已被索引):

- 检测到且你尚未配置数据源过滤时,会弹出一次通知,引导你进入设置面板(<kbd>Settings</kbd> > <kbd>PHP</kbd> > <kbd>Hyperf Query</kbd>);
- 该通知**每个项目最多显示一次**,插件停用期间不再弹出。

## 5. 三步验证

在任意 Hyperf 代码中验证以下三点,全部生效即安装成功:

```php
use Hyperf\DbConnection\Db;

// ① 输入表名时出现补全列表
Db::table('');        // 光标置于引号内,应列出 users 等表

// ② Ctrl+Click 表名,跳转到 Database 工具窗口中的表
Db::table('users')->get();

// ③ 悬停在列名上,显示该列的类型/可空/默认值/注释
Db::table('users')->where('status', 1);
```

## 6. 无补全?排错速查

| 症状 | 排查 |
| --- | --- |
| 完全没有补全 | 数据源是否已添加并 <kbd>Refresh</kbd>;schema 是否展开能看到表 |
| 完全没有补全 | vendor 目录是否被排除(Mark Directory as Excluded)或依赖未 `composer install` |
| 完全没有补全 | <kbd>Settings</kbd> > <kbd>PHP</kbd> > <kbd>Hyperf Query</kbd> 中 **Enable Hyperf Query** 是否被关闭 |
| 候选列表里有别的库 | 开启 [Filter data sources](./configuration.md#filter-data-sources),只勾选本项目 schema |
| 补全的不是当前连接的表 | 检查模型 `$connection` / `Db::connection()` 指向的连接配置,见[连接作用域](./features.md#5-数据库连接感知) |
| 表名前缀解析不对 | 见[表前缀优先级](./configuration.md#table-prefix):连接配置 `prefix` 优先于插件全局前缀 |
| 数据库刚改过结构 | 在数据源上 <kbd>Refresh</kbd>,插件基于 IDE 缓存取元数据 |

更多细节:[功能详解](./features.md) | [相关配置](./configuration.md)
