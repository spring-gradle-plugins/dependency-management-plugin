/*
 * Copyright 2014-2023 the original author or authors.
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

import io.spring.gradle.dependencymanagement.org.apache.maven.model.Model;
import io.spring.gradle.dependencymanagement.org.apache.maven.model.building.ModelBuildingRequest;
import io.spring.gradle.dependencymanagement.org.apache.maven.model.building.ModelProblemCollector;
import io.spring.gradle.dependencymanagement.org.apache.maven.model.interpolation.DefaultModelVersionProcessor;
import io.spring.gradle.dependencymanagement.org.apache.maven.model.validation.DefaultModelValidator;
import io.spring.gradle.dependencymanagement.org.apache.maven.model.validation.ModelValidator;
import org.gradle.api.Action;

/**
 * A {@link ModelValidator} that is more relaxed than {@link DefaultModelValidator}.
 *
 * @author Andy Wilkinson
 */
class RelaxedModelValidator extends DefaultModelValidator {

	RelaxedModelValidator() {
		super(new DefaultModelVersionProcessor());
	}

	@Override
	public void validateRawModel(Model model, ModelBuildingRequest request, ModelProblemCollector problems) {
		withNoDistributionManagementStatus(model,
				(modifiedModel) -> RelaxedModelValidator.super.validateRawModel(modifiedModel, request, problems));
	}

	@Override
	public void validateEffectiveModel(Model model, ModelBuildingRequest request, ModelProblemCollector problems) {
		withNoDistributionManagementStatus(model,
				(modifiedModel) -> RelaxedModelValidator.super.validateEffectiveModel(modifiedModel, request,
						problems));
	}

	private void withNoDistributionManagementStatus(Model model, Action<Model> action) {
		if (model.getDistributionManagement() != null) {
			String status = model.getDistributionManagement().getStatus();
			model.getDistributionManagement().setStatus(null);
			try {
				action.execute(model);
			}
			finally {
				model.getDistributionManagement().setStatus(status);
			}
		}
		else {
			action.execute(model);
		}
	}

}
