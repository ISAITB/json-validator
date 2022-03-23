package eu.europa.ec.itb.json.validation;

import com.gitb.tr.TAR;
import eu.europa.ec.itb.json.ApplicationConfig;
import eu.europa.ec.itb.validation.commons.BaseFileManager;
import eu.europa.ec.itb.validation.commons.config.DomainConfig;
import org.apache.commons.io.FileUtils;
import org.apache.tika.Tika;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Manages file-system operations.
 */
@Component
public class FileManager extends BaseFileManager<ApplicationConfig> {

    /**
     * @see BaseFileManager#getFileExtension(String)
     *
     * @param contentType The content type (ignored).
     * @return Always "json".
     */
    @Override
    public String getFileExtension(String contentType) {
        return "json";
    }

    /**
     * Use byte sampling to check the actual content of the provided stream.
     *
     * @param stream The stream to check.
     * @return True if the detected content type is accepted.
     * @throws IOException If a stream reading error occurs.
     */
    public boolean checkFileType(InputStream stream) throws IOException {
        Tika tika = new Tika();
        String type = tika.detect(stream);
        return config.getAcceptedMimeTypes().contains(type);
    }

    /**
     * Write the provided JSON content to a file.
     *
     * @param domain The current validation domain.
     * @param json The JSON content.
     * @return The UUID used while generating the resulting file name for subsequent referencing.
     * @throws IOException If an error occurs writing the file.
     */
    public String writeJson(String domain, String json) throws IOException {
        UUID fileUUID = UUID.randomUUID();
        String jsonID = domain+"_"+ fileUUID;
        File outputFile = new File(getReportFolder(), getInputFileName(jsonID));
        outputFile.getParentFile().mkdirs();
        FileUtils.writeStringToFile(outputFile, json, StandardCharsets.UTF_8);
        return jsonID;
    }

    /**
     * Returns the name of an input file based on the provided identifier.
     *
     * @param uuid The UUID to consider.
     * @return The file name.
     */
    public String getInputFileName(String uuid) {
        return "ITB-"+uuid+".json";
    }

    /**
     * Returns the name of an PDF report file based on the provided identifier.
     *
     * @param uuid The UUID to consider.
     * @return The file name.
     */
    public String getReportFileNamePdf(String uuid) {
        return "TAR-"+uuid+".pdf";
    }

    /**
     * Returns the name of an XML report file based on the provided identifier.
     *
     * @param uuid The UUID to consider.
     * @return The file name.
     */
    public String getReportFileNameXml(String uuid) {
        return "TAR-"+uuid+".xml";
    }

    /**
     * Save the provided TAR validation report.
     *
     * @param report The report to serialise.
     * @param xmlID The UUID to use as part of the report's file name.
     * @param domainConfig The current domain's configuration.
     * @param <R> The specific type of domain configuration class.
     */
    @Override
    public <R extends DomainConfig> void saveReport(TAR report, String xmlID, R domainConfig) {
        File outputFile = new File(getReportFolder(), getReportFileNameXml(xmlID));
        saveReport(report, outputFile, domainConfig);
    }

}
