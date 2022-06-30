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

package io.spring.gradle.dependencymanagement.internal.dsl;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import groovy.lang.GString;
import io.spring.gradle.dependencymanagement.dsl.DependenciesHandler;
import io.spring.gradle.dependencymanagement.dsl.DependencySetHandler;
import io.spring.gradle.dependencymanagement.internal.DependencyManagementContainer;
import io.spring.gradle.dependencymanagement.internal.Exclusion;
import org.codehaus.groovy.runtime.GStringImpl;
import org.gradle.api.Action;
import org.gradle.api.artifacts.Configuration;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link StandardDependenciesHandler}.
 *
 * @author Andy Wilkinson
 *
 */
class StandardDependenciesHandlerTests {

	private final DependencyManagementContainer container = mock(DependencyManagementContainer.class);

	private final Configuration configuration = mock(Configuration.class);

	private final DependenciesHandler handler = new StandardDependenciesHandler(this.container, this.configuration);

	@Test
	@SuppressWarnings({ "rawtypes", "unchecked" })
	void dependencyCanBeConfiguredUseAMapWithGStringValues() {
		Map dependencyId = new HashMap();
		dependencyId.put("group", gstring("com.example"));
		dependencyId.put("name", gstring("example"));
		dependencyId.put("version", gstring("1.0"));
		this.handler.dependency(dependencyId);
		then(this.container).should().addManagedVersion(this.configuration, "com.example", "example", "1.0",
				Collections.<Exclusion>emptyList());
	}

	@Test
	void dependencyConfiguredWithAnEmptyArtifactIdIsRejected() {
		try {
			this.handler.dependency("com.example::1.0");
			fail("Exception was not thrown");
		}
		catch (Exception ex) {
			assertThat(ex.getMessage()).isEqualTo(
					"Dependency identifier 'com.example::1.0' is malformed. The required form is 'group:name:version'");
		}
	}

	@Test
	@SuppressWarnings({ "rawtypes", "unchecked" })
	void dependencySetCanBeConfiguredUseAMapWithGStringValues() {
		Map dependencyId = new HashMap();
		dependencyId.put("group", gstring("com.example"));
		dependencyId.put("name", gstring("example"));
		dependencyId.put("version", gstring("1.0"));
		this.handler.dependency(dependencyId);
		then(this.container).should().addManagedVersion(this.configuration, "com.example", "example", "1.0",
				Collections.<Exclusion>emptyList());
	}

	@Test
	void aDependencySetConfiguredUsingAMapWithoutAGroupIsRejected() {
		Map<String, String> setId = new HashMap<>();
		setId.put("version", "1.7.7");
		try {
			this.handler.dependencySet(setId, (Action<DependencySetHandler>) null);
			fail("Exception was not thrown");
		}
		catch (Exception ex) {
			assertThat(ex.getMessage()).isEqualTo("A dependency set requires both a group and a version");
		}
	}

	@Test
	void aDependencySetConfiguredUsingAMapWithoutAVersionIsRejected() {
		Map<String, String> setId = new HashMap<>();
		setId.put("group", "com.example");
		try {
			this.handler.dependencySet(setId, (Action<DependencySetHandler>) null);
			fail("Exception was not thrown");
		}
		catch (Exception ex) {
			assertThat(ex.getMessage()).isEqualTo("A dependency set requires both a group and a version");
		}
	}

	@Test
	void anExclusionUsingAStringInTheWrongFormatIsRejected() {
		try {
			this.handler.dependency("com.example:example:1.0",
					(dependencyHandler) -> dependencyHandler.exclude("malformed"));
			fail("Exception was not thrown");
		}
		catch (Exception ex) {
			assertThat(ex.getMessage())
					.isEqualTo("Exclusion 'malformed' is malformed. The required form is 'group:name'");
		}
	}

	@Test
	void anExclusionConfiguredUsingAMapWithoutANameIsRejected() {
		try {
			this.handler.dependency("com.example:example:1.0", (dependencyHandler) -> {
				Map<String, String> exclusion = new HashMap<>();
				exclusion.put("group", "com.example");
				dependencyHandler.exclude(exclusion);
			});
			fail("Exception was not thrown");
		}
		catch (Exception ex) {
			assertThat(ex.getMessage()).isEqualTo("An exclusion requires both a group and a name");
		}
	}

	@Test
	void anExclusionConfiguredUsingAMapWithoutAGroupIsRejected() {
		try {
			this.handler.dependency("com.example:example:1.0", (dependencyHandler) -> {
				Map<String, String> exclusion = new HashMap<>();
				exclusion.put("name", "example-core");
				dependencyHandler.exclude(exclusion);
			});
			fail("Exception was not thrown");
		}
		catch (Exception ex) {
			assertThat(ex.getMessage()).isEqualTo("An exclusion requires both a group and a name");
		}
	}

	private GString gstring(String string) {
		return new GStringImpl(new Object[0], new String[] { string });
	}

}
