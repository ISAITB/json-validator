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
import eu.europa.ec.itb.validation.commons.LocalisationHelper;
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

/**
 * Component that carries out the validation of provided JSON content.
 */
@Component
@Scope("prototype")
public class JSONValidator {

    private static final Logger LOG = LoggerFactory.getLogger(JSONValidator.class);

    @Autowired
    private FileManager fileManager = null;
    @Autowired
    private PluginManager pluginManager = null;
    @Autowired
    private DomainPluginConfigProvider<DomainConfig> pluginConfigProvider = null;
    @Autowired
    private ApplicationConfig appConfig = null;
    @Autowired
    private JsonValidationService jsonValidationService = null;

    private ObjectFactory objectFactory = new ObjectFactory();
    private File inputFileToValidate;
    private final DomainConfig domainConfig;
    private final boolean locationAsPointer;
    private final boolean addInputToReport;
    private String validationType;
    private List<FileInfo> externalSchemaFileInfo;
    private final ValidationArtifactCombinationApproach externalSchemaCombinationApproach;
    private final LocalisationHelper localiser;

    /**
     * Constructor.
     *
     * @param inputFileToValidate The file that contains the JSON content to validate.
     * @param validationType The validation type to consider (can be null if there is only one).
     * @param externalSchemas The list of external (user-provided) JSON schemas.
     * @param externalSchemaCombinationApproach The way in which multiple user-provided JSON schemas are to be combined.
     * @param domainConfig The current domain configuration.
     * @param localiser The helper class for localisations.
     * @param locationAsPointer True if the location for error messages should be a JSON pointer.
     */
    public JSONValidator(File inputFileToValidate, String validationType, List<FileInfo> externalSchemas, ValidationArtifactCombinationApproach externalSchemaCombinationApproach, DomainConfig domainConfig, LocalisationHelper localiser, boolean locationAsPointer) {
        this(inputFileToValidate, validationType, externalSchemas, externalSchemaCombinationApproach, domainConfig, localiser, locationAsPointer, true);
    }

    /**
     * Constructor.
     *
     * @param inputFileToValidate The file that contains the JSON content to validate.
     * @param validationType The validation type to consider (can be null if there is only one).
     * @param externalSchemas The list of external (user-provided) JSON schemas.
     * @param externalSchemaCombinationApproach The way in which multiple user-provided JSON schemas are to be combined.
     * @param domainConfig The current domain configuration.
     * @param locationAsPointer True if the location for error messages should be a JSON pointer.
     * @param localiser The helper class for localisations.
     * @param addInputToReport True if the provided input should be added as context to the produced TAR report.
     */
    public JSONValidator(File inputFileToValidate, String validationType, List<FileInfo> externalSchemas, ValidationArtifactCombinationApproach externalSchemaCombinationApproach, DomainConfig domainConfig, LocalisationHelper localiser, boolean locationAsPointer, boolean addInputToReport) {
        this.inputFileToValidate = inputFileToValidate;
        this.validationType = validationType;
        this.domainConfig = domainConfig;
        this.externalSchemaFileInfo = externalSchemas;
        this.externalSchemaCombinationApproach = externalSchemaCombinationApproach;
        this.locationAsPointer = locationAsPointer;
        this.addInputToReport = addInputToReport;
        this.localiser = localiser;
        if (validationType == null) {
            this.validationType = domainConfig.getType().get(0);
        }
    }

    /**
     * Get the identifier of the current domain (its folder name).
     *
     * @return The identifier.
     */
    public String getDomain() {
        return this.domainConfig.getDomain();
    }

    /**
     * Get the selected validation type.
     *
     * @return The validation type.
     */
    public String getValidationType() { 
        return this.validationType;
    }

    /**
     * Run the validation and produce the report.
     *
     * @return The validation TAR report.
     */
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

    /**
     * Validate the JSON content against any configured plugins and return their aggregated validation report.
     *
     * @return The plugin validation report.
     */
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

    /**
     * Prepare the inputs expected by custom plugins.
     *
     * @param pluginTmpFolder A temporary folder to use to store plugin inputs.
     * @return The request instance to pass to the plugins.
     */
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
        request.getInput().add(Utils.createInputItem("locale", localiser.getLocale().toString()));
        return request;
    }

    /**
     * Pretty-print the JSON content to ensure meaningful location information.
     *
     * @param input The input to process.
     * @return The pretty-printed result.
     */
    private File prettyPrint(File input) {
        try (FileReader in = new FileReader(input)) {
            JsonElement json = com.google.gson.JsonParser.parseReader(in);
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            String jsonOutput = gson.toJson(json);
            File output = new File(input.getParent(), input.getName() + ".pretty");
            FileUtils.writeStringToFile(output, jsonOutput, StandardCharsets.UTF_8);
            return output;
        } catch (JsonSyntaxException e) {
            throw new ValidatorException("validator.label.exception.providedInputNotJSON", e);
        } catch (IOException e) {
            throw new ValidatorException("validator.label.exception.failedToParseJSON", e);
        }
    }

    /**
     * Run the validation against a set of JSON schemas and obtain the produced error messages.
     *
     * @param schemaFileInfos The JSON schemas to use.
     * @param combinationApproach The way in which these should be combined (if multiple).
     * @return The list of error messages.
     */
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
                aggregatedErrorMessages.add(localiser.localise("validator.label.exception.onlyOneSchemaShouldBeValid", successCount));
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

    /**
     * Add errors relevant to a specific validation branch.
     *
     * @param aggregatedErrorMessages The errors for all branches (used to collect new errors).
     * @param branchMessages The messages linked to the specific branch.
     * @param branchCounter The counter of the current branch.
     */
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

    /**
     * Validate the JSON content against one JSON schema.
     *
     * @param schemaFile The schema file to use.
     * @return The resulting error messages.
     */
    private List<String> validateAgainstSchema(File schemaFile) {
        JsonSchema schema;
        try {
            schema = jsonValidationService.readSchema(schemaFile.toPath());
        } catch (JsonParsingException e) {
            throw new ValidatorException("validator.label.exception.failedToParseJSONSchema", e, e.getMessage());
        }
        List<String> errorMessages = new ArrayList<>();
        ProblemHandler handler = jsonValidationService.createProblemPrinterBuilder(errorMessages::add)
                .withLocation(true)
                .withLocale(localiser.getLocale())
                .build();
        try (JsonParser parser = jsonValidationService.createParser(inputFileToValidate.toPath(), schema, handler)) {
            while (parser.hasNext()) {
                parser.next();
            }
        }
        return errorMessages;
    }

    /**
     * Run the internal steps needed for the validation.
     *
     * @return The resulting validation report.
     */
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
        if (addInputToReport) {
            report.setContext(new AnyContent());
            report.getContext().setType("map");
            AnyContent inputReportContent = new AnyContent();
            inputReportContent.setType("string");
            inputReportContent.setEmbeddingMethod(ValueEmbeddingEnumeration.STRING);
            inputReportContent.setName(ValidationConstants.INPUT_CONTENT);
            inputReportContent.setMimeType("application/json");
            try {
                inputReportContent.setValue(FileUtils.readFileToString(inputFileToValidate, StandardCharsets.UTF_8));
            } catch (IOException e) {
                throw new IllegalStateException("Unable to generate output report", e);
            }
            report.getContext().getItem().add(inputReportContent);
        }
        return report;
    }

    /**
     * Create a TAR validation report from a list of internal error message texts.
     *
     * @param errorMessages The error messages to process.
     * @return The corresponding report.
     */
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