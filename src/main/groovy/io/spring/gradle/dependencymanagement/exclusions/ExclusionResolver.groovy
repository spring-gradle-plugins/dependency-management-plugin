package io.spring.gradle.dependencymanagement.exclusions

import io.spring.gradle.dependencymanagement.maven.EffectiveModelBuilder
import io.spring.gradle.dependencymanagement.maven.ModelExclusionCollector
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.artifacts.ResolveException
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.artifacts.result.ResolvedComponentResult
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

    Exclusions resolveExclusions(ResolvedComponentResult dependency) {
        def exclusions
        def pomDependency = getPomDependency(dependency)

        if (pomDependency) {
            exclusions = this.exclusionsCache[pomDependency]
            if (!exclusions) {
                def configuration = this.configurationContainer.detachedConfiguration(pomDependency)
                try {
                    def files = configuration.resolve()
                    if (files) {
                        def pom = files.iterator().next()
                        def model = this.effectiveModelBuilder.buildModel(pom)
                        exclusions = new ModelExclusionCollector().collectExclusions(model)
                        this.exclusionsCache[pomDependency] = exclusions
                    }
                }
                catch (ResolveException ex) {
                    log.debug("Failed to resolve $pomDependency")
                }
            }
        }

        if (!exclusions) {
            exclusions = new Exclusions()
        }

        return exclusions
    }

    private getPomDependency(dependency) {
        def moduleVersion = dependency.moduleVersion
        if (moduleVersion.group && moduleVersion.name && moduleVersion.version) {
            this.dependencyHandler.create(
                    "$moduleVersion.group:$moduleVersion.name:$moduleVersion.version@pom")
        }
    }
}
