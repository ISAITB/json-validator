package eu.europa.ec.itb.json.web;

import eu.europa.ec.itb.json.ApplicationConfig;
import eu.europa.ec.itb.json.DomainConfig;
import eu.europa.ec.itb.json.DomainConfigCache;
import eu.europa.ec.itb.json.validation.FileManager;
import eu.europa.ec.itb.json.validation.JSONValidator;
import eu.europa.ec.itb.json.validation.ValidationSpecs;
import eu.europa.ec.itb.validation.commons.FileInfo;
import eu.europa.ec.itb.validation.commons.LocalisationHelper;
import eu.europa.ec.itb.validation.commons.artifact.ExternalArtifactSupport;
import eu.europa.ec.itb.validation.commons.artifact.ValidationArtifactCombinationApproach;
import eu.europa.ec.itb.validation.commons.artifact.ValidationArtifactInfo;
import eu.europa.ec.itb.validation.commons.error.ValidatorException;
import eu.europa.ec.itb.validation.commons.web.BaseUploadController;
import eu.europa.ec.itb.validation.commons.web.Constants;
import eu.europa.ec.itb.validation.commons.web.dto.Translations;
import eu.europa.ec.itb.validation.commons.web.dto.UploadResult;
import eu.europa.ec.itb.validation.commons.web.locale.CustomLocaleResolver;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
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

import static eu.europa.ec.itb.validation.commons.web.Constants.*;

/**
 * Controller to manage the validator's web user interface.
 */
@Controller
public class UploadController extends BaseUploadController<DomainConfig, DomainConfigCache> {

    private static final Logger logger = LoggerFactory.getLogger(UploadController.class);

    @Autowired
    private FileManager fileManager = null;
    @Autowired
    private BeanFactory beans = null;
    @Autowired
    private ApplicationConfig appConfig = null;
    @Autowired
    private CustomLocaleResolver localeResolver = null;

    /**
     * Prepare the upload page.
     *
     * @param domain The domain name.
     * @param request The received request.
     * @param response The HTTP response.
     * @return The model and view information.
     */
    @GetMapping(value = "/{domain}/upload")
    public ModelAndView upload(@PathVariable("domain") String domain, HttpServletRequest request, HttpServletResponse response) {
        var config = getDomainConfig(request);
        Map<String, Object> attributes = new HashMap<>();
        attributes.put(PARAM_DOMAIN_CONFIG, config);
        attributes.put(PARAM_APP_CONFIG, appConfig);
        attributes.put(PARAM_MINIMAL_UI, isMinimalUI(request));
        attributes.put(PARAM_EXTERNAL_ARTIFACT_INFO, config.getExternalArtifactInfoMap());
        var localisationHelper = new LocalisationHelper(config, localeResolver.resolveLocale(request, response, config, appConfig));
        attributes.put(PARAM_LOCALISER, localisationHelper);
        attributes.put(PARAM_HTML_BANNER_EXISTS, localisationHelper.propertyExists("validator.bannerHtml"));
        return new ModelAndView(VIEW_UPLOAD_FORM, attributes);
    }

