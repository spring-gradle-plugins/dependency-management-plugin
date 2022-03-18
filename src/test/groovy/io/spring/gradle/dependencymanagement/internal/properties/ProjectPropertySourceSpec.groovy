/*
 * Copyright 2014-2022 the original author or authors.
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

package io.spring.gradle.dependencymanagement.internal.properties;

import java.io.File;

import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import spock.lang.Specification;

import io.spring.gradle.dependencymanagement.internal.DependencyManagementConfigurationContainer;
import io.spring.gradle.dependencymanagement.internal.maven.MavenPomResolver;

/**
 * Tests for {@link ProjectPropertySource}
 *
 * @author Andy Wilkinson
 */
public class ProjectPropertySourceSpec extends Specification {

    Project project

    ProjectPropertySource propertySource

    def setup() {
        this.project = new ProjectBuilder().build()
        this.propertySource = new ProjectPropertySource(this.project)
    }

    def 'Null is returned when project does not have a property'() {
        when: 'A non-existent property is retrieved'
        def property = propertySource.getProperty("does.not.exist")
        then: 'The property is null'
        property == null
    }

    def 'Property is returned when project has property'() {
        given: 'A project with a property'
        this.project.extensions.extraProperties.setProperty("alpha", "a")
        when: 'The property is retrieved'
        def property = this.propertySource.getProperty("alpha")
        then: 'The property has the expected value'
        property == "a"
    }

    def 'Null is returned when project has null property'() {
        given: 'A project with a null property'
        this.project.extensions.extraProperties.setProperty("alpha", null)
        when: 'The property is retrieved'
        def property = this.propertySource.getProperty("alpha")
        then: 'The property is null'
        property == null
    }

    def 'Null is returned when project has null property'() {
        given: 'A project'
        project.version = '1.2.3'
        when: 'The version property is retrieved'
        def property = this.propertySource.getProperty("version")
        then: 'The property is null'
        property == null
    }

}
