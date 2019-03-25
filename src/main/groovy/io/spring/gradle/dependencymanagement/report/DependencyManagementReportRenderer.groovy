/*
 * Copyright 2014-2015 the original author or authors.
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

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration

/**
 * @author Andy Wilkinson.
 */
class DependencyManagementReportRenderer {

    private PrintWriter output

    DependencyManagementReportRenderer() {
        this(new PrintWriter(System.out))
    }

    protected DependencyManagementReportRenderer(PrintWriter writer) {
        this.output = writer;
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
        output.println("------------------------------------------------------------")
        output.println()
    }

    void renderGlobalManagedVersions(def globalManagedVersions) {
        renderDependencyManagementHeader("global", "Default dependency management for all configurations")

        if (globalManagedVersions) {
            renderManagedVersions(globalManagedVersions)
        }
        else {
            output.println("No dependency management")
            output.println()
        }
        output.flush()
    }

    private void renderDependencyManagementHeader(String identifier, String description) {
        output.println("$identifier - $description")
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
            output.println("    $dependency $version")
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
                output.println("No configuration-specific dependency management")
                output.println()
            }
        } else {
            output.println("No dependency management")
            output.println()
        }
        output.flush()
    }
}
