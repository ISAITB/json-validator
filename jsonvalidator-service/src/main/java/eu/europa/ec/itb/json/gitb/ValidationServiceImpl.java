package eu.europa.ec.itb.json.gitb;

import com.gitb.core.*;
import com.gitb.vs.Void;
import com.gitb.vs.*;
import eu.europa.ec.itb.json.DomainConfig;
import eu.europa.ec.itb.json.InputHelper;
import eu.europa.ec.itb.json.validation.FileManager;
import eu.europa.ec.itb.json.validation.JSONValidator;
import eu.europa.ec.itb.json.validation.ValidationConstants;
import eu.europa.ec.itb.validation.commons.FileInfo;
import eu.europa.ec.itb.validation.commons.Utils;
import eu.europa.ec.itb.validation.commons.error.ValidatorException;
import eu.europa.ec.itb.validation.commons.artifact.ExternalArtifactSupport;
import eu.europa.ec.itb.validation.commons.artifact.TypedValidationArtifactInfo;
import eu.europa.ec.itb.validation.commons.artifact.ValidationArtifactCombinationApproach;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.jws.WebParam;
import javax.xml.ws.WebServiceContext;

import java.io.File;
import java.util.List;

/**
 * Spring component that realises the validation SOAP service.
 */
@Component
@Scope("prototype")
public class ValidationServiceImpl implements ValidationService {

    private static final Logger logger = LoggerFactory.getLogger(ValidationServiceImpl.class);
    private final DomainConfig domainConfig;

    @Autowired
    private ApplicationContext ctx = null;
    @Autowired
    private FileManager fileManager = null;
    @Autowired
    private InputHelper inputHelper = null;
    @Resource
    private WebServiceContext wsContext;

    /**
     * Constructor.
     *
     * @param domainConfig The domain configuration (each domain has its own instance).
     */
    public ValidationServiceImpl(DomainConfig domainConfig) {
        this.domainConfig = domainConfig;
    }
    
    /**
     * The purpose of the getModuleDefinition call is to inform its caller on how the service is supposed to be called.
     *
     * @param parameters No parameters are expected.
     * @return The response.
     */
    @Override
    public GetModuleDefinitionResponse getModuleDefinition(@WebParam(name = "GetModuleDefinitionRequest", targetNamespace = "http://www.gitb.com/vs/v1/", partName = "parameters") Void parameters) {
        MDC.put("domain", domainConfig.getDomain());
        GetModuleDefinitionResponse response = new GetModuleDefinitionResponse();
        response.setModule(new ValidationModule());
        response.getModule().setId(domainConfig.getWebServiceId());
        response.getModule().setOperation("V");
        response.getModule().setMetadata(new Metadata());
        response.getModule().getMetadata().setName(domainConfig.getWebServiceId());
        response.getModule().getMetadata().setVersion("1.0.0");
        response.getModule().setInputs(new TypedParameters());
        response.getModule().getInputs().getParam().add(Utils.createParameter(ValidationConstants.INPUT_CONTENT, "binary", UsageEnumeration.R, domainConfig.getWebServiceDescription().get(ValidationConstants.INPUT_CONTENT)));
        response.getModule().getInputs().getParam().add(Utils.createParameter(ValidationConstants.INPUT_EMBEDDING_METHOD, "string", UsageEnumeration.O, domainConfig.getWebServiceDescription().get(ValidationConstants.INPUT_EMBEDDING_METHOD)));
        response.getModule().getInputs().getParam().add(Utils.createParameter(ValidationConstants.INPUT_VALIDATION_TYPE, "string", UsageEnumeration.O, domainConfig.getWebServiceDescription().get(ValidationConstants.INPUT_VALIDATION_TYPE)));
        boolean allowsExternalSchemas = definesTypeWithExternalSchemas();
        if (allowsExternalSchemas) {
            response.getModule().getInputs().getParam().add(Utils.createParameter(ValidationConstants.INPUT_EXTERNAL_SCHEMAS, "list[map]", UsageEnumeration.O, domainConfig.getWebServiceDescription().get(ValidationConstants.INPUT_EXTERNAL_SCHEMAS)));
            response.getModule().getInputs().getParam().add(Utils.createParameter(ValidationConstants.INPUT_EXTERNAL_SCHEMA_COMBINATION_APPROACH, "boolean", UsageEnumeration.O, domainConfig.getWebServiceDescription().get(ValidationConstants.INPUT_EXTERNAL_SCHEMA_COMBINATION_APPROACH)));
        }
        response.getModule().getInputs().getParam().add(Utils.createParameter(ValidationConstants.INPUT_LOCATION_AS_POINTER, "boolean", UsageEnumeration.O, domainConfig.getWebServiceDescription().get(ValidationConstants.INPUT_LOCATION_AS_POINTER)));
        response.getModule().getInputs().getParam().add(Utils.createParameter(ValidationConstants.INPUT_ADD_INPUT_TO_REPORT, "boolean", UsageEnumeration.O, ConfigurationType.SIMPLE, domainConfig.getWebServiceDescription().get(ValidationConstants.INPUT_ADD_INPUT_TO_REPORT)));
        return response;
    }

