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
* Hover / quick documentation for query builder column arguments — hover over a column name in `where()`, `select()`, `orderBy()` and other builder methods to see the resolved database column's type, nullability, default value and comment; table names show a lightweight summary (schema + comment) instead of the full DDL
* Column name completion, Ctrl+Click navigation and hover documentation for model properties — `$fillable`, `$guarded`, `$hidden`, `$visible`, `$dates` array values and `$casts` array keys resolve against the model's table (from `$table` or the class name convention); `$casts` array values complete Hyperf attribute cast types (`integer`, `datetime`, `decimal:<digits>`, `date:<format>`, ...)
* SQL named-placeholder (`:name`) completion and navigation for `Db::select()`, `Db::update()` and other `Hyperf\DbConnection\Db` methods with `$bindings` — binding array keys complete against the placeholders in the query string (both `name` and `:name` forms), and Ctrl+Click on a key navigates to the placeholder in the SQL
* Database connection names from `config/autoload/databases.php` complete in `Db::connection()` / `Schema::connection()` and the model `$connection` property, with Ctrl+Click navigation to the config entry; table/column completion, references and inspections on a chain (or model) using a connection are scoped to that connection's configured `database` schema (falling back to the `default` connection when none is specified), and the connection's `prefix` is honored for table names
* Raw SQL fragments (`selectRaw()`, `whereRaw()`, `orderByRaw()`, `havingRaw()`, `groupByRaw()`, `Db::raw()`, ...) get column completion, navigation and inspection for simple column expressions — including comma-separated lists and table/alias references carrying the connection's table prefix — while complex SQL (functions, arithmetic) is left untouched to avoid false warnings
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
