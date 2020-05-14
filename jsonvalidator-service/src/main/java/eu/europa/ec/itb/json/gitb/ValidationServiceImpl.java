package eu.europa.ec.itb.json.gitb;

import com.gitb.core.*;
import com.gitb.vs.Void;
import com.gitb.vs.*;
import eu.europa.ec.itb.json.DomainConfig;
import eu.europa.ec.itb.json.ValidatorContent;
import eu.europa.ec.itb.json.errors.ValidatorException;
import eu.europa.ec.itb.json.validation.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.jws.WebParam;
import java.io.File;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

/**
 * Spring component that realises the validation service.
 */
@Component
@Scope("prototype")
public class ValidationServiceImpl implements ValidationService {

    /** Logger. **/
    private static final Logger logger = LoggerFactory.getLogger(ValidationServiceImpl.class);
    private final DomainConfig domainConfig;

    @Autowired
    ApplicationContext ctx;
    @Autowired
    ValidatorContent validatorContent;
    @Autowired
    FileManager fileManager;

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
        response.getModule().getInputs().getParam().add(createParameter(ValidationConstants.INPUT_CONTENT, "binary", UsageEnumeration.R, ConfigurationType.SIMPLE, domainConfig.getWebServiceDescription().get(ValidationConstants.INPUT_CONTENT)));
        response.getModule().getInputs().getParam().add(createParameter(ValidationConstants.INPUT_EMBEDDING_METHOD, "string", UsageEnumeration.O, ConfigurationType.SIMPLE, domainConfig.getWebServiceDescription().get(ValidationConstants.INPUT_EMBEDDING_METHOD)));
        response.getModule().getInputs().getParam().add(createParameter(ValidationConstants.INPUT_VALIDATION_TYPE, "string", UsageEnumeration.O, ConfigurationType.SIMPLE, domainConfig.getWebServiceDescription().get(ValidationConstants.INPUT_VALIDATION_TYPE)));
        boolean allowsExternalSchemas = definesTypeWithExternalSchemas();
        if (allowsExternalSchemas) {
            response.getModule().getInputs().getParam().add(createParameter(ValidationConstants.INPUT_EXTERNAL_SCHEMAS, "list[map]", UsageEnumeration.O, ConfigurationType.SIMPLE, domainConfig.getWebServiceDescription().get(ValidationConstants.INPUT_EXTERNAL_SCHEMAS)));
            response.getModule().getInputs().getParam().add(createParameter(ValidationConstants.INPUT_EXTERNAL_SCHEMA_COMBINATION_APPROACH, "boolean", UsageEnumeration.O, ConfigurationType.SIMPLE, domainConfig.getWebServiceDescription().get(ValidationConstants.INPUT_EXTERNAL_SCHEMA_COMBINATION_APPROACH)));
        }
        response.getModule().getInputs().getParam().add(createParameter(ValidationConstants.INPUT_LOCATION_AS_POINTER, "boolean", UsageEnumeration.O, ConfigurationType.SIMPLE, domainConfig.getWebServiceDescription().get(ValidationConstants.INPUT_LOCATION_AS_POINTER)));
        return response;
    }

    private boolean definesTypeWithExternalSchemas() {
        for (Boolean value: domainConfig.getExternalSchemas().values()) {
            if (value) {
                return true;
            }
        }
        return false;
    }

    /**
     * Create a parameter definition.
     *
     * @param name The name of the parameter.
     * @param type The type of the parameter. This needs to match one of the GITB types.
     * @param use The use (required or optional).
     * @param kind The kind of parameter it is (whether it should be provided as the specific value, as BASE64 content or as a URL that needs to be looked up to obtain the value).
     * @param description The description of the parameter.
     * @return The created parameter.
     */
    private TypedParameter createParameter(String name, String type, UsageEnumeration use, ConfigurationType kind, String description) {
        TypedParameter parameter =  new TypedParameter();
        parameter.setName(name);
        parameter.setType(type);
        parameter.setUse(use);
        parameter.setKind(kind);
        parameter.setDesc(description);
        return parameter;
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
			String contentEmbeddingMethod = validateContentEmbeddingMethod(validateRequest);
			boolean locationAsPointer = getLocationAsPointerInputValue(validateRequest);
			File contentToValidate = validateContentToValidate(validateRequest, contentEmbeddingMethod, tempFolderPath);
			String validationType = validateValidationType(validateRequest);
            List<FileInfo> externalSchemas = validateExternalSchemas(validateRequest, validationType, tempFolderPath);
            SchemaCombinationApproach externalSchemaCombinationApproach = validateExternalSchemaCombinationApproach(validateRequest, validationType);
            ValidationResponse result = new ValidationResponse();
			// Execute validation
            JSONValidator validator = ctx.getBean(JSONValidator.class, contentToValidate, validationType, externalSchemas, externalSchemaCombinationApproach, domainConfig, locationAsPointer);
			result.setReport(validator.validate());
			return result;
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

    private SchemaCombinationApproach validateExternalSchemaCombinationApproach(ValidateRequest validateRequest, String validationType) {
        List<AnyContent> inputs =  getInputFor(validateRequest, ValidationConstants.INPUT_EXTERNAL_SCHEMA_COMBINATION_APPROACH);
        SchemaCombinationApproach approach;
        if (inputs.size() > 0) {
            approach = SchemaCombinationApproach.valueOf(inputs.get(0).getValue());
        } else {
            approach = domainConfig.getExternalSchemaCombinationApproach().get(validationType);
        }
        return approach;
    }

    private boolean getLocationAsPointerInputValue(ValidateRequest validateRequest) {
        List<AnyContent> inputs =  getInputFor(validateRequest, ValidationConstants.INPUT_LOCATION_AS_POINTER);
        if (inputs.isEmpty()) {
            return false;
        } else {
            return Boolean.parseBoolean(inputs.get(0).getValue());
        }
    }

    /**
     * Validation of the external schemas.
     * @param validateRequest The request's parameters.
     * @return The list of external schemas.
     */
    private List<FileInfo> validateExternalSchemas(ValidateRequest validateRequest, String validationType, File parentFolder) {
        List<FileContent> filesContent = new ArrayList<>();
        List<AnyContent> listInput = getInputFor(validateRequest, ValidationConstants.INPUT_EXTERNAL_SCHEMAS);

        if (!listInput.isEmpty()) {
            if (!domainConfig.getExternalSchemas().get(validationType)) {
                throw new ValidatorException(String.format("Validation type [%s] does not expect user-provided schemas.", validationType));
            }
            AnyContent listRuleSets = listInput.get(0);
            FileContent ruleFileContent = getFileContent(listRuleSets);
            if (!StringUtils.isEmpty(ruleFileContent.getContent())) filesContent.add(ruleFileContent);
            for(AnyContent content : listRuleSets.getItem()) {
                FileContent fileContent = getFileContent(content);

                if (!StringUtils.isEmpty(fileContent.getContent())) {
                    filesContent.add(fileContent);
                }
            }
            return getExternalSchemas(filesContent, parentFolder);
        } else {
            return Collections.emptyList();
        }
    }

    /**
     * Transforms the list of FileContent to FileInfo.
     * @param externalSchemas The list of external shapes as FileContent.
     * @return The list of external shapes as FileInfo.
     */
    private List<FileInfo> getExternalSchemas(List<FileContent> externalSchemas, File parentFolder) {
        List<FileInfo> schemaFiles;
        if (externalSchemas != null) {
            try {
                schemaFiles = fileManager.getRemoteExternalSchemas(parentFolder, externalSchemas);
            } catch (Exception e) {
                throw new ValidatorException("An error occurred while trying to read the provided external shapes.", e);
            }
        } else {
            schemaFiles = Collections.emptyList();
        }
        return schemaFiles;
    }

    private FileContent getFileContent(AnyContent content) {
        FileContent fileContent = new FileContent();
        ValueEmbeddingEnumeration embeddingMethod = null;
        String explicitEmbeddingMethod = null;
        if (content.getItem() != null && !content.getItem().isEmpty()) {
            boolean isSchema = false;
            for (AnyContent schema : content.getItem()) {
                if (StringUtils.equals(schema.getName(), ValidationConstants.INPUT_EXTERNAL_SCHEMAS_SCHEMA)) {
                    embeddingMethod = schema.getEmbeddingMethod();
                    fileContent.setContent(schema.getValue());
                    isSchema = true;
                }
                if(StringUtils.equals(schema.getName(), ValidationConstants.INPUT_EMBEDDING_METHOD)) {
                    explicitEmbeddingMethod = getEmbeddingMethod(schema);
                }
            }
            if (isSchema) {
                if (explicitEmbeddingMethod == null) {
                    explicitEmbeddingMethod = FileContent.fromValueEmbeddingEnumeration(embeddingMethod);
                }
                if (explicitEmbeddingMethod == null) {
                    // Embedding method not provided as input nor as parameter.
                    throw new ValidatorException(String.format("For user-provided schemas the embedding method needs to be provided either as a separate input [%s] or as an attribute of the [%s] input.", ValidationConstants.INPUT_EMBEDDING_METHOD, ValidationConstants.INPUT_EXTERNAL_SCHEMAS_SCHEMA));
                }
                if (embeddingMethod == ValueEmbeddingEnumeration.BASE_64 && !FileContent.embedding_BASE64.equals(explicitEmbeddingMethod)) {
                    // This is a URI or a plain text string encoded as BASE64.
                    fileContent.setContent(new String(Base64.getDecoder().decode(fileContent.getContent())));
                }
                fileContent.setEmbeddingMethod(explicitEmbeddingMethod);
            }
        }
        return fileContent;
    }

    /**
     * Validation of the mime type of the provided RDF content.
     * @param validateRequest The request's parameters.
     * @return The type of validation.
     */
    private String validateValidationType(ValidateRequest validateRequest) {
        List<AnyContent> listValidationType = getInputFor(validateRequest, ValidationConstants.INPUT_VALIDATION_TYPE);
        String validationType = null;
        if (!listValidationType.isEmpty()) {
	    	AnyContent content = listValidationType.get(0);
	    	if (content.getEmbeddingMethod() == ValueEmbeddingEnumeration.STRING) {
				validationType = content.getValue();
	    	} else {
				throw new ValidatorException(String.format("The validation type to perform must be provided with a [STRING] embeddingMethod. This was provided as [%s].", content.getEmbeddingMethod()));
	    	}
        }
		return validatorContent.validateValidationType(validationType, domainConfig);
	}
    
    private String validateContentEmbeddingMethod(ValidateRequest validateRequest){
        List<AnyContent> listContentEmbeddingMethod = getInputFor(validateRequest, ValidationConstants.INPUT_EMBEDDING_METHOD);        
        
        if(!listContentEmbeddingMethod.isEmpty()) {
        	AnyContent content = listContentEmbeddingMethod.get(0);

        	return getEmbeddingMethod(content);
        }else {
        	return null;
        }
    }
    
    private String getEmbeddingMethod(AnyContent content) {
    	String value = content.getValue();
    	if (!StringUtils.isBlank(value)) {
			if (!FileContent.isValid(value)) {
				throw new ValidatorException(String.format("The provided embedding method [%s] is not valid.", value));
			}
		}
    	return value;
    }
    
    /**
     * Validation of the content.
     * @param validateRequest The request's parameters.
     * @param explicitEmbeddingMethod The embedding method.
	 * @return The file to validate.
     */
    private File validateContentToValidate(ValidateRequest validateRequest, String explicitEmbeddingMethod, File parentFolder) {
        List<AnyContent> listContentToValidate = getInputFor(validateRequest, ValidationConstants.INPUT_CONTENT);
        if (!listContentToValidate.isEmpty()) {
	    	AnyContent content = listContentToValidate.get(0);
			if (explicitEmbeddingMethod == null) {
				explicitEmbeddingMethod = FileContent.fromValueEmbeddingEnumeration(content.getEmbeddingMethod());
			}
			if (explicitEmbeddingMethod == null) {
				// Embedding method not provided as input nor as parameter.
				throw new ValidatorException(String.format("The embedding method needs to be provided either as input parameter [%s] or be set as an attribute on the [%s] input.", ValidationConstants.INPUT_EMBEDDING_METHOD, ValidationConstants.INPUT_CONTENT));
			}
			String valueToProcess = content.getValue();
			if (content.getEmbeddingMethod() == ValueEmbeddingEnumeration.BASE_64 && !FileContent.embedding_BASE64.equals(explicitEmbeddingMethod)) {
				// This is a URI or a plain text string encoded as BASE64.
				valueToProcess = new String(Base64.getDecoder().decode(valueToProcess));
			}
	    	return validatorContent.getContentToValidate(explicitEmbeddingMethod, valueToProcess, parentFolder);
        } else {
        	throw new ValidatorException(String.format("No content was provided for validation (input parameter [%s]).", ValidationConstants.INPUT_CONTENT));
        }
    }

    /**
     * Lookup a provided input from the received request parameters.
     *
     * @param validateRequest The request's parameters.
     * @param name The name of the input to lookup.
     * @return The inputs found to match the parameter name (not null).
     */
    private List<AnyContent> getInputFor(ValidateRequest validateRequest, String name) {
        List<AnyContent> inputs = new ArrayList<>();
        if (validateRequest != null && validateRequest.getInput() != null) {
            for (AnyContent anInput: validateRequest.getInput()) {
                if (name.equals(anInput.getName())) {
                    inputs.add(anInput);
                }
            }
        }
        return inputs;
    }

}
