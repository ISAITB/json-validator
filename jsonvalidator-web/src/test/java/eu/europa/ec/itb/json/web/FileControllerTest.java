package eu.europa.ec.itb.json.web;

import eu.europa.ec.itb.json.validation.FileManager;
import eu.europa.ec.itb.validation.commons.web.BaseFileController;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FileControllerTest {

    private FileController createFileController() throws Exception {
        var fileController = new FileController();
        var fileManagerField = BaseFileController.class.getDeclaredField("fileManager");
        fileManagerField.setAccessible(true);
        fileManagerField.set(fileController, new FileManager());
        return fileController;
    }

    @Test
    void testGetInputFileName() throws Exception {
        var result = createFileController().getInputFileName("UUID");
        assertEquals("ITB-UUID.json", result);
    }

}
