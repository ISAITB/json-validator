package eu.europa.ec.itb.json.webhook;

import eu.europa.ec.itb.json.web.Translations;
import eu.europa.ec.itb.json.web.UploadController;
import eu.europa.ec.itb.json.web.UploadResult;
import org.aspectj.lang.JoinPoint;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StatisticReportingAspectTest {

    @Test
    void testUploadPointcut() {
        final boolean[] aspectCalled = {false};
        final boolean[] targetCalled = {false};
        // USe subclasses as AspectJ proxies cannot be made over Mockito mocks and spies.
        var target = new UploadController() {
            @Override
            public UploadResult<Translations> handleUpload(String domain, MultipartFile file, String uri, String string, String validationType, String contentType, String[] externalSchema, MultipartFile[] externalSchemaFiles, String[] externalSchemaUri, String[] externalSchemaString, String combinationType, RedirectAttributes redirectAttributes, HttpServletRequest request, HttpServletResponse response) {
                assertEquals("domain1", domain);
                assertTrue(aspectCalled[0]); // We expect the aspect to have been called before the method.
                // We only want to check if this was called.
                targetCalled[0] = true;
                return null;
            }
        };
        var aspect = new StatisticReportingAspect() {
            @Override
            public void getUploadContext(JoinPoint joinPoint) {
                assertEquals("domain1", joinPoint.getArgs()[0]);
                aspectCalled[0] = true;
            }
        };
        var aspectFactory = new AspectJProxyFactory(target);
        aspectFactory.addAspect(aspect);
        UploadController controller = aspectFactory.getProxy();
        controller.handleUpload("domain1", null, null, null, null, null, null, null, null, null, null, null, null, null);
        assertTrue(aspectCalled[0]);
        assertTrue(targetCalled[0]);
    }

    @Test
    void testMinimalUploadPointcut() {
        final boolean[] aspectCalled = {false};
        final boolean[] targetCalled = {false};
        // USe subclasses as AspectJ proxies cannot be made over Mockito mocks and spies.
        var target = new UploadController() {
            @Override
            public UploadResult<Translations> handleUploadMinimal(String domain, MultipartFile file, String uri, String string, String validationType, String contentType, String[] externalSchema, MultipartFile[] externalSchemaFiles, String[] externalSchemaUri, String[] externalSchemaString, String combinationType, RedirectAttributes redirectAttributes, HttpServletRequest request, HttpServletResponse response) {
                assertEquals("domain1", domain);
                assertTrue(aspectCalled[0]); // We expect the aspect to have been called before the method.
                // We only want to check if this was called.
                targetCalled[0] = true;
                return null;
            }
        };
        var aspect = new StatisticReportingAspect() {
            @Override
            public void getUploadMinimalContext(JoinPoint joinPoint) {
                assertEquals("domain1", joinPoint.getArgs()[0]);
                aspectCalled[0] = true;
            }
        };
        var aspectFactory = new AspectJProxyFactory(target);
        aspectFactory.addAspect(aspect);
        UploadController controller = aspectFactory.getProxy();
        controller.handleUploadMinimal("domain1", null, null, null, null, null, null, null, null, null, null, null, null, null);
        assertTrue(aspectCalled[0]);
        assertTrue(targetCalled[0]);
    }

}
