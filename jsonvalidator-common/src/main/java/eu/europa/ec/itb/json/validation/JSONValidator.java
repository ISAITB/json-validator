package eu.europa.ec.itb.json.validation;

import com.gitb.core.AnyContent;
import com.gitb.core.ValueEmbeddingEnumeration;
import com.gitb.tr.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonSyntaxException;
import eu.europa.ec.itb.json.DomainConfig;
import eu.europa.ec.itb.validation.commons.FileInfo;
import eu.europa.ec.itb.validation.commons.Utils;
import eu.europa.ec.itb.validation.commons.artifact.ValidationArtifactCombinationApproach;
import eu.europa.ec.itb.validation.commons.error.ValidatorException;
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
    private final ValidationArtifactCombinationApproach externalSchemaCombinationApproach;

    public JSONValidator(File inputFileToValidate, String validationType, List<FileInfo> externalSchemas, ValidationArtifactCombinationApproach externalSchemaCombinationApproach, DomainConfig domainConfig, boolean locationAsPointer) {
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

    private List<String> validateAgainstSetOfSchemas(List<FileInfo> schemaFileInfos, ValidationArtifactCombinationApproach combinationApproach) {
        List<String> aggregatedErrorMessages = new ArrayList<>();
        if (combinationApproach == ValidationArtifactCombinationApproach.ALL) {
            // All schema validations must result in success.
            for (FileInfo fileInfo: schemaFileInfos) {
                aggregatedErrorMessages.addAll(validateAgainstSchema(fileInfo.getFile()));
            }
        } else if (combinationApproach == ValidationArtifactCombinationApproach.ONE_OF) {
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
                aggregatedErrorMessages.add("[0,0][] Only one schema should be valid. Instead the content validated against "+successCount+" schemas.");
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
        ProblemHandler handler = service.createProblemPrinterBuilder(errorMessages::add).withLocation(true).build();
        try (JsonParser parser = service.createParser(inputFileToValidate.toPath(), schema, handler)) {
            while (parser.hasNext()) {
                parser.next();
            }
        }
        return errorMessages;
    }

    private TAR validateInternal() {
        TAR report;
        List<FileInfo> preconfiguredSchemaFiles = fileManager.getPreconfiguredValidationArtifacts(domainConfig, validationType);
        if (preconfiguredSchemaFiles.isEmpty() && externalSchemaFileInfo.isEmpty()) {
            // No preconfigured nor user-provided schemas defined.
            throw new ValidatorException("No schemas are defined for the validation.");
        } else {
            // Pretty-print input to get good location results.
            inputFileToValidate = prettyPrint(inputFileToValidate);
            List<String> aggregatedErrorMessages = new ArrayList<>();
            if (!preconfiguredSchemaFiles.isEmpty()) {
                ValidationArtifactCombinationApproach combinationApproach = domainConfig.getSchemaInfo(validationType).getArtifactCombinationApproach();
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
        report.setDate(Utils.getXMLGregorianCalendarDateTime());
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
                String locationPointer = null;
                String locationLineColumn = null;
                String message = null;
                // Messages are of the form "[LINE,COL][POINTER] MESSAGE".
                if (errorMessage.startsWith("[")) {
                    int firstClosingBracketPosition = errorMessage.indexOf(']');
                    if (firstClosingBracketPosition > 0) {
                        locationLineColumn = errorMessage.substring(1, firstClosingBracketPosition);
                        int finalClosingBracketPosition = errorMessage.indexOf(']', firstClosingBracketPosition+1);
                        if (finalClosingBracketPosition <= 0) {
                            finalClosingBracketPosition = firstClosingBracketPosition;
                        } else {
                            locationPointer = errorMessage.substring(firstClosingBracketPosition+2, finalClosingBracketPosition);
                        }
                        message = errorMessage.substring(finalClosingBracketPosition+1).trim();
                    }
                }
                String locationToSetInReport = null;
                if (locationAsPointer) {
                    locationToSetInReport = locationPointer;
                } else {
                    String[] locationLineColumnParts = StringUtils.split(locationLineColumn, ',');
                    if (locationLineColumnParts != null && locationLineColumnParts.length > 0) {
                        locationToSetInReport = ValidationConstants.INPUT_CONTENT+":"+locationLineColumnParts[0]+":"+locationLineColumnParts[1];
                    }
                    if (message != null) {
                        if (StringUtils.isNotBlank(locationPointer)) {
                            message = "["+locationPointer+"] " + message;
                        }
                    }
                }
                if (message == null) {
                    message = errorMessage;
                }
                error.setDescription(message.trim());
                error.setLocation(locationToSetInReport);
                report.getReports().getInfoOrWarningOrError().add(objectFactory.createTestAssertionGroupReportsTypeError(error));
            }
        }
        return report;
    }

}