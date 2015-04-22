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
        def root = configurationCopy.incoming.resolutionResult.root
        Set allDependencies = []
        use (ResolvedComponentResultCategory) {
            root.visit { def dependency, def parent ->
                allDependencies << dependency
            }
        }

        DependencyGraphNode dependencyGraph = DependencyGraph.create(root,
                this.dependencyManagementContainer.getExclusions(this.configuration))
        if (log.debugEnabled) {
            log.debug "Initial dependency graph:"
            dumpGraph(dependencyGraph)
        }

        def exclusionsFromDependencies = this.exclusionResolver.resolveExclusions(allDependencies)

        if (log.debugEnabled) {
            log.debug "Exclusions from dependencies:"
            exclusionsFromDependencies.each { pom, exclusionsByDependency ->
                log.debug "    ${pom} depends on:"
                exclusionsByDependency.all().each { dependency, exclusions ->
                    log.debug "        ${dependency} excludes:"
                    exclusions.each { log.debug "            ${it}" }
                }
            }
        }

        dependencyGraph.applyExclusions(exclusionsFromDependencies)
        if (log.debugEnabled) {
            log.debug "Dependency graph with exclusions applied:"
            dumpGraph(dependencyGraph)
        }

        dependencyGraph.prune()
        if (log.debugEnabled) {
            log.debug "Dependency graph after pruning:"
            dumpGraph(dependencyGraph)
        }

        Set unexcludedDependencies = []
        collectDependencies(dependencyGraph, unexcludedDependencies)

        allDependencies.removeAll(unexcludedDependencies)
        allDependencies
    }

    private void collectDependencies(DependencyGraphNode node, Set<ResolvedComponentResult> collected) {
        if (!collected.add(node.dependency)) {
            return;
        }
        node.children.each { collectDependencies(it, collected) }
    }

    private void dumpGraph(DependencyGraphNode node) {
        dumpGraph(node, "")
    }

    private void dumpGraph(DependencyGraphNode node, String indent) {
        log.debug("${indent}${node.id}")
        indent += "    "
        node.children.each { dumpGraph it, indent }
    }
}
