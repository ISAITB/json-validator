package eu.europa.ec.itb.json.validation;

import com.gitb.core.AnyContent;
import com.gitb.core.ValueEmbeddingEnumeration;
import com.gitb.tr.*;
import com.gitb.vs.ValidateRequest;
import com.gitb.vs.ValidationResponse;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonSyntaxException;
import eu.europa.ec.itb.json.ApplicationConfig;
import eu.europa.ec.itb.json.DomainConfig;
import eu.europa.ec.itb.validation.commons.FileInfo;
import eu.europa.ec.itb.validation.commons.Utils;
import eu.europa.ec.itb.validation.commons.artifact.ValidationArtifactCombinationApproach;
import eu.europa.ec.itb.validation.commons.config.DomainPluginConfigProvider;
import eu.europa.ec.itb.validation.commons.error.ValidatorException;
import eu.europa.ec.itb.validation.plugin.PluginManager;
import eu.europa.ec.itb.validation.plugin.ValidationPlugin;
import jakarta.json.stream.JsonParser;
import jakarta.json.stream.JsonParsingException;
import org.apache.commons.io.FileUtils;
import org.leadpony.justify.api.JsonSchema;
import org.leadpony.justify.api.JsonValidationService;
import org.leadpony.justify.api.ProblemHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

@Component
@Scope("prototype")
public class JSONValidator {

    private static final Logger LOG = LoggerFactory.getLogger(JSONValidator.class);

    @Autowired
    private FileManager fileManager = null;
    @Autowired
    private PluginManager pluginManager = null;
    @Autowired
    private DomainPluginConfigProvider pluginConfigProvider = null;
    @Autowired
    private ApplicationConfig appConfig = null;
    @Autowired
    private JsonValidationService jsonValidationService = null;

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

    public String getDomain() {
        return this.domainConfig.getDomain();
    }

    public String getValidationType() { 
        return this.validationType;
    }

    public TAR validate() {
        TAR validationResult;
        try {
            fileManager.signalValidationStart(domainConfig.getDomainName());
            validationResult = validateInternal();
        } finally {
            fileManager.signalValidationEnd(domainConfig.getDomainName());
        }
        TAR pluginResult = validateAgainstPlugins();
        if (pluginResult != null) {
            validationResult = Utils.mergeReports(new TAR[] {validationResult, pluginResult});
        }
        return validationResult;
    }

    private TAR validateAgainstPlugins() {
        TAR pluginReport = null;
        ValidationPlugin[] plugins = pluginManager.getPlugins(pluginConfigProvider.getPluginClassifier(domainConfig, validationType));
        if (plugins != null && plugins.length > 0) {
            File pluginTmpFolder = new File(inputFileToValidate.getParentFile(), UUID.randomUUID().toString());
            try {
                pluginTmpFolder.mkdirs();
                ValidateRequest pluginInput = preparePluginInput(pluginTmpFolder);
                for (ValidationPlugin plugin: plugins) {
                    String pluginName = plugin.getName();
                    ValidationResponse response = plugin.validate(pluginInput);
                    if (response != null && response.getReport() != null && response.getReport().getReports() != null) {
                        LOG.info("Plugin [{}] produced [{}] report item(s).", pluginName, response.getReport().getReports().getInfoOrWarningOrError().size());
                        if (pluginReport == null) {
                            pluginReport = response.getReport();
                        } else {
                            pluginReport = Utils.mergeReports(new TAR[] {pluginReport, response.getReport()});
                        }
                    }
                }
            } finally {
                // Cleanup plugin tmp folder.
                FileUtils.deleteQuietly(pluginTmpFolder);
            }
        }
        return pluginReport;
    }

