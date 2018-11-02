/* ==================================================================
 * MyBatisGlobalMetricCampaignOptInDaoTests.java - 2/11/2018 5:16:37 PM
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
import net.solarnetwork.central.in.tracker.dao.mybatis.MyBatisGlobalMetricCampaignOptInDao;
import net.solarnetwork.central.in.tracker.domain.GlobalMetricCampaignNodePropertyPK;
import net.solarnetwork.central.in.tracker.domain.GlobalMetricCampaignOptIn;
import net.solarnetwork.domain.GeneralDatumSamplesType;

/**
 * Test cases for the {@link MyBatisGlobalMetricCampaignOptInDao} class.
 * 
 * @author matt
 * @version 1.0
 */
public class MyBatisGlobalMetricCampaignOptInDaoTests extends AbstractMyBatisDaoTestSupport {

	private MyBatisGlobalMetricCampaignOptInDao dao;

	private GlobalMetricCampaignOptIn info;

	@Before
	public void setUp() throws Exception {
		dao = new MyBatisGlobalMetricCampaignOptInDao();
		dao.setSqlSessionFactory(getSqlSessionFactory());
		info = null;
	}

	@Test
	public void storeNew() {
		GlobalMetricCampaignOptIn info = new GlobalMetricCampaignOptIn();
		info.setCampaignId(UUID.randomUUID().toString());
		info.setNodeId(1L);
		info.setSourceId(UUID.randomUUID().toString());
		info.setPropertyType(GeneralDatumSamplesType.Instantaneous);
		info.setPropertyName(UUID.randomUUID().toString());
		GlobalMetricCampaignNodePropertyPK id = dao.store(info);
		assertThat("Primary key given", id, notNullValue());
		assertThat("Primary key matches", id, equalTo(info.getId()));

		// stash results for other tests to use
		this.info = info;
	}

	@Test
	public void getByPrimaryKey() {
		storeNew();
		GlobalMetricCampaignOptIn info = dao.get(this.info.getId());
		assertThat("Found by PK", info, notNullValue());
		assertThat("Different instance", info, not(sameInstance(this.info)));
		assertThat("PK", info.getId(), equalTo(this.info.getId()));
		assertThat("Created assigned", info.getCreated(), notNullValue());
		assertThat("Node ID", info.getNodeId(), equalTo(this.info.getNodeId()));
		assertThat("Source ID", info.getSourceId(), equalTo(this.info.getSourceId()));
		assertThat("Property type", info.getPropertyType(), equalTo(this.info.getPropertyType()));
		assertThat("Property name", info.getPropertyName(), equalTo(this.info.getPropertyName()));
	}

}
