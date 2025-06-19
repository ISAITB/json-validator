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

package eu.europa.ec.itb.json.gitb;

import com.gitb.core.*;
import com.gitb.vs.Void;
import com.gitb.vs.*;
import eu.europa.ec.itb.json.DomainConfig;
import eu.europa.ec.itb.json.InputHelper;
import eu.europa.ec.itb.json.validation.FileManager;
import eu.europa.ec.itb.json.validation.JSONValidator;
import eu.europa.ec.itb.json.validation.ValidationConstants;
import eu.europa.ec.itb.json.validation.ValidationSpecs;
import eu.europa.ec.itb.validation.commons.FileInfo;
import eu.europa.ec.itb.validation.commons.LocalisationHelper;
import eu.europa.ec.itb.validation.commons.Utils;
import eu.europa.ec.itb.validation.commons.artifact.ExternalArtifactSupport;
import eu.europa.ec.itb.validation.commons.artifact.TypedValidationArtifactInfo;
import eu.europa.ec.itb.validation.commons.artifact.ValidationArtifactCombinationApproach;
import eu.europa.ec.itb.validation.commons.error.ValidatorException;
import eu.europa.ec.itb.validation.commons.web.WebServiceContextProvider;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.LocaleUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import jakarta.jws.WebParam;
import jakarta.xml.ws.WebServiceContext;
import java.io.File;
import java.util.List;

/**
 * Spring component that realises the validation SOAP service.
 */
