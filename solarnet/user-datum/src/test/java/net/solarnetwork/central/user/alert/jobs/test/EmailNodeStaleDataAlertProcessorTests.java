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

package net.solarnetwork.central.user.alert.jobs.test;

import static java.util.Arrays.asList;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static net.solarnetwork.central.datum.v2.domain.BasicObjectDatumStreamMetadata.emptyMeta;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import org.easymock.Capture;
import org.easymock.CaptureType;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.mail.MailMessage;
import org.springframework.mail.SimpleMailMessage;
import net.solarnetwork.central.dao.SolarNodeDao;
import net.solarnetwork.central.datum.domain.DatumFilterCommand;
import net.solarnetwork.central.datum.v2.dao.BasicObjectDatumStreamFilterResults;
import net.solarnetwork.central.datum.v2.dao.DatumEntity;
import net.solarnetwork.central.datum.v2.dao.DatumEntityDao;
import net.solarnetwork.central.datum.v2.dao.ObjectDatumStreamFilterResults;
import net.solarnetwork.central.datum.v2.domain.Datum;
import net.solarnetwork.central.datum.v2.domain.DatumPK;
import net.solarnetwork.central.domain.SolarLocation;
import net.solarnetwork.central.domain.SolarNode;
import net.solarnetwork.central.mail.MailService;
import net.solarnetwork.central.mail.mock.MockMailSender;
import net.solarnetwork.central.mail.support.DefaultMailService;
import net.solarnetwork.central.test.AbstractCentralTest;
import net.solarnetwork.central.user.alert.jobs.EmailNodeStaleDataAlertProcessor;
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
import net.solarnetwork.domain.datum.ObjectDatumKind;
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadata;

/**
 * Test cases for the {@link EmailNodeStaleDataAlertProcessor} class.
 * 
 * @author matt
 * @version 1.2
 */
public class EmailNodeStaleDataAlertProcessorTests extends AbstractCentralTest {

	private static final MockMailSender MailSender = new MockMailSender();
	private static final MailService MailService = new DefaultMailService(MailSender);
	private static final ResourceBundleMessageSource MessageSource = new ResourceBundleMessageSource();

	private static final Long TEST_USER_ID = -99L;
	private static final Long TEST_NODE_ID_2 = -2L;
	private static final String TEST_SOURCE_ID = "test.source";
	private static final Long TEST_USER_ALERT_ID = -999L;
	private static final String TEST_USER_EMAIL = "test@localhost";
	private static final String TEST_USER_NAME = "Tester Dude";

	private static final String TEST_TIME_ZONE_ID = "Pacific/Auckland";

	private static final AtomicLong AlertIdCounter = new AtomicLong(TEST_USER_ALERT_ID);

	private SolarNodeDao solarNodeDao;
	private UserDao userDao;
	private UserNodeDao userNodeDao;
	private UserAlertDao userAlertDao;
	private UserAlertSituationDao userAlertSituationDao;
	private DatumEntityDao datumDao;

	private User testUser;
	private SolarNode testNode;

	private TestEmailNodeStaleDataAlertProcessor service;

	private static class TestEmailNodeStaleDataAlertProcessor extends EmailNodeStaleDataAlertProcessor {

		private Instant systemTime = null;

		public TestEmailNodeStaleDataAlertProcessor(SolarNodeDao solarNodeDao, UserDao userDao,
				UserNodeDao userNodeDao, UserAlertDao userAlertDao,
				UserAlertSituationDao userAlertSituationDao, DatumEntityDao datumDao,
				net.solarnetwork.central.mail.MailService mailService,
				org.springframework.context.MessageSource messageSource) {
			super(solarNodeDao, userDao, userNodeDao, userAlertDao, userAlertSituationDao, datumDao,
					mailService, messageSource);
		}

		@Override
		public Instant getCurrentTime() {
			if ( systemTime != null ) {
				return systemTime;
			}
			return super.getCurrentTime();
		}

		public void setSystemTime(Instant systemTime) {
			this.systemTime = systemTime;
		}

	}

	@BeforeClass
	public static void setupClass() {
		MessageSource.setBasename(EmailNodeStaleDataAlertProcessor.class.getName());
	}

