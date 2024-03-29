/* ==================================================================
 * JdbcUserEventDaoTests.java - 3/08/2022 11:37:27 am
 * 
 * Copyright 2022 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.common.dao.jdbc.test;

import static java.util.stream.Collectors.toSet;
import static net.solarnetwork.codec.JsonUtils.getStringMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasItemInArray;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import java.io.IOException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.threeten.extra.MutableClock;
import net.solarnetwork.central.common.dao.BasicUserEventFilter;
import net.solarnetwork.central.common.dao.jdbc.JdbcUserEventDao;
import net.solarnetwork.central.domain.UserEvent;
import net.solarnetwork.central.domain.UserUuidPK;
import net.solarnetwork.central.support.AbstractFilteredResultsProcessor;
import net.solarnetwork.central.test.AbstractJUnit5JdbcDaoTestSupport;
import net.solarnetwork.central.test.CommonDbTestUtils;
import net.solarnetwork.dao.FilterResults;
import net.solarnetwork.util.TimeBasedV7UuidGenerator;
import net.solarnetwork.util.UuidGenerator;

/**
 * Test cases for the {@link JdbcUserEventDao} class.
 * 
 * @author matt
 * @version 1.1
 */
public class JdbcUserEventDaoTests extends AbstractJUnit5JdbcDaoTestSupport {

	private MutableClock clock;
	private JdbcUserEventDao dao;
	private Long userId;
	private UuidGenerator uuidGenerator;

	@BeforeEach
	public void setup() {
		clock = MutableClock.of(Instant.now().truncatedTo(ChronoUnit.HOURS), ZoneOffset.UTC);
		dao = new JdbcUserEventDao(jdbcTemplate);
		userId = CommonDbTestUtils.insertUser(jdbcTemplate);
		uuidGenerator = new TimeBasedV7UuidGenerator(new SecureRandom(), clock);
	}

	private List<Map<String, Object>> allUserEventData() {
		List<Map<String, Object>> data = jdbcTemplate
				.queryForList("select * from solaruser.user_event_log ORDER BY user_id, event_id");
		log.debug("solarnet.user_event_log table has {} items: [{}]", data.size(),
				data.stream().map(Object::toString).collect(Collectors.joining("\n\t", "\n\t", "\n")));
		return data;
	}

	@Test
	public void insert() {
		// GIVEN
		UserEvent event = new UserEvent(userId, uuidGenerator.generate(),
				new String[] { "foo", "bar", "bam" }, "Test obj", "{\"foo\":123}");

		// WHEN
		dao.add(event);

		// THEN
		List<Map<String, Object>> data = allUserEventData();
		assertThat("Table has 1 row", data, hasSize(1));
		Map<String, Object> row = data.get(0);
		assertThat("Row user ID matches", row, hasEntry("user_id", event.getUserId()));
		assertThat("Row event ID matches", row, hasEntry("event_id", event.getEventId()));
		assertThat("Row message matches", row.get("message"), is(equalTo(event.getMessage())));
		assertThat("Row jdata matches", getStringMap(row.get("jdata").toString()),
				is(equalTo(getStringMap(event.getData()))));
	}

	@Test
	public void find_tags() {
		// GIVEN
		final Instant start = clock.instant();
		final int groupSize = 4;
		final int count = groupSize * 4;
		final List<UserEvent> events = new ArrayList<>(count);
		for ( int i = 0; i < count; i++ ) {
			String[] tags;
			switch (i % groupSize) {
				case 0:
					tags = new String[] { "a", "b", "c" };
					break;
				case 1:
					tags = new String[] { "a", "b", "d" };
					break;
				case 2:
					tags = new String[] { "a", "e", "f" };
					break;
				default:
					tags = new String[] { "g", "h", "i" };
					break;
			}
			UserEvent event = new UserEvent(userId, uuidGenerator.generate(), tags, null, null);
			dao.add(event);
			events.add(event);
			clock.add(1, ChronoUnit.SECONDS);
		}

		allUserEventData();

		// WHEN
		BasicUserEventFilter f = new BasicUserEventFilter();
		f.setUserId(userId);
		f.setStartDate(start.plusSeconds(4));
		f.setEndDate(start.plusSeconds(count - 4));

		f.setTags(new String[] { "a", "b" });
		FilterResults<UserEvent, UserUuidPK> result = dao.findFiltered(f);
		assertThat("Results returned for query", result, is(notNullValue()));
		assertThat("Results with a and b returned", result.getReturnedResultCount(), is(equalTo(4)));
	}

