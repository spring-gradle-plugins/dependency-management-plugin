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

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests that verify the plugin's compatibility with the
 * <a href="https://github.com/ben-manes/gradle-versions-plugin>Versions plugin</a>.
 *
 * @author Andy Wilkinson
 */
public class VersionsPluginCompatibilityIntegerationTests {

    @Rule
    public final GradleBuild gradleBuild = new GradleBuild();

    @Test
    public void versionsPluginReportsUpgradesForDependenciesWithManagedVersions() {
        BuildResult result = this.gradleBuild.runner().withArguments("dependencyUpdates").build();
        assertThat(result.task(":dependencyUpdates").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
        assertThat(result.getOutput()).contains("commons-logging:commons-logging [1.1.3 ->");
    }

}
