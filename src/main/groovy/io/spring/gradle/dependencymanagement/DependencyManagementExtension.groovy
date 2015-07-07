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

package io.spring.gradle.dependencymanagement

import io.spring.gradle.dependencymanagement.maven.PomDependencyManagementConfigurer
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration

/**
 * Extension object that provides the dependency management plugin's DSL
 *
 * @author Andy Wilkinson
 */
class DependencyManagementExtension {

    protected DependencyManagementContainer dependencyManagementContainer

    protected Project project

    boolean applyMavenExclusions = true

	boolean dependenciesOverrideDependencyManagement = true

    PomCustomizationConfiguration generatedPomCustomization = new PomCustomizationConfiguration()

    DependencyManagementExtension(DependencyManagementContainer dependencyManagementContainer,
            Project project) {
        this.dependencyManagementContainer = dependencyManagementContainer
        this.project = project
    }

    void imports(Closure closure) {
        new DependencyManagementHandler(dependencyManagementContainer).imports(closure)
    }

    void dependencies(Closure closure) {
        new DependencyManagementHandler(dependencyManagementContainer)
                .dependencies(closure)
    }

    void resolutionStrategy(Closure closure) {
        this.dependencyManagementContainer.resolutionStrategy closure
    }

    void generatedPomCustomization(Closure closure) {
        closure.delegate = this.generatedPomCustomization
        closure.resolveStrategy = Closure.DELEGATE_FIRST
        closure.call()
    }

    def getManagedVersions() {
        dependencyManagementContainer.managedVersionsForConfiguration(null)
    }

    def getPomConfigurer() {
        new PomDependencyManagementConfigurer(dependencyManagementContainer
                .globalDependencyManagement, generatedPomCustomization)
    }

    Properties getImportedProperties() {
        dependencyManagementContainer.importedPropertiesForConfiguration(null)
    }

    def methodMissing(String name, args) {
        Closure closure
        if ("configurations" == name) {
            closure = args.last()
            List handlers = args.take(args.size() - 1)
            closure.delegate = new CompoundDependencyManagementHandler(handlers)
        }
        else {
            Configuration configuration = project.configurations.getAt(name)
            closure = args[0]
            closure.delegate = new DependencyManagementHandler(dependencyManagementContainer,
                    configuration)
        }

        closure.resolveStrategy = Closure.DELEGATE_ONLY
        closure.call()
    }

    def propertyMissing(String name) {
        return forConfiguration(name)
    }

	DependencyManagementHandler forConfiguration(String name) {
		new DependencyManagementHandler(dependencyManagementContainer,
			project.configurations.getAt(name))
	}

    static class PomCustomizationConfiguration {

        static enum ImportedBomAction {
            IMPORT,
            COPY
        }

        boolean enabled = true;

        ImportedBomAction importedBomAction = ImportedBomAction.IMPORT

        void setImportedBomAction(String importedBomAction) {
            this.importedBomAction = ImportedBomAction.valueOf(importedBomAction.toUpperCase())
        }
    }
}
