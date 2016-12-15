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

package io.spring.gradle.dependencymanagement.internal.pom;

import java.util.List;

/**
 * A {@code PomResolver} creates {@link Pom Poms} from {@link PomReference PomReferences}.
 *
 * @author Andy Wilkinson
 */
public interface PomResolver {

    /**
     * Resolves the given {@code pomReferences}, failing if any references cannot be resolved.
     *
     * @param pomReferences the pom references to resolve
     * @return the poms resolved from the references
     */
    List<Pom> resolvePoms(List<PomReference> pomReferences);

    /**
     * Resolves the given {@code pomReferences}, ignoring any references that cannot be resolved.
     *
     * @param pomReferences the pom references to resolve
     * @return the poms resolved from the references
     */
    List<Pom> resolvePomsLeniently(List<PomReference> pomReferences);

}
