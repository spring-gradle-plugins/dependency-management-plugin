package io.spring.gradle.dependencymanagement

import org.gradle.api.Project
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
			project.apply plugin: 'io.spring.dependency-management'
		then: 'The extension is available'
			project.dependencyManagement
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
			files.size() == 2
			files.collect { it.name }.containsAll(['spring-core-4.0.6.RELEASE.jar', 'commons-logging-1.1.3.jar'])
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
			files.size() == 2
			files.collect { it.name }.containsAll(['spring-core-4.0.5.RELEASE.jar', 'commons-logging-1.1.3.jar'])
	}

	def "Dependency management can be declared in the build"() {
		given: 'A project with inline dependency management'
			project.apply plugin: 'io.spring.dependency-management'
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

	def "Versions of direct dependencies take precedence over direct dependency management"() {
		given: 'A project with a version on a direct dependency and dependency management for the dependency'
			project.apply plugin: 'io.spring.dependency-management'
			project.apply plugin: 'java'
			project.dependencyManagement {
				dependencies {
					'org.springframework:spring-core' '4.0.4.RELEASE'
				}
			}
			project.dependencies {
				compile 'org.springframework:spring-core:4.0.6.RELEASE'
			}
		when: 'A configuration is resolved'
			def files = project.configurations.compile.resolve()
		then: 'Dependency management is not applied to the versioned dependency'
			files.size() == 2
			files.collect { it.name }.containsAll(['spring-core-4.0.6.RELEASE.jar', 'commons-logging-1.1.3.jar'])
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
			files.size() == 2
			files.collect { it.name }.containsAll(['spring-core-4.0.4.RELEASE.jar', 'commons-logging-1.1.3.jar'])
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
						'commons-logging:commons-logging' '1.1.2'
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
			managedFiles.collect { it.name }.containsAll(['spring-core-4.0.6.RELEASE.jar', 'commons-logging-1.1.2.jar'])
			unmanagedFiles.size() == 2
			unmanagedFiles.collect { it.name }.containsAll(['spring-core-4.0.6.RELEASE.jar', 'commons-logging-1.1.3.jar'])
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
						'commons-logging:commons-logging' '1.1.2'
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
			managed1Files.collect { it.name }.containsAll(['spring-core-4.0.6.RELEASE.jar', 'commons-logging-1.1.2.jar'])
			managed2Files.size() == 2
			managed2Files.collect { it.name }.containsAll(['spring-core-4.0.6.RELEASE.jar', 'commons-logging-1.1.2.jar'])
			unmanagedFiles.size() == 2
			unmanagedFiles.collect { it.name }.containsAll(['spring-core-4.0.6.RELEASE.jar', 'commons-logging-1.1.3.jar'])
	}

	def "Configuration-specific dependency management takes precedence over global dependency management"() {
		given: "A project with global and configuration-specific dependency management"
			project.apply plugin: 'io.spring.dependency-management'
			project.apply plugin: 'java'

			project.dependencyManagement {
				dependencies {
					'commons-logging:commons-logging' '1.1.2'
				}
				compile {
					dependencies {
						'commons-logging:commons-logging' '1.1.1'
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
			files.collect { it.name }.containsAll(['spring-core-4.0.6.RELEASE.jar', 'commons-logging-1.1.1.jar'])
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
}
