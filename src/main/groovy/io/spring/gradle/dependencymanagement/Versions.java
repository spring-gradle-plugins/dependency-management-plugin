/*
 * Copyright 2014-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.spring.gradle.dependencymanagement;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Utility methods for working with dependency versions.
 *
 * @author Andy Wilkinson
 */
class Versions {

    private static final Set<String> DYNAMIC_PREFIXES = Collections.unmodifiableSet(
            new HashSet<String>(Arrays.asList("latest.", "[", "]", "(")));

    static boolean isDynamic(String version) {
        for (String dynamicPrefix: DYNAMIC_PREFIXES) {
            if (version.startsWith(dynamicPrefix)) {
                return true;
            }
        }
        if (version.endsWith("+")) {
            return true;
        }
        return false;
    }

}
