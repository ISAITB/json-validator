/*
 * Copyright (C) 2025 European Union
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the European Commission - subsequent
 * versions of the EUPL (the "Licence"); You may not use this work except in compliance with the Licence.
 *
 * You may obtain a copy of the Licence at:
 *
 * https://interoperable-europe.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an
 * "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the Licence for
 * the specific language governing permissions and limitations under the Licence.
 */

package eu.europa.ec.itb.json.validation;

/**
 * Constants used to name inputs.
 */
public class ValidationConstants {

    /**
     * Constructor to prevent instantiation.
     */
    private ValidationConstants() { throw new IllegalStateException("Utility class"); }

    /** The JSON content to validate. */
    public static final String INPUT_CONTENT = "contentToValidate";
    /** Whether location information in errors should be a JSON pointer. */
    public static final String INPUT_LOCATION_AS_POINTER = "locationAsPointer";
    /** The explicit content embedding method. */
    public static final String INPUT_EMBEDDING_METHOD = "embeddingMethod";
    /** The validation type. */
    public static final String INPUT_VALIDATION_TYPE = "validationType";
    /** The user-provided JSON schemas. */
    public static final String INPUT_EXTERNAL_SCHEMAS = "externalSchemas";
    /** The schema linked to a user-provided artifact. */
    public static final String INPUT_EXTERNAL_SCHEMAS_SCHEMA = "schema";
    /** The combination approach for multiple user-provided artifacts. */
    public static final String INPUT_EXTERNAL_SCHEMA_COMBINATION_APPROACH = "externalSchemaCombinationApproach";
    /** Whether the validated content should be added to the TAR report. */
    public static final String INPUT_ADD_INPUT_TO_REPORT = "addInputToReport";
    /** The locale string to consider. */
    public static final String INPUT_LOCALE = "locale";

}
