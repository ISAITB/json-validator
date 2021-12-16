package eu.europa.ec.itb.json.web;

import com.gitb.tr.TAR;
import eu.europa.ec.itb.json.ApplicationConfig;
import eu.europa.ec.itb.json.DomainConfig;
import eu.europa.ec.itb.json.DomainConfigCache;
import eu.europa.ec.itb.json.validation.FileManager;
import eu.europa.ec.itb.json.validation.JSONValidator;
import eu.europa.ec.itb.validation.commons.FileInfo;
import eu.europa.ec.itb.validation.commons.LocalisationHelper;
import eu.europa.ec.itb.validation.commons.ValidatorChannel;
import eu.europa.ec.itb.validation.commons.artifact.ExternalArtifactSupport;
import eu.europa.ec.itb.validation.commons.artifact.ValidationArtifactCombinationApproach;
import eu.europa.ec.itb.validation.commons.artifact.ValidationArtifactInfo;
import eu.europa.ec.itb.validation.commons.error.ValidatorException;
import eu.europa.ec.itb.validation.commons.web.errors.NotFoundException;
import eu.europa.ec.itb.validation.commons.web.locale.CustomLocaleResolver;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
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
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * Controller to manage the validator's web user interface.
 */
@Controller
public class UploadController {

    private static final Logger logger = LoggerFactory.getLogger(UploadController.class);
    private static final String IS_MINIMAL = "isMinimal";
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
    @Autowired
    private CustomLocaleResolver localeResolver = null;

    /**
     * Prepare the upload page.
     *
     * @param domain The domain name.
     * @param model The UI model.
     * @param request The received request.
     * @param response The HTTP response.
     * @return The model and view information.
     */
    @RequestMapping(method = RequestMethod.GET, value = "/{domain}/upload")
    public ModelAndView upload(@PathVariable("domain") String domain, Model model, HttpServletRequest request, HttpServletResponse response) {
        setMinimalUIFlag(request, false);
        DomainConfig config = domainConfigs.getConfigForDomainName(domain);
        if (config == null || !config.getChannels().contains(ValidatorChannel.FORM)) {
            throw new NotFoundException();
        }
        MDC.put("domain", domain);
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("config", config);
        attributes.put("appConfig", appConfig);
        attributes.put("minimalUI", false);
        attributes.put("externalArtifactInfo", config.getExternalArtifactInfoMap());
        var localisationHelper = new LocalisationHelper(config, localeResolver.resolveLocale(request, response, config, appConfig));
        attributes.put("localiser", localisationHelper);
        attributes.put("htmlBannerExists", localisationHelper.propertyExists("validator.bannerHtml"));
        return new ModelAndView("uploadForm", attributes);
    }

