package eu.europa.ec.itb.json.validation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.gitb.core.AnyContent;
import com.gitb.core.ValueEmbeddingEnumeration;
import com.gitb.tr.*;
import com.gitb.vs.ValidateRequest;
import com.gitb.vs.ValidationResponse;
import com.networknt.schema.*;
import com.networknt.schema.i18n.ResourceBundleMessageSource;
import eu.europa.ec.itb.json.DomainConfig;
import eu.europa.ec.itb.json.validation.location.NodeCoordinateDetector;
import eu.europa.ec.itb.validation.commons.*;
import eu.europa.ec.itb.validation.commons.artifact.ValidationArtifactCombinationApproach;
import eu.europa.ec.itb.validation.commons.config.DomainPluginConfigProvider;
import eu.europa.ec.itb.validation.commons.error.ValidatorException;
import eu.europa.ec.itb.validation.plugin.PluginManager;
import eu.europa.ec.itb.validation.plugin.ValidationPlugin;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Component that carries out the validation of provided JSON content.
 */
@Component
@Scope("prototype")
public class JSONValidator {

    public static final String ITEM_COUNT = "itemCount";
    private static final Logger LOG = LoggerFactory.getLogger(JSONValidator.class);

    @Autowired
    private FileManager fileManager = null;
    @Autowired
    private PluginManager pluginManager = null;
    @Autowired
    private DomainPluginConfigProvider<DomainConfig> pluginConfigProvider = null;
    @Autowired
    private LocalSchemaCache localSchemaCache = null;

