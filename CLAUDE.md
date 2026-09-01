# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

JetBrains IDE 插件(PhpStorm/DataGrip)，为 **Hyperf 3.2** 查询构建器提供数据库补全、检查与引用解析。它是原 `ekvedaras/laravel-query-intellij` 的适配分支，已将识别目标从 `Illuminate\Database\*` 全部迁移到 `Hyperf\Database\*`。

## 常用命令

```bash
# 编译主代码 + 测试代码(不跑测试)
./gradlew compileKotlin compileTestKotlin

# 完整构建(含插件校验、instrumentCode)
./gradlew build

# 跑全部测试
./gradlew test

# 跑单个测试类
./gradlew test --tests "dev.ekvedaras.hyperfquery.reference.SchemaTableReferenceTest"

# 跑单个测试方法
./gradlew test --tests "dev.ekvedaras.hyperfquery.reference.SchemaTableReferenceTest.testResolvesSchemaReference"

# 启动带插件的调试 IDE(对应 .run/Run IDE with Plugin.run.xml)
./gradlew runIde

# 打包插件(zip 产物在 build/distributions/)
./gradlew buildPlugin

# 插件兼容性验证
./gradlew verifyPlugin
```

**注意事项:**
- `gradle-wrapper.properties` 锁定 Gradle 8.5；若本机缓存不完整导致下载超时，可临时改用缓存版本(如 8.10.2)直接调用二进制:`~/.gradle/wrapper/dists/gradle-*/gradle-*/bin/gradle`
- 首次构建需联网下载 Kotlin 插件和 IntelliJ Platform SDK;`./gradlew test` 会用内存 H2/真实 MySQL dump 初始化数据源，耗时较长
- detekt 规则在 `detekt-config.yml`,CI 用 `verifyPlugin` + `test` 两个任务把关，无独立 lint 任务

## 高层架构

### 识别核心：`HyperfUtils.kt` + `HyperfClasses`

插件唯一的框架耦合点在 `src/main/kotlin/dev.ekvedaras.hyperfquery/utils/HyperfUtils.kt`:

- `object HyperfClasses` 定义了所有要识别的 Hyperf 类 FQN(`QueryBuilder`/`Model`/`SchemaFacade`/`DbFacade`/`Blueprint`/`JoinClause`/`Relation`/`ColumnDefinition`)
- `InterestingClasses` 列表决定哪些类会被触发补全/检查；`SchemaBuilderClasses` 决定哪些被视为 Schema Builder 方法
- 补全方法白名单(`BuilderTableMethods`/`BuilderTableColumnsParams`/`BuilderTableIndexesParams` 等)按方法名+参数位置声明列、索引、表应该在哪提示

改框架支持只需改这一个文件 + 测试 stub，不用动补全逻辑。

### 三大功能管线

1. **Completion** (`completion/`):在字符串字面量里触发补全
   - `ColumnCompletionContributor/Provider`:列名/表名/别名补全
   - `TableOrViewCompletionContributor/Provider`:表/视图/schema 补全
   - `IndexCompletionContributor/Provider`:索引/外键补全
   - `NewMigrationCompletionContributor/Provider`:migration 文件内的索引/列补全(仅 Hyperf Blueprint 场景)

2. **Inspection** (`inspection/`):未知表/列/视图告警
   - `UnknownTableOrViewInspection`、`UnknownColumnInspection`，逻辑与 completion 复用 `DbReferenceExpression`

3. **Reference** (`reference/`):Ctrl+Click 跳转到数据库元素
   - `ColumnPsiReference`/`TableOrViewPsiReference`/`SchemaPsiReference`/`IndexPsiReference` 等

### 表名/别名解析链

`DbReferenceExpression`(models/)→ `TableAndAliasCollector` → `AliasCollector`/`ModelRelationResolver`/`SchemaTableResolver` → `DbReferenceResolver`。负责从 `Db::table('users')`、`Model::query()`、join、relation 闭包等语境中提取表名和别名。

### 设置与 UI

- `services/HyperfQuerySettings.kt`:项目级 PersistentStateComponent，存 `filterDataSources`/`filteredDataSources`/`tablePrefix`/`ignoreSettings`
- `services/HyperfQuerySettingsConfigurable.kt` + `services/forms/HyperfQuerySettingsForm.kt/.form`:Settings 面板
- `Startup.kt`:检测 `HyperfClasses.QueryBuilder` 是否存在，提示配置数据源过滤

### 测试体系

- 测试基类 `BaseTestCase.kt`:每个测试前自动从 `src/test/resources/test-db.sql` 建两个 MySQL schema(`testProject1`/`testProject2`，含 users/customers/failed_jobs/migrations 表)，加载 `stubs.php`
- **关键约定**:`src/test/resources/stubs.php` 定义了插件所依赖的全部 Hyperf 类(Query Builder、Model、Schema Builder、Blueprint、JoinClause、Db、Schema 等)。测试代码不依赖真实 hyperf vendor，靠这个 stub 让 PhpStorm 解析类型。新增 Hyperf 类识别时必须同步更新 stubs.php
- 测试资源按场景分目录:`completion/`、`inspection/`、`reference/`、`schema/`、`edgeCases/`、`model/`

## 包名与命名遗留

- Java 包名、Gradle `pluginGroup`/`pluginName`/`rootProject.name` 已全部改为 `dev.ekvedaras.hyperfquery` / "Hyperf Query" / "hyperf-query"
- 插件 ID 为 `dev.ekvedaras.hyperfquery`，显示名 "Hyperf Query"
- 类名已统一为 `HyperfUtils`/`HyperfClasses`/`HyperfQuerySettings*`；不再有 `LaravelUtils`/`LaravelQuerySettings`

## 已移除的 Laravel 专属能力

- `Illuminate\Foundation\Testing\TestCase` 数据库断言补全(`assertDatabaseHas`/`assertDeleted` 等)——Hyperf 无此测试基类
- `\DB`/`\Schema` 全局 facade 别名——Hyperf 没有这种别名注册机制
- 相关测试用例与 stubs.php 中的 `Tests\TestCase`/`InteractsWithDatabase` 存根

## 新增/修改时的关键约束

1. **新增 Hyperf 类识别**：改 `HyperfClasses` + `InterestingClasses`/`SchemaBuilderClasses`，并在 `src/test/resources/stubs.php` 中补充同名同包 stub，否则测试无法解析
2. **设置面板 parentId**：`projectConfigurable` 的 `parentId` 必须用 `reference.webide.settings.project.settings.php`（官方 Hyperf 插件同款）。`parentId="language"` 在 PhpStorm 2023+ 上会导致设置面板显示占位文案（`createComponent()` 不被调用）
2. **stubs.php 的 Schema 继承链**:`namespace Hyperf\Database\Schema { class Builder {...} }` → `namespace Hyperf\DbConnection { class Schema extends \Hyperf\Database\Schema\Builder {} }` → `namespace { class Schema extends \Hyperf\DbConnection\Schema {} }`，三层链不能断，否则 `Schema::table()` 解析失败
3. **测试偏移量**：很多 reference 测试断言硬编码 `navigationRange.startOffset` 数值(如 51/78/103)，改 stubs.php 中类名长度后要同步更新这些断言
4. **PLUGIN_DESCRIPTION.md** 的内容会被 `patchPluginXml` 整文件读入并打进插件市场描述(以中文为主),改功能特性时同步更新;README.md 不再含 `<!-- Plugin description -->` 区块
5. **CHANGELOG.md** 由 gradle-changelog 插件读取 `## Unreleased` 区块生成发布说明
