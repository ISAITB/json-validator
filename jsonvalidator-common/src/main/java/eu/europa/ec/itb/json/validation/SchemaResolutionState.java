package eu.europa.ec.itb.json.validation;

import eu.europa.ec.itb.json.DomainConfig;

import java.util.HashSet;
import java.util.Set;

/**
 * Class to record the thread-specific state when resolving a given schema.
 *
 * This state is used for two reasons:
 * - To pass the domain in question.
 * - To ensure that we don't end up with cyclic dependencies that could lead to infinite lookups.
 */
public class SchemaResolutionState {

    private final DomainConfig domain;
    private final Set<String> pendingResolution;

    /**
     * @param domain The domain relevant to the current validation.
     */
    public SchemaResolutionState(DomainConfig domain) {
        this.domain = domain;
        pendingResolution = new HashSet<>();
    }

    /**
     * @return The domain relevant to the current validation.
     */
    public DomainConfig getDomain() {
        return domain;
    }

    /**
     * @return The set of IDs that are already being resolved as part of a single schema read attempt.
     */
    public Set<String> getPendingResolution() {
        return pendingResolution;
    }
}