    /**
     * Handle the upload form's submission.
     *
     * @param domain The domain name.
     * @param file The input file (if provided via file upload).
     * @param uri The input URI (if provided via remote URI).
     * @param string The input content (if provided via editor).
     * @param validationType The validation type.
     * @param contentType The type of the provided content.
     * @param externalSchemaContentType The content type of the user-provided schemas.
     * @param externalSchemaFiles The user-provided schemas (those provided as file uploads).
     * @param externalSchemaUri The user-provided schemas (those provided as URIs).
     * @param combinationType The combination type option in case of multiple schemas.
     * @param redirectAttributes Redirect attributes.
     * @param request The received request.
     * @param response The HTTP response.
     * @return The model and view information.
     */
    @RequestMapping(method = RequestMethod.POST, value = "/{domain}/upload")
    public ModelAndView handleUpload(@PathVariable("domain") String domain,
                                     @RequestParam("file") MultipartFile file,
                                     @RequestParam(value = "uri", defaultValue = "") String uri,
                                     @RequestParam(value = "text-editor", defaultValue = "") String string,
                                     @RequestParam(value = "validationType", defaultValue = "") String validationType,
                                     @RequestParam(value = "contentType", defaultValue = "") String contentType,
                                     @RequestParam(value = "contentType-external_default", required = false) String[] externalSchemaContentType,
                                     @RequestParam(value = "inputFile-external_default", required= false) MultipartFile[] externalSchemaFiles,
                                     @RequestParam(value = "uri-external_default", required = false) String[] externalSchemaUri,
                                     @RequestParam(value = "combinationType", defaultValue = "") String combinationType,
                                     RedirectAttributes redirectAttributes,
                                     HttpServletRequest request,
                                     HttpServletResponse response) {
        setMinimalUIFlag(request, false);
        DomainConfig config = domainConfigs.getConfigForDomainName(domain);
        if (config == null || !config.getChannels().contains(ValidatorChannel.FORM)) {
            throw new NotFoundException();
        }
        var localisationHelper = new LocalisationHelper(config, localeResolver.resolveLocale(request, response, config, appConfig));
        MDC.put("domain", domain);
        InputStream stream = null;
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("config", config);
        attributes.put("minimalUI", false);
        attributes.put("localiser", localisationHelper);
        attributes.put("htmlBannerExists", localisationHelper.propertyExists("validator.bannerHtml"));
        attributes.put("externalArtifactInfo", config.getExternalArtifactInfoMap());

        if (StringUtils.isNotBlank(validationType)) {
            attributes.put("validationTypeLabel", config.getCompleteTypeOptionLabel(validationType, localisationHelper));
        }
        attributes.put("appConfig", appConfig);
        try (InputStream fis = getInputStream(contentType, file.getInputStream(), uri, string)) {
            if (fileManager.checkFileType(fis)) {
                stream = getInputStream(contentType, file.getInputStream(), uri, string);
            } else {
                attributes.put("message", localisationHelper.localise("validator.label.exception.providedInputNotJSON"));
            }
        } catch (IOException e) {
            logger.error("Error while reading uploaded file [" + e.getMessage() + "]", e);
            attributes.put("message", localisationHelper.localise("validator.label.exception.errorInUpload", e.getMessage()));
        }
        if (StringUtils.isBlank(validationType)) {
            validationType = config.getType().get(0);
        }
        if (config.hasMultipleValidationTypes() && (validationType == null || !config.getType().contains(validationType))) {
            // A validation type is required.
            attributes.put("message", localisationHelper.localise("validator.label.exception.providedValidationTypeNotValid"));
        }
        File tempFolderForRequest = fileManager.createTemporaryFolderPath();
        try {
            if (stream != null) {
                File contentToValidate = fileManager.getFileFromInputStream(tempFolderForRequest, stream, null, UUID.randomUUID().toString()+".json");
                List<FileInfo> externalSchemas = new ArrayList<>();
                boolean proceedToValidate = true;
                try {
                    externalSchemas = getExternalFiles(externalSchemaContentType, externalSchemaFiles, externalSchemaUri, config.getSchemaInfo(validationType), validationType, tempFolderForRequest);
                } catch (IOException e) {
                    logger.error("Error while reading uploaded file [" + e.getMessage() + "]", e);
                    attributes.put("message", localisationHelper.localise("validator.label.exception.errorInUpload", e.getMessage()));
                    proceedToValidate = false;
                }
                if (proceedToValidate) {
                    ValidationArtifactCombinationApproach externalSchemaCombinationApproach = getSchemaCombinationApproach(validationType, combinationType, config);
                    JSONValidator validator = beans.getBean(JSONValidator.class, contentToValidate, validationType, externalSchemas, externalSchemaCombinationApproach, config, localisationHelper, false);
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
                        String inputID = fileManager.writeJson(config.getDomainName(), report.getContext().getItem().get(0).getValue());
                        attributes.put("inputID", inputID);
                        fileManager.saveReport(report, inputID, config);
                    } catch (IOException e) {
                        logger.error("Error generating detailed report [" + e.getMessage() + "]", e);
                        attributes.put("message", "Error generating detailed report: " + e.getMessage());
                    }
                }
            }
        } catch (ValidatorException e) {
            logger.error(e.getMessageForLog(), e);
            attributes.put("message", e.getMessageForDisplay(localisationHelper));
        } catch (Exception e) {
            logger.error("An error occurred during the validation [" + e.getMessage() + "]", e);
            if (e.getMessage() != null) {
                attributes.put("message", localisationHelper.localise("validator.label.exception.unexpectedErrorDuringValidationWithParams", e.getMessage()));
            } else {
                attributes.put("message", localisationHelper.localise("validator.label.exception.unexpectedErrorDuringValidation"));
            }
        } finally {
            // Cleanup temporary resources for request.
            if (tempFolderForRequest.exists()) {
                FileUtils.deleteQuietly(tempFolderForRequest);
            }
        }
        return new ModelAndView("uploadForm", attributes);
    }

    /**
     * Validate and get the combination approach for multiple schemas.
     *
     * @param validationType The validation type.
     * @param combinationType The combination type.
     * @param config The domain configuration.
     * @return The approach to use.
     */
    private ValidationArtifactCombinationApproach getSchemaCombinationApproach(String validationType, String combinationType, DomainConfig config) {
        ValidationArtifactCombinationApproach externalSchemaCombinationApproach = config.getSchemaInfo(validationType).getArtifactCombinationApproach();
        if (StringUtils.isNotBlank(combinationType)) {
            try {
                externalSchemaCombinationApproach = ValidationArtifactCombinationApproach.byName(combinationType);
            } catch (IllegalArgumentException e) {
                throw new ValidatorException("validator.label.exception.invalidSchemaCombinationApproach", combinationType);
            }
        }
        return externalSchemaCombinationApproach;
    }

    /**
     * Prepare the upload page (minimal UI version).
     *
     * @param domain The domain name.
     * @param model The UI model.
     * @param request The received request.
     * @param response The HTTP response.
     * @return The model and view information.
     */
    @RequestMapping(method = RequestMethod.GET, value = "/{domain}/uploadm")
    public ModelAndView uploadm(@PathVariable("domain") String domain, Model model, HttpServletRequest request, HttpServletResponse response) {
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
        attributes.put("minimalUI", true);
        attributes.put("externalArtifactInfo", config.getExternalArtifactInfoMap());
        var localisationHelper = new LocalisationHelper(config, localeResolver.resolveLocale(request, response, config, appConfig));
        attributes.put("localiser", localisationHelper);
        attributes.put("htmlBannerExists", localisationHelper.propertyExists("validator.bannerHtml"));
        return new ModelAndView("uploadForm", attributes);
    }


    /**
     * Handle the upload form's submission (minimal UI version).
     *
     * @param domain The domain name.
     * @param file The input file (if provided via file upload).
     * @param uri The input URI (if provided via remote URI).
     * @param string The input content (if provided via editor).
     * @param validationType The validation type.
     * @param contentType The type of the provided content.
     * @param externalSchema The content type of the user-provided schemas.
     * @param externalSchemaFiles The user-provided schemas (those provided as file uploads).
     * @param externalSchemaUri The user-provided schemas (those provided as URIs).
     * @param combinationType The combination type option in case of multiple schemas.
     * @param redirectAttributes Redirect attributes.
     * @param request The received request.
     * @param request The HTTP response.
     * @return The model and view information.
     */
    @RequestMapping(method = RequestMethod.POST, value = "/{domain}/uploadm")
    public ModelAndView handleUploadM(@PathVariable("domain") String domain,
                                      @RequestParam("file") MultipartFile file,
                                      @RequestParam(value = "uri", defaultValue = "") String uri,
                                      @RequestParam(value = "text-editor", defaultValue = "") String string,
                                      @RequestParam(value = "validationType", defaultValue = "") String validationType,
                                      @RequestParam(value = "contentType", defaultValue = "") String contentType,
                                      @RequestParam(value = "contentType-external_default", required = false) String[] externalSchema,
                                      @RequestParam(value = "inputFile-external_default", required= false) MultipartFile[] externalSchemaFiles,
                                      @RequestParam(value = "uriToValidate-external_default", required = false) String[] externalSchemaUri,
                                      @RequestParam(value = "combinationType", defaultValue = "") String combinationType,
                                      RedirectAttributes redirectAttributes,
                                      HttpServletRequest request,
                                      HttpServletResponse response) {

        setMinimalUIFlag(request, true);
        ModelAndView mv = handleUpload(domain, file, uri, string, validationType, contentType, externalSchema, externalSchemaFiles, externalSchemaUri, combinationType, redirectAttributes, request, response);

        Map<String, Object> attributes = mv.getModel();
        attributes.put("minimalUI", true);

        return new ModelAndView("uploadForm", attributes);
    }

    /**
     * Record whether the current request is through a minimal UI.
     *
     * @param request The current request.
     * @param isMinimal True in case of the minimal UI being used.
     */
    private void setMinimalUIFlag(HttpServletRequest request, boolean isMinimal) {
        if (request.getAttribute(IS_MINIMAL) == null) {
            request.setAttribute(IS_MINIMAL, isMinimal);
        }
    }

    /**
     * Validate and get the user-provided schemas.
     *
     * @param externalContentType The directly provided schemas.
     * @param externalFiles The schemas provided as files.
     * @param externalUri The schemas provided as URIs.
     * @param schemaInfo The schema information from the domain.
     * @param validationType The validation type.
     * @param parentFolder The temporary folder to use for file system storage.
     * @return The list of user-provided artifacts.
     * @throws IOException If an IO error occurs.
     */
    private List<FileInfo> getExternalFiles(String[] externalContentType, MultipartFile[] externalFiles, String[] externalUri,
                                            ValidationArtifactInfo schemaInfo, String validationType, File parentFolder) throws IOException {
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
        if (validateExternalFiles(lis, schemaInfo, validationType)) {
            return lis;
        } else {
            logger.error("An error occurred during the validation of the external schema(s).");
            throw new ValidatorException("validator.label.exception.errorDuringValidationExternalSchemas");
        }

    }

    /**
     * Validate the list of user-provided schemas.
     *
     * @param lis The schemas.
     * @param schemaInfo The schema information from the domain configuration.
     * @param validationType The validation type.
     * @return True for correctly provided schemas.
     */
    private boolean validateExternalFiles(List<FileInfo> lis, ValidationArtifactInfo schemaInfo, String validationType) {
        ExternalArtifactSupport externalArtifactSupport = schemaInfo.getExternalArtifactSupport();

        boolean validated = false;

        switch (externalArtifactSupport) {
            case REQUIRED:
                if(lis!=null && !lis.isEmpty()) {
                    validated = true;
                }
                break;
            case OPTIONAL:
                validated = true;
                break;
            case NONE:
                if(lis==null || lis.isEmpty()) {
                    validated = true;
                }
                break;
        }

        return validated;
    }

    /**
     * Get the content to validate as a file.
     *
     * @param contentType The directly provided content.
     * @param inputFile The uploaded content file.
     * @param inputUri The provided URI to load the content from.
     * @param parentFolder The temporary folder to use.
     * @return The input content's file.
     * @throws IOException If an error occurs.
     */
    private File getInputFile(String contentType, MultipartFile inputFile, String inputUri, File parentFolder) throws IOException {
        File f = null;
        switch (contentType) {
            case contentType_file:
                if (inputFile!=null && !inputFile.isEmpty()) {
                    f = this.fileManager.getFileFromInputStream(parentFolder, inputFile.getInputStream(), null, inputFile.getOriginalFilename());
                }
                break;
            case contentType_uri:
                if (!inputUri.isEmpty()) {
                    f = this.fileManager.getFileFromURL(parentFolder, inputUri);
                }
                break;
        }

        return f;
    }

    /**
     * Load a strea from the provided input.
     *
     * @param contentType The type of input provided.
     * @param inputStream The stream.
     * @param uri The URI.
     * @param string The text content
     * @return The stream to read.
     */
    private InputStream getInputStream(String contentType, InputStream inputStream, String uri, String string) {
        InputStream is = null;

        switch(contentType) {
            case contentType_file:
                is = inputStream;
                break;

            case contentType_uri:
                is = this.fileManager.getInputStreamFromURL(uri);
                break;

            case contentType_string:
                is = new ByteArrayInputStream(string.getBytes());
                break;
        }

        return is;
    }

}
