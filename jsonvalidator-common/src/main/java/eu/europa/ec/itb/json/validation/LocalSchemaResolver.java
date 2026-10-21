/*
 * Copyright (C) 2026 European Union
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
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.resource.InputStreamSource;
import com.networknt.schema.resource.ResourceLoader;
import com.networknt.schema.utils.AbsoluteIris;
import eu.europa.ec.itb.json.DomainConfig;
import eu.europa.ec.itb.validation.commons.FileInfo;
import eu.europa.ec.itb.validation.commons.ImportedFileAuthorizer;
import eu.europa.ec.itb.validation.commons.ImportedUriAuthorizer;
import eu.europa.ec.itb.validation.commons.RemoteRequestGuard;
import eu.europa.ec.itb.validation.commons.URLReader;
import org.apache.commons.lang3.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.nio.file.Files;

/**
 * Custom schema resolver that loads schemas defined as local references.
 */
public class LocalSchemaResolver implements ResourceLoader {

    private static final Logger LOG = LoggerFactory.getLogger(LocalSchemaResolver.class);

    private final DomainConfig domain;
    private final LocalSchemaCache localSchemaCache;
    private final FileInfo schemaInfo;
    private final ImportedUriAuthorizer importAuthorizer;
    private final ImportedFileAuthorizer importFileAuthorizer;
    private URLReader urlReader;

    /**
     * Constructor.
     *
     * @param domain The configuration domain.
     * @param localSchemaCache The cache of locally-defined schemas.
     * @param schemaInfo The schema information.
     * @param importAuthorizer The authorizer for imported schema URIs.
     * @param importFileAuthorizer The authorizer for imported schema files.
     */
    public LocalSchemaResolver(DomainConfig domain, LocalSchemaCache localSchemaCache, FileInfo schemaInfo, ImportedUriAuthorizer importAuthorizer, ImportedFileAuthorizer importFileAuthorizer) {
        this.domain = domain;
        this.localSchemaCache = localSchemaCache;
        this.schemaInfo = schemaInfo;
        this.importAuthorizer = importAuthorizer;
        this.importFileAuthorizer = importFileAuthorizer;
    }

    /**
     * Resolve a schema based on the provided ID.
     *
     * @param absoluteIri the identifier of the schema to return.
     * @return The resolved schema or null if none could be resolved from the configured shared schema references.
     */
    @Override
    public InputStreamSource getResource(AbsoluteIri absoluteIri) {
        String idToCheck = Strings.CS.appendIfMissing(absoluteIri.toString(), "#");
        var schema = localSchemaCache.getSchemaForId(domain, idToCheck);
        if (schema.isEmpty()) {
            LOG.debug("Schema with URI {} not found locally. Looking up remotely.", absoluteIri);
            URI uriToResolve = URI.create(AbsoluteIris.toUri(absoluteIri));
            /*
             * Every request (including redirects) is checked by the guard. Credentials configured for the schema are only
             * sent to the host the schema itself was retrieved from, and not to any other host it references.
             */
            var guard = RemoteRequestGuard.forArtefact(importAuthorizer, schemaInfo.getSource());
            guard.check(uriToResolve);
            return () -> getUrlReader().stream(uriToResolve, null, domain.getHttpVersion(), schemaInfo.getRequestDecorator(), guard).stream();
        } else {
            LOG.debug("Schema with URI {} found locally.", absoluteIri);
            if (importFileAuthorizer != null) importFileAuthorizer.isPathAllowed(schema.get());
            return () -> Files.newInputStream(schema.get());
        }
    }

    /**
     * Prevent schemas from referring to resources on the validator's classpath (via "classpath:" and "resource:" IRIs), which
     * the library attempts to load before any other loader.
     * <p>
     * This must be the last customisation of the loaders as the restriction is not carried over by further customisations.
     *
     * @param builder The schema registry builder to restrict.
     * @return The restricted builder.
     */
    static SchemaRegistry.Builder blockClasspathResources(SchemaRegistry.Builder builder) {
        return builder.schemaLoader(loaderBuilder -> loaderBuilder.block(iri -> {
            var iriAsString = iri.toString();
            return iriAsString.startsWith("classpath:") || iriAsString.startsWith("resource:");
        }));
    }

    /**
     * Get the URL reader to use.
     *
     * @return The reader.
     */
    private URLReader getUrlReader() {
        if (urlReader == null) {
            urlReader = new URLReader();
        }
        return urlReader;
    }

}