    /**
     * Prepare the upload page (minimal UI version).
     *
     * @param domain The domain name.
     * @param request The received request.
     * @param response The HTTP response.
     * @return The model and view information.
     */
    @GetMapping(value = "/{domain}/uploadm")
    public ModelAndView uploadMinimal(@PathVariable("domain") String domain, HttpServletRequest request, HttpServletResponse response) {
        return upload(domain, request, response);
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
    @PostMapping(value = "/{domain}/upload", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public UploadResult<Translations> handleUpload(@PathVariable("domain") String domain,
                                                   @RequestParam(value = "file", required = false) MultipartFile file,
                                                   @RequestParam(value = "uri", defaultValue = "") String uri,
                                                   @RequestParam(value = "text", defaultValue = "") String string,
                                                   @RequestParam(value = "validationType", defaultValue = "") String validationType,
                                                   @RequestParam(value = "contentType", defaultValue = "") String contentType,
                                                   @RequestParam(value = "contentType-external_default", required = false) String[] externalSchemaContentType,
                                                   @RequestParam(value = "inputFile-external_default", required= false) MultipartFile[] externalSchemaFiles,
                                                   @RequestParam(value = "uri-external_default", required = false) String[] externalSchemaUri,
                                                   @RequestParam(value = "combinationType", defaultValue = "") String combinationType,
                                                   RedirectAttributes redirectAttributes,
                                                   HttpServletRequest request,
                                                   HttpServletResponse response) {
        var config = getDomainConfig(request);
        contentType = checkInputType(contentType, file, uri, string);
        var localisationHelper = new LocalisationHelper(config, localeResolver.resolveLocale(request, response, config, appConfig));
        var result = new UploadResult<>();

        if (StringUtils.isBlank(validationType)) {
            validationType = config.getType().get(0);
        }
        if (config.hasMultipleValidationTypes() && (validationType == null || !config.getType().contains(validationType))) {
            // A validation type is required.
            result.setMessage(localisationHelper.localise("validator.label.exception.providedValidationTypeNotValid"));
        } else {
            InputStream stream = null;
            try (InputStream fis = getInputStream(contentType, file.getInputStream(), uri, string)) {
                if (fileManager.checkFileType(fis)) {
                    stream = getInputStream(contentType, file.getInputStream(), uri, string);
                } else {
                    result.setMessage(localisationHelper.localise("validator.label.exception.providedInputNotJSON"));
                }
            } catch (IOException e) {
                logger.error("Error while reading uploaded file [" + e.getMessage() + "]", e);
                result.setMessage(localisationHelper.localise("validator.label.exception.errorInUpload", e.getMessage()));
            }
            if (stream != null) {
                File tempFolderForRequest = fileManager.createTemporaryFolderPath();
                try {
                    File contentToValidate = fileManager.getFileFromInputStream(tempFolderForRequest, stream, null, UUID.randomUUID() +".json");
                    List<FileInfo> externalSchemas = new ArrayList<>();
                    boolean proceedToValidate = true;
                    try {
                        externalSchemas = getExternalFiles(externalSchemaContentType, externalSchemaFiles, externalSchemaUri, config.getSchemaInfo(validationType), tempFolderForRequest);
                    } catch (IOException e) {
                        logger.error("Error while reading uploaded file [" + e.getMessage() + "]", e);
                        result.setMessage(localisationHelper.localise("validator.label.exception.errorInUpload", e.getMessage()));
                        proceedToValidate = false;
                    }
                    if (proceedToValidate) {
                        JSONValidator validator = beans.getBean(JSONValidator.class, ValidationSpecs.builder(contentToValidate, localisationHelper, config)
                                .withValidationType(validationType)
                                .withExternalSchemas(externalSchemas, getSchemaCombinationApproach(validationType, combinationType, config))
                                .addInputToReport()
                                .produceAggregateReport()
                                .build());
                        var reports = validator.validate();
                        // Cache detailed report.
                        try {
                            String inputID = fileManager.writeJson(config.getDomainName(), reports.getDetailedReport().getContext().getItem().get(0).getValue());
                            fileManager.saveReport(reports.getDetailedReport(), inputID, config);
                            fileManager.saveReport(reports.getAggregateReport(), inputID, config, true);
                            String fileName;
                            if (contentType.equals(CONTENT_TYPE_FILE)) {
                                fileName = file.getOriginalFilename();
                            } else if (contentType.equals(CONTENT_TYPE_URI)) {
                                fileName = uri;
                            } else {
                                fileName = "-";
                            }
                            result.populateCommon(localisationHelper, validationType, config, inputID,
                                    fileName, reports.getDetailedReport(), reports.getAggregateReport(),
                                    new Translations(localisationHelper, reports.getDetailedReport(), config));
                        } catch (IOException e) {
                            logger.error("Error generating detailed report [" + e.getMessage() + "]", e);
                            result.setMessage(localisationHelper.localise("validator.label.exception.errorGeneratingDetailedReport", e.getMessage()));
                        }
                    }
                } catch (ValidatorException e) {
                    logger.error(e.getMessageForLog(), e);
                    result.setMessage(e.getMessageForDisplay(localisationHelper));
                } catch (Exception e) {
                    logger.error("An error occurred during the validation [" + e.getMessage() + "]", e);
                    if (e.getMessage() != null) {
                        result.setMessage(localisationHelper.localise("validator.label.exception.unexpectedErrorDuringValidationWithParams", e.getMessage()));
                    } else {
                        result.setMessage(localisationHelper.localise("validator.label.exception.unexpectedErrorDuringValidation"));
                    }
                } finally {
                    // Cleanup temporary resources for request.
                    if (tempFolderForRequest.exists()) {
                        FileUtils.deleteQuietly(tempFolderForRequest);
                    }
                }
            }
        }
        return result;
    }

    /**
     * Handle the upload form's submission when the user interface is minimal.
     *
     * @see UploadController#handleUpload(String, MultipartFile, String, String, String, String, String[], MultipartFile[], String[], String, RedirectAttributes, HttpServletRequest, HttpServletResponse)
     */
    @PostMapping(value = "/{domain}/uploadm", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public UploadResult<Translations> handleUploadMinimal(@PathVariable("domain") String domain,
                                                    @RequestParam(value = "file", required = false) MultipartFile file,
                                                    @RequestParam(value = "uri", defaultValue = "") String uri,
                                                    @RequestParam(value = "text", defaultValue = "") String string,
                                                    @RequestParam(value = "validationType", defaultValue = "") String validationType,
                                                    @RequestParam(value = "contentType", defaultValue = "") String contentType,
                                                    @RequestParam(value = "contentType-external_default", required = false) String[] externalSchema,
                                                    @RequestParam(value = "inputFile-external_default", required= false) MultipartFile[] externalSchemaFiles,
                                                    @RequestParam(value = "uriToValidate-external_default", required = false) String[] externalSchemaUri,
                                                    @RequestParam(value = "combinationType", defaultValue = "") String combinationType,
                                                    RedirectAttributes redirectAttributes,
                                                    HttpServletRequest request,
                                                    HttpServletResponse response) {
        return handleUpload(domain, file, uri, string, validationType, contentType, externalSchema, externalSchemaFiles, externalSchemaUri, combinationType, redirectAttributes, request, response);
    }

    /**
     * Handle the upload form's submission when the user interface is embedded in another web page.
     *
     * @see UploadController#handleUpload(String, MultipartFile, String, String, String, String, String[], MultipartFile[], String[], String, RedirectAttributes, HttpServletRequest, HttpServletResponse)
     */
    @PostMapping(value = "/{domain}/upload", produces = MediaType.TEXT_HTML_VALUE)
    public ModelAndView handleUploadEmbedded(@PathVariable("domain") String domain,
                                             @RequestParam(value = "file", required = false) MultipartFile file,
                                             @RequestParam(value = "uri", defaultValue = "") String uri,
                                             @RequestParam(value = "text", defaultValue = "") String string,
                                             @RequestParam(value = "validationType", defaultValue = "") String validationType,
                                             @RequestParam(value = "contentType", defaultValue = "") String contentType,
                                             @RequestParam(value = "contentType-external_default", required = false) String[] externalSchema,
                                             @RequestParam(value = "inputFile-external_default", required= false) MultipartFile[] externalSchemaFiles,
                                             @RequestParam(value = "uriToValidate-external_default", required = false) String[] externalSchemaUri,
                                             @RequestParam(value = "combinationType", defaultValue = "") String combinationType,
                                             RedirectAttributes redirectAttributes,
                                             HttpServletRequest request,
                                             HttpServletResponse response) {
        var uploadForm = upload(domain, request, response);
        var uploadResult = handleUpload(domain, file, uri, string, validationType, contentType, externalSchema, externalSchemaFiles, externalSchemaUri, combinationType, redirectAttributes, request, response);
        uploadForm.getModel().put(Constants.PARAM_REPORT_DATA, writeResultToString(uploadResult));
        return uploadForm;
    }

    /**
     * Handle the upload form's submission when the user interface is minimal and embedded in another web page.
     *
     * @see UploadController#handleUpload(String, MultipartFile, String, String, String, String, String[], MultipartFile[], String[], String, RedirectAttributes, HttpServletRequest, HttpServletResponse)
     */
    @PostMapping(value = "/{domain}/uploadm", produces = MediaType.TEXT_HTML_VALUE)
    public ModelAndView handleUploadMinimalEmbedded(@PathVariable("domain") String domain,
                                                    @RequestParam(value = "file", required = false) MultipartFile file,
                                                    @RequestParam(value = "uri", defaultValue = "") String uri,
                                                    @RequestParam(value = "text", defaultValue = "") String string,
                                                    @RequestParam(value = "validationType", defaultValue = "") String validationType,
                                                    @RequestParam(value = "contentType", defaultValue = "") String contentType,
                                                    @RequestParam(value = "contentType-external_default", required = false) String[] externalSchema,
                                                    @RequestParam(value = "inputFile-external_default", required= false) MultipartFile[] externalSchemaFiles,
                                                    @RequestParam(value = "uriToValidate-external_default", required = false) String[] externalSchemaUri,
                                                    @RequestParam(value = "combinationType", defaultValue = "") String combinationType,
                                                    RedirectAttributes redirectAttributes,
                                                    HttpServletRequest request,
                                                    HttpServletResponse response) {
        return handleUploadEmbedded(domain, file, uri, string, validationType, contentType, externalSchema, externalSchemaFiles, externalSchemaUri, combinationType, redirectAttributes, request, response);
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
     * Validate and get the user-provided schemas.
     *
     * @param externalContentType The directly provided schemas.
     * @param externalFiles The schemas provided as files.
     * @param externalUri The schemas provided as URIs.
     * @param schemaInfo The schema information from the domain.
     * @param parentFolder The temporary folder to use for file system storage.
     * @return The list of user-provided artifacts.
     * @throws IOException If an IO error occurs.
     */
    private List<FileInfo> getExternalFiles(String[] externalContentType, MultipartFile[] externalFiles, String[] externalUri,
                                            ValidationArtifactInfo schemaInfo, File parentFolder) throws IOException {
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
        if (validateExternalFiles(lis, schemaInfo)) {
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
     * @return True for correctly provided schemas.
     */
    private boolean validateExternalFiles(List<FileInfo> lis, ValidationArtifactInfo schemaInfo) {
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
        File file = null;
        if (CONTENT_TYPE_FILE.equals(contentType)) {
            if (inputFile!=null && !inputFile.isEmpty()) {
                file = this.fileManager.getFileFromInputStream(parentFolder, inputFile.getInputStream(), null, inputFile.getOriginalFilename());
            }
        } else if (CONTENT_TYPE_URI.equals(contentType)) {
            if (!inputUri.isEmpty()) {
                file = this.fileManager.getFileFromURL(parentFolder, inputUri);
            }
        }
        return file;
    }

    /**
     * Load a stream from the provided input.
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
            case CONTENT_TYPE_FILE:
                is = inputStream;
                break;

            case CONTENT_TYPE_URI:
                is = this.fileManager.getInputStreamFromURL(uri);
                break;

            case CONTENT_TYPE_STRING:
                is = new ByteArrayInputStream(string.getBytes());
                break;
        }

        return is;
    }

}
