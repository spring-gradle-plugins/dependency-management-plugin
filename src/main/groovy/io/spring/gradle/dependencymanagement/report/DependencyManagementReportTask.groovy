/*
 * Copyright 2014-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.spring.gradle.dependencymanagement.report

import io.spring.gradle.dependencymanagement.DependencyManagementContainer
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.tasks.TaskAction

import javax.inject.Inject
/**
 * Task to display the dependency management for a project.
 *
 * @author Andy Wilkinson.
 */
class DependencyManagementReportTask extends DefaultTask {

    DependencyManagementContainer dependencyManagement

    DependencyManagementReportRenderer renderer

    @Inject
    DependencyManagementReportTask() {
        this.renderer = new DependencyManagementReportRenderer();
    }

    DependencyManagementReportTask(DependencyManagementReportRenderer renderer) {
        this.renderer = renderer;
    }

    @TaskAction
    public void report() {
        this.renderer.startProject(project)

        def globalManagedVersions = this.dependencyManagement.managedVersionsForConfiguration(null)

        this.renderer.renderGlobalManagedVersions(globalManagedVersions)

        project.configurations.sort {
            a, b -> a.name.compareTo(b.name)
        }.each { Configuration c ->
            def managedVersions = this.dependencyManagement.managedVersionsForConfiguration(c)
            this.renderer.
                    renderConfigurationManagedVersions(managedVersions, c, globalManagedVersions)
        }
    }

}
