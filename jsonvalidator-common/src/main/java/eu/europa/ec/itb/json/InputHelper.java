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
