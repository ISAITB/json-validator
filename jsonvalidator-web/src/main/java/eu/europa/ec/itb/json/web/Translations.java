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

package eu.europa.ec.itb.json.web;

import com.gitb.tr.TAR;
import eu.europa.ec.itb.validation.commons.LocalisationHelper;
import eu.europa.ec.itb.validation.commons.config.DomainConfig;

/**
 * JSON validator specific subclass of the web translations.
 */
public class Translations extends eu.europa.ec.itb.validation.commons.web.dto.Translations {

    private String resultItemCountLabel;

    /**
     * Constructor that set's all labels that are common for all validators.
     *
     * @param helper       The helper class to facilitate localisation.
     * @param report       The (detailed) TAR validation report.
     * @param domainConfig The relevant domain configuration.
     */
    public Translations(LocalisationHelper helper, TAR report, DomainConfig domainConfig) {
        super(helper, report, domainConfig);
        setResultItemCountLabel(helper.localise("validator.label.resultItemCountLabel"));
    }


    /**
     * @return The label value.
     */
    public String getResultItemCountLabel() {
        return resultItemCountLabel;
    }

    /**
     * @param resultItemCountLabel The label value to set.
     */
    public void setResultItemCountLabel(String resultItemCountLabel) {
        this.resultItemCountLabel = resultItemCountLabel;
    }
}
