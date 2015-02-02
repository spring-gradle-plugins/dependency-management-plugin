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

package io.spring.gradle.dependencymanagement.maven

import io.spring.gradle.dependencymanagement.DependencyManagement
import io.spring.gradle.dependencymanagement.DependencyManagementExtension.PomCustomizationConfiguration
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification


class PomDependencyManagementConfigurerSpec extends Specification {

    Project project

    def setup() {
        project = new ProjectBuilder().build()
        project.repositories {
            mavenCentral()
        }
    }

    def "An imported bom can be imported in the pom"() {
        given: 'Dependency management that imports a bom'
            DependencyManagement dependencyManagement = new DependencyManagement(project, null)
            dependencyManagement.importBom('io.spring.platform:platform-bom:1.0.3.RELEASE')
        when: 'The pom is configured'
            Node pom = new XmlParser().parseText("<project></project>")
            new PomDependencyManagementConfigurer(dependencyManagement, new
                    PomCustomizationConfiguration()).configurePom(pom)
        then: 'The imported bom has been added'
            pom.dependencyManagement.dependencies.dependency.size() == 1
            def dependency = pom.dependencyManagement.dependencies.dependency[0]
            dependency.groupId[0].value() == 'io.spring.platform'
            dependency.artifactId[0].value() == 'platform-bom'
            dependency.version[0].value() == '1.0.3.RELEASE'
            dependency.scope[0].value() == 'import'
            dependency.type[0].value() == 'pom'
    }

    def "Multiple imports are imported in the order in which they were imported"() {
        given: 'Dependency management that imports two boms'
            project.repositories {
                maven { url new File("src/test/resources/maven-repo").toURI().toURL().toString() }
            }
            DependencyManagement dependencyManagement = new DependencyManagement(project, null)
            dependencyManagement.importBom('test:bravo-pom-customization-bom:1.0')
            dependencyManagement.importBom('test:alpha-pom-customization-bom:1.0')

        when: 'The pom is configured'
            Node pom = new XmlParser().parseText("<project></project>")
            PomCustomizationConfiguration configuration = new PomCustomizationConfiguration()
            new PomDependencyManagementConfigurer(dependencyManagement, configuration).configurePom(pom)
        then: 'The imported boms have been imported in their imported order'
            pom.dependencyManagement.dependencies.dependency.size() == 2
            def dependency1 = pom.dependencyManagement.dependencies.dependency[0]
            dependency1.groupId[0].value() == 'test'
            dependency1.artifactId[0].value() == 'bravo-pom-customization-bom'
            dependency1.version[0].value() == '1.0'
            dependency1.scope[0].value() == 'import'
            dependency1.type[0].value() == 'pom'

            def dependency2 = pom.dependencyManagement.dependencies.dependency[1]
            dependency2.groupId[0].value() == 'test'
            dependency2.artifactId[0].value() == 'alpha-pom-customization-bom'
            dependency2.version[0].value() == '1.0'
            dependency2.scope[0].value() == 'import'
            dependency2.type[0].value() == 'pom'
    }

    def "An imported bom can be copied into the pom"() {
        given: 'Dependency management that imports a bom'
            project.repositories {
                maven { url new File("src/test/resources/maven-repo").toURI().toURL().toString() }
            }
            DependencyManagement dependencyManagement = new DependencyManagement(project, null)
            dependencyManagement.importBom('test:alpha-pom-customization-bom:1.0')
        when: 'The pom is configured'
            Node pom = new XmlParser().parseText("<project></project>")
            PomCustomizationConfiguration configuration = new PomCustomizationConfiguration()
            configuration.importedBomAction = PomCustomizationConfiguration.ImportedBomAction.COPY
            new PomDependencyManagementConfigurer(dependencyManagement, configuration).configurePom(pom)
        then: 'The imported bom has been copied into the pom'
            pom.dependencyManagement.dependencies.dependency.size() == 1
            def dependency = pom.dependencyManagement.dependencies.dependency[0]
            dependency.groupId[0].value() == 'alpha'
            dependency.artifactId[0].value() == 'alpha'
            dependency.version[0].value() == '1.0'
            dependency.scope[0].value() == 'runtime'
            dependency.type[0].value() == 'foo'
            dependency.exclusions.exclusion.groupId[0].value()[0] == 'commons-logging'
            dependency.exclusions.exclusion.artifactId[0].value()[0] == 'commons-logging'
    }

    def "Multiple imports are copied in the order in which they were imported"() {
        given: 'Dependency management that imports two boms'
            project.repositories {
                maven { url new File("src/test/resources/maven-repo").toURI().toURL().toString() }
            }
            DependencyManagement dependencyManagement = new DependencyManagement(project, null)
            dependencyManagement.importBom('test:bravo-pom-customization-bom:1.0')
            dependencyManagement.importBom('test:alpha-pom-customization-bom:1.0')
        when: 'The pom is configured'
            Node pom = new XmlParser().parseText("<project></project>")
            PomCustomizationConfiguration configuration = new PomCustomizationConfiguration()
            configuration.importedBomAction = PomCustomizationConfiguration.ImportedBomAction.COPY
            new PomDependencyManagementConfigurer(dependencyManagement, configuration).configurePom(pom)
        then: 'The imported boms have been copied in their imported order'
            pom.dependencyManagement.dependencies.dependency.size() == 2

            def dependency1 = pom.dependencyManagement.dependencies.dependency[0]
            dependency1.groupId[0].value() == 'bravo'
            dependency1.artifactId[0].value() == 'bravo'
            dependency1.version[0].value() == '1.0'

            def dependency2 = pom.dependencyManagement.dependencies.dependency[1]
            dependency2.groupId[0].value() == 'alpha'
            dependency2.artifactId[0].value() == 'alpha'
            dependency2.version[0].value() == '1.0'
            dependency2.scope[0].value() == 'runtime'
            dependency2.type[0].value() == 'foo'
            dependency2.exclusions.exclusion.groupId[0].value()[0] == 'commons-logging'
            dependency2.exclusions.exclusion.artifactId[0].value()[0] == 'commons-logging'
    }

    def "Customization of published poms can be disabled"() {
        given: 'Dependency management that imports a bom'
            DependencyManagement dependencyManagement = new DependencyManagement(project, null)
            dependencyManagement.importBom('io.spring.platform:platform-bom:1.0.3.RELEASE')
        when: 'The pom is configured'
            Node pom = new XmlParser().parseText("<project></project>")
            PomCustomizationConfiguration configuration = new PomCustomizationConfiguration()
            configuration.enabled = false
            new PomDependencyManagementConfigurer(dependencyManagement, configuration).configurePom(pom)
        then: 'The imported bom has not been added'
            pom.dependencyManagement.dependencies.dependency.size() == 0
    }

    def "Individual dependency management is added to the pom"() {
        given: 'Dependency management that manages a dependency'
            DependencyManagement dependencyManagement = new DependencyManagement(project, null)
            dependencyManagement.addExplicitManagedVersion('org.springframework', 'spring-core',
                    '4.1.3.RELEASE')
        when: 'The pom is configured'
            Node pom = new XmlParser().parseText("<project></project>")
            new PomDependencyManagementConfigurer(dependencyManagement, new PomCustomizationConfiguration()).configurePom(pom)
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
            new PomDependencyManagementConfigurer(dependencyManagement, new PomCustomizationConfiguration()).configurePom(pom)
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
