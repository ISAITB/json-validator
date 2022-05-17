package eu.europa.ec.itb.json.validation;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonSyntaxException;
import com.google.gson.stream.JsonWriter;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.JsonPathException;
import eu.europa.ec.itb.json.DomainConfig;
import eu.europa.ec.itb.validation.commons.FileInfo;
import eu.europa.ec.itb.validation.commons.LocalisationHelper;
import eu.europa.ec.itb.validation.commons.artifact.ValidationArtifactCombinationApproach;
import eu.europa.ec.itb.validation.commons.error.ValidatorException;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.UUID;

/**
 * Class used to wrap the specifications with which to carry out a validation.
 */
public class ValidationSpecs {

    private File input;
    private File inputToUse;
    private LocalisationHelper localisationHelper;
    private DomainConfig domainConfig;
    private String validationType;
    private List<FileInfo> externalSchemas;
    private ValidationArtifactCombinationApproach externalSchemaCombinationApproach;
    private boolean locationAsPointer;
    private boolean addInputToReport;
    private boolean produceAggregateReport;

    /**
     * Private constructor to prevent direct initialisation.
     */
    private ValidationSpecs() {}

    /**
     * @return The pretty-printed JSON content to validate.
     */
    public File getInput() {
        return input;
    }

    /**
     * @return The preprocessed input.
     */
    public File getInputFileToUse() {
        if (inputToUse == null) {
            var expression = domainConfig.getInputPreprocessorPerType().get(validationType);
            if (expression == null) {
                // No preprocessing needed.
                inputToUse = prettyPrint(input);
            } else {
                inputToUse = new File(input.getParent(), UUID.randomUUID().toString() + ".json");
                // A preprocessing JSONPath expression has been provided for the given validation type.
                try (InputStream inputStream = new FileInputStream(input)) {
                    Object preprocessedJsonObject = JsonPath.parse(inputStream).read(expression);
                    Gson gson = new Gson();
                    try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(inputToUse.getAbsolutePath()), "UTF-8"))) {
                        gson.toJson(preprocessedJsonObject, writer);
                        writer.flush();
                        inputToUse = prettyPrint(inputToUse);
                    }
                } catch (JsonPathException e) {
                    throw new ValidatorException("validator.label.exception.jsonPathError", e, expression);
                } catch (IOException e) {
                    throw new ValidatorException("validator.label.exception.errorInputForPreprocessing", e);
                }
            }
        }
        return inputToUse;
    }

    /**
     * @return Helper class to facilitate translation lookups.
     */
    public LocalisationHelper getLocalisationHelper() {
        return localisationHelper;
    }

    /**
     * @return The current domain configuration.
     */
    public DomainConfig getDomainConfig() {
        return domainConfig;
    }

    /**
     * @return The requested validation type.
     */
    public String getValidationType() {
        if (validationType == null) {
            validationType = domainConfig.getType().get(0);
        }
        return validationType;
    }

    /**
     * @return The user-provided schemas to consider.
     */
    public List<FileInfo> getExternalSchemas() {
        return externalSchemas;
    }

    /**
     * @return The way in which multiple user-provided JSON schemas are to be combined.
     */
    public ValidationArtifactCombinationApproach getExternalSchemaCombinationApproach() {
        return externalSchemaCombinationApproach;
    }

    /**
     * @return True if the location for error messages should be a JSON pointer.
     */
    public boolean isLocationAsPointer() {
        return locationAsPointer;
    }

    /**
     * @return True if the provided input should be added as context to the produced TAR report.
     */
    public boolean isAddInputToReport() {
        return addInputToReport;
    }

    /**
     * @return True if an aggregated validation report should also be produced.
     */
    public boolean isProduceAggregateReport() {
        return produceAggregateReport;
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
     * Build the validation specifications.
     *
     * @param input The JSON content to validate.
     * @param localisationHelper Helper class to facilitate translation lookups.
     * @param domainConfig The current domain configuration.
     * @return The specification builder.
     */
    public static Builder builder(File input, LocalisationHelper localisationHelper, DomainConfig domainConfig) {
        return new Builder(input, localisationHelper, domainConfig);
    }

    /**
     * Builder class used to incrementally create a specification instance.
     */
    public static class Builder {

        private final ValidationSpecs instance;

        /**
         * Constructor.
         *
         * @param input The JSON content to validate.
         * @param localisationHelper Helper class to facilitate translation lookups.
         * @param domainConfig The current domain configuration.
         */
        Builder(File input, LocalisationHelper localisationHelper, DomainConfig domainConfig) {
            instance = new ValidationSpecs();
            instance.input = input;
            instance.localisationHelper = localisationHelper;
            instance.domainConfig = domainConfig;
        }

        /**
         * @return The specification instance to use.
         */
        public ValidationSpecs build() {
            return instance;
        }

        /**
         * @param validationType Set the validation type to consider.
         * @return The builder.
         */
        public Builder withValidationType(String validationType) {
            instance.validationType = validationType;
            return this;
        }

        /**
         * @param externalSchemas Set the user-provided schemas to consider.
         * @return The builder.
         */
        public Builder withExternalSchemas(List<FileInfo> externalSchemas, ValidationArtifactCombinationApproach externalSchemaCombinationApproach) {
            instance.externalSchemas = externalSchemas;
            instance.externalSchemaCombinationApproach = externalSchemaCombinationApproach;
            return this;
        }

        /**
         * Set the report items' location as JSON pointer expressions.
         *
         * @return The builder.
         */
        public Builder locationAsPointer() {
            instance.locationAsPointer = true;
            return this;
        }

        /**
         * Add the validated input content to the detailed TAR report's context.
         *
         * @return The builder.
         */
        public Builder addInputToReport() {
            instance.addInputToReport = true;
            return this;
        }

        /**
         * Generate also the aggregate report.
         *
         * @return The builder.
         */
        public Builder produceAggregateReport() {
            instance.produceAggregateReport = true;
            return this;
        }
    }

}