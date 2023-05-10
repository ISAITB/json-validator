package eu.europa.ec.itb.json.validation;

import com.google.gson.JsonParser;
import eu.europa.ec.itb.json.ApplicationConfig;
import eu.europa.ec.itb.json.DomainConfig;
import eu.europa.ec.itb.json.DomainConfigCache;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Component used to load locally-defined schemas and cache their paths. Cached schemas are then used in schema
 * lookups by first checking to see if a given URI corresponds to a locally cached schema before making an online
 * lookup.
 */
@Component
public class LocalSchemaCache {

    private static final Logger LOG = LoggerFactory.getLogger(LocalSchemaCache.class);

    @Autowired
    private DomainConfigCache domainConfigCache;
    @Autowired
    private ApplicationConfig appConfig;

    private final Map<String, Path> idsToPaths = new ConcurrentHashMap<>();

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
     * Get the local schema for a given ID and configuration domain.
     *
     * @param domainConfig The domain configuration.
     * @param id The schema ID.
     * @return The path.
     */
    public Optional<Path> getSchemaForId(DomainConfig domainConfig, String id) {
        return Optional.ofNullable(idsToPaths.get(schemaKey(domainConfig, id)));
    }

}
