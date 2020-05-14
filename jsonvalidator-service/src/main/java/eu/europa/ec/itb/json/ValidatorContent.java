package eu.europa.ec.itb.json;

import eu.europa.ec.itb.json.errors.ValidatorException;
import eu.europa.ec.itb.json.validation.FileContent;
import eu.europa.ec.itb.json.validation.FileManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;

@Component
public class ValidatorContent {

    @Autowired
	private FileManager fileManager;

    public String validateValidationType(String validationType, DomainConfig domainConfig) {
    	if (validationType != null && !domainConfig.getType().contains(validationType)) {
			throw new ValidatorException(String.format("The provided validation type [%s] is not valid for domain [%s]. Available types are [%s].", validationType, domainConfig.getDomainName(), String.join(", ", domainConfig.getType())));
    	} else if (validationType == null && domainConfig.getType().size() != 1) {
			throw new ValidatorException(String.format("A validation type must be provided for domain [%s]. Available types are [%s].", domainConfig.getDomainName(), String.join(", ", domainConfig.getType())));
		}
    	return validationType==null ? domainConfig.getType().get(0) : validationType;
    }
    
	public File getContentToValidate(String embeddingMethod, String contentToValidate, File parentFolder) {
		File contentFile;
    	// EmbeddingMethod validation
    	if(embeddingMethod!=null) {
    		switch(embeddingMethod) {
    			case FileContent.embedding_URL:
    				try{
    					contentFile = fileManager.getURLFile(parentFolder, contentToValidate);
    				}catch(IOException e) {
						throw new ValidatorException("An error occurred while trying to read the content to validate from the provided URL.", e);
					}
    				break;
    			case FileContent.embedding_BASE64:
    			    contentFile = getBase64File(parentFolder, contentToValidate);
    			    break;
    			case FileContent.embedding_STRING:
    				try {
        				contentFile = fileManager.getStringFile(parentFolder, contentToValidate);
    				} catch (IOException e) {
						throw new ValidatorException("An error occurred while trying to read the content to validate as a string.", e);
					}
    				break;
    			default:
    				throw new ValidatorException(String.format("The provided embedding method [%s] is not supported.", embeddingMethod));
    		}
    	} else {
			try {
				contentFile = fileManager.getFileAsUrlOrBase64(parentFolder, contentToValidate);
			} catch (IOException e) {
				throw new ValidatorException("An error occurred while trying to read the provided content.");
			}
		}
    	return contentFile;
	}
    
    /**
     * From Base64 string to File
     * @param base64Convert Base64 as String
     * @return File
     */
    private File getBase64File(File parentFolder, String base64Convert) {
		try {
			return fileManager.getBase64File(parentFolder, base64Convert);
		} catch (Exception e) {
			throw new ValidatorException("An error occurred while trying to read a file from a BASE64 text", e);
		}
    }
}
