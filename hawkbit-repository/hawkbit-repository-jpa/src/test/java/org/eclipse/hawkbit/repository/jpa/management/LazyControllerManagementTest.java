/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.management;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.TimeUnit;

import org.eclipse.hawkbit.repository.RepositoryProperties;
import org.eclipse.hawkbit.repository.event.remote.TargetPollEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetCreatedEvent;
import org.eclipse.hawkbit.repository.jpa.AbstractJpaIntegrationTest;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.test.matcher.Expect;
import org.eclipse.hawkbit.repository.test.matcher.ExpectEvents;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

/**
 * Feature: Component Tests - Repository<br/>
 * Story: Controller Management
 */
@TestPropertySource(locations = "classpath:/jpa-test.properties", properties = {
        "hawkbit.server.repository.eagerPollPersistence=false",
        "hawkbit.server.repository.pollPersistenceFlushTime=1000" })
class LazyControllerManagementTest extends AbstractJpaIntegrationTest {

    @Autowired
    private RepositoryProperties repositoryProperties;

    /**
     * Verifies that lazy target poll update is executed as specified.
     */
    @Test
    @ExpectEvents({
            @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = TargetPollEvent.class, count = 2) })
    void lazyFindOrRegisterTargetIfItDoesNotExist() throws InterruptedException {
        final Target target = controllerManagement.findOrRegisterTargetIfItDoesNotExist("AA", LOCALHOST);
        assertThat(target).as("target should not be null").isNotNull();

        TimeUnit.MILLISECONDS.sleep(10);
        controllerManagement.findOrRegisterTargetIfItDoesNotExist("AA", LOCALHOST);
        TimeUnit.MILLISECONDS.sleep(repositoryProperties.getPollPersistenceFlushTime() + 10);

        final Target updated = targetManagement.get(target.getId()).get();

        assertThat(updated.getOptLockRevision()).isEqualTo(target.getOptLockRevision());
        assertThat(updated.getLastTargetQuery()).isGreaterThan(target.getLastTargetQuery());
    }
}