	@Before
	public void setup() {
		datumDao = EasyMock.createMock(DatumEntityDao.class);
		solarNodeDao = EasyMock.createMock(SolarNodeDao.class);
		userDao = EasyMock.createMock(UserDao.class);
		userNodeDao = EasyMock.createMock(UserNodeDao.class);
		userAlertDao = EasyMock.createMock(UserAlertDao.class);
		userAlertSituationDao = EasyMock.createMock(UserAlertSituationDao.class);
		MailSender.getSent().clear();
		service = new TestEmailNodeStaleDataAlertProcessor(solarNodeDao, userDao, userNodeDao,
				userAlertDao, userAlertSituationDao, datumDao, MailService, MessageSource);
		service.setBatchSize(1);
		AlertIdCounter.set(TEST_USER_ALERT_ID);

		service.setMailTemplateResource(EmailNodeStaleDataAlertProcessor.DEFAULT_MAIL_TEMPLATE_RESOURCE);
		service.setMailTemplateResolvedResource(
				EmailNodeStaleDataAlertProcessor.DEFAULT_MAIL_TEMPLATE_RESOLVED_RESOURCE);

		testUser = new User(TEST_USER_ID, TEST_USER_EMAIL);
		testUser.setName(TEST_USER_NAME);

		SolarLocation testLoc = new SolarLocation();
		testLoc.setId(TEST_LOC_ID);
		testLoc.setTimeZoneId(TEST_TIME_ZONE_ID);
		testNode = new SolarNode(TEST_NODE_ID, testLoc.getId());
		testNode.setLocation(testLoc);
	}

	@After
	public void teardown() {
		EasyMock.verify(datumDao, solarNodeDao, userDao, userNodeDao, userAlertDao,
				userAlertSituationDao);
	}

	private void replayAll() {
		EasyMock.replay(datumDao, solarNodeDao, userDao, userNodeDao, userAlertDao,
				userAlertSituationDao);
	}

