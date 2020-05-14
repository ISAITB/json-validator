package eu.europa.ec.itb.json.validation;

import com.gitb.core.AnyContent;
import com.gitb.core.ValueEmbeddingEnumeration;
import com.gitb.tr.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import eu.europa.ec.itb.json.DomainConfig;
import eu.europa.ec.itb.json.errors.ValidatorException;
import eu.europa.ec.itb.json.utils.Utils;
import jakarta.json.stream.JsonParser;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.leadpony.justify.api.JsonSchema;
import org.leadpony.justify.api.JsonValidationService;
import org.leadpony.justify.api.ProblemHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.xml.datatype.DatatypeConfigurationException;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

@Component
@Scope("prototype")
public class JSONValidator {

    private static final Logger logger = LoggerFactory.getLogger(JSONValidator.class);

    @Autowired
    private FileManager fileManager;

    private ObjectFactory objectFactory = new ObjectFactory();
    private File inputFileToValidate;
    private final DomainConfig domainConfig;
    private final boolean locationAsPointer;
    private String validationType;
    private List<FileInfo> externalSchemaFileInfo;
    private final SchemaCombinationApproach externalSchemaCombinationApproach;

    public JSONValidator(File inputFileToValidate, String validationType, List<FileInfo> externalSchemas, SchemaCombinationApproach externalSchemaCombinationApproach, DomainConfig domainConfig, boolean locationAsPointer) {
        this.inputFileToValidate = inputFileToValidate;
        this.validationType = validationType;
        this.domainConfig = domainConfig;
        this.externalSchemaFileInfo = externalSchemas;
        this.externalSchemaCombinationApproach = externalSchemaCombinationApproach;
        this.locationAsPointer = locationAsPointer;
        if (validationType == null) {
            this.validationType = domainConfig.getType().get(0);
        }
    }

    public TAR validate() {
        try {
            fileManager.signalValidationStart(domainConfig.getDomainName());
            return validateInternal_Justify();
        } finally {
            fileManager.signalValidationEnd(domainConfig.getDomainName());
        }
    }

    private File prettyPrint(File input) {
        try (FileReader in = new FileReader(input)) {
            JsonElement json = com.google.gson.JsonParser.parseReader(in);
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            String jsonOutput = gson.toJson(json);
            File output = new File(input.getParent(), input.getName()+".pretty");
            FileUtils.writeStringToFile(output, jsonOutput, StandardCharsets.UTF_8);
            return output;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to parse JSON input.", e);
        }
    }

    private List<String> validateAgainstSetOfSchemas(List<FileInfo> schemaFileInfos, SchemaCombinationApproach combinationApproach) {
        List<String> aggregatedErrorMessages = new ArrayList<>();
        if (combinationApproach == SchemaCombinationApproach.allOf) {
            // All schema validations must result in success.
            for (FileInfo fileInfo: schemaFileInfos) {
                aggregatedErrorMessages.addAll(validateAgainstSchema(fileInfo.getFile()));
            }
        } else if (combinationApproach == SchemaCombinationApproach.oneOf) {
            // All schemas need to be validated but only one should validate successfully.
            int successCount = 0;
            for (FileInfo fileInfo: schemaFileInfos) {
                List<String> latestErrors = validateAgainstSchema(fileInfo.getFile());
                if (latestErrors.isEmpty()) {
                    successCount += 1;
                } else {
                    aggregatedErrorMessages.addAll(latestErrors);
                }
            }
            if (successCount == 1) {
                aggregatedErrorMessages.clear();
            } else if (successCount > 1) {
                if (locationAsPointer) {
                    aggregatedErrorMessages.add("[#/] Only one schema should be valid. Instead the content validated against "+successCount+" schemas.");
                } else {
                    aggregatedErrorMessages.add("[0,0] Only one schema should be valid. Instead the content validated against "+successCount+" schemas.");
                }
            }
        } else {
            // Any of the schemas should validate successfully.
            for (FileInfo fileInfo: schemaFileInfos) {
                List<String> latestErrors = validateAgainstSchema(fileInfo.getFile());
                if (latestErrors.isEmpty()) {
                    aggregatedErrorMessages.clear();
                    break;
                } else {
                    aggregatedErrorMessages.addAll(latestErrors);
                }
            }
        }
        return aggregatedErrorMessages;
    }

