package eu.europa.ec.itb.json.gitb;

import com.gitb.core.UsageEnumeration;
import com.gitb.vs.Void;
import eu.europa.ec.itb.json.DomainConfig;
import eu.europa.ec.itb.json.validation.ValidationConstants;
import eu.europa.ec.itb.validation.commons.artifact.ExternalArtifactSupport;
import eu.europa.ec.itb.validation.commons.artifact.TypedValidationArtifactInfo;
import eu.europa.ec.itb.validation.commons.artifact.ValidationArtifactInfo;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

class ValidationServiceImplTest {

    @Test
    void testGetModuleDefinition() {
        var domainConfig = mock(DomainConfig.class);
        doReturn("domain1").when(domainConfig).getDomain();
        doReturn("service1").when(domainConfig).getWebServiceId();
        doAnswer((Answer<?>) ctx -> {
            var info = Map.of("type1", new TypedValidationArtifactInfo());
            info.get("type1").add(TypedValidationArtifactInfo.DEFAULT_TYPE, new ValidationArtifactInfo());
            info.get("type1").get().setExternalArtifactSupport(ExternalArtifactSupport.NONE);
            return info;
        }).when(domainConfig).getArtifactInfo();
        doReturn(Map.of(
                ValidationConstants.INPUT_CONTENT, "Description of INPUT_CONTENT",
                ValidationConstants.INPUT_EMBEDDING_METHOD, "Description of INPUT_EMBEDDING_METHOD",
                ValidationConstants.INPUT_VALIDATION_TYPE, "Description of INPUT_VALIDATION_TYPE",
                ValidationConstants.INPUT_LOCATION_AS_POINTER, "Description of INPUT_LOCATION_AS_POINTER",
                ValidationConstants.INPUT_ADD_INPUT_TO_REPORT, "Description of INPUT_ADD_INPUT_TO_REPORT",
                ValidationConstants.INPUT_LOCALE, "Description of INPUT_LOCALE"
        )).when(domainConfig).getWebServiceDescription();
        var service = new ValidationServiceImpl(domainConfig);
        var result = service.getModuleDefinition(new Void());
        assertNotNull(result);
        assertNotNull(result.getModule());
        assertEquals("service1", result.getModule().getId());
        assertNotNull(result.getModule().getInputs());
        assertEquals(6, result.getModule().getInputs().getParam().size());
        assertEquals(ValidationConstants.INPUT_CONTENT, result.getModule().getInputs().getParam().get(0).getName());
        assertEquals("Description of INPUT_CONTENT", result.getModule().getInputs().getParam().get(0).getDesc());
        assertEquals(UsageEnumeration.R, result.getModule().getInputs().getParam().get(0).getUse());
        assertEquals(ValidationConstants.INPUT_EMBEDDING_METHOD, result.getModule().getInputs().getParam().get(1).getName());
        assertEquals("Description of INPUT_EMBEDDING_METHOD", result.getModule().getInputs().getParam().get(1).getDesc());
        assertEquals(UsageEnumeration.O, result.getModule().getInputs().getParam().get(1).getUse());
        assertEquals(ValidationConstants.INPUT_VALIDATION_TYPE, result.getModule().getInputs().getParam().get(2).getName());
        assertEquals("Description of INPUT_VALIDATION_TYPE", result.getModule().getInputs().getParam().get(2).getDesc());
        assertEquals(UsageEnumeration.O, result.getModule().getInputs().getParam().get(2).getUse());
        assertEquals(ValidationConstants.INPUT_LOCATION_AS_POINTER, result.getModule().getInputs().getParam().get(3).getName());
        assertEquals("Description of INPUT_LOCATION_AS_POINTER", result.getModule().getInputs().getParam().get(3).getDesc());
        assertEquals(UsageEnumeration.O, result.getModule().getInputs().getParam().get(3).getUse());
        assertEquals("Description of INPUT_ADD_INPUT_TO_REPORT", result.getModule().getInputs().getParam().get(4).getDesc());
        assertEquals(UsageEnumeration.O, result.getModule().getInputs().getParam().get(4).getUse());
        assertEquals("Description of INPUT_LOCALE", result.getModule().getInputs().getParam().get(5).getDesc());
        assertEquals(UsageEnumeration.O, result.getModule().getInputs().getParam().get(5).getUse());
    }

}