	private UserAlert newUserAlertInstance() {
		UserAlert alert = new UserAlert();
		alert.setCreated(Instant.now());
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

	private static DatumEntity newDatum(ZonedDateTime created, UUID streamId) {
		DatumEntity d = new DatumEntity(streamId, created.toInstant(), Instant.now(), null);
		return d;
	}

	private static ObjectDatumStreamFilterResults<Datum, DatumPK> newTestResults(ZonedDateTime created,
			UUID streamId) {
		return newTestResults(created, streamId, newDatum(created, streamId));
	}

	private static ObjectDatumStreamFilterResults<Datum, DatumPK> newTestResults(ZonedDateTime created,
			UUID streamId, Datum... nodeData) {
		return newTestResults(created, new ObjectDatumStreamMetadata[] {
				emptyMeta(streamId, TEST_TZ, ObjectDatumKind.Node, TEST_NODE_ID, TEST_SOURCE_ID) },
				nodeData);
	}

	private static ObjectDatumStreamFilterResults<Datum, DatumPK> newTestResults(ZonedDateTime created,
			ObjectDatumStreamMetadata[] metas, Datum... nodeData) {
		BasicObjectDatumStreamFilterResults<Datum, DatumPK> nodeDataResults = new BasicObjectDatumStreamFilterResults<>(
				asList(metas).stream()
						.collect(toMap(ObjectDatumStreamMetadata::getStreamId, identity())),
				asList(nodeData), (long) nodeData.length, 0, nodeData.length);
		return nodeDataResults;
	}

	@Test
	public void processNoAlerts() {
		List<UserAlert> pendingAlerts = Collections.emptyList();
		final Instant batchTime = Instant.now();
		expect(userAlertDao.findAlertsToProcess(UserAlertType.NodeStaleData, null, batchTime,
				service.getBatchSize())).andReturn(pendingAlerts);
		replayAll();
		Long startingId = service.processAlerts(null, batchTime);
		Assert.assertNull(startingId);
		Assert.assertEquals("No mail sent", 0, MailSender.getSent().size());
	}

	@Test
	public void processOneAlertTrigger() {
		final Instant batchTime = Instant.now();

		List<UserAlert> pendingAlerts = Arrays.asList(newUserAlertInstance());
		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setNodeIds(new Long[] { TEST_NODE_ID });
		filter.setMostRecent(true);
		final Instant pendingAlertValidTo = pendingAlerts.get(0).getValidTo();

		final UUID streamId = UUID.randomUUID();
		final ObjectDatumStreamFilterResults<Datum, DatumPK> nodeDataResults = newTestResults(
				ZonedDateTime.now().minusSeconds(10), streamId);

		// first query for pending alerts, starting at beginning
		expect(userAlertDao.findAlertsToProcess(UserAlertType.NodeStaleData, null, batchTime,
				service.getBatchSize())).andReturn(pendingAlerts);

		// then query for most recent node datum
		expect(datumDao.findFiltered(anyObject())).andReturn(nodeDataResults);

		// then query for the node, to grab time zone
		expect(solarNodeDao.get(TEST_NODE_ID)).andReturn(testNode);

		// then query for active situation
		expect(userAlertSituationDao.getActiveAlertSituationForAlert(pendingAlerts.get(0).getId()))
				.andReturn(null);

		// get User for alert
		expect(userDao.get(TEST_USER_ID)).andReturn(testUser);

		// then save active situation
		Capture<UserAlertSituation> newSituation = new Capture<UserAlertSituation>();
		expect(userAlertSituationDao.store(EasyMock.capture(newSituation)))
				.andReturn(AlertIdCounter.getAndIncrement());

		replayAll();
		Long startingId = service.processAlerts(null, batchTime);
		assertThat("Next staring ID is last processed alert ID", startingId,
				is(pendingAlerts.get(0).getId()));
		assertThat("Mail sent", MailSender.getSent().size(), is(1));
		SimpleMailMessage sentMail = (SimpleMailMessage) MailSender.getSent().element();
		assertThat("Mail sent to owner address", sentMail.getTo(),
				is(arrayContaining(String.format("\"%s\" <%s>", TEST_USER_NAME, TEST_USER_EMAIL))));
		assertThat("Mail subject", sentMail.getSubject(),
				is("SolarNetwork alert: SolarNode " + TEST_NODE_ID + " data is stale"));
		assertThat("Mail has source ID", sentMail.getText(),
				containsString("source \"" + TEST_SOURCE_ID));
		assertThat("Mail has formatted datum date", sentMail.getText(), containsString(
				"since " + mailFormattedDate(nodeDataResults.iterator().next().getTimestamp())));
		Assert.assertTrue("Situation created", newSituation.hasCaptured());
		Assert.assertEquals(pendingAlerts.get(0), newSituation.getValue().getAlert());
		Assert.assertEquals(UserAlertSituationStatus.Active, newSituation.getValue().getStatus());
		Assert.assertNotNull(newSituation.getValue().getNotified());
		Assert.assertTrue("Saved alert validTo not increased",
				pendingAlerts.get(0).getValidTo().equals(pendingAlertValidTo));
	}

	@Test
	public void processOneAlertTrigger_customEmail() {
		final Instant batchTime = Instant.now();

		final String alertEmailTo = "custom@localhost";
		final UserAlert alert = newUserAlertInstance();
		alert.getOptions().put(UserAlertOptions.EMAIL_TOS, new String[] { alertEmailTo });
		List<UserAlert> pendingAlerts = Arrays.asList(alert);
		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setNodeIds(new Long[] { TEST_NODE_ID });
		filter.setMostRecent(true);
		final Instant pendingAlertValidTo = pendingAlerts.get(0).getValidTo();

		final UUID streamId = UUID.randomUUID();
		final ObjectDatumStreamFilterResults<Datum, DatumPK> nodeDataResults = newTestResults(
				ZonedDateTime.now().minusSeconds(10), streamId);

		// first query for pending alerts, starting at beginning
		expect(userAlertDao.findAlertsToProcess(UserAlertType.NodeStaleData, null, batchTime,
				service.getBatchSize())).andReturn(pendingAlerts);

		// then query for most recent node datum
		expect(datumDao.findFiltered(anyObject())).andReturn(nodeDataResults);

		// then query for the node, to grab time zone
		expect(solarNodeDao.get(TEST_NODE_ID)).andReturn(testNode);

		// then query for active situation
		expect(userAlertSituationDao.getActiveAlertSituationForAlert(pendingAlerts.get(0).getId()))
				.andReturn(null);

		// get User for alert
		expect(userDao.get(TEST_USER_ID)).andReturn(testUser);

		// then save active situation
		Capture<UserAlertSituation> newSituation = new Capture<UserAlertSituation>();
		expect(userAlertSituationDao.store(EasyMock.capture(newSituation)))
				.andReturn(AlertIdCounter.getAndIncrement());

		replayAll();
		Long startingId = service.processAlerts(null, batchTime);
		assertThat("Next staring ID is last processed alert ID", startingId,
				is(pendingAlerts.get(0).getId()));
		assertThat("Mail sent", MailSender.getSent().size(), is(1));
		SimpleMailMessage sentMail = (SimpleMailMessage) MailSender.getSent().element();
		assertThat("Mail sent to custom address", sentMail.getTo(), is(arrayContaining(alertEmailTo)));
		assertThat("Mail subject", sentMail.getSubject(),
				is(equalTo("SolarNetwork alert: SolarNode " + TEST_NODE_ID + " data is stale")));
		assertThat("Mail has source ID", sentMail.getText(),
				containsString("source \"" + TEST_SOURCE_ID));
		assertThat("Mail has formatted datum date", sentMail.getText(), containsString(
				"since " + mailFormattedDate(nodeDataResults.iterator().next().getTimestamp())));
		assertThat("Situation created", newSituation.hasCaptured(), is(true));
		assertThat(newSituation.getValue().getAlert(), is(equalTo(pendingAlerts.get(0))));
		assertThat(newSituation.getValue().getStatus(), is(equalTo(UserAlertSituationStatus.Active)));
		assertThat(newSituation.getValue().getNotified(), is(notNullValue()));
		assertThat("Saved alert validTo not increased",
				pendingAlerts.get(0).getValidTo().equals(pendingAlertValidTo), is(true));
	}

	@Test
	public void processOneAlertTriggerSuppressed() {
		final Instant batchTime = Instant.now();

		final UserAlert pendingAlert = newUserAlertInstance();
		pendingAlert.setStatus(UserAlertStatus.Suppressed);
		List<UserAlert> pendingAlerts = Arrays.asList(pendingAlert);
		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setNodeIds(new Long[] { TEST_NODE_ID });
		filter.setMostRecent(true);
		final Instant pendingAlertValidTo = pendingAlert.getValidTo();

		final UUID streamId = UUID.randomUUID();
		final ObjectDatumStreamFilterResults<Datum, DatumPK> nodeDataResults = newTestResults(
				ZonedDateTime.now().minusSeconds(10), streamId);

		// first query for pending alerts, starting at beginning
		expect(userAlertDao.findAlertsToProcess(UserAlertType.NodeStaleData, null, batchTime,
				service.getBatchSize())).andReturn(pendingAlerts);

		// then query for most recent node datum
		expect(datumDao.findFiltered(anyObject())).andReturn(nodeDataResults);

		// then query for the node, to grab time zone
		expect(solarNodeDao.get(TEST_NODE_ID)).andReturn(testNode);

		// then query for active situation
		expect(userAlertSituationDao.getActiveAlertSituationForAlert(pendingAlert.getId()))
				.andReturn(null);

		// then save active situation
		Capture<UserAlertSituation> newSituation = new Capture<UserAlertSituation>();
		expect(userAlertSituationDao.store(EasyMock.capture(newSituation)))
				.andReturn(AlertIdCounter.getAndIncrement());

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

	private String mailFormattedDate(Instant ts) {
		return service.getTimestampFormat().withLocale(Locale.US)
				.format(ts.atZone(ZoneId.of(TEST_TIME_ZONE_ID)));
	}

	@Test
	public void processOneAlertTriggerForUser() {
		final Instant batchTime = Instant.now();

		final UserAlert pendingAlert = newUserAlertInstance();
		pendingAlert.setNodeId(null); // change to "all nodes for user"
		List<UserAlert> pendingAlerts = Arrays.asList(pendingAlert);
		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setUserIds(new Long[] { TEST_USER_ID });
		filter.setMostRecent(true);
		final Instant pendingAlertValidTo = pendingAlert.getValidTo();

		final UUID streamId_1 = UUID.randomUUID();
		final UUID streamId_2 = UUID.randomUUID();
		final ObjectDatumStreamMetadata[] metas = new ObjectDatumStreamMetadata[] {
				emptyMeta(streamId_1, TEST_TZ, ObjectDatumKind.Node, TEST_NODE_ID, TEST_SOURCE_ID),
				emptyMeta(streamId_2, TEST_TZ, ObjectDatumKind.Node, TEST_NODE_ID_2, TEST_SOURCE_ID) };
		final ObjectDatumStreamFilterResults<Datum, DatumPK> nodeDataResults = newTestResults(
				ZonedDateTime.now().minusSeconds(10), metas,
				newDatum(ZonedDateTime.now().minusSeconds(20), streamId_2),
				newDatum(ZonedDateTime.now().minusSeconds(10), streamId_1));

		// first query for pending alerts, starting at beginning
		EasyMock.expect(userAlertDao.findAlertsToProcess(UserAlertType.NodeStaleData, null, batchTime,
				service.getBatchSize())).andReturn(pendingAlerts);

		// will next query on user ID to get available nodes
		final User testUser = new User(TEST_USER_ID, "test@localhost");
		final SolarNode testNode2 = new SolarNode(TEST_NODE_ID_2, testNode.getLocationId());
		testNode2.setLocation(testNode.getLocation());

		final List<UserNode> userNodes = Arrays.asList(new UserNode(testUser, testNode),
				new UserNode(testUser, testNode2));
		expect(userNodeDao.findUserNodesForUser(eq(testUser))).andReturn(userNodes);

		// then query for most recent node datum
		expect(datumDao.findFiltered(anyObject())).andReturn(nodeDataResults);

		// then query for active situation
		expect(userAlertSituationDao.getActiveAlertSituationForAlert(pendingAlert.getId()))
				.andReturn(null);

		// get User for alert
		expect(userDao.get(TEST_USER_ID)).andReturn(testUser);

		// then save active situation
		Capture<UserAlertSituation> newSituation = new Capture<>();
		expect(userAlertSituationDao.store(EasyMock.capture(newSituation)))
				.andReturn(AlertIdCounter.getAndIncrement());

		replayAll();
		Long startingId = service.processAlerts(null, batchTime);
		assertThat("Next staring ID is last processed alert ID", startingId, is(pendingAlert.getId()));
		assertThat("Mail sent", MailSender.getSent().size(), is(1));
		SimpleMailMessage sentMail = (SimpleMailMessage) MailSender.getSent().element();
		assertThat("Mail subject", sentMail.getSubject(),
				is("SolarNetwork alert: SolarNode " + TEST_NODE_ID_2 + " data is stale"));
		assertThat("Mail has source ID", sentMail.getText(),
				containsString("source \"" + TEST_SOURCE_ID));
		assertThat("Mail has formatted datum date", sentMail.getText(), containsString(
				"since " + mailFormattedDate(nodeDataResults.iterator().next().getTimestamp())));
		Assert.assertTrue("Situation created", newSituation.hasCaptured());
		Assert.assertEquals(pendingAlerts.get(0), newSituation.getValue().getAlert());
		Assert.assertEquals(UserAlertSituationStatus.Active, newSituation.getValue().getStatus());
		Assert.assertNotNull(newSituation.getValue().getNotified());
		Assert.assertTrue("Saved alert validTo not increased",
				pendingAlert.getValidTo().equals(pendingAlertValidTo));
		Assert.assertNotNull(newSituation.getValue().getInfo());
		Assert.assertEquals(TEST_NODE_ID_2, newSituation.getValue().getInfo()
				.get(EmailNodeStaleDataAlertProcessor.SITUATION_INFO_NODE_ID));
		Assert.assertEquals(TEST_SOURCE_ID, newSituation.getValue().getInfo()
				.get(EmailNodeStaleDataAlertProcessor.SITUATION_INFO_SOURCE_ID));
		Assert.assertEquals(nodeDataResults.iterator().next().getTimestamp().toEpochMilli(),
				newSituation.getValue().getInfo()
						.get(EmailNodeStaleDataAlertProcessor.SITUATION_INFO_DATUM_CREATED));
	}

	@Test
	public void processOneAlertTriggerForUserOutsideTimeWindow() {
		final ZoneId nodeTZ = testNode.getTimeZone().toZoneId();
		final Instant batchTime = ZonedDateTime.of(2016, 4, 1, 8, 59, 56, 0, nodeTZ).toInstant();
		service.setSystemTime(batchTime);

		final UserAlert pendingAlert = newUserAlertInstance();
		pendingAlert.setNodeId(null); // change to "all nodes for user"

		// create time window
		List<Map<String, Object>> windows = new ArrayList<Map<String, Object>>();
		Map<String, Object> window = new HashMap<String, Object>();
		window.put("timeStart", "09:00");
		window.put("timeEnd", "16:00");
		windows.add(window);
		pendingAlert.getOptions().put(UserAlertOptions.TIME_WINDOWS, windows);

		List<UserAlert> pendingAlerts = Arrays.asList(pendingAlert);
		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setUserIds(new Long[] { TEST_USER_ID });
		filter.setMostRecent(true);

		ZonedDateTime dataTimestamp = ZonedDateTime.of(2016, 4, 1, 8, 59, 50, 0, nodeTZ);
		final UUID streamId = UUID.randomUUID();
		final ObjectDatumStreamFilterResults<Datum, DatumPK> nodeDataResults = newTestResults(
				dataTimestamp, streamId);

		// first query for pending alerts, starting at beginning
		expect(userAlertDao.findAlertsToProcess(UserAlertType.NodeStaleData, null, batchTime,
				service.getBatchSize())).andReturn(pendingAlerts);

		// will next query on user ID to get available nodes
		final User testUser = new User(TEST_USER_ID, "test@localhost");

		final List<UserNode> userNodes = Arrays.asList(new UserNode(testUser, testNode));
		expect(userNodeDao.findUserNodesForUser(EasyMock.eq(testUser))).andReturn(userNodes);

		// then query for most recent node datum
		expect(datumDao.findFiltered(anyObject())).andReturn(nodeDataResults);

		// then query for active situation
		EasyMock.expect(userAlertSituationDao.getActiveAlertSituationForAlert(pendingAlert.getId()))
				.andReturn(null);

		// and finally save the alert valid date
		userAlertDao.updateValidTo(EasyMock.eq(pendingAlerts.get(0).getId()),
				EasyMock.<Instant> anyObject());

		replayAll();
		Long startingId = service.processAlerts(null, batchTime);
		Assert.assertEquals("Next staring ID is last processed alert ID", pendingAlert.getId(),
				startingId);
		Assert.assertEquals("Mail sent", 0, MailSender.getSent().size());
	}

	@Test
	public void processOneAlertTriggerForUserWithinTimeWindow() {
		final ZoneId nodeTZ = testNode.getTimeZone().toZoneId();
		final Instant batchTime = ZonedDateTime.of(2016, 4, 1, 9, 0, 1, 0, nodeTZ).toInstant();
		service.setSystemTime(batchTime);

		final UserAlert pendingAlert = newUserAlertInstance();
		pendingAlert.setNodeId(null); // change to "all nodes for user"

		// create time window
		List<Map<String, Object>> windows = new ArrayList<Map<String, Object>>();
		Map<String, Object> window = new HashMap<String, Object>();
		window.put("timeStart", "09:00");
		window.put("timeEnd", "16:00");
		windows.add(window);
		pendingAlert.getOptions().put(UserAlertOptions.TIME_WINDOWS, windows);

		List<UserAlert> pendingAlerts = Arrays.asList(pendingAlert);
		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setUserIds(new Long[] { TEST_USER_ID });
		filter.setMostRecent(true);
		final Instant pendingAlertValidTo = pendingAlert.getValidTo();

		ZonedDateTime dataTimestamp = ZonedDateTime.of(2016, 4, 1, 8, 59, 50, 0, nodeTZ);
		final UUID streamId = UUID.randomUUID();
		final ObjectDatumStreamFilterResults<Datum, DatumPK> nodeDataResults = newTestResults(
				dataTimestamp, streamId);

		// first query for pending alerts, starting at beginning
		expect(userAlertDao.findAlertsToProcess(UserAlertType.NodeStaleData, null, batchTime,
				service.getBatchSize())).andReturn(pendingAlerts);

		// will next query on user ID to get available nodes
		final User testUser = new User(TEST_USER_ID, "test@localhost");

		final List<UserNode> userNodes = Arrays.asList(new UserNode(testUser, testNode));
		expect(userNodeDao.findUserNodesForUser(EasyMock.eq(testUser))).andReturn(userNodes);

		// then query for most recent node datum
		expect(datumDao.findFiltered(anyObject())).andReturn(nodeDataResults);

		// then query for active situation
		expect(userAlertSituationDao.getActiveAlertSituationForAlert(pendingAlert.getId()))
				.andReturn(null);

		// get User for alert
		expect(userDao.get(TEST_USER_ID)).andReturn(testUser);

		//		// then save active situation
		Capture<UserAlertSituation> newSituation = new Capture<>();
		expect(userAlertSituationDao.store(EasyMock.capture(newSituation)))
				.andReturn(AlertIdCounter.getAndIncrement());

		replayAll();
		Long startingId = service.processAlerts(null, batchTime);
		assertThat("Next staring ID is last processed alert ID", startingId, is(pendingAlert.getId()));
		assertThat("Mail sent", MailSender.getSent().size(), is(1));
		SimpleMailMessage sentMail = (SimpleMailMessage) MailSender.getSent().element();
		assertThat("Mail subject", sentMail.getSubject(),
				is("SolarNetwork alert: SolarNode " + TEST_NODE_ID + " data is stale"));
		assertThat("Mail has source ID", sentMail.getText(),
				containsString("source \"" + TEST_SOURCE_ID));
		assertThat("Mail has formatted datum date", sentMail.getText(), containsString(
				"since " + mailFormattedDate(nodeDataResults.iterator().next().getTimestamp())));
		Assert.assertTrue("Situation created", newSituation.hasCaptured());
		Assert.assertEquals(pendingAlerts.get(0), newSituation.getValue().getAlert());
		Assert.assertEquals(UserAlertSituationStatus.Active, newSituation.getValue().getStatus());
		Assert.assertNotNull(newSituation.getValue().getNotified());
		Assert.assertTrue("Saved alert validTo not increased",
				pendingAlert.getValidTo().equals(pendingAlertValidTo));
		Assert.assertNotNull(newSituation.getValue().getInfo());
		Assert.assertEquals(TEST_NODE_ID, newSituation.getValue().getInfo()
				.get(EmailNodeStaleDataAlertProcessor.SITUATION_INFO_NODE_ID));
		Assert.assertEquals(TEST_SOURCE_ID, newSituation.getValue().getInfo()
				.get(EmailNodeStaleDataAlertProcessor.SITUATION_INFO_SOURCE_ID));
		Assert.assertEquals(dataTimestamp.toInstant().toEpochMilli(), newSituation.getValue().getInfo()
				.get(EmailNodeStaleDataAlertProcessor.SITUATION_INFO_DATUM_CREATED));
	}

	@Test
	public void processBatchAlertsTrigger() {
		final Instant batchTime = Instant.now();

		// add 10 alerts, so we can test batching
		List<UserAlert> pendingAlerts = new ArrayList<>();
		for ( int i = 0; i < 12; i++ ) {
			pendingAlerts.add(newUserAlertInstance());
		}

		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setNodeIds(new Long[] { TEST_NODE_ID });
		filter.setMostRecent(true);

		ZonedDateTime dataTimestamp = ZonedDateTime.now(ZoneId.of(TEST_TZ)).minusSeconds(10);
		final UUID streamId = UUID.randomUUID();
		final ObjectDatumStreamFilterResults<Datum, DatumPK> nodeDataResults = newTestResults(
				dataTimestamp, streamId);

		// first query for pending alerts, starting at beginning
		expect(userAlertDao.findAlertsToProcess(UserAlertType.NodeStaleData, null, batchTime,
				service.getBatchSize())).andReturn(pendingAlerts.subList(0, 5));

		// then query for most recent node datum
		expect(datumDao.findFiltered(anyObject())).andReturn(nodeDataResults);

		// then query for the node, to grab time zone
		expect(solarNodeDao.get(TEST_NODE_ID)).andReturn(testNode);

		Capture<UserAlertSituation> newSituation = new Capture<>(CaptureType.ALL);

		// then query for active situation
		for ( int i = 0; i < 5; i++ ) {
			expect(userAlertSituationDao.getActiveAlertSituationForAlert(pendingAlerts.get(i).getId()))
					.andReturn(null);

			// get User for alert
			expect(userDao.get(TEST_USER_ID)).andReturn(testUser);

			// then save active situation
			expect(userAlertSituationDao.store(capture(newSituation)))
					.andReturn(AlertIdCounter.getAndIncrement());
		}

		// 2nd batch query for pending alerts, starting at previous ID
		expect(userAlertDao.findAlertsToProcess(UserAlertType.NodeStaleData,
				pendingAlerts.get(4).getId(), batchTime, service.getBatchSize()))
						.andReturn(pendingAlerts.subList(5, 10));

		// then query for most recent node datum
		expect(datumDao.findFiltered(anyObject())).andReturn(nodeDataResults);

		// then query for the node, to grab time zone
		expect(solarNodeDao.get(TEST_NODE_ID)).andReturn(testNode);

		// then query for active situation
		for ( int i = 5; i < 10; i++ ) {
			expect(userAlertSituationDao.getActiveAlertSituationForAlert(pendingAlerts.get(i).getId()))
					.andReturn(null);

			// get User for alert
			expect(userDao.get(TEST_USER_ID)).andReturn(testUser);

			// then save active situation
			expect(userAlertSituationDao.store(capture(newSituation)))
					.andReturn(AlertIdCounter.getAndIncrement());
		}

		// 3rd batch query for pending alerts, starting at previous ID
		expect(userAlertDao.findAlertsToProcess(UserAlertType.NodeStaleData,
				pendingAlerts.get(9).getId(), batchTime, service.getBatchSize()))
						.andReturn(pendingAlerts.subList(10, pendingAlerts.size()));

		// then query for most recent node datum
		expect(datumDao.findFiltered(anyObject())).andReturn(nodeDataResults);

		// then query for the node, to grab time zone
		expect(solarNodeDao.get(TEST_NODE_ID)).andReturn(testNode);

		// then query for active situation
		for ( int i = 10; i < 12; i++ ) {
			expect(userAlertSituationDao.getActiveAlertSituationForAlert(pendingAlerts.get(i).getId()))
					.andReturn(null);

			// get User, SolarNode for alert
			expect(userDao.get(TEST_USER_ID)).andReturn(testUser);

			// then save active situation
			expect(userAlertSituationDao.store(EasyMock.capture(newSituation)))
					.andReturn(AlertIdCounter.getAndIncrement());
		}

		// 4th batch query for pending alerts, starting at previous ID
		expect(userAlertDao.findAlertsToProcess(UserAlertType.NodeStaleData,
				pendingAlerts.get(11).getId(), batchTime, service.getBatchSize()))
						.andReturn(Collections.<UserAlert> emptyList());

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
				for ( MailMessage sent : MailSender.getSent() ) {
					SimpleMailMessage sentMail = (SimpleMailMessage) sent;
					assertThat("Mail subject", sentMail.getSubject(),
							is("SolarNetwork alert: SolarNode " + TEST_NODE_ID + " data is stale"));
					assertThat("Mail has source ID", sentMail.getText(),
							containsString("source \"" + TEST_SOURCE_ID));
					assertThat("Mail has formatted datum date", sentMail.getText(),
							containsString("since " + mailFormattedDate(dataTimestamp.toInstant())));
				}
			}
			MailSender.getSent().clear();
		}
	}

