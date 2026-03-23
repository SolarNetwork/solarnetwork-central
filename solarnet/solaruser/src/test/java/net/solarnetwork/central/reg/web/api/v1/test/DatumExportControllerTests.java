/* ==================================================================
 * DatumExportControllerTests.java - 23/03/2026 4:45:19 pm
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

package net.solarnetwork.central.reg.web.api.v1.test;

import static java.time.Instant.now;
import static net.solarnetwork.central.domain.UserLongCompositePK.UNASSIGNED_ENTITY_ID;
import static net.solarnetwork.central.security.SecurityUtils.becomeUser;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static org.assertj.core.api.BDDAssertions.and;
import static org.assertj.core.api.BDDAssertions.from;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import net.solarnetwork.central.datum.export.domain.OutputCompressionType;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.reg.web.api.v1.DatumExportController;
import net.solarnetwork.central.reg.web.domain.UserDatumExportConfigurationInput;
import net.solarnetwork.central.user.datum.export.biz.UserExportBiz;
import net.solarnetwork.central.user.datum.export.domain.UserAdhocDatumExportTaskInfo;
import net.solarnetwork.central.user.datum.export.domain.UserDatumExportConfiguration;
import net.solarnetwork.central.user.datum.export.domain.UserDestinationConfiguration;
import net.solarnetwork.central.user.datum.export.domain.UserOutputConfiguration;
import net.solarnetwork.codec.jackson.JsonUtils;
import net.solarnetwork.domain.Result;

/**
 * Test cases for the {@link DatumExportController} class.
 *
 * @author matt
 * @version 1.0
 */
@SuppressWarnings("static-access")
@ExtendWith(MockitoExtension.class)
public class DatumExportControllerTests {

	@Mock
	private UserExportBiz userExportBiz;

	@Captor
	private ArgumentCaptor<UserDatumExportConfiguration> exportConfigCaptor;

	private DatumExportController controller;

	@BeforeEach
	public void setup() {
		controller = new DatumExportController(userExportBiz);
	}

	@Test
	public void submitAdHocExport() {
		// GIVEN
		final Long userId = randomLong();
		final String destServiceId = randomString();
		final String outputServiceId = randomString();

		// submit export
		final var taskInfo = new UserAdhocDatumExportTaskInfo(userId);
		given(userExportBiz.saveAdhocDatumExportTaskForConfiguration(any())).willReturn(taskInfo);

		final var input = JsonUtils.JSON_OBJECT_MAPPER.readValue("""
				{
					"name":"Test",
					"dataConfiguration":{
						"datumFilter":{
							"nodeIds":[179],
							"sourceIds":["test/meter/1"],
							"aggregationKey":"30m",
							"startDate":"2026-01-01 00:00",
							"endDate":"2026-02-01 00:00"
						}
					},
					"destinationConfiguration":{
						"name": "Test",
						"serviceIdentifier":"%s"
					},
					"outputConfiguration":{
						"name": "Test",
						"serviceIdentifier":"%s",
						"compressionTypeKey":"n"
					}
				}
				""".formatted(destServiceId, outputServiceId), UserDatumExportConfigurationInput.class);

		// WHEN
		becomeUser(randomString(), null, userId);

		Result<UserAdhocDatumExportTaskInfo> result = controller.submitAdhocExportJobRequest(input);

		// THEN
		// @formatter:off
		then(userExportBiz).should().saveAdhocDatumExportTaskForConfiguration(exportConfigCaptor.capture());

		and.then(exportConfigCaptor.getValue())
			.as("Export configuration submitted")
			.isNotNull()
			.returns(new UserLongCompositePK(userId, UNASSIGNED_ENTITY_ID), from(UserDatumExportConfiguration::getId))
			;

		and.then(result)
			.as("Result provided")
			.isNotNull()
			.satisfies(r -> {
				and.then(r.getData())
					.as("Task info from UserExportBiz returned")
					.isSameAs(taskInfo)
					;
			})
			;
		// @formatter:on
	}

	@Test
	public void submitAdHocReferenceExport() {
		// GIVEN
		final Long userId = randomLong();
		final Long destinationId = randomLong();
		final Long outputId = randomLong();

		// look up destination config
		final var destConfig = new UserDestinationConfiguration(userId, destinationId, now(),
				randomString(), randomString());
		given(userExportBiz.configurationForUser(userId, UserDestinationConfiguration.class,
				destinationId)).willReturn(destConfig);

		// look up output config
		final var outputConfig = new UserOutputConfiguration(userId, outputId, now(), randomString(),
				randomString(), OutputCompressionType.None);
		given(userExportBiz.configurationForUser(userId, UserOutputConfiguration.class, outputId))
				.willReturn(outputConfig);

		// submit export
		final var taskInfo = new UserAdhocDatumExportTaskInfo(userId);
		given(userExportBiz.saveAdhocDatumExportTaskForConfiguration(any())).willReturn(taskInfo);

		final var input = JsonUtils.JSON_OBJECT_MAPPER.readValue("""
				{
					"name":"Test",
					"dataConfiguration":{
						"datumFilter":{
							"nodeIds":[179],
							"sourceIds":["test/meter/1"],
							"aggregationKey":"30m",
							"startDate":"2026-01-01 00:00",
							"endDate":"2026-02-01 00:00"
						}
					},
					"destinationConfigurationId":"%d",
					"outputConfigurationId":"%d"
				}
				""".formatted(destinationId, outputId), UserDatumExportConfigurationInput.class);

		// WHEN
		becomeUser(randomString(), null, userId);

		Result<UserAdhocDatumExportTaskInfo> result = controller
				.submitAdhocExportReferenceJobRequest(input);

		// THEN
		// @formatter:off
		then(userExportBiz).should().saveAdhocDatumExportTaskForConfiguration(exportConfigCaptor.capture());

		and.then(exportConfigCaptor.getValue())
			.as("Export configuration submitted")
			.isNotNull()
			.returns(new UserLongCompositePK(userId, UNASSIGNED_ENTITY_ID), from(UserDatumExportConfiguration::getId))
			;

		and.then(result)
			.as("Result provided")
			.isNotNull()
			.satisfies(r -> {
				and.then(r.getData())
					.as("Task info from UserExportBiz returned")
					.isSameAs(taskInfo)
					;
			})
			;
		// @formatter:on
	}

}
