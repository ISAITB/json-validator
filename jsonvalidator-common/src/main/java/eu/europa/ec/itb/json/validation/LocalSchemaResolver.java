package eu.europa.ec.itb.json.validation;

import com.networknt.schema.uri.URIFetcher;
import com.networknt.schema.uri.URLFetcher;
import eu.europa.ec.itb.json.DomainConfig;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;

/**
 * Custom schema resolver that loads schemas defined as local references.
 */
public class LocalSchemaResolver implements URIFetcher {

    private static final Logger LOG = LoggerFactory.getLogger(LocalSchemaResolver.class);

    private final URLFetcher urlFetcher = new URLFetcher();
    private final DomainConfig domain;
    private final LocalSchemaCache localSchemaCache;

    /**
     * Constructor.
     *
     * @param domain The configuration domain.
     * @param localSchemaCache The cache of locally-defined schemas.
     */
    public LocalSchemaResolver(DomainConfig domain, LocalSchemaCache localSchemaCache) {
        this.domain = domain;
        this.localSchemaCache = localSchemaCache;
    }

    /**
     * Resolve a schema based on the provided ID.
     *
     * @param id the identifier of the schema to return.
     * @return The resolved schema or null if none could be resolved from the configured shared schema references.
     */
    @Override
    public InputStream fetch(URI id) throws IOException {
        String idToCheck = StringUtils.appendIfMissing(id.toString(), "#");
        var schema = localSchemaCache.getSchemaForId(domain, idToCheck);
        if (schema.isEmpty()) {
            LOG.debug("Schema with URI {} not found locally. Looking up remotely.", id);
            return urlFetcher.fetch(id);
        } else {
            LOG.debug("Schema with URI {} found locally.", id);
            return Files.newInputStream(schema.get());
        }
    }

}