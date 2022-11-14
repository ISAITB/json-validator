package eu.europa.ec.itb.json;

import eu.europa.ec.itb.json.validation.FileManager;
import eu.europa.ec.itb.validation.commons.BaseInputHelper;
import eu.europa.ec.itb.validation.commons.artifact.ValidationArtifactCombinationApproach;
import eu.europa.ec.itb.validation.commons.error.ValidatorException;
import org.apache.tika.utils.StringUtils;
import org.springframework.stereotype.Component;

/**
 * Component to facilitate the validation and preparation of inputs.
 *
 * This class serves simply as a marked to include the parent component as-is.
 */
@Component
public class InputHelper extends BaseInputHelper<ApplicationConfig, FileManager, DomainConfig> {

    /**
     * Validate and return the combination approach when multiple user-provided schemas are present.
     *
     * @param domainConfig The domain configuration.
     * @param validationType The validation type.
     * @param value The value of the combination approach setting.
     * @return The combination approach to use.
     */
    public ValidationArtifactCombinationApproach getValidationArtifactCombinationApproach(DomainConfig domainConfig, String validationType, String value) {
        ValidationArtifactCombinationApproach approach;
        if (!StringUtils.isEmpty(value)) {
            try {
                approach = ValidationArtifactCombinationApproach.byName(value);
            } catch (IllegalArgumentException e) {
                throw new ValidatorException("validator.label.exception.invalidSchemaCombinationApproach", e, value);
            }
        } else {
            approach = domainConfig.getSchemaInfo(validationType).getExternalArtifactCombinationApproach();
        }
        return approach;
    }

}
