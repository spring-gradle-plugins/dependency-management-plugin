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

package io.spring.gradle.dependencymanagement

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.slf4j.impl.StaticLoggerBinder
import spock.lang.Specification

public class DependencyManagementPluginSpec extends Specification {

    Project project

    def setup() {
        project = new ProjectBuilder().build()
        project.repositories {
            mavenCentral()
        }
    }

    def cleanup() {
        project.projectDir.deleteDir()
    }

    def "Plugin provides the dependency management extension"() {
        when: 'The plugin is applied'
            project.apply plugin: 'io.spring.dependency-management'
        then: 'The extension is available'
            project.dependencyManagement
    }

    def "Customization of generated poms can be disabled"() {
        given: 'A project with the plugin applied'
            project.apply plugin: 'io.spring.dependency-management'
        when: 'Published pom customization is disabled'
            project.dependencyManagement {
                generatedPomCustomization {
                    enabled = false
                }
            }
        then: 'The configuration change has taken effect'
        def extension = project.extensions.getByType(DependencyManagementExtension)
        def configuration = extension.generatedPomCustomization
        !configuration.enabled
    }

    def "The pom configurer is available"() {
        given: 'A project with the plugin applied'
            project.apply plugin: 'io.spring.dependency-management'
        expect: 'The pom configurer is available'
            project.dependencyManagement.pomConfigurer
    }

    def "Customization of published poms can be configured to copy imported boms"() {
        given: 'A project with the plugin applied'
            project.apply plugin: 'io.spring.dependency-management'
        when: 'Published pom customization is configured to copy imported boms'
            project.dependencyManagement {
                generatedPomCustomization {
                    importedBomAction = 'copy'
                }
            }
        then: 'The configuration change has taken effect'
            project.extensions.getByType(DependencyManagementExtension).generatedPomCustomization
                    .importedBomAction == DependencyManagementExtension.PomCustomizationConfiguration
                    .ImportedBomAction.COPY
    }

    def "An imported bom can be used to apply dependency management"() {
        given: 'A project that imports a bom'
            project.apply plugin: 'io.spring.dependency-management'
            project.apply plugin: 'java'
            project.dependencyManagement {
                imports {
                    mavenBom 'io.spring.platform:platform-bom:1.0.1.RELEASE'
                }
            }
            project.dependencies {
                compile 'org.springframework:spring-core'
            }
        when: 'A configuration is resolved'
            def files = project.configurations.compile.resolve()
        then: "The bom's dependency management has been applied"
            files.size() == 1
            files.collect { it.name }.containsAll(['spring-core-4.0.6.RELEASE.jar'])
    }

    def "An imported bom's versions can be overridden"() {
        given: 'A project that overrides a version of an imported bom'
            project.apply plugin: 'io.spring.dependency-management'
            project.apply plugin: 'java'
            project.ext['spring.version'] = '4.0.5.RELEASE'
            project.dependencyManagement {
                imports {
                    mavenBom 'io.spring.platform:platform-bom:1.0.1.RELEASE'
                }
            }
            project.dependencies {
                compile 'org.springframework:spring-core'
            }
        when: 'A configuration is resolved'
            def files = project.configurations.compile.resolve()
        then: 'Dependency management has been applied with the overridden version'
            '4.0.5.RELEASE' == project.properties['spring.version']
            files.size() == 1
            files.collect { it.name }.containsAll(['spring-core-4.0.5.RELEASE.jar'])
    }

    def "Dependency management can be declared in the build"() {
        given: 'A project with inline dependency management'
            project.apply plugin: 'io.spring.dependency-management'
            project.apply plugin: 'java'
            project.dependencyManagement {
                dependencies {
                    dependency 'org.springframework:spring-core:4.0.4.RELEASE'
                    dependency ('commons-logging:commons-logging:1.1.2') {
                        exclude 'foo:bar'
                    }
                }
            }
            project.dependencies {
                compile 'org.springframework:spring-core'
            }
        when: 'A configuration is resolved'
            def files = project.configurations.compile.resolve()
        then: 'Dependency management has been applied'
            files.size() == 2
            files.collect { it.name }
                    .containsAll(['spring-core-4.0.4.RELEASE.jar', 'commons-logging-1.1.2.jar'])
    }

    def "Dependency management can be declared in the build using the new syntax"() {
        given: 'A project with the plugin applied'
            project.apply plugin: 'io.spring.dependency-management'
            project.apply plugin: 'java'
        when: 'Dependency management is configured'
            project.dependencyManagement {
                dependencies {
                    dependency 'org.springframework:spring-core:4.0.4.RELEASE'
                    dependency('commons-logging:commons-logging:1.1.2') {
                        exclude 'foo:bar'
                    }
                    dependency group:'alpha', name: 'bravo', version: '1.0'
                    dependency(group:'charlie', name: 'delta', version: '2.0') {
                        exclude group:'bar', name:'baz'
                    }
                }
            }
        then: 'The configuration has taken effect'
            project.dependencyManagement
                    .managedVersions['org.springframework:spring-core'] == '4.0.4.RELEASE'
            project.dependencyManagement
                    .managedVersions['commons-logging:commons-logging'] == '1.1.2'
            project.dependencyManagement.managedVersions['alpha:bravo'] == '1.0'
            project.dependencyManagement.managedVersions['charlie:delta'] == '2.0'
            def exclusions =  project.dependencyManagement.dependencyManagementContainer
                    .getExclusions(null)
            exclusions.exclusionsForDependency('charlie:delta') as List == ['bar:baz']
            exclusions.exclusionsForDependency('commons-logging:commons-logging') as List ==
                    ['foo:bar']
    }

