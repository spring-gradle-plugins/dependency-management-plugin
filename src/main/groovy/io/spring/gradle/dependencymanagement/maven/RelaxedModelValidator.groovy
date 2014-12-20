package io.spring.gradle.dependencymanagement.maven

import org.gradle.mvn3.org.apache.maven.model.DistributionManagement
import org.gradle.mvn3.org.apache.maven.model.Model
import org.gradle.mvn3.org.apache.maven.model.building.ModelBuildingRequest
import org.gradle.mvn3.org.apache.maven.model.building.ModelProblemCollector
import org.gradle.mvn3.org.apache.maven.model.validation.DefaultModelValidator
import org.gradle.mvn3.org.apache.maven.model.validation.ModelValidator

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
