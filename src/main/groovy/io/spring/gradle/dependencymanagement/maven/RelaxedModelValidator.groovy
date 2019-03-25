/*
 * Copyright 2014-2016 the original author or authors.
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

package io.spring.gradle.dependencymanagement.maven

import io.spring.gradle.dependencymanagement.org.apache.maven.model.Model
import io.spring.gradle.dependencymanagement.org.apache.maven.model.building.ModelBuildingRequest
import io.spring.gradle.dependencymanagement.org.apache.maven.model.building.ModelProblemCollector
import io.spring.gradle.dependencymanagement.org.apache.maven.model.validation.DefaultModelValidator
import io.spring.gradle.dependencymanagement.org.apache.maven.model.validation.ModelValidator

/**
 * A {@link ModelValidator} that is more relaxed than {@link DefaultModelValidator}
 *
 * @author Andy Wilkinson
 */
class RelaxedModelValidator extends DefaultModelValidator {

    @Override
    void validateRawModel(Model model, ModelBuildingRequest request,
            ModelProblemCollector problems) {
        withNoDistributionManagementStatus(model) {
            super.validateRawModel(model, request, problems)
        }
    }

    @Override
    void validateEffectiveModel(Model model, ModelBuildingRequest request,
            ModelProblemCollector problems) {
        withNoDistributionManagementStatus(model) {
            super.validateEffectiveModel(model, request, problems)
        }
    }

    private void withNoDistributionManagementStatus(Model model, Closure closure) {
        if (model.distributionManagement) {
            String distributionManagementStatus = model.distributionManagement.status
            model.distributionManagement.status = null
            try {
                closure.call()
            } finally {
                model.distributionManagement.status = distributionManagementStatus
            }
        } else {
            closure.call()
        }
    }
}