    def "Dependency management with exclusions can be declared in the build"() {
        given: 'A project with inline dependency management'
            project.apply plugin: 'io.spring.dependency-management'
            project.apply plugin: 'java'
            project.dependencyManagement {
                dependencies {
                    dependency ('org.springframework:spring-core:4.0.4.RELEASE') {
                        exclude 'commons-logging:commons-logging'
                    }
                }
            }
            project.dependencies {
                compile 'org.springframework:spring-core'
            }
        when: 'A configuration is resolved'
            def files = project.configurations.compile.resolve()
        then: 'Dependency management and its exclusions has been applied'
            files.size() == 1
            files.collect { it.name }.containsAll(['spring-core-4.0.4.RELEASE.jar'])
    }

    def "Versions of direct dependencies take precedence over direct dependency management"() {
        given: 'A project with a version on a direct dependency and dependency management for the dependency'
            project.apply plugin: 'io.spring.dependency-management'
            project.apply plugin: 'java'
            project.dependencyManagement {
                dependencies {
                    dependency 'org.springframework:spring-core:4.0.4.RELEASE'
                }
            }
            project.dependencies {
                compile 'org.springframework:spring-core:4.0.6.RELEASE'
            }
        when: 'A configuration is resolved'
            def files = project.configurations.compile.resolve()
        then: 'Dependency management is not applied to the versioned dependency'
            files.size() == 2
            files.collect { it.name }
                    .containsAll(['spring-core-4.0.6.RELEASE.jar', 'commons-logging-1.1.3.jar'])
    }

    def "Direct project dependencies take precedence over dependency management"() {
        given: 'A project with a project dependency and dependency management for the dependency'
            def child = new ProjectBuilder().withName('child').withParent(project).build()
            child.group = 'test'
            child.version = '1.1.0'
            child.apply plugin: 'java'

            project.apply plugin: 'io.spring.dependency-management'
            project.apply plugin: 'java'
            project.dependencyManagement {
                dependencies {
                    dependency 'test:child:1.0.0'
                }
            }
            project.dependencies {
                compile project([path: ':child'])
            }
        when: 'A configuration is resolved'
            def files = project.configurations.compile.resolve()
        then: 'Dependency management is not applied to the project dependency'
            files.size() == 1
            files.collect { it.name }.containsAll(['child-1.1.0.jar'])
    }

    def "Transitive project dependencies take precedence over dependency management"() {
        given: 'A project with a transitive project dependency and dependency management for the dependency'

            def child = new ProjectBuilder().withName('child').withParent(project).build()
            def grandchild = new ProjectBuilder().withName('grandchild').withParent(project).
                    build()

            project.apply plugin: 'io.spring.dependency-management'
            project.apply plugin: 'java'

            grandchild.group = 'test-other'
            grandchild.version = '1.1.0'
            grandchild.apply plugin: 'java'

            child.group = 'test'
            child.version = '1.1.0'
            child.apply plugin: 'java'

            project.dependencyManagement {
                dependencies {
                    dependency 'test:child:1.0.0'
                    dependency 'test-other:grandchild:1.0.0'
                }
            }
            project.dependencies {
                compile project([path: ':child'])
            }
            child.dependencies {
                compile project([path: ':grandchild'])
            }
        when: 'A configuration is resolved'
            def files = project.configurations.compile.resolve()
        then: 'Dependency management is not applied to the project dependencies'
            files.size() == 2
            files.collect { it.name }.containsAll(['child-1.1.0.jar', 'grandchild-1.1.0.jar'])
    }

    def "Versions of direct dependencies take precedence over dependency management in an imported bom"() {
        given: 'A project with a version on a direct dependency and imported dependency management for the dependency'
            project.apply plugin: 'io.spring.dependency-management'
            project.apply plugin: 'java'
            project.dependencyManagement {
                imports {
                    mavenBom 'io.spring.platform:platform-bom:1.0.1.RELEASE'
                }
            }
            project.dependencies {
                compile 'org.springframework:spring-core:4.0.4.RELEASE'
            }
        when: 'A configuration is resolved'
            def files = project.configurations.compile.resolve()
        then: 'Dependency management is not applied to the versioned dependency'
            files.size() == 1
            files.collect { it.name }.containsAll(['spring-core-4.0.4.RELEASE.jar'])
    }

