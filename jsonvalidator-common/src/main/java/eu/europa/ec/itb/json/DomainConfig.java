package eu.europa.ec.itb.json;

import eu.europa.ec.itb.json.validation.SchemaCombinationApproach;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class DomainConfig {

    private boolean isDefined;
    private String domain;
    private String domainName;
    private List<String> type;
    private Map<String, String> typeLabel;
    private Map<String, Boolean> externalSchemas;
    private Map<String, SchemaCombinationApproach> externalSchemaCombinationApproach;
    private Set<ValidatorChannel> channels;
    private Map<String, SchemaFileInfo> schemaFile;
    private String webServiceId = "JSONValidationService";
    private Map<String, String> webServiceDescription;
    private boolean reportsOrdered;

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

    public boolean isReportsOrdered() {
        return reportsOrdered;
    }

    public void setReportsOrdered(boolean reportsOrdered) {
        this.reportsOrdered = reportsOrdered;
    }

    public Map<String, SchemaFileInfo> getSchemaFile() {
        return schemaFile;
    }

    public void setSchemaFile(Map<String, SchemaFileInfo> schemaFile) {
        this.schemaFile = schemaFile;
    }

    public void setExternalSchemas(Map<String, Boolean> externalSchemas) {
        this.externalSchemas = externalSchemas;
    }

    public Map<String, Boolean> getExternalSchemas() {
        return this.externalSchemas;
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

}
