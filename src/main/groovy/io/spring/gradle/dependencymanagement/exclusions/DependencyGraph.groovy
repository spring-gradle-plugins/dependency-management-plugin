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

import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * A model of a dependency graph
 *
 * @author Andy Wilkinson
 */
class DependencyGraph {

    private static Logger log = LoggerFactory.getLogger(DependencyGraph)

    static DependencyGraphNode create(ResolvedComponentResult root,
            Exclusions dependencyManagementExclusions) {
        use(ResolvedComponentResultCategory) {
            root.visit { def dependency, def parent ->
                DependencyGraphNode node = new DependencyGraphNode(parent?.node, dependency)
                Set ancestorExclusions
                if (parent) {
                    ancestorExclusions = new HashSet(parent.ancestorExclusions)
                    Set exclusionsForDependency =
                            dependencyManagementExclusions.exclusionsForDependency(parent.node.id)
                    if (exclusionsForDependency?.contains(node.id) || ancestorExclusions.contains (node.id)) {
                        log.debug("Excluding ${node.id} from ${parent.node.id} due to dependency " +
                                "management exclusion")
                        return null
                    }
                    if (exclusionsForDependency) {
                        ancestorExclusions.addAll(exclusionsForDependency)
                    }
                    parent.node.children << node
                }
                else {
                    ancestorExclusions = []
                }
                [ 'node':node, 'ancestorExclusions':ancestorExclusions]
            }.node
        }
    }

    static class DependencyGraphNode {

        private static final Comparator NODE_COMPARATOR = { a, b -> return a.depth - b.depth} as Comparator

        final DependencyGraphNode parent

        final ResolvedComponentResult dependency

        final String id

        final List<DependencyGraphNode> children = []

        final int depth;

        DependencyGraphNode(DependencyGraphNode parent, ResolvedComponentResult dependency) {
            this.parent = parent;
            if (this.parent) {
                this.depth = parent.depth + 1
            }
            else {
                this.depth = 0
            }
            this.dependency = dependency
            this.id = "$dependency.moduleVersion.group:$dependency.moduleVersion.name"
        }

        void applyExclusions(Map<String, Exclusions> dependencyExclusions) {
            doApplyExclusions(dependencyExclusions, [] as Set)
        }

        private void doApplyExclusions(Map<String, Exclusions> dependencyExclusions,
                Set<String> exclusionsFromAncestors) {
            exclusionsFromAncestors = new HashSet(exclusionsFromAncestors)
            if (parent) {
                Exclusions exclusions = dependencyExclusions[parent.id];
                Set<String> exclusionsForDependency = exclusions?.exclusionsForDependency(id)
                if (exclusionsForDependency || exclusionsFromAncestors) {
                    Iterator<DependencyGraphNode> each = children.iterator()
                    while (each.hasNext()) {
                        DependencyGraphNode child = each.next()
                        if (exclusionsForDependency?.contains(child.id) ||
                                exclusionsFromAncestors?.contains(child.id)) {
                            log.debug("Excluding ${child.id} from ${id}")
                            each.remove()
                        }
                    }
                }
                if (exclusionsForDependency) {
                    exclusionsFromAncestors.addAll(exclusionsForDependency)
                }
            }
            children.each { it.doApplyExclusions(dependencyExclusions, exclusionsFromAncestors) }
        }

        void prune() {
            Map<String, SortedSet<DependencyGraphNode>> nodesById = [:]
            collectById(nodesById)
            nodesById.each { key, List nodes ->
                nodes.sort(NODE_COMPARATOR)
                nodes.remove(0)
                nodes.each { node ->
                    node.parent.children.remove(node)
                }
            }
        }

        private void collectById(Map<String, SortedSet<DependencyGraphNode>> nodesById) {
            List<DependencyGraphNode> nodes = nodesById[id]
            if (!nodes) {
                nodes = []
                nodesById[id] = nodes
            }
            nodes.add this
            children.each { it.collectById(nodesById) }
        }

        public String toString() {
            return "${id} ${depth}"
        }

    }
}