    def "Dependency management can be applied to a specific configuration"() {
        given: "A project with two configurations and dependency management for one of them"
            project.apply plugin: 'io.spring.dependency-management'
            project.apply plugin: 'java'

            project.configurations {
                managed
                unmanaged
            }

            project.dependencyManagement {
                managed {
                    dependencies {
                        dependency 'commons-logging:commons-logging:1.1.2'
                    }
                }
            }

            project.dependencies {
                managed 'org.springframework:spring-core:4.0.6.RELEASE'
                unmanaged 'org.springframework:spring-core:4.0.6.RELEASE'
            }
        when: 'The configurations are resolved'
            def managedFiles = project.configurations.managed.resolve()
            def unmanagedFiles = project.configurations.unmanaged.resolve()
        then: 'Dependency management is only applied to the managed configuration'
            managedFiles.size() == 2
            managedFiles.collect { it.name }
                    .containsAll(['spring-core-4.0.6.RELEASE.jar', 'commons-logging-1.1.2.jar'])
            unmanagedFiles.size() == 2
            unmanagedFiles.collect { it.name }
                    .containsAll(['spring-core-4.0.6.RELEASE.jar', 'commons-logging-1.1.3.jar'])
    }

    def "Dependency management can be applied to multiple specific configurations"() {
        given: "A project with three configurations and dependency management for two of them"
            project.apply plugin: 'io.spring.dependency-management'
            project.apply plugin: 'java'

            project.configurations {
                managed1
                managed2
                unmanaged
            }

            project.dependencyManagement {
                configurations(managed1, managed2) {
                    dependencies {
                        dependency 'commons-logging:commons-logging:1.1.2'
                    }
                }
            }

            project.dependencies {
                managed1 'org.springframework:spring-core:4.0.6.RELEASE'
                managed2 'org.springframework:spring-core:4.0.6.RELEASE'
                unmanaged 'org.springframework:spring-core:4.0.6.RELEASE'
            }
        when: 'The configurations are resolved'
            def managed1Files = project.configurations.managed1.resolve()
            def managed2Files = project.configurations.managed2.resolve()
            def unmanagedFiles = project.configurations.unmanaged.resolve()
        then: 'Dependency management is only applied to the managed configurations'
            managed1Files.size() == 2
            managed1Files.collect { it.name }
                    .containsAll(['spring-core-4.0.6.RELEASE.jar', 'commons-logging-1.1.2.jar'])
            managed2Files.size() == 2
            managed2Files.collect { it.name }
                    .containsAll(['spring-core-4.0.6.RELEASE.jar', 'commons-logging-1.1.2.jar'])
            unmanagedFiles.size() == 2
            unmanagedFiles.collect { it.name }
                    .containsAll(['spring-core-4.0.6.RELEASE.jar', 'commons-logging-1.1.3.jar'])
    }

    def "Configuration-specific dependency management takes precedence over global dependency management"() {
        given: "A project with global and configuration-specific dependency management"
            project.apply plugin: 'io.spring.dependency-management'
            project.apply plugin: 'java'

            project.dependencyManagement {
                dependencies {
                    dependency 'commons-logging:commons-logging:1.1.2'
                }
                compile {
                    dependencies {
                        dependency 'commons-logging:commons-logging:1.1.1'
                    }
                }
            }

            project.dependencies {
                compile 'org.springframework:spring-core:4.0.6.RELEASE'
            }
        when: 'The configuration is resolved'
            def files = project.configurations.compile.resolve()
        then: 'The configuration-specific dependency management has taken precedence'
            files.size() == 2
            files.collect { it.name }
                    .containsAll(['spring-core-4.0.6.RELEASE.jar', 'commons-logging-1.1.1.jar'])
    }

    def "Configuration-specific dependency management is inherited by extending configurations"() {
        given: "A project with global and configuration-specific dependency management"
            project.apply plugin: 'io.spring.dependency-management'
            project.apply plugin: 'java'

            project.dependencyManagement {
                compile {
                    dependencies {
                        dependency 'commons-logging:commons-logging:1.1.1'
                    }
                }
            }

            project.dependencies {
                testRuntime 'commons-logging:commons-logging'
            }
        when: 'The extending configuration is resolved'
            def files = project.configurations.testRuntime.resolve()
        then: 'The configuration-specific dependency management has been inherited'
            files.size() == 1
            files.collect { it.name }.containsAll(['commons-logging-1.1.1.jar'])
    }

    def "A version on a direct dependency provides dependency management to extending configurations"() {
        given: "A project with global dependency management and a versioned dependency in the compile configuration"
            project.apply plugin: 'io.spring.dependency-management'
            project.apply plugin: 'java'

            project.dependencyManagement {
                dependencies {
                    dependency 'commons-logging:commons-logging:1.1.1'
                }
            }

            project.dependencies {
                compile 'commons-logging:commons-logging:1.1.3'
            }
        when: 'An extending configuration is resolved'
            def files = project.configurations.testRuntime.resolve()
        then: 'The dependency management provided by the direct dependency has been inherited'
            files.size() == 1
            files.collect { it.name }.containsAll(['commons-logging-1.1.3.jar'])
    }

