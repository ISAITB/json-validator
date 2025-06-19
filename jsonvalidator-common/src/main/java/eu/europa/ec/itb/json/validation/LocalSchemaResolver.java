/*
 * Copyright (C) 2025 European Union
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the European Commission - subsequent
 * versions of the EUPL (the "Licence"); You may not use this work except in compliance with the Licence.
 *
 * You may obtain a copy of the Licence at:
 *
 * https://interoperable-europe.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an
 * "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the Licence for
 * the specific language governing permissions and limitations under the Licence.
 */

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