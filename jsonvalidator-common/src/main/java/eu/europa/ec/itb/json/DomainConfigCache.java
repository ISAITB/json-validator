package eu.europa.ec.itb.json;

import eu.europa.ec.itb.validation.commons.ValidatorChannel;
import eu.europa.ec.itb.validation.commons.config.WebDomainConfigCache;
import org.apache.commons.configuration2.Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class DomainConfigCache extends WebDomainConfigCache<DomainConfig> {

    @Autowired
    private ApplicationConfig appConfig = null;

    @Override
    protected DomainConfig newDomainConfig() {
        return new DomainConfig();
    }

    @Override
    protected ValidatorChannel[] getSupportedChannels() {
        return new ValidatorChannel[] {ValidatorChannel.FORM, ValidatorChannel.SOAP_API};
    }

    @PostConstruct
    public void init() {
        super.init();
    }

    @Override
    protected void addDomainConfiguration(DomainConfig domainConfig, Configuration config) {
        super.addDomainConfiguration(domainConfig, config);
        addValidationArtifactInfo("validator.schemaFile", "validator.externalSchemas", "validator.externalSchemaCombinationApproach", domainConfig, config);
        // Labels.
        domainConfig.getLabel().setPopupTitle(config.getString("validator.label.popupTitle", "JSON content"));
        domainConfig.getLabel().setExternalSchemaLabel(config.getString("validator.label.externalSchemaLabel", "JSON Schema"));
        domainConfig.getLabel().setExternalSchemaPlaceholder(config.getString("validator.label.externalSchemaPlaceholder", "Select file..."));
        domainConfig.getLabel().setSchemaCombinationLabel(config.getString("validator.label.schemaCombinationLabel", "Validation approach"));
        domainConfig.getLabel().setSchemaCombinationAllOf(config.getString("validator.label.schemaCombinationAllOf", "Content must validate against all schemas"));
        domainConfig.getLabel().setSchemaCombinationAnyOf(config.getString("validator.label.schemaCombinationAnyOf", "Content must validate against any schema"));
        domainConfig.getLabel().setSchemaCombinationOneOf(config.getString("validator.label.schemaCombinationOneOf", "Content must validate against exactly one schema"));
        addMissingDefaultValues(domainConfig.getWebServiceDescription(), appConfig.getDefaultLabels());
    }

}
