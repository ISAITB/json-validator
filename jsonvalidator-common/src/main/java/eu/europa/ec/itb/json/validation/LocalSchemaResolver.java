package eu.europa.ec.itb.json.validation;

import com.networknt.schema.AbsoluteIri;
import com.networknt.schema.resource.InputStreamSource;
import com.networknt.schema.resource.SchemaLoader;
import com.networknt.schema.resource.UriSchemaLoader;
import eu.europa.ec.itb.json.DomainConfig;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;

/**
 * Custom schema resolver that loads schemas defined as local references.
 */
public class LocalSchemaResolver implements SchemaLoader {

    private static final Logger LOG = LoggerFactory.getLogger(LocalSchemaResolver.class);

    private final UriSchemaLoader uriSchemaLoader = new UriSchemaLoader();
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
     * @param absoluteIri the identifier of the schema to return.
     * @return The resolved schema or null if none could be resolved from the configured shared schema references.
     */
    @Override
    public InputStreamSource getSchema(AbsoluteIri absoluteIri) {
        String idToCheck = StringUtils.appendIfMissing(absoluteIri.toString(), "#");
        var schema = localSchemaCache.getSchemaForId(domain, idToCheck);
        if (schema.isEmpty()) {
            LOG.debug("Schema with URI {} not found locally. Looking up remotely.", absoluteIri);
            return uriSchemaLoader.getSchema(absoluteIri);
        } else {
            LOG.debug("Schema with URI {} found locally.", absoluteIri);
            return () -> Files.newInputStream(schema.get());
        }
    }
}