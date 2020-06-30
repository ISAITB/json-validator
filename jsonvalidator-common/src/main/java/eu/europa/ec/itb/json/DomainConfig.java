package eu.europa.ec.itb.json;

import eu.europa.ec.itb.validation.commons.artifact.ValidationArtifactInfo;
import eu.europa.ec.itb.validation.commons.config.LabelConfig;
import eu.europa.ec.itb.validation.commons.config.WebDomainConfig;

public class DomainConfig extends WebDomainConfig<DomainConfig.Label> {

    @Override
    protected Label newLabelConfig() {
        return new Label();
    }

    public ValidationArtifactInfo getSchemaInfo(String validationType) {
        return getArtifactInfo().get(validationType).get();
    }

    public static class Label extends LabelConfig {
        private String externalSchemaLabel;
        private String externalSchemaPlaceholder;
        private String schemaCombinationLabel;
        private String schemaCombinationAnyOf;
        private String schemaCombinationAllOf;
        private String schemaCombinationOneOf;

        public String getSchemaCombinationLabel() {
            return schemaCombinationLabel;
        }

        public void setSchemaCombinationLabel(String schemaCombinationLabel) {
            this.schemaCombinationLabel = schemaCombinationLabel;
        }

        public String getSchemaCombinationAnyOf() {
            return schemaCombinationAnyOf;
        }

        public void setSchemaCombinationAnyOf(String schemaCombinationAnyOf) {
            this.schemaCombinationAnyOf = schemaCombinationAnyOf;
        }

        public String getSchemaCombinationAllOf() {
            return schemaCombinationAllOf;
        }

        public void setSchemaCombinationAllOf(String schemaCombinationAllOf) {
            this.schemaCombinationAllOf = schemaCombinationAllOf;
        }

        public String getSchemaCombinationOneOf() {
            return schemaCombinationOneOf;
        }

        public void setSchemaCombinationOneOf(String schemaCombinationOneOf) {
            this.schemaCombinationOneOf = schemaCombinationOneOf;
        }

        public String getExternalSchemaLabel() {
            return externalSchemaLabel;
        }

        public void setExternalSchemaLabel(String externalSchemaLabel) {
            this.externalSchemaLabel = externalSchemaLabel;
        }

        public String getExternalSchemaPlaceholder() {
            return externalSchemaPlaceholder;
        }

        public void setExternalSchemaPlaceholder(String externalSchemaPlaceholder) {
            this.externalSchemaPlaceholder = externalSchemaPlaceholder;
        }

    }

}
