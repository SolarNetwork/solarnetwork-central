/* ==================================================================
 * MyBatisUserDatumExportConfigurationDaoTests.java - 22/03/2018 12:32:26 PM
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

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.central.datum.domain.DatumFilterCommand;
import net.solarnetwork.central.datum.export.domain.OutputCompressionType;
import net.solarnetwork.central.datum.export.domain.ScheduleType;
import net.solarnetwork.central.domain.Aggregation;
import net.solarnetwork.central.user.domain.User;
import net.solarnetwork.central.user.export.dao.mybatis.MyBatisUserDataConfigurationDao;
import net.solarnetwork.central.user.export.dao.mybatis.MyBatisUserDatumExportConfigurationDao;
import net.solarnetwork.central.user.export.dao.mybatis.MyBatisUserDestinationConfigurationDao;
import net.solarnetwork.central.user.export.dao.mybatis.MyBatisUserOutputConfigurationDao;
import net.solarnetwork.central.user.export.domain.UserDataConfiguration;
import net.solarnetwork.central.user.export.domain.UserDatumExportConfiguration;
import net.solarnetwork.central.user.export.domain.UserDestinationConfiguration;
import net.solarnetwork.central.user.export.domain.UserOutputConfiguration;

/**
 * Test cases for the {@link MyBatisUserDatumExportConfigurationDao} class.
 * 
 * @author matt
 * @version 1.1
 */
public class MyBatisUserDatumExportConfigurationDaoTests extends AbstractMyBatisUserDaoTestSupport {

	private static final String TEST_NAME = "test.name";
	private static final DateTime TEST_DATE = new DateTime(2018, 4, 29, 7, 0, DateTimeZone.UTC);

	private MyBatisUserDataConfigurationDao dataConfDao;
	private MyBatisUserDestinationConfigurationDao destConfDao;
	private MyBatisUserOutputConfigurationDao outpConfDao;

	private MyBatisUserDatumExportConfigurationDao dao;

	private User user;
	private UserDatumExportConfiguration conf;

	@Before
	public void setUp() throws Exception {
		dataConfDao = new MyBatisUserDataConfigurationDao();
		dataConfDao.setSqlSessionFactory(getSqlSessionFactory());
		destConfDao = new MyBatisUserDestinationConfigurationDao();
		destConfDao.setSqlSessionFactory(getSqlSessionFactory());
		outpConfDao = new MyBatisUserOutputConfigurationDao();
		outpConfDao.setSqlSessionFactory(getSqlSessionFactory());

		dao = new MyBatisUserDatumExportConfigurationDao();
		dao.setSqlSessionFactory(getSqlSessionFactory());

		setupTestLocation();
		this.user = createNewUser(TEST_EMAIL);
		assertThat("Test user created", this.user, notNullValue());
		this.user.setLocationId(TEST_LOC_ID);
		userDao.store(this.user);

		conf = null;
	}

	private UserDataConfiguration addDataConf() {
		UserDataConfiguration conf = new UserDataConfiguration();
		conf.setCreated(new DateTime());
		conf.setUserId(this.user.getId());
		conf.setName(UUID.randomUUID().toString());
		conf.setServiceIdentifier(UUID.randomUUID().toString());

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

		Long id = dataConfDao.store(conf);
		conf.setId(id);
		return conf;
	}

	private UserDestinationConfiguration addDestConf() {
		UserDestinationConfiguration conf = new UserDestinationConfiguration();
		conf.setCreated(new DateTime());
		conf.setUserId(this.user.getId());
		conf.setName(UUID.randomUUID().toString());
		conf.setServiceIdentifier(UUID.randomUUID().toString());

		Map<String, Object> sprops = new HashMap<String, Object>(4);
		sprops.put("string", "foo");
		sprops.put("number", 42);

		List<String> optionList = new ArrayList<String>(4);
		optionList.add("first");
		optionList.add("second");
		sprops.put("list", optionList);

		conf.setServiceProps(sprops);

		Long id = destConfDao.store(conf);
		conf.setId(id);
		return conf;
	}

