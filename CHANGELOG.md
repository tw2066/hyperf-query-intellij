<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# Hyperf Query Changelog

## Unreleased

## 1.0.5 - 2026-08-10

### Added
- Hover / quick documentation for column arguments of query builder methods (`where()`, `select()`, `orderBy()`, ...): hovering over a column name now shows the resolved database column's type, nullability, default value and comment
- Column name completion, Ctrl+Click navigation and hover documentation for Hyperf model properties: `$fillable`/`$guarded`/`$hidden`/`$visible`/`$dates` array values and `$casts` array keys (including plain strings not yet written as `key =>`) resolve against the model's table (read from the `$table` property or derived from the class name)
- Attribute cast type completion for `$casts` array values: `int`, `integer`, `real`, `float`, `double`, `string`, `bool`, `boolean`, `object`, `array`, `json`, `collection`, `date`, `datetime`, `timestamp`, plus parameterized forms `decimal:<digits>`, `date:<format>`, `datetime:<format>`

## 1.0.4 - 2026-08-08

### Added
- Completion of SQL named placeholders for `\Hyperf\DbConnection\Db` methods with a `$bindings` array (`select`, `selectOne`, `insert`, `update`, `delete`, `statement`, `affectingStatement`): binding array keys complete against the `:name` placeholders found in the query string, suggested both as `name` and `:name`. Placeholders already bound as keys are skipped. The query string may be an inline literal or a variable assignment (`$sql = '...'`)
- Navigation from a binding array key (`'id'` or `':id'`) to the corresponding placeholder in the query string for the same `Db` methods
- Column/table completion, references and unknown-element inspections are now suppressed on these `Db` SQL methods, where the query string and binding keys were previously treated as database columns

### Fixed
- Completion form follows the typed prefix: an empty key (`['' => ...]`) suggests only the plain `name` form, while a colon prefix (`[':' => ...]`) suggests only the `:name` form
- Completion now triggers on a bare empty element (`['']`) inside the bindings array, not only on an explicit `=>` key
- Navigation and completion resolve to the nearest `$sql = '...'` assignment before the call when the variable is reassigned, instead of always the first assignment
- Fixed a `StackOverflowError` in the background highlighter: resolving the SQL variable's assignment via a reference search recursively invoked this plugin's own reference provider. Assignment lookup now walks the PSI tree backwards from the call site, which is both recursion-free and resolves the nearest preceding assignment

## 1.0.3 - 2026-08-05

### Fixed
- Column completion and reference resolution now work inside `when()`/`when`-style closures even when other builder methods are chained before `when()` (e.g. `Model::query()->where(...)->when(..., function (Builder $query) { $query->where('<caret>'); })`). Previously the closure was only recognized when `when()` directly followed `Model::query()`

### Added
- Column completion for `insert()`, `insertGetId()` and `insertOrIgnore()` array keys, including batch (nested array) `insert()` calls. Keys complete only columns of the resolved model/table, matching the existing `update()` behavior
- Column completion for `first()` column array values (e.g. `Model::query()->first(['id', '<caret>'])`), matching the existing `get()` behavior

## 1.0.2 - 2026-08-04

### Fixed
- Settings panel showing placeholder text on PhpStorm 2023+: changed `projectConfigurable` parent from `language` to `reference.webide.settings.project.settings.php` so the "Hyperf Query" settings panel renders correctly

### Changed
- Adapted plugin for Hyperf 3.2: now recognizes `Hyperf\Database\*` query builder, model and schema builder classes instead of `Illuminate\Database\*`
- Renamed `LaravelUtils` to `HyperfUtils` and settings classes to `HyperfQuerySettings`
- Plugin display name changed to "Hyperf Query"

### Removed
- Removed Laravel `Illuminate\Foundation\Testing\TestCase` database assertion support (`assertDatabaseHas`, `assertDeleted`, etc.) which is not available in Hyperf
- Removed `\DB` and `\Schema` facade aliases which don't exist in Hyperf
