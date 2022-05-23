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
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the plugin's compatibility with different versions of Gradle.
 *
 * @author Andy Wilkinson
 */
@RunWith(Parameterized.class)
public class GradleVersionCompatibilityIntegrationTests {

    @Rule
    public final GradleBuild gradleBuild = new GradleBuild();

    @Parameter(0)
    public String gradleVersion;

    @Test
    public void pluginIsCompatible() {
        BuildResult result = this.gradleBuild.runner().withGradleVersion(this.gradleVersion).withArguments("resolve").build();
        assertThat(result.task(":resolve").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
    }

    @Parameters(name = "Gradle {0}")
    public static List<String[]> gradleVersions() {
        List<String> versions = Arrays.asList("2.9", "2.10", "2.11", "2.12", "2.13", "2.14.1", "3.0", "3.1", "3.2", "3.3", "3.4",
                "3.4.1", "3.5.1", "4.0", "4.1", "4.2.1", "4.3.1", "4.4.1", "4.5.1", "4.6", "4.7",
                "4.8", "4.9", "4.10.2");
        List<String[]> result = new ArrayList<String[]>();
        for (String version: versions) {
            result.add(new String[] { version });
        }
        return result;
    }

}
