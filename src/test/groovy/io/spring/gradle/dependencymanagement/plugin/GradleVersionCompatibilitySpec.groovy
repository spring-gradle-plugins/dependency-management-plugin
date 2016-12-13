package io.spring.gradle.dependencymanagement.plugin

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification
import spock.lang.Unroll

/**
 * Tests that verify the plugin's compatibility with various versions of Gradle
 */
class GradleVersionCompatibilitySpec extends Specification {

    @Rule
    final TemporaryFolder projectFolder = new TemporaryFolder()

    private File buildFile

    def setup() {
        buildFile = projectFolder.newFile('build.gradle')
    }

    @Unroll
    def "Plugin can be used with Gradle #gradleVersion"() {
        given:
        buildFile << """
            buildscript {
                dependencies {
                    classpath files('${new File("build/classes/main").getAbsolutePath()}',
                            '${new File("build/resources/main").getAbsolutePath()}',
                            '${new File("build/libs/maven-repack-3.0.4.jar").getAbsolutePath()}')
                }
            }

            apply plugin: 'io.spring.dependency-management'
            apply plugin: 'java'

            repositories {
                mavenCentral()
            }

            dependencyManagement {
                imports {
                    mavenBom 'org.springframework.boot:spring-boot-dependencies:1.4.2.RELEASE'
                }
            }

            dependencies {
                compile 'org.springframework.boot:spring-boot-starter'
            }

            task resolve {
                doLast {
                    def names = configurations.compile.resolve().collect { it.name }
                    if (!names.containsAll("spring-boot-starter-1.4.2.RELEASE.jar", "spring-boot-1.4.2.RELEASE.jar",
                            "spring-boot-autoconfigure-1.4.2.RELEASE.jar",
                            "spring-boot-starter-logging-1.4.2.RELEASE.jar", "spring-core-4.3.4.RELEASE.jar",
                            "snakeyaml-1.17.jar", "spring-context-4.3.4.RELEASE.jar", "logback-classic-1.1.7.jar",
                            "jcl-over-slf4j-1.7.21.jar", "jul-to-slf4j-1.7.21.jar", "log4j-over-slf4j-1.7.21.jar",
                            "spring-aop-4.3.4.RELEASE.jar", "spring-beans-4.3.4.RELEASE.jar",
                            "spring-expression-4.3.4.RELEASE.jar", "logback-core-1.1.7.jar", "slf4j-api-1.7.21.jar")) {
                        throw new RuntimeException("Dependency were not resolved as expected")
                    }

                }
            }
        """

        when:
        def result = GradleRunner.create().withProjectDir(projectFolder.root).withArguments("resolve").build()

        then:
        result.task(":resolve").outcome == TaskOutcome.SUCCESS
        println result.output

        where:
        gradleVersion << ['2.9', '2.10', '2.11', '2.12', '2.13', '2.14', '2.14.1', '3.0', '3.1', '3.2']
    }

}