    def "The JBoss Java EE bom can be imported and used for dependency management (see gh-3)"() {
        given: 'A project that imports the JBoss bom'
            project.apply plugin: 'io.spring.dependency-management'
            project.apply plugin: 'java'
            project.dependencyManagement {
                imports {
                    mavenBom 'org.jboss.spec:jboss-javaee-6.0:1.0.0.Final'
                }
            }
            project.dependencies {
                compile 'org.jboss.spec.javax.el:jboss-el-api_2.2_spec'
            }
        when: 'A configuration is resolved'
            def files = project.configurations.compile.resolve()
        then: "The bom's dependency management has been applied"
            files.size() == 1
            files.iterator().next().name == 'jboss-el-api_2.2_spec-1.0.0.Final.jar'
    }

    def "A bom with no dependency management can be imported and its properties used (see gh-41)" () {
        given: 'A project with the plugin applied'
            project.apply plugin: 'io.spring.dependency-management'
            project.apply plugin: 'java'
            project.repositories {
                maven {
                    url 'https://maven.repository.redhat.com/techpreview/all'
                }
            }
        when: 'A bom with no dependency management is imported'
            project.dependencyManagement {
                imports {
                    mavenBom 'org.jboss.bom.eap:jboss-eap-bom-parent:6.3.3.GA'
                }
            }
        then: "The bom's properties are available"
            '1.1.0.Final' == project.dependencyManagement
                    .importedProperties['version.org.jboss.arquillian']
    }

    def "The Spring Cloud Starter Parent bom can be imported and used for dependency management"() {
        given: 'A project that imports the Spring Cloud Starter Parent bom'
            project.apply plugin: 'io.spring.dependency-management'
            project.apply plugin: 'java'
            project.repositories {
                maven { url 'https://repo.spring.io/libs-milestone' }
            }
            project.dependencyManagement {
                imports {
                    mavenBom 'org.springframework.cloud:spring-cloud-starter-parent:1.0.0.M3'
                }
            }
        expect: "The bom's versions are available"
            project.dependencyManagement.compile
                    .managedVersions['org.springframework.cloud:spring-cloud-starter-eureka-server'] == '1.0.0.M3'
    }

    def 'A dependency set can be used to provide dependency management for multiple modules with the same group and version'() {
        given: 'A project with dependency management that uses a dependency set'
            project.apply plugin: 'io.spring.dependency-management'
            project.apply plugin: 'java'
            project.dependencyManagement {
                dependencies {
                    dependencySet(group: 'org.slf4j', version: '1.7.7') {
                        entry 'slf4j-api'
                        entry 'slf4j-simple'
                    }
                }
            }
            project.dependencies {
                compile 'org.slf4j:slf4j-api'
                compile 'org.slf4j:slf4j-simple'
            }
        when: 'The configuration is resolved'
            def files = project.configurations.compile.resolve()
        then: 'The dependency management has been applied'
            files.size() == 2
            files.collect { it.name }.containsAll(['slf4j-api-1.7.7.jar', 'slf4j-simple-1.7.7.jar'])
    }

    def 'An exclusion can be declared on an entry in a dependency set'() {
        given: 'A project with dependency management that uses a dependency set'
            project.apply plugin: 'io.spring.dependency-management'
            project.apply plugin: 'java'
            project.dependencyManagement {
                dependencies {
                    dependencySet(group: 'org.springframework', version: '4.1.4.RELEASE') {
                        entry('spring-core') {
                            exclude 'commons-logging:commons-logging'
                        }
                    }
                }
            }
            project.dependencies {
                compile 'org.springframework:spring-core'
            }
        when: 'The configuration is resolved'
            def files = project.configurations.compile.resolve()
        then: 'The dependency management and its exclusions has been applied'
            files.size() == 1
            files.collect { it.name }.containsAll(['spring-core-4.1.4.RELEASE.jar'])
    }

    def 'The build fails if a dependency set is configured without a group'() {
        given: 'A project'
            project.apply plugin: 'io.spring.dependency-management'
            project.apply plugin: 'java'
        when: 'A dependency set is declared with a version but not group'
            project.dependencyManagement {
                dependencies {
                    dependencySet(version: '1.7.7') {}
                }
            }
        then: 'An exception with an appropriate message is thrown'
            def e = thrown(GradleException)
            e.message == 'A dependency set requires both a group and a version'
    }

    def 'The build fails if a dependency set is configured without a version'() {
        given: 'A project'
            project.apply plugin: 'io.spring.dependency-management'
            project.apply plugin: 'java'
        when: 'A dependency set is declared with a group but not version'
            project.dependencyManagement {
                dependencies {
                    dependencySet(group: 'org.slf4j') {}
                }
            }
        then: 'An exception with an appropriate message is thrown'
            def e = thrown(GradleException)
            e.message == 'A dependency set requires both a group and a version'
    }