	@Test
	public void find_tags_stream() throws IOException {
		// GIVEN
		final Instant start = clock.instant();
		final int groupSize = 4;
		final int count = groupSize * 4;
		final List<UserEvent> events = new ArrayList<>(count);
		for ( int i = 0; i < count; i++ ) {
			String[] tags;
			switch (i % groupSize) {
				case 0:
					tags = new String[] { "a", "b", "c" };
					break;
				case 1:
					tags = new String[] { "a", "b", "d" };
					break;
				case 2:
					tags = new String[] { "a", "e", "f" };
					break;
				default:
					tags = new String[] { "g", "h", "i" };
					break;
			}
			UserEvent event = new UserEvent(userId, uuidGenerator.generate(), tags, null, null);
			dao.add(event);
			events.add(event);
			clock.add(1, ChronoUnit.SECONDS);
		}

		allUserEventData();

		// WHEN
		BasicUserEventFilter f = new BasicUserEventFilter();
		f.setUserId(userId);
		f.setStartDate(start.plusSeconds(4));
		f.setEndDate(start.plusSeconds(count - 4));

		f.setTags(new String[] { "a", "b" });
		List<UserEvent> results = new ArrayList<>(4);
		dao.findFilteredStream(f, new AbstractFilteredResultsProcessor<UserEvent>() {

			@Override
			public void handleResultItem(UserEvent resultItem) throws IOException {
				results.add(resultItem);
			}

		});
		assertThat("Results with a and b returned", results, hasSize(4));
		for ( UserEvent event : results ) {
			assertThat("Results has both a and b tags", event.getTags(),
					allOf(hasItemInArray("a"), hasItemInArray("b")));
		}
	}

	@Test
	public void delete_userOlderThan() {
		// GIVEN
		final Instant start = clock.instant();
		final int userCount = 3;
		final int eventCount = 5;
		final int count = userCount * eventCount;
		final List<UserEvent> events = new ArrayList<>(count);
		final Long[] userIds = new Long[userCount];
		for ( int i = 0; i < eventCount; i++ ) {
			for ( int j = 0; j < userCount; j++ ) {
				if ( i == 0 ) {
					userIds[j] = CommonDbTestUtils.insertUser(jdbcTemplate);
				}
				UserEvent event = new UserEvent(userIds[j], uuidGenerator.generate(),
						new String[] { "foo", "bar" }, null, null);
				dao.add(event);
				events.add(event);
			}
			clock.add(1, ChronoUnit.SECONDS);
		}

		allUserEventData();

		// WHEN
		BasicUserEventFilter f = new BasicUserEventFilter();
		f.setUserId(userIds[0]);
		f.setEndDate(start.plusSeconds(eventCount - 3));
		long result = dao.purgeEvents(f);

		assertThat("Deleted count", result, is(equalTo(2L)));

		UUID[] expectedUuids = events.stream().filter(e -> {
			return (!e.getUserId().equals(userIds[0]) || !e.getCreated().isBefore(f.getEndDate()));
		}).map(UserEvent::getEventId).toArray(UUID[]::new);
		assertThat("Remaining event IDs",
				allUserEventData().stream().map(e -> (UUID) e.get("event_id")).collect(toSet()),
				containsInAnyOrder(expectedUuids));
	}

	@Test
	public void find_searchFilter() {
		// GIVEN
		final int groupSize = 4;
		final int count = groupSize * 4;
		final List<UserEvent> events = new ArrayList<>(count);
		final List<UserEvent> expected = new ArrayList<>(5);
		for ( int i = 0; i < count; i++ ) {
			String[] tags;
			switch (i % groupSize) {
				case 0:
					tags = new String[] { "a", "b", "c" };
					break;
				case 1:
					tags = new String[] { "a", "b", "d" };
					break;
				case 2:
					tags = new String[] { "a", "e", "f" };
					break;
				default:
					tags = new String[] { "g", "h", "i" };
					break;
			}
			UserEvent event = new UserEvent(userId, uuidGenerator.generate(), tags, null,
					"{\"count\":%d}".formatted(i));
			dao.add(event);
			events.add(event);
			if ( i > 0 && i < 6 ) {
				expected.add(event);
			}
			clock.add(1, ChronoUnit.SECONDS);
		}

		allUserEventData();

		// WHEN
		BasicUserEventFilter f = new BasicUserEventFilter();
		f.setUserId(userId);
		f.setSearchFilter("(&(count>=1)(count<6))");
		FilterResults<UserEvent, UserUuidPK> result = dao.findFiltered(f);
		assertThat("Results returned for query", result, is(notNullValue()));
		assertThat("Results with count 1..5 returned", result.getReturnedResultCount(), is(equalTo(5)));
		assertThat("Results as expected", StreamSupport.stream(result.spliterator(), false).toList(),
				contains(expected.toArray(UserEvent[]::new)));
	}

}
