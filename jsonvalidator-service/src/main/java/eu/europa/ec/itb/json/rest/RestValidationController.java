package eu.europa.ec.itb.json.rest;

import com.gitb.tr.TAR;
import eu.europa.ec.itb.json.ApplicationConfig;
import eu.europa.ec.itb.json.DomainConfig;
import eu.europa.ec.itb.json.InputHelper;
import eu.europa.ec.itb.json.rest.model.Input;
import eu.europa.ec.itb.json.validation.FileManager;
import eu.europa.ec.itb.json.validation.JSONValidator;
import eu.europa.ec.itb.json.validation.ValidationSpecs;
import eu.europa.ec.itb.validation.commons.LocalisationHelper;
import eu.europa.ec.itb.validation.commons.Utils;
import eu.europa.ec.itb.validation.commons.error.ValidatorException;
import eu.europa.ec.itb.validation.commons.web.errors.NotFoundException;
import eu.europa.ec.itb.validation.commons.web.rest.BaseRestController;
import eu.europa.ec.itb.validation.commons.web.rest.model.Output;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.LocaleUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import jakarta.servlet.http.HttpServletRequest;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Objects;

/**
 * REST controller to allow triggering the validator via its REST API.
 */
@Tag(name = "/{domain}/api", description = "Operations for the validation of JSON and YAML content based on JSON schema(s).")
@RestController
public class RestValidationController extends BaseRestController<DomainConfig, ApplicationConfig, FileManager, InputHelper> {

    @Autowired
    private ApplicationContext ctx = null;
    @Autowired
    private FileManager fileManager = null;

