package eu.europa.ec.itb.json;

import eu.europa.ec.itb.json.validation.SchemaCombinationApproach;
import eu.europa.ec.itb.json.validation.ValidationConstants;
import org.apache.commons.configuration2.CompositeConfiguration;
import org.apache.commons.configuration2.FileBasedConfiguration;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FilenameFilter;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
public class DomainConfigCache {

    private static Logger logger = LoggerFactory.getLogger(DomainConfigCache.class);

    @Autowired
    private ApplicationConfig appConfig;
    private ConcurrentHashMap<String, DomainConfig> domainConfigs = new ConcurrentHashMap<>();
    private DomainConfig undefinedDomainConfig = new DomainConfig(false);

    private ExtensionFilter propertyFilter = new ExtensionFilter(".properties");

    @PostConstruct
    public void init() {
        getAllDomainConfigurations();
    }

    public DomainConfig[] getAllDomainConfigurations() {
        List<DomainConfig> configs = new ArrayList<>();
        for (String domain: appConfig.getDomain()) {
            DomainConfig domainConfig = getConfigForDomain(domain);
            if (domainConfig != null && domainConfig.isDefined()) {
                configs.add(domainConfig);
            }
        }
        return configs.toArray(new DomainConfig[0]);
    }

    public DomainConfig getConfigForDomainName(String domainName) {
        DomainConfig config = getConfigForDomain(appConfig.getDomainNameToDomainId().getOrDefault(domainName, ""));
        if (config == null) {
            logger.warn("Invalid domain name ["+domainName+"].");
        }
        return config;
    }

    public DomainConfig getConfigForDomain(String domain) {
        DomainConfig domainConfig = domainConfigs.get(domain);
        if (domainConfig == null) {
            String[] files = Paths.get(appConfig.getResourceRoot(), domain).toFile().list(propertyFilter);
            if (files == null || files.length == 0) {
                domainConfig = undefinedDomainConfig;
            } else {
                CompositeConfiguration config = new CompositeConfiguration();
                for (String file: files) {
                    Parameters params = new Parameters();
                    FileBasedConfigurationBuilder<FileBasedConfiguration> builder =
                            new FileBasedConfigurationBuilder<FileBasedConfiguration>(PropertiesConfiguration.class)
                                    .configure(params.properties().setFile(Paths.get(appConfig.getResourceRoot(), domain, file).toFile()));
                    try {
                        config.addConfiguration(builder.getConfiguration());
                    } catch (ConfigurationException e) {
                        throw new IllegalStateException("Unable to load property file ["+file+"]", e);
                    }
                }
                domainConfig = new DomainConfig();
                domainConfig.setDomain(domain);
                domainConfig.setDomainName(appConfig.getDomainIdToDomainName().get(domain));
                domainConfig.setType(Arrays.stream(StringUtils.split(config.getString("validator.type"), ',')).map(String::trim).collect(Collectors.toList()));
                domainConfig.setTypeLabel(parseMap("validator.typeLabel", config, domainConfig.getType()));
                domainConfig.setChannels(Arrays.stream(StringUtils.split(config.getString("validator.channels", ValidatorChannel.REST_API.getName()+","+ValidatorChannel.SOAP_API.getName()+","+ValidatorChannel.FORM.getName()), ',')).map(String::trim).map(ValidatorChannel::byName).collect(Collectors.toSet()));
                domainConfig.setSchemaFile(parseSchemaMap("validator.schemaFile", config, domainConfig.getType()));
                domainConfig.setExternalSchemas(parseBooleanMap("validator.externalSchemas", config, domainConfig.getType()));
                domainConfig.setExternalSchemaCombinationApproach(parseEnumMap("validator.externalSchemaCombinationApproach", SchemaCombinationApproach.class, SchemaCombinationApproach.allOf, config, domainConfig.getType()));
                domainConfig.setWebServiceId(config.getString("validator.webServiceId", "ValidatorService"));
                domainConfig.setWebServiceDescription(parseMap("validator.webServiceDescription", config, Arrays.asList(ValidationConstants.INPUT_CONTENT, ValidationConstants.INPUT_VALIDATION_TYPE, ValidationConstants.INPUT_EMBEDDING_METHOD, ValidationConstants.INPUT_EXTERNAL_SCHEMAS, ValidationConstants.INPUT_LOCATION_AS_POINTER)));
                domainConfig.setReportsOrdered(config.getBoolean("validator.reportsOrdered", false));
                domainConfigs.put(domain, domainConfig);
                logger.info("Loaded configuration for domain ["+domain+"]");
            }
        }
        return domainConfig;
    }

