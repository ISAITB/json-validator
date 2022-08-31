package eu.europa.ec.itb.json.validation;

import com.google.gson.JsonParser;
import eu.europa.ec.itb.json.ApplicationConfig;
import eu.europa.ec.itb.json.DomainConfig;
import eu.europa.ec.itb.json.DomainConfigCache;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.leadpony.justify.api.JsonSchema;
import org.leadpony.justify.api.JsonSchemaReader;
import org.leadpony.justify.api.JsonSchemaResolver;
import org.leadpony.justify.api.JsonValidationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Custom schema resolver that loads schemas defined as local references.
 */
@Component
public class LocalSchemaResolver implements JsonSchemaResolver {

    public static ThreadLocal<SchemaResolutionState> RESOLUTION_STATE = new ThreadLocal<>();
    private static final Logger LOG = LoggerFactory.getLogger(LocalSchemaResolver.class);

    @Autowired
    private JsonValidationService jsonValidationService;
    @Autowired
    private DomainConfigCache domainConfigCache;
    @Autowired
    private ApplicationConfig appConfig;

    private final Map<String, Path> idsToPaths = new ConcurrentHashMap<>();
    private final Map<String, Optional<JsonSchema>> schemaCache = new ConcurrentHashMap<>();

    /**
     * Read the schema IDs (and their respective paths) that are defined as shared schema references.
     */
    @PostConstruct
    public void initialise() {
        domainConfigCache.getAllDomainConfigurations().forEach(domainConfig -> domainConfig.getSharedSchemas().forEach(sharedSchema -> {
            try {
                // Read all the defined schemas, mapped by their IDs.
                readSchemaIdsAndPaths(domainConfig);
            } catch (Exception e) {
                LOG.error("Error raised while processing shared schemas", e);
            }
        }));
        LOG.info("Preloaded {} shared schema(s)", idsToPaths.size());
    }

    /**
     * Read the ID of a schema defined at the provided path.
     *
     * @param schemaPath The path to read from.
     * @return The schema's ID.
     */
    private String readSchemaId(Path schemaPath) {
        try (var fileReader = new FileReader(schemaPath.toFile())) {
            var json = JsonParser.parseReader(fileReader).getAsJsonObject();
            if (json.has("$id")) {
                return json.get("$id").getAsString();
            }
        } catch (Exception e) {
            throw new IllegalStateException("Error raised while processing shared schema ["+schemaPath+"]", e);
        }
        return null;
    }

    /**
     * Load all shared schema references, recording their IDs and Paths to be used when requested to resolve schemas.
     *
     * @param domainConfig The domain configuration.
     * @throws IOException In case an IO error occurs.
     */
    private void readSchemaIdsAndPaths(DomainConfig domainConfig) throws IOException {
        for (var sharedSchema: domainConfig.getSharedSchemas()) {
            Path pathToProcess;
            if (appConfig.isRestrictResourcesToDomain()) {
                if ((new File(sharedSchema.trim())).isAbsolute() || !domainConfigCache.isInDomainFolder(domainConfig.getDomain(), sharedSchema)) {
                    throw new IllegalStateException("Shared schema definition ["+sharedSchema+"] is not under the domain root");
                } else {
                    pathToProcess = Paths.get(appConfig.getResourceRoot(), domainConfig.getDomain(), sharedSchema.trim()).toFile().getCanonicalFile().toPath();
                }
            } else {
                if (new File(sharedSchema.trim()).isAbsolute()) {
                    pathToProcess = Paths.get(sharedSchema.trim());
                } else {
                    pathToProcess = Paths.get(appConfig.getResourceRoot(), domainConfig.getDomain(), sharedSchema.trim()).toFile().getCanonicalFile().toPath();
                }
            }
            List<Path> pathsToMap = new ArrayList<>();
            if (Files.isRegularFile(pathToProcess)) {
                pathsToMap.add(pathToProcess);
            } else if (Files.isDirectory(pathToProcess)) {
                try (var fileStream = Files.find(pathToProcess, 10, (path, attrs) -> Files.isRegularFile(path) && "json".equalsIgnoreCase(FilenameUtils.getExtension(path.getFileName().toString())))) {
                    fileStream.forEach(pathsToMap::add);
                }
            }
            for (var pathToMap: pathsToMap) {
                var parsedId = readSchemaId(pathToMap);
                if (parsedId == null) {
                    LOG.warn("Unable to read schema ID from file configured as shared schema [{}]", pathToMap);
                } else {
                    idsToPaths.put(schemaKey(domainConfig, parsedId), pathToMap);
                }
            }
        }
    }

    /**
     * Generate the cache key for the provided schema ID.
     *
     * @param domainConfig The domain in question.
     * @param id The schema ID.
     * @return The key to use.
     */
    private String schemaKey(DomainConfig domainConfig, String id) {
        return StringUtils.appendIfMissing(String.format("com.gitb.domain.%s|%s", domainConfig.getDomain(), id), "#");
    }

    /**
     * Read the provided path as a schema using the current instance as an additional schema resolver.
     *
     * @param path The path to read.
     * @return The schema.
     */
    private JsonSchema readSchema(Path path) {
        var schemaReaderFactory = jsonValidationService
                .createSchemaReaderFactoryBuilder()
                .withSchemaResolver(this)
                .build();
        try (JsonSchemaReader schemaReader = schemaReaderFactory.createSchemaReader(path)) {
            return schemaReader.read();
        }
    }

    /**
     * Resolve a schema based on the provided ID.
     *
     * @param id the identifier of the schema to return.
     * @return The resolved schema or null if none could be resolved from the configured shared schema references.
     */
    @Override
    public JsonSchema resolveSchema(URI id) {
        var resolutionState = RESOLUTION_STATE.get();
        Objects.requireNonNull(resolutionState);
        Objects.requireNonNull(resolutionState.getDomain());
        String idToCheck = StringUtils.appendIfMissing(id.toString(), "#");
        if (resolutionState.getPendingResolution().contains(idToCheck)) {
            // We have a cyclic reference.
            throw new IllegalStateException(String.format("References to schema URI [%s] resulted in a cyclic dependency", id));
        }
        var key = schemaKey(resolutionState.getDomain(), idToCheck);
        JsonSchema schema = null;
        if (schemaCache.containsKey(key)) {
            schema = schemaCache.get(key).orElse(null);
        } else {
            if (idsToPaths.containsKey(key)) {
                try {
                    // Record the schema ID to ensure we don't end up with cyclic references.
                    resolutionState.getPendingResolution().add(idToCheck);
                    schema = readSchema(idsToPaths.get(key));
                } finally {
                    // Remove the schema ID.
                    resolutionState.getPendingResolution().remove(idToCheck);
                }
            }
            schemaCache.put(key, Optional.ofNullable(schema));
        }
        return schema;
    }

}