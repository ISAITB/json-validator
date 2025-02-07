package eu.europa.ec.itb.json;

import eu.europa.ec.itb.json.validation.YamlSupportEnum;
import eu.europa.ec.itb.validation.commons.ValidatorChannel;
import eu.europa.ec.itb.validation.commons.config.ParseUtils;
import eu.europa.ec.itb.validation.commons.config.WebDomainConfigCache;
import jakarta.annotation.PostConstruct;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;

import static eu.europa.ec.itb.validation.commons.config.ParseUtils.addMissingDefaultValues;

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
        return new ValidatorChannel[] {ValidatorChannel.FORM, ValidatorChannel.SOAP_API, ValidatorChannel.REST_API};
    }

    /**
     * Initialise the configuration.
     */
    @Override
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
        domainConfig.getSharedSchemas().addAll(Arrays.asList(StringUtils.split(StringUtils.defaultIfBlank(config.getString("validator.referencedSchemas"), ""), ',')));
        domainConfig.setReportItemCount(config.getBoolean("validator.reportContentArrayItemCount", false));
        var defaultYamlSupport = YamlSupportEnum.fromValue(config.getString("validator.yamlSupport", "none"));
        domainConfig.setYamlSupport(ParseUtils.parseEnumMap("validator.yamlSupport", defaultYamlSupport, config, domainConfig.getType(), YamlSupportEnum::fromValue), defaultYamlSupport);
        addMissingDefaultValues(domainConfig.getWebServiceDescription(), appConfig.getDefaultLabels());
    }

}
