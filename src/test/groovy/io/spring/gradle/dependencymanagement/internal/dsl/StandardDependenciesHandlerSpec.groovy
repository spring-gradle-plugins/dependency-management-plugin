/*
 * Copyright 2014-2019 the original author or authors.
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
import org.gradle.api.GradleException
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

    def 'Dependency configured with an empty artifactId is rejected'() {
        when: 'A dependency is configured with not artifactId'
        handler.dependency 'com.example::1.0'

        then: 'An exception with an appropriate message is thrown'
        def ex = thrown(GradleException)
        ex.message == "Dependency identifier 'com.example::1.0' is malformed. The required form is 'group:name:version'"
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

    def 'A dependency set configured using a map without a group is rejected'() {
        when: 'A dependency set is declared with a version but not a group'
        this.handler.dependencySet(version: '1.7.7') {}

        then: 'An exception with an appropriate message is thrown'
        def ex = thrown(GradleException)
        ex.message == 'A dependency set requires both a group and a version'
    }

    def 'A dependency set configured using a map without a version is rejected'() {
        when: 'A dependency set is declared with a group but not a version'
        this.handler.dependencySet(group: 'com.example') {}

        then: 'An exception with an appropriate message is thrown'
        def ex = thrown(GradleException)
        ex.message == 'A dependency set requires both a group and a version'
    }

    def 'An exclusion using a string in the wrong format is rejected'() {
        when: 'A dependency exclusion is configured as a string that is not in the required groupId:artifactId format'
        this.handler.dependency('com.example:example:1.0') {
            it.exclude('malformed')
        }
        then: 'An exception with an appropriate message is thrown'
        def ex = thrown(GradleException)
        ex.message == "Exclusion 'malformed' is malformed. The required form is 'group:name'"
    }

    def 'An exclusion configured using a map without a name is rejected'() {
        when: 'A dependency exclusion is configured with a group and no name'
        this.handler.dependency('com.example:example:1.0') {
            it.exclude(group:'com.example')
        }
        then: 'An exception with an appropriate message is thrown'
        def ex = thrown(GradleException)
        ex.message == 'An exclusion requires both a group and a name'
    }

    def 'An exclusion configured using a map without a group is rejected'() {
        when: 'A dependency exclusion is configured with a name and no group'
        this.handler.dependency('com.example:example:1.0') {
            it.exclude(name:'com.example')
        }
        then: 'An exception with an appropriate message is thrown'
        def ex = thrown(GradleException)
        ex.message == 'An exclusion requires both a group and a name'
    }

}
