/*
 * Copyright 2014-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.spring.gradle.dependencymanagement.report

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.logging.StyledTextOutput

import static org.gradle.logging.StyledTextOutput.Style.Description
import static org.gradle.logging.StyledTextOutput.Style.Header
import static org.gradle.logging.StyledTextOutput.Style.Identifier
import static org.gradle.logging.StyledTextOutput.Style.Normal

/**
 * @author Andy Wilkinson.
 */
class DependencyManagementReportRenderer {

    private StyledTextOutput output;

    DependencyManagementReportRenderer(StyledTextOutput output) {
        this.output = output
    }

    void startProject(Project project) {
        output.println()
        output.println("------------------------------------------------------------")
        String heading
        if (project.getRootProject() == project) {
            heading = "Root project"
        }
        else {
            heading = "Project ${project.path}"
        }
        if (project.description) {
            heading += " - ${project.description}"
        }
        output.println(heading)
        output.withStyle(Header).text("------------------------------------------------------------")
        output.println().println()
    }

    void renderGlobalManagedVersions(def globalManagedVersions) {
        renderDependencyManagementHeader("global", "Default dependency management for all configurations")

        if (globalManagedVersions) {
            renderManagedVersions(globalManagedVersions)
        }
        else {
            output.withStyle(Description).text("No dependency management")
            output.println().println()
        }
    }

    private void renderDependencyManagementHeader(String identifier, String description) {
        output.withStyle(Identifier).text(identifier)
        output.withStyle(Description).text(" - ${description}")
        output.println()
    }

    private void renderManagedVersions(def managedVersions) {
        managedVersions.sort { a, b ->
            def (String groupA, String moduleA) = a.key.split(':')
            def (String groupB, String moduleB) = b.key.split(':')
            def result = groupA.compareTo(groupB)
            if (!result) {
                result = moduleA.compareTo(moduleB)
            }
            return result
        } .each { dependency, version ->
            output.withStyle(Normal).text("    ${dependency} ${version}")
            output.println()
        }
        output.println()
    }


    void renderConfigurationManagedVersions(def managedVersions, Configuration configuration,
            def globalManagedVersions) {
        renderDependencyManagementHeader(configuration.name, "Dependency management for the " +
                "${configuration.name} configuration")

        if (managedVersions) {
            if (managedVersions != globalManagedVersions) {
                renderManagedVersions(managedVersions)
            }
            else {
                output.withStyle(Description).text("No configuration-specific dependency " +
                        "management")
                output.println().println()
            }
        } else {
            output.withStyle(Description).text("No dependency management")
            output.println().println()
        }
    }
}
