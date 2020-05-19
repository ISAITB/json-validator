package eu.europa.ec.itb.json;

public enum ValidatorChannel {
	
    FORM("form"),
	SOAP_API("soap_api");

    private String name;

    ValidatorChannel(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static ValidatorChannel byName(String name) {
        if (FORM.getName().equals(name)) {
            return FORM;
        } else if (SOAP_API.getName().equals(name)) {
            return SOAP_API;            
        } else {
            throw new IllegalArgumentException("Unknown validator channel ["+name+"]");
        }
    }
}
