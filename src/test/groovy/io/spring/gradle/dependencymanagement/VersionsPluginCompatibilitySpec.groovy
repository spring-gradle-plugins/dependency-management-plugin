/*
 * Copyright 2014-2017 the original author or authors.
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

import groovy.json.JsonSlurper
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

/**
 * Tests that verify the plugin's compatibility with the
 * <a href="https://github.com/ben-manes/gradle-versions-plugin>Versions plugin</a>.
 *
 * @author Andy Wilkinson
 */
class VersionsPluginCompatibilitySpec extends Specification {

    @Rule
    final TemporaryFolder projectFolder = new TemporaryFolder()

    private File buildFile

    def setup() {
        buildFile = projectFolder.newFile('build.gradle')
    }

    def "Versions plugin reports upgrades for dependencies with managed versions"() {
        given:
        this.buildFile << """
            buildscript {
                dependencies {
                    classpath files('${new File("build/classes/main").getAbsolutePath()}',
                            '${new File("build/resources/main").getAbsolutePath()}',
                            '${new File("build/libs/maven-repack-3.0.4.jar").getAbsolutePath()}')
                }
            }
            plugins {
                id 'com.github.ben-manes.versions' version '0.14.0'
            }

            apply plugin: 'io.spring.dependency-management'
            apply plugin: 'java'

            repositories {
                mavenCentral()
            }

            dependencyManagement {
                dependencies {
                    dependency 'commons-logging:commons-logging:1.1.3'
                }
            }

            dependencies {
                compile 'commons-logging:commons-logging'
            }

            dependencyUpdates {
                outputFormatter = 'json'
            }
        """

        when:
        def result = GradleRunner.create().withProjectDir(projectFolder.root)
                .withArguments("dependencyUpdates").build()

        then:
        result.task(":dependencyUpdates").outcome == TaskOutcome.SUCCESS
        def report = new JsonSlurper().parse(new File(this.projectFolder.root, 'build/dependencyUpdates/report.json'))
        report.outdated.dependencies
                .collect { "${it.group}:${it.name}" as String }
                .contains 'commons-logging:commons-logging'
    }

}
