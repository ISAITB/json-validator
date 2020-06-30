package eu.europa.ec.itb.json.validation;

import com.gitb.tr.TAR;
import eu.europa.ec.itb.json.ApplicationConfig;
import eu.europa.ec.itb.validation.commons.BaseFileManager;
import org.apache.commons.io.FileUtils;
import org.apache.tika.Tika;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Component
public class FileManager extends BaseFileManager<ApplicationConfig> {

	@Override
	public String getFileExtension(String contentType) {
		return "json";
	}

	public boolean checkFileType(InputStream stream) throws IOException {
		Tika tika = new Tika();
		String type = tika.detect(stream);
		return config.getAcceptedMimeTypes().contains(type);
	}

	public String writeJson(String domain, String json) throws IOException {
		UUID fileUUID = UUID.randomUUID();
		String jsonID = domain+"_"+fileUUID.toString();
		File outputFile = new File(getReportFolder(), getInputFileName(jsonID));
		outputFile.getParentFile().mkdirs();
		FileUtils.writeStringToFile(outputFile, json, StandardCharsets.UTF_8);
		return jsonID;
	}

	public String getInputFileName(String uuid) {
		return "ITB-"+uuid+".json";
	}

	public String getReportFileNamePdf(String uuid) {
		return "TAR-"+uuid+".pdf";
	}

	public String getReportFileNameXml(String uuid) {
		return "TAR-"+uuid+".xml";
	}

	public void saveReport(TAR report, String xmlID) {
		File outputFile = new File(getReportFolder(), getReportFileNameXml(xmlID));
		saveReport(report, outputFile);
	}

}
