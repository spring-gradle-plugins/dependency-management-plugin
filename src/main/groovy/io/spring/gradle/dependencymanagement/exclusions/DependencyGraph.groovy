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

import org.gradle.api.artifacts.result.DependencyResult
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.artifacts.result.ResolvedDependencyResult
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
        visit(root) { ResolvedComponentResult component, DependencyGraphNode previous ->
            DependencyGraphNode current = previous;
            String id = "${component.moduleVersion.group}:${component.moduleVersion.name}"
            while (current) {
                if (current.exclusions?.contains(id) || current.id == id) {
                    return null;
                }
                current = current.parent;
            }
            DependencyGraphNode node = new DependencyGraphNode(id, previous, component, dependencyManagementExclusions.exclusionsForDependency(id))
            if (previous) {
                previous.children.add(node);
            }
            return node
        }
    }

    private static DependencyGraphNode visit(ResolvedComponentResult component, Closure callback) {
        return doVisit(new HashSet<String>(), component, null, callback);
    }

    private static DependencyGraphNode doVisit(Set<String> seen, ResolvedComponentResult
            component, DependencyGraphNode previous, Closure<DependencyGraphNode> callback) {
        DependencyGraphNode result = callback.call(component, previous);
        if (result != null) {
            Set<? extends DependencyResult> dependencies = component.getDependencies();
            for (DependencyResult dependencyResult : dependencies) {
                if (dependencyResult instanceof ResolvedDependencyResult) {
                    ResolvedComponentResult child = ((ResolvedDependencyResult)dependencyResult).getSelected();
                    doVisit(seen, child, result, callback);
                }
            }
            return result
        }
        return previous
    }

    static class DependencyGraphNode {

        private static final Comparator NODE_COMPARATOR = { a, b -> return a.depth - b.depth} as Comparator

        final DependencyGraphNode parent

        final ResolvedComponentResult dependency

        final String id

        final List<DependencyGraphNode> children = []

        final int depth;

        final Set<String> exclusions

        DependencyGraphNode(String id, DependencyGraphNode parent, ResolvedComponentResult
                dependency, Set<String> exclusions) {
            this.parent = parent;
            if (this.parent) {
                this.depth = parent.depth + 1
            }
            else {
                this.depth = 0
            }
            this.dependency = dependency
            this.id = id
            this.exclusions = exclusions
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
                Collections.sort(nodes, NODE_COMPARATOR)
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
