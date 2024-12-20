/*
 * Copyright 2024 C Thing Software
 * SPDX-License-Identifier: Apache-2.0
 */

package org.cthing.gradle.plugins.versioning;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Stream;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.BuildTask;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.gradle.testkit.runner.UnexpectedBuildFailure;
import org.gradle.util.GradleVersion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.junit.jupiter.params.provider.Arguments.arguments;


public class PluginIntegTest {
    private static final Path BASE_DIR = Path.of(System.getProperty("buildDir"), "integTest");
    private static final Path WORKING_DIR;

    static {
        try {
            Files.createDirectories(BASE_DIR);
            WORKING_DIR = Files.createTempDirectory(BASE_DIR, "working");
        } catch (final IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private Path projectDir;

    public static Stream<Arguments> gradleVersionProvider() {
        return Stream.of(
                arguments("8.0"),
                arguments(GradleVersion.current().getVersion())
        );
    }

    @BeforeEach
    public void setup() throws IOException {
        this.projectDir = Files.createTempDirectory(BASE_DIR, "project");
    }

    @ParameterizedTest
    @MethodSource("gradleVersionProvider")
    public void testGoodVersion(final String gradleVersion) throws IOException {
        Files.writeString(this.projectDir.resolve("settings.gradle.kts"), "rootProject.name=\"test\"");
        Files.writeString(this.projectDir.resolve("build.gradle.kts"), """
                import org.cthing.projectversion.BuildType
                import org.cthing.projectversion.ProjectVersion

                repositories {
                    mavenCentral()
                }

                plugins {
                    id("org.cthing.cthing-versioning")
                }

                buildscript {
                    repositories {
                        mavenCentral()
                    }
                    dependencies {
                        classpath("org.cthing:cthing-projectversion:1.0.0")
                    }
                }

                version = ProjectVersion("1.2.3", BuildType.snapshot)
                """);

        final BuildResult result = createGradleRunner(gradleVersion, "version").build();
        final BuildTask versionTask = result.task(":version");
        assertThat(versionTask).isNotNull();
        assertThat(versionTask.getOutcome()).as(result.getOutput()).isEqualTo(TaskOutcome.SUCCESS);
        assertThat(result.getOutput()).contains("1.2.3");
        assertThat(this.projectDir.resolve("build/projectversion.txt")).exists();
    }

    @ParameterizedTest
    @MethodSource("gradleVersionProvider")
    public void testStringVersion(final String gradleVersion) throws IOException {
        Files.writeString(this.projectDir.resolve("settings.gradle.kts"), "rootProject.name=\"test\"");
        Files.writeString(this.projectDir.resolve("build.gradle.kts"), """
                plugins {
                    id("org.cthing.cthing-versioning")
                }

                version = "1.2.3"
                """);

        final UnexpectedBuildFailure exception =
                catchThrowableOfType(UnexpectedBuildFailure.class,
                                     () -> createGradleRunner(gradleVersion, "version").build());
        final BuildResult result = exception.getBuildResult();
        assertThat(result.getOutput()).contains("Version is not an instance of org.cthing.projectversion.ProjectVersion");
    }

    @ParameterizedTest
    @MethodSource("gradleVersionProvider")
    public void testRelaseWithSnapshots(final String gradleVersion) throws IOException {
        Files.writeString(this.projectDir.resolve("settings.gradle.kts"), "rootProject.name=\"test\"");
        Files.writeString(this.projectDir.resolve("build.gradle.kts"), """
                import org.cthing.projectversion.BuildType
                import org.cthing.projectversion.ProjectVersion

                repositories {
                    mavenCentral()
                }

                plugins {
                    `java-library`
                    id("org.cthing.cthing-versioning")
                }

                buildscript {
                    repositories {
                        mavenCentral()
                    }
                    dependencies {
                        classpath("org.cthing:cthing-projectversion:1.0.0")
                    }
                }

                version = ProjectVersion("1.2.3", BuildType.release)

                dependencies {
                    implementation("org.cthing:versionparser:4.+")
                }
                """);

        final UnexpectedBuildFailure exception =
                catchThrowableOfType(UnexpectedBuildFailure.class,
                                     () -> createGradleRunner(gradleVersion, "version").build());
        final BuildResult result = exception.getBuildResult();
        assertThat(result.getOutput()).contains("Release build depends on snapshot artifact org.cthing:versionparser:4.+ (implementation)");
    }

    @ParameterizedTest
    @MethodSource("gradleVersionProvider")
    public void testVersionFile(final String gradleVersion) throws IOException {
        Files.writeString(this.projectDir.resolve("settings.gradle.kts"), "rootProject.name=\"test\"");
        Files.writeString(this.projectDir.resolve("build.gradle.kts"), """
                import org.cthing.projectversion.BuildType
                import org.cthing.projectversion.ProjectVersion

                repositories {
                    mavenCentral()
                }

                plugins {
                    id("org.cthing.cthing-versioning")
                }

                buildscript {
                    repositories {
                        mavenCentral()
                    }
                    dependencies {
                        classpath("org.cthing:cthing-projectversion:1.0.0")
                    }
                }

                version = ProjectVersion("1.2.3", BuildType.snapshot)
                """);

        final BuildResult result = createGradleRunner(gradleVersion, "projectVersionFile").build();
        final BuildTask versionFileTask = result.task(":projectVersionFile");
        assertThat(versionFileTask).isNotNull();
        assertThat(versionFileTask.getOutcome()).as(result.getOutput()).isEqualTo(TaskOutcome.SUCCESS);
        assertThat(this.projectDir.resolve("build/projectversion.txt")).exists();
    }

    @ParameterizedTest
    @MethodSource("gradleVersionProvider")
    public void testVersionFileCleanOnly(final String gradleVersion) throws IOException {
        Files.writeString(this.projectDir.resolve("settings.gradle.kts"), "rootProject.name=\"test\"");
        Files.writeString(this.projectDir.resolve("build.gradle.kts"), """
                import org.cthing.projectversion.BuildType
                import org.cthing.projectversion.ProjectVersion

                repositories {
                    mavenCentral()
                }

                plugins {
                    id("java")
                    id("org.cthing.cthing-versioning")
                }

                buildscript {
                    repositories {
                        mavenCentral()
                    }
                    dependencies {
                        classpath("org.cthing:cthing-projectversion:1.0.0")
                    }
                }

                version = ProjectVersion("1.2.3", BuildType.snapshot)
                """);

        final BuildResult result = createGradleRunner(gradleVersion, "clean").build();
        final BuildTask cleanTask = result.task(":clean");
        assertThat(cleanTask).isNotNull();
        assertThat(cleanTask.getOutcome()).as(result.getOutput()).isEqualTo(TaskOutcome.UP_TO_DATE);
        assertThat(this.projectDir.resolve("build/projectversion.txt")).doesNotExist();
    }

    private GradleRunner createGradleRunner(final String gradleVersion, final String... arguments) {
        return GradleRunner.create()
                           .withProjectDir(this.projectDir.toFile())
                           .withTestKitDir(WORKING_DIR.toFile())
                           .withArguments(arguments)
                           .withPluginClasspath()
                           .withEnvironment(Map.of("CTHING_CI", "true"))
                           .withGradleVersion(gradleVersion);
    }
}
