package eu.europa.ec.itb.json.validation.location;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;

import java.io.IOException;
import java.io.Reader;

/**
 * Custom parser factory that removes thread safety for increased speed.
 */
public class CustomParserFactory extends JsonFactory {

    private JsonParser parser;

    @Override
    public JsonParser createParser(Reader r) throws IOException {
        parser = super.createParser(r);
        return parser;
    }

    @Override
    public JsonParser createParser(String content) throws IOException {
        parser = super.createParser(content);
        return parser;
    }
}