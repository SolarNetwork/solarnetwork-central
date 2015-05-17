/* ==================================================================
 * EmailNodeStaleDataAlertProcessorTests.java - 18/05/2015 9:09:18 am
 * 
 * Copyright 2007-2015 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.users.alerts.test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import net.solarnetwork.central.dao.SolarNodeDao;
import net.solarnetwork.central.datum.dao.GeneralNodeDatumDao;
import net.solarnetwork.central.datum.domain.DatumFilterCommand;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumFilterMatch;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumMatch;
import net.solarnetwork.central.domain.SolarLocation;
import net.solarnetwork.central.domain.SolarNode;
import net.solarnetwork.central.domain.SortDescriptor;
import net.solarnetwork.central.mail.MailService;
import net.solarnetwork.central.mail.mock.MockMailSender;
import net.solarnetwork.central.mail.support.DefaultMailService;
import net.solarnetwork.central.support.BasicFilterResults;
import net.solarnetwork.central.test.AbstractCentralTest;
import net.solarnetwork.central.user.alerts.EmailNodeStaleDataAlertProcessor;
import net.solarnetwork.central.user.dao.UserAlertDao;
import net.solarnetwork.central.user.dao.UserDao;
import net.solarnetwork.central.user.domain.User;
import net.solarnetwork.central.user.domain.UserAlert;
import net.solarnetwork.central.user.domain.UserAlertOptions;
import net.solarnetwork.central.user.domain.UserAlertStatus;
import net.solarnetwork.central.user.domain.UserAlertType;
import org.easymock.EasyMock;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.mail.SimpleMailMessage;

/**
 * Test cases for the {@link EmailNodeStaleDataAlertProcessor} class.
 * 
 * @author matt
 * @version 1.0
 */
public class EmailNodeStaleDataAlertProcessorTests extends AbstractCentralTest {

	private static final MockMailSender MailSender = new MockMailSender();
	private static final MailService MailService = new DefaultMailService(MailSender);
	private static final ResourceBundleMessageSource MessageSource = new ResourceBundleMessageSource();

	private static final Long TEST_USER_ID = -99L;
	private static final String TEST_SOURCE_ID = "test.source";
	private static final Long TEST_USER_ALERT_ID = -999L;

	private static final AtomicLong AlertIdCounter = new AtomicLong(TEST_USER_ALERT_ID);

	private SolarNodeDao solarNodeDao;
	private UserDao userDao;
	private UserAlertDao userAlertDao;
	private GeneralNodeDatumDao generalNodeDatumDao;

	private User testUser;
	private SolarNode testNode;

	private EmailNodeStaleDataAlertProcessor service;

	@BeforeClass
	public static void setupClass() {
		MessageSource.setBasename("net.solarnetwork.central.user.alerts.messages");
	}

	@Before
	public void setup() {
		generalNodeDatumDao = EasyMock.createMock(GeneralNodeDatumDao.class);
		solarNodeDao = EasyMock.createMock(SolarNodeDao.class);
		userDao = EasyMock.createMock(UserDao.class);
		userAlertDao = EasyMock.createMock(UserAlertDao.class);
		MailSender.getSent().clear();
		service = new EmailNodeStaleDataAlertProcessor(solarNodeDao, userDao, userAlertDao,
				generalNodeDatumDao, MailService, MessageSource);
		service.setBatchSize(5);
		AlertIdCounter.set(TEST_USER_ALERT_ID);

		testUser = new User(TEST_USER_ID, "test@localhost");
		testUser.setName("Tester Dude");

		SolarLocation testLoc = new SolarLocation();
		testLoc.setId(TEST_LOC_ID);
		testLoc.setTimeZoneId("Pacific/Auckland");
		testNode = new SolarNode(TEST_NODE_ID, testLoc.getId());
		testNode.setLocation(testLoc);
	}

	@After
	public void teardown() {
		EasyMock.verify(generalNodeDatumDao, solarNodeDao, userDao, userAlertDao);
	}

