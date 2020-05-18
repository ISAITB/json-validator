package eu.europa.ec.itb.json.web;

/**
 * Created by simatosc on 09/08/2016.
 */
public class ValidationType {

    private String key;
    private String label;

    public ValidationType(String key, String label) {
        this.key = key;
        this.label = label;
    }

    public String getKey() {
        return key;
    }

    public String getLabel() {
        return label;
    }
}
