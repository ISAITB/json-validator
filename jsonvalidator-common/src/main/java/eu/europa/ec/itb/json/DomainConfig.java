package eu.europa.ec.itb.json;

import eu.europa.ec.itb.json.validation.SchemaCombinationApproach;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class DomainConfig {

    public static final String externalFile_req     	= "required" ;
    public static final String externalFile_opt     	= "optional" ;
    public static final String externalFile_none     	= "none" ;

    private boolean isDefined;
    private String domain;
    private String domainName;
    private List<String> type;
    private Map<String, String> typeLabel;
    private Map<String, String> externalSchemas;
    private Map<String, SchemaCombinationApproach> externalSchemaCombinationApproach;
    private Set<ValidatorChannel> channels;
    private Map<String, SchemaFileInfo> schemaFile;
    private String webServiceId = "JSONValidationService";
    private Map<String, String> webServiceDescription;
    private String uploadTitle = "Validator";
    private String reportTitle = "Validation report";
    private String htmlBanner;
    private String htmlFooter;
    private boolean supportMinimalUserInterface;
    private boolean showAbout;

    public boolean isShowAbout() {
        return showAbout;
    }

    public void setShowAbout(boolean showAbout) {
        this.showAbout = showAbout;
    }

    public String getUploadTitle() {
        return uploadTitle;
    }

    public void setUploadTitle(String uploadTitle) {
        this.uploadTitle = uploadTitle;
    }

    public String getReportTitle() {
        return reportTitle;
    }

    public void setReportTitle(String reportTitle) {
        this.reportTitle = reportTitle;
    }

    public String getHtmlBanner() {
        return htmlBanner;
    }

    public void setHtmlBanner(String htmlBanner) {
        this.htmlBanner = htmlBanner;
    }

    public String getHtmlFooter() {
        return htmlFooter;
    }

    public void setHtmlFooter(String htmlFooter) {
        this.htmlFooter = htmlFooter;
    }

    public boolean isSupportMinimalUserInterface() {
        return supportMinimalUserInterface;
    }

    public void setSupportMinimalUserInterface(boolean supportMinimalUserInterface) {
        this.supportMinimalUserInterface = supportMinimalUserInterface;
    }

    private Label label = new Label();

    public DomainConfig() {
        this(true);
    }

    public DomainConfig(boolean isDefined) {
        this.isDefined = isDefined;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public boolean hasMultipleValidationTypes() {
        return type != null && type.size() > 1;
    }

    public List<String> getType() {
        return type;
    }

    public void setType(List<String> type) {
        this.type = type;
    }

    public Set<ValidatorChannel> getChannels() {
        return channels;
    }

    public void setChannels(Set<ValidatorChannel> channels) {
        this.channels = channels;
    }

    public boolean isDefined() {
        return isDefined;
    }

    public void setDefined(boolean defined) {
        isDefined = defined;
    }

    public String getDomainName() {
        return domainName;
    }

    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    public Map<String, String> getTypeLabel() {
        return typeLabel;
    }

    public void setTypeLabel(Map<String, String> typeLabel) {
        this.typeLabel = typeLabel;
    }

    public String getWebServiceId() {
        return webServiceId;
    }

    public void setWebServiceId(String webServiceId) {
        this.webServiceId = webServiceId;
    }

    public Map<String, String> getWebServiceDescription() {
        return webServiceDescription;
    }

    public void setWebServiceDescription(Map<String, String> webServiceDescription) {
        this.webServiceDescription = webServiceDescription;
    }

    public Map<String, SchemaFileInfo> getSchemaFile() {
        return schemaFile;
    }

    public void setSchemaFile(Map<String, SchemaFileInfo> schemaFile) {
        this.schemaFile = schemaFile;
    }

    public void setExternalSchemas(Map<String, String> externalSchemas) {
        this.externalSchemas = externalSchemas;
    }

    public Map<String, String> getExternalSchemas() {
        return this.externalSchemas;
    }

    public Label getLabel() {
        return label;
    }

    public void setLabel(Label label) {
        this.label = label;
    }

    public Map<String, SchemaCombinationApproach> getExternalSchemaCombinationApproach() {
        return externalSchemaCombinationApproach;
    }

    public void setExternalSchemaCombinationApproach(Map<String, SchemaCombinationApproach> externalSchemaCombinationApproach) {
        this.externalSchemaCombinationApproach = externalSchemaCombinationApproach;
    }

    public static class SchemaFileInfo {
        String localFolder;
        List<RemoteInfo> remote;
        SchemaCombinationApproach schemaCombinationApproach;
        public String getLocalFolder() {
            return localFolder;
        }
        public void setLocalFolder(String localFolder) {
            this.localFolder = localFolder;
        }
        public List<RemoteInfo> getRemote() {
            return remote;
        }
        public void setRemote(List<RemoteInfo> remote) {
            this.remote = remote;
        }
        public SchemaCombinationApproach getSchemaCombinationApproach() {
            return schemaCombinationApproach;
        }
        public void setSchemaCombinationApproach(SchemaCombinationApproach schemaCombinationApproach) {
            this.schemaCombinationApproach = schemaCombinationApproach;
        }
    }

    public static class RemoteInfo {
        String url;
        public String getUrl() {
            return url;
        }
        public void setUrl(String url) {
            this.url = url;
        }
    }

    public static class Label {

        private String resultSectionTitle;
        private String fileInputLabel;
        private String fileInputPlaceholder;
        private String typeLabel;
        private String uploadButton;
        private String resultSubSectionOverviewTitle;
        private String resultDateLabel;
        private String resultFileNameLabel;
        private String resultResultLabel;
        private String resultErrorsLabel;
        private String resultWarningsLabel;
        private String resultMessagesLabel;
        private String viewAnnotatedInputButton;
        private String downloadXMLReportButton;
        private String downloadPDFReportButton;
        private String resultSubSectionDetailsTitle;
        private String resultTestLabel;
        private String popupTitle;
        private String popupCloseButton;
        private String resultValidationTypeLabel;
        private String optionContentFile;
        private String optionContentURI;
        private String optionContentDirectInput;
        private String includeExternalArtefacts;
        private String externalArtefactsTooltip;
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

        public String getResultSectionTitle() {
            return resultSectionTitle;
        }

        public void setResultSectionTitle(String resultSectionTitle) {
            this.resultSectionTitle = resultSectionTitle;
        }

        public String getFileInputLabel() {
            return fileInputLabel;
        }

        public void setFileInputLabel(String fileInputLabel) {
            this.fileInputLabel = fileInputLabel;
        }

        public String getFileInputPlaceholder() {
            return fileInputPlaceholder;
        }

        public void setFileInputPlaceholder(String fileInputPlaceholder) {
            this.fileInputPlaceholder = fileInputPlaceholder;
        }

        public String getTypeLabel() {
            return typeLabel;
        }

        public void setTypeLabel(String typeLabel) {
            this.typeLabel = typeLabel;
        }

        public String getUploadButton() {
            return uploadButton;
        }

        public void setUploadButton(String uploadButton) {
            this.uploadButton = uploadButton;
        }

        public String getResultSubSectionOverviewTitle() {
            return resultSubSectionOverviewTitle;
        }

        public void setResultSubSectionOverviewTitle(String resultSubSectionOverviewTitle) {
            this.resultSubSectionOverviewTitle = resultSubSectionOverviewTitle;
        }

        public String getResultDateLabel() {
            return resultDateLabel;
        }

        public void setResultDateLabel(String resultDateLabel) {
            this.resultDateLabel = resultDateLabel;
        }

        public String getResultFileNameLabel() {
            return resultFileNameLabel;
        }

        public void setResultFileNameLabel(String resultFileNameLabel) {
            this.resultFileNameLabel = resultFileNameLabel;
        }

        public String getResultResultLabel() {
            return resultResultLabel;
        }

        public void setResultResultLabel(String resultResultLabel) {
            this.resultResultLabel = resultResultLabel;
        }

        public String getResultErrorsLabel() {
            return resultErrorsLabel;
        }

        public void setResultErrorsLabel(String resultErrorsLabel) {
            this.resultErrorsLabel = resultErrorsLabel;
        }

        public String getResultWarningsLabel() {
            return resultWarningsLabel;
        }

        public void setResultWarningsLabel(String resultWarningsLabel) {
            this.resultWarningsLabel = resultWarningsLabel;
        }

        public String getResultMessagesLabel() {
            return resultMessagesLabel;
        }

        public void setResultMessagesLabel(String resultMessagesLabel) {
            this.resultMessagesLabel = resultMessagesLabel;
        }

        public String getViewAnnotatedInputButton() {
            return viewAnnotatedInputButton;
        }

        public void setViewAnnotatedInputButton(String viewAnnotatedInputButton) {
            this.viewAnnotatedInputButton = viewAnnotatedInputButton;
        }

        public String getDownloadXMLReportButton() {
            return downloadXMLReportButton;
        }

        public void setDownloadXMLReportButton(String downloadXMLReportButton) {
            this.downloadXMLReportButton = downloadXMLReportButton;
        }

        public String getDownloadPDFReportButton() {
            return downloadPDFReportButton;
        }

        public void setDownloadPDFReportButton(String downloadPDFReportButton) {
            this.downloadPDFReportButton = downloadPDFReportButton;
        }

        public String getResultSubSectionDetailsTitle() {
            return resultSubSectionDetailsTitle;
        }

        public void setResultSubSectionDetailsTitle(String resultSubSectionDetailsTitle) {
            this.resultSubSectionDetailsTitle = resultSubSectionDetailsTitle;
        }

        public String getResultTestLabel() {
            return resultTestLabel;
        }

        public void setResultTestLabel(String resultTestLabel) {
            this.resultTestLabel = resultTestLabel;
        }

        public String getPopupTitle() {
            return popupTitle;
        }

        public void setPopupTitle(String popupTitle) {
            this.popupTitle = popupTitle;
        }

        public String getPopupCloseButton() {
            return popupCloseButton;
        }

        public void setPopupCloseButton(String popupCloseButton) {
            this.popupCloseButton = popupCloseButton;
        }

        public String getResultValidationTypeLabel() {
            return resultValidationTypeLabel;
        }

        public void setResultValidationTypeLabel(String resultValidationTypeLabel) {
            this.resultValidationTypeLabel = resultValidationTypeLabel;
        }

        public String getOptionContentFile() {
            return optionContentFile;
        }

        public void setOptionContentFile(String optionContentFile) {
            this.optionContentFile = optionContentFile;
        }

        public String getOptionContentURI() {
            return optionContentURI;
        }

        public void setOptionContentURI(String optionContentURI) {
            this.optionContentURI = optionContentURI;
        }

        public String getOptionContentDirectInput() {
            return optionContentDirectInput;
        }

        public void setOptionContentDirectInput(String optionContentDirectInput) {
            this.optionContentDirectInput = optionContentDirectInput;
        }

        public String getIncludeExternalArtefacts() {
            return includeExternalArtefacts;
        }

        public void setIncludeExternalArtefacts(String includeExternalArtefacts) {
            this.includeExternalArtefacts = includeExternalArtefacts;
        }

        public String getExternalArtefactsTooltip() {
            return externalArtefactsTooltip;
        }

        public void setExternalArtefactsTooltip(String externalArtefactsTooltip) {
            this.externalArtefactsTooltip = externalArtefactsTooltip;
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
