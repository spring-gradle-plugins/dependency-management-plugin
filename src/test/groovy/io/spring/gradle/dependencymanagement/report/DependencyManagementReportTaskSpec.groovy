/*
 * Copyright 2014-2016 the original author or authors.
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

import io.spring.gradle.dependencymanagement.DependencyManagementConfigurationContainer
import io.spring.gradle.dependencymanagement.DependencyManagementContainer
import io.spring.gradle.dependencymanagement.maven.EffectiveModelBuilder
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification

/**
 * Tests for @{link DependencyManagementReportTask}
 *
 * @author Andy Wilkinson
 */
class DependencyManagementReportTaskSpec extends Specification {

    private Project project = new ProjectBuilder().build()

    private DependencyManagementReportTask task = project.tasks.create('dependencyManagement', DependencyManagementReportTask)

    private DependencyManagementReportRenderer renderer = Mock(DependencyManagementReportRenderer)

    def setup() {
        DependencyManagementConfigurationContainer configurationContainer = new
                DependencyManagementConfigurationContainer(project)
        this.task.dependencyManagement = new DependencyManagementContainer(project,
                configurationContainer, new EffectiveModelBuilder(project, configurationContainer))
        this.task.renderer = renderer
    }

    def "Basic report"() {
        when:
            task.report()
        then: 1 * renderer.startProject(project)
        then: 1 * renderer.renderGlobalManagedVersions(_)
        then: 0 * renderer._
    }

    def "Report for project with configurations"() {
        given:
            def configurationOne = project.configurations.create("foo")
            def configurationTwo = project.configurations.create("bar")
        when:
            task.report()
        then: 1 * renderer.startProject(project)
        then: 1 * renderer.renderGlobalManagedVersions(_)
        then: 1 * renderer.renderConfigurationManagedVersions(_, configurationTwo, _)
        then: 1 * renderer.renderConfigurationManagedVersions(_, configurationOne, _)
        then: 0 * renderer._
    }

}
