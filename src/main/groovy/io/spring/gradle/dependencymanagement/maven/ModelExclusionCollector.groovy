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

import io.spring.gradle.dependencymanagement.exclusions.Exclusions
import org.gradle.mvn3.org.apache.maven.model.Model

/**
 * Collects exclusions from a Maven {@link Model}
 *
 * @author Andy Wilkinson
 */
class ModelExclusionCollector {

    /**
     * Collects the exclusions from the given {@code model}. Exclusions are
     * collected from dependencies in the model's dependency management and
     * its main dependencies
     *
     * @param model The model to collect the exclusions from
     * @return The collected exclusions
     */
    def collectExclusions(Model model) {
        def exclusions = new Exclusions()
        def dependencies = model?.dependencyManagement?.dependencies ?: []
        dependencies.addAll model?.dependencies ?: []
        dependencies.findAll { !it.isOptional()}
                .findAll { "provided" != it.scope }
                .findAll { "test" != it.scope }
                .each { dependency ->
            String dependencyId = "${dependency.groupId}:${dependency.artifactId}"
            if (dependency.exclusions) {
                exclusions.add(dependencyId, dependency.exclusions.collect { exclusion ->
                    "${exclusion.groupId}:${exclusion.artifactId}" as String
                } as Set<String>)
            }
        }
        exclusions
    }
}
