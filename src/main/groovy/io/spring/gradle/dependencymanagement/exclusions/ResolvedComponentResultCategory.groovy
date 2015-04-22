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
import org.gradle.api.artifacts.result.ResolvedDependencyResult

/**
 * Category for {@link ResolvedComponentResult} that allows the result and its
 * dependency hierarchy to be visited.
 *
 * @author Andy Wilkinson
 */
@Category(ResolvedComponentResult)
class ResolvedComponentResultCategory {

    def visit(Closure visitor) {
        doVisit([] as Set, visitor, null)
    }

    def doVisit(Set<String> ancestors, Closure visitor, def result) {
        String id = "${moduleVersion.group}:${moduleVersion.name}"
        if (!ancestors.contains(id)) {
            result = visitor.call((ResolvedComponentResult) this, result)
            if (result) {
                ancestors = new HashSet(ancestors)
                ancestors.add(id)
                use(ResolvedComponentResultCategory) {
                    this.dependencies
                            .findAll { it instanceof ResolvedDependencyResult }
                            .collect { it.selected }
                            .each { it.doVisit(ancestors, visitor, result) }
                }
            }
        }
        result
    }
}
