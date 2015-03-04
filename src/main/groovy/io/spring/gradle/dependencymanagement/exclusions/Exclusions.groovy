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
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * A set of dependency exclusions
 *
 * @author Andy Wilkinson
 */
class Exclusions {

    private final Logger log = LoggerFactory.getLogger(Exclusions)

    private def exclusionsByExcluder = [:]

    void add(params) {
        def exclusion = getId(params.exclusion)
        def excluder = getId(params.from)

        if (log.debugEnabled) {
            log.debug "Adding exclusion of ${exclusion} by ${excluder}"
        }

        def exclusions = exclusionsByExcluder[excluder] ?: [] as Set
        exclusions << exclusion
        exclusionsByExcluder[excluder] = exclusions
    }

    void addAll(Exclusions newExclusions) {
        newExclusions.exclusionsByExcluder.each { excluder, exclusions ->
            Set existingExclusions = exclusionsByExcluder[excluder] ?: [] as Set
            existingExclusions.addAll(exclusions)
            exclusionsByExcluder[excluder] = existingExclusions
        }
    }

    def exclusionsForDependency(String dependency) {
        exclusionsByExcluder[dependency]
    }

    private String getId(def toIdentify) {
        toIdentify instanceof CharSequence ? toIdentify : "$toIdentify.groupId:$toIdentify.artifactId"
    }

    String toString() {
        return exclusionsByExcluder.toString()
    }
}
