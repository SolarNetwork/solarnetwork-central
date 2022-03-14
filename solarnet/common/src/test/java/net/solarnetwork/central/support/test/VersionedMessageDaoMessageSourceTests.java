/* ==================================================================
 * VersionedMessageDaoMessageSourceTests.java - 25/07/2020 10:23:22 AM
 * 
 * Copyright 2020 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.support.test;

import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Locale;
import java.util.Properties;
import javax.cache.Cache;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.central.dao.VersionedMessageDao;
import net.solarnetwork.central.dao.VersionedMessageDao.VersionedMessages;
import net.solarnetwork.central.support.VersionedMessageDaoMessageSource;

/**
 * Test cases for the {@link VersionedMessageDaoMessageSource} class.
 * 
 * @author matt
 * @version 2.0
 */
public class VersionedMessageDaoMessageSourceTests {

	private VersionedMessageDao dao;
	private Cache<String, VersionedMessages> cache;

	@SuppressWarnings("unchecked")
	@Before
	public void setup() {
		dao = EasyMock.createMock(VersionedMessageDao.class);
		cache = EasyMock.createMock(Cache.class);
	}

	private void replayAll() {
		EasyMock.replay(dao, cache);
	}

	@After
	public void teardown() {
		EasyMock.verify(dao, cache);
	}

	@Test
	public void resolveMessage() {
		// GIVEN
		final String[] bundleNames = new String[] { "foo" };
		final Instant version = LocalDate.of(2020, 1, 1).atStartOfDay(ZoneId.of("UTC")).toInstant();

		final Properties props = new Properties();
		props.put("hello", "world");
		expect(dao.findMessages(version, bundleNames, "en")).andReturn(props);

		// WHEN
		replayAll();
		VersionedMessageDaoMessageSource src = new VersionedMessageDaoMessageSource(dao, bundleNames,
				version, null);
		String result = src.getMessage("hello", null, Locale.ENGLISH);

		// THEN
		assertThat("Message resolved", result, equalTo("world"));
	}

	@Test
	public void resolveMessage_withCache() {
		// GIVEN
		final String[] bundleNames = new String[] { "foo" };
		final Instant version = LocalDate.of(2020, 1, 1).atStartOfDay(ZoneId.of("UTC")).toInstant();
		final String cacheKey = "foo.en." + version.toEpochMilli();

		// try cache first (miss)
		expect(cache.get(cacheKey)).andReturn(null);

		// load from DAO second
		final Properties props = new Properties();
		props.put("hello", "world");
		expect(dao.findMessages(version, bundleNames, "en")).andReturn(props);

		// save to cache
		final Capture<VersionedMessages> verMsgCaptor = new Capture<>();
		expect(cache.putIfAbsent(eq(cacheKey), capture(verMsgCaptor))).andReturn(true);

		// second time cache hit
		expect(cache.get(cacheKey)).andAnswer(new IAnswer<VersionedMessages>() {

			@Override
			public VersionedMessages answer() throws Throwable {
				return verMsgCaptor.getValue();
			}
		});

		// WHEN
		replayAll();
		VersionedMessageDaoMessageSource src = new VersionedMessageDaoMessageSource(dao, bundleNames,
				version, cache);

		// first time cache miss
		String result = src.getMessage("hello", null, Locale.ENGLISH);

		// second time cache hit
		String result2 = src.getMessage("hello", null, Locale.ENGLISH);

		// THEN
		assertThat("Message resolved from DAO", result, equalTo("world"));
		assertThat("Message resolved from cache", result2, equalTo("world"));
	}

	@Test
	public void resolveMessage_fromLanguageParent() {
		// GIVEN
		final String[] bundleNames = new String[] { "foo" };
		final Instant version = LocalDate.of(2020, 1, 1).atStartOfDay(ZoneId.of("UTC")).toInstant();

		final Properties props = new Properties();
		props.put("hello", "world");

		// next try language only (hit)
		expect(dao.findMessages(version, bundleNames, "en")).andReturn(props);

		// first try full locale (miss)
		expect(dao.findMessages(version, bundleNames, "en_US")).andReturn(null);

		// WHEN
		replayAll();
		VersionedMessageDaoMessageSource src = new VersionedMessageDaoMessageSource(dao, bundleNames,
				version, null);
		String result = src.getMessage("hello", null, Locale.US);

		// THEN
		assertThat("Message resolved", result, equalTo("world"));
	}

	@Test
	public void resolveMessage_fromLanguageParent_merged() {
		// GIVEN
		final String[] bundleNames = new String[] { "foo" };
		final Instant version = LocalDate.of(2020, 1, 1).atStartOfDay(ZoneId.of("UTC")).toInstant();

		final Properties baseProps = new Properties();
		baseProps.put("hello", "world");

		final Properties props = new Properties();
		props.put("hello", "USA");

		// next try language only (hit)
		expect(dao.findMessages(version, bundleNames, "en")).andReturn(baseProps);

		// first try full locale (hit)
		expect(dao.findMessages(version, bundleNames, "en_US")).andReturn(props);

		// WHEN
		replayAll();
		VersionedMessageDaoMessageSource src = new VersionedMessageDaoMessageSource(dao, bundleNames,
				version, null);
		String result = src.getMessage("hello", null, Locale.US);

		// THEN
		assertThat("Message resolved", result, equalTo("USA"));
	}

}