	private UserOutputConfiguration addOutpConf() {
		UserOutputConfiguration conf = new UserOutputConfiguration();
		conf.setCreated(new DateTime());
		conf.setUserId(this.user.getId());
		conf.setName(UUID.randomUUID().toString());
		conf.setServiceIdentifier(UUID.randomUUID().toString());
		conf.setCompressionType(OutputCompressionType.None);

		Map<String, Object> sprops = new HashMap<String, Object>(4);
		sprops.put("string", "foo");
		sprops.put("number", 42);

		List<String> optionList = new ArrayList<String>(4);
		optionList.add("first");
		optionList.add("second");
		sprops.put("list", optionList);

		conf.setServiceProps(sprops);

		Long id = outpConfDao.store(conf);
		conf.setId(id);
		return conf;
	}

	@Test
	public void storeNew() {
		UserDatumExportConfiguration conf = new UserDatumExportConfiguration();
		conf.setCreated(new DateTime());
		conf.setUserId(this.user.getId());
		conf.setName(TEST_NAME);
		conf.setHourDelayOffset(2);
		conf.setSchedule(ScheduleType.Weekly);

		Long id = dao.store(conf);
		assertThat("Primary key assigned", id, notNullValue());

		// stash results for other tests to use
		conf.setId(id);
		this.conf = conf;
	}

	@Test
	public void storeNewWithConfigurations() {
		UserDataConfiguration dataConf = addDataConf();
		UserDestinationConfiguration destConf = addDestConf();
		UserOutputConfiguration outpConf = addOutpConf();

		UserDatumExportConfiguration conf = new UserDatumExportConfiguration();
		conf.setCreated(new DateTime());
		conf.setUserId(this.user.getId());
		conf.setName(TEST_NAME);
		conf.setHourDelayOffset(2);
		conf.setSchedule(ScheduleType.Weekly);

		conf.setUserDataConfiguration(dataConf);
		conf.setUserDestinationConfiguration(destConf);
		conf.setUserOutputConfiguration(outpConf);

		Long id = dao.store(conf);
		assertThat("Primary key assigned", id, notNullValue());

		// stash results for other tests to use
		conf.setId(id);
		this.conf = conf;
	}

	@Test
	public void getByPrimaryKey() {
		storeNewWithConfigurations();
		UserDatumExportConfiguration conf = dao.get(this.conf.getId(), this.user.getId());
		assertThat("Found by PK", conf, notNullValue());
		assertThat("Different instance", conf, not(sameInstance(this.conf)));
		assertThat("PK", conf.getId(), equalTo(this.conf.getId()));
		assertThat("Created", conf.getCreated().secondOfMinute().roundFloorCopy(),
				equalTo(this.conf.getCreated().secondOfMinute().roundFloorCopy()));
		assertThat("User ID", conf.getUserId(), equalTo(this.user.getId()));
		assertThat("Name", conf.getName(), equalTo(TEST_NAME));
		assertThat("Hour delay", conf.getHourDelayOffset(), equalTo(2));
		assertThat("Schedule", conf.getSchedule(), equalTo(ScheduleType.Weekly));
		assertThat("Time zone", conf.getTimeZoneId(), equalTo(TEST_TZ));

		UserDataConfiguration dataConf = conf.getUserDataConfiguration();
		assertThat("Data conf retrieved", dataConf, notNullValue());
		assertThat("Data conf different instance", dataConf,
				not(sameInstance(this.conf.getUserDataConfiguration())));
		assertThat("Data conf created", dataConf.getCreated().secondOfMinute().roundFloorCopy(), equalTo(
				this.conf.getUserDataConfiguration().getCreated().secondOfMinute().roundFloorCopy()));
		assertThat("Data conf name", dataConf.getName(),
				equalTo(this.conf.getUserDataConfiguration().getName()));
		assertThat("Data conf service ident", dataConf.getServiceIdentifier(),
				equalTo(this.conf.getUserDataConfiguration().getServiceIdentifier()));
		assertThat("Data conf filter", dataConf.getFilter(),
				equalTo(this.conf.getUserDataConfiguration().getFilter()));
		// TODO rest of properties

		UserDestinationConfiguration destConf = conf.getUserDestinationConfiguration();
		assertThat("Dest conf retrieved", destConf, notNullValue());
		assertThat("Dest conf different instance", destConf,
				not(sameInstance(this.conf.getUserDestinationConfiguration())));
		assertThat("Dest conf created", destConf.getCreated().secondOfMinute().roundFloorCopy(),
				equalTo(this.conf.getUserDestinationConfiguration().getCreated().secondOfMinute()
						.roundFloorCopy()));
		assertThat("Dest conf name", destConf.getName(),
				equalTo(this.conf.getUserDestinationConfiguration().getName()));
		assertThat("Dest conf service ident", destConf.getServiceIdentifier(),
				equalTo(this.conf.getUserDestinationConfiguration().getServiceIdentifier()));

		UserOutputConfiguration outpConf = conf.getUserOutputConfiguration();
		assertThat("Outp conf retrieved", outpConf, notNullValue());
		assertThat("Outp conf different instance", outpConf,
				not(sameInstance(this.conf.getUserOutputConfiguration())));
		assertThat("Outp conf created", outpConf.getCreated().secondOfMinute().roundFloorCopy(), equalTo(
				this.conf.getUserOutputConfiguration().getCreated().secondOfMinute().roundFloorCopy()));
		assertThat("Outp conf name", outpConf.getName(),
				equalTo(this.conf.getUserOutputConfiguration().getName()));
		assertThat("Outp conf service ident", outpConf.getServiceIdentifier(),
				equalTo(this.conf.getUserOutputConfiguration().getServiceIdentifier()));
		assertThat("Outp conf compression", outpConf.getCompressionType(),
				equalTo(this.conf.getUserOutputConfiguration().getCompressionType()));
	}

