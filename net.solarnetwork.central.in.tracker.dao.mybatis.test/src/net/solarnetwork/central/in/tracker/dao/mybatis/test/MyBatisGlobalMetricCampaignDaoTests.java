/* ==================================================================
 * MyBatisGlobalMetricCampaignDaoTests.java - 1/11/2018 7:19:48 PM
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

package net.solarnetwork.central.in.tracker.dao.mybatis.test;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.central.in.tracker.dao.mybatis.MyBatisGlobalMetricCampaignDao;
import net.solarnetwork.central.in.tracker.domain.GlobalMetricCampaign;

/**
 * Test cases for the {@link MyBatisGlobalMetricCampaignDao} class.
 * 
 * @author matt
 * @version 1.0
 */
public class MyBatisGlobalMetricCampaignDaoTests extends AbstractMyBatisDaoTestSupport {

	private MyBatisGlobalMetricCampaignDao dao;

	private GlobalMetricCampaign info;

	@Before
	public void setUp() throws Exception {
		dao = new MyBatisGlobalMetricCampaignDao();
		dao.setSqlSessionFactory(getSqlSessionFactory());
		info = null;
	}

	@Test
	public void storeNew() {
		GlobalMetricCampaign info = new GlobalMetricCampaign();
		info.setId(UUID.randomUUID().toString());
		info.setEnabled(true);
		info.setName(UUID.randomUUID().toString());
		info.setDescription(UUID.randomUUID().toString());
		String id = dao.store(info);
		assertThat("Primary key given", id, notNullValue());
		assertThat("Primary key matches", id, equalTo(info.getId()));

		// stash results for other tests to use
		this.info = info;
	}

	@Test
	public void getByPrimaryKey() {
		storeNew();
		GlobalMetricCampaign info = dao.get(this.info.getId());
		assertThat("Found by PK", info, notNullValue());
		assertThat("PK", info.getId(), equalTo(this.info.getId()));
		assertThat("Created assigned", info.getCreated(), notNullValue());
		assertThat("Enabled", info.isEnabled(), equalTo(true));
		assertThat("Name", info.getName(), equalTo(this.info.getName()));
		assertThat("Description", info.getDescription(), equalTo(this.info.getDescription()));
	}

	@Test
	public void updateResults() {
		storeNew();
		GlobalMetricCampaign info = dao.get(this.info.getId());
		info.setEnabled(false);
		info.setName("Yee haw!");
		info.setDescription("Wha hoo!");
		String id = dao.store(info);
		assertThat("ID unchanged", id, equalTo(info.getId()));

		GlobalMetricCampaign updated = dao.get(info.getId());
		assertThat("Updated instance", updated, not(sameInstance(info)));
		assertThat("Enabled", updated.isEnabled(), equalTo(false));
		assertThat("Name", updated.getName(), equalTo(info.getName()));
		assertThat("Description", updated.getDescription(), equalTo(info.getDescription()));
	}

}
