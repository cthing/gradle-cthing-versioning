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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.junit.jupiter.params.provider.Arguments.arguments;


public class PluginIntegTest {

    private Path projectDir;

    public static Stream<Arguments> gradleVersionProvider() {
        return Stream.of(
                arguments("8.0"),
                arguments("8.10.2")
        );
    }

    @BeforeEach
    public void setup() throws IOException {
        final Path baseDir = Path.of(System.getProperty("buildDir"), "integTest");
        Files.createDirectories(baseDir);
        this.projectDir = Files.createTempDirectory(baseDir, null);
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

        final BuildResult result = createGradleRunner(gradleVersion).build();
        final BuildTask versionTask = result.task(":version");
        assertThat(versionTask).isNotNull();
        assertThat(versionTask.getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
        assertThat(result.getOutput()).contains("1.2.3");
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

        final UnexpectedBuildFailure exception = catchThrowableOfType(UnexpectedBuildFailure.class,
                                                                      () -> createGradleRunner(gradleVersion).build());
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

        final UnexpectedBuildFailure exception = catchThrowableOfType(UnexpectedBuildFailure.class,
                                                                      () -> createGradleRunner(gradleVersion).build());
        final BuildResult result = exception.getBuildResult();
        assertThat(result.getOutput()).contains("Release build depends on snapshot artifact org.cthing:versionparser:4.+ (implementation)");
    }

    private GradleRunner createGradleRunner(final String gradleVersion) {
        return GradleRunner.create()
                           .withProjectDir(this.projectDir.toFile())
                           .withArguments("version")
                           .withPluginClasspath()
                           .withEnvironment(Map.of("CTHING_CI", "true"))
                           .withGradleVersion(gradleVersion);
    }
}
