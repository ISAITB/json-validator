package eu.europa.ec.itb.json.web;

import com.gitb.tr.TAR;
import eu.europa.ec.itb.json.ApplicationConfig;
import eu.europa.ec.itb.json.DomainConfig;
import eu.europa.ec.itb.json.DomainConfigCache;
import eu.europa.ec.itb.json.ValidatorChannel;
import eu.europa.ec.itb.json.errors.ValidatorException;
import eu.europa.ec.itb.json.validation.FileInfo;
import eu.europa.ec.itb.json.validation.FileManager;
import eu.europa.ec.itb.json.validation.JSONValidator;
import eu.europa.ec.itb.json.validation.SchemaCombinationApproach;
import eu.europa.ec.itb.json.web.errors.NotFoundException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * Created by simatosc on 14/05/2020.
 */
@Controller
public class UploadController {

    private static Logger logger = LoggerFactory.getLogger(UploadController.class);

    public static final String IS_MINIMAL = "isMinimal";

    private static final String contentType_file     	= "fileType" ;
    private static final String contentType_uri     		= "uriType" ;
    private static final String contentType_string     	= "stringType" ;
    
    @Autowired
	private FileManager fileManager = null;

    @Autowired
	private BeanFactory beans = null;

    @Autowired
	private DomainConfigCache domainConfigs = null;

    @Autowired
	private ApplicationConfig appConfig = null;

    @RequestMapping(method = RequestMethod.GET, value = "/{domain}/upload")
    public ModelAndView upload(@PathVariable("domain") String domain, Model model, HttpServletRequest request) {
    	setMinimalUIFlag(request, false);
    	DomainConfig config = domainConfigs.getConfigForDomainName(domain);
        if (config == null || !config.getChannels().contains(ValidatorChannel.FORM)) {
            throw new NotFoundException();
        }
        MDC.put("domain", domain);
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("config", config);
        attributes.put("appConfig", appConfig);
        attributes.put("validationTypes", getValidationTypes(config));
        attributes.put("minimalUI", false);
        attributes.put("contentType", getContentType(config));
        attributes.put("externalSchema", includeExternalArtefacts(config.getExternalSchemas()));
        return new ModelAndView("uploadForm", attributes);
    }

    @RequestMapping(method = RequestMethod.POST, value = "/{domain}/upload")
    public ModelAndView handleUpload(@PathVariable("domain") String domain,
                                     @RequestParam("file") MultipartFile file,
                                     @RequestParam(value = "uri", defaultValue = "") String uri,
                                     @RequestParam(value = "text-editor", defaultValue = "") String string,
                                     @RequestParam(value = "validationType", defaultValue = "") String validationType,
                                     @RequestParam(value = "contentType", defaultValue = "") String contentType,
                                     @RequestParam(value = "contentType-externalSchema", required = false) String[] externalSchemaContentType,
                                     @RequestParam(value = "inputFile-externalSchema", required= false) MultipartFile[] externalSchemaFiles,
                                     @RequestParam(value = "uri-externalSchema", required = false) String[] externalSchemaUri,
									 @RequestParam(value = "combinationType", defaultValue = "") String combinationType,
                                     RedirectAttributes redirectAttributes,
                                     HttpServletRequest request) {
		setMinimalUIFlag(request, false);
        DomainConfig config = domainConfigs.getConfigForDomainName(domain);
        if (config == null || !config.getChannels().contains(ValidatorChannel.FORM)) {
            throw new NotFoundException();
        }
        MDC.put("domain", domain);
        InputStream stream = null;
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("validationTypes", getValidationTypes(config));
        attributes.put("config", config);
        attributes.put("minimalUI", false);
        attributes.put("contentType", getContentType(config));
        attributes.put("externalSchema", includeExternalArtefacts(config.getExternalSchemas()));

        if (StringUtils.isNotBlank(validationType)) {
            attributes.put("validationTypeLabel", config.getTypeLabel().get(validationType));
        }
        attributes.put("appConfig", appConfig);
        try (InputStream fis = getInputStream(contentType, file.getInputStream(), uri, string)) {
        	if (fileManager.checkFileType(fis)) {
                stream = getInputStream(contentType, file.getInputStream(), uri, string);
            } else {
                attributes.put("message", "Provided input is not a JSON document");
            }
        } catch (IOException e) {
            logger.error("Error while reading uploaded file [" + e.getMessage() + "]", e);
            attributes.put("message", "Error in upload [" + e.getMessage() + "]");
        }
        if (StringUtils.isBlank(validationType)) {
            validationType = config.getType().get(0);
        }
        if (config.hasMultipleValidationTypes() && (validationType == null || !config.getType().contains(validationType))) {
            // A validation type is required.
            attributes.put("message", "Provided validation type is not valid");
        }
		File tempFolderForRequest = fileManager.createTemporaryFolderPath();
        try {
            if (stream != null) {
				File contentToValidate = fileManager.getInputStreamFile(tempFolderForRequest, stream, UUID.randomUUID().toString()+".json");
            	List<FileInfo> externalSchemas = new ArrayList<>();
            	boolean proceedToValidate = true;
            	try {
            		externalSchemas = getExternalFiles(externalSchemaContentType, externalSchemaFiles, externalSchemaUri, config.getExternalSchemas(), validationType, tempFolderForRequest);
            	} catch (Exception e) {
                    logger.error("Error while reading uploaded file [" + e.getMessage() + "]", e);
                    attributes.put("message", "Error in upload [" + e.getMessage() + "]");
					proceedToValidate = false;
                }
            	if (proceedToValidate) {
					SchemaCombinationApproach externalSchemaCombinationApproach = getSchemaCombinationApproach(validationType, combinationType, config);
					JSONValidator validator = beans.getBean(JSONValidator.class, contentToValidate, validationType, externalSchemas, externalSchemaCombinationApproach, config, false);
					TAR report = validator.validate();
					attributes.put("report", report);
					attributes.put("date", report.getDate().toString());
					if (contentType.equals(contentType_file)) {
						attributes.put("fileName", file.getOriginalFilename());
					} else if(contentType.equals(contentType_uri)) {
						attributes.put("fileName", uri);
					} else {
						attributes.put("fileName", "-");
					}
					// Cache detailed report.
					try {
						String jsonID = fileManager.writeJson(config.getDomainName(), report.getContext().getItem().get(0).getValue());
						attributes.put("jsonID", jsonID);
						fileManager.saveReport(report, jsonID);
					} catch (IOException e) {
						logger.error("Error generating detailed report [" + e.getMessage() + "]", e);
						attributes.put("message", "Error generating detailed report: " + e.getMessage());
					}
				}
            }
        } catch (Exception e) {
            logger.error("An error occurred during the validation [" + e.getMessage() + "]", e);
            attributes.put("message", "An error occurred during the validation: " + e.getMessage());
        } finally {
        	// Cleanup temporary resources for request.
        	if (tempFolderForRequest.exists()) {
				FileUtils.deleteQuietly(tempFolderForRequest);
			}
		}
        return new ModelAndView("uploadForm", attributes);
    }

