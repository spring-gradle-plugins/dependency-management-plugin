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

import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * A set of dependency exclusions
 *
 * @author Andy Wilkinson
 */
class Exclusions {

    private final Logger log = LoggerFactory.getLogger(Exclusions)

    private Map<String, Set<String>> exclusionsByDependency = [:]

    void add(String dependency, Collection<String> exclusionsForDependency) {
        Set existingExclusions = exclusionsByDependency.get(dependency, [] as Set)
        existingExclusions.addAll(exclusionsForDependency)
    }

    void addAll(Exclusions toAdd) {
        toAdd.exclusionsByDependency.each { dependency, exclusionsForDependency ->
            Set existingExclusions = exclusionsByDependency.get(dependency, [] as Set)
            existingExclusions.addAll(exclusionsForDependency)
        }
    }

    Set<String> exclusionsForDependency(String dependency) {
        exclusionsByDependency[dependency]
    }

    Map<String, Set<String>> all() {
        exclusionsByDependency
    }

    String toString() {
        return exclusionsByDependency.toString()
    }
}
