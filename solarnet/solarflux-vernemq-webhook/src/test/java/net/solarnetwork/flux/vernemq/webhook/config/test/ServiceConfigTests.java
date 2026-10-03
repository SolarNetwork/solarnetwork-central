/* ==================================================================
 * ServiceConfigTests.java - 3/10/2026 4:40:12 pm
 *
 * Copyright 2026 SolarNetwork.net Dev Team
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 2 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
 * 02111-1307 USA
 * ==================================================================
 */

package net.solarnetwork.flux.vernemq.webhook.config.test;

import static org.assertj.core.api.BDDAssertions.and;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import net.solarnetwork.central.common.dao.jdbc.JdbcNodeServiceAuditorCount;
import net.solarnetwork.flux.vernemq.webhook.config.ServiceConfig;
import net.solarnetwork.flux.vernemq.webhook.domain.v311.DeliverRequest;
import net.solarnetwork.flux.vernemq.webhook.service.AuthorizationEvaluator;
import net.solarnetwork.flux.vernemq.webhook.service.impl.JdbcAuditService;

/**
 * Test cases for the {@link ServiceConfig} class.
 *
 * @author matt
 * @version 1.0
 */
@SuppressWarnings("static-access")
@ExtendWith(MockitoExtension.class)
public class ServiceConfigTests {

	@Mock
	private DataSource dataSource;

	@Mock
	private Connection jdbcConnection;

	@Mock
	private CallableStatement jdbcStatement;

	@Mock
	private AuthorizationEvaluator authorizationEvaluator;

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(ConfigurationPropertiesAutoConfiguration.class))
			.withUserConfiguration(ServiceConfig.class);

	private static long countsFlushed(JdbcAuditService service) throws Exception {
		return (service.performPingTest().getProperties()
				.get(JdbcNodeServiceAuditorCount.CountsFlushed.name()) instanceof Long n ? n : 0L);
	}

	@Test
	public void auditService_writesRemainingCountsWhenClosed() throws Exception {
		// GIVEN
		given(dataSource.getConnection()).willReturn(jdbcConnection);
		given(jdbcConnection.prepareCall(JdbcAuditService.DEFAULT_SERVICE_INCREMENT_SQL))
				.willReturn(jdbcStatement);

		final DeliverRequest msg = DeliverRequest.builder()
				.withTopic("user/123/event/ocpp/charger/disconnected")
				.withPayload("Hi there!".getBytes()).build();

		// WHEN
		final long[] closeStart = new long[1];
		contextRunner.withBean(DataSource.class, () -> dataSource)
				.withBean(AuthorizationEvaluator.class, () -> authorizationEvaluator)
				// a long delay, so the writer is waiting for its next flush when the count is added
				.withPropertyValues("app.audit.jdbc.flush-delay=600000").run(ctx -> {
					final JdbcAuditService service = ctx.getBean(JdbcAuditService.class);

					// wait for the writer to start its first flush, so the count is added after it
					final long end = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
					while ( countsFlushed(service) < 1 && System.nanoTime() < end ) {
						Thread.sleep(10);
					}

					service.auditDeliverMessage(msg);
					closeStart[0] = System.nanoTime();
				});
		final Duration closeTime = Duration.ofNanos(System.nanoTime() - closeStart[0]);

		// THEN
		then(jdbcStatement).should().setString(1,
				JdbcAuditService.DEFAULT_AUDIT_DELIVER_MQTT_SERVICE_NAME);
		then(jdbcStatement).should().setObject(2, 123L);
		then(jdbcStatement).should().setInt(5, msg.getPayload().length);
		then(jdbcStatement).should().execute();
		then(jdbcConnection).should().close();

		// the count is written with the writer's connection, not another one
		then(dataSource).should().getConnection();

		// @formatter:off
		and.then(closeTime)
			.as("Closing did not wait for the next flush")
			.isLessThan(Duration.ofSeconds(5))
			;
		// @formatter:on
	}

}
