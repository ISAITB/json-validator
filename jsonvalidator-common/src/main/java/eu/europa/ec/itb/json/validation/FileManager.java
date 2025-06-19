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

import com.gitb.tr.TAR;
import eu.europa.ec.itb.json.ApplicationConfig;
import eu.europa.ec.itb.validation.commons.BaseFileManager;
import eu.europa.ec.itb.validation.commons.config.DomainConfig;
import org.apache.commons.io.FileUtils;
import org.apache.tika.Tika;
import org.apache.tika.metadata.Metadata;
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
        var metadata = new Metadata();
        metadata.set(Metadata.CONTENT_TYPE, "application/json");
        String type = tika.detect(stream, metadata);
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
     * @param aggregate Whether the report is an aggregate.
     * @return The file name.
     */
    public String getReportFileNamePdf(String uuid, boolean aggregate) {
        return "TAR-"+uuid+(aggregate?"_aggregate":"")+".pdf";
    }

    /**
     * Returns the name of a CSV report file based on the provided identifier.
     *
     * @param uuid The UUID to consider.
     * @param aggregate Whether the report is an aggregate.
     * @return The file name.
     */
    public String getReportFileNameCsv(String uuid, boolean aggregate) {
        return "TAR-"+uuid+(aggregate?"_aggregate":"")+".csv";
    }

    /**
     * Returns the name of an XML report file based on the provided identifier.
     *
     * @param uuid The UUID to consider.
     * @param aggregate Whether the report is an aggregate.
     * @return The file name.
     */
    public String getReportFileNameXml(String uuid, boolean aggregate) {
        return "TAR-"+uuid+(aggregate?"_aggregate":"")+".xml";
    }

    /**
     * Save the provided TAR validation report.
     *
     * @param report The report to serialise.
     * @param xmlID The UUID to use as part of the report's file name.
     * @param domainConfig The current domain's configuration.
     * @param aggregate Whether the report is an aggregate.
     * @param <R> The specific type of domain configuration class.
     */
    @Override
    public <R extends DomainConfig> void saveReport(TAR report, String xmlID, R domainConfig, boolean aggregate) {
        File outputFile = new File(getReportFolder(), getReportFileNameXml(xmlID, aggregate));
        saveReport(report, outputFile, domainConfig);
    }

}
