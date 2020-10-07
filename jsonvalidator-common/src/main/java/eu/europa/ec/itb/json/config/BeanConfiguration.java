package eu.europa.ec.itb.json.config;

import eu.europa.ec.itb.validation.commons.config.DomainPluginConfigProvider;
import org.leadpony.justify.api.JsonValidationService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BeanConfiguration {

    @Bean
    public DomainPluginConfigProvider pluginConfigProvider() {
        return new DomainPluginConfigProvider();
    }

    @Bean
    public JsonValidationService jsonValidationService() {
        return JsonValidationService.newInstance();
    }

}
