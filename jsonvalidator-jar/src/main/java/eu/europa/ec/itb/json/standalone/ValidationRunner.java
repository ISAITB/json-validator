package eu.europa.ec.itb.json.standalone;

import eu.europa.ec.itb.json.DomainConfig;
import eu.europa.ec.itb.json.InputHelper;
import eu.europa.ec.itb.json.validation.FileManager;
import eu.europa.ec.itb.json.validation.JSONValidator;
import eu.europa.ec.itb.json.validation.ValidationSpecs;
import eu.europa.ec.itb.validation.commons.CsvReportGenerator;
import eu.europa.ec.itb.validation.commons.FileInfo;
import eu.europa.ec.itb.validation.commons.LocalisationHelper;
import eu.europa.ec.itb.validation.commons.Utils;
import eu.europa.ec.itb.validation.commons.artifact.ValidationArtifactCombinationApproach;
import eu.europa.ec.itb.validation.commons.error.ValidatorException;
import eu.europa.ec.itb.validation.commons.jar.BaseValidationRunner;
import eu.europa.ec.itb.validation.commons.jar.FileReport;
import eu.europa.ec.itb.validation.commons.jar.ValidationInput;
import eu.europa.ec.itb.validation.commons.report.ReportGeneratorBean;
import org.apache.commons.lang3.LocaleUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Component that handles the actual triggering of validation and resulting reporting.
 */
@Component
@Scope("prototype")
public class ValidationRunner extends BaseValidationRunner<DomainConfig> {

    private static final String FLAG_NO_REPORTS = "-noreports";
    private static final String FLAG_VALIDATION_TYPE = "-type";
    private static final String FLAG_INPUT = "-input";
    private static final String FLAG_SCHEMA = "-schema";
    private static final String FLAG_SCHEMA_COMBINATION = "-combination";
    private static final String FLAG_LOCALE = "-locale";
    private static final ValidationArtifactCombinationApproach DEFAULT_COMBINATION_APPROACH = ValidationArtifactCombinationApproach.ALL;

    @Autowired
    private ApplicationContext ctx;
    @Autowired
    private FileManager fileManager;
    @Autowired
    private ReportGeneratorBean reportGenerator;
    @Autowired
    private CsvReportGenerator csvReportGenerator;
    @Autowired
    private InputHelper inputHelper;

