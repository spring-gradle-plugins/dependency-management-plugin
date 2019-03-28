/*
 * Copyright 2014-2017 the original author or authors.
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

package io.spring.gradle.dependencymanagement.internal;

import java.util.HashMap;
import java.util.Map;

/**
 * Shared cache for found exclusions.
 *
 * @author Tom Briers
 */
final class ExclusionsCache {
    private static final Map<String, Exclusions> exclusionsCache = new HashMap<String, Exclusions>();

    private ExclusionsCache() {

    }

    static Exclusions get(String id) {
        return exclusionsCache.get(id);
    }

    static Exclusions put(String id, Exclusions exclusions) {
        return exclusionsCache.put(id, exclusions);
    }
}
