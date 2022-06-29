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

package io.spring.gradle.dependencymanagement.internal.properties;

import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ProjectPropertySource}.
 *
 * @author Andy Wilkinson
 */
public class ProjectPropertySourceTests {

	private final Project project = ProjectBuilder.builder().build();

	private final ProjectPropertySource propertySource = new ProjectPropertySource(this.project);

	@Test
	public void nullIsReturnedWhenProjectDoesNotHaveAProperty() {
		assertThat(this.propertySource.getProperty("does.not.exist")).isNull();
	}

	@Test
	public void propertyIsReturnedWhenProjectHasProperty() {
		this.project.getExtensions().getExtraProperties().set("alpha", "a");
		assertThat(this.propertySource.getProperty("alpha")).isEqualTo("a");
	}

	@Test
	public void nullIsReturnedWhenProjectHasProperty() {
		this.project.getExtensions().getExtraProperties().set("alpha", null);
		assertThat(this.propertySource.getProperty("alpha")).isNull();
	}

	@Test
	public void nullIsReturnedWhenVersionPropertyIsRetrieved() {
		this.project.setVersion("1.2.3");
		assertThat(this.propertySource.getProperty("version")).isNull();
	}

}
