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

package io.spring.gradle.dependencymanagement;

import java.nio.file.Paths;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for interacting with the {@link org.gradle.api.plugins.MavenPlugin}.
 *
 * @author Andy Wilkinson
 */
class MavenPluginIntegrationTests {

	@RegisterExtension
	private final GradleBuild gradleBuild = new GradleBuild();

	@Test
	void generatedPomsAreCustomized() {
		this.gradleBuild.runner().withArguments("install").build();
		assertThat(generatedPom()).nodeAtPath("//dependencyManagement").isNotNull();
	}

	@Test
	void customizationOfGeneratedPomsCanBeDisabled() {
		this.gradleBuild.runner().withArguments("install").build();
		assertThat(generatedPom()).nodeAtPath("//dependencyManagement").isNull();
	}

	@Test
	void pomCustomizationDoesNotStopCustomConfToScopeMappingsFromWorking() {
		this.gradleBuild.runner().withArguments("install").build();
		assertThat(generatedPom())
				.textAtPath("/project/dependencies/dependency[groupId/text()='commons-logging']/scope")
				.isEqualTo("compile");
	}

	private NodeAssert generatedPom() {
		return new NodeAssert(this.gradleBuild.runner().getProjectDir().toPath()
				.resolve(Paths.get("build", "poms", "pom-default.xml")));
	}

}
