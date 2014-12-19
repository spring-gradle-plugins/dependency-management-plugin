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

import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * A set of dependency exclusions
 *
 * @author Andy Wilkinson
 */
class Exclusions {

    private final Logger log = LoggerFactory.getLogger(ExclusionConfiguringAction)

    def excludersByExclusion = [:]

    def exclusionsByExcluder = [:]

    void add(params) {
        def exclusion = getId(params.exclusion)
        def excluders = excludersByExclusion[exclusion] ?: [] as Set
        def excluder = getId(params.from)
        excluders << excluder
        excludersByExclusion[exclusion] = excluders

        def exclusions = exclusionsByExcluder[excluder] ?: [] as Set
        exclusions << exclusion
        exclusionsByExcluder[excluder] = exclusions

        log.debug("{} is excluded by {}", exclusion, excluder)
    }

    void addAll(Exclusions newExclusions) {
        newExclusions.each { exclusion, excluders ->
            def existingExcluders = excludersByExclusion[exclusion] ?: [] as Set
            existingExcluders.addAll(excluders)
            excludersByExclusion[exclusion] = existingExcluders
        }
        newExclusions.exclusionsByExcluder.each { excluder, exclusions ->
            def existingExclusions = exclusionsByExcluder[excluder] ?: [] as Set
            existingExclusions.addAll(exclusions)
            exclusionsByExcluder[excluder] = existingExclusions
        }
    }

    void each(Closure c) {
        excludersByExclusion.each(c)
    }

    def exclusionsForDependency(dependency) {
        exclusionsByExcluder[dependency]
    }

    boolean containsExclusionFor(def dependency) {
        excludersByExclusion.keySet().contains(dependency)
    }

    def collect(Closure c) {
        excludersByExclusion.collect(c)
    }

    String toString() {
        excludersByExclusion.toString()
    }

    private String getId(def toIdentify) {
        toIdentify instanceof String ? toIdentify : "$toIdentify.groupId:$toIdentify.artifactId"
    }
}
