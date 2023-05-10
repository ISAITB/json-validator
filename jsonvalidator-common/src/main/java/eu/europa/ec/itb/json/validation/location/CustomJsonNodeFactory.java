package eu.europa.ec.itb.json.validation.location;

import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.*;
import com.fasterxml.jackson.databind.util.RawValue;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.IdentityHashMap;

/**
 * Custom node Jackson factory that records nodes and their locations.
 *
 * Based on <a href="https://stackoverflow.com/questions/63585750/get-the-line-number-of-a-json-file-given-a-json-path-json-pointer-in-java">this SO post</a>.
 */
public class CustomJsonNodeFactory extends JsonNodeFactory {

    private final JsonNodeFactory delegate;
    private final JsonParser parser;

    /*
     * This is not a normal map because when nodes are created (and collected) their hashcodes are different from their final
     * ones as they don't take into account children.
     */
    private final IdentityHashMap<JsonNode, JsonLocation> locationMapping;

    /**
     * Constructor.
     *
     * @param nodeFactory The node factory to use.
     * @param parser The parser to use.
     */
    public CustomJsonNodeFactory(JsonNodeFactory nodeFactory, JsonParser parser) {
        this.delegate = nodeFactory;
        this.parser = parser;
        locationMapping = new IdentityHashMap<>();
    }

    /**
     * Given a node, find its location, or null if it wasn't found.
     *
     * @param jsonNode The node to search for.
     * @return The location of the node or null if not found.
     */
    public JsonLocation getLocationForNode(JsonNode jsonNode) {
        return this.locationMapping.get(jsonNode);
    }

    /**
     * Simple interceptor to mark the node in the lookup list and return it back.
     *
     * @param <T>  The type of the JsonNode.
     * @param node The node itself.
     * @return The node itself, having marked its location.
     */
    private <T extends JsonNode> T markNode(T node) {
        locationMapping.put(node, parser.getCurrentLocation());
        return node;
    }

    @Override
    public BooleanNode booleanNode(boolean v) {
        return markNode(delegate.booleanNode(v));
    }

    @Override
    public NullNode nullNode() {
        return markNode(delegate.nullNode());
    }

    @Override
    public NumericNode numberNode(byte v) {
        return markNode(delegate.numberNode(v));
    }

    @Override
    public ValueNode numberNode(Byte value) {
        return markNode(delegate.numberNode(value));
    }

    @Override
    public NumericNode numberNode(short v) {
        return markNode(delegate.numberNode(v));
    }

    @Override
    public ValueNode numberNode(Short value) {
        return markNode(delegate.numberNode(value));
    }

    @Override
    public NumericNode numberNode(int v) {
        return markNode(delegate.numberNode(v));
    }

    @Override
    public ValueNode numberNode(Integer value) {
        return markNode(delegate.numberNode(value));
    }

    @Override
    public NumericNode numberNode(long v) {
        return markNode(delegate.numberNode(v));
    }

    @Override
    public ValueNode numberNode(Long value) {
        return markNode(delegate.numberNode(value));
    }

    @Override
    public ValueNode numberNode(BigInteger v) {
        return markNode(delegate.numberNode(v));
    }

    @Override
    public NumericNode numberNode(float v) {
        return markNode(delegate.numberNode(v));
    }

    @Override
    public ValueNode numberNode(Float value) {
        return markNode(delegate.numberNode(value));
    }

    @Override
    public NumericNode numberNode(double v) {
        return markNode(delegate.numberNode(v));
    }

    @Override
    public ValueNode numberNode(Double value) {
        return markNode(delegate.numberNode(value));
    }

    @Override
    public ValueNode numberNode(BigDecimal v) {
        return markNode(delegate.numberNode(v));
    }

    @Override
    public TextNode textNode(String text) {
        return markNode(delegate.textNode(text));
    }

    @Override
    public BinaryNode binaryNode(byte[] data) {
        return markNode(delegate.binaryNode(data));
    }

    @Override
    public BinaryNode binaryNode(byte[] data, int offset, int length) {
        return markNode(delegate.binaryNode(data, offset, length));
    }

    @Override
    public ValueNode pojoNode(Object pojo) {
        return markNode(delegate.pojoNode(pojo));
    }

    @Override
    public ValueNode rawValueNode(RawValue value) {
        return markNode(delegate.rawValueNode(value));
    }

    @Override
    public ArrayNode arrayNode() {
        return markNode(delegate.arrayNode());
    }

    @Override
    public ArrayNode arrayNode(int capacity) {
        return markNode(delegate.arrayNode(capacity));
    }

    @Override
    public ObjectNode objectNode() {
        return markNode(delegate.objectNode());
    }

}
