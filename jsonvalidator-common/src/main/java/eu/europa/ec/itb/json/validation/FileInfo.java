package eu.europa.ec.itb.json.validation;

import java.io.File;

public class FileInfo {

    private File file;

    FileInfo(File file) {
        this.file = file;
    }

    File getFile() {
        return file;
    }

}
