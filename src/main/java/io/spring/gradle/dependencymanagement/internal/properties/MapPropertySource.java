/*
 * Copyright 2014-2017 the original author or authors.
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

package io.spring.gradle.dependencymanagement.internal.properties;

import java.util.Map;

/**
 * A {@link PropertySource} backs by a {@link Map}.
 *
 * @author Andy Wilkinson
 */
public class MapPropertySource implements PropertySource {

    private final Map<String, ? extends Object> properties;

    /**
     * Creates a new {@code MapPropertySource} backed by the given {@map}.
     *
     * @param map the map
     */
    public MapPropertySource(Map<String, ? extends Object> map) {
        this.properties = map;
    }

    @Override
    public Object getProperty(String name) {
        return this.properties.get(name);
    }

}
