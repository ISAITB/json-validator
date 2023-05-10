package eu.europa.ec.itb.json.validation.location;

import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import eu.europa.ec.itb.json.validation.ValidationConstants;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.function.Function;

/**
 * Class used to detect location strings for JSON paths.
 * <p>
 * Based on <a href="https://stackoverflow.com/questions/63585750/get-the-line-number-of-a-json-file-given-a-json-path-json-pointer-in-java">this SO post</a>.
 */
public class NodeCoordinateDetector implements Function<String, String> {

    private static final Logger LOG = LoggerFactory.getLogger(NodeCoordinateDetector.class);

    private CustomJsonNodeFactory locationAwareFactory = null;
    private JsonNode rootNode;

    /**
     * Constructor.
     *
     * @param inputFile The (pretty-printed) input file. This will be used to make a line number aware parse to dereference pointer expressions.
     */
    public NodeCoordinateDetector(File inputFile) {
        try (var reader = new FileReader(inputFile)) {
            var parser = new CustomParserFactory().createParser(reader);
            locationAwareFactory = new CustomJsonNodeFactory(JsonNodeFactory.instance, parser);
            var customObjectMapper = new ObjectMapper();
            customObjectMapper.setConfig(customObjectMapper.getDeserializationConfig().with(locationAwareFactory));
            rootNode = customObjectMapper.readTree(parser);
        } catch (IOException e) {
            LOG.warn("Unable to parse JSON content for location-aware reporting", e);
            rootNode = null;
        }
    }

    /**
     * Lookup a given node path expression (expected to be a JSONPointer) and return the location string for the TAR report.
     *
     * @param path The path expression (expected to be a JSONPointer),
     * @return The location expression to be used in the TAR report.
     */
    @Override
    public String apply(String path) {
        JsonLocation location = null;
        if (rootNode != null && locationAwareFactory != null && StringUtils.isNotBlank(path)) {
            try {
                var relatedNode = rootNode.at(JsonPointer.compile(path));
                if (relatedNode != null) {
                    location = locationAwareFactory.getLocationForNode(relatedNode);
                }
            } catch (RuntimeException e) {
                // Unable to parse as valid JSON Path expression - Do nothing.
            }
        }
        if (location == null) {
            return ValidationConstants.INPUT_CONTENT+":0:0";
        } else {
            return ValidationConstants.INPUT_CONTENT+":"+location.getLineNr()+":0";
        }
    }
}
