package eu.europa.ec.itb.json.validation;

import com.gitb.core.AnyContent;
import com.gitb.core.ValueEmbeddingEnumeration;
import com.gitb.tr.*;
import com.gitb.vs.ValidateRequest;
import com.gitb.vs.ValidationResponse;
import eu.europa.ec.itb.json.ApplicationConfig;
import eu.europa.ec.itb.json.DomainConfig;
import eu.europa.ec.itb.validation.commons.*;
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

    private final ObjectFactory objectFactory = new ObjectFactory();
    private final ValidationSpecs specs;

    /**
     * Constructor.
     *
     * @param specs The specifications with which to carry out the validation.
     */
    public JSONValidator(ValidationSpecs specs) {
        this.specs = specs;
    }

    /**
     * Get the identifier of the current domain (its folder name).
     *
     * @return The identifier.
     */
    public String getDomain() {
        return specs.getDomainConfig().getDomain();
    }

    /**
     * Get the selected validation type.
     *
     * @return The validation type.
     */
    public String getValidationType() { 
        return specs.getValidationType();
    }

    /**
     * Run the validation and produce the report.
     *
     * @return The validation TAR reports (detailed and aggregated).
     */
    public ReportPair validate() {
        TAR overallReportDetailed;
        TAR overallReportAggregated;
        try {
            fileManager.signalValidationStart(specs.getDomainConfig().getDomainName());
            var coreReports = validateInternal();
            overallReportDetailed = coreReports.getDetailedReport();
            overallReportAggregated = coreReports.getAggregateReport();
        } finally {
            fileManager.signalValidationEnd(specs.getDomainConfig().getDomainName());
        }
        TAR pluginReport = validateAgainstPlugins();
        if (pluginReport != null) {
            overallReportDetailed = Utils.mergeReports(new TAR[] {overallReportDetailed, pluginReport});
            if (specs.isProduceAggregateReport()) {
                overallReportAggregated = Utils.mergeReports(new TAR[] {overallReportAggregated, Utils.toAggregatedTAR(pluginReport, specs.getLocalisationHelper())});
            }
        }
        if (specs.getDomainConfig().isReportsOrdered()) {
            if (overallReportDetailed.getReports() != null) {
                overallReportDetailed.getReports().getInfoOrWarningOrError().sort(new ReportItemComparator());
            }
            if (specs.isProduceAggregateReport() && overallReportAggregated.getReports() != null) {
                overallReportAggregated.getReports().getInfoOrWarningOrError().sort(new ReportItemComparator());
            }
        }
        if (specs.isAddInputToReport()) {
            overallReportDetailed.setContext(new AnyContent());
            overallReportDetailed.getContext().setType("map");
            AnyContent inputReportContent = new AnyContent();
            inputReportContent.setType("string");
            inputReportContent.setEmbeddingMethod(ValueEmbeddingEnumeration.STRING);
            inputReportContent.setName(ValidationConstants.INPUT_CONTENT);
            inputReportContent.setMimeType("application/json");
            try {
                inputReportContent.setValue(FileUtils.readFileToString(specs.getInputFileToUse(), StandardCharsets.UTF_8));
            } catch (IOException e) {
                throw new IllegalStateException("Unable to generate output report", e);
            }
            overallReportDetailed.getContext().getItem().add(inputReportContent);
        }
        return new ReportPair(overallReportDetailed, overallReportAggregated);
    }

    /**
     * Validate the JSON content against any configured plugins and return their aggregated validation report.
     *
     * @return The plugin validation report.
     */
    private TAR validateAgainstPlugins() {
        TAR pluginReport = null;
        ValidationPlugin[] plugins = pluginManager.getPlugins(pluginConfigProvider.getPluginClassifier(specs.getDomainConfig(), specs.getValidationType()));
        if (plugins != null && plugins.length > 0) {
            File pluginTmpFolder = new File(specs.getInput().getParentFile(), UUID.randomUUID().toString());
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
            FileUtils.copyFile(specs.getInputFileToUse(), pluginInputFile);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to copy input file for plugin", e);
        }
        ValidateRequest request = new ValidateRequest();
        request.getInput().add(Utils.createInputItem("contentToValidate", pluginInputFile.getAbsolutePath()));
        request.getInput().add(Utils.createInputItem("domain", specs.getDomainConfig().getDomainName()));
        request.getInput().add(Utils.createInputItem("validationType", specs.getValidationType()));
        request.getInput().add(Utils.createInputItem("tempFolder", pluginTmpFolder.getAbsolutePath()));
        request.getInput().add(Utils.createInputItem("locale", specs.getLocalisationHelper().getLocale().toString()));
        return request;
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
                aggregatedErrorMessages.add(specs.getLocalisationHelper().localise("validator.label.exception.onlyOneSchemaShouldBeValid", successCount));
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
                .withLocale(specs.getLocalisationHelper().getLocale())
                .build();
        try (JsonParser parser = jsonValidationService.createParser(specs.getInputFileToUse().toPath(), schema, handler)) {
            while (parser.hasNext()) {
                parser.next();
            }
        }
        return errorMessages;
    }

    /**
     * Run the internal steps needed for the validation.
     *
     * @return The resulting validation reports (detailed and aggregate).
     */
    private ReportPair validateInternal() {
        List<String> aggregatedErrorMessages = new ArrayList<>();
        List<FileInfo> preconfiguredSchemaFiles = fileManager.getPreconfiguredValidationArtifacts(specs.getDomainConfig(), specs.getValidationType());
        if (!preconfiguredSchemaFiles.isEmpty()) {
            ValidationArtifactCombinationApproach combinationApproach = specs.getDomainConfig().getSchemaInfo(specs.getValidationType()).getArtifactCombinationApproach();
            aggregatedErrorMessages.addAll(validateAgainstSetOfSchemas(preconfiguredSchemaFiles, combinationApproach));
        }
        if (!specs.getExternalSchemas().isEmpty()) {
            // We apply "allOf" semantics when there are both preconfigured and external schemas.
            aggregatedErrorMessages.addAll(validateAgainstSetOfSchemas(specs.getExternalSchemas(), specs.getExternalSchemaCombinationApproach()));
        }
        return createReport(ErrorMessage.processMessages(aggregatedErrorMessages, appConfig.getBranchErrorMessageValues()));
    }

    /**
     * Create a TAR validation report from a list of internal error message texts.
     *
     * @param errorMessages The error messages to process.
     * @return The corresponding reports (detailed and aggregate).
     */
    private ReportPair createReport(List<ErrorMessage> errorMessages) {
        TAR report = new TAR();
        report.setDate(Utils.getXMLGregorianCalendarDateTime());
        report.setCounters(new ValidationCounters());
        report.getCounters().setNrOfWarnings(BigInteger.ZERO);
        report.getCounters().setNrOfAssertions(BigInteger.ZERO);
        report.setReports(new TestAssertionGroupReportsType());
        AggregateReportItems aggregateReportItems = null;
        if (specs.isProduceAggregateReport()) {
            aggregateReportItems = new AggregateReportItems(objectFactory, specs.getLocalisationHelper());
        }
        if (errorMessages == null || errorMessages.isEmpty()) {
            report.setResult(TestResultType.SUCCESS);
            report.getCounters().setNrOfErrors(BigInteger.ZERO);
        } else {
            report.setResult(TestResultType.FAILURE);
            report.getCounters().setNrOfErrors(BigInteger.valueOf(errorMessages.size()));
            for (ErrorMessage errorMessage: errorMessages) {
                BAR error = new BAR();
                error.setDescription(errorMessage.getMessage());
                if (specs.isLocationAsPointer()) {
                    error.setLocation(errorMessage.getLocationPointer());
                } else {
                    error.setLocation(ValidationConstants.INPUT_CONTENT+":"+errorMessage.getLocationLine()+":"+errorMessage.getLocationColumn());
                }
                var elementForReport = objectFactory.createTestAssertionGroupReportsTypeError(error);
                report.getReports().getInfoOrWarningOrError().add(elementForReport);
                if (aggregateReportItems != null) {
                    // Aggregate based on severity and message (without location prefix).
                    aggregateReportItems.updateForReportItem(elementForReport, e -> String.format("%s|%s", elementForReport.getName().getLocalPart(), errorMessage.getMessageWithoutLocation()));
                }
            }
        }
        // Create the aggregate report if needed.
        TAR aggregateReport = null;
        if (aggregateReportItems != null) {
            aggregateReport = new TAR();
            aggregateReport.setContext(new AnyContent());
            aggregateReport.setResult(report.getResult());
            aggregateReport.setCounters(report.getCounters());
            aggregateReport.setDate(report.getDate());
            aggregateReport.setName(report.getName());
            aggregateReport.setReports(new TestAssertionGroupReportsType());
            aggregateReport.getReports().getInfoOrWarningOrError().addAll(aggregateReportItems.getReportItems());
        }
        return new ReportPair(report, aggregateReport);
    }

}