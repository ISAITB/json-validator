package eu.europa.ec.itb.json;

import eu.europa.ec.itb.validation.commons.artifact.ExternalArtifactSupport;
import eu.europa.ec.itb.validation.commons.artifact.TypedValidationArtifactInfo;
import eu.europa.ec.itb.validation.commons.artifact.ValidationArtifactInfo;
import eu.europa.ec.itb.validation.commons.config.LabelConfig;
import eu.europa.ec.itb.validation.commons.config.WebDomainConfig;

/**
 * The configuration for a specific validation domain.
 */
public class DomainConfig extends WebDomainConfig<DomainConfig.Label> {

    /**
     * Create an empty label configuration object.
     *
     * @return The object.
     */
    @Override
    protected Label newLabelConfig() {
        return new Label();
    }

    /**
     * Get the JSON schema configuration for a given validation type.
     *
     * @param validationType The validation type.
     * @return The schema configuration.
     */
    public ValidationArtifactInfo getSchemaInfo(String validationType) {
        return getArtifactInfo().get(validationType).get();
    }

    /**
     * Check to see if there is a validation type that supports user-provided JSON schemas.
     *
     * @return True if user-provided schemas are allowed.
     */
    public boolean definesTypeWithExternalSchemas() {
        for (TypedValidationArtifactInfo info : getArtifactInfo().values()) {
            if (info.get().getExternalArtifactSupport() != ExternalArtifactSupport.NONE) {
                return true;
            }
        }
        return false;
    }

    /**
     * UI label configuration.
     */
    public static class Label extends LabelConfig {
        private String externalSchemaLabel;
        private String externalSchemaPlaceholder;
        private String schemaCombinationLabel;
        private String schemaCombinationAnyOf;
        private String schemaCombinationAllOf;
        private String schemaCombinationOneOf;

        /**
         * @return Label for the schema combination option.
         */
        public String getSchemaCombinationLabel() {
            return schemaCombinationLabel;
        }

        /**
         * @param schemaCombinationLabel Label for the schema combination option.
         */
        public void setSchemaCombinationLabel(String schemaCombinationLabel) {
            this.schemaCombinationLabel = schemaCombinationLabel;
        }

        /**
         * @return Label for the anyOf combination option.
         */
        public String getSchemaCombinationAnyOf() {
            return schemaCombinationAnyOf;
        }

        /**
         * @param schemaCombinationAnyOf Label for the anyOf combination option.
         */
        public void setSchemaCombinationAnyOf(String schemaCombinationAnyOf) {
            this.schemaCombinationAnyOf = schemaCombinationAnyOf;
        }

        /**
         * @return Label for the allOf combination option.
         */
        public String getSchemaCombinationAllOf() {
            return schemaCombinationAllOf;
        }

        /**
         * @param schemaCombinationAllOf Label for the allOf combination option.
         */
        public void setSchemaCombinationAllOf(String schemaCombinationAllOf) {
            this.schemaCombinationAllOf = schemaCombinationAllOf;
        }

        /**
         * @return Label for the oneOf combination option.
         */
        public String getSchemaCombinationOneOf() {
            return schemaCombinationOneOf;
        }

        /**
         * @param schemaCombinationOneOf Label for the oneOf combination option.
         */
        public void setSchemaCombinationOneOf(String schemaCombinationOneOf) {
            this.schemaCombinationOneOf = schemaCombinationOneOf;
        }

        /**
         * @return Label for user-provided schemas.
         */
        public String getExternalSchemaLabel() {
            return externalSchemaLabel;
        }

        /**
         * @param externalSchemaLabel Label for user-provided schemas.
         */
        public void setExternalSchemaLabel(String externalSchemaLabel) {
            this.externalSchemaLabel = externalSchemaLabel;
        }

        /**
         * @return The placeholder text for the user-provided schema input.
         */
        public String getExternalSchemaPlaceholder() {
            return externalSchemaPlaceholder;
        }

        /**
         * @param externalSchemaPlaceholder The placeholder text for the user-provided schema input.
         */
        public void setExternalSchemaPlaceholder(String externalSchemaPlaceholder) {
            this.externalSchemaPlaceholder = externalSchemaPlaceholder;
        }

    }

}
