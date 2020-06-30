package eu.europa.ec.itb.json.gitb;

import eu.europa.ec.itb.json.DomainConfig;
import eu.europa.ec.itb.json.DomainConfigCache;
import eu.europa.ec.itb.validation.commons.ValidatorChannel;
import org.apache.cxf.Bus;
import org.apache.cxf.jaxws.EndpointImpl;
import org.apache.cxf.transport.servlet.CXFServlet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import javax.xml.namespace.QName;

/**
 * Configuration class responsible for creating the Spring beans required by the service.
 */
@Configuration
public class ValidationServiceConfig {

    private static final String CXF_ROOT = "soap";

    @Autowired
    private Bus cxfBus = null;
    
    @Autowired
    private ApplicationContext applicationContext = null;

    @Autowired
    private DomainConfigCache domainConfigCache = null;

    @Bean
    public ServletRegistrationBean servletRegistrationBean(ApplicationContext context) {
        ServletRegistrationBean<CXFServlet> srb = new ServletRegistrationBean<>(new CXFServlet(), "/"+ CXF_ROOT +"/*");
        srb.addInitParameter("hide-service-list-page", "true");
        return srb;
    }
    
    @PostConstruct
    public void publishValidationServices() {
    	for (DomainConfig domainConfig: domainConfigCache.getAllDomainConfigurations()) {
            if (domainConfig.getChannels().contains(ValidatorChannel.SOAP_API)) {
                EndpointImpl endpoint = new EndpointImpl(cxfBus, applicationContext.getBean(ValidationServiceImpl.class, domainConfig));
                endpoint.setEndpointName(new QName("http://www.gitb.com/vs/v1/", "ValidationServicePort"));
                endpoint.setServiceName(new QName("http://www.gitb.com/vs/v1/", "ValidationService"));
                endpoint.publish("/"+domainConfig.getDomainName()+"/validation");
            }
    	}
    }

}
