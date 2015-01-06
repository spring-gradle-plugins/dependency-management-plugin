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
        def configurationCopy = configuration.copyRecursive()
        def root = configurationCopy.incoming.resolutionResult.root
        def dependencyGraph = new DependencyGraph(root)

        applyExclusionsToDependencyGraph(dependencyGraph)

        Exclusions exclusionCandidates = findExclusionCandidates(dependencyGraph)

        findExclusionsToApply(exclusionCandidates, dependencyGraph).each {
            resolvableDependencies.dependencies
                .matching { it instanceof ModuleDependency }
                .all { ModuleDependency dependency ->
                    log.debug("Excluding {} from {}", it, "$dependency.group:$dependency.name")
                    def (group, module) = it.split(':')
                    ((ModuleDependency)dependency).exclude(group: group, module: module)
                }
        }
    }

    private void applyExclusionsToDependencyGraph(DependencyGraph dependencyGraph) {
        Map<String, Exclusions> allExclusions = resolveExclusions(dependencyGraph)
        dependencyGraph.acceptUnique { DependencyGraphNode node ->
            if (node.parent) {
                def exclusions = allExclusions[node.parent.id]
                if (exclusions) {
                    def exclusionsForDependency = exclusions.exclusionsForDependency(node.id)
                    if (exclusionsForDependency) {
                        node.exclusions.addAll(exclusionsForDependency)
                    }
                }
            }
        }
    }

    private Map<String, Exclusions> resolveExclusions(DependencyGraph dependencyGraph) {
        def dependencies = []
        dependencyGraph.acceptUnique { DependencyGraphNode node ->
            dependencies.add(node.dependency)
        }
        this.exclusionResolver.resolveExclusions(dependencies)
    }

    private Exclusions findExclusionCandidates(DependencyGraph dependencyGraph) {
        Exclusions candidateExclusions = new Exclusions()

        def dependencyManagementExclusions =
                this.dependencyManagementContainer.getExclusions(this.configuration)

        dependencyGraph.acceptUnique { DependencyGraphNode node ->
            log.debug "Determining exclusions for $node.id"

            dependencyManagementExclusions.exclusionsForDependency(node.id).each {
                candidateExclusions.add(from: node.id, exclusion: it)
            }

            node.exclusions.each {
                candidateExclusions.add(from: node.id, exclusion: it)
            }

            if (node.parent) {
                candidateExclusions.exclusionsForDependency(node.parent.id).each {
                    candidateExclusions.add(from: node.id, exclusion: it)
                }
            }
        }
        candidateExclusions
    }

    private findExclusionsToApply(Exclusions exclusionCandidates, dependencyGraph) {
        def paths = getPathsToRootForExclusionCandidates(exclusionCandidates, dependencyGraph)

        exclusionCandidates.collect { candidate, value ->
            def pathsForCandidate = paths[candidate]
            log.debug("Paths for $candidate: $pathsForCandidate")
            log.debug("$value")
            def unexcludedPaths = pathsForCandidate.findAll { path ->
                !value.any { path.contains(it) }
            }
            if (!unexcludedPaths) {
                return candidate
            }
            else {
                log.debug(
                        "$candidate will not be excluded due to path(s) $unexcludedPaths")
                return null
            }
        }.findAll { it != null }
    }

    private getPathsToRootForExclusionCandidates(Exclusions exclusions,
            DependencyGraph dependencyGraph) {
        def paths = [:]
        dependencyGraph.accept { DependencyGraphNode node ->
            if (exclusions.containsExclusionFor(node.id)) {
                def pathsForDependency = paths[node.id]
                if (!pathsForDependency) {
                    pathsForDependency = []
                    paths[node.id] = pathsForDependency
                }
                def path = []
                DependencyGraphNode current = node.parent;
                while (current != null) {
                    path << current.id
                    current = current.parent
                }
                pathsForDependency.add(path)
            }
        }
        paths
    }
}
