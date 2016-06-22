package io.spring.gradle.dependencymanagement.maven;

import io.spring.gradle.dependencymanagement.org.apache.maven.model.Model;
import io.spring.gradle.dependencymanagement.org.apache.maven.model.building.ModelBuildingRequest;
import io.spring.gradle.dependencymanagement.org.apache.maven.model.building.ModelProblemCollector;
import io.spring.gradle.dependencymanagement.org.apache.maven.model.interpolation.ModelInterpolator;
import io.spring.gradle.dependencymanagement.org.apache.maven.model.interpolation.StringSearchModelInterpolator;
import io.spring.gradle.dependencymanagement.org.apache.maven.model.path.DefaultPathTranslator;
import io.spring.gradle.dependencymanagement.org.apache.maven.model.path.DefaultUrlNormalizer;
import io.spring.gradle.dependencymanagement.org.codehaus.plexus.interpolation.MapBasedValueSource;
import io.spring.gradle.dependencymanagement.org.codehaus.plexus.interpolation.PropertiesBasedValueSource;
import io.spring.gradle.dependencymanagement.org.codehaus.plexus.interpolation.ValueSource;
import org.gradle.api.Project;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * A {@link ModelInterpolator} that uses a Gradle project's properties as a {@link ValueSource}
 *
 * @author Andy Wilkinson
 */
class ProjectPropertiesModelInterpolator extends StringSearchModelInterpolator {

    private final Project project;

    private final Map<String, String> additionalProperties;

    ProjectPropertiesModelInterpolator(Project project, Map<String, String> additionalProperties) {
        this.project = project;
        this.additionalProperties = additionalProperties;
        setUrlNormalizer(new DefaultUrlNormalizer());
        setPathTranslator(new DefaultPathTranslator());
    }

    public List<ValueSource> createValueSources(Model model, File projectDir,
            ModelBuildingRequest request, ModelProblemCollector collector) {
        List<ValueSource> valueSources = new ArrayList<ValueSource>(
                Arrays.asList(new MapBasedValueSource(this.additionalProperties),
                        new MapBasedValueSource(project.getProperties()),
                        new PropertiesBasedValueSource(System.getProperties())));
        valueSources.addAll(super.createValueSources(model, projectDir, request, collector));
        return valueSources;
    }


}
