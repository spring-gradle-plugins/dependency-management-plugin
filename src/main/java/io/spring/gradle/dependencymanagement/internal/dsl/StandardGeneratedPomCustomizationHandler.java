package io.spring.gradle.dependencymanagement.internal.dsl;

import java.util.Locale;

import io.spring.gradle.dependencymanagement.dsl.GeneratedPomCustomizationHandler;
import io.spring.gradle.dependencymanagement.internal.DependencyManagementSettings.PomCustomizationSettings;

/**
 * Standard implementation of {@link GeneratedPomCustomizationHandler}.
 *
 * @author Andy Wilkinson
 */
public class StandardGeneratedPomCustomizationHandler implements GeneratedPomCustomizationHandler {

    private final PomCustomizationSettings settings;

    StandardGeneratedPomCustomizationHandler(PomCustomizationSettings settings) {
        this.settings = settings;
    }

    @Override
    public void includeImportedBomsBy(String action) {
        includeImportedBomsBy(IncludeImportedBomAction.valueOf(action.toUpperCase(Locale.ENGLISH)));
    }

    @Override
    public void includeImportedBomsBy(IncludeImportedBomAction action) {
        this.settings.setIncludeImportedBomAction(action);
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.settings.setEnabled(enabled);
    }

    @Override
    public void enabled(boolean enabled) {
        this.settings.setEnabled(enabled);
    }

}
