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
