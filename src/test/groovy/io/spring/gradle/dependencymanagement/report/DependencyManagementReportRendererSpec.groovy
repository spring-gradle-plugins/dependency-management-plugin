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

import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification

/**
 * Tests for {@link DependencyManagementReportRenderer}
 *
 * @author Andy Wilkinson.
 */
class DependencyManagementReportRendererSpec extends Specification {

    private StringWriter textOutput = new StringWriter()

    private DependencyManagementReportRenderer renderer = new DependencyManagementReportRenderer(new PrintWriter(textOutput))

    def 'Project header for root project'() {
        given:
            def rootProject = new ProjectBuilder().build()
        when:
            renderer.startProject(rootProject)
        then:
            textOutput.toString().readLines() == [
                    '',
                    '------------------------------------------------------------',
                    'Root project',
                    '------------------------------------------------------------',
                    ''
            ]
    }

    def 'Project header for subproject'() {
        given:
            def subproject = new ProjectBuilder().withParent(new ProjectBuilder().build())
                    .withName("alpha").build()
        when:
            renderer.startProject(subproject)
        then:
            textOutput.toString().readLines() == [
                    '',
                    '------------------------------------------------------------',
                    'Project :alpha',
                    '------------------------------------------------------------',
                    ''
            ]
    }

    def 'Project header for subproject with description'() {
        given:
            def subproject = new ProjectBuilder().withParent(new ProjectBuilder().build())
                    .withName("alpha").build()
            subproject.description = 'foo bar baz'
        when:
            renderer.startProject(subproject)
        then:
            textOutput.toString().readLines() == [
                    '',
                    '------------------------------------------------------------',
                    'Project :alpha - foo bar baz',
                    '------------------------------------------------------------',
                    ''
            ]
    }

    def 'Global dependency management with no managed versions'() {
        when:
            renderer.renderGlobalManagedVersions([:])
        then:
            textOutput.toString().readLines() == [
                    'global - Default dependency management for all configurations',
                    'No dependency management',
                    ''
            ]
    }

    def 'Global dependency management with managed versions'() {
        when:
            renderer.renderGlobalManagedVersions([
                    'com.example:bravo':'1.0.0',
                    'com.example:alpha':'1.2.3'
            ])
        then:
            textOutput.toString().readLines() == [
                    'global - Default dependency management for all configurations',
                    '    com.example:alpha 1.2.3',
                    '    com.example:bravo 1.0.0',
                    ''
            ]
    }

    def 'Configuration dependency management with no managed versions at all'() {
        given:
            def configuration = new ProjectBuilder().build().configurations.create("test")
        when:
            renderer.renderConfigurationManagedVersions([:], configuration, [:])
        then:
            textOutput.toString().readLines() == [
                    'test - Dependency management for the test configuration',
                    'No dependency management',
                    ''
            ]
    }

    def 'Configuration dependency management with only global managed versions'() {
        given:
            def configuration = new ProjectBuilder().build().configurations.create("test")
        when:
            renderer.renderConfigurationManagedVersions(['a:b':'1.0'], configuration, ['a:b':'1.0'])
        then:
            textOutput.toString().readLines() == [
                    'test - Dependency management for the test configuration',
                    'No configuration-specific dependency management',
                    ''
            ]
    }

    def 'Configuration dependency management'() {
        given:
            def configuration = new ProjectBuilder().build().configurations.create("test")
            def managedVersions = [
                    'com.example:bravo':'1.0.0',
                    'com.example:alpha':'1.2.3'
            ]
        when:
            renderer.renderConfigurationManagedVersions(managedVersions, configuration, [:])
        then:
            textOutput.toString().readLines() == [
                    'test - Dependency management for the test configuration',
                    '    com.example:alpha 1.2.3',
                    '    com.example:bravo 1.0.0',
                    ''
            ]
    }

}
