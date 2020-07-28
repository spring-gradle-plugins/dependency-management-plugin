/*
 * Copyright 2014-2020 the original author or authors.
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

/**
 * An exclusion of an artifact by group ID and artifact ID.
 *
 * @author Andy Wilkinson
 */
public class Exclusion {

    private final String groupId;

    private final String artifactId;

    /**
     * Creates a new {@code Exclusion} using the given {@code groupId} and {@code artifactId}.
     *
     * @param groupId group ID
     * @param artifactId artifact ID
     */
    public Exclusion(String groupId, String artifactId) {
        this.groupId = groupId;
        this.artifactId = artifactId;
    }

    /**
     * Returns the group ID that is excluded.
     * @return the group ID
     */
    public String getGroupId() {
        return groupId;
    }

    /**
     * Returns the artifact ID that is excluded.
     * @return the artifact ID
     */
    public String getArtifactId() {
        return artifactId;
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = 31 * result + artifactId.hashCode();
        result = 31 * result + groupId.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Exclusion other = (Exclusion) obj;
        if (!artifactId.equals(other.artifactId)) {
            return false;
        }
        if (!groupId.equals(other.groupId)) {
            return false;
        }
        return true;
    }

}
