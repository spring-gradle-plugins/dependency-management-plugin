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
import io.spring.gradle.dependencymanagement.maven.EffectiveModelBuilder
import io.spring.gradle.dependencymanagement.maven.ModelExclusionCollector
import io.spring.gradle.dependencymanagement.exclusions.DependencyGraph.DependencyGraphNode
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ResolvableDependencies
import org.gradle.api.artifacts.ResolveException
import org.gradle.api.artifacts.result.ResolvedDependencyResult
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

    private DependencyManagementContainer dependencyManagementContainer

    private Configuration configuration

    private Project project

    private final EffectiveModelBuilder effectiveModelBuilder

    ExclusionConfiguringAction(
            DependencyManagementContainer dependencyManagementContainer,
            Configuration configuration, Project project) {
        this.dependencyManagementContainer = dependencyManagementContainer
        this.configuration = configuration
        this.project = project
        this.effectiveModelBuilder = new EffectiveModelBuilder(project)
    }

    @Override
    void execute(ResolvableDependencies resolvableDependencies) {
        def configurationCopy = configuration.copyRecursive()
        def dependencyManagementExclusions =
                dependencyManagementContainer.getExclusions(configuration)
        def candidateExclusions = collectExclusions(configurationCopy.incoming.resolutionResult
                .root, dependencyManagementExclusions)
        candidateExclusions.
                addAll(dependencyManagementContainer.getExclusions(configuration))
        def paths = getPathsToRootForExclusionCandidates(candidateExclusions,
                configurationCopy.incoming.resolutionResult.root)

        def exclusions = candidateExclusions.collect { candidate, value ->
            def pathsForCandidate = paths[candidate]
            def unexcludedPaths = pathsForCandidate.findAll { path ->
                !value.any { path.contains(it) }
            }
            if (!unexcludedPaths) {
                candidate
            }
            else {
                log.debug(
                        "$candidate will not be excluded due to path(s) $unexcludedPaths")
            }
        }.findAll { it != null }

        exclusions.each {
            def (group, module) = it.split(':')
            log.debug("Excluding $it from $configuration.name configuration")
            configuration.exclude(group: group, module: module)
        }
    }

    def collectExclusions(root, dependencyManagementExclusions) {
        def exclusions = new Exclusions()
        root.dependencies.findAll { it instanceof ResolvedDependencyResult }
                .collect { it.selected }
                .each { dependency ->
            exclusions.addAll(collectExclusions(dependency, [], new Exclusions(),
                    dependencyManagementExclusions))
        }
        exclusions
    }

    def collectExclusions(dependency, processed, ancestorExclusions,
            dependencyManagementExclusions) {
        Exclusions exclusionsForDependency = new Exclusions()

        def pomDependency = getPomDependency(dependency)

        if (pomDependency) {
            if (!processed.contains(pomDependency)) {
                processed << pomDependency
                ancestorExclusions.each { exclusion, excluders ->
                    exclusionsForDependency.add(exclusion: exclusion,
                            from: [groupId: pomDependency.group, artifactId: pomDependency.name])
                }
                def dependencyId = "$pomDependency.group:$pomDependency.name"
                def dependencyManagementExclusionsForDependency =
                        dependencyManagementExclusions.exclusionsForDependency(dependencyId)
                if (dependencyManagementExclusionsForDependency) {
                    dependencyManagementExclusionsForDependency.each { exclusion ->
                        exclusionsForDependency.add(exclusion: exclusion,
                                from: [groupId: pomDependency.group, artifactId: pomDependency.name])
                    }
                }

                exclusionsForDependency.addAll(ancestorExclusions)
                if (pomDependency) {
                    def configuration = project.configurations.
                            detachedConfiguration(pomDependency)
                    try {
                        def files = configuration.resolve()
                        if (files) {
                            def pom = files.iterator().next()
                            def model = effectiveModelBuilder.buildModel(pom)
                            def exclusionsFromModel = new ModelExclusionCollector().
                                    collectExclusions(model)
                            exclusionsForDependency.addAll(exclusionsFromModel)
                        }
                    }
                    catch (ResolveException ex) {
                        log.debug("Failed to resolve $pomDependency")
                    }
                }
            }
        }

        dependency.dependencies
                .findAll { it instanceof ResolvedDependencyResult }
                .collect { it.selected }
                .each {
            exclusionsForDependency.addAll(collectExclusions(it, processed,
                    exclusionsForDependency, dependencyManagementExclusions))
        }

        exclusionsForDependency
    }

    def getPomDependency(dependency) {
        def moduleVersion = dependency.moduleVersion
        if (moduleVersion.group && moduleVersion.name && moduleVersion.version) {
            project.dependencies.create(
                    "$moduleVersion.group:$moduleVersion.name:$moduleVersion.version@pom")
        }
    }

    def getPathsToRootForExclusionCandidates(Exclusions exclusions, dependency) {
        def paths = [:]
        new DependencyGraph(dependency).accept { DependencyGraphNode node ->
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