	@Test
	public void processOneAlertResolved() {
		final Instant batchTime = Instant.now();

		List<UserAlert> pendingAlerts = Arrays.asList(newUserAlertInstance());
		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setNodeIds(new Long[] { TEST_NODE_ID });
		filter.setMostRecent(true);
		final Instant pendingAlertValidTo = pendingAlerts.get(0).getValidTo();

		ZonedDateTime dataTimestamp = ZonedDateTime.now(ZoneId.of(TEST_TZ));
		final UUID streamId = UUID.randomUUID();
		final ObjectDatumStreamFilterResults<Datum, DatumPK> nodeDataResults = newTestResults(
				dataTimestamp, streamId);

		// first query for pending alerts, starting at beginning
		expect(userAlertDao.findAlertsToProcess(UserAlertType.NodeStaleData, null, batchTime,
				service.getBatchSize())).andReturn(pendingAlerts);

		// then query for most recent node datum
		expect(datumDao.findFiltered(anyObject())).andReturn(nodeDataResults);

		// then query for the node, to grab time zone
		expect(solarNodeDao.get(TEST_NODE_ID)).andReturn(testNode);

		// then query for active situation
		final UserAlertSituation activeSituation = new UserAlertSituation();
		activeSituation.setId(AlertIdCounter.getAndIncrement());
		activeSituation.setCreated(Instant.now());
		activeSituation.setAlert(pendingAlerts.get(0));
		activeSituation.setStatus(UserAlertSituationStatus.Active);
		expect(userAlertSituationDao.getActiveAlertSituationForAlert(pendingAlerts.get(0).getId()))
				.andReturn(activeSituation);

		// get User for alert
		expect(userDao.get(TEST_USER_ID)).andReturn(testUser);

		// then save active situation -> resolved
		expect(userAlertSituationDao.store(activeSituation)).andReturn(activeSituation.getId());

		// and finally save the alert valid date
		userAlertDao.updateValidTo(eq(pendingAlerts.get(0).getId()), anyObject());

		replayAll();
		Long startingId = service.processAlerts(null, batchTime);
		assertThat("Next staring ID is last processed alert ID", startingId,
				is(pendingAlerts.get(0).getId()));
		assertThat("Mail sent", MailSender.getSent().size(), is(1));
		SimpleMailMessage sentMail = (SimpleMailMessage) MailSender.getSent().element();
		assertThat("Mail subject", sentMail.getSubject(), is(
				"SolarNetwork alert resolved: SolarNode " + TEST_NODE_ID + " data is no longer stale"));
		assertThat("Mail has source ID", sentMail.getText(),
				containsString("source \"" + TEST_SOURCE_ID));
		assertThat("Mail has formatted datum date", sentMail.getText(), containsString(
				"on " + mailFormattedDate(nodeDataResults.iterator().next().getTimestamp())));
		Assert.assertEquals(UserAlertSituationStatus.Resolved, activeSituation.getStatus());
		Assert.assertNotNull(activeSituation.getNotified());
		Assert.assertTrue("Saved alert validTo increased",
				pendingAlerts.get(0).getValidTo().isAfter(pendingAlertValidTo));
	}

}
