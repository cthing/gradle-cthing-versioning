# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [unreleased]

## [3.1.0] - 2025-10-04

### Changed

- Plugin configuration can now be cached

## [3.0.0] - 2024-12-01

### Added

- The plugin now generates the text file `build/projectversion.txt` containing the
  complete semantic version number of the plugin.

## [2.0.0] - 2024-10-26
 
- The plugin has been migrated from JSR 305 to [JSpecify](https://jspecify.dev/) for `null` checking

## [1.0.1] - 2024-06-21

### Changed

- Fix NPE due to dependency with no group name

## [1.0.0] - 2024-05-30

### Added

- First release

[unreleased]: https://github.com/cthing/gradle-cthing-versioning/compare/3.1.0...HEAD
[3.1.0]: https://github.com/cthing/gradle-cthing-versioning/releases/tag/3.1.0
[3.0.0]: https://github.com/cthing/gradle-cthing-versioning/releases/tag/3.0.0
[2.0.0]: https://github.com/cthing/gradle-cthing-versioning/releases/tag/2.0.0
[1.0.1]: https://github.com/cthing/gradle-cthing-versioning/releases/tag/1.0.1
[1.0.0]: https://github.com/cthing/gradle-cthing-versioning/releases/tag/1.0.0