	private void replayAll() {
		EasyMock.replay(generalNodeDatumDao, solarNodeDao, userDao, userAlertDao);
	}

	private UserAlert newUserAlertInstance() {
		UserAlert alert = new UserAlert();
		alert.setCreated(new DateTime());
		alert.setUserId(TEST_USER_ID);
		alert.setNodeId(TEST_NODE_ID);
		alert.setType(UserAlertType.NodeStaleData);
		alert.setStatus(UserAlertStatus.Active);

		Map<String, Object> options = new HashMap<String, Object>(4);
		options.put(UserAlertOptions.AGE_THRESHOLD, 5);
		alert.setOptions(options);

		alert.setId(AlertIdCounter.getAndIncrement());
		return alert;
	}

	private GeneralNodeDatumFilterMatch newGeneralNodeDatumMatch(DateTime created, Long nodeId,
			String sourceId) {
		GeneralNodeDatumMatch d = new GeneralNodeDatumMatch();
		d.setCreated(created);
		d.setNodeId(nodeId);
		d.setSourceId(sourceId);
		return d;
	}

	@Test
	public void processNoAlerts() {
		List<UserAlert> pendingAlerts = Collections.emptyList();
		EasyMock.expect(
				userAlertDao.findAlertsToProcess(UserAlertType.NodeStaleData, null,
						service.getBatchSize())).andReturn(pendingAlerts);
		replayAll();
		Long startingId = service.processAlerts(null);
		Assert.assertNull(startingId);
		Assert.assertEquals("No mail sent", 0, MailSender.getSent().size());
	}

	@Test
	public void processOneAlertTrigger() {
		List<UserAlert> pendingAlerts = Arrays.asList(newUserAlertInstance());
		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setNodeIds(new Long[] { TEST_NODE_ID });
		filter.setMostRecent(true);

		List<GeneralNodeDatumFilterMatch> nodeData = Arrays.asList(newGeneralNodeDatumMatch(
				new DateTime().minusSeconds(10), TEST_NODE_ID, TEST_SOURCE_ID));
		BasicFilterResults<GeneralNodeDatumFilterMatch> nodeDataResults = new BasicFilterResults<GeneralNodeDatumFilterMatch>(
				nodeData, 1L, 0, 1);

		// first query for pending alerts, starting at beginning
		EasyMock.expect(
				userAlertDao.findAlertsToProcess(UserAlertType.NodeStaleData, null,
						service.getBatchSize())).andReturn(pendingAlerts);

		// then query for most recent node datum
		EasyMock.expect(
				generalNodeDatumDao.findFiltered(EasyMock.<DatumFilterCommand> anyObject(),
						EasyMock.<List<SortDescriptor>> isNull(), EasyMock.<Integer> isNull(),
						EasyMock.<Integer> isNull())).andReturn(nodeDataResults);

		// get User, SolarNode for alert
		EasyMock.expect(userDao.get(TEST_USER_ID)).andReturn(testUser);
		EasyMock.expect(solarNodeDao.get(TEST_NODE_ID)).andReturn(testNode);

		replayAll();
		Long startingId = service.processAlerts(null);
		Assert.assertEquals("Next staring ID is last processed alert ID", pendingAlerts.get(0).getId(),
				startingId);
		Assert.assertEquals("Mail sent", 1, MailSender.getSent().size());
		SimpleMailMessage sentMail = MailSender.getSent().element();
		Assert.assertEquals("SolarNetwork alert: SolarNode " + TEST_NODE_ID + " data is stale",
				sentMail.getSubject());
		Assert.assertTrue("Mail has source ID", sentMail.getText().contains("source " + TEST_SOURCE_ID));
		Assert.assertTrue(
				"Mail has formatted datum date",
				sentMail.getText().contains(
						"since "
								+ service.getTimestampFormat().print(
										nodeData.get(0).getId().getCreated())));
	}
}