    private List<String> validateAgainstSchema(File schemaFile) {
        JsonValidationService service = JsonValidationService.newInstance();
        JsonSchema schema = service.readSchema(schemaFile.toPath());
        List<String> errorMessages = new ArrayList<>();
        ProblemHandler handler = service.createProblemPrinterBuilder(errorMessages::add).withLocation(!locationAsPointer).build();
        try (JsonParser parser = service.createParser(inputFileToValidate.toPath(), schema, handler)) {
            while (parser.hasNext()) {
                parser.next();
            }
        }
        return errorMessages;
    }

    private TAR validateInternal_Justify() {
        TAR report;
        List<FileInfo> preconfiguredSchemaFiles = fileManager.getPreconfiguredSchemaFileInfos(domainConfig, validationType);
        if (preconfiguredSchemaFiles.isEmpty() && externalSchemaFileInfo.isEmpty()) {
            // No preconfigured nor user-provided schemas defined.
            throw new ValidatorException("No schemas are defined for the validation.");
        } else {
            // Pretty-print input to get good location results.
            inputFileToValidate = prettyPrint(inputFileToValidate);
            List<String> aggregatedErrorMessages = new ArrayList<>();
            if (!preconfiguredSchemaFiles.isEmpty()) {
                SchemaCombinationApproach combinationApproach = domainConfig.getSchemaFile().get(validationType).getSchemaCombinationApproach();
                aggregatedErrorMessages.addAll(validateAgainstSetOfSchemas(preconfiguredSchemaFiles, combinationApproach));
            }
            if (!externalSchemaFileInfo.isEmpty()) {
                // We apply "allOf" semantics when there are both preconfigured and external schemas.
                aggregatedErrorMessages.addAll(validateAgainstSetOfSchemas(externalSchemaFileInfo, externalSchemaCombinationApproach));
            }
            report = createReport(aggregatedErrorMessages);
            report.setContext(new AnyContent());
            report.getContext().setType("map");
            AnyContent inputReportContent = new AnyContent();
            inputReportContent.setType("string");
            inputReportContent.setEmbeddingMethod(ValueEmbeddingEnumeration.STRING);
            inputReportContent.setName(ValidationConstants.INPUT_CONTENT);
            try {
                inputReportContent.setValue(FileUtils.readFileToString(inputFileToValidate, StandardCharsets.UTF_8));
            } catch (IOException e) {
                throw new IllegalStateException("Unable to generate output report", e);
            }
            report.getContext().getItem().add(inputReportContent);
        }
        return report;
    }

    private TAR validateInternal_Everit() {
        TAR report;
        // TODO specify schema draft version
        // TODO specify semantics when having multiple schemas (any, all, one)
        List<FileInfo> schemaFiles = fileManager.getPreconfiguredSchemaFileInfos(domainConfig, validationType);
        schemaFiles.addAll(externalSchemaFileInfo);
        if (schemaFiles.isEmpty()) {
            throw new ValidatorException("No schemas are defined for the validation.");
        } else {
            File schemaFile = schemaFiles.get(0).getFile();
            try (InputStream in = Files.newInputStream(schemaFile.toPath())) {
                JSONObject rawSchema = new JSONObject(new JSONTokener(in));
                Schema schema = SchemaLoader.load(rawSchema);
                // TODO allow external definition of character set.
                // TODO don't process using strings.
                JSONObject input = new JSONObject(FileUtils.readFileToString(inputFileToValidate, StandardCharsets.UTF_8));
                try {
                    schema.validate(input);
                    report = createReport();
                } catch (ValidationException validationException) {
                    // We have validation failures.
                    report = createReport(validationException);
                }
            } catch (IOException e) {
                throw new ValidatorException("An error occurred while running the schema validation.", e);
            }
        }
        return report;
    }