    def 'Managed versions can be accessed programatically'() {
        given: 'A project with the plugin applied'
            project.apply plugin: 'io.spring.dependency-management'
            project.apply plugin: 'java'
        when: 'Dependency management is configured'
            project.dependencyManagement {
                imports {
                    mavenBom 'io.spring.platform:platform-bom:1.0.1.RELEASE'
                }
                testRuntime {
                    dependencies {
                        dependency 'com.foo:bar:1.2.3'
                    }
                }
                dependencies {
                    dependencySet(group: 'com.alpha', version: '1.0') {
                        entry 'bravo'
                        entry 'charlie'
                    }
                }
            }
        then: 'The managed versions can be accessed'
            project.dependencyManagement.compile
                    .managedVersions['org.springframework:spring-core'] == '4.0.6.RELEASE'
            project.dependencyManagement.testRuntime
                    .managedVersions['org.springframework:spring-core'] == '4.0.6.RELEASE'
            project.dependencyManagement
                    .managedVersions['org.springframework:spring-core'] == '4.0.6.RELEASE'

            project.dependencyManagement.compile.managedVersions['com.foo:bar'] == null
            project.dependencyManagement.testRuntime.managedVersions['com.foo:bar'] == '1.2.3'
            project.dependencyManagement.managedVersions['com.foo:bar'] == null

            project.dependencyManagement.compile.managedVersions['com.alpha:bravo'] == '1.0'
            project.dependencyManagement.testRuntime.managedVersions['com.alpha:bravo'] == '1.0'
            project.dependencyManagement.managedVersions['com.alpha:bravo'] == '1.0'

            project.dependencyManagement.compile.managedVersions['com.alpha:charlie'] == '1.0'
            project.dependencyManagement.testRuntime.managedVersions['com.alpha:charlie'] == '1.0'
            project.dependencyManagement.managedVersions['com.alpha:charlie'] == '1.0'
    }

    def 'Properties imported from a bom can be accessed'() {
        given: 'A project with the plugin applied'
            project.apply plugin: 'io.spring.dependency-management'
            project.apply plugin: 'java'
        when: 'A bom is imported'
            project.configurations {
                myConfiguration
            }
            project.dependencyManagement {
                imports {
                    mavenBom 'io.spring.platform:platform-bom:1.0.1.RELEASE'
                }
                myConfiguration {
                    imports {
                        mavenBom 'org.springframework.boot:spring-boot-dependencies:1.2.1.RELEASE'
                    }
                }
            }
        then: 'The properties in the bom can be accessed'
            '4.3.5.Final' == project.dependencyManagement.importedProperties['hibernate.version']
            '4.1.4.RELEASE' == project.dependencyManagement.myConfiguration
                    .importedProperties['spring.version']
            '1.7.12' == project.dependencyManagement.myConfiguration.importedProperties['jruby.version']
    }

    def 'A bom property can be used to version a dependency'() {
        given: 'A project that imports a bom'
            project.apply plugin: 'io.spring.dependency-management'
            project.apply plugin: 'java'
            project.configurations {
                myConfiguration
            }
            project.dependencyManagement {
                imports {
                    mavenBom 'io.spring.platform:platform-bom:1.0.1.RELEASE'
                }
            }
        when: 'A property is used to version a dependency that is not included in the bom'
            project.dependencies {
                myConfiguration "org.hibernate:hibernate-envers:${project.dependencyManagement.importedProperties['hibernate.version']}"
            }
        then: 'The dependency has the expected version'
            def files = project.configurations.myConfiguration.resolve()
            files.collect { it.name }.contains('hibernate-envers-4.3.5.Final.jar')
    }

    def 'Bom that references java home can be imported'() {
        given: 'A project with the plugin applied'
            project.apply plugin: 'io.spring.dependency-management'
            project.apply plugin: 'java'
        when: 'A bom that references java home is imported'
            project.dependencyManagement {
                imports {
                    mavenBom 'org.jboss:jboss-parent:11'
                }
            }
        then: 'Its dependency management can be accessed'
            project.dependencyManagement.managedVersions['com.sun:tools'] == '1.6'
    }

    def 'Dependency versions can be defined using properties'() {
        given: 'A project with the plugin applied and a version property'
            project.apply plugin: 'io.spring.dependency-management'
            project.apply plugin: 'java'
            project.ext['springVersion'] = '4.1.1.RELEASE'
            project.ext['slf4jVersion'] = '1.7.7'
        when: 'Dependency management that contains a dependency that references the property is declared'
            project.dependencyManagement {
                dependencies {
                    dependency "org.springframework:spring-core:$springVersion"
                    dependency "org.springframework:spring-beans:$springVersion"
                    dependency "org.springframework:spring-tx:${project.ext['springVersion']}"
                    dependencySet(group: 'org.slf4j', version: slf4jVersion) {
                        entry 'slf4j-api'
                    }
                }
            }
        then: 'The expected version has been applied'
            project.dependencyManagement
                    .managedVersions['org.springframework:spring-core'] == '4.1.1.RELEASE'
            project.dependencyManagement
                    .managedVersions['org.springframework:spring-tx'] == '4.1.1.RELEASE'
            project.dependencyManagement
                    .managedVersions['org.slf4j:slf4j-api'] == '1.7.7'

    }