    /**
     * Service to trigger one validation for the provided input and settings.
     *
     * @param domain The relevant domain for the validation.
     * @param in The input for the validation.
     * @param request The HTTP request.
     * @return The result of the validator.
     */
    @Operation(summary = "Validate a single document.", description="Validate a single document. The content can be provided either within the request as a BASE64 encoded string or remotely as a URL.")
    @ApiResponse(responseCode = "200", description = "Success (for successful validation)", content = { @Content(mediaType = MediaType.APPLICATION_XML_VALUE), @Content(mediaType = MediaType.APPLICATION_JSON_VALUE) })
    @ApiResponse(responseCode = "500", description = "Error (If a problem occurred with processing the request)", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    @ApiResponse(responseCode = "404", description = "Not found (for an invalid domain value)", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    @PostMapping(value = "/{domain}/api/validate", consumes = MediaType.APPLICATION_JSON_VALUE, produces = { MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE } )
    public ResponseEntity<StreamingResponseBody> validate(
            @Parameter(required = true, name = "domain", description = "A fixed value corresponding to the specific validation domain.",
                    examples = {
                            @ExampleObject(name="order", summary="Sample 'order' configuration", value="order", description = "The domain value to use for the demo 'order' validator at https://www.itb.ec.europe.eu/json/order/upload."),
                            @ExampleObject(name="any", summary="Generic 'any' configuration", value = "any", description = "The domain value to use for the generic 'any' validator at https://www.itb.ec.europe.eu/json/any/upload used to validate JSON with user-provided schemas.")
                    }
            )
            @PathVariable("domain") String domain,
            @Parameter(required = true, name = "input", description = "The input for the validation (content and metadata for one document).")
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(
                            examples = {
                                    @ExampleObject(name="order1", summary = "Validate string", description = "Validate content provided as a string for the 'large' validation type of the 'order' sample validator (see https://www.itb.ec.europe.eu/json/order/upload). To try it out select also 'order' for the 'domain' parameter.", value = """
                                    {
                                        "contentToValidate": "{\\r\\n  \\"shipTo\\": {\\r\\n    \\"name\\": \\"John Doe\\",\\r\\n    \\"street\\": \\"Europa Avenue 123\\",\\r\\n    \\"city\\": \\"Brussels\\",\\r\\n    \\"zip\\": 1000\\r\\n  },\\r\\n  \\"billTo\\": {\\r\\n    \\"name\\": \\"Jane Doe\\",\\r\\n    \\"street\\": \\"Europa Avenue 210\\",\\r\\n    \\"city\\": \\"Brussels\\",\\r\\n    \\"zip\\": 1000\\r\\n  },\\r\\n  \\"orderDate\\": \\"2020-01-22\\",\\r\\n  \\"comment\\": \\"Send in one package please\\",\\r\\n  \\"items\\": [\\r\\n    {\\r\\n      \\"partNum\\": \\"XYZ-123876\\",\\r\\n      \\"productName\\": \\"Mouse\\",\\r\\n      \\"quantity\\": 20,\\r\\n      \\"priceEUR\\": 15.99,\\r\\n      \\"comment\\": \\"Confirm this is wireless\\"\\r\\n    },\\r\\n    {\\r\\n      \\"partNum\\": \\"ABC-32478\\",\\r\\n      \\"productName\\": \\"Keyboard\\",\\r\\n      \\"quantity\\": 15,\\r\\n      \\"priceEUR\\": 25.50\\r\\n    }\\r\\n  ]\\r\\n}",
                                        "validationType": "large"
                                    }
                                    """),
                                    @ExampleObject(name="order2", summary = "Validate remote URI", description = "Validate content provided as a URI for the 'large' validation type of the 'order' sample validator (see https://www.itb.ec.europe.eu/json/order/upload). To try it out select also 'order' for the 'domain' parameter.", value = """
                                    {
                                        "contentToValidate": "https://www.itb.ec.europa.eu/files/samples/json/sample-invalid.json",
                                        "embeddingMethod": "URL",
                                        "validationType": "large"
                                    }
                                    """),
                                    @ExampleObject(name="order3", summary = "Validate Base64-encoded content", description = "Validate content encoded in a Base64 string for the 'large' validation type of the 'order' sample validator (see https://www.itb.ec.europe.eu/json/order/upload). To try it out select also 'order' for the 'domain' parameter.", value = """
                                    {
                                        "contentToValidate": "ewogICJzaGlwVG8iOiB7CiAgICAibmFtZSI6ICJKb2huIERvZSIsCiAgICAic3RyZWV0IjogIkV1cm9wYSBBdmVudWUgMTIzIiwKICAgICJjaXR5IjogIkJydXNzZWxzIiwKICAgICJ6aXAiOiAxMDAwCiAgfSwKICAiYmlsbFRvIjogewogICAgIm5hbWUiOiAiSmFuZSBEb2UiLAogICAgInN0cmVldCI6ICJFdXJvcGEgQXZlbnVlIDIxMCIsCiAgICAiY2l0eSI6ICJCcnVzc2VscyIsCiAgICAiemlwIjogMTAwMAogIH0sCiAgIm9yZGVyRGF0ZSI6ICIyMDIwLTAxLTIyIiwKICAiY29tbWVudCI6ICJTZW5kIGluIG9uZSBwYWNrYWdlIHBsZWFzZSIsCiAgIml0ZW1zIjogWwogICAgewogICAgICAicGFydE51bSI6ICJYWVotMTIzODc2IiwKICAgICAgInByb2R1Y3ROYW1lIjogIk1vdXNlIiwKICAgICAgInF1YW50aXR5IjogMjAsCiAgICAgICJwcmljZUVVUiI6IDE1Ljk5LAogICAgICAiY29tbWVudCI6ICJDb25maXJtIHRoaXMgaXMgd2lyZWxlc3MiCiAgICB9LAogICAgewogICAgICAicGFydE51bSI6ICJBQkMtMzI0NzgiLAogICAgICAicHJvZHVjdE5hbWUiOiAiS2V5Ym9hcmQiLAogICAgICAicXVhbnRpdHkiOiAxNSwKICAgICAgInByaWNlRVVSIjogMjUuNTAKICAgIH0KICBdCn0=",
                                        "embeddingMethod": "BASE64",
                                        "validationType": "large"
                                    }
                                    """),
                                    @ExampleObject(name="any", summary = "Validate remote URI with user-provided schemas", description = "Validate content provided as a URI and using user-provided schemas, with the generic 'any' validator (see https://www.itb.ec.europe.eu/json/any/upload). To try it out select also 'any' for the 'domain' parameter.", value = """
                                    {
                                        "contentToValidate": "https://www.itb.ec.europa.eu/files/samples/json/sample-invalid.json",
                                        "embeddingMethod": "URL",
                                        "externalSchemas": [
                                            {
                                                "schema": "https://raw.githubusercontent.com/ISAITB/validator-resources-json-sample/master/resources/schemas/PurchaseOrder.schema.json",
                                                "embeddingMethod": "URL"
                                            },
                                            {
                                                "schema": "https://raw.githubusercontent.com/ISAITB/validator-resources-json-sample/master/resources/schemas/PurchaseOrder-large.schema.json",
                                                "embeddingMethod": "URL"
                                            }
                                        ]
                                    }
                                    """)
                            }
                    )
            )
            @RequestBody Input in,
            HttpServletRequest request
    ) {
        DomainConfig domainConfig = validateDomain(domain);
        /*
         * Important: We call executeValidationProcess here and not in the return statement because the StreamingResponseBody
         * uses a separate thread. Doing so would break the ThreadLocal used in the statistics reporting.
         */
        var report = executeValidationProcess(in, domainConfig);
        var reportType = MediaType.valueOf(getAcceptHeader(request, MediaType.APPLICATION_XML_VALUE));
        return ResponseEntity.ok()
                .contentType(reportType)
                .body(outputStream -> {
                    if (MediaType.APPLICATION_JSON.equals(reportType)) {
                        writeReportAsJson(outputStream, report, domainConfig);
                    } else {
                        var wrapReportDataInCDATA = Objects.requireNonNullElse(in.getWrapReportDataInCDATA(), false);
                        fileManager.saveReport(report, outputStream, domainConfig, wrapReportDataInCDATA);
                    }
                });
    }

    /**
     * Execute the process to validate the content.
     *
     * @param in The input for the validation of one JSON document.
     * @param domainConfig The validation domain.
     * @return The report.
     */
    private TAR executeValidationProcess(Input in, DomainConfig domainConfig) {
        var parentFolder = fileManager.createTemporaryFolderPath();
        var localiser = new LocalisationHelper(domainConfig, Utils.getSupportedLocale(LocaleUtils.toLocale(in.getLocale()), domainConfig));
        try {
            // Extract and validate inputs.
            var validationType = inputHelper.validateValidationType(domainConfig, in.getValidationType());
            var locationAsPointer = Objects.requireNonNullElse(in.getLocationAsPointer(), true);
            var addInputToReport = Objects.requireNonNullElse(in.getAddInputToReport(), false);
            var contentEmbeddingMethod = inputHelper.getEmbeddingMethod(in.getEmbeddingMethod());
            var externalSchemaCombinationApproach = inputHelper.getValidationArtifactCombinationApproach(domainConfig, validationType, in.getExternalSchemaCombinationApproach());
            var externalSchemas = getExternalSchemas(domainConfig, in.getExternalSchemas(), validationType, null, parentFolder);
            var contentToValidate = inputHelper.validateContentToValidate(in.getContentToValidate(), contentEmbeddingMethod, null, parentFolder, domainConfig.getHttpVersion()).getFile();
            var builder = ValidationSpecs.builder(contentToValidate, localiser, domainConfig)
                    .withValidationType(validationType)
                    .withExternalSchemas(externalSchemas, externalSchemaCombinationApproach);
            if (locationAsPointer) builder = builder.locationAsPointer();
            if (addInputToReport) builder = builder.addInputToReport();
            // Validate.
            JSONValidator validator = ctx.getBean(JSONValidator.class, builder.build());
            return validator.validate().getDetailedReport();
        } catch (ValidatorException | NotFoundException e) {
            // Localisation of the ValidatorException takes place in the ErrorHandler.
            throw e;
        } catch (Exception e) {
            // Localisation of the ValidatorException takes place in the ErrorHandler.
            throw new ValidatorException(e);
        } finally {
            FileUtils.deleteQuietly(parentFolder);
        }
    }

    /**
     * Validate multiple JSON document inputs considering their settings and producing separate validation reports.
     *
     * @param domain The domain where the validator is executed.
     * @param inputs The input for the validation (content and metadata for one or more JSON documents).
     * @param request The HTTP request.
     * @return The validation result.
     */
    @Operation(summary = "Validate multiple documents.", description="Validate multiple documents. The content for each instance can be provided either within the request as a BASE64 encoded string or remotely as a URL.")
    @ApiResponse(responseCode = "200", description = "Success (for successful validation)", content = { @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, array = @ArraySchema(schema = @Schema(implementation = Output.class))) })
    @ApiResponse(responseCode = "500", description = "Error (If a problem occurred with processing the request)", content = @Content)
    @ApiResponse(responseCode = "404", description = "Not found (for an invalid domain value)", content = @Content)
    @PostMapping(value = "/{domain}/api/validateMultiple", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Output[] validateMultiple(
            @Parameter(required = true, name = "domain", description = "A fixed value corresponding to the specific validation domain.",
                    examples = {
                            @ExampleObject(name="order", summary="Sample 'order' configuration", value="order", description = "The domain value to use for the demo 'order' validator at https://www.itb.ec.europe.eu/json/order/upload."),
                            @ExampleObject(name="any", summary="Generic 'any' configuration", value = "any", description = "The domain value to use for the generic 'any' validator at https://www.itb.ec.europe.eu/json/any/upload used to validate JSON with user-provided schemas.")
                    }
            )
            @PathVariable("domain") String domain,
            @Parameter(required = true, name = "input", description = "The input for the validation (content and metadata for one or more documents).")
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(
                            examples = {
                                    @ExampleObject(name="order", summary = "Validate remote URIs", description = "Validate content provided as URIs for the 'large' validation type of the 'order' sample validator (see https://www.itb.ec.europe.eu/json/order/upload). To try it out select also 'order' for the 'domain' parameter.", value = """
                                    [
                                        {
                                            "contentToValidate": "https://www.itb.ec.europa.eu/files/samples/json/sample-invalid.json",
                                            "embeddingMethod": "URL",
                                            "validationType": "large"
                                        },
                                        {
                                            "contentToValidate": "https://www.itb.ec.europa.eu/files/samples/json/sample-invalid.json",
                                            "embeddingMethod": "URL",
                                            "validationType": "basic"
                                        }
                                    ]
                                    """),
                                    @ExampleObject(name="any", summary = "Validate remote URIs with user-provided schemas", description = "Validate content provided as URIs and using user-provided schemas, with the generic 'any' validator (see https://www.itb.ec.europe.eu/json/any/upload). To try it out select also 'any' for the 'domain' parameter.", value = """
                                    [
                                        {
                                            "contentToValidate": "https://www.itb.ec.europa.eu/files/samples/json/sample-invalid.json",
                                            "embeddingMethod": "URL",
                                            "externalSchemas": [
                                                {
                                                    "schema": "https://raw.githubusercontent.com/ISAITB/validator-resources-json-sample/master/resources/schemas/PurchaseOrder.schema.json",
                                                    "embeddingMethod": "URL"
                                                }
                                            ]
                                        },
                                        {
                                            "contentToValidate": "https://www.itb.ec.europa.eu/files/samples/json/sample-invalid.json",
                                            "embeddingMethod": "URL",
                                            "externalSchemas": [
                                                {
                                                    "schema": "https://raw.githubusercontent.com/ISAITB/validator-resources-json-sample/master/resources/schemas/PurchaseOrder.schema.json",
                                                    "embeddingMethod": "URL"
                                                },
                                                {
                                                    "schema": "https://raw.githubusercontent.com/ISAITB/validator-resources-json-sample/master/resources/schemas/PurchaseOrder-large.schema.json",
                                                    "embeddingMethod": "URL"
                                                }
                                            ]
                                        }
                                    ]
                                    """)
                            }
                    )
            )
            @RequestBody Input[] inputs,
            HttpServletRequest request
    ) {
        DomainConfig domainConfig = validateDomain(domain);
        var outputs = new ArrayList<Output>(inputs.length);
        for (Input input: inputs) {
            Output output = new Output();
            var report = executeValidationProcess(input, domainConfig);
            try (var bos = new ByteArrayOutputStream()) {
                var wrapReportDataInCDATA = Objects.requireNonNullElse(input.getWrapReportDataInCDATA(), false);
                fileManager.saveReport(report, bos, domainConfig, wrapReportDataInCDATA);
                output.setReport(Base64.getEncoder().encodeToString(bos.toByteArray()));
                outputs.add(output);
            } catch (IOException e) {
                throw new ValidatorException(e);
            }
        }
        return outputs.toArray(new Output[] {});
    }

}
