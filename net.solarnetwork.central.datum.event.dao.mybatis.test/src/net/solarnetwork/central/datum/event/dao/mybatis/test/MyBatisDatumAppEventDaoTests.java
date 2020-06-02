/* ==================================================================
 * MyBatisDatumAppEventDaoTests.java - 2/06/2020 4:54:41 pm
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

package net.solarnetwork.central.datum.event.dao.mybatis.test;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.central.datum.dao.DatumAppEventEntity;
import net.solarnetwork.central.datum.dao.DatumAppEventKey;
import net.solarnetwork.central.datum.event.dao.mybatis.MyBatisDatumAppEventDao;

/**
 * Test cases for the {@link MyBatisDatumAppEventDao} class.
 * 
 * @author matt
 * @version 1.0
 */
public class MyBatisDatumAppEventDaoTests extends AbstractMyBatisDatumEventDaoTestSupport {

	private static final String TEST_TOPIC = "test.topic";
	private static final String TEST_SOURCE_ID = "test.source";

	private MyBatisDatumAppEventDao dao;

	private DatumAppEventEntity last;

	@Before
	public void setUp() throws Exception {
		dao = new MyBatisDatumAppEventDao();
		dao.setSqlSessionFactory(getSqlSessionFactory());

		last = null;
	}

	@Test
	public void storeNew() {
		final DatumAppEventKey key = new DatumAppEventKey(TEST_TOPIC, Instant.now(), TEST_NODE_ID,
				TEST_SOURCE_ID);
		final Map<String, Object> props = new LinkedHashMap<>();
		props.put("foo", "bar");
		final DatumAppEventEntity event = new DatumAppEventEntity(key, props);

		DatumAppEventKey savedKey = dao.save(event);
		assertThat("Primary key returned", savedKey, notNullValue());
		assertThat("Primary key matches", savedKey, equalTo(key));

		// stash results for other tests to use
		this.last = event;
	}

	@Test
	public void getByPrimaryKey() {
		storeNew();

		DatumAppEventEntity event = dao.get(last.getId());
		assertThat("Found by PK", event, notNullValue());
		assertThat("PK", event.getId(), equalTo(last.getId()));
		assertThat("Properties same", event.getEventProperties(), equalTo(last.getEventProperties()));

		// stash results for other tests to use
		this.last = event;
	}

	@Test
	public void update() {
		storeNew();

		final Map<String, Object> props = new LinkedHashMap<>();
		props.put("bim", "bam");
		final DatumAppEventEntity event = new DatumAppEventEntity(last.getId(), props);

		DatumAppEventKey savedKey = dao.save(event);
		assertThat("PK unchanged", savedKey, equalTo(last.getId()));

		DatumAppEventEntity updated = dao.get(last.getId());
		assertThat("Found by PK", updated, notNullValue());
		assertThat("New entity returned", updated, not(sameInstance(event)));
		assertThat("PK", updated.getId(), equalTo(event.getId()));
		assertThat("Properties unchanged (update ignored)", updated.getEventProperties(),
				equalTo(last.getEventProperties()));
	}

}
