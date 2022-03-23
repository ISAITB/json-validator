package eu.europa.ec.itb.json.config;

import eu.europa.ec.itb.json.DomainConfig;
import eu.europa.ec.itb.validation.commons.config.DomainPluginConfigProvider;
import org.leadpony.justify.api.JsonValidationService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configure common spring beans.
 */
@Configuration
public class BeanConfiguration {

    /**
     * Support the definition of plugins.
     *
     * @return The default plugin provider.
     */
    @Bean
    public DomainPluginConfigProvider<DomainConfig> pluginConfigProvider() {
        return new DomainPluginConfigProvider<>();
    }

    /**
     * Define the internal service used to validate JSON data.
     *
     * @return The validation service.
     */
    @Bean
    public JsonValidationService jsonValidationService() {
        return JsonValidationService.newInstance();
    }

}