	private SchemaCombinationApproach getSchemaCombinationApproach(String validationType, String combinationType, DomainConfig config) {
		SchemaCombinationApproach externalSchemaCombinationApproach = config.getExternalSchemaCombinationApproach().get(validationType);
		if (StringUtils.isNotBlank(combinationType)) {
			try {
				externalSchemaCombinationApproach = SchemaCombinationApproach.valueOf(combinationType);
			} catch (IllegalArgumentException e) {
				throw new ValidatorException("Invalid schema combination approach ["+combinationType+"]");
			}
		}
		return externalSchemaCombinationApproach;
	}

	@RequestMapping(method = RequestMethod.GET, value = "/{domain}/uploadm")
    public ModelAndView uploadm(@PathVariable("domain") String domain, Model model, HttpServletRequest request) {
		setMinimalUIFlag(request, true);

		DomainConfig config = domainConfigs.getConfigForDomainName(domain);
        if (config == null || !config.getChannels().contains(ValidatorChannel.FORM)) {
            throw new NotFoundException();
        }
        
		if(!config.isSupportMinimalUserInterface()) {
			logger.error("Minimal user interface is not supported in this domain [" + domain + "].");
			throw new NotFoundException();
		}
        
        MDC.put("domain", domain);
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("config", config);
        attributes.put("appConfig", appConfig);
        attributes.put("validationTypes", getValidationTypes(config));
        attributes.put("minimalUI", true);
        attributes.put("contentType", getContentType(config));
        attributes.put("externalSchema", includeExternalArtefacts(config.getExternalSchemas()));
        return new ModelAndView("uploadForm", attributes);
    }
    

    @RequestMapping(method = RequestMethod.POST, value = "/{domain}/uploadm")
    public ModelAndView handleUploadM(@PathVariable("domain") String domain,
                                      @RequestParam("file") MultipartFile file,
                                      @RequestParam(value = "uri", defaultValue = "") String uri,
                                      @RequestParam(value = "text-editor", defaultValue = "") String string,
                                      @RequestParam(value = "validationType", defaultValue = "") String validationType,
                                      @RequestParam(value = "contentType", defaultValue = "") String contentType,
                                      @RequestParam(value = "contentType-externalSchema", required = false) String[] externalSchema,
                                      @RequestParam(value = "inputFile-externalSchema", required= false) MultipartFile[] externalSchemaFiles,
                                      @RequestParam(value = "uriToValidate-externalSchema", required = false) String[] externalSchemaUri,
									  @RequestParam(value = "combinationType", defaultValue = "") String combinationType,
                                      RedirectAttributes redirectAttributes,
                                      HttpServletRequest request) {
    	
		setMinimalUIFlag(request, true);
		ModelAndView mv = handleUpload(domain, file, uri, string, validationType, contentType, externalSchema, externalSchemaFiles, externalSchemaUri, combinationType, redirectAttributes, request);
				
		Map<String, Object> attributes = mv.getModel();
        attributes.put("minimalUI", true);

        return new ModelAndView("uploadForm", attributes);
	}
    
