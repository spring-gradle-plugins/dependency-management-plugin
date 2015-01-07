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

package io.spring.gradle.dependencymanagement.exclusions

import io.spring.gradle.dependencymanagement.DependencyManagementContainer
import io.spring.gradle.dependencymanagement.DependencyManagementExtension
import io.spring.gradle.dependencymanagement.exclusions.DependencyGraph.DependencyGraphNode
import org.gradle.api.Action
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ModuleDependency
import org.gradle.api.artifacts.ResolvableDependencies
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * An {@link Action} to be applied to {@link ResolvableDependencies} that configures exclusions
 * based on the Maven exclusion metadata gleaned from the dependencies.
 *
 * @author Andy Wilkinson
 */
class ExclusionConfiguringAction implements Action<ResolvableDependencies> {

    private final Logger log = LoggerFactory.getLogger(ExclusionConfiguringAction)

    private final DependencyManagementExtension dependencyManagementExtension

    private final DependencyManagementContainer dependencyManagementContainer

    private final Configuration configuration

    private final ExclusionResolver exclusionResolver

    ExclusionConfiguringAction(DependencyManagementExtension dependencyManagementExtension,
            DependencyManagementContainer dependencyManagementContainer,
            Configuration configuration, ExclusionResolver exclusionResolver) {
        this.dependencyManagementExtension = dependencyManagementExtension
        this.dependencyManagementContainer = dependencyManagementContainer
        this.configuration = configuration
        this.exclusionResolver = exclusionResolver
    }

    @Override
    void execute(ResolvableDependencies resolvableDependencies) {
        if (this.dependencyManagementExtension.applyMavenExclusions) {
            applyMavenExclusions(resolvableDependencies)
        }
    }

    private void applyMavenExclusions(ResolvableDependencies resolvableDependencies) {
        def excludedDependencies = findExcludedDependencies()

        if (log.infoEnabled) {
            log.info "Excluding $excludedDependencies"
        }

        resolvableDependencies.dependencies
                .matching { it instanceof ModuleDependency}
                .all { ModuleDependency dependency ->
                    excludedDependencies.each { ResolvedComponentResult exclusion ->
                        dependency.exclude(group: exclusion.moduleVersion.group,
                                module:exclusion.moduleVersion.name)
                    }
        }
    }

    private Set findExcludedDependencies() {
        def configurationCopy = this.configuration.copyRecursive()
        def resolutionResult = configurationCopy.incoming.resolutionResult

        def dependencies = resolutionResult.allComponents

        def allExclusions = this.exclusionResolver.resolveExclusions(dependencies)
        allExclusions.addAll(this.dependencyManagementContainer.getExclusions(this.configuration))

        def exclusionCandidates = [] as Set
        exclusionCandidates.addAll(dependencies)

        def dependencyGraph = DependencyGraph.create(resolutionResult.root)

        removeUnexcludedDependencies(dependencyGraph, allExclusions, [] as Set, exclusionCandidates)

        exclusionCandidates
    }

    private void removeUnexcludedDependencies(DependencyGraphNode node, Exclusions allExclusions,
            Set<String> exclusions, Set exclusionCandidates) {
        if (exclusionCandidates.contains(node.dependency) && !exclusions.contains(node.id)) {
            exclusionCandidates.remove(node.dependency)
            if (log.debugEnabled) {
                log.debug "${node.id} is not excluded due to path ${getPath(node)}"
            }
            Set<String> exclusionsForChildren = new HashSet<String>(exclusions)
            def exclusionsForDependency = allExclusions.exclusionsForDependency(node.id)
            if (exclusionsForDependency) {
                exclusionsForChildren.addAll(allExclusions.exclusionsForDependency(node.id))
            }
            node.children.each {
                removeUnexcludedDependencies(it, allExclusions, exclusionsForChildren, exclusionCandidates)
            }
        }
    }

    private String getPath(DependencyGraphNode node) {
        String path = node.id
        def current = node.parent
        while (current != null) {
            path += " -> " + current.id
            current = current.parent
        }
        path
    }
}