    private final ObjectFactory objectFactory = new ObjectFactory();
    private final ValidationSpecs specs;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private JsonNode contentNode;

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
            ensureContextCreated(overallReportDetailed);
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
        if (specs.getDomainConfig().isReportItemCount() && getContentNode() instanceof ArrayNode arrayNode) {
            ensureContextCreated(overallReportDetailed);
            var countItem = new AnyContent();
            countItem.setType("number");
            countItem.setName(ITEM_COUNT);
            countItem.setValue(String.valueOf(arrayNode.size()));
            overallReportDetailed.getContext().getItem().add(countItem);
        }
        specs.getDomainConfig().applyMetadata(overallReportDetailed, getValidationType());
        specs.getDomainConfig().applyMetadata(overallReportAggregated, getValidationType());
        Utils.sanitizeIfNeeded(overallReportDetailed, specs.getDomainConfig());
        Utils.sanitizeIfNeeded(overallReportAggregated, specs.getDomainConfig());
        return new ReportPair(overallReportDetailed, overallReportAggregated);
    }

    /**
     * Ensure the report's context is created.
     *
     * @param report The report.
     */
    private void ensureContextCreated(TAR report) {
        if (report.getContext() == null) {
            report.setContext(new AnyContent());
            report.getContext().setType("map");
        }
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
        File pluginInputFile = new File(pluginTmpFolder, UUID.randomUUID() +".json");
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
        request.getInput().add(Utils.createInputItem("locationAsPointer", String.valueOf(specs.isLocationAsPointer())));
        return request;
    }

    /**
     * Run the validation against a set of JSON schemas and obtain the produced error messages.
     *
     * @param schemaFileInfos The JSON schemas to use.
     * @param combinationApproach The way in which these should be combined (if multiple).
     * @return The list of error messages.
     */
    private List<Message> validateAgainstSetOfSchemas(List<FileInfo> schemaFileInfos, ValidationArtifactCombinationApproach combinationApproach) {
        var aggregatedMessages = new LinkedList<Message>();
        if (combinationApproach == ValidationArtifactCombinationApproach.ALL) {
            // All schema validations must result in success.
            for (FileInfo fileInfo: schemaFileInfos) {
                aggregatedMessages.addAll(validateAgainstSchema(fileInfo.getFile()));
            }
        } else if (combinationApproach == ValidationArtifactCombinationApproach.ONE_OF) {
            // All schemas need to be validated but only one should validate successfully.
            int successCount = 0;
            int branchCounter = 1;
            for (FileInfo fileInfo: schemaFileInfos) {
                var latestErrors = validateAgainstSchema(fileInfo.getFile());
                if (latestErrors.isEmpty()) {
                    successCount += 1;
                } else {
                    addBranchErrors(aggregatedMessages, latestErrors, branchCounter++);
                }
            }
            if (successCount == 0) {
                aggregatedMessages.addFirst(new Message(specs.getLocalisationHelper().localise("validator.label.exception.oneOfSchemasShouldBeValid")));
            } else if (successCount == 1) {
                aggregatedMessages.clear();
            } else if (successCount > 1) {
                aggregatedMessages.clear();
                aggregatedMessages.add(new Message(specs.getLocalisationHelper().localise("validator.label.exception.onlyOneSchemaShouldBeValid", successCount)));
            }
        } else {
            // Any of the schemas should validate successfully.
            int branchCounter = 1;
            for (FileInfo fileInfo: schemaFileInfos) {
                List<Message> latestErrors = validateAgainstSchema(fileInfo.getFile());
                if (latestErrors.isEmpty()) {
                    aggregatedMessages.clear();
                    break;
                } else {
                    addBranchErrors(aggregatedMessages, latestErrors, branchCounter++);
                }
            }
            if (!aggregatedMessages.isEmpty()) {
                aggregatedMessages.addFirst(new Message(specs.getLocalisationHelper().localise("validator.label.exception.anyOfSchemasShouldBeValid")));
            }
        }
        return aggregatedMessages;
    }

    /**
     * Add errors relevant to a specific validation branch.
     *
     * @param aggregatedMessages The errors for all branches (used to collect new errors).
     * @param branchMessages The messages linked to the specific branch.
     * @param branchCounter The counter of the current branch.
     */
    private void addBranchErrors(List<Message> aggregatedMessages, List<Message> branchMessages, int branchCounter) {
        for (var error: branchMessages) {
            error.setDescription("["+branchCounter+"]: "+error.getDescription());
            aggregatedMessages.add(error);
        }
    }

    /**
     * Read the schema defined from the provided path.
     * <p>
     * The schema loading in this case extends what the official spec foresees, allowing to read definitions
     * from local files (and reuse schemas).
     *
     * @param path The schema path.
     * @return The parsed schema.
     */
    private JsonSchema readSchema(Path path) {
        try {
            var jsonNode = objectMapper.readTree(path.toFile());
            var jsonSchemaVersion = JsonSchemaFactory.checkVersion(SpecVersionDetector.detect(jsonNode));
            var metaSchema = jsonSchemaVersion.getInstance();
            /*
             * The schema factory is created per validation. This is done to avoid caching of schemas across validations that
             * may be remotely loaded or schemas that are user-provided. In addition, it allows us to treat schemas that
             * may use different specification versions.
             */
            var schemaFactory = JsonSchemaFactory.builder()
                    .schemaLoaders(schemaLoaders -> schemaLoaders.add(new LocalSchemaResolver(specs.getDomainConfig(), localSchemaCache)))
                    .metaSchema(metaSchema)
                    .defaultMetaSchemaIri(metaSchema.getIri())
                    .build();
            SchemaValidatorsConfig config = new SchemaValidatorsConfig();
            config.setPathType(PathType.JSON_POINTER);
            config.setLocale(specs.getLocalisationHelper().getLocale());
            config.setMessageSource(new ResourceBundleMessageSource("i18n/jsv-messages"));
            config.setLocale(this.specs.getLocalisationHelper().getLocale());
            return schemaFactory.getSchema(jsonNode, config);
        } catch (IOException e) {
            throw new ValidatorException("validator.label.exception.failedToParseJSONSchema", e, e.getMessage());
        }
    }

    /**
     * Validate the JSON content against one JSON schema.
     *
     * @param schemaFile The schema file to use.
     * @return The resulting error messages.
     */
    private List<Message> validateAgainstSchema(File schemaFile) {
        var schema = readSchema(schemaFile.toPath());
        var content = getContentNode();
        return schema.validate(content).stream().map((message) -> new Message(StringUtils.removeStart(message.getMessage(), "[] "), message.getInstanceLocation().toString())).collect(Collectors.toList());
    }

    /**
     * Parse the JSON node for the provided input file.
     *
     * @return The JSON node.
     */
    private JsonNode getContentNode() {
        if (contentNode == null) {
            try {
                contentNode = objectMapper.readTree(specs.getInputFileToUse());
            } catch (IOException e) {
                throw new ValidatorException("validator.label.exception.failedToParseJSON", e);
            }
        }
        return contentNode;
    }

    /**
     * Run the internal steps needed for the validation.
     *
     * @return The resulting validation reports (detailed and aggregate).
     */
    private ReportPair validateInternal() {
        List<Message> aggregatedMessages = new ArrayList<>();
        List<FileInfo> preconfiguredSchemaFiles = fileManager.getPreconfiguredValidationArtifacts(specs.getDomainConfig(), specs.getValidationType());
        if (!preconfiguredSchemaFiles.isEmpty()) {
            var combinationApproach = specs.getDomainConfig().getSchemaInfo(specs.getValidationType()).getArtifactCombinationApproach();
            aggregatedMessages.addAll(validateAgainstSetOfSchemas(preconfiguredSchemaFiles, combinationApproach));
        }
        if (!specs.getExternalSchemas().isEmpty()) {
            // We apply "allOf" semantics when there are both preconfigured and external schemas.
            aggregatedMessages.addAll(validateAgainstSetOfSchemas(specs.getExternalSchemas(), specs.getExternalSchemaCombinationApproach()));
        }
        return createReport(aggregatedMessages);
    }

    /**
     * Create a TAR validation report from a list of internal error message texts.
     *
     * @param messages The error messages to process.
     * @return The corresponding reports (detailed and aggregate).
     */
    private ReportPair createReport(List<Message> messages) {
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
        if (messages == null || messages.isEmpty()) {
            report.setResult(TestResultType.SUCCESS);
            report.getCounters().setNrOfErrors(BigInteger.ZERO);
        } else {
            report.setResult(TestResultType.FAILURE);
            report.getCounters().setNrOfErrors(BigInteger.valueOf(messages.size()));

            Function<String, String> locationSupplier;
            if (specs.isLocationAsPointer()) {
                locationSupplier = (contentPath) -> contentPath;
            } else {
                locationSupplier = new NodeCoordinateDetector(specs.getInputFileToUse());
            }
            for (var message: messages) {
                BAR error = new BAR();
                error.setDescription(message.getDescription());
                error.setLocation(locationSupplier.apply(message.getContentPath()));
                var elementForReport = objectFactory.createTestAssertionGroupReportsTypeError(error);
                report.getReports().getInfoOrWarningOrError().add(elementForReport);
                if (aggregateReportItems != null) {
                    // Aggregate based on severity and message (without location prefix).
                    aggregateReportItems.updateForReportItem(elementForReport, e -> {
                        var description = message.getDescription();
                        if (description != null) {
                            int positionStart = description.indexOf('[');
                            if (positionStart == 0) {
                                int positionEnd = description.indexOf("] ");
                                if (positionEnd > positionStart) {
                                    description = description.substring(positionEnd+2);
                                }
                            }
                        }
                        return String.format("%s|%s", elementForReport.getName().getLocalPart(), description);
                    });
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