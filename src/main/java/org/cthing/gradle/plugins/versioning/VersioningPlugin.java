/*
 * Copyright 2024 C Thing Software
 * SPDX-License-Identifier: Apache-2.0
 */

package org.cthing.gradle.plugins.versioning;

import java.util.Set;
import java.util.regex.Pattern;

import org.cthing.projectversion.ProjectVersion;
import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;


/**
 * Enforces semantic versioning of artifacts. When this plugin has been applied, project versions
 * and the type of build must be set using a {@link ProjectVersion} instance:
 * <pre>
 * version = ProjectVersion("1.2.3", BuildType.snapshot)
 * </pre>
 * <p>
 * The plugin also enforces that release builds only depend on release versions of C Thing Software
 * artifacts.
 * </p>
 */
public class VersioningPlugin implements Plugin<Project> {

    public static final String VERSION_TASK_NAME = "version";

    private static final Pattern SNAPHOT_VERSION_PATTERN = Pattern.compile(".*(?:\\+|-SNAPSHOT|-\\d+)$");
    private static final Set<String> CTHING_GROUPS = Set.of("com.cthing", "org.cthing");
    private static final Set<String> BUILD_CONFIGS = Set.of("api",
                                                            "compileOnly",
                                                            "compileOnlyApi",
                                                            "implementation",
                                                            "runtimeOnly");

    @Override
    public void apply(final Project project) {
        if (!"buildSrc".equals(project.getName())) {
            // Validate the version object
            project.afterEvaluate(proj -> {
                if (!(proj.getVersion() instanceof ProjectVersion)) {
                    throw new GradleException("Version is not an instance of org.cthing.projectversion.ProjectVersion");
                }
            });

            // Validate that a release build only depends on release build internal artifacts.
            validateReleaseDependencies(project);

            project.getTasks().register(VERSION_TASK_NAME, task -> {
                task.setGroup("Help");
                task.setDescription("Display project version number");
                task.doFirst(t -> System.out.println(project.getVersion()));
            });
        }
    }

    /**
     * A release build cannot depend on any snapshot C Thing Software artifacts for compilation or runtime.
     *
     * @param project  Project whose dependencies are to be validated.
     */
    private void validateReleaseDependencies(final Project project) {
        project.afterEvaluate(proj -> {
            if (((ProjectVersion)proj.getVersion()).isReleaseBuild()) {
                for (final Project childProject : proj.getAllprojects()) {
                    for (final Configuration config : childProject.getConfigurations()) {
                        if (BUILD_CONFIGS.contains(config.getName())) {
                            for (final Dependency dep : config.getDependencies()) {
                                final String version = dep.getVersion();
                                final String group = dep.getGroup();
                                if (CTHING_GROUPS.contains(group)
                                        && (version == null || SNAPHOT_VERSION_PATTERN.matcher(version).matches())) {
                                    proj.getLogger().error(
                                            "Release build depends on snapshot artifact {}:{}:{} ({})",
                                            group, dep.getName(), version, config.getName());
                                    throw new GradleException("Release build depends on snapshot artifacts");
                                }
                            }
                        }
                    }
                }
            }
        });
    }
}
