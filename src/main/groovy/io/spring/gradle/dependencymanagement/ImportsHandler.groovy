/*
 * Copyright 2014 the original author or authors.
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

import org.gradle.api.artifacts.Configuration

/**
 * Internal handler for the {@code imports} block of the dependency management DSL
 *
 * @author Andy Wilkinson
 */
class ImportsHandler {

    private final DependencyManagementContainer container

    private final Configuration configuration

    ImportsHandler(DependencyManagementContainer container, Configuration configuration) {
        this.container = container
        this.configuration = configuration
    }

    void mavenBom(String coordinates) {
        this.mavenBom(coordinates, null)
    }

    void mavenBom(String coordinates, Closure closure) {
        BomImport bomImport = new BomImport()
        if (closure) {
            closure.delegate = bomImport
            closure.resolveStrategy = Closure.DELEGATE_ONLY
            closure.call()
        }
        container.importBom(configuration, coordinates, bomImport.bomProperties)
    }

    def propertyMissing(String name) {
        return container.project.property(name)
    }

    private class BomImport {

        private Map<String, String> bomProperties = new HashMap<String, String>();

        void bomProperty(String name, String value) {
            this.bomProperties.put(name, value);
        }

        void bomProperties(Map<String, String> properties) {
            this.bomProperties.putAll(properties);
        }

    }

}