    def 'The import of a bom can reference a property'() {
        given: 'A project with the plugin applied and a version property'
            project.apply plugin: 'io.spring.dependency-management'
            project.apply plugin: 'java'
            project.ext['platformVersion'] = '1.0.1.RELEASE'
        when: 'Dependency management that imports a bom using a property for its version is declared'
            project.dependencyManagement {
                imports {
                    mavenBom "io.spring.platform:platform-bom:$platformVersion"
                }
            }
        then: 'The dependency management from the bom has been applied'
            project.dependencyManagement
                    .managedVersions['org.springframework:spring-core'] == '4.0.6.RELEASE'

    }

    def 'An explicit dependency prevents the dependency from being excluded'() {
        given: 'A project that imports a bom'
            project.apply plugin: 'io.spring.dependency-management'
            project.apply plugin: 'java'
            project.dependencyManagement {
                imports {
                    mavenBom 'io.spring.platform:platform-bom:1.0.1.RELEASE'
                }
            }
            project.dependencies {
                compile 'org.springframework:spring-core'
                compile 'commons-logging:commons-logging:1.1.3'
            }
        when: 'A configuration is resolved'
            def files = project.configurations.compile.resolve()
        then: "The bom's dependency management has been applied"
            files.size() == 2
            files.collect { it.name }
                    .containsAll(['spring-core-4.0.6.RELEASE.jar', 'commons-logging-1.1.3.jar'])
    }

    def 'A transitive dependency with an unexcluded path prevents exclusion'() {
        given: 'A project that imports a bom'
            project.apply plugin: 'io.spring.dependency-management'
            project.apply plugin: 'java'
            project.dependencyManagement {
                imports {
                    mavenBom 'io.spring.platform:platform-bom:1.0.1.RELEASE'
                }
            }
            project.dependencies {
                compile 'org.springframework:spring-core'
                compile 'org.apache.hive:hive-common:0.14.0'
            }
        when: 'A configuration is resolved'
            def files = project.configurations.compile.resolve()
        then: "The bom's dependency management has been applied"
            files.collect { it.name }.
                    containsAll(['spring-core-4.0.6.RELEASE.jar', 'commons-logging-1.1.3.jar',
                                 'hive-common-0.14.0.jar'])
    }

    def 'An exclusion declared on the dependency that has the excluded dependency is honoured'() {
        given: 'A project with the plugin applied'
            project.apply plugin: 'io.spring.dependency-management'
            project.apply plugin: 'java'
            project.repositories {
                maven { url new File("src/test/resources/maven-repo").toURI().toURL().toString() }
            }
        when: 'It depends on a module that directly excludes commons-logging'
            project.dependencies {
                compile 'test:direct-exclude:1.0'
            }
            def files = project.configurations.compile.resolve()
        then: "commons-logging has been excluded"
            files.size() == 4
            files.collect { it.name }.containsAll(['direct-exclude-1.0.jar',
                                                   'spring-tx-4.1.2.RELEASE.jar',
                                                   'spring-beans-4.1.2.RELEASE.jar',
                                                   'spring-core-4.1.2.RELEASE.jar'])
    }

    def 'A dependency with an otherwise excluded transitive dependency overrides the exclude'() {
        given: 'A project with the plugin applied'
            project.plugins.apply(DependencyManagementPlugin)
            project.apply plugin: 'java'
            project.repositories {
                maven { url new File("src/test/resources/maven-repo").toURI().toURL().toString() }
            }
        when: 'It depends on a module that directly excludes commons-logging and one that does not'
            project.dependencies {
                compile 'test:direct-exclude:1.0'
                compile 'org.springframework:spring-core:4.1.2.RELEASE'
            }
            def files = project.configurations.compile.resolve()
        then: "commons-logging has not been excluded"
            files.size() == 5
            files.collect { it.name }.containsAll(['direct-exclude-1.0.jar',
                                                   'spring-tx-4.1.2.RELEASE.jar',
                                                   'spring-beans-4.1.2.RELEASE.jar',
                                                   'spring-core-4.1.2.RELEASE.jar'])
    }

    def 'An exclusion that applies transitively is honoured'() {
        given: 'A project with the plugin applied'
            project.apply plugin: 'io.spring.dependency-management'
            project.apply plugin: 'java'
            project.repositories {
                maven { url new File("src/test/resources/maven-repo").toURI().toURL().toString() }
            }
        when: 'It depends on a module that transitively excludes commons-logging'
            project.dependencies {
                compile 'test:transitive-exclude:1.0'
            }
            def files = project.configurations.compile.resolve()
        then: "commons-logging has been excluded"
            files.size() == 4
            files.collect { it.name }.containsAll(['transitive-exclude-1.0.jar',
                                                   'spring-tx-4.1.2.RELEASE.jar',
                                                   'spring-beans-4.1.2.RELEASE.jar',
                                                   'spring-core-4.1.2.RELEASE.jar'])
    }

