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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the plugin's compatibility with different versions of Gradle.
 *
 * @author Andy Wilkinson
 */
class GradleVersionCompatibilityIntegrationTests {

	@RegisterExtension
	private final GradleBuild gradleBuild = new GradleBuild();

	@ParameterizedTest(name = "Gradle {0}")
	@MethodSource("gradleVersions")
	void pluginIsCompatible(String gradleVersion, String configuration) {
		BuildResult result = this.gradleBuild.runner().withGradleVersion(gradleVersion)
				.withArguments("-Pconfiguration=" + configuration, "resolve").build();
		assertThat(result.task(":resolve").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
	}

	static List<String[]> gradleVersions() {
		List<String> compileVersions = Arrays.asList("2.9", "2.10", "2.11", "2.12", "2.13", "2.14.1", "3.0", "3.1",
				"3.2", "3.3", "3.4.1", "3.5.1", "4.0", "4.1", "4.2.1", "4.3.1", "4.4.1", "4.5.1", "4.6", "4.7", "4.8",
				"4.9", "4.10.3");
		List<String> implementationVersions = Arrays.asList("5.0", "5.1.1", "5.2.1", "5.3.1", "5.4.1", "5.5.1", "5.6.4",
				"6.0.1", "6.1.1", "6.2.2", "6.3", "6.4.1", "6.5.1", "6.6.1", "6.7.1", "6.8.3", "7.0.2", "7.1.1", "7.2",
				"7.3.3", "7.4.2");
		List<String[]> result = new ArrayList<>();
		for (String version : compileVersions) {
			result.add(new String[] { version, "compile" });
		}
		for (String version : implementationVersions) {
			result.add(new String[] { version, "implementation" });
		}
		return result;
	}

}
