/* ==================================================================
 * DaoUserEventAppenderBizTests.java - 20/03/2026 6:45:57 am
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

package net.solarnetwork.central.biz.dao.test;

import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static org.assertj.core.api.BDDAssertions.and;
import static org.assertj.core.api.BDDAssertions.from;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;
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
import net.solarnetwork.central.biz.dao.DaoUserEventAppenderBiz;
import net.solarnetwork.central.common.dao.GenericWriteOnlyDao;
import net.solarnetwork.central.domain.LogEventInfo;
import net.solarnetwork.central.domain.UserEvent;
import net.solarnetwork.central.domain.UserUuidPK;
import net.solarnetwork.util.TimeBasedV7UuidGenerator;

/**
 * Test cases for the {@link DaoUserEventAppenderBiz} class.
 * 
 * @author matt
 * @version 1.0
 */
@SuppressWarnings("static-access")
@ExtendWith(MockitoExtension.class)
public class DaoUserEventAppenderBizTests {

	@Mock
	private GenericWriteOnlyDao<UserEvent, UserUuidPK> dao;

	@Captor
	private ArgumentCaptor<UserEvent> userEventCaptor;

	private DaoUserEventAppenderBiz service;

	@BeforeEach
	public void setup() {
		service = new DaoUserEventAppenderBiz(dao, TimeBasedV7UuidGenerator.INSTANCE_MICROS);
	}

	@Test
	public void addEvent() {
		// GIVEN
		final var userId = randomLong();
		final var logEvent = new LogEventInfo(new String[] { randomString() }, randomString(),
				randomString());

		given(dao.persist(any())).will(inv -> {
			UserEvent evt = inv.getArgument(0);
			return evt.id();
		});

		// WHEN
		UserEvent result = service.addEvent(userId, logEvent);

		// THEN
		// @formatter:off
		then(dao).should().persist(userEventCaptor.capture());
		and.then(userEventCaptor.getValue())
			.as("UserEvent persisted in DAO")
			.isNotNull()
			.as("Same instance persisted in DAO returned")
			.isSameAs(result)
			.as("Given user ID used for user event")
			.returns(userId, from(UserEvent::getUserId))
			.as("Given tags used for user event")
			.returns(logEvent.getTags(), from(UserEvent::getTags))
			.as("Given message used for user event")
			.returns(logEvent.getMessage(), from(UserEvent::getMessage))
			.as("Given data used for user event")
			.returns(logEvent.getData(), from(UserEvent::getData))
			.as("ID generated in user event")
			.returns(true, from(UserEvent::hasId))
			;
		
		and.then(result.getId()).as("UserEvent ID generated").isNotNull();
		// @formatter:on
	}

	@Test
	public void addEvent_nullUserId() {
		// GIVEN
		final var logEvent = new LogEventInfo(new String[] { randomString() }, randomString(),
				randomString());

		// WHEN
		thenThrownBy(() -> {
			service.addEvent(null, logEvent);
		}, "IllegalArgument thrown when user ID is null");
	}

	@Test
	public void addEvent_nullEvent() {
		// GIVEN
		final var userId = randomLong();

		// WHEN
		thenThrownBy(() -> {
			service.addEvent(userId, null);
		}, "IllegalArgument thrown when event is null");
	}

}
