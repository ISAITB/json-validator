/*
 * Copyright (C) 2026 European Union
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

import eu.europa.ec.itb.json.validation.YamlSupportEnum;
import eu.europa.ec.itb.validation.commons.artifact.ExternalArtifactSupport;
import eu.europa.ec.itb.validation.commons.artifact.TypedValidationArtifactInfo;
import eu.europa.ec.itb.validation.commons.artifact.ValidationArtifactInfo;
import eu.europa.ec.itb.validation.commons.config.WebDomainConfig;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * The configuration for a specific validation domain.
 */
public class DomainConfig extends WebDomainConfig {

    private final Set<String> sharedSchemas = new HashSet<>();
    private boolean reportItemCount = false;
    private Map<String, YamlSupportEnum> yamlSupport = new HashMap<>();
    private YamlSupportEnum defaultYamlSupport = YamlSupportEnum.NONE;

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
     * @param yamlSupport Set the map of validation types to YAML support.
     */
    public void setYamlSupport(Map<String, YamlSupportEnum> yamlSupport, YamlSupportEnum defaultSupport) {
        if (yamlSupport != null) {
            this.yamlSupport = yamlSupport;
        }
        if (defaultSupport != null) {
            this.defaultYamlSupport = defaultSupport;
        }
    }

    /**
     * Check whether the provided (full) validation type support's YAML.
     *
     * @param validationType The full validation to check its YAML support.
     * @return The mapping of full validation type to their level of YAML support (NONE being the default).
     */
    public YamlSupportEnum getYamlSupportForType(String validationType) {
        return yamlSupport.getOrDefault(validationType, defaultYamlSupport);
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
