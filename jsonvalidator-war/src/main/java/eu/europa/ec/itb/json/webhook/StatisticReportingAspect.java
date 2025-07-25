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

package eu.europa.ec.itb.json.webhook;

import com.gitb.tr.TAR;
import com.gitb.tr.TestResultType;
import eu.europa.ec.itb.json.validation.JSONValidator;
import eu.europa.ec.itb.validation.commons.ReportPair;
import eu.europa.ec.itb.validation.commons.war.webhook.StatisticReporting;
import eu.europa.ec.itb.validation.commons.war.webhook.StatisticReportingConstants;
import eu.europa.ec.itb.validation.commons.war.webhook.UsageData;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Aspect that advises the application's entry points to extract and send usage statistics (if enabled).
 */
@Aspect
@Component
@ConditionalOnProperty(name = "validator.webhook.statistics")
public class StatisticReportingAspect extends StatisticReporting {

    private static final Logger logger = LoggerFactory.getLogger(StatisticReportingAspect.class);

    /**
     * Pointcut for minimal WEB validation.
     */
    @Pointcut("execution(public * eu.europa.ec.itb.json.web.UploadController.handleUploadMinimal(..))")
    private void minimalUploadValidation() {}

    /**
     * Pointcut for regular WEB validation.
     */
    @Pointcut("execution(public * eu.europa.ec.itb.json.web.UploadController.handleUpload(..))")
    private void uploadValidation() {}

    /**
     * Pointcut for minimal WEB validation.
     */
    @Pointcut("execution(public * eu.europa.ec.itb.json.web.UploadController.handleUploadMinimalEmbedded(..))")
    private void minimalEmbeddedUploadValidation(){}

    /**
     * Pointcut for regular WEB validation.
     */
    @Pointcut("execution(public * eu.europa.ec.itb.json.web.UploadController.handleUploadEmbedded(..))")
    private void embeddedUploadValidation(){}

    /**
     * Advice to obtain the arguments passed to the web upload API call (minimal UI).
     *
     * @param joinPoint The original call's information.
     */
    @Before("minimalUploadValidation() || minimalEmbeddedUploadValidation()")
    public void getUploadMinimalContext(JoinPoint joinPoint) {
        handleUploadContext(joinPoint, StatisticReportingConstants.WEB_MINIMAL_API);
    }

    /**
     * Advice to obtain the arguments passed to the web upload API call.
     *
     * @param joinPoint The original call's information.
     */
    @Before("uploadValidation() || embeddedUploadValidation()")
    public void getUploadContext(JoinPoint joinPoint) {
        handleUploadContext(joinPoint, StatisticReportingConstants.WEB_API);
    }

    /**
     * Advice to obtain the arguments passed to the SOAP API call.
     *
     * @param joinPoint The original call's information.
     */
    @Before(value = "execution(public * eu.europa.ec.itb.json.gitb.ValidationServiceImpl.validate(..))")
    public void getSoapCallContext(JoinPoint joinPoint) {
        handleSoapCallContext(joinPoint);
    }

    /**
     * Pointcut for the single REST validation.
     */
    @Pointcut("execution(public * eu.europa.ec.itb.json.rest.RestValidationController.validate(..))")
    private void singleRestValidation(){}

    /**
     * Pointcut for batch REST validation.
     */
    @Pointcut("execution(public * eu.europa.ec.itb.json.rest.RestValidationController.validateMultiple(..))")
    private void multipleRestValidation(){}

    /**
     * Advice to obtain the arguments passed to the REST API call.
     *
     * @param joinPoint The original call's information.
     */
    @Before("singleRestValidation() || multipleRestValidation()")
    public void getRestCallContext(JoinPoint joinPoint) {
        handleRestCallContext(joinPoint);
    }

    /**
     * Advice to send the usage report.
     *
     * @param joinPoint The original call's information.
     */
    @Around("execution(public * eu.europa.ec.itb.json.validation.JSONValidator.validate(..))")
    public Object reportValidatorDataUsage(ProceedingJoinPoint joinPoint) throws Throwable {
        JSONValidator validator = (JSONValidator) joinPoint.getTarget();
        Object report = joinPoint.proceed();
        try {
            Map<String, String> usageParams = getAdviceContext();
            String validatorId = config.getIdentifier();
            String domain = validator.getDomain();
            String validationType = validator.getValidationType();
            String api = usageParams.get("api");
            // obtain the result of the model
            String ip = usageParams.get("ip");
            ReportPair reportTARs = (ReportPair) report;
            UsageData.Result result = extractResult(reportTARs.getDetailedReport());
            // Send the usage data
            sendUsageData(validatorId, domain, api, validationType, result, ip);
        } catch (Exception ex) {
            // Ensure unexpected errors never block validation processing
            logger.warn("Unexpected error during statistics reporting", ex);
        }
        return report;
    }

    /**
     * Method that obtains a TAR object and obtains the result of the validation to
     * be reported.
     *
     * @param report The report to consider.
     * @return The validation result.
     */
    private UsageData.Result extractResult(TAR report) {
        TestResultType tarResult = report.getResult();
        if (tarResult == TestResultType.SUCCESS) {
            return UsageData.Result.SUCCESS;
        } else if (tarResult == TestResultType.WARNING) {
            return UsageData.Result.WARNING;
        } else {
            return UsageData.Result.FAILURE;
        }
    }

}
