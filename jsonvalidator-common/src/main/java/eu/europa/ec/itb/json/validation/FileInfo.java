package eu.europa.ec.itb.json.validation;

import java.io.File;

public class FileInfo {

    private File file;

    public FileInfo(File file) {
        this.file = file;
    }

    public File getFile() {
        return file;
    }

}
