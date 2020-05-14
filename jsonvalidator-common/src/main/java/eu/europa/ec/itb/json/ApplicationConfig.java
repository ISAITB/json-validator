package eu.europa.ec.itb.json;

import eu.europa.ec.itb.json.validation.ValidationConstants;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FileFilter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by simatosc on 12/05/2016.
 */
@Component
@ConfigurationProperties("validator")
public class ApplicationConfig {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationConfig.class);

    @Autowired
    private Environment env;

    private String resourceRoot;
    private String tmpFolder;
    private Set<String> acceptedSchemaExtensions;
    private Set<String> domain;
    private Map<String, String> domainIdToDomainName = new HashMap<>();
    private Map<String, String> domainNameToDomainId = new HashMap<>();
    private Map<String, String> defaultLabels = new HashMap<>();

    private String defaultContentToValidateDescription;
    private String defaultEmbeddingMethodDescription;
    private String defaultExternalSchemasDescription;
    private String defaultExternalSchemaCombinationApproachDescription;
    private String defaultValidationTypeDescription;
    private String defaultLocationAsPointerDescription;

    public String getTmpFolder() {
        return tmpFolder;
    }

    public void setTmpFolder(String tmpFolder) {
        this.tmpFolder = tmpFolder;
    }

    public Set<String> getAcceptedSchemaExtensions() {
        return acceptedSchemaExtensions;
    }

    public void setAcceptedSchemaExtensions(Set<String> acceptedSchemaExtensions) {
        this.acceptedSchemaExtensions = acceptedSchemaExtensions;
    }

    public String getResourceRoot() {
        return resourceRoot;
    }

    public void setResourceRoot(String resourceRoot) {
        this.resourceRoot = resourceRoot;
    }
    
    public Set<String> getDomain() {
        return domain;
    }

    public void setDomain(Set<String> domain) {
        this.domain = domain;
    }

    public Map<String, String> getDomainIdToDomainName() {
        return domainIdToDomainName;
    }

    public Map<String, String> getDomainNameToDomainId() {
        return domainNameToDomainId;
    }

    public Map<String, String> getDefaultLabels() {
        return defaultLabels;
    }

    public String getDefaultContentToValidateDescription() {
        return defaultContentToValidateDescription;
    }

    public void setDefaultContentToValidateDescription(String defaultContentToValidateDescription) {
        this.defaultContentToValidateDescription = defaultContentToValidateDescription;
    }

    public String getDefaultEmbeddingMethodDescription() {
        return defaultEmbeddingMethodDescription;
    }

    public void setDefaultEmbeddingMethodDescription(String defaultEmbeddingMethodDescription) {
        this.defaultEmbeddingMethodDescription = defaultEmbeddingMethodDescription;
    }

    public String getDefaultValidationTypeDescription() {
        return defaultValidationTypeDescription;
    }

    public void setDefaultValidationTypeDescription(String defaultValidationTypeDescription) {
        this.defaultValidationTypeDescription = defaultValidationTypeDescription;
    }

    public String getDefaultExternalSchemasDescription() {
        return defaultExternalSchemasDescription;
    }

    public void setDefaultExternalSchemasDescription(String defaultExternalSchemasDescription) {
        this.defaultExternalSchemasDescription = defaultExternalSchemasDescription;
    }

    public String getDefaultLocationAsPointerDescription() {
        return defaultLocationAsPointerDescription;
    }

    public void setDefaultLocationAsPointerDescription(String defaultLocationAsPointerDescription) {
        this.defaultLocationAsPointerDescription = defaultLocationAsPointerDescription;
    }

    public String getDefaultExternalSchemaCombinationApproachDescription() {
        return defaultExternalSchemaCombinationApproachDescription;
    }

    public void setDefaultExternalSchemaCombinationApproachDescription(String defaultExternalSchemaCombinationApproachDescription) {
        this.defaultExternalSchemaCombinationApproachDescription = defaultExternalSchemaCombinationApproachDescription;
    }

    @PostConstruct
    public void init() {
        if (resourceRoot != null && Files.isDirectory(Paths.get(resourceRoot))) {
            // Setup domain.
            if (domain == null || domain.isEmpty()) {
                File[] directories = new File(resourceRoot).listFiles(new FileFilter() {
                    @Override
                    public boolean accept(File file) {
                        return file.isDirectory();
                    }
                });
                if (directories == null || directories.length == 0) {
                    throw new IllegalStateException("The resource root directory ["+resourceRoot+"] is empty");
                }
                domain = Arrays.stream(directories).map(File::getName).collect(Collectors.toSet());
            }
        } else {
            throw new IllegalStateException("Invalid resourceRoot configured ["+resourceRoot+"]. Ensure you specify the validator.resourceRoot property correctly.");
        }
        logger.info("Loaded validation domains: "+domain);
        // Load domain names.
        StringBuilder logMsg = new StringBuilder();
        for (String domainFolder: domain) {
            String domainName = StringUtils.defaultIfBlank(env.getProperty("validator.domainName."+domainFolder), domainFolder);
            this.domainIdToDomainName.put(domainFolder, domainName);
            this.domainNameToDomainId.put(domainName, domainFolder);
            logMsg.append('[').append(domainFolder).append("]=[").append(domainName).append("]");
        }
        logger.info("Loaded validation domain names: " + logMsg.toString());
        // Default labels.
        defaultLabels.put(ValidationConstants.INPUT_CONTENT, defaultContentToValidateDescription);
        defaultLabels.put(ValidationConstants.INPUT_EMBEDDING_METHOD, defaultEmbeddingMethodDescription);
        defaultLabels.put(ValidationConstants.INPUT_EXTERNAL_SCHEMAS, defaultExternalSchemasDescription);
        defaultLabels.put(ValidationConstants.INPUT_VALIDATION_TYPE, defaultValidationTypeDescription);
        defaultLabels.put(ValidationConstants.INPUT_LOCATION_AS_POINTER, defaultLocationAsPointerDescription);
        defaultLabels.put(ValidationConstants.INPUT_EXTERNAL_SCHEMA_COMBINATION_APPROACH, defaultExternalSchemaCombinationApproachDescription);
    }

}
