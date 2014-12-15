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

package io.spring.gradle.dependencymanagement.maven

import io.spring.gradle.dependencymanagement.DependencyManagement
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification

class PomDependencyManagementConfigurerSpec extends Specification {

    Project project = new ProjectBuilder().build()

    def "An imported bom is added to the pom"() {
        given: 'Dependency management that imports a bom'
            DependencyManagement dependencyManagement = new DependencyManagement(project, null)
            dependencyManagement.importBom('io.spring.platform:platform-bom:1.0.3.RELEASE')
        when: 'The pom is configured'
            Node pom = new XmlParser().parseText("<project></project>")
            new PomDependencyManagementConfigurer(dependencyManagement).configurePom(pom)
        then: 'The imported bom has been added'
            pom.dependencyManagement.dependencies.dependency.size() == 1
            def dependency = pom.dependencyManagement.dependencies.dependency[0]
            dependency.groupId[0].value() == 'io.spring.platform'
            dependency.artifactId[0].value() == 'platform-bom'
            dependency.version[0].value() == '1.0.3.RELEASE'
            dependency.scope[0].value() == 'import'
            dependency.type[0].value() == 'pom'
    }

    def "Individual dependency management is added to the pom"() {
        given: 'Dependency management that manages a dependency'
            DependencyManagement dependencyManagement = new DependencyManagement(project, null)
            dependencyManagement.addExplicitManagedVersion('org.springframework', 'spring-core',
                    '4.1.3.RELEASE')
        when: 'The pom is configured'
            Node pom = new XmlParser().parseText("<project></project>")
            new PomDependencyManagementConfigurer(dependencyManagement).configurePom(pom)
        then: 'The managed dependency has been added'
            pom.dependencyManagement.dependencies.dependency.size() == 1
            def dependency = pom.dependencyManagement.dependencies.dependency[0]
            dependency.groupId[0].value() == 'org.springframework'
            dependency.artifactId[0].value() == 'spring-core'
            dependency.version[0].value() == '4.1.3.RELEASE'
    }

    def "Dependency management can be added to a pom with existing dependency management"() {
        given: 'Dependency management that imports a bom'
            DependencyManagement dependencyManagement = new DependencyManagement(project, null)
            dependencyManagement.importBom('io.spring.platform:platform-bom:1.0.3.RELEASE')
        when: 'The pom with existing dependency management is configured'
            Node pom = new XmlParser().parseText("<project><dependencyManagement><dependencies></dependencies></dependencyManagement></project>")
            new PomDependencyManagementConfigurer(dependencyManagement).configurePom(pom)
        then: 'The imported bom has been added'
            pom.dependencyManagement.dependencies.dependency.size() == 1
            def dependency = pom.dependencyManagement.dependencies.dependency[0]
            dependency.groupId[0].value() == 'io.spring.platform'
            dependency.artifactId[0].value() == 'platform-bom'
            dependency.version[0].value() == '1.0.3.RELEASE'
            dependency.scope[0].value() == 'import'
            dependency.type[0].value() == 'pom'
    }
}
