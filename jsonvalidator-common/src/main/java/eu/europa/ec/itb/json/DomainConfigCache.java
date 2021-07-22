package eu.europa.ec.itb.json;

import eu.europa.ec.itb.validation.commons.ValidatorChannel;
import eu.europa.ec.itb.validation.commons.config.WebDomainConfigCache;
import org.apache.commons.configuration2.Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * Component to load, record and share the domain configurations.
 */
@Component
public class DomainConfigCache extends WebDomainConfigCache<DomainConfig> {

    @Autowired
    private ApplicationConfig appConfig = null;

    /**
     * Create a new and empty domain configuration object.
     *
     * @return The object.
     */
    @Override
    protected DomainConfig newDomainConfig() {
        return new DomainConfig();
    }

    /**
     * @see eu.europa.ec.itb.validation.commons.config.DomainConfigCache#getSupportedChannels()
     *
     * @return Form and SOAP API.
     */
    @Override
    protected ValidatorChannel[] getSupportedChannels() {
        return new ValidatorChannel[] {ValidatorChannel.FORM, ValidatorChannel.SOAP_API};
    }

    /**
     * Initialise the configuration.
     */
    @PostConstruct
    public void init() {
        super.init();
    }

    /**
     * Extend the domain configuration loading with JSON-specific information.
     *
     * @param domainConfig The domain configuration to enrich.
     * @param config The configuration properties to consider.
     */
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
