/*
 * Copyright 2014-2016 the original author or authors.
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

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification
import spock.lang.Unroll

/**
 * Tests that verify the plugin's compatibility with various versions of Gradle.
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

        where:
        gradleVersion << ['2.9', '2.10', '2.11', '2.12', '2.13', '2.14', '2.14.1', '3.0', '3.1', '3.2', '3.3', '3.4',
                          '3.4.1', '3.5', '3.5.1', '4.0', '4.1', '4.2']
    }

}
