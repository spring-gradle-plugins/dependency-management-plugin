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

package io.spring.gradle.dependencymanagement

import org.gradle.api.GradleException
import org.gradle.api.InvalidUserDataException
import org.gradle.api.artifacts.Configuration
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Internal handler for the {@code dependencies} block of the dependency management DSL
 *
 * @author Andy Wilkinson
 */
class DependenciesHandler {

    private final Logger log = LoggerFactory.getLogger(DependenciesHandler)

    private final DependencyManagementContainer container

    private final Configuration configuration

    DependenciesHandler(DependencyManagementContainer container, Configuration configuration) {
        this.container = container
        this.configuration = configuration
    }

    def dependencySet(Map setSpecification, Closure closure) {
        def group = setSpecification['group']
        def version = setSpecification['version']

        if (hasText(group) && hasText(version)) {
            closure.setResolveStrategy(Closure.DELEGATE_FIRST)
            closure.delegate = new DependencySetHandler(group, version)
            closure.call()
        }
        else {
            throw new GradleException("A dependency set requires both a group and a version")
        }
    }

    private def hasText(String string) {
        return string != null && string.trim().length() > 0
    }

    def dependency(def id) {
        dependency(id, null)
    }

    def dependency(def id, Closure closure) {
        if (id instanceof CharSequence) {
            String[] components = id.split(':')
            if (components.length == 3) {
                configureDependency(components[0], components[1], components[2], closure)
            }
            else {
                throw new InvalidUserDataException("Dependency identifier '$id' is malformed." +
                        " Required form is 'group:name:version'");
            }
        }
        else {
            def missingAttributes = ['group', 'name', 'version'];
            missingAttributes.removeAll(id.keySet());
            if (missingAttributes) {
                throw new InvalidUserDataException("Dependency identifier '$id' did not " +
                        "specify ${missingAttributes.join(', ')}");
            }
            else {
                configureDependency(id['group'], id['name'], id['version'], closure)
            }
        }
    }

    private def configureDependency(String group, String name, String version, Closure closure) {
        def excludeHandler = new DependencyExcludeHandler()
        if (closure) {
            closure.delegate = excludeHandler;
            closure.call()
        }
        container.addExplicitManagedVersion(configuration, group, name, version,
                excludeHandler.exclusions)
    }

    def propertyMissing(String name) {
        this.container.project.property(name)
    }

    private class DependencySetHandler {

        private final String group

        private final String version

        DependencySetHandler(String group, String version) {
            this.group = group
            this.version = version
        }

        def entry(String module) {
            entry(module, null)
        }

        def entry(String module, Closure closure) {
            def excludeHandler = new DependencyExcludeHandler()
            if (closure) {
                closure.delegate = excludeHandler;
                closure.call();
            }
            container.addExplicitManagedVersion(configuration, group, module, version,
                    excludeHandler.exclusions)
        }
    }

    private class DependencyExcludeHandler {

        def exclusions = []

        def exclude(String exclusion) {
            exclusions << exclusion
        }

        def exclude(Map exclusion) {
            exclusions << exclusion['group'] + ':' + exclusion['name']
        }
    }
}
