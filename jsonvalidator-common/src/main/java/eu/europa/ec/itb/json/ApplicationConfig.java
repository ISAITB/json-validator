package eu.europa.ec.itb.json;

import eu.europa.ec.itb.json.validation.ValidationConstants;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.HashSet;
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
    private Map<String, String> branchErrorMessages;
    private Set<String> branchErrorMessageValues;

    /**
     * @return The branch error messages.
     */
    public Set<String> getBranchErrorMessageValues() {
        return branchErrorMessageValues;
    }

    /**
     * @return The branch error messages per validation type.
     */
    public Map<String, String> getBranchErrorMessages() {
        return branchErrorMessages;
    }

    /**
     * @param branchErrorMessages The branch error messages per validation type.
     */
    public void setBranchErrorMessages(Map<String, String> branchErrorMessages) {
        this.branchErrorMessages = branchErrorMessages;
    }

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
    @PostConstruct
    public void init() {
        super.init();
        // Default labels.
        defaultLabels.put(ValidationConstants.INPUT_CONTENT, defaultContentToValidateDescription);
        defaultLabels.put(ValidationConstants.INPUT_EMBEDDING_METHOD, defaultEmbeddingMethodDescription);
        defaultLabels.put(ValidationConstants.INPUT_EXTERNAL_SCHEMAS, defaultExternalSchemasDescription);
        defaultLabels.put(ValidationConstants.INPUT_VALIDATION_TYPE, defaultValidationTypeDescription);
        defaultLabels.put(ValidationConstants.INPUT_LOCATION_AS_POINTER, defaultLocationAsPointerDescription);
        defaultLabels.put(ValidationConstants.INPUT_EXTERNAL_SCHEMA_COMBINATION_APPROACH, defaultExternalSchemaCombinationApproachDescription);
        defaultLabels.put(ValidationConstants.INPUT_ADD_INPUT_TO_REPORT, defaultAddInputToReportDescription);
        defaultLabels.put(ValidationConstants.INPUT_LOCALE, defaultLocaleDescription);
        // Branch error messages.
        branchErrorMessageValues = new HashSet<>(getBranchErrorMessages().values());
    }

}
