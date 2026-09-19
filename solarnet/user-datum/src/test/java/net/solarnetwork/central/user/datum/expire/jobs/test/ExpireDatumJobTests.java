/* ==================================================================
 * ExpireDatumJobTests.java - 22/04/2026 11:31:39 am
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

package net.solarnetwork.central.user.datum.expire.jobs.test;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.JSON;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.json;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomSourceId;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static org.assertj.core.api.BDDAssertions.and;
import static org.assertj.core.api.BDDAssertions.from;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.BDDMockito.given;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import net.solarnetwork.central.biz.InMemoryUserEventAppenderBiz;
import net.solarnetwork.central.datum.domain.DatumFilterCommand;
import net.solarnetwork.central.domain.UserEvent;
import net.solarnetwork.central.user.datum.expire.dao.ExpireUserDataConfigurationDao;
import net.solarnetwork.central.user.datum.expire.domain.DatumExpireUserEvents;
import net.solarnetwork.central.user.datum.expire.domain.ExpireUserDataConfiguration;
import net.solarnetwork.central.user.datum.expire.jobs.ExpireDatumJob;
import net.solarnetwork.codec.jackson.JsonUtils;

/**
 * Test cases for the {@link DatumExpireJob} class.
 * 
 * @author matt
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
@SuppressWarnings("static-access")
public class ExpireDatumJobTests implements DatumExpireUserEvents {

	@Mock
	private ExpireUserDataConfigurationDao configDao;

	private InMemoryUserEventAppenderBiz userEventAppenderBiz;
	private ExpireDatumJob job;

	@BeforeEach
	public void setup() {
		userEventAppenderBiz = new InMemoryUserEventAppenderBiz();
		job = new ExpireDatumJob(configDao);
		job.setUserEventAppenderBiz(userEventAppenderBiz);
	}

	@Test
	public void run_oneJob() {
		// GIVEN
		final Long userId = randomLong();
		ExpireUserDataConfiguration config = new ExpireUserDataConfiguration(randomLong(), userId,
				Instant.now(), randomString(), randomString());
		config.setActive(true);

		var filter = new DatumFilterCommand();
		filter.setNodeId(randomLong());
		filter.setSourceId(randomSourceId());
		config.setFilter(filter);

		given(configDao.getAll(null)).willReturn(List.of(config));

		final long deleteCount = randomLong();
		given(configDao.deleteExpiredDataForConfiguration(same(config))).willReturn(deleteCount);

		// WHEN
		job.run();

		// THEN
		thenStartEndEventsGenerated(config, deleteCount);
	}

	private void thenStartEndEventsGenerated(ExpireUserDataConfiguration info, long deleteCount) {
		// @formatter:off
		and.then(userEventAppenderBiz.getEvents())
			.as("Events for export start/end created")
			.hasSize(2)
			.allSatisfy(evt -> {
				and.then(evt)
					.as("Event for export user")
					.returns(info.getUserId(), from(UserEvent::getUserId))
					;
			})
			.satisfies(evts -> {
				and.then(evts).element(0)
					.as("Datum expire tags provided in event")
					.returns(DATUM_EXPIRE_TAGS.toArray(String[]::new), from(UserEvent::getTags))
					.as("Message generated")
					.returns("Expire datum", from(UserEvent::getMessage))
					.extracting(UserEvent::getData, JSON)
					.as("Job data provided for expire start")
					.isObject()
					.isEqualTo(json(JsonUtils.getJSONString(Map.of(
							CONFIG_ID_DATA_KEY, info.getId(),
							CONFIGURATION_DATA_KEY, JsonUtils.getStringMap(info.getFilterJson()),
							EXPIRE_DAYS_DATA_KEY, info.getExpireDays()
							)))
					)
					;
				and.then(evts).element(1)
					.as("Datum expire tags provided in event")
					.returns(DATUM_EXPIRE_TAGS.toArray(String[]::new), from(UserEvent::getTags))
					.as("Message generated")
					.returns("Expire datum end", from(UserEvent::getMessage))
					.extracting(UserEvent::getData, JSON)
					.as("Job data provided for expire end")
					.isObject()
					.isEqualTo(json(JsonUtils.getJSONString(Map.of(
							CONFIG_ID_DATA_KEY, info.getId(),
							DATUM_COUNT_DATA_KEY, deleteCount
						)))
					)
					;
			})
			;
		// @formatter:on
	}

}