    def 'A direct exclusion declared in a bom is honoured'() {
        given: 'A project with the plugin applied'
            project.apply plugin: 'io.spring.dependency-management'
            project.apply plugin: 'java'
            project.repositories {
                maven { url new File("src/test/resources/maven-repo").toURI().toURL().toString() }
            }
        when: 'It imports a bom that directly excludes commons-logging'
            project.dependencyManagement {
                imports {
                    mavenBom 'test:direct-exclude-bom:1.0'
                }
            }
            project.dependencies {
                compile 'org.springframework:spring-tx:4.1.2.RELEASE'
            }
            def files = project.configurations.compile.resolve()
        then: "commons-logging has been excluded"
            files.size() == 3
            files.collect { it.name }.containsAll(['spring-tx-4.1.2.RELEASE.jar',
                                                   'spring-beans-4.1.2.RELEASE.jar',
                                                   'spring-core-4.1.2.RELEASE.jar'])
    }

    def 'A transitive exclusion declared in a bom is honoured'() {
        given: 'A project with the plugin applied'
            project.apply plugin: 'io.spring.dependency-management'
            project.apply plugin: 'java'
            project.repositories {
                maven { url new File("src/test/resources/maven-repo").toURI().toURL().toString() }
            }
        when: 'It imports a bom that transitively excludes commons-logging'
            project.dependencyManagement {
                imports {
                    mavenBom 'test:transitive-exclude-bom:1.0'
                }
            }
            project.dependencies {
                compile 'org.springframework:spring-tx:4.1.2.RELEASE'
            }
            def files = project.configurations.compile.resolve()
        then: "commons-logging has been excluded"
            files.size() == 3
            files.collect { it.name }.containsAll(['spring-tx-4.1.2.RELEASE.jar',
                                                   'spring-beans-4.1.2.RELEASE.jar',
                                                   'spring-core-4.1.2.RELEASE.jar'])
    }

    def 'Exclusions are not inherited and do not affect direct dependencies (see gh-21)'() {
        given: 'A project with the plugin applied'
            project.apply plugin: 'io.spring.dependency-management'
            project.apply plugin: 'java'
            project.repositories {
                maven { url new File("src/test/resources/maven-repo").toURI().toURL().toString() }
            }
        when: 'It depends on a module that excludes commons-logging in compile and on ' +
                'commons-logging in testCompile'
            project.dependencies {
                compile 'test:direct-exclude:1.0'
                testCompile 'commons-logging:commons-logging:1.1.3'
            }
        then: "commons-logging has been excluded from compile but not testCompile"
            def compileFiles = project.configurations.compile.resolve()
            compileFiles.size() == 4
            compileFiles.collect { it.name }.containsAll(['direct-exclude-1.0.jar',
                                                   'spring-tx-4.1.2.RELEASE.jar',
                                                   'spring-beans-4.1.2.RELEASE.jar',
                                                   'spring-core-4.1.2.RELEASE.jar'])
            def testCompileFiles = project.configurations.testCompile.resolve()
            testCompileFiles.size() == 5
            testCompileFiles.collect { it.name }.containsAll(['direct-exclude-1.0.jar',
                                                          'spring-tx-4.1.2.RELEASE.jar',
                                                          'spring-beans-4.1.2.RELEASE.jar',
                                                          'spring-core-4.1.2.RELEASE.jar',
                                                          'commons-logging-1.1.3.jar'])
    }

    def 'Exclusions are not inherited and do not affect transitive dependencies (see gh-21)'() {
        given: 'A project with a compile dependency that pulls in a JUnit exclude'
            project.apply plugin: 'io.spring.dependency-management'
            project.apply plugin: 'java'
            project.dependencyManagement {
                imports {
                    mavenBom 'org.springframework.boot:spring-boot-dependencies:1.2.0.RELEASE'
                }
            }
            project.dependencies {
                compile 'org.codehaus.groovy:groovy'
            }
        when: 'It has a transitive testCompile dependency on JUnit'
            project.dependencies {
                testCompile 'org.springframework.boot:spring-boot-starter-test'
            }
        then: "JUnit has not be excluded"
            def testCompileFiles = project.configurations.testCompile.resolve()
            testCompileFiles.size() == 9
            testCompileFiles
                    .collect { it.name }
                    .containsAll(['groovy-2.3.8.jar',
                                  'spring-boot-starter-test-1.2.0.RELEASE.jar',
                                  'junit-4.12.jar',
                                  'mockito-core-1.10.8.jar',
                                  'hamcrest-core-1.3.jar',
                                  'hamcrest-library-1.3.jar',
                                  'spring-core-4.1.3.RELEASE.jar',
                                  'spring-test-4.1.3.RELEASE.jar',
                                  'objenesis-2.1.jar'])
    }

