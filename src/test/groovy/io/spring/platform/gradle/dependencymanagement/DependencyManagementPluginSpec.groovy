package io.spring.platform.gradle.dependencymanagement;

import org.gradle.api.Project
import org.gradle.api.logging.LogLevel;
import org.gradle.testfixtures.ProjectBuilder

import spock.lang.Specification

public class DependencyManagementPluginSpec extends Specification {

	Project project

	def setup() {
		project = new ProjectBuilder().build()
		project.repositories {
			mavenCentral()
		}
	}

	def "Plugin provides the dependency management extension"() {
		when: 'The plugin is applied'
			project.apply plugin: DependencyManagementPlugin
		then: 'The extension is available'
			project.dependencyManagement
	}

	def "An imported bom can be used to apply dependency management"() {
		given: 'A project that imports a bom'
			project.apply plugin: 'dependency-management'
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
			files.size() == 2
			files.collect { it.name }.containsAll(['spring-core-4.0.6.RELEASE.jar', 'commons-logging-1.1.3.jar'])
	}

	def "An imported bom's versions can be overridden"() {
		given: 'A project that overrides a version of an imported bom'
			project.apply plugin: 'dependency-management'
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
			files.size() == 2
			files.collect { it.name }.containsAll(['spring-core-4.0.5.RELEASE.jar', 'commons-logging-1.1.3.jar'])
	}

	def "Dependency management can be declared in the build"() {
		given: 'A project with inline dependency management'
			project.apply plugin: 'dependency-management'
			project.apply plugin: 'java'
			project.dependencyManagement {
				dependencies {
					'org.springframework:spring-core' '4.0.4.RELEASE'
					'commons-logging:commons-logging' '1.1.2'
				}
			}
			project.dependencies {
				compile 'org.springframework:spring-core'
			}
			when: 'A configuration is resolved'
				def files = project.configurations.compile.resolve()
			then: 'Dependency management has been applied'
				files.size() == 2
				files.collect { it.name }.containsAll(['spring-core-4.0.4.RELEASE.jar', 'commons-logging-1.1.2.jar'])
	}
}
