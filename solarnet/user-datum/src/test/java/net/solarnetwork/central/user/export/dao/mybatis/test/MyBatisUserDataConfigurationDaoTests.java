/* ==================================================================
 * MyBatisUserDataConfigurationDaoTests.java - 22/03/2018 7:10:36 AM
 * 
 * Copyright 2018 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.export.dao.mybatis.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertNotNull;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.central.datum.domain.AggregateGeneralNodeDatumFilter;
import net.solarnetwork.central.datum.domain.DatumFilterCommand;
import net.solarnetwork.central.domain.Aggregation;
import net.solarnetwork.central.user.domain.User;
import net.solarnetwork.central.user.export.dao.mybatis.MyBatisUserDataConfigurationDao;
import net.solarnetwork.central.user.export.domain.UserDataConfiguration;

/**
 * Test cases for the {@link MyBatisUserDataConfigurationDao} class.
 * 
 * @author matt
 * @version 2.0
 */
public class MyBatisUserDataConfigurationDaoTests extends AbstractMyBatisUserDaoTestSupport {

	private static final String TEST_NAME = "test.name";
	private static final String TEST_SERVICE_IDENT = "test.ident";

	private MyBatisUserDataConfigurationDao confDao;

	private User user;
	private UserDataConfiguration conf;

	@Before
	public void setUp() throws Exception {
		confDao = new MyBatisUserDataConfigurationDao();
		confDao.setSqlSessionFactory(getSqlSessionFactory());

		this.user = createNewUser(TEST_EMAIL);
		assertNotNull(this.user);
		conf = null;
	}

	@Test
	public void storeNew() {
		UserDataConfiguration conf = new UserDataConfiguration();
		conf.setCreated(Instant.now());
		conf.setUserId(this.user.getId());
		conf.setName(TEST_NAME);
		conf.setServiceIdentifier(TEST_SERVICE_IDENT);

		Map<String, Object> sprops = new HashMap<String, Object>(4);
		sprops.put("string", "foo");
		sprops.put("number", 42);

		List<String> optionList = new ArrayList<String>(4);
		optionList.add("first");
		optionList.add("second");
		sprops.put("list", optionList);

		conf.setServiceProps(sprops);

		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setAggregate(Aggregation.Day);
		filter.setNodeId(TEST_NODE_ID);
		conf.setFilter(filter);

		Long id = confDao.store(conf);
		assertThat("Primary key assigned", id, notNullValue());

		// stash results for other tests to use
		conf.setId(id);
		this.conf = conf;
	}

	@Test
	public void getByPrimaryKey() {
		storeNew();
		UserDataConfiguration conf = confDao.get(this.conf.getId(), this.user.getId());
		assertThat("Found by PK", conf, notNullValue());
		assertThat("PK", conf.getId(), equalTo(this.conf.getId()));
		assertThat("Created", conf.getCreated().truncatedTo(ChronoUnit.MINUTES),
				equalTo(this.conf.getCreated().truncatedTo(ChronoUnit.MINUTES)));
		assertThat("User ID", conf.getUserId(), equalTo(this.user.getId()));
		assertThat("Name", conf.getName(), equalTo(TEST_NAME));
		assertThat("Service identifier", conf.getServiceIdentifier(), equalTo(TEST_SERVICE_IDENT));

		Map<String, ?> sprops = conf.getServiceProperties();
		assertThat("Service props", sprops, notNullValue());
		assertThat("Service props size", sprops.keySet(), hasSize(3));
		assertThat(sprops, hasEntry("string", "foo"));
		assertThat(sprops, hasEntry("number", 42));
		assertThat(sprops, hasEntry("list", Arrays.asList("first", "second")));

		AggregateGeneralNodeDatumFilter filter = conf.getDatumFilter();
		assertThat("Filter", filter, notNullValue());
		assertThat("Filter aggregate", filter.getAggregation(), equalTo(Aggregation.Day));
		assertThat("Filter node ID", filter.getNodeId(), equalTo(TEST_NODE_ID));
	}

	@Test
	public void update() {
		storeNew();
		UserDataConfiguration conf = confDao.get(this.conf.getId(), this.user.getId());

		conf.setName("new.name");
		conf.setServiceIdentifier("new.ident");

		Map<String, Object> options = conf.getServiceProps();
		options.put("string", "updated");
		options.put("added-string", "added");

		DatumFilterCommand filter = conf.getFilter();
		filter.setNodeId((long) Integer.MIN_VALUE);
		filter.setSourceId("test.source");
		conf.setFilter(filter); // necessary to clear cached JSON

		Long id = confDao.store(conf);
		assertThat("PK unchanged", id, equalTo(this.conf.getId()));

		UserDataConfiguration updatedConf = confDao.get(id, this.user.getId());
		assertThat("Found by PK", updatedConf, notNullValue());
		assertThat("New entity returned", updatedConf, not(sameInstance(conf)));
		assertThat("PK", updatedConf.getId(), equalTo(conf.getId()));
		assertThat("Created unchanged", updatedConf.getCreated(), equalTo(conf.getCreated()));
		assertThat("Uesr ID", updatedConf.getUserId(), equalTo(conf.getUserId()));
		assertThat("Updated name", updatedConf.getName(), equalTo(conf.getName()));
		assertThat("Updated service identifier", updatedConf.getServiceIdentifier(),
				equalTo(conf.getServiceIdentifier()));

		Map<String, ?> sprops = conf.getServiceProperties();
		assertThat("Service props", sprops, notNullValue());
		assertThat("Service props size", sprops.keySet(), hasSize(4));
		assertThat(sprops, hasEntry("string", "updated"));
		assertThat(sprops, hasEntry("number", 42));
		assertThat(sprops, hasEntry("list", Arrays.asList("first", "second")));
		assertThat(sprops, hasEntry("added-string", "added"));

		AggregateGeneralNodeDatumFilter f = updatedConf.getDatumFilter();
		assertThat("Filter", f, notNullValue());
		assertThat("New filter instance", f, not(sameInstance(filter)));
		assertThat("Filter aggregate", f.getAggregation(), equalTo(filter.getAggregation()));
		assertThat("Filter node ID", f.getNodeId(), equalTo(filter.getNodeId()));
		assertThat("Filter source ID", f.getSourceId(), equalTo(filter.getSourceId()));
	}

	@Test
	public void findAllForUser() {
		List<UserDataConfiguration> confs = new ArrayList<>(3);
		for ( int i = 0; i < 3; i++ ) {
			storeNew();
			confs.add(this.conf);
		}

		List<UserDataConfiguration> found = confDao.findConfigurationsForUser(this.user.getId());
		assertThat(found, not(sameInstance(confs)));
		assertThat(found, equalTo(confs));
	}

}