    /**
     * Run the validation.
     *
     * @param args The command-line arguments.
     * @param parentFolder The temporary folder to use for this validator's run.
     */
    @Override
    protected void bootstrapInternal(String[] args, File parentFolder) {
        // Process input arguments
        boolean typeRequired = domainConfig.hasMultipleValidationTypes() && domainConfig.getDefaultType() == null;
        List<ValidationInput> inputs = new ArrayList<>();
        List<FileInfo> externalSchemaFileInfo = new ArrayList<>();
        boolean noReports = false;
        String validationType = null;
        String locale = null;
        String schemaCombinationApproachValue = null;
        ValidationArtifactCombinationApproach externalSchemaCombinationApproach = null;
        try {
            int i = 0;
            while (i < args.length) {
                if (FLAG_NO_REPORTS.equalsIgnoreCase(args[i])) {
                    noReports = true;
                } else if (FLAG_VALIDATION_TYPE.equalsIgnoreCase(args[i])) {
                    validationType = argumentAsString(args, i);
                } else if (FLAG_INPUT.equalsIgnoreCase(args[i])) {
                    if (args.length > i+1) {
                        String path = args[++i];
                        inputs.add(new ValidationInput(getContent(path, parentFolder, domainConfig.getHttpVersion()), path));
                    }
                } else if (FLAG_SCHEMA.equalsIgnoreCase(args[i])) {
                    if (args.length > i + 1) {
                        externalSchemaFileInfo.add(new FileInfo(getContent(args[++i], parentFolder, domainConfig.getHttpVersion())));
                    }
                } else if (FLAG_SCHEMA_COMBINATION.equalsIgnoreCase(args[i])) {
                    if (args.length > i + 1) {
                        schemaCombinationApproachValue = args[++i];
                    }
                } else if (FLAG_LOCALE.equalsIgnoreCase(args[i]) && args.length > i+1) {
                    locale = args[++i];
                }
                i++;
            }
            validationType = inputHelper.validateValidationType(domainConfig, validationType);
            externalSchemaCombinationApproach = inputHelper.getValidationArtifactCombinationApproach(domainConfig, validationType, schemaCombinationApproachValue);
        } catch (ValidatorException e) {
            LOGGER_FEEDBACK.info("\nInvalid arguments provided: {}\n", e.getMessageForDisplay(new LocalisationHelper(domainConfig, Locale.ENGLISH)));
            LOGGER.error(String.format("Invalid arguments provided: %s", e.getMessageForLog()), e);
            inputs.clear();
        } catch (IllegalArgumentException e) {
            LOGGER_FEEDBACK.info("\nInvalid arguments provided: {}\n", e.getMessage());
            LOGGER.error(String.format("Invalid arguments provided: %s", e.getMessage()), e);
            inputs.clear();
        } catch (Exception e) {
            LOGGER_FEEDBACK.info("\nAn error occurred while processing the provided arguments.\n");
            LOGGER.error("An error occurred while processing the provided arguments.", e);
            inputs.clear();
        }
        if (inputs.isEmpty()) {
            printUsage(typeRequired);
        } else {
            // Do validation
            StringBuilder summary = new StringBuilder();
            summary.append("\n");
            int i = 0;
            var localiser = new LocalisationHelper(domainConfig, Utils.getSupportedLocale(LocaleUtils.toLocale(locale), domainConfig));
            for (ValidationInput input: inputs) {
                LOGGER_FEEDBACK.info("\nValidating {} of {} ...", i+1, inputs.size());
                try {
                    JSONValidator validator = ctx.getBean(JSONValidator.class, ValidationSpecs.builder(input.getInputFile(), localiser, domainConfig)
                            .withValidationType(validationType)
                            .withExternalSchemas(externalSchemaFileInfo, externalSchemaCombinationApproach)
                            .build());
                    var report = validator.validate().getDetailedReport();
                    if (report == null) {
                        summary.append("\nNo validation report was produced.\n");
                    } else {
                        int itemCount = 0;
                        if (report.getReports() != null && report.getReports().getInfoOrWarningOrError() != null) {
                            itemCount = report.getReports().getInfoOrWarningOrError().size();
                        }
                        FileReport reportData = new FileReport(input.getFileName(), report, typeRequired, validationType);
                        summary.append("\n").append(reportData).append("\n");
                        if (!noReports) {
                            File xmlReportFile = new File(Path.of(System.getProperty("user.dir")).toFile(), "report."+i+".xml");
                            Files.deleteIfExists(xmlReportFile.toPath());
                            // Create XML report
                            fileManager.saveReport(report, xmlReportFile, domainConfig);
                            if (itemCount <= domainConfig.getMaximumReportsForDetailedOutput()) {
                                // Create PDF and CSV reports
                                File pdfReportFile = new File(xmlReportFile.getParentFile(), "report."+i+".pdf");
                                File csvReportFile = new File(xmlReportFile.getParentFile(), "report."+i+".csv");
                                Files.deleteIfExists(pdfReportFile.toPath());
                                Files.deleteIfExists(csvReportFile.toPath());
                                reportGenerator.writeReport(xmlReportFile, pdfReportFile, localiser, domainConfig.isRichTextReports());
                                csvReportGenerator.writeReport(xmlReportFile, csvReportFile, localiser, domainConfig);
                                summary.append("- Detailed reports in [").append(xmlReportFile.getAbsolutePath()).append("], [").append(pdfReportFile.getAbsolutePath()).append("] and [").append(csvReportFile.getAbsolutePath()).append("]\n");
                            } else if (report.getCounters() != null && (report.getCounters().getNrOfAssertions().longValue() + report.getCounters().getNrOfErrors().longValue() + report.getCounters().getNrOfWarnings().longValue()) <= domainConfig.getMaximumReportsForXmlOutput()) {
                                summary.append("- Detailed report in [").append(xmlReportFile.getAbsolutePath()).append("] (PDF and CSV reports skipped due to large number of report items) \n");
                            } else {
                                summary.append("- Detailed report in [").append(xmlReportFile.getAbsolutePath()).append("] (report limited to first ").append(domainConfig.getMaximumReportsForXmlOutput()).append(" items, and skipped PDF and CSV reports) \n");
                            }
                        }
                    }
                } catch (ValidatorException e) {
                    LOGGER_FEEDBACK.info("\nAn error occurred while executing the validation: {}", e.getMessageForDisplay(localiser));
                    LOGGER.error(String.format("An error occurred while executing the validation: %s", e.getMessageForLog()), e);
                    break;

                } catch (Exception e) {
                    LOGGER_FEEDBACK.info("\nAn error occurred while executing the validation.");
                    LOGGER.error(String.format("An error occurred while executing the validation: %s", e.getMessage()), e);
                    break;
                }
                i++;
                LOGGER_FEEDBACK.info(" Done.");
            }
            var summaryString = summary.toString();
            LOGGER_FEEDBACK.info(summaryString);
            LOGGER_FEEDBACK_FILE.info(summaryString);
        }
    }