    def "Exclusions from a dependency's ancestors are applied correctly (see gh-23)"() {
        given: "A project with the plugin applied that depends on Spring Cloud's Zuul starter"
            project.apply plugin: 'io.spring.dependency-management'
            project.apply plugin: 'java'
            project.dependencies {
                compile 'org.springframework.cloud:spring-cloud-starter-zuul:1.0.0.RC1'
            }
            project.repositories {
                maven { url 'https://repo.spring.io/libs-milestone' }
            }
        when: 'The configuration is resolved'
            def files = project.configurations.compile.resolve()
        then: 'ribbon-loadbalancer has not be excluded'
            files.collect { it.name }
                    .contains 'ribbon-loadbalancer-2.0-RC13.jar'


    }

    def "Pom exclusions can be disabled"() {
        given: 'A project that imports a bom and disables pom exclusions'
            project.apply plugin: 'io.spring.dependency-management'
            project.apply plugin: 'java'
            project.dependencyManagement {
                imports {
                    mavenBom 'io.spring.platform:platform-bom:1.0.1.RELEASE'
                }
                applyMavenExclusions false
            }
            project.dependencies {
                compile 'org.springframework:spring-core'
            }
        when: 'A configuration is resolved'
            def files = project.configurations.compile.resolve()
        then: "The bom's dependency management has been applied but the exclusions have not"
            files.size() == 2
            files.collect { it.name }.containsAll(['spring-core-4.0.6.RELEASE.jar',
                                                   'commons-logging-1.1.3.jar'])
    }

    def "Exclusions are applied correctly to dependencies that are referenced multiple times"() {
        given: 'A project that depends on spring-boot-starter-remote-shall'
            project.apply plugin: 'io.spring.dependency-management'
            project.apply plugin: 'java'
            project.dependencies {
                compile 'org.springframework.boot:spring-boot-starter-remote-shell:1.2.0.RELEASE'
            }
        when: "its compile configuration is resolved"
            def files = project.configurations.compile.resolve()
        then: "groovy-all has been excluded"
            files.collect { it.name }.findAll { it.startsWith 'groovy-all' } .size() == 0
    }

    def "Transitive dependencies with a circular reference are tolerated (see gh-33)"() {
        given: 'A project that depends on org.apache.xmlgraphics:batik-rasterizer'
            project.apply plugin: 'io.spring.dependency-management'
            project.apply plugin: 'java'
            project.dependencies {
                compile 'org.apache.xmlgraphics:batik-rasterizer:1.7'
            }
        when: "its compile configuration is resolved"
            def files = project.configurations.compile.resolve()
        then: "there is no StackOverflowException and resolution succeeds"
            !files.empty
    }

    def "The resolution strategy of a dependency management configuration can be customized"() {
        when: 'A project has the plugin applied'
            project.apply plugin: 'io.spring.dependency-management'
            project.apply plugin: 'java'
        then: "The resolution strategy can be customized"
            project.dependencyManagement {
                resolutionStrategy {
                    cacheChangingModulesFor 0, 'seconds'
                }
            }
    }

    def "A managed dependency can be configured using a GString"() {
        given: 'A project that has the plugin applied'
            project.apply plugin: 'io.spring.dependency-management'
            project.apply plugin: 'java'
        when: "A managed dependency is configured using a GString"
            def springVersion = '4.1.5.RELEASE'
            project.dependencyManagement {
                dependencies {
                    dependency "org.springframework:spring-core:$springVersion"
                }
            }
        then: 'The managed version has been configured correctly'
            '4.1.5.RELEASE' == project.dependencyManagement.managedVersions['org.springframework:spring-core']
    }

    def "Exclusions are handled correctly for dependencies that appear multiple times"() {
        given: 'A project that has the plugin applied'
            project.apply plugin: 'io.spring.dependency-management'
            project.apply plugin: 'java'
        when: "It has dependencies that depend on and exclude javax.validation:validation-api"
            project.dependencies {
                compile("org.springframework.cloud:spring-cloud-starter-eureka:1.0.0.RELEASE")
                compile("org.springframework.boot:spring-boot-starter-web:1.2.3.RELEASE")
            }
        then: 'validation-api has not been excluded'
            def files = project.configurations.compile.resolve()
            files.collect { it.name }.containsAll(['validation-api-1.1.0.Final.jar'])

    }

    def "The order in which boms are imported is reflected in the managed versions"() {
        given: 'A project that has the plugin applied'
            project.apply plugin: 'io.spring.dependency-management'
            project.apply plugin: 'java'
        when: 'Multiple boms are imported'
            project.dependencyManagement {
                imports {
                    mavenBom 'org.springframework.boot:spring-boot-dependencies:1.2.7.RELEASE'
                    mavenBom 'io.spring.platform:platform-bom:2.0.0.RELEASE'
                }
            }
        then: 'The versions from the second bom override the versions from the first'
            '4.2.3.RELEASE' == project.dependencyManagement.managedVersions['org.springframework:spring-core']
    }
}
