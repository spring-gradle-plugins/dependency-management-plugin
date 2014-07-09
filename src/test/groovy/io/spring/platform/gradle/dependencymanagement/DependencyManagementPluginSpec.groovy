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

	def "Dependency management is applied"() {
		given: 'An appropriately configured project'
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
		then: 'Dependency management has been applied'
			files.size() == 2
			files.collect { it.name }.containsAll(['spring-core-4.0.6.RELEASE.jar', 'commons-logging-1.1.3.jar'])
	}
}
