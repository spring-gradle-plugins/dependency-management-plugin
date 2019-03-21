/*
 * Copyright 2014-2017 the original author or authors.
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

package io.spring.gradle.dependencymanagement.internal.maven

import io.spring.gradle.dependencymanagement.internal.DependencyManagementConfigurationContainer
import io.spring.gradle.dependencymanagement.internal.pom.Coordinates
import io.spring.gradle.dependencymanagement.internal.pom.PomReference
import io.spring.gradle.dependencymanagement.internal.properties.MapPropertySource
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification

/**
 * Tests for {@link MavenPomResolver}.
 *
 * @author Andy Wilkinson
 */
class MavenPomResolverSpec extends Specification {

    Project project

    MavenPomResolver resolver

    def setup() {
        this.project = new ProjectBuilder().build()
        this.resolver = new MavenPomResolver(this.project,
                new DependencyManagementConfigurationContainer(this.project));
        this.project.repositories {
            mavenCentral()
            maven { url new File("src/test/resources/maven-repo").toURI().toURL().toString() }
        }
    }

    def cleanup() {
        this.project.projectDir.deleteDir()
    }

    def 'Pom can be resolved leniently when it is only Maven 2.0 compatible'() {
        given: 'A reference to a pom that is not Maven 3.0 compatible'
            PomReference reference = new PomReference(new Coordinates("log4j", "log4j", "1.2.16"))
        when: 'The reference is resolved'
            def result = this.resolver.resolvePomsLeniently([reference])
        then: 'It was successful'
            result.size() == 1
    }

    def 'Pom can be resolved when it is only Maven 2.0 compatible'() {
        given: 'A reference to a pom that is not Maven 3.0 compatible'
        PomReference reference = new PomReference(new Coordinates("log4j", "log4j", "1.2.16"))
        when: 'The reference is resolved'
        def result = this.resolver.resolvePoms([reference], new MapPropertySource([:]))
        then: 'It was successful'
        result.size() == 1
    }

    def 'Pom that results in a ModelBuildingException can still be resolved'() {
        given: 'A reference to a pom that contains a dependency with an illegal system path'
        PomReference reference = new PomReference(new Coordinates("test", "illegal-system-path", "1.0"))
        when: 'The reference is resolved'
        def result = this.resolver.resolvePoms([reference], new MapPropertySource([:]))
        then: 'It was successful'
        result.size() == 1
    }

}