	@Test
	public void update() {
		storeNewWithConfigurations();
		UserDatumExportConfiguration conf = dao.get(this.conf.getId(), this.user.getId());

		conf.setName("new.name");
		conf.setHourDelayOffset(5);
		conf.setSchedule(ScheduleType.Monthly);
		conf.setMinimumExportDate(TEST_DATE);

		Long id = dao.store(conf);
		assertThat("PK unchanged", id, equalTo(this.conf.getId()));

		UserDatumExportConfiguration updatedConf = dao.get(id, this.user.getId());
		assertThat("Found by PK", updatedConf, notNullValue());
		assertThat("New entity returned", updatedConf, not(sameInstance(conf)));
		assertThat("PK", updatedConf.getId(), equalTo(conf.getId()));
		assertThat("Created unchanged", updatedConf.getCreated(), equalTo(conf.getCreated()));
		assertThat("Uesr ID", updatedConf.getUserId(), equalTo(conf.getUserId()));
		assertThat("Updated name", updatedConf.getName(), equalTo(conf.getName()));
		assertThat("Updated hour offset", updatedConf.getHourDelayOffset(),
				equalTo(conf.getHourDelayOffset()));
		assertThat("Updated schedule", updatedConf.getSchedule(), equalTo(conf.getSchedule()));
		assertThat("Time zone", updatedConf.getTimeZoneId(), equalTo(TEST_TZ));
		assertThat("Minimum export date", updatedConf.getMinimumExportDate().isEqual(TEST_DATE),
				equalTo(true));
	}

	@Test
	public void updateNoChangeToMinimumExportDate() {
		update();

		UserDatumExportConfiguration conf = new UserDatumExportConfiguration();
		conf.setId(this.conf.getId());
		conf.setUserId(this.user.getId());
		conf.setName("new.new.name");
		conf.setHourDelayOffset(6);
		conf.setSchedule(ScheduleType.Weekly);
		conf.setMinimumExportDate(null);

		Long id = dao.store(conf);
		assertThat("PK unchanged", id, equalTo(this.conf.getId()));

		UserDatumExportConfiguration updatedConf = dao.get(id, this.user.getId());
		assertThat("Found by PK", updatedConf, notNullValue());
		assertThat("New entity returned", updatedConf, not(sameInstance(conf)));
		assertThat("PK", updatedConf.getId(), equalTo(conf.getId()));
		assertThat("Created unchanged", updatedConf.getCreated(), equalTo(this.conf.getCreated()));
		assertThat("Uesr ID", updatedConf.getUserId(), equalTo(conf.getUserId()));
		assertThat("Updated name", updatedConf.getName(), equalTo(conf.getName()));
		assertThat("Updated hour offset", updatedConf.getHourDelayOffset(),
				equalTo(conf.getHourDelayOffset()));
		assertThat("Updated schedule", updatedConf.getSchedule(), equalTo(conf.getSchedule()));
		assertThat("Time zone", updatedConf.getTimeZoneId(), equalTo(TEST_TZ));
		assertThat("Minimum export date unchanged",
				updatedConf.getMinimumExportDate().isEqual(TEST_DATE), equalTo(true));
	}

