package eu.europa.ec.itb.json.web;

import com.gitb.tr.TAR;
import eu.europa.ec.itb.json.DomainConfig;
import eu.europa.ec.itb.json.validation.JSONValidator;
import eu.europa.ec.itb.validation.commons.LocalisationHelper;

/**
 * JSON validator specific subclass of the results DTO.
 */
public class UploadResult <T extends Translations> extends eu.europa.ec.itb.validation.commons.web.dto.UploadResult<T> {

    private Integer itemCount;

    /**
     * @return The parsed item count.
     */
    public Integer getItemCount() {
        return itemCount;
    }

    /**
     * @param itemCount The parsed item count.
     */
    public void setItemCount(Integer itemCount) {
        this.itemCount = itemCount;
    }

    /**
     * Populate all result properties that are common to all validator types.
     *
     * @param helper The localisation helper to use to lookup translations.
     * @param validationType The current validation type.
     * @param domainConfig The domain configuration.
     * @param reportId The report identifier.
     * @param fileName The fime name to display.
     * @param detailedReport The detailed TAR report.
     * @param aggregateReport The aggregated report.
     * @param translations The translations to use.
     */
    public void populateCommon(LocalisationHelper helper, String validationType, DomainConfig domainConfig, String reportId, String fileName, TAR detailedReport, TAR aggregateReport, T translations) {
        super.populateCommon(helper, validationType, domainConfig, reportId, fileName, detailedReport, aggregateReport, translations);
        if (domainConfig.isReportItemCount()) {
            detailedReport.getContext().getItem().stream()
                    .filter(item -> JSONValidator.ITEM_COUNT.equals(item.getName()))
                    .findAny()
                    .ifPresent(anyContent -> setItemCount(Integer.valueOf(anyContent.getValue())));
        }
    }
}
