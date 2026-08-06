<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# Hyperf Query Changelog

## Unreleased

## 1.0.3 - 2026-08-05

### Fixed
- Column completion and reference resolution now work inside `when()`/`when`-style closures even when other builder methods are chained before `when()` (e.g. `Model::query()->where(...)->when(..., function (Builder $query) { $query->where('<caret>'); })`). Previously the closure was only recognized when `when()` directly followed `Model::query()`

### Added
- Column completion for `insert()`, `insertGetId()` and `insertOrIgnore()` array keys, including batch (nested array) `insert()` calls. Keys complete only columns of the resolved model/table, matching the existing `update()` behavior

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
