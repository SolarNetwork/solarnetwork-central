/* ==================================================================
 * MyBatisDatumExportTaskInfoDaoTests.java - 19/04/2018 11:01:19 AM
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

package net.solarnetwork.central.datum.export.dao.mybatis.test;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import java.util.UUID;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.central.datum.export.dao.mybatis.MyBatisDatumExportTaskInfoDao;
import net.solarnetwork.central.datum.export.domain.BasicConfiguration;
import net.solarnetwork.central.datum.export.domain.DatumExportState;
import net.solarnetwork.central.datum.export.domain.DatumExportTaskInfo;
import net.solarnetwork.central.datum.export.domain.ScheduleType;

/**
 * Test cases for the {@link MyBatisDatumExportTaskInfoDao} class.
 * 
 * @author matt
 * @version 1.0
 */
public class MyBatisDatumExportTaskInfoDaoTests extends AbstractMyBatisDaoTestSupport {

	private static final DateTime TEST_EXPORT_DATE = new DateTime(2017, 4, 18, 9, 0, 0,
			DateTimeZone.UTC);
	private static final String TEST_NAME = "test.name";
	private static final int TEST_HOUR_OFFSET = 1;

	private MyBatisDatumExportTaskInfoDao dao;

	private DatumExportTaskInfo info;

	@Before
	public void setUp() throws Exception {
		dao = new MyBatisDatumExportTaskInfoDao();
		dao.setSqlSessionFactory(getSqlSessionFactory());
		info = null;
	}

	@Test
	public void storeNew() {
		DatumExportTaskInfo info = new DatumExportTaskInfo();
		info.setConfig(new BasicConfiguration(TEST_NAME, ScheduleType.Daily, TEST_HOUR_OFFSET));
		info.setExportDate(TEST_EXPORT_DATE);
		info.setId(UUID.randomUUID());
		info.setStatus(DatumExportState.Queued);
		UUID id = dao.store(info);
		assertThat("Primary key assigned", id, notNullValue());
		assertThat("Primary key matches", id, equalTo(info.getId()));

		// stash results for other tests to use
		this.info = info;
	}

	@Test
	public void getByPrimaryKey() {
		storeNew();
		DatumExportTaskInfo info = dao.get(this.info.getId());
		assertThat("Found by PK", info, notNullValue());
		assertThat("PK", info.getId(), equalTo(this.info.getId()));
		assertThat("Created assigned", info.getCreated(), notNullValue());
		assertThat("Modified assigned", info.getModified(), notNullValue());
		assertThat("Export date", TEST_EXPORT_DATE.isEqual(info.getExportDate()), equalTo(true));
		assertThat("Status", info.getStatus(), equalTo(DatumExportState.Queued));

		assertThat("Config", info.getConfig(), notNullValue());
		assertThat("Config name", info.getConfig().getName(), equalTo(TEST_NAME));
		assertThat("Config schedule", info.getConfig().getSchedule(), equalTo(ScheduleType.Daily));
		assertThat("Config offset", info.getConfig().getHourDelayOffset(), equalTo(TEST_HOUR_OFFSET));
	}

}
