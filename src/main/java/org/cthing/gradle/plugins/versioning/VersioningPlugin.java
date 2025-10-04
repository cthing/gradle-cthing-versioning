/*
 * Copyright 2024 C Thing Software
 * SPDX-License-Identifier: Apache-2.0
 */

package org.cthing.gradle.plugins.versioning;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import javax.inject.Inject;

import org.cthing.projectversion.ProjectVersion;
import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.file.ProjectLayout;
import org.gradle.api.plugins.BasePlugin;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.TaskExecutionException;


/**
 * Enforces semantic versioning of artifacts. When this plugin has been applied, project versions
 * and the type of build must be set using a {@link ProjectVersion} instance:
 * <pre>
 * version = ProjectVersion("1.2.3", BuildType.snapshot)
 * </pre>
 *
 * <p>
 * The plugin also enforces that release builds only depend on release versions of C Thing Software
 * artifacts.
 * </p>
 */
public class VersioningPlugin implements Plugin<Project> {

    public static final String VERSION_TASK_NAME = "version";

    private static final String VERSION_FILE_TASK_NAME = "projectVersionFile";
    private static final String VERSION_FILE_TASK_PATH = ":" + VERSION_FILE_TASK_NAME;
    private static final Pattern SNAPHOT_VERSION_PATTERN = Pattern.compile(".*(?:\\+|-SNAPSHOT|-\\d+)$");
    private static final String PROJECT_VERSION_FILENAME = "projectversion.txt";
    private static final Set<String> CTHING_GROUPS = Set.of("com.cthing", "org.cthing");
    private static final Set<String> BUILD_CONFIGS = Set.of("api",
                                                            "compileOnly",
                                                            "compileOnlyApi",
                                                            "implementation",
                                                            "runtimeOnly");
    private static final List<String> BUILD_RELATED_FILES = List.of("gradle.properties",
                                                                    "settings.gradle",
                                                                    "settings.gradle.kts",
                                                                    "gradle/libs.versions.toml");

    private final ProjectLayout projectLayout;

    @Inject
    public VersioningPlugin(final ProjectLayout projectLayout) {
        this.projectLayout = projectLayout;
    }

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

            // Create the task that writes the version file.
            createVersionFileTask(project);

            // Create the task that displays the version.
            createVersionTask(project);
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
                                if (group != null && CTHING_GROUPS.contains(group)
                                        && (version == null || SNAPHOT_VERSION_PATTERN.matcher(version).matches())) {
                                    proj.getLogger().error("Release build depends on snapshot artifact {}:{}:{} ({})",
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

    /**
     * Create the task that writes the version file.
     *
     * @param project Project whose version is to be written
     */
    private void createVersionFileTask(final Project project) {
        // Only write the file if this is the root project and "clean" is not the only task.
        if (project.equals(project.getRootProject()) && isNotCleanOnly(project)) {
            final TaskContainer tasks = project.getTasks();
            final Set<Project> projects = project.getAllprojects();
            final Project rootProject = project.getRootProject();

            tasks.register(VERSION_FILE_TASK_NAME, task -> {
                final File buildDir = this.projectLayout.getBuildDirectory().get().getAsFile();
                final File projectVersionFile = new File(buildDir, PROJECT_VERSION_FILENAME);

                // The project version file is the output
                task.getOutputs().file(projectVersionFile);

                // Build related files are the inputs.
                projects.forEach(proj -> task.getInputs().file(proj.getBuildFile()));
                BUILD_RELATED_FILES.forEach(filename -> {
                    final File file = this.projectLayout.getProjectDirectory().file(filename).getAsFile();
                    if (file.exists()) {
                        task.getInputs().file(file);
                    }
                });

                // If "clean" is one of the tasks, generate the project version file after it has run
                if (tasks.findByName(BasePlugin.CLEAN_TASK_NAME) != null) {
                    task.mustRunAfter(BasePlugin.CLEAN_TASK_NAME);
                }

                task.doLast(t -> {
                    try {
                        Files.writeString(projectVersionFile.toPath(),
                                          rootProject.getVersion().toString(),
                                          StandardCharsets.UTF_8);
                    } catch (final IOException ex) {
                        throw new TaskExecutionException(task, ex);
                    }
                });
            });

            // Ensure that the task is always considered for execution.
            final List<String> taskNames = new ArrayList<>(project.getGradle().getStartParameter().getTaskNames());
            taskNames.add(VERSION_FILE_TASK_PATH);
            project.getGradle().getStartParameter().setTaskNames(taskNames);
        }
    }

    /**
     * Creates the task to display the project version.
     *
     * @param project Project whose version is to be displayed
     */
    private void createVersionTask(final Project project) {
        project.getTasks().register(VERSION_TASK_NAME, task -> {
            task.setGroup("Help");
            task.setDescription("Display project version number");
            task.doFirst(t -> System.out.println(project.getVersion()));
        });
    }

    /**
     * Indicates whether the build will run more than just the 'clean' task.
     *
     * @param project  Root project to test for tasks
     * @return {@code true} if the there are no tasks specified (i.e. use default tasks) or the tasks include more
     *      than just "clean".
     */
    private static boolean isNotCleanOnly(final Project project) {
        final List<String> taskNames = project.getGradle().getStartParameter().getTaskNames();
        return taskNames.isEmpty()
                || taskNames.stream().anyMatch(name -> !BasePlugin.CLEAN_TASK_NAME.equals(name));
    }
}
