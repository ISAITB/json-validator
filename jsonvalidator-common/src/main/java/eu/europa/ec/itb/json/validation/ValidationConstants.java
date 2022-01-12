package eu.europa.ec.itb.json.validation;

/**
 * Constants used to name inputs.
 */
public class ValidationConstants {

    /** The JSON content to validate. */
    public static String INPUT_CONTENT = "contentToValidate";
    /** Whether location information in errors should be a JSON pointer. */
    public static String INPUT_LOCATION_AS_POINTER = "locationAsPointer";
    /** The explicit content embedding method. */
    public static String INPUT_EMBEDDING_METHOD = "embeddingMethod";
    /** The validation type. */
    public static String INPUT_VALIDATION_TYPE = "validationType";
    /** The user-provided JSON schemas. */
    public static String INPUT_EXTERNAL_SCHEMAS = "externalSchemas";
    /** The schema linked to a user-provided artifact. */
    public static String INPUT_EXTERNAL_SCHEMAS_SCHEMA = "schema";
    /** The combination approach for multiple user-provided artifacts. */
    public static String INPUT_EXTERNAL_SCHEMA_COMBINATION_APPROACH = "externalSchemaCombinationApproach";
    /** Whether the validated content should be added to the TAR report. */
    public static String INPUT_ADD_INPUT_TO_REPORT = "addInputToReport";
    /** The locale string to consider. */
    public static String INPUT_LOCALE = "locale";

}
