package eu.europa.ec.itb.json.validation;

import com.networknt.schema.AbsoluteIri;
import com.networknt.schema.InputFormat;
import com.networknt.schema.SchemaRegistry;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import eu.europa.ec.itb.json.DomainConfig;
import eu.europa.ec.itb.validation.commons.FileInfo;
import eu.europa.ec.itb.validation.commons.ImportedFileAuthorizer;
import eu.europa.ec.itb.validation.commons.ImportedUriAuthorizer;
import eu.europa.ec.itb.validation.commons.error.ValidatorException;
import eu.europa.ec.itb.validation.commons.config.ApplicationConfig;
import eu.europa.ec.itb.validation.commons.config.NormalizedURI;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.json.JsonMapper;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for the loading of schemas referenced from JSON schemas.
 */
class LocalSchemaResolverTest {

    private static final String SCHEMA = "{\"type\":\"object\"}";

    @TempDir
    Path root;
    private HttpServer server;
    private final AtomicInteger blockedTargetRequests = new AtomicInteger();
    private final List<String> authorizationHeaders = Collections.synchronizedList(new ArrayList<>());
    private DomainConfig domainConfig;
    private LocalSchemaCache schemaCache;

    @BeforeEach
    void setUp() throws IOException {
        domainConfig = mock(DomainConfig.class);
        when(domainConfig.getHttpVersion()).thenReturn(HttpClient.Version.HTTP_1_1);
        schemaCache = mock(LocalSchemaCache.class);
        when(schemaCache.getSchemaForId(any(), any())).thenReturn(Optional.empty());
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/allowed/schema.json", exchange -> {
            authorizationHeaders.add(String.valueOf(exchange.getRequestHeaders().getFirst("Authorization")));
            respond(exchange, 200, SCHEMA, null);
        });
        server.createContext("/allowed/redirect.json", exchange -> respond(exchange, 302, "", "/allowed/schema.json"));
        server.createContext("/allowed/to-blocked.json", exchange -> respond(exchange, 302, "", "/blocked/schema.json"));
        server.createContext("/blocked/schema.json", exchange -> {
            blockedTargetRequests.incrementAndGet();
            respond(exchange, 200, SCHEMA, null);
        });
        server.start();
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    private static void respond(HttpExchange exchange, int status, String body, String location) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        if (location != null) {
            exchange.getResponseHeaders().add("Location", location);
        }
        exchange.sendResponseHeaders(status, bytes.length == 0 ? -1 : bytes.length);
        if (bytes.length > 0) {
            exchange.getResponseBody().write(bytes);
        }
        exchange.close();
    }

    private String serverUrl(String path) {
        return "http://127.0.0.1:" + server.getAddress().getPort() + path;
    }

    private ApplicationConfig appConfig(String... allowedUriPrefixes) {
        var appConfig = mock(ApplicationConfig.class);
        when(appConfig.isRestrictResourcesToDomain()).thenReturn(true);
        when(appConfig.getTmpFolder()).thenReturn(root.resolve("tmp").toString());
        when(appConfig.getResourceRoot()).thenReturn(root.resolve("resources").toString());
        when(appConfig.getNormalizedAllowedUriImports()).thenReturn(java.util.Arrays.stream(allowedUriPrefixes).map(prefix -> NormalizedURI.of(URI.create(prefix))).toList());
        when(domainConfig.getDomainRoot()).thenReturn(root.resolve("resources").resolve("domain").toString());
        return appConfig;
    }

    private LocalSchemaResolver resolver(FileInfo schemaInfo, String... allowedUriPrefixes) {
        var appConfig = appConfig(allowedUriPrefixes);
        return new LocalSchemaResolver(domainConfig, schemaCache, schemaInfo, ImportedUriAuthorizer.forUserContent(appConfig), ImportedFileAuthorizer.from(appConfig, domainConfig));
    }

    private String read(LocalSchemaResolver resolver, String path) throws IOException {
        try (var in = resolver.getResource(AbsoluteIri.of(serverUrl(path))).getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    void testAllowedRemoteSchemaIsLoaded() throws IOException {
        var resolver = resolver(new FileInfo(new File("schema.json")), serverUrl("/allowed"));
        assertEquals(SCHEMA, read(resolver, "/allowed/schema.json"));
    }

    @Test
    void testRemoteSchemaThatIsNotAllowedIsRejected() {
        var resolver = resolver(new FileInfo(new File("schema.json")));
        assertThrows(ValidatorException.class, () -> resolver.getResource(AbsoluteIri.of(serverUrl("/allowed/schema.json"))));
    }

    @Test
    void testRedirectToAllowedLocationIsFollowed() throws IOException {
        var resolver = resolver(new FileInfo(new File("schema.json")), serverUrl("/allowed"));
        assertEquals(SCHEMA, read(resolver, "/allowed/redirect.json"));
    }

    @Test
    void testRedirectToLocationThatIsNotAllowedIsRejected() {
        var resolver = resolver(new FileInfo(new File("schema.json")), serverUrl("/allowed"));
        assertThrows(ValidatorException.class, () -> read(resolver, "/allowed/to-blocked.json"));
        assertEquals(0, blockedTargetRequests.get(), "The redirect target must not be requested");
    }

    @Test
    void testCredentialsAreOnlySentToTheHostOfTheSchema() throws IOException {
        Consumer<HttpRequest.Builder> decorator = builder -> builder.header("Authorization", "Bearer secret");
        // Schema retrieved from the same host as the referenced schema.
        read(resolver(new FileInfo(new File("schema.json"), null, URI.create(serverUrl("/main.json")), decorator), serverUrl("/allowed")), "/allowed/schema.json");
        // Schema that was not retrieved from a URI (e.g. uploaded).
        read(resolver(new FileInfo(new File("schema.json"), null, null, decorator), serverUrl("/allowed")), "/allowed/schema.json");
        // Schema retrieved from another host.
        read(resolver(new FileInfo(new File("schema.json"), null, URI.create("https://93.184.216.34/main.json"), decorator), serverUrl("/allowed")), "/allowed/schema.json");
        assertEquals(List.of("Bearer secret", "null", "null"), authorizationHeaders);
    }

    private SchemaRegistry.Builder registryBuilder() {
        var resolver = resolver(new FileInfo(new File("schema.json")));
        return SchemaRegistry.builder().resourceLoaders(builder -> builder.add(resolver));
    }

    private String validationFailure(SchemaRegistry registry, String schema) {
        var error = assertThrows(Exception.class, () -> registry.getSchema(JsonMapper.shared().readTree(schema)).validate("{}", InputFormat.JSON));
        var message = new StringBuilder();
        for (Throwable t = error; t != null; t = t.getCause()) {
            message.append(t.getMessage()).append(" | ");
        }
        return message.toString();
    }

    @Test
    void testClasspathResourcesCannotBeReferencedIfBlocked() {
        var registry = LocalSchemaResolver.blockClasspathResources(registryBuilder()).build();
        for (String reference : List.of("classpath:i18n/jsv-messages.properties", "resource:i18n/jsv-messages.properties")) {
            var failure = validationFailure(registry, "{\"$schema\":\"https://json-schema.org/draft/2020-12/schema\",\"$ref\":\"%s\"}".formatted(reference));
            assertTrue(failure.contains("not allowed to be loaded"), "Unexpected failure: " + failure);
        }
    }

    @Test
    void testClasspathResourcesAreNotBlockedByDefault() {
        // Control: this documents the behaviour of the library that the restriction is needed for.
        var failure = validationFailure(registryBuilder().build(), "{\"$schema\":\"https://json-schema.org/draft/2020-12/schema\",\"$ref\":\"classpath:i18n/jsv-messages.properties\"}");
        assertFalse(failure.contains("not allowed to be loaded"), "Unexpected failure: " + failure);
    }

    @Test
    void testBundledMetaSchemasStillResolveIfClasspathResourcesAreBlocked() {
        var registry = LocalSchemaResolver.blockClasspathResources(registryBuilder()).build();
        var errors = registry.getSchema(JsonMapper.shared().readTree("{\"$schema\":\"https://json-schema.org/draft/2020-12/schema\",\"type\":\"object\"}")).validate("{}", InputFormat.JSON);
        assertTrue(errors.isEmpty());
    }

}