    private TAR createReport(List<String> errorMessages) {
        TAR report = new TAR();
        try {
            report.setDate(Utils.getXMLGregorianCalendarDateTime());
        } catch (DatatypeConfigurationException e) {
            throw new IllegalStateException("Exception while creating XMLGregorianCalendar", e);
        }
        report.setCounters(new ValidationCounters());
        report.getCounters().setNrOfWarnings(BigInteger.ZERO);
        report.getCounters().setNrOfAssertions(BigInteger.ZERO);
        if (errorMessages == null || errorMessages.isEmpty()) {
            report.setResult(TestResultType.SUCCESS);
            report.getCounters().setNrOfErrors(BigInteger.ZERO);
        } else {
            report.setResult(TestResultType.FAILURE);
            report.getCounters().setNrOfErrors(BigInteger.valueOf(errorMessages.size()));
            report.setReports(new TestAssertionGroupReportsType());
            for (String errorMessage: errorMessages) {
                BAR error = new BAR();
                String location = null;
                String message = null;
                if (locationAsPointer) {
                    // Messages are of the form "[POINTER] MESSAGE".
                    int closingBracketPosition = errorMessage.indexOf(']');
                    if (closingBracketPosition > 0 && closingBracketPosition < errorMessage.length()) {
                        location = errorMessage.substring(1, closingBracketPosition);
                        message = errorMessage.substring(closingBracketPosition + 1);
                    } else {
                        message = errorMessage;
                    }
                } else {
                    // Messages are of the form "[LINE,COL][POINTER] MESSAGE".
                    if (errorMessage.startsWith("[")) {
                        int firstClosingBracketPosition = errorMessage.indexOf(']');
                        if (firstClosingBracketPosition > 0) {
                            String locationPart = errorMessage.substring(1, firstClosingBracketPosition);
                            String[] locationParts = StringUtils.split(locationPart, ',');
                            if (locationParts.length > 0) {
                                location = ValidationConstants.INPUT_CONTENT+":"+locationParts[0]+":"+locationParts[1];
                            }
                            int finalClosingBracketPosition = errorMessage.indexOf(']', firstClosingBracketPosition+1);
                            if (finalClosingBracketPosition <= 0) {
                                finalClosingBracketPosition = firstClosingBracketPosition;
                            }
                            message = errorMessage.substring(finalClosingBracketPosition+1);
                        }
                    }
                }
                if (message == null) {
                    message = errorMessage;
                }
                error.setDescription(message.trim());
                error.setLocation(location);
                report.getReports().getInfoOrWarningOrError().add(objectFactory.createTestAssertionGroupReportsTypeError(error));
            }
        }
        return report;
    }

    private TAR createReport() {
        return createReport((ValidationException)null);
    }

    private TAR createReport(ValidationException validationException) {
        TAR report = new TAR();
        if (validationException == null) {
            report.setResult(TestResultType.SUCCESS);
        } else {
            report.setResult(TestResultType.FAILURE);
            report.setCounters(new ValidationCounters());
            report.getCounters().setNrOfErrors(BigInteger.ZERO);
            report.getCounters().setNrOfWarnings(BigInteger.ZERO);
            report.getCounters().setNrOfAssertions(BigInteger.ZERO);
            report.setReports(new TestAssertionGroupReportsType());
            addReportItem(report, validationException);
        }
        try {
            report.setDate(Utils.getXMLGregorianCalendarDateTime());
        } catch (DatatypeConfigurationException e) {
            throw new IllegalStateException("Exception while creating XMLGregorianCalendar", e);
        }
        return report;
    }

    private void addReportItem(TAR report, ValidationException exception) {
        if (exception.getCausingExceptions() == null || exception.getCausingExceptions().isEmpty()) {
            // Leaf - record report item.
            BAR error = new BAR();
            if (StringUtils.isNotBlank(exception.getKeyword())) {
                error.setDescription("["+exception.getKeyword()+"] " + exception.getMessage());
            } else {
                error.setDescription(exception.getMessage());
            }
            error.setLocation(exception.getPointerToViolation());
            report.getReports().getInfoOrWarningOrError().add(objectFactory.createTestAssertionGroupReportsTypeError(error));
            report.getCounters().setNrOfErrors(report.getCounters().getNrOfErrors().add(BigInteger.ONE));
        } else {
            // Step into children failures.
            exception.getCausingExceptions().forEach((childException) -> addReportItem(report, childException));
        }
    }

}