package eu.europa.ec.itb.json;

import eu.europa.ec.itb.validation.commons.artifact.ExternalArtifactSupport;
import eu.europa.ec.itb.validation.commons.artifact.TypedValidationArtifactInfo;
import eu.europa.ec.itb.validation.commons.artifact.ValidationArtifactInfo;
import eu.europa.ec.itb.validation.commons.config.WebDomainConfig;

import java.util.HashSet;
import java.util.Set;

/**
 * The configuration for a specific validation domain.
 */
public class DomainConfig extends WebDomainConfig {

    private final Set<String> sharedSchemas = new HashSet<>();
    private boolean reportItemCount = false;

    /**
     * Whether the result report should list the number of parsed items in case an array was provided.
     *
     * @return The flag value.
     */
    public boolean isReportItemCount() {
        return reportItemCount;
    }

    /**
     * @param reportItemCount  Whether the result report should list the number of parsed items in case an array was provided.
     */
    public void setReportItemCount(boolean reportItemCount) {
        this.reportItemCount = reportItemCount;
    }

    /**
     * Get the configured values for shared schema references.
     *
     * @return The set of references.
     */
    public Set<String> getSharedSchemas() {
        return sharedSchemas;
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

}
