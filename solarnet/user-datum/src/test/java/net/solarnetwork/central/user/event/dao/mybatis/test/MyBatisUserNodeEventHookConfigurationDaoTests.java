/* ==================================================================
 * MyBatisUserNodeEventHookConfigurationDaoTests.java - 3/06/2020 4:44:25 pm
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

package net.solarnetwork.central.user.event.dao.mybatis.test;

import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.central.user.domain.User;
import net.solarnetwork.central.user.domain.UserLongPK;
import net.solarnetwork.central.user.event.dao.mybatis.MyBatisUserNodeEventHookConfigurationDao;
import net.solarnetwork.central.user.event.domain.UserNodeEventHookConfiguration;

/**
 * Test cases for the {@link MyBatisUserNodeEventHookConfigurationDao} class.
 * 
 * @author matt
 * @version 1.0
 */
public class MyBatisUserNodeEventHookConfigurationDaoTests
		extends AbstractMyBatisUserEventDaoTestSupport {

	private static final String TEST_SERVICE_ID = "test.service";
	private static final String TEST_TOPIC = "test.topic";
	private static final String TEST_SOURCE_ID = "test.source";
	private static final String TEST_EMAIL = "test@localhost";
	private static final String TEST_CONFIG_NAME = "test.config";

	private MyBatisUserNodeEventHookConfigurationDao dao;

	private User user;
	private UserNodeEventHookConfiguration last;

	@Before
	public void setUp() throws Exception {
		dao = new MyBatisUserNodeEventHookConfigurationDao();
		dao.setSqlSessionFactory(getSqlSessionFactory());

		setupTestNode();
		this.user = createNewUser(TEST_EMAIL);

		last = null;
	}

	@Test
	public void storeNew() {
		final Map<String, Object> props = new LinkedHashMap<>();
		props.put("foo", "bar");

		final UserNodeEventHookConfiguration hook = new UserNodeEventHookConfiguration(user.getId(),
				Instant.now());
		hook.setName(TEST_CONFIG_NAME);
		hook.setNodeIds(new Long[] { TEST_NODE_ID });
		hook.setSourceIds(new String[] { TEST_SOURCE_ID });
		hook.setTopic(TEST_TOPIC);
		hook.setServiceProps(props);
		hook.setServiceIdentifier(TEST_SERVICE_ID);

		UserLongPK savedKey = dao.save(hook);
		assertThat("Primary key returned", savedKey, notNullValue());
		assertThat("Primary key user ID", savedKey.getUserId(), equalTo(user.getId()));
		assertThat("Primary key ID generated", savedKey.getId(), notNullValue());

		// stash results for other tests to use
		this.last = hook;
	}

	@Test
	public void getByPrimaryKey() {
		storeNew();

		UserNodeEventHookConfiguration hook = dao.get(last.getId());
		assertThat("Found by PK", hook, notNullValue());
		assertThat("PK", hook.getId(), equalTo(last.getId()));
		assertThat("Properties same", hook.getServiceProperties(), equalTo(last.getServiceProperties()));
		assertThat("Topic same", hook.getTopic(), equalTo(last.getTopic()));
		assertThat("Service ID same", hook.getServiceIdentifier(), equalTo(last.getServiceIdentifier()));
		assertThat("Name same", hook.getName(), equalTo(last.getName()));
		assertThat("Node IDs same", hook.getNodeIds(), arrayContaining(TEST_NODE_ID));
		assertThat("Source IDs same", hook.getSourceIds(), arrayContaining(TEST_SOURCE_ID));

		// stash results for other tests to use
		this.last = hook;
	}

	@Test
	public void update() {
		storeNew();

		final Map<String, Object> props = new LinkedHashMap<>();
		props.put("bim", "bam");
		UserNodeEventHookConfiguration hook = dao.get(last.getId());
		hook.setServiceProps(props);
		hook.setNodeIds(null); // all nodes
		hook.setSourceIds(new String[] { "foo", "bar" });

		UserLongPK savedKey = dao.save(hook);
		assertThat("PK unchanged", savedKey, equalTo(last.getId()));

		UserNodeEventHookConfiguration updated = dao.get(last.getId());
		assertThat("Found by PK", updated, notNullValue());
		assertThat("New entity returned", updated, not(sameInstance(hook)));
		assertThat("PK", updated.getId(), equalTo(hook.getId()));
		assertThat("Properties changed", updated.getServiceProperties(), equalTo(props));
		assertThat("Topic same", hook.getTopic(), equalTo(last.getTopic()));
		assertThat("Service ID same", hook.getServiceIdentifier(), equalTo(last.getServiceIdentifier()));
		assertThat("Name same", hook.getName(), equalTo(last.getName()));
		assertThat("Node IDs changed", hook.getNodeIds(), nullValue());
		assertThat("Source IDs changed", hook.getSourceIds(), arrayContaining("foo", "bar"));
	}

	@Test
	public void findAllForUser() {
		List<UserNodeEventHookConfiguration> confs = new ArrayList<>(3);
		for ( int i = 0; i < 3; i++ ) {
			storeNew();
			confs.add(this.last);
		}

		List<UserNodeEventHookConfiguration> found = dao.findConfigurationsForUser(this.user.getId());
		assertThat(found, not(sameInstance(confs)));
		assertThat(found, equalTo(confs));
	}

}
