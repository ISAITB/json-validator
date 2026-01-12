/*
 * Copyright (C) 2026 European Union
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

package eu.europa.ec.itb.json.validation;

import eu.europa.ec.itb.json.DomainConfig;

import java.util.HashSet;
import java.util.Set;

/**
 * Class to record the thread-specific state when resolving a given schema.
 * <p>
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