    private ValidateRequest preparePluginInput(File pluginTmpFolder) {
        File pluginInputFile = new File(pluginTmpFolder, UUID.randomUUID().toString()+".json");
        try {
            FileUtils.copyFile(inputFileToValidate, pluginInputFile);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to copy input file for plugin", e);
        }
        ValidateRequest request = new ValidateRequest();
        request.getInput().add(Utils.createInputItem("contentToValidate", pluginInputFile.getAbsolutePath()));
        request.getInput().add(Utils.createInputItem("domain", domainConfig.getDomainName()));
        request.getInput().add(Utils.createInputItem("validationType", validationType));
        request.getInput().add(Utils.createInputItem("tempFolder", pluginTmpFolder.getAbsolutePath()));
        return request;
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
        LinkedList<String> aggregatedErrorMessages = new LinkedList<>();
        if (combinationApproach == ValidationArtifactCombinationApproach.ALL) {
            // All schema validations must result in success.
            for (FileInfo fileInfo: schemaFileInfos) {
                aggregatedErrorMessages.addAll(validateAgainstSchema(fileInfo.getFile()));
            }
        } else if (combinationApproach == ValidationArtifactCombinationApproach.ONE_OF) {
            // All schemas need to be validated but only one should validate successfully.
            int successCount = 0;
            int branchCounter = 1;
            for (FileInfo fileInfo: schemaFileInfos) {
                List<String> latestErrors = validateAgainstSchema(fileInfo.getFile());
                if (latestErrors.isEmpty()) {
                    successCount += 1;
                } else {
                    addBranchErrors(aggregatedErrorMessages, latestErrors, branchCounter++);
                }
            }
            if (successCount == 0) {
                aggregatedErrorMessages.addFirst(appConfig.getBranchErrorMessages().get("oneOf"));
            } else if (successCount == 1) {
                aggregatedErrorMessages.clear();
            } else if (successCount > 1) {
                aggregatedErrorMessages.add("Only one schema should be valid. Instead the content validated against "+successCount+" schemas.");
            }
        } else {
            // Any of the schemas should validate successfully.
            int branchCounter = 1;
            for (FileInfo fileInfo: schemaFileInfos) {
                List<String> latestErrors = validateAgainstSchema(fileInfo.getFile());
                if (latestErrors.isEmpty()) {
                    aggregatedErrorMessages.clear();
                    break;
                } else {
                    addBranchErrors(aggregatedErrorMessages, latestErrors, branchCounter++);
                }
            }
            if (!aggregatedErrorMessages.isEmpty()) {
                aggregatedErrorMessages.addFirst(appConfig.getBranchErrorMessages().get("anyOf"));
            }
        }
        return aggregatedErrorMessages;
    }

    private void addBranchErrors(List<String> aggregatedErrorMessages, List<String> branchMessages, int branchCounter) {
        boolean firstForBranch = true;
        for (String error: branchMessages) {
            if (firstForBranch) {
                aggregatedErrorMessages.add(branchCounter+") "+error);
                firstForBranch = false;
            } else {
                aggregatedErrorMessages.add("   "+error);
            }
        }
    }

    private List<String> validateAgainstSchema(File schemaFile) {
        JsonSchema schema;
        try {
            schema = jsonValidationService.readSchema(schemaFile.toPath());
        } catch (JsonParsingException e) {
            throw new ValidatorException("Error while parsing JSON schema: "+e.getMessage(), e);
        }
        List<String> errorMessages = new ArrayList<>();
        ProblemHandler handler = jsonValidationService.createProblemPrinterBuilder(errorMessages::add).withLocation(true).build();
        try (JsonParser parser = jsonValidationService.createParser(inputFileToValidate.toPath(), schema, handler)) {
            while (parser.hasNext()) {
                parser.next();
            }
        }
        return errorMessages;
    }

    private TAR validateInternal() {
        // Pretty-print input to get good location results.
        inputFileToValidate = prettyPrint(inputFileToValidate);
        List<String> aggregatedErrorMessages = new ArrayList<>();
        List<FileInfo> preconfiguredSchemaFiles = fileManager.getPreconfiguredValidationArtifacts(domainConfig, validationType);
        if (!preconfiguredSchemaFiles.isEmpty()) {
            ValidationArtifactCombinationApproach combinationApproach = domainConfig.getSchemaInfo(validationType).getArtifactCombinationApproach();
            aggregatedErrorMessages.addAll(validateAgainstSetOfSchemas(preconfiguredSchemaFiles, combinationApproach));
        }
        if (!externalSchemaFileInfo.isEmpty()) {
            // We apply "allOf" semantics when there are both preconfigured and external schemas.
            aggregatedErrorMessages.addAll(validateAgainstSetOfSchemas(externalSchemaFileInfo, externalSchemaCombinationApproach));
        }
        TAR report = createReport(ErrorMessage.processMessages(aggregatedErrorMessages, appConfig.getBranchErrorMessageValues()));
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
        return report;
    }

    private TAR createReport(List<ErrorMessage> errorMessages) {
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
            for (ErrorMessage errorMessage: errorMessages) {
                BAR error = new BAR();
                error.setDescription(errorMessage.getMessage());
                if (locationAsPointer) {
                    error.setLocation(errorMessage.getLocationPointer());
                } else {
                    error.setLocation(ValidationConstants.INPUT_CONTENT+":"+errorMessage.getLocationLine()+":"+errorMessage.getLocationColumn());
                }
                report.getReports().getInfoOrWarningOrError().add(objectFactory.createTestAssertionGroupReportsTypeError(error));
            }
        }
        return report;
    }

}