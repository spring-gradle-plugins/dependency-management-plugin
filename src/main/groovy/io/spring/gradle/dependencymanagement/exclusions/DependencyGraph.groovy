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

import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.artifacts.result.ResolvedDependencyResult

/**
 * A model of a dependency graph that can be visited with a closure
 *
 * @author Andy Wilkinson
 */
class DependencyGraph {

    private final DependencyGraphNode root

    DependencyGraph(ResolvedComponentResult root) {
        this.root = process(null, root)
    }

    void accept(Closure visitor) {
        this.root.accept(visitor)
    }

    void acceptUnique(Closure visitor) {
        def seen = [] as Set
        this.root.accept(seen, visitor)
    }

    private DependencyGraphNode process(DependencyGraphNode parent,
            ResolvedComponentResult dependency) {

        DependencyGraphNode node = new DependencyGraphNode(parent, dependency)

        if (parent) {
            parent.children << node
        }

        def dependencies = dependency.dependencies
                .findAll { it instanceof ResolvedDependencyResult }
                .collect { ResolvedDependencyResult it -> it.selected }

        if (dependencies) {
            dependencies.each { process(node, it) }
        }
        node
    }

    static class DependencyGraphNode {

        final DependencyGraphNode parent

        final ResolvedComponentResult dependency

        final String id

        final List<DependencyGraphNode> children = []

        final List<String> exclusions = []

        DependencyGraphNode(DependencyGraphNode parent, ResolvedComponentResult dependency) {
            this.parent = parent;
            this.dependency = dependency
            this.id = "$dependency.moduleVersion.group:$dependency.moduleVersion.name"
        }

        private accept(seen, Closure closure) {
            if (!seen.contains(this.id)) {
                seen.add(this.id)
                closure.call(this)
                children.each { it.accept(seen, closure) }
            }
        }

        private accept(Closure closure) {
            closure.call(this)
            children.each { it.accept(closure) }
        }
    }
}
