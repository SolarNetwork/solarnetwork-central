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

import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.central.in.tracker.dao.mybatis.MyBatisGlobalMetricCampaignDao;
import net.solarnetwork.central.in.tracker.domain.GlobalMetricCampaign;
import net.solarnetwork.central.support.SimpleSortDescriptor;

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

	private static class CampaignById implements Comparator<GlobalMetricCampaign> {

		@Override
		public int compare(GlobalMetricCampaign o1, GlobalMetricCampaign o2) {
			if ( o1 == o2 ) {
				return 0;
			}
			String l = (o1 != null ? o1.getId() : null);
			String r = (o2 != null ? o2.getId() : null);
			if ( l == r ) {
				return 0;
			}
			if ( l == null ) {
				return -1;
			}
			return l.compareTo(r);
		}

	}

	@Test
	public void findAll() {
		List<GlobalMetricCampaign> campaigns = new ArrayList<>(3);
		for ( int i = 0; i < 3; i++ ) {
			GlobalMetricCampaign info = new GlobalMetricCampaign();
			info.setId(UUID.randomUUID().toString());
			info.setEnabled(true);
			info.setName(UUID.randomUUID().toString());
			dao.store(info);
			campaigns.add(info);
		}

		Collections.sort(campaigns, new CampaignById());

		List<GlobalMetricCampaign> results = dao.getAll(null);
		assertThat("Result count", results, hasSize(3));
		for ( int i = 0; i < 3; i++ ) {
			assertThat("Result " + (i + 1), results.get(i), equalTo(campaigns.get(i)));
		}
	}

	private static class CampaignByName implements Comparator<GlobalMetricCampaign> {

		@Override
		public int compare(GlobalMetricCampaign o1, GlobalMetricCampaign o2) {
			if ( o1 == o2 ) {
				return 0;
			}
			String l = (o1 != null ? o1.getName() : null);
			String r = (o2 != null ? o2.getName() : null);
			if ( l == r ) {
				return 0;
			}
			if ( l == null ) {
				return -1;
			}
			return l.compareToIgnoreCase(r);
		}

	}

	@Test
	public void findAllSortByName() {
		List<GlobalMetricCampaign> campaigns = new ArrayList<>(3);
		for ( int i = 0; i < 3; i++ ) {
			GlobalMetricCampaign info = new GlobalMetricCampaign();
			info.setId(UUID.randomUUID().toString());
			info.setEnabled(true);
			info.setName(UUID.randomUUID().toString());
			dao.store(info);
			campaigns.add(info);
		}

		Collections.sort(campaigns, new CampaignByName());

		List<GlobalMetricCampaign> results = dao
				.getAll(singletonList(new SimpleSortDescriptor("name", false)));
		assertThat("Result count", results, hasSize(3));
		for ( int i = 0; i < 3; i++ ) {
			assertThat("Result " + (i + 1), results.get(i), equalTo(campaigns.get(i)));
		}
	}

}
