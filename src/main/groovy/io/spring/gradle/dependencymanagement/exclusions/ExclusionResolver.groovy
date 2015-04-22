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

package io.spring.gradle.dependencymanagement.exclusions

import io.spring.gradle.dependencymanagement.maven.EffectiveModelBuilder
import io.spring.gradle.dependencymanagement.maven.ModelExclusionCollector
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ResolvedArtifact
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.specs.Specs

/**
 * Resolves the {@link Exclusions exclusions} for a {@link ResolvedComponentResult dependency}
 *
 * @author Andy Wilkinson
 */
class ExclusionResolver {

    private final DependencyHandler dependencyHandler

    private final ConfigurationContainer configurationContainer

    private final EffectiveModelBuilder effectiveModelBuilder

    private final Map<String, Exclusions> exclusionsCache = [:]

    ExclusionResolver(DependencyHandler dependencyHandler,
            ConfigurationContainer configurationContainer,
            EffectiveModelBuilder effectiveModelBuilder) {
        this.dependencyHandler = dependencyHandler
        this.configurationContainer = configurationContainer
        this.effectiveModelBuilder = effectiveModelBuilder
    }

    Map<String, Exclusions> resolveExclusions(Collection<ResolvedComponentResult>
            resolvedComponents) {
        def dependencies = []
        Map<String, Exclusions> exclusions = [:]

        resolvedComponents
                .findAll { !(it.id instanceof ProjectComponentIdentifier) }
                .findAll { it.moduleVersion.group && it.moduleVersion.name && it.moduleVersion.version}
                .each {
                    def id = "$it.moduleVersion.group:$it.moduleVersion.name"
                    def existing = this.exclusionsCache[id]
                    if (existing) {
                        exclusions[id] = existing
                    } else {
                        dependencies << this.dependencyHandler
                                .create(id + ":$it.moduleVersion.version@pom")
                    }
                }

        def configuration = this.configurationContainer.detachedConfiguration(dependencies
                .toArray(new Dependency[dependencies.size()]))

        configuration.resolvedConfiguration.lenientConfiguration
                .getArtifacts(Specs.SATISFIES_ALL).each { ResolvedArtifact artifact ->
            def moduleId = artifact.moduleVersion.id
            def pom = artifact.file
            def model = this.effectiveModelBuilder.buildModel(pom)

            def newExclusions = new ModelExclusionCollector().collectExclusions(model)

            String id = "$moduleId.group:$moduleId.name"
            this.exclusionsCache[id] = newExclusions
            exclusions[id] = newExclusions
        }

        return exclusions
    }
}
