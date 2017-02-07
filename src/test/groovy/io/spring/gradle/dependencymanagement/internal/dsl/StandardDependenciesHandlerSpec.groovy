/*
 * Copyright 2014-2017 the original author or authors.
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

package io.spring.gradle.dependencymanagement.internal.dsl

import io.spring.gradle.dependencymanagement.dsl.DependenciesHandler
import io.spring.gradle.dependencymanagement.internal.DependencyManagementContainer
import org.gradle.api.artifacts.Configuration
import spock.lang.Specification

/**
 * Tests for {@link StandardDependenciesHandler}.
 *
 * @author Andy Wilkinson
 */
class StandardDependenciesHandlerSpec extends Specification {

    DependencyManagementContainer container = Mock(DependencyManagementContainer)

    Configuration configuration = Mock(Configuration)

    DependenciesHandler handler = new StandardDependenciesHandler(container, configuration)

    def 'Dependency can be configured using a Map with GString values'() {
        when:
        def group = "group"
        def name = "name"
        def version = "1.0"
        handler.dependency "group": "$group", "name": "$name", "version": "$version"

        then:
        1 * container.addManagedVersion(configuration, "group", "name", "1.0", _)
    }

    def 'Dependency set can be configured using a Map with GString values'() {
        when:
        def group = "group"
        def version = "1.0"
        handler.dependencySet("group": "$group", "version": "$version") {
            entry "name"
        }

        then:
        1 * container.addManagedVersion(configuration, "group", "name", "1.0", _)
    }

}
