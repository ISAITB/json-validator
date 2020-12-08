package eu.europa.ec.itb.json.standalone;

import java.io.File;

public class ValidationInput {

    private File inputFile;
    private String fileName;

    public ValidationInput(File inputFile, String fileName) {
        this.inputFile = inputFile;
        this.fileName = fileName;
    }

    public File getInputFile() {
        return inputFile;
    }

    public String getFileName() {
        return fileName;
    }
}
