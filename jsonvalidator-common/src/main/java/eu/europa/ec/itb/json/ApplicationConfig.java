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
 * Created by simatosc on 12/05/2016.
 */
@Component
@ConfigurationProperties("validator")
public class ApplicationConfig extends eu.europa.ec.itb.validation.commons.config.ApplicationConfig {

    private Set<String> acceptedSchemaExtensions;
    private Map<String, String> defaultLabels = new HashMap<>();
    private Set<String> acceptedMimeTypes;

    private String defaultContentToValidateDescription;
    private String defaultEmbeddingMethodDescription;
    private String defaultExternalSchemasDescription;
    private String defaultExternalSchemaCombinationApproachDescription;
    private String defaultValidationTypeDescription;
    private String defaultLocationAsPointerDescription;
    private Map<String, String> branchErrorMessages;
    private Set<String> branchErrorMessageValues;

    public Set<String> getBranchErrorMessageValues() {
        return branchErrorMessageValues;
    }

    public Map<String, String> getBranchErrorMessages() {
        return branchErrorMessages;
    }

    public void setBranchErrorMessages(Map<String, String> branchErrorMessages) {
        this.branchErrorMessages = branchErrorMessages;
    }

    public Set<String> getAcceptedMimeTypes() {
        return acceptedMimeTypes;
    }

    public void setAcceptedMimeTypes(Set<String> acceptedMimeTypes) {
        this.acceptedMimeTypes = acceptedMimeTypes;
    }

    public Set<String> getAcceptedSchemaExtensions() {
        return acceptedSchemaExtensions;
    }

    public void setAcceptedSchemaExtensions(Set<String> acceptedSchemaExtensions) {
        this.acceptedSchemaExtensions = acceptedSchemaExtensions;
    }

    public Map<String, String> getDefaultLabels() {
        return defaultLabels;
    }

    public String getDefaultContentToValidateDescription() {
        return defaultContentToValidateDescription;
    }

    public void setDefaultContentToValidateDescription(String defaultContentToValidateDescription) {
        this.defaultContentToValidateDescription = defaultContentToValidateDescription;
    }

    public String getDefaultEmbeddingMethodDescription() {
        return defaultEmbeddingMethodDescription;
    }

    public void setDefaultEmbeddingMethodDescription(String defaultEmbeddingMethodDescription) {
        this.defaultEmbeddingMethodDescription = defaultEmbeddingMethodDescription;
    }

    public String getDefaultValidationTypeDescription() {
        return defaultValidationTypeDescription;
    }

    public void setDefaultValidationTypeDescription(String defaultValidationTypeDescription) {
        this.defaultValidationTypeDescription = defaultValidationTypeDescription;
    }

    public String getDefaultExternalSchemasDescription() {
        return defaultExternalSchemasDescription;
    }

    public void setDefaultExternalSchemasDescription(String defaultExternalSchemasDescription) {
        this.defaultExternalSchemasDescription = defaultExternalSchemasDescription;
    }

    public String getDefaultLocationAsPointerDescription() {
        return defaultLocationAsPointerDescription;
    }

    public void setDefaultLocationAsPointerDescription(String defaultLocationAsPointerDescription) {
        this.defaultLocationAsPointerDescription = defaultLocationAsPointerDescription;
    }

    public String getDefaultExternalSchemaCombinationApproachDescription() {
        return defaultExternalSchemaCombinationApproachDescription;
    }

    public void setDefaultExternalSchemaCombinationApproachDescription(String defaultExternalSchemaCombinationApproachDescription) {
        this.defaultExternalSchemaCombinationApproachDescription = defaultExternalSchemaCombinationApproachDescription;
    }

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
        // Branch error messages.
        branchErrorMessageValues = new HashSet<>(getBranchErrorMessages().values());
    }

}
