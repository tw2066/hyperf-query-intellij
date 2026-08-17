# Hyperf Query

<!-- Plugin description -->
This plugin provides database integration for Hyperf query builder. It works with DataGrip to provide autocompletion for database schemas, tables, views, and columns.

This is a fork of [laravel-query-intellij](https://github.com/ekvedaras/laravel-query-intellij) (MIT licensed), adapted to target `Hyperf\Database\*` instead of `Illuminate\Database\*`.

## Features

* Schemas, tables, views and columns completion for query and schema builder methods
* Completion for migrations
* Inspection of unknown database elements
* Table alias support
* Table name resolving from model for builder methods
* Model relation table name resolving for builder relation closure methods
* Text linking with database elements for navigation and refactoring
* Hover / quick documentation for query builder column arguments — hover over a column name in `where()`, `select()`, `orderBy()` and other builder methods to see the resolved database column's type, nullability, default value and comment
* Column name completion, Ctrl+Click navigation and hover documentation for model properties — `$fillable`, `$guarded`, `$hidden`, `$visible`, `$dates` array values and `$casts` array keys resolve against the model's table (from `$table` or the class name convention); `$casts` array values complete Hyperf attribute cast types (`integer`, `datetime`, `decimal:<digits>`, `date:<format>`, ...)
* SQL named-placeholder (`:name`) completion and navigation for `Db::select()`, `Db::update()` and other `Hyperf\DbConnection\Db` methods with `$bindings` — binding array keys complete against the placeholders in the query string (both `name` and `:name` forms), and Ctrl+Click on a key navigates to the placeholder in the SQL
* Configurable table prefix and datasource filtering
<!-- Plugin description end -->

## Installation

- Using IDE built-in plugin system:

  <kbd>Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> > <kbd>Search for "Hyperf Query"</kbd> >
  <kbd>Install Plugin</kbd>

- Manually:

  Download the [latest release](https://github.com/tw2066/hyperf-query-intellij/releases/latest) and install it manually using
  <kbd>Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>

---
Plugin based on the [IntelliJ Platform Plugin Template][template].

[template]: https://github.com/JetBrains/intellij-platform-plugin-template
