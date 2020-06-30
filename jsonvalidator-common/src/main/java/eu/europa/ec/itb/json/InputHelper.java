package eu.europa.ec.itb.json;

import eu.europa.ec.itb.json.validation.FileManager;
import eu.europa.ec.itb.validation.commons.BaseInputHelper;
import org.springframework.stereotype.Component;

@Component
public class InputHelper extends BaseInputHelper<FileManager, DomainConfig, ApplicationConfig> {
}
