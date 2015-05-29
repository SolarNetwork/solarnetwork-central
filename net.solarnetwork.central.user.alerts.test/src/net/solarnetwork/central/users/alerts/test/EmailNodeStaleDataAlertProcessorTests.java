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

import java.util.ArrayList;
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
import net.solarnetwork.central.user.dao.UserAlertSituationDao;
import net.solarnetwork.central.user.dao.UserDao;
import net.solarnetwork.central.user.dao.UserNodeDao;
import net.solarnetwork.central.user.domain.User;
import net.solarnetwork.central.user.domain.UserAlert;
import net.solarnetwork.central.user.domain.UserAlertOptions;
import net.solarnetwork.central.user.domain.UserAlertSituation;
import net.solarnetwork.central.user.domain.UserAlertSituationStatus;
import net.solarnetwork.central.user.domain.UserAlertStatus;
import net.solarnetwork.central.user.domain.UserAlertType;
import net.solarnetwork.central.user.domain.UserNode;
import org.easymock.Capture;
import org.easymock.CaptureType;
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
	private static final Long TEST_NODE_ID_2 = -2L;
	private static final String TEST_SOURCE_ID = "test.source";
	private static final Long TEST_USER_ALERT_ID = -999L;

	private static final AtomicLong AlertIdCounter = new AtomicLong(TEST_USER_ALERT_ID);

	private SolarNodeDao solarNodeDao;
	private UserDao userDao;
	private UserNodeDao userNodeDao;
	private UserAlertDao userAlertDao;
	private UserAlertSituationDao userAlertSituationDao;
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
		userNodeDao = EasyMock.createMock(UserNodeDao.class);
		userAlertDao = EasyMock.createMock(UserAlertDao.class);
		userAlertSituationDao = EasyMock.createMock(UserAlertSituationDao.class);
		MailSender.getSent().clear();
		service = new EmailNodeStaleDataAlertProcessor(solarNodeDao, userDao, userNodeDao, userAlertDao,
				userAlertSituationDao, generalNodeDatumDao, MailService, MessageSource);
		service.setBatchSize(1);
		AlertIdCounter.set(TEST_USER_ALERT_ID);

		// not quite sure why unit tests require no leading slash, but runtime DOES
		service.setMailTemplateResource(EmailNodeStaleDataAlertProcessor.DEFAULT_MAIL_TEMPLATE_RESOURCE
				.substring(1));
		service.setMailTemplateResolvedResource(EmailNodeStaleDataAlertProcessor.DEFAULT_MAIL_TEMPLATE_RESOLVED_RESOURCE
				.substring(1));

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
		EasyMock.verify(generalNodeDatumDao, solarNodeDao, userDao, userNodeDao, userAlertDao,
				userAlertSituationDao);
	}

	private void replayAll() {
		EasyMock.replay(generalNodeDatumDao, solarNodeDao, userDao, userNodeDao, userAlertDao,
				userAlertSituationDao);
	}

	private UserAlert newUserAlertInstance() {
		UserAlert alert = new UserAlert();
		alert.setCreated(new DateTime());
		alert.setValidTo(alert.getCreated());
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
		final DateTime batchTime = new DateTime();
		EasyMock.expect(
				userAlertDao.findAlertsToProcess(UserAlertType.NodeStaleData, null, batchTime,
						service.getBatchSize())).andReturn(pendingAlerts);
		replayAll();
		Long startingId = service.processAlerts(null, batchTime);
		Assert.assertNull(startingId);
		Assert.assertEquals("No mail sent", 0, MailSender.getSent().size());
	}

	@Test
	public void processOneAlertTrigger() {
		final DateTime batchTime = new DateTime();

		List<UserAlert> pendingAlerts = Arrays.asList(newUserAlertInstance());
		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setNodeIds(new Long[] { TEST_NODE_ID });
		filter.setMostRecent(true);
		final DateTime pendingAlertValidTo = pendingAlerts.get(0).getValidTo();

		List<GeneralNodeDatumFilterMatch> nodeData = Arrays.asList(newGeneralNodeDatumMatch(
				new DateTime().minusSeconds(10), TEST_NODE_ID, TEST_SOURCE_ID));
		BasicFilterResults<GeneralNodeDatumFilterMatch> nodeDataResults = new BasicFilterResults<GeneralNodeDatumFilterMatch>(
				nodeData, 1L, 0, 1);

		// first query for pending alerts, starting at beginning
		EasyMock.expect(
				userAlertDao.findAlertsToProcess(UserAlertType.NodeStaleData, null, batchTime,
						service.getBatchSize())).andReturn(pendingAlerts);

		// then query for most recent node datum
		EasyMock.expect(
				generalNodeDatumDao.findFiltered(EasyMock.<DatumFilterCommand> anyObject(),
						EasyMock.<List<SortDescriptor>> isNull(), EasyMock.<Integer> isNull(),
						EasyMock.<Integer> isNull())).andReturn(nodeDataResults);

		// then query for the node, to grab time zone
		EasyMock.expect(solarNodeDao.get(TEST_NODE_ID)).andReturn(testNode);

		// then query for active situation
		EasyMock.expect(
				userAlertSituationDao.getActiveAlertSituationForAlert(pendingAlerts.get(0).getId()))
				.andReturn(null);

		// get User for alert
		EasyMock.expect(userDao.get(TEST_USER_ID)).andReturn(testUser);

		// then save active situation
		Capture<UserAlertSituation> newSituation = new Capture<UserAlertSituation>();
		EasyMock.expect(userAlertSituationDao.store(EasyMock.capture(newSituation))).andReturn(
				AlertIdCounter.getAndIncrement());

		replayAll();
		Long startingId = service.processAlerts(null, batchTime);
		Assert.assertEquals("Next staring ID is last processed alert ID", pendingAlerts.get(0).getId(),
				startingId);
		Assert.assertEquals("Mail sent", 1, MailSender.getSent().size());
		SimpleMailMessage sentMail = MailSender.getSent().element();
		Assert.assertEquals("SolarNetwork alert: SolarNode " + TEST_NODE_ID + " data is stale",
				sentMail.getSubject());
		Assert.assertTrue("Mail has source ID", sentMail.getText()
				.contains("source \"" + TEST_SOURCE_ID));
		Assert.assertTrue(
				"Mail has formatted datum date",
				sentMail.getText().contains(
						"since "
								+ service.getTimestampFormat().print(
										nodeData.get(0).getId().getCreated())));
		Assert.assertTrue("Situation created", newSituation.hasCaptured());
		Assert.assertEquals(pendingAlerts.get(0), newSituation.getValue().getAlert());
		Assert.assertEquals(UserAlertSituationStatus.Active, newSituation.getValue().getStatus());
		Assert.assertNotNull(newSituation.getValue().getNotified());
		Assert.assertTrue("Saved alert validTo not increased",
				pendingAlerts.get(0).getValidTo().equals(pendingAlertValidTo));
	}

	@Test
	public void processOneAlertTriggerSuppressed() {
		final DateTime batchTime = new DateTime();

		final UserAlert pendingAlert = newUserAlertInstance();
		pendingAlert.setStatus(UserAlertStatus.Suppressed);
		List<UserAlert> pendingAlerts = Arrays.asList(pendingAlert);
		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setNodeIds(new Long[] { TEST_NODE_ID });
		filter.setMostRecent(true);
		final DateTime pendingAlertValidTo = pendingAlert.getValidTo();

		List<GeneralNodeDatumFilterMatch> nodeData = Arrays.asList(newGeneralNodeDatumMatch(
				new DateTime().minusSeconds(10), TEST_NODE_ID, TEST_SOURCE_ID));
		BasicFilterResults<GeneralNodeDatumFilterMatch> nodeDataResults = new BasicFilterResults<GeneralNodeDatumFilterMatch>(
				nodeData, 1L, 0, 1);

		// first query for pending alerts, starting at beginning
		EasyMock.expect(
				userAlertDao.findAlertsToProcess(UserAlertType.NodeStaleData, null, batchTime,
						service.getBatchSize())).andReturn(pendingAlerts);

		// then query for most recent node datum
		EasyMock.expect(
				generalNodeDatumDao.findFiltered(EasyMock.<DatumFilterCommand> anyObject(),
						EasyMock.<List<SortDescriptor>> isNull(), EasyMock.<Integer> isNull(),
						EasyMock.<Integer> isNull())).andReturn(nodeDataResults);

		// then query for the node, to grab time zone
		EasyMock.expect(solarNodeDao.get(TEST_NODE_ID)).andReturn(testNode);

		// then query for active situation
		EasyMock.expect(userAlertSituationDao.getActiveAlertSituationForAlert(pendingAlert.getId()))
				.andReturn(null);

		// then save active situation
		Capture<UserAlertSituation> newSituation = new Capture<UserAlertSituation>();
		EasyMock.expect(userAlertSituationDao.store(EasyMock.capture(newSituation))).andReturn(
				AlertIdCounter.getAndIncrement());

		replayAll();
		Long startingId = service.processAlerts(null, batchTime);
		Assert.assertEquals("Next staring ID is last processed alert ID", pendingAlert.getId(),
				startingId);
		Assert.assertEquals("No mail sent", 0, MailSender.getSent().size());
		Assert.assertTrue("Situation created", newSituation.hasCaptured());
		Assert.assertEquals(pendingAlert, newSituation.getValue().getAlert());
		Assert.assertEquals(UserAlertSituationStatus.Active, newSituation.getValue().getStatus());
		Assert.assertNotNull(newSituation.getValue().getNotified());
		Assert.assertTrue("Saved alert validTo not increased",
				pendingAlert.getValidTo().equals(pendingAlertValidTo));
	}

	@Test
	public void processOneAlertTriggerForUser() {
		final DateTime batchTime = new DateTime();

		final UserAlert pendingAlert = newUserAlertInstance();
		pendingAlert.setNodeId(null); // change to "all nodes for user"
		List<UserAlert> pendingAlerts = Arrays.asList(pendingAlert);
		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setUserIds(new Long[] { TEST_USER_ID });
		filter.setMostRecent(true);
		final DateTime pendingAlertValidTo = pendingAlert.getValidTo();

		List<GeneralNodeDatumFilterMatch> nodeData = Arrays.asList(

		newGeneralNodeDatumMatch(new DateTime().minusSeconds(20), TEST_NODE_ID_2, TEST_SOURCE_ID),
				newGeneralNodeDatumMatch(new DateTime().minusSeconds(10), TEST_NODE_ID, TEST_SOURCE_ID));
		BasicFilterResults<GeneralNodeDatumFilterMatch> nodeDataResults = new BasicFilterResults<GeneralNodeDatumFilterMatch>(
				nodeData, 2L, 0, 2);

		// first query for pending alerts, starting at beginning
		EasyMock.expect(
				userAlertDao.findAlertsToProcess(UserAlertType.NodeStaleData, null, batchTime,
						service.getBatchSize())).andReturn(pendingAlerts);

		// will next query on user ID to get available nodes
		final User testUser = new User(TEST_USER_ID, "test@localhost");
		final SolarNode testNode2 = new SolarNode(TEST_NODE_ID_2, testNode.getLocationId());
		testNode2.setLocation(testNode.getLocation());

		final List<UserNode> userNodes = Arrays.asList(new UserNode(testUser, testNode), new UserNode(
				testUser, testNode2));
		EasyMock.expect(userNodeDao.findUserNodesForUser(EasyMock.eq(testUser))).andReturn(userNodes);

		// then query for most recent node datum
		EasyMock.expect(
				generalNodeDatumDao.findFiltered(EasyMock.<DatumFilterCommand> anyObject(),
						EasyMock.<List<SortDescriptor>> isNull(), EasyMock.<Integer> isNull(),
						EasyMock.<Integer> isNull())).andReturn(nodeDataResults);

		// then query for active situation
		EasyMock.expect(userAlertSituationDao.getActiveAlertSituationForAlert(pendingAlert.getId()))
				.andReturn(null);

		// get User for alert
		EasyMock.expect(userDao.get(TEST_USER_ID)).andReturn(testUser);

		// then save active situation
		Capture<UserAlertSituation> newSituation = new Capture<UserAlertSituation>();
		EasyMock.expect(userAlertSituationDao.store(EasyMock.capture(newSituation))).andReturn(
				AlertIdCounter.getAndIncrement());

		replayAll();
		Long startingId = service.processAlerts(null, batchTime);
		Assert.assertEquals("Next staring ID is last processed alert ID", pendingAlert.getId(),
				startingId);
		Assert.assertEquals("Mail sent", 1, MailSender.getSent().size());
		SimpleMailMessage sentMail = MailSender.getSent().element();
		Assert.assertEquals("SolarNetwork alert: SolarNode " + TEST_NODE_ID_2 + " data is stale",
				sentMail.getSubject());
		Assert.assertTrue("Mail has source ID", sentMail.getText()
				.contains("source \"" + TEST_SOURCE_ID));
		Assert.assertTrue(
				"Mail has formatted datum date",
				sentMail.getText().contains(
						"since "
								+ service.getTimestampFormat().print(
										nodeData.get(0).getId().getCreated())));
		Assert.assertTrue("Situation created", newSituation.hasCaptured());
		Assert.assertEquals(pendingAlerts.get(0), newSituation.getValue().getAlert());
		Assert.assertEquals(UserAlertSituationStatus.Active, newSituation.getValue().getStatus());
		Assert.assertNotNull(newSituation.getValue().getNotified());
		Assert.assertTrue("Saved alert validTo not increased",
				pendingAlert.getValidTo().equals(pendingAlertValidTo));
	}

	@Test
	public void processBatchAlertsTrigger() {
		final DateTime batchTime = new DateTime();

		// add 10 alerts, so we can test batching
		List<UserAlert> pendingAlerts = new ArrayList<UserAlert>();
		for ( int i = 0; i < 12; i++ ) {
			pendingAlerts.add(newUserAlertInstance());
		}

		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setNodeIds(new Long[] { TEST_NODE_ID });
		filter.setMostRecent(true);

		List<GeneralNodeDatumFilterMatch> nodeData = Arrays.asList(newGeneralNodeDatumMatch(
				new DateTime().minusSeconds(10), TEST_NODE_ID, TEST_SOURCE_ID));
		BasicFilterResults<GeneralNodeDatumFilterMatch> nodeDataResults = new BasicFilterResults<GeneralNodeDatumFilterMatch>(
				nodeData, 1L, 0, 1);

		// first query for pending alerts, starting at beginning
		EasyMock.expect(
				userAlertDao.findAlertsToProcess(UserAlertType.NodeStaleData, null, batchTime,
						service.getBatchSize())).andReturn(pendingAlerts.subList(0, 5));

		// then query for most recent node datum
		EasyMock.expect(
				generalNodeDatumDao.findFiltered(EasyMock.<DatumFilterCommand> anyObject(),
						EasyMock.<List<SortDescriptor>> isNull(), EasyMock.<Integer> isNull(),
						EasyMock.<Integer> isNull())).andReturn(nodeDataResults);

		// then query for the node, to grab time zone
		EasyMock.expect(solarNodeDao.get(TEST_NODE_ID)).andReturn(testNode);

		Capture<UserAlertSituation> newSituation = new Capture<UserAlertSituation>(CaptureType.ALL);

		// then query for active situation
		for ( int i = 0; i < 5; i++ ) {
			EasyMock.expect(
					userAlertSituationDao.getActiveAlertSituationForAlert(pendingAlerts.get(i).getId()))
					.andReturn(null);

			// get User for alert
			EasyMock.expect(userDao.get(TEST_USER_ID)).andReturn(testUser);

			// then save active situation
			EasyMock.expect(userAlertSituationDao.store(EasyMock.capture(newSituation))).andReturn(
					AlertIdCounter.getAndIncrement());
		}

		// 2nd batch query for pending alerts, starting at previous ID
		EasyMock.expect(
				userAlertDao.findAlertsToProcess(UserAlertType.NodeStaleData, pendingAlerts.get(4)
						.getId(), batchTime, service.getBatchSize())).andReturn(
				pendingAlerts.subList(5, 10));

		// then query for most recent node datum
		EasyMock.expect(
				generalNodeDatumDao.findFiltered(EasyMock.<DatumFilterCommand> anyObject(),
						EasyMock.<List<SortDescriptor>> isNull(), EasyMock.<Integer> isNull(),
						EasyMock.<Integer> isNull())).andReturn(nodeDataResults);

		// then query for the node, to grab time zone
		EasyMock.expect(solarNodeDao.get(TEST_NODE_ID)).andReturn(testNode);

		// then query for active situation
		for ( int i = 5; i < 10; i++ ) {
			EasyMock.expect(
					userAlertSituationDao.getActiveAlertSituationForAlert(pendingAlerts.get(i).getId()))
					.andReturn(null);

			// get User for alert
			EasyMock.expect(userDao.get(TEST_USER_ID)).andReturn(testUser);

			// then save active situation
			EasyMock.expect(userAlertSituationDao.store(EasyMock.capture(newSituation))).andReturn(
					AlertIdCounter.getAndIncrement());
		}

		// 3rd batch query for pending alerts, starting at previous ID
		EasyMock.expect(
				userAlertDao.findAlertsToProcess(UserAlertType.NodeStaleData, pendingAlerts.get(9)
						.getId(), batchTime, service.getBatchSize())).andReturn(
				pendingAlerts.subList(10, pendingAlerts.size()));

		// then query for most recent node datum
		EasyMock.expect(
				generalNodeDatumDao.findFiltered(EasyMock.<DatumFilterCommand> anyObject(),
						EasyMock.<List<SortDescriptor>> isNull(), EasyMock.<Integer> isNull(),
						EasyMock.<Integer> isNull())).andReturn(nodeDataResults);

		// then query for the node, to grab time zone
		EasyMock.expect(solarNodeDao.get(TEST_NODE_ID)).andReturn(testNode);

		// then query for active situation
		for ( int i = 10; i < 12; i++ ) {
			EasyMock.expect(
					userAlertSituationDao.getActiveAlertSituationForAlert(pendingAlerts.get(i).getId()))
					.andReturn(null);

			// get User, SolarNode for alert
			EasyMock.expect(userDao.get(TEST_USER_ID)).andReturn(testUser);

			// then save active situation
			EasyMock.expect(userAlertSituationDao.store(EasyMock.capture(newSituation))).andReturn(
					AlertIdCounter.getAndIncrement());
		}

		// 4th batch query for pending alerts, starting at previous ID
		EasyMock.expect(
				userAlertDao.findAlertsToProcess(UserAlertType.NodeStaleData, pendingAlerts.get(11)
						.getId(), batchTime, service.getBatchSize())).andReturn(
				Collections.<UserAlert> emptyList());

		replayAll();

		Long startingId = null;
		for ( int i = 0; i < 4; i++ ) {
			final int batchSize = (i < 2 ? 5 : 2);
			startingId = service.processAlerts(startingId, batchTime);
			if ( i == 3 ) {
				Assert.assertNull("No more batch values", startingId);
			} else {
				Assert.assertEquals("Next staring ID is last processed alert ID",
						pendingAlerts.get((i * 5) + batchSize - 1).getId(), startingId);
				Assert.assertEquals("Mail sent", batchSize, MailSender.getSent().size());
				for ( SimpleMailMessage sentMail : MailSender.getSent() ) {
					Assert.assertEquals("SolarNetwork alert: SolarNode " + TEST_NODE_ID
							+ " data is stale", sentMail.getSubject());
					Assert.assertTrue("Mail has source ID",
							sentMail.getText().contains("source \"" + TEST_SOURCE_ID));
					Assert.assertTrue(
							"Mail has formatted datum date",
							sentMail.getText().contains(
									"since "
											+ service.getTimestampFormat().print(
													nodeData.get(0).getId().getCreated())));
				}
			}
			MailSender.getSent().clear();
		}
	}

	@Test
	public void processOneAlertResolved() {
		final DateTime batchTime = new DateTime();

		List<UserAlert> pendingAlerts = Arrays.asList(newUserAlertInstance());
		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setNodeIds(new Long[] { TEST_NODE_ID });
		filter.setMostRecent(true);
		final DateTime pendingAlertValidTo = pendingAlerts.get(0).getValidTo();

		List<GeneralNodeDatumFilterMatch> nodeData = Arrays.asList(newGeneralNodeDatumMatch(
				new DateTime(), TEST_NODE_ID, TEST_SOURCE_ID));
		BasicFilterResults<GeneralNodeDatumFilterMatch> nodeDataResults = new BasicFilterResults<GeneralNodeDatumFilterMatch>(
				nodeData, 1L, 0, 1);

		// first query for pending alerts, starting at beginning
		EasyMock.expect(
				userAlertDao.findAlertsToProcess(UserAlertType.NodeStaleData, null, batchTime,
						service.getBatchSize())).andReturn(pendingAlerts);

		// then query for most recent node datum
		EasyMock.expect(
				generalNodeDatumDao.findFiltered(EasyMock.<DatumFilterCommand> anyObject(),
						EasyMock.<List<SortDescriptor>> isNull(), EasyMock.<Integer> isNull(),
						EasyMock.<Integer> isNull())).andReturn(nodeDataResults);

		// then query for the node, to grab time zone
		EasyMock.expect(solarNodeDao.get(TEST_NODE_ID)).andReturn(testNode);

		// then query for active situation
		final UserAlertSituation activeSituation = new UserAlertSituation();
		activeSituation.setId(AlertIdCounter.getAndIncrement());
		activeSituation.setCreated(new DateTime());
		activeSituation.setAlert(pendingAlerts.get(0));
		activeSituation.setStatus(UserAlertSituationStatus.Active);
		EasyMock.expect(
				userAlertSituationDao.getActiveAlertSituationForAlert(pendingAlerts.get(0).getId()))
				.andReturn(activeSituation);

		// get User for alert
		EasyMock.expect(userDao.get(TEST_USER_ID)).andReturn(testUser);

		// then save active situation -> resolved
		EasyMock.expect(userAlertSituationDao.store(activeSituation)).andReturn(activeSituation.getId());

		// and finally save the alert valid date
		userAlertDao.updateValidTo(EasyMock.eq(pendingAlerts.get(0).getId()),
				EasyMock.<DateTime> anyObject());

		replayAll();
		Long startingId = service.processAlerts(null, batchTime);
		Assert.assertEquals("Next staring ID is last processed alert ID", pendingAlerts.get(0).getId(),
				startingId);
		Assert.assertEquals("Mail sent", 1, MailSender.getSent().size());
		SimpleMailMessage sentMail = MailSender.getSent().element();
		Assert.assertEquals("SolarNetwork alert resolved: SolarNode " + TEST_NODE_ID
				+ " data is no longer stale", sentMail.getSubject());
		Assert.assertTrue("Mail has source ID", sentMail.getText()
				.contains("source \"" + TEST_SOURCE_ID));
		Assert.assertTrue(
				"Mail has formatted datum date",
				sentMail.getText()
						.contains(
								"on "
										+ service.getTimestampFormat().print(
												nodeData.get(0).getId().getCreated())));
		Assert.assertEquals(UserAlertSituationStatus.Resolved, activeSituation.getStatus());
		Assert.assertNotNull(activeSituation.getNotified());
		Assert.assertTrue("Saved alert validTo increased",
				pendingAlerts.get(0).getValidTo().isAfter(pendingAlertValidTo));
	}

}