@Component
@Scope("prototype")
public class ValidationServiceImpl implements ValidationService, WebServiceContextProvider {

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
        domainConfig.applyWebServiceMetadata(response.getModule());
        response.getModule().setInputs(new TypedParameters());
        response.getModule().getInputs().getParam().add(Utils.createParameter(ValidationConstants.INPUT_CONTENT, "binary", UsageEnumeration.R, domainConfig.getWebServiceDescription().get(ValidationConstants.INPUT_CONTENT)));
        response.getModule().getInputs().getParam().add(Utils.createParameter(ValidationConstants.INPUT_EMBEDDING_METHOD, "string", UsageEnumeration.O, domainConfig.getWebServiceDescription().get(ValidationConstants.INPUT_EMBEDDING_METHOD)));
        UsageEnumeration usage = UsageEnumeration.O;
        if(domainConfig.hasMultipleValidationTypes() && domainConfig.getDefaultType() == null) {
            usage = UsageEnumeration.R;
        }
        response.getModule().getInputs().getParam().add(Utils.createParameter(ValidationConstants.INPUT_VALIDATION_TYPE, "string", usage, domainConfig.getWebServiceDescription().get(ValidationConstants.INPUT_VALIDATION_TYPE)));
        boolean allowsExternalSchemas = definesTypeWithExternalSchemas();
        if (allowsExternalSchemas) {
            response.getModule().getInputs().getParam().add(Utils.createParameter(ValidationConstants.INPUT_EXTERNAL_SCHEMAS, "list[map]", UsageEnumeration.O, domainConfig.getWebServiceDescription().get(ValidationConstants.INPUT_EXTERNAL_SCHEMAS)));
            response.getModule().getInputs().getParam().add(Utils.createParameter(ValidationConstants.INPUT_EXTERNAL_SCHEMA_COMBINATION_APPROACH, "boolean", UsageEnumeration.O, domainConfig.getWebServiceDescription().get(ValidationConstants.INPUT_EXTERNAL_SCHEMA_COMBINATION_APPROACH)));
        }
        response.getModule().getInputs().getParam().add(Utils.createParameter(ValidationConstants.INPUT_LOCATION_AS_POINTER, "boolean", UsageEnumeration.O, domainConfig.getWebServiceDescription().get(ValidationConstants.INPUT_LOCATION_AS_POINTER)));
        response.getModule().getInputs().getParam().add(Utils.createParameter(ValidationConstants.INPUT_ADD_INPUT_TO_REPORT, "boolean", UsageEnumeration.O, ConfigurationType.SIMPLE, domainConfig.getWebServiceDescription().get(ValidationConstants.INPUT_ADD_INPUT_TO_REPORT)));
        response.getModule().getInputs().getParam().add(Utils.createParameter(ValidationConstants.INPUT_LOCALE, "string", UsageEnumeration.O, ConfigurationType.SIMPLE, domainConfig.getWebServiceDescription().get(ValidationConstants.INPUT_LOCALE)));
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
        var localiser = new LocalisationHelper(domainConfig, Utils.getSupportedLocale(LocaleUtils.toLocale(getInputAsString(validateRequest, ValidationConstants.INPUT_LOCALE, null)), domainConfig));
    	try {
			// Validation of the input data
			ValueEmbeddingEnumeration contentEmbeddingMethod = inputHelper.validateContentEmbeddingMethod(validateRequest, ValidationConstants.INPUT_EMBEDDING_METHOD);
			boolean locationAsPointer = getInputAsBoolean(validateRequest, ValidationConstants.INPUT_LOCATION_AS_POINTER, false);
            boolean addInputToReport = getInputAsBoolean(validateRequest, ValidationConstants.INPUT_ADD_INPUT_TO_REPORT, true);
            File contentToValidate = inputHelper.validateContentToValidate(validateRequest, ValidationConstants.INPUT_CONTENT, contentEmbeddingMethod, null, tempFolderPath, domainConfig.getHttpVersion()).getFile();
            String validationType = inputHelper.validateValidationType(domainConfig, validateRequest, ValidationConstants.INPUT_VALIDATION_TYPE);
            List<FileInfo> externalSchemas = inputHelper.validateExternalArtifacts(domainConfig, validateRequest, ValidationConstants.INPUT_EXTERNAL_SCHEMAS, ValidationConstants.INPUT_EXTERNAL_SCHEMAS_SCHEMA, ValidationConstants.INPUT_EMBEDDING_METHOD, validationType, null, tempFolderPath);
            ValidationArtifactCombinationApproach externalSchemaCombinationApproach = validateExternalSchemaCombinationApproach(validateRequest, validationType);
            ValidationResponse result = new ValidationResponse();
			// Execute validation
            var builder = ValidationSpecs.builder(contentToValidate, localiser, domainConfig)
                    .withValidationType(validationType)
                    .withExternalSchemas(externalSchemas, externalSchemaCombinationApproach);
            if (locationAsPointer) builder = builder.locationAsPointer();
            if (addInputToReport) builder = builder.addInputToReport();
            JSONValidator validator = ctx.getBean(JSONValidator.class, builder.build());
			result.setReport(validator.validate().getDetailedReport());
			return result;
        } catch (ValidatorException e) {
            logger.error(e.getMessageForLog(), e);
            throw new ValidatorException(e.getMessageForDisplay(localiser), true);
		} catch (Exception e) {
			logger.error("Unexpected error", e);
            var message = localiser.localise(ValidatorException.MESSAGE_DEFAULT);
            throw new ValidatorException(message, e, true, (Object[]) null);
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
        String approach = null;
        if (!inputs.isEmpty()) {
            approach = inputs.get(0).getValue();
        }
        return inputHelper.getValidationArtifactCombinationApproach(domainConfig, validationType, approach);
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
     * Get the provided (optional) input as a string value.
     *
     * @param validateRequest The input parameters.
     * @param inputName The name of the input to look for.
     * @param defaultIfMissing The default value to use if the input is not provided.
     * @return The value to use.
     */
    private String getInputAsString(ValidateRequest validateRequest, String inputName, String defaultIfMissing) {
        List<AnyContent> input = Utils.getInputFor(validateRequest, inputName);
        if (!input.isEmpty()) {
            return input.get(0).getValue();
        }
        return defaultIfMissing;
    }

    /**
     * @return The web service context.
     */
    @Override
    public WebServiceContext getWebServiceContext() {
        return this.wsContext;
    }    

}
