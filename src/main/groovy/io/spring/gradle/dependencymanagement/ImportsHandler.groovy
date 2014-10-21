/*
 * Copyright 2014 the original author or authors.
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
        container.importBom(configuration, coordinates)
    }

    def propertyMissing(String name) {
        return container.project.property(name)
    }
}
