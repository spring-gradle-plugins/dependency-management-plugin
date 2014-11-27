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

/**
 * A set of dependency exclusions
 *
 * @author Andy Wilkinson
 */
class Exclusions {

    def exclusions = [:]

    void add(params) {
        def exclusion = getId(params.exclusion)
        def excluders = exclusions[exclusion] ?: [] as Set
        excluders << getId(params.from)
        exclusions[exclusion] = excluders
    }

    void addAll(Exclusions newExclusions) {
        newExclusions.each { exclusion, excluders ->
            def existingExcluders = exclusions[exclusion] ?: [] as Set
            existingExcluders.addAll(excluders)
            exclusions[exclusion] = existingExcluders
        }
    }

    void each(Closure c) {
        exclusions.each(c)
    }

    boolean containsExclusionFor(def dependency) {
        exclusions.keySet().contains(dependency)
    }

    def collect(Closure c) {
        exclusions.collect(c)
    }

    String toString() {
        exclusions.toString()
    }

    private String getId(def toIdentify) {
        return "$toIdentify.groupId:$toIdentify.artifactId"
    }
}
