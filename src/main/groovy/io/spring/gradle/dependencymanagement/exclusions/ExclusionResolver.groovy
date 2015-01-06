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
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Resolves the {@link Exclusions exclusions} for a {@link ResolvedComponentResult dependency}
 *
 * @author Andy Wilkinson
 */
class ExclusionResolver {

    private final Logger log = LoggerFactory.getLogger(ExclusionConfiguringAction)

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

    Map<String, Exclusions> resolveExclusions(Set<ResolvedComponentResult> resolvedComponents) {
        def dependencies = []
        def exclusions = [:]

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
            exclusions[id] = newExclusions
            this.exclusionsCache[id] = newExclusions
        }

        this.exclusionsCache.putAll(exclusions)

        return exclusions
    }
}
