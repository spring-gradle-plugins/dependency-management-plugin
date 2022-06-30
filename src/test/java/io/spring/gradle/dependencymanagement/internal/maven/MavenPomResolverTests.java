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

package io.spring.gradle.dependencymanagement.internal.maven;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import io.spring.gradle.dependencymanagement.internal.DependencyManagementConfigurationContainer;
import io.spring.gradle.dependencymanagement.internal.pom.Coordinates;
import io.spring.gradle.dependencymanagement.internal.pom.Pom;
import io.spring.gradle.dependencymanagement.internal.pom.PomReference;
import io.spring.gradle.dependencymanagement.internal.properties.MapPropertySource;
import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MavenPomResolver}.
 *
 * @author Andy Wilkinson
 *
 */
class MavenPomResolverTests {

	private final Project project = ProjectBuilder.builder().build();

	private final MavenPomResolver resolver = new MavenPomResolver(this.project,
			new DependencyManagementConfigurationContainer(this.project));

	MavenPomResolverTests() {
		this.project.getRepositories().mavenCentral();
		this.project.getRepositories()
				.maven((repository) -> repository.setUrl(new File("src/test/resources/maven-repo").getAbsoluteFile()));
	}

	@Test
	void pomCanBeResolvedLenientlyWhenItIsOnlyMaven20Compatibile() {
		PomReference reference = new PomReference(new Coordinates("log4j", "log4j", "1.2.16"));
		List<Pom> result = this.resolver.resolvePomsLeniently(Arrays.asList(reference));
		assertThat(result).hasSize(1);
	}

	@Test
	void pomCanBeResolvedWhenItIsOnlyMaven20Compatibile() {
		PomReference reference = new PomReference(new Coordinates("log4j", "log4j", "1.2.16"));
		List<Pom> result = this.resolver.resolvePoms(Arrays.asList(reference),
				new MapPropertySource(Collections.<String, String>emptyMap()));
		assertThat(result).hasSize(1);
	}

	@Test
	void pomThatResultsInAModelBuildingExceptionCanStillBeResolved() {
		PomReference reference = new PomReference(new Coordinates("test", "illegal-system-path", "1.0"));
		List<Pom> result = this.resolver.resolvePoms(Arrays.asList(reference),
				new MapPropertySource(Collections.<String, String>emptyMap()));
		assertThat(result).hasSize(1);
	}

}