    private List<ValidationType> getValidationTypes(DomainConfig config) {
        List<ValidationType> types = new ArrayList<>();
        if (config.hasMultipleValidationTypes()) {
            for (String type: config.getType()) {
                types.add(new ValidationType(type, config.getTypeLabel().get(type)));
            }
        }
        return types;
    }
    
	private List<ValidationType> includeExternalArtefacts(Map<String, String> externalArtefact) {
        List<ValidationType> types = new ArrayList<>();
    	for (Map.Entry<String, String> entry : externalArtefact.entrySet()) {
    		types.add(new ValidationType(entry.getKey(), entry.getValue()));
    	}
    	return types;
    }

	private void setMinimalUIFlag(HttpServletRequest request, boolean isMinimal) {
		if (request.getAttribute(IS_MINIMAL) == null) {
			request.setAttribute(IS_MINIMAL, isMinimal);
		}
	}
    
    private List<ValidationType> getContentType(DomainConfig config){
        List<ValidationType> types = new ArrayList<>();
		types.add(new ValidationType(contentType_file, config.getLabel().getOptionContentFile()));
		types.add(new ValidationType(contentType_uri, config.getLabel().getOptionContentURI()));
		types.add(new ValidationType(contentType_string, config.getLabel().getOptionContentDirectInput()));
		return types;
    }
    
    private List<FileInfo> getExternalFiles(String[] externalContentType, MultipartFile[] externalFiles, String[] externalUri,
                                            Map<String, String> externalProperties, String validationType, File parentFolder) throws Exception {
    	List<FileInfo> lis = new ArrayList<>();
    	if (externalContentType != null) {
	    	for(int i=0; i<externalContentType.length; i++) {
				File inputFile;
				MultipartFile currentExtFile = null;
				String currentExtUri = "";
				
				if(externalFiles!=null && externalFiles.length>i) {
					currentExtFile = externalFiles[i];
				}
				if(externalUri!=null && externalUri.length>i) {
					currentExtUri = externalUri[i];
				}
				
				inputFile = getInputFile(externalContentType[i], currentExtFile, currentExtUri, parentFolder);
				FileInfo fi = new FileInfo(inputFile);
				lis.add(fi);
	    	}
    	}
    	if (validateExternalFiles(lis, externalProperties, validationType)) {
        	return lis;
    	} else {
            logger.error("An error occurred during the validation of the external schema(s).");
    		throw new Exception("An error occurred during the validation of the external Schema(s).");
    	}
    	
    }

    private boolean validateExternalFiles(List<FileInfo> lis, Map<String, String> externalArtefacts, String validationType) {
    	String externalArtefactProperty = externalArtefacts.get(validationType);
		
    	boolean validated = false;
    	
    	switch(externalArtefactProperty) {
    		case DomainConfig.externalFile_req:
    			if(lis!=null && !lis.isEmpty()) {
    				validated = true;
    			}
    			break;
    		case DomainConfig.externalFile_opt:
    			validated = true;
    			break;
    		case DomainConfig.externalFile_none:
    			if(lis==null || lis.isEmpty()) {
    				validated = true;
    			}
    			break;
    	}
    	
		return validated;
	}
    
    private File getInputFile(String contentType, MultipartFile inputFile, String inputUri, File parentFolder) throws IOException {
    	File f = null;
    	switch (contentType) {
			case contentType_file:
				if (inputFile!=null && !inputFile.isEmpty()) {
		        	f = this.fileManager.getInputStreamFile(parentFolder, inputFile.getInputStream(), inputFile.getOriginalFilename());
				}
				break;
			case contentType_uri:					
				if (!inputUri.isEmpty()) {
					f = this.fileManager.getURLFile(parentFolder, inputUri);
				}
				break;
		}
    	
    	return f;
    }
    
	private InputStream getInputStream(String contentType, InputStream inputStream, String uri, String string) {
		InputStream is = null;
		
		switch(contentType) {
			case contentType_file:
		    	is = inputStream;
				break;
			
			case contentType_uri:
				is = this.fileManager.getURIInputStream(uri);
				break;
				
			case contentType_string:
				is = new ByteArrayInputStream(string.getBytes());
				break;
		}

		return is;
	}
    
}
