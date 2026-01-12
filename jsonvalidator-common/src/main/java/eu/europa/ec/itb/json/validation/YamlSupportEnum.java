/*
 * Copyright (C) 2026 European Union
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

public enum YamlSupportEnum {

    /** No support (only JSON is supported). */
    NONE("none"),
    /** Forced (only YAML is supported). */
    FORCE("force"),
    /** Supported (both YAML and JSON are supported). */
    SUPPORT("support");

    private final String value;

    /**
     * Constructor.
     *
     * @param value The enum's underlying value.
     */
    YamlSupportEnum(String value) {
        this.value = value;
    }

    /**
     * Get the enum type that corresponds to the provided value.
     *
     * @param value The value to process.
     * @return The resulting enum.
     * @throws IllegalArgumentException If the provided value is unknown.
     */
    public static YamlSupportEnum fromValue(String value) {
        if (NONE.value.equals(value)) {
            return NONE;
        } else if (FORCE.value.equals(value)) {
            return FORCE;
        } else if (SUPPORT.value.equals(value)) {
            return SUPPORT;
        }
        throw new IllegalArgumentException("Unknown YAML support type ["+value+"]");
    }

}
