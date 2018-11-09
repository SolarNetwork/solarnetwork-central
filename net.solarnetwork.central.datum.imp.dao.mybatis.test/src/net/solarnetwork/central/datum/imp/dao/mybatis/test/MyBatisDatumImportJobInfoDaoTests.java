/* ==================================================================
 * MyBatisDatumImportJobInfoDaoTests.java - 10/11/2018 7:17:14 AM
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

package net.solarnetwork.central.datum.imp.dao.mybatis.test;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import java.util.Collections;
import java.util.UUID;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.central.datum.imp.dao.mybatis.MyBatisDatumImportJobInfoDao;
import net.solarnetwork.central.datum.imp.domain.BasicConfiguration;
import net.solarnetwork.central.datum.imp.domain.BasicInputConfiguration;
import net.solarnetwork.central.datum.imp.domain.DatumImportJobInfo;
import net.solarnetwork.central.user.domain.User;
import net.solarnetwork.central.user.domain.UserUuidPK;

/**
 * Test cases for the {@link MyBatisDatumImportJobInfoDao} class.
 * 
 * @author matt
 * @version 1.0
 */
public class MyBatisDatumImportJobInfoDaoTests extends AbstractMyBatisDatumImportDaoTestSupport {

	private MyBatisDatumImportJobInfoDao dao;

	private User user;
	private DatumImportJobInfo info;

	@Before
	public void setUp() throws Exception {
		dao = new MyBatisDatumImportJobInfoDao();
		dao.setSqlSessionFactory(getSqlSessionFactory());

		this.user = createNewUser(TEST_EMAIL);
		assertThat("Test user", this.user, notNullValue());

		info = null;
	}

	private BasicConfiguration createNewConfig() {
		BasicConfiguration conf = new BasicConfiguration();
		conf.setName(TEST_NAME);
		conf.setStage(true);

		BasicInputConfiguration inputConfig = new BasicInputConfiguration();
		inputConfig.setName(TEST_NAME);
		inputConfig.setServiceIdentifier("foo.bar");
		inputConfig.setTimeZoneId("Pacific/Auckland");
		inputConfig.setServiceProps(Collections.singletonMap("foo", "bar"));
		conf.setInputConfiguration(inputConfig);

		return conf;
	}

	@Test
	public void storeNew() {
		DatumImportJobInfo info = new DatumImportJobInfo();
		info.setId(new UserUuidPK(this.user.getId(), UUID.randomUUID()));
		info.setImportDate(new DateTime());
		info.setConfig(createNewConfig());

		UserUuidPK id = dao.store(info);
		assertThat("Primary key assigned", id, notNullValue());
		assertThat("Primary key matches", id, equalTo(info.getId()));

		// stash results for other tests to use
		info.setId(id);
		this.info = info;
	}

	@Test
	public void getByPrimaryKey() {
		storeNew();
		DatumImportJobInfo info = dao.get(this.info.getId());
		assertThat("Found by PK", info, notNullValue());
		assertThat("PK", info.getId(), equalTo(this.info.getId()));
		assertThat("Created assigned", info.getCreated(), notNullValue());
		assertThat("User ID", info.getUserId(), equalTo(this.user.getId()));
		assertThat("Config", info.getConfig(), notNullValue());
		assertThat("Import date", info.getImportDate(), notNullValue());

		// stash results for other tests to use
		this.info = info;
	}

}
