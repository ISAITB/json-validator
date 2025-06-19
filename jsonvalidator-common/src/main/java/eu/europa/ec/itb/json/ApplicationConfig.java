/*
 * Copyright (C) 2025 European Union
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the European Commission - subsequent
 * versions of the EUPL (the "Licence"); You may not use this work except in compliance with the Licence.
 *
 * You may obtain a copy of the Licence at:
 *
 * https://interoperable-europe.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an
 * "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the Licence for
 * the specific language governing permissions and limitations under the Licence.
 */

package eu.europa.ec.itb.json;

import eu.europa.ec.itb.json.validation.ValidationConstants;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * The validator application's configuration.
 */
@Component
@ConfigurationProperties("validator")
public class ApplicationConfig extends eu.europa.ec.itb.validation.commons.config.ApplicationConfig {

    private Set<String> acceptedSchemaExtensions;
    private final Map<String, String> defaultLabels = new HashMap<>();
    private Set<String> acceptedMimeTypes;
    private String defaultContentToValidateDescription;
    private String defaultEmbeddingMethodDescription;
    private String defaultExternalSchemasDescription;
    private String defaultExternalSchemaCombinationApproachDescription;
    private String defaultValidationTypeDescription;
    private String defaultLocationAsPointerDescription;
    private String defaultAddInputToReportDescription;
    private String defaultLocaleDescription;

    /**
     * @return The set of accepted mime types for JSON input.
     */
    public Set<String> getAcceptedMimeTypes() {
        return acceptedMimeTypes;
    }

    /**
     * @param acceptedMimeTypes The set of accepted mime types for JSON input.
     */
    public void setAcceptedMimeTypes(Set<String> acceptedMimeTypes) {
        this.acceptedMimeTypes = acceptedMimeTypes;
    }

    /**
     * @return The file extensions to consider as JSON schemas when scanning local artifact files.
     */
    public Set<String> getAcceptedSchemaExtensions() {
        return acceptedSchemaExtensions;
    }

    /**
     * @param acceptedSchemaExtensions The file extensions to consider as JSON schemas when scanning local artifact files.
     */
    public void setAcceptedSchemaExtensions(Set<String> acceptedSchemaExtensions) {
        this.acceptedSchemaExtensions = acceptedSchemaExtensions;
    }

    /**
     * @return The default web service labels (per input).
     */
    public Map<String, String> getDefaultLabels() {
        return defaultLabels;
    }

    /**
     * @return The default description of the content to validate.
     */
    public String getDefaultContentToValidateDescription() {
        return defaultContentToValidateDescription;
    }

    /**
     * @param defaultContentToValidateDescription The default description of the content to validate.
     */
    public void setDefaultContentToValidateDescription(String defaultContentToValidateDescription) {
        this.defaultContentToValidateDescription = defaultContentToValidateDescription;
    }

    /**
     * @return The default description of the explicit embedding method.
     */
    public String getDefaultEmbeddingMethodDescription() {
        return defaultEmbeddingMethodDescription;
    }

    /**
     * @param defaultEmbeddingMethodDescription The default description of the explicit embedding method.
     */
    public void setDefaultEmbeddingMethodDescription(String defaultEmbeddingMethodDescription) {
        this.defaultEmbeddingMethodDescription = defaultEmbeddingMethodDescription;
    }

    /**
     * @return The default description of the validation type input.
     */
    public String getDefaultValidationTypeDescription() {
        return defaultValidationTypeDescription;
    }

    /**
     * @param defaultValidationTypeDescription The default description of the validation type input.
     */
    public void setDefaultValidationTypeDescription(String defaultValidationTypeDescription) {
        this.defaultValidationTypeDescription = defaultValidationTypeDescription;
    }

    /**
     * @return The default description for the input of user-provided schemas.
     */
    public String getDefaultExternalSchemasDescription() {
        return defaultExternalSchemasDescription;
    }

    /**
     * @param defaultExternalSchemasDescription The default description for the input of user-provided schemas.
     */
    public void setDefaultExternalSchemasDescription(String defaultExternalSchemasDescription) {
        this.defaultExternalSchemasDescription = defaultExternalSchemasDescription;
    }

    /**
     * @return The default description for the input on the desired location type.
     */
    public String getDefaultLocationAsPointerDescription() {
        return defaultLocationAsPointerDescription;
    }

    /**
     * @param defaultLocationAsPointerDescription The default description for the input on the desired location type.
     */
    public void setDefaultLocationAsPointerDescription(String defaultLocationAsPointerDescription) {
        this.defaultLocationAsPointerDescription = defaultLocationAsPointerDescription;
    }

    /**
     * @return The default description for the input on the combination approach for user-provided schemas.
     */
    public String getDefaultExternalSchemaCombinationApproachDescription() {
        return defaultExternalSchemaCombinationApproachDescription;
    }

    /**
     * @param defaultExternalSchemaCombinationApproachDescription The default description for the input on the combination approach for user-provided schemas.
     */
    public void setDefaultExternalSchemaCombinationApproachDescription(String defaultExternalSchemaCombinationApproachDescription) {
        this.defaultExternalSchemaCombinationApproachDescription = defaultExternalSchemaCombinationApproachDescription;
    }

    /**
     * @return The default web service input description for the locale to use.
     */
    public String getDefaultLocaleDescription() {
        return defaultLocaleDescription;
    }

    /**
     * @return The default web service input description for the add input to report option.
     */
    public String getDefaultAddInputToReportDescription() {
        return defaultAddInputToReportDescription;
    }

    /**
     * @param defaultAddInputToReportDescription The default web service input description for the add input to report option.
     */
    public void setDefaultAddInputToReportDescription(String defaultAddInputToReportDescription) {
        this.defaultAddInputToReportDescription = defaultAddInputToReportDescription;
    }

    /**
     * @param defaultLocaleDescription The default web service input description for the locale to use.
     */
    public void setDefaultLocaleDescription(String defaultLocaleDescription) {
        this.defaultLocaleDescription = defaultLocaleDescription;
    }

    /**
     * Initialise the configuration.
     */
    @Override
    @PostConstruct
    public void init() {
        super.init();
        setSupportsAdditionalInformationInReportItems(false);
        setSupportsTestDefinitionInReportItems(false);
        // Default labels.
        defaultLabels.put(ValidationConstants.INPUT_CONTENT, defaultContentToValidateDescription);
        defaultLabels.put(ValidationConstants.INPUT_EMBEDDING_METHOD, defaultEmbeddingMethodDescription);
        defaultLabels.put(ValidationConstants.INPUT_EXTERNAL_SCHEMAS, defaultExternalSchemasDescription);
        defaultLabels.put(ValidationConstants.INPUT_VALIDATION_TYPE, defaultValidationTypeDescription);
        defaultLabels.put(ValidationConstants.INPUT_LOCATION_AS_POINTER, defaultLocationAsPointerDescription);
        defaultLabels.put(ValidationConstants.INPUT_EXTERNAL_SCHEMA_COMBINATION_APPROACH, defaultExternalSchemaCombinationApproachDescription);
        defaultLabels.put(ValidationConstants.INPUT_ADD_INPUT_TO_REPORT, defaultAddInputToReportDescription);
        defaultLabels.put(ValidationConstants.INPUT_LOCALE, defaultLocaleDescription);
    }

}