	@Test
	public void updateMinimumDate() {
		storeNew();

		DateTime minDate = ScheduleType.Hourly.nextExportDate(new DateTime());

		int updated = dao.updateMinimumExportDate(this.conf.getId(), this.user.getId(), minDate);
		assertThat("Update count", updated, equalTo(1));

		UserDatumExportConfiguration conf = dao.get(this.conf.getId(), this.user.getId());
		assertThat("Min date", conf.getMinimumExportDate(), equalTo(minDate));
	}

	@Test
	public void findAllForUser() {
		List<UserDatumExportConfiguration> confs = new ArrayList<>(3);
		for ( int i = 0; i < 3; i++ ) {
			storeNewWithConfigurations();
			confs.add(this.conf);
		}

		List<UserDatumExportConfiguration> found = dao.findConfigurationsForUser(this.user.getId());
		assertThat(found, not(sameInstance(confs)));
		assertThat(found, equalTo(confs));
	}

	@Test
	public void findForExecutionNotFullyConfigured() {
		DateTime exportDate = new DateTime(2017, 4, 18, 8, 0, 0, DateTimeZone.UTC);

		UserDatumExportConfiguration conf = new UserDatumExportConfiguration();
		conf.setCreated(new DateTime());
		conf.setUserId(this.user.getId());
		conf.setName(TEST_NAME);
		conf.setHourDelayOffset(2);
		conf.setSchedule(ScheduleType.Hourly);
		conf.setMinimumExportDate(exportDate);

		Long id = dao.store(conf);
		conf.setId(id);

		List<UserDatumExportConfiguration> found = dao.findForExecution(exportDate.plusHours(1),
				ScheduleType.Hourly);
		assertThat("0900 query export date finds nothing because not configured", found, hasSize(0));
	}

	@Test
	public void findForExecution() {
		UserDataConfiguration dataConf = addDataConf();
		UserDestinationConfiguration destConf = addDestConf();
		UserOutputConfiguration outpConf = addOutpConf();

		List<UserDatumExportConfiguration> confs = new ArrayList<>(3);
		DateTime exportDate = new DateTime(2017, 4, 18, 8, 0, 0, DateTimeZone.UTC);
		for ( int i = 0; i < 3; i++ ) {
			UserDatumExportConfiguration conf = new UserDatumExportConfiguration();
			conf.setCreated(new DateTime());
			conf.setUserId(this.user.getId());
			conf.setName(TEST_NAME);
			conf.setHourDelayOffset(2);
			conf.setSchedule(ScheduleType.Hourly);
			conf.setMinimumExportDate(exportDate.plusHours(i));

			conf.setUserDataConfiguration(dataConf);
			conf.setUserDestinationConfiguration(destConf);
			conf.setUserOutputConfiguration(outpConf);

			Long id = dao.store(conf);
			conf.setId(id);
			confs.add(conf);
		}

		List<UserDatumExportConfiguration> found = dao.findForExecution(exportDate, ScheduleType.Hourly);
		assertThat("0800 query export date finds nothing", found, hasSize(0));

		found = dao.findForExecution(exportDate.plusHours(1), ScheduleType.Hourly);
		assertThat("0900 query export date finds 0800 conf", found, hasSize(1));
		assertThat(found.get(0), equalTo(confs.get(0)));

		found = dao.findForExecution(exportDate.plusHours(2), ScheduleType.Hourly);
		assertThat("0901 query export date finds 0800, 0900 confs", found, hasSize(2));
		assertThat(found.get(0), equalTo(confs.get(0)));
		assertThat(found.get(1), equalTo(confs.get(1)));

		found = dao.findForExecution(exportDate.plusHours(3), ScheduleType.Hourly);
		assertThat("1001 query export date finds 0800, 0900, 1000 confs", found, hasSize(3));
		assertThat(found, not(sameInstance(confs)));
		assertThat(found, equalTo(confs));
	}

}