    private Map<String, DomainConfig.SchemaFileInfo> parseSchemaMap(String key, CompositeConfiguration config, List<String> types){
        Map<String, DomainConfig.SchemaFileInfo> map = new HashMap<>();
        for (String type: types) {
            DomainConfig.SchemaFileInfo schemaFileInfo = new DomainConfig.SchemaFileInfo();
            List<DomainConfig.RemoteInfo> remoteInfo = new ArrayList<>();
            Set<String> processedRemote = new HashSet<>();
            String internalSchemaFile = config.getString(key+"."+type, null);
            schemaFileInfo.setLocalFolder(internalSchemaFile);
            Iterator<String> it = config.getKeys("validator.schemaFile." + type + ".remote");
            while (it.hasNext()) {
                String remoteKeys = it.next();
                String remoteInt = remoteKeys.replaceAll("(validator.schemaFile." + type + ".remote.)([0-9]{1,})(.[a-zA-Z]*)", "$2");
                if (!processedRemote.contains(remoteInt)) {
                    DomainConfig.RemoteInfo ri = new DomainConfig.RemoteInfo();
                    ri.setUrl(config.getString("validator.schemaFile." + type + ".remote."+remoteInt+".url"));
                    remoteInfo.add(ri);
                    processedRemote.add(remoteInt);
                }
            }
            schemaFileInfo.setRemote(remoteInfo);
            // Schema combination approach.
            SchemaCombinationApproach combinationApproach = SchemaCombinationApproach.allOf;
            if (config.containsKey("validator.schemaFile." + type + ".combinationApproach")) {
                combinationApproach = SchemaCombinationApproach.valueOf(config.getString("validator.schemaFile." + type + ".combinationApproach"));
            }
            schemaFileInfo.setSchemaCombinationApproach(combinationApproach);
            map.put(type, schemaFileInfo);
        }
        return map;
    }

    private <T extends Enum<T>> Map<String, T> parseEnumMap(String key, Class<T> enumType, T defaultValue, CompositeConfiguration config, List<String> types) {
        Map<String, T> map = new HashMap<>();
        for (String type: types) {
            map.put(type, T.valueOf(enumType, config.getString(key+"."+type, defaultValue.name())));
        }
        return map;
    }

    private Map<String, Boolean> parseBooleanMap(String key, CompositeConfiguration config, List<String> types) {
        Map<String, Boolean> map = new HashMap<>();
        for (String type: types) {
            boolean value = false;

            try {
                value = config.getBoolean(key+"."+type);
            }catch(Exception e){
                value = false;
            }
            finally {
                map.put(type, value);
            }
        }
        return map;
    }

    private Map<String, String> parseMap(String key, CompositeConfiguration config, List<String> types) {
        Map<String, String> map = new HashMap<>();
        for (String type: types) {
            String defaultValue = appConfig.getDefaultLabels().get(type);
            String val = config.getString(key+"."+type, defaultValue);
            if (val != null) {
                map.put(type, val.trim());
            }
        }
        return map;
    }

    private class ExtensionFilter implements FilenameFilter {

        private String ext;

        public ExtensionFilter(String ext) {
            this.ext = ext;
        }

        public boolean accept(File dir, String name) {
            return (name.endsWith(ext));
        }
    }

}
