package eu.europa.ec.itb.json.validation;

import com.gitb.core.AnyContent;
import com.gitb.core.ValueEmbeddingEnumeration;
import com.gitb.tr.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonSyntaxException;
import eu.europa.ec.itb.json.DomainConfig;
import eu.europa.ec.itb.json.errors.ValidatorException;
import eu.europa.ec.itb.json.utils.Utils;
import jakarta.json.stream.JsonParser;
import jakarta.json.stream.JsonParsingException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.leadpony.justify.api.JsonSchema;
import org.leadpony.justify.api.JsonValidationService;
import org.leadpony.justify.api.ProblemHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.xml.datatype.DatatypeConfigurationException;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Component
@Scope("prototype")
public class JSONValidator {

    @Autowired
    private FileManager fileManager = null;

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
            return validateInternal();
        } finally {
            fileManager.signalValidationEnd(domainConfig.getDomainName());
        }
    }

    private File prettyPrint(File input) {
        try (FileReader in = new FileReader(input)) {
            JsonElement json = com.google.gson.JsonParser.parseReader(in);
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            String jsonOutput = gson.toJson(json);
            File output = new File(input.getParent(), input.getName() + ".pretty");
            FileUtils.writeStringToFile(output, jsonOutput, StandardCharsets.UTF_8);
            return output;
        } catch (JsonSyntaxException e) {
            throw new ValidatorException("The provided input is not valid JSON.", e);
        } catch (IOException e) {
            throw new ValidatorException("Failed to parse JSON input.", e);
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
        JsonSchema schema = null;
        try {
            schema = service.readSchema(schemaFile.toPath());
        } catch (JsonParsingException e) {
            throw new ValidatorException("Error while parsing JSON schema: "+e.getMessage(), e);
        }
        List<String> errorMessages = new ArrayList<>();
        ProblemHandler handler = service.createProblemPrinterBuilder(errorMessages::add).withLocation(!locationAsPointer).build();
        try (JsonParser parser = service.createParser(inputFileToValidate.toPath(), schema, handler)) {
            while (parser.hasNext()) {
                parser.next();
            }
        }
        return errorMessages;
    }

    private TAR validateInternal() {
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
        report.setReports(new TestAssertionGroupReportsType());
        if (errorMessages == null || errorMessages.isEmpty()) {
            report.setResult(TestResultType.SUCCESS);
            report.getCounters().setNrOfErrors(BigInteger.ZERO);
        } else {
            report.setResult(TestResultType.FAILURE);
            report.getCounters().setNrOfErrors(BigInteger.valueOf(errorMessages.size()));
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

}