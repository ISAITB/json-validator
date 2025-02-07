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
