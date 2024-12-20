# ![C Thing Software](https://www.cthing.com/branding/CThingSoftware-57x60.png "C Thing Software") gradle-cthing-versioning

[![CI](https://github.com/cthing/gradle-cthing-versioning/actions/workflows/ci.yml/badge.svg)](https://github.com/cthing/gradle-cthing-versioning/actions/workflows/ci.yml)
[![Portal](https://img.shields.io/gradle-plugin-portal/v/org.cthing.cthing-versioning?label=Plugin%20Portal&logo=gradle)](https://plugins.gradle.org/plugin/org.cthing.cthing-versioning)

A Gradle plugin that establishes the versioning scheme for C Thing Software projects. The plugin
enforces the following:

* The project version is an instance of the ProjectVersion class
* Release builds do not depend on snapshot versions of C Thing Software artifacts

When applied to the root project, the plugin generates the file `build/projectversion.txt`
containing the complete semantic version of the project.

The plugin provides the `version` task, which displays the complete semantic version of the
project on the standard output.

## Usage

The plugin is available from the
[Gradle Plugin Portal](https://plugins.gradle.org/plugin/org.cthing.cthing-versioning) and can be
applied to a Gradle project using the `plugins` block:

```kotlin
buildscript {
    repositories {
        mavenCentral()
    }
}

plugins {
  id("org.cthing.cthing-versioning") version "3.0.0"
}
```

## Compatibility

The following Gradle and Java versions are supported:

| Plugin Version | Gradle Version | Minimum Java Version |
|----------------|----------------|----------------------|
| 3.0.0+         | 8.0+           | 17                   |

## Building

The plugin is compiled for Java 17. If a Java 17 toolchain is not available, one will be downloaded.

Gradle is used to build the plugin:
```bash
./gradlew build
```
The Javadoc for the plugin can be generated by running:
```bash
./gradlew javadoc
```

## Releasing

This project is released on the [Gradle Plugin Portal](https://plugins.gradle.org/plugin/org.cthing.cthing-versioning).
Perform the following steps to create a release.

- Commit all changes for the release
- In the `build.gradle.kts` file, edit the `ProjectVersion` object
    - Set the version for the release. The project follows [semantic versioning](https://semver.org/).
    - Set the build type to `BuildType.release`
- Commit the changes
- Wait until CI successfully builds the release candidate
- Verify GitHub Actions build is successful
- In a browser go to the C Thing Software Jenkins CI page
- Run the `gradle-cthing-versioning-validate` job
- Wait until that job successfully completes
- Run the `gradle-cthing-versioning-release` job to release the plugin to the Gradle Plugin Portal
- Wait for the plugin to be reviewed and made available by the Gradle team
- In a browser, go to the project on GitHub
- Generate a release with the tag `<version>`
- In the build.gradle.kts file, edit the `ProjectVersion` object
    - Increment the version patch number
    - Set the build type to `BuildType.snapshot`
- Update the `CHANGELOG.md` with the changes in the release and prepare for next release changes
- Update the `Usage` and `Compatibility` sections in the `README.md` with the latest artifact release version
- Commit these changes
