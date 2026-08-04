<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# Hyperf Query Changelog

## Unreleased

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
