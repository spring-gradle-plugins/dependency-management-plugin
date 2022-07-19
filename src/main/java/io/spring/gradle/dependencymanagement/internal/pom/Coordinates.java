/*
 * Copyright 2014-2022 the original author or authors.
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

package io.spring.gradle.dependencymanagement.internal.pom;

/**
 * Coordinates (group ID, artifact ID, and version) for a Maven artifact.
 *
 * @author Andy Wilkinson
 */
public class Coordinates {

	private final String groupId;

	private final String artifactId;

	private final String version;

	/**
	 * Creates a new {@code Coordinates} with the given {@code groupId},
	 * {@code artifactId}, and {@code version}.
	 * @param groupId the group ID
	 * @param artifactId the artifact ID
	 * @param version the version
	 */
	public Coordinates(String groupId, String artifactId, String version) {
		this.groupId = groupId;
		this.artifactId = artifactId;
		this.version = version;
	}

	/**
	 * Returns the coordinates' group ID.
	 * @return the group ID
	 */
	public String getGroupId() {
		return this.groupId;
	}

	/**
	 * Returns the coordinates' artifact ID.
	 * @return the artifact ID
	 */
	public String getArtifactId() {
		return this.artifactId;
	}

	/**
	 * Returns the coordinates' version.
	 * @return the version
	 */
	public String getVersion() {
		return this.version;
	}

	public String getGroupAndArtifactId() {
		return String.format("%s:%s", getGroupId(), getArtifactId());
	}

	@Override
	public String toString() {
		return String.format("%s:%s:%s", getGroupId(), getArtifactId(), getVersion());
	}

}
