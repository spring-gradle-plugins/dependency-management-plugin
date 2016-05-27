/*
 * Copyright 2014-2016 the original author or authors.
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

package io.spring.gradle.dependencymanagement.exclusions;

import java.util.*;

/**
 * A set of dependency exclusions
 *
 * @author Andy Wilkinson
 */
public class Exclusions {

    private final Map<String, Set<String>> exclusionsByDependency = new HashMap<String, Set<String>>();

    void add(String dependency, Collection<String> exclusionsForDependency) {
        Set<String> exclusions = this.exclusionsByDependency.get(dependency);
        if (exclusions == null) {
            exclusions = new HashSet<String>();
            exclusionsByDependency.put(dependency, exclusions);
        }

        exclusions.addAll(exclusionsForDependency);
    }

    void addAll(Exclusions toAdd) {
        for (Map.Entry<String, Set<String>> entry : toAdd.exclusionsByDependency.entrySet()) {
            add(entry.getKey(), entry.getValue());
        }
    }

    Set<String> exclusionsForDependency(String dependency) {
        return exclusionsByDependency.get(dependency);
    }

    Map<String, Set<String>> all() {
        return exclusionsByDependency;
    }

    public String toString() {
        return exclusionsByDependency.toString();
    }

}