    /**
     * Check to see if the domain configuration includes validation types that support or require
     * user-provided schemas.
     *
     * @return True if user-provided schemas are expected or required.
     */
    private boolean definesTypeWithExternalSchemas() {
        for (TypedValidationArtifactInfo info: domainConfig.getArtifactInfo().values()) {
            if (info.get().getExternalArtifactSupport() != ExternalArtifactSupport.NONE) {
                return true;
            }
        }
        return false;
    }

    /**
     * The validate operation is called to validate the input and produce a validation report.
     *
     * The expected input is described for the service's client through the getModuleDefinition call.
     *
     * @param validateRequest The input parameters and configuration for the validation.
     * @return The response containing the validation report.
     */
    @Override
    public ValidationResponse validate(@WebParam(name = "ValidateRequest", targetNamespace = "http://www.gitb.com/vs/v1/", partName = "parameters") ValidateRequest validateRequest) {
    	MDC.put("domain", domainConfig.getDomain());
    	File tempFolderPath = fileManager.createTemporaryFolderPath();
    	try {
			// Validation of the input data
			ValueEmbeddingEnumeration contentEmbeddingMethod = inputHelper.validateContentEmbeddingMethod(validateRequest, ValidationConstants.INPUT_EMBEDDING_METHOD);
			boolean locationAsPointer = getInputAsBoolean(validateRequest, ValidationConstants.INPUT_LOCATION_AS_POINTER, false);
            boolean addInputToReport = getInputAsBoolean(validateRequest, ValidationConstants.INPUT_ADD_INPUT_TO_REPORT, true);
            File contentToValidate = inputHelper.validateContentToValidate(validateRequest, ValidationConstants.INPUT_CONTENT, contentEmbeddingMethod, tempFolderPath);
            String validationType = inputHelper.validateValidationType(domainConfig, validateRequest, ValidationConstants.INPUT_VALIDATION_TYPE);
            List<FileInfo> externalSchemas = inputHelper.validateExternalArtifacts(domainConfig, validateRequest, ValidationConstants.INPUT_EXTERNAL_SCHEMAS, ValidationConstants.INPUT_EXTERNAL_SCHEMAS_SCHEMA, ValidationConstants.INPUT_EMBEDDING_METHOD, validationType, null, tempFolderPath);
            ValidationArtifactCombinationApproach externalSchemaCombinationApproach = validateExternalSchemaCombinationApproach(validateRequest, validationType);
            ValidationResponse result = new ValidationResponse();
			// Execute validation
            JSONValidator validator = ctx.getBean(JSONValidator.class, contentToValidate, validationType, externalSchemas, externalSchemaCombinationApproach, domainConfig, locationAsPointer, addInputToReport);
			result.setReport(validator.validate());
			return result;
        } catch (ValidatorException e) {
            logger.error("Validation error", e);
            throw e;
		} catch (Exception e) {
			logger.error("Unexpected error", e);
			throw new ValidatorException(e);
		} finally {
    	    // Cleanup.
    	    if (tempFolderPath.exists()) {
                FileUtils.deleteQuietly(tempFolderPath);
            }
        }
    }

    /**
     * Validate the received external schema combination approach.
     *
     * @param validateRequest The received request.
     * @param validationType The validation type.
     * @return The approach to use.
     */
    private ValidationArtifactCombinationApproach validateExternalSchemaCombinationApproach(ValidateRequest validateRequest, String validationType) {
        List<AnyContent> inputs = Utils.getInputFor(validateRequest, ValidationConstants.INPUT_EXTERNAL_SCHEMA_COMBINATION_APPROACH);
        ValidationArtifactCombinationApproach approach;
        if (inputs.size() > 0) {
            try {
                approach = ValidationArtifactCombinationApproach.byName(inputs.get(0).getValue());
            } catch (IllegalArgumentException e) {
                throw new ValidatorException("Invalid schema combination approach ["+inputs.get(0).getValue()+"].", e);
            }
        } else {
            approach = domainConfig.getSchemaInfo(validationType).getExternalArtifactCombinationApproach();
        }
        return approach;
    }

    /**
     * Get the provided (optional) input as a boolean value.
     *
     * @param validateRequest The input parameters.
     * @param inputName The name of the input to look for.
     * @param defaultIfMissing The default value to use if the input is not provided.
     * @return The value to use.
     */
    private boolean getInputAsBoolean(ValidateRequest validateRequest, String inputName, boolean defaultIfMissing) {
        List<AnyContent> input = Utils.getInputFor(validateRequest, inputName);
        if (!input.isEmpty()) {
            return Boolean.parseBoolean(input.get(0).getValue());
        }
        return defaultIfMissing;
    }

    /**
     * @return The web service context.
     */
    public WebServiceContext getWebServiceContext() {
        return this.wsContext;
    }    

}