    /**
     * Get the JSON content to validate based on the provided path (can be a URL or file reference).
     *
     * @param contentPath The path to process.
     * @param parentFolder The validation run's temporary folder.
     * @param httpVersion The HTTP version to use.
     * @return The file with the JSON content to use for the validation.
     * @throws IOException If an IO error occurs.
     */
    private File getContent(String contentPath, File parentFolder, HttpClient.Version httpVersion) throws IOException {
        File fileToUse;
        if (isValidURL(contentPath)) {
            // Value is a URL.
            try {
                fileToUse = fileManager.getFileFromURL(parentFolder, contentPath, httpVersion);
            } catch (IOException e) {
                throw new IllegalArgumentException("Unable to read file from URL ["+contentPath+"]");
            }
        } else {
            // Value is a local file. Copy this in the tmp folder as we may later be changing it (e.g. encoding updates).
            Path inputFile = Paths.get(contentPath);
            if (!Files.exists(inputFile) || !Files.isRegularFile(inputFile) || !Files.isReadable(inputFile)) {
                throw new IllegalArgumentException("Unable to read file ["+contentPath+"]");
            }
            Path finalInputFile = Paths.get(parentFolder.getAbsolutePath(), inputFile.getFileName().toString());
            Files.createDirectories(finalInputFile.getParent());
            fileToUse = Files.copy(inputFile, finalInputFile).toFile();
        }
        return fileToUse;
    }

    /**
     * Print the usage string for the validator.
     *
     * @param requireType True if the validation type should be included in the message.
     */
    private void printUsage(boolean requireType) {
        StringBuilder usageMessage = new StringBuilder();
        StringBuilder parametersMessage = new StringBuilder();
        usageMessage.append("\nExpected usage: java -jar validator.jar ").append(FLAG_INPUT).append(" FILE_OR_URI_1 ... [").append(FLAG_INPUT).append(" FILE_OR_URI_N] [").append(FLAG_NO_REPORTS).append("] [").append(FLAG_LOCALE).append(" LOCALE]");
        if (requireType) {
            usageMessage.append(" [").append(FLAG_VALIDATION_TYPE).append(" VALIDATION_TYPE]");
            parametersMessage.append("\n").append(PAD).append(PAD).append("- VALIDATION_TYPE is the type of validation to perform, one of [").append(String.join("|", domainConfig.getType())).append("].");
        } else if (domainConfig.hasMultipleValidationTypes()) {
            usageMessage.append(" [").append(FLAG_VALIDATION_TYPE).append(" VALIDATION_TYPE]");
            parametersMessage.append("\n").append(PAD).append(PAD).append("- VALIDATION_TYPE is the type of validation to perform, one of [").append(String.join("|", domainConfig.getType())).append("] (default is ").append(domainConfig.getDefaultType()).append(").");
        }
        if (domainConfig.definesTypeWithExternalSchemas()) {
            usageMessage.append(" [").append(FLAG_SCHEMA).append(" SCHEMA_FILE_OR_URI_1] ... [").append(FLAG_SCHEMA).append("SCHEMA_FILE_OR_URI_N] [").append(FLAG_SCHEMA_COMBINATION).append(" SCHEMA_COMBINATION]");
            parametersMessage
                    .append("\n").append(PAD).append(PAD).append("- SCHEMA_FILE_OR_URI_X is the full file path or URI to a schema for the validation.")
                    .append("\n").append(PAD).append(PAD).append("- SCHEMA_COMBINATION defines how the provided schemas will be combined, one of [")
                        .append(String.join("|", new String[] {ValidationArtifactCombinationApproach.ALL.getName(), ValidationArtifactCombinationApproach.ANY.getName(), ValidationArtifactCombinationApproach.ONE_OF.getName()}))
                        .append("] (default is ").append(DEFAULT_COMBINATION_APPROACH.getName()).append(").");
        }
        usageMessage.append("\n").append(PAD).append("Where:");
        usageMessage.append("\n").append(PAD).append(PAD).append("- FILE_OR_URI_X is the full file path or URI to the content to validate.");
        usageMessage.append("\n").append(PAD).append(PAD).append("- LOCALE is the language code to consider for reporting of results. If the provided locale is not supported by the validator the default locale will be used instead (e.g. 'fr', 'fr_FR').");
        usageMessage.append(parametersMessage);
        usageMessage.append("\n\nThe summary of each validation will be printed and the detailed reports produced in the current directory (as \"report.X.xml\", \"report.X.pdf\" and \"report.X.csv\").");
        System.out.println(usageMessage);
    }

}
