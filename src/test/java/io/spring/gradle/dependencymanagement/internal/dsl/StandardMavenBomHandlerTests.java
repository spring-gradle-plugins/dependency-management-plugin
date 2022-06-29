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

import java.util.HashMap;
import java.util.Map;

import groovy.lang.GString;
import org.codehaus.groovy.runtime.GStringImpl;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link StandardMavenBomHandler}.
 *
 * @author Andy Wilkinson
 */
public class StandardMavenBomHandlerTests {

	private final StandardMavenBomHandler handler = new StandardMavenBomHandler();

	@Test
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void bomPropertiesCanBeConfiguredUsingAMapContainingGString() {
		Map properties = new HashMap();
		properties.put(gstring("example.version"), gstring("1.0"));
		this.handler.bomProperties(properties);
		Object version = this.handler.getBomProperties().getProperty("example.version");
		assertThat(version).isInstanceOf(String.class).isEqualTo("1.0");
	}

	private GString gstring(String string) {
		return new GStringImpl(new Object[0], new String[] { string });
	}

}
