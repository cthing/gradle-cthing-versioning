/*
 * Copyright 2024 C Thing Software
 * SPDX-License-Identifier: Apache-2.0
 */

package org.cthing.gradle.plugins.versioning;

import java.io.File;

import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;


public class PluginApplyTest {

    @Test
    public void testApply(@TempDir final File projectDir) {
        final Project project = ProjectBuilder.builder().withName("testProject").withProjectDir(projectDir).build();
        project.getPluginManager().apply("org.cthing.cthing-versioning");

        final Task versionTask = project.getTasks().findByName("version");
        assertThat(versionTask).isNotNull();
    }
}
