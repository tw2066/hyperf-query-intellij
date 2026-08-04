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
* Configurable table prefix and datasource filtering

## Prerequisites

### Connect your database
See <https://www.jetbrains.com/help/phpstorm/connecting-to-a-database.html#connect-to-mysql-database> for instructions.

### Hyperf tools

You need [hyperf/ide-helper](https://github.com/hyperf/hyperf-ide-helper) added to your project and run
```shell
composer require hyperf/ide-helper --dev
php bin/hyperf.php ide-helper:generate
```
which will generate a `.phpstorm.meta.php` helper file so your IDE could see Hyperf database builder methods.

Hyperf Query plugin needs it to work otherwise, it cannot understand for which methods to trigger autocompletion.
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
