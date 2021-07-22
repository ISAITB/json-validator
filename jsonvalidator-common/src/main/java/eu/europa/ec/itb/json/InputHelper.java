package eu.europa.ec.itb.json;

import eu.europa.ec.itb.json.validation.FileManager;
import eu.europa.ec.itb.validation.commons.BaseInputHelper;
import org.springframework.stereotype.Component;

/**
 * Component to facilitate the validation and preparation of inputs.
 *
 * This class serves simply as a marked to include the parent component as-is.
 */
@Component
public class InputHelper extends BaseInputHelper<FileManager, DomainConfig, ApplicationConfig> {
}
