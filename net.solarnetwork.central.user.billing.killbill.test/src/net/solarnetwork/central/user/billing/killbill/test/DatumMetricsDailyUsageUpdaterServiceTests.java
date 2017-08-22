/* ==================================================================
 * DatumMetricsDailyUsageUpdaterJobTests.java - 22/08/2017 1:39:59 PM
 * 
 * Copyright 2017 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.billing.killbill.test;

import static net.solarnetwork.central.user.billing.killbill.DatumMetricsDailyUsageUpdaterService.DEFAULT_BASE_PLAN_NAME;
import static net.solarnetwork.central.user.billing.killbill.DatumMetricsDailyUsageUpdaterService.DEFAULT_BATCH_SIZE;
import static net.solarnetwork.central.user.billing.killbill.DatumMetricsDailyUsageUpdaterService.DEFAULT_BILL_CYCLE_DAY;
import static net.solarnetwork.central.user.billing.killbill.DatumMetricsDailyUsageUpdaterService.DEFAULT_PAMENT_METHOD_DATA;
import static net.solarnetwork.central.user.billing.killbill.DatumMetricsDailyUsageUpdaterService.DEFAULT_USAGE_UNIT_NAME;
import static net.solarnetwork.central.user.billing.killbill.DatumMetricsDailyUsageUpdaterService.KILLBILL_ACCOUNT_KEY_DATA_PROP;
import static net.solarnetwork.central.user.billing.killbill.DatumMetricsDailyUsageUpdaterService.KILLBILL_DAILY_USAGE_PLAN_DATA_PROP;
import static net.solarnetwork.central.user.billing.killbill.DatumMetricsDailyUsageUpdaterService.KILLBILL_MOST_RECENT_USAGE_KEY_DATA_PROP;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isNull;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import org.easymock.Capture;
import org.easymock.CaptureType;
import org.easymock.EasyMock;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Interval;
import org.joda.time.ReadableInterval;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.central.dao.SolarLocationDao;
import net.solarnetwork.central.datum.dao.GeneralNodeDatumDao;
import net.solarnetwork.central.datum.domain.DatumFilterCommand;
import net.solarnetwork.central.domain.FilterResults;
import net.solarnetwork.central.domain.SolarLocation;
import net.solarnetwork.central.domain.SolarNode;
import net.solarnetwork.central.support.BasicFilterResults;
import net.solarnetwork.central.user.billing.domain.BillingDataConstants;
import net.solarnetwork.central.user.billing.killbill.DatumMetricsDailyUsageUpdaterJob;
import net.solarnetwork.central.user.billing.killbill.DatumMetricsDailyUsageUpdaterService;
import net.solarnetwork.central.user.billing.killbill.KillbillClient;
import net.solarnetwork.central.user.billing.killbill.domain.Account;
import net.solarnetwork.central.user.billing.killbill.domain.Bundle;
import net.solarnetwork.central.user.billing.killbill.domain.Subscription;
import net.solarnetwork.central.user.billing.killbill.domain.UsageRecord;
import net.solarnetwork.central.user.dao.UserDao;
import net.solarnetwork.central.user.dao.UserNodeDao;
import net.solarnetwork.central.user.domain.User;
import net.solarnetwork.central.user.domain.UserFilterCommand;
import net.solarnetwork.central.user.domain.UserFilterMatch;
import net.solarnetwork.central.user.domain.UserMatch;
import net.solarnetwork.central.user.domain.UserNode;

/**
 * Test cases for the {@link DatumMetricsDailyUsageUpdaterJob} class.
 * 
 * @author matt
 * @version 1.0
 */
public class DatumMetricsDailyUsageUpdaterServiceTests {

	private static final DateTimeFormatter ISO_DATE_FORMATTER = ISODateTimeFormat.date();

	private static final Long TEST_USER_ID = -1L;
	private static final String TEST_USER_EMAIL = "test@localhost";
	private static final String TEST_USER_NAME = "John Doe";
	private static final String TEST_USER_ACCOUNT_KEY = "SN_" + TEST_USER_ID;
	private static final Long TEST_NODE_ID = -2L;
	private static final Long TEST_LOCATION_ID = -3L;
	private static final String TEST_LOCATION_COUNTRY = "NZ";
	private static final String TEST_NODE_TZ = "Pacific/Auckland";
	private static final String TEST_NODE_TZ_OFFSET = "+12:00";
	private static final String TEST_ACCOUNT_ID = "abc-123";
	private static final String TEST_PAYMENT_METHOD_ID = "def-234";
	private static final String TEST_ACCOUNT_CURRENCY = "NZD";
	private static final String TEST_BUNDLE_KEY = "IN_" + TEST_NODE_ID;
	private static final String TEST_BUNDLE_ID = "efg-345";

	private static final Long TEST_USER2_ID = -4L;
	private static final String TEST_USER2_EMAIL = "test2@localhost";
	private static final String TEST_USER2_NAME = "John2 Doe";
	private static final String TEST_USER2_ACCOUNT_KEY = "SN_" + TEST_USER2_ID;
	private static final Long TEST_NODE2_ID = -5L;
	private static final Long TEST_LOCATION2_ID = -6L;
	private static final String TEST_LOCATION2_COUNTRY = "US";
	private static final String TEST_NODE2_TZ_OFFSET = "-08:00";
	private static final String TEST_ACCOUNT2_ID = "hij-456";
	private static final String TEST_ACCOUNT2_CURRENCY = "USD";
	private static final String TEST_BUNDLE2_KEY = "IN_" + TEST_NODE2_ID;
	private static final String TEST_BUNDLE2_ID = "klm-567";

	private DatumMetricsDailyUsageUpdaterService service;

	private SolarLocationDao locationDao;
	private UserDao userDao;
	private UserNodeDao userNodeDao;
	private GeneralNodeDatumDao nodeDatumDao;
	private KillbillClient client;

	@Before
	public void setup() {
		locationDao = EasyMock.createMock(SolarLocationDao.class);
		userDao = EasyMock.createMock(UserDao.class);
		userNodeDao = EasyMock.createMock(UserNodeDao.class);
		nodeDatumDao = EasyMock.createMock(GeneralNodeDatumDao.class);
		client = EasyMock.createMock(KillbillClient.class);
		service = new DatumMetricsDailyUsageUpdaterService(locationDao, userDao, userNodeDao,
				nodeDatumDao, client);
	}

	private void replayAll() {
		replay(locationDao, userDao, userNodeDao, nodeDatumDao, client);
	}

	private void verifyAll() {
		verify(locationDao, userDao, userNodeDao, nodeDatumDao, client);
	}

	@After
	public void teardown() {
		verifyAll();
	}

	private Account createTestAccount() {
		Account account = new Account();
		account.setAccountId(TEST_ACCOUNT_ID);
		account.setCountry(TEST_LOCATION_COUNTRY);
		account.setCurrency(TEST_ACCOUNT_CURRENCY);
		account.setEmail(TEST_USER_EMAIL);
		account.setExternalKey(TEST_USER_ACCOUNT_KEY);
		account.setPaymentMethodId(TEST_PAYMENT_METHOD_ID);
		account.setTimeZone(TEST_NODE_TZ_OFFSET);
		return account;
	}

	private Account createTestAccount2() {
		Account account = new Account();
		account.setAccountId(TEST_ACCOUNT2_ID);
		account.setCountry(TEST_LOCATION2_COUNTRY);
		account.setCurrency(TEST_ACCOUNT2_CURRENCY);
		account.setEmail(TEST_USER2_EMAIL);
		account.setExternalKey(TEST_USER2_ACCOUNT_KEY);
		account.setPaymentMethodId(TEST_PAYMENT_METHOD_ID);
		account.setTimeZone(TEST_NODE2_TZ_OFFSET);
		return account;
	}

	private Map<String, Object> userSearchBillingData() {
		Map<String, Object> killbillAccountFilter = new HashMap<>();
		killbillAccountFilter.put(BillingDataConstants.ACCOUNTING_DATA_PROP,
				DatumMetricsDailyUsageUpdaterService.KILLBILL_ACCOUNTING_VALUE);
		killbillAccountFilter.put(KILLBILL_DAILY_USAGE_PLAN_DATA_PROP, true);
		return killbillAccountFilter;
	}

	@Test
	public void noUsersConfiguredForKillbill() {
		// search for users configured to use killbill; but find none
		Capture<UserFilterCommand> filterCapture = new Capture<>();

		List<UserFilterMatch> usersWithAccounting = Collections.emptyList();
		FilterResults<UserFilterMatch> userAccountingSearchResults = new BasicFilterResults<>(
				usersWithAccounting);
		expect(userDao.findFiltered(capture(filterCapture), isNull(), eq(0), eq(DEFAULT_BATCH_SIZE)))
				.andReturn(userAccountingSearchResults);

		replayAll();

		service.execute();

		// verify filter
		assertEquals("Search filter", userSearchBillingData(),
				filterCapture.getValue().getBillingData());
	}

	@Test
	public void oneUserNoAccountNoData() {
		// search for users configured to use killbill; find one
		Map<String, Object> userBillingData = userSearchBillingData();
		User user = new User(TEST_USER_ID, TEST_USER_EMAIL);
		user.setBillingData(userBillingData);
		user.setLocationId(TEST_LOCATION_ID);
		Capture<UserFilterCommand> filterCapture = new Capture<>();
		List<UserFilterMatch> usersWithAccounting = new ArrayList<>();
		UserMatch userMatch = new UserMatch(TEST_USER_ID, TEST_USER_EMAIL);
		userMatch.setBillingData(userBillingData);
		userMatch.setLocationId(TEST_LOCATION_ID);
		userMatch.setName(TEST_USER_NAME);
		usersWithAccounting.add(userMatch);
		FilterResults<UserFilterMatch> userAccountingSearchResults = new BasicFilterResults<>(
				usersWithAccounting, 1L, 0, 1);
		expect(userDao.findFiltered(capture(filterCapture), isNull(), eq(0), eq(DEFAULT_BATCH_SIZE)))
				.andReturn(userAccountingSearchResults);

		// configure the account key based on user ID because it's not already configured
		expect(userDao.storeBillingDataProperty(TEST_USER_ID, KILLBILL_ACCOUNT_KEY_DATA_PROP,
				TEST_USER_ACCOUNT_KEY)).andReturn(1);

		// look for Killbill Account
		expect(client.accountForExternalKey(TEST_USER_ACCOUNT_KEY)).andReturn(null);

		// because Account not found, create one now, using the user's location for time zone
		SolarLocation location = new SolarLocation();
		location.setId(TEST_LOCATION_ID);
		location.setCountry(TEST_LOCATION_COUNTRY);
		location.setTimeZoneId(TEST_NODE_TZ);
		expect(locationDao.get(TEST_LOCATION_ID)).andReturn(location);
		Capture<Account> accountCapture = new Capture<>(CaptureType.ALL);
		expect(client.createAccount(capture(accountCapture))).andReturn(TEST_ACCOUNT_ID);

		// add payment method to account, as not configured yet
		expect(client.addPaymentMethodToAccount(capture(accountCapture), eq(DEFAULT_PAMENT_METHOD_DATA),
				eq(true))).andReturn(TEST_PAYMENT_METHOD_ID);

		// now iterate over all user's nodes to look for usage
		SolarNode node = new SolarNode(TEST_NODE_ID, TEST_LOCATION_ID);
		UserNode userNode = new UserNode(user, node);
		List<UserNode> allUserNodes = Collections.singletonList(userNode);
		expect(userNodeDao.findUserNodesForUser(userMatch)).andReturn(allUserNodes);

		// FOR EACH UserNode here; we have just one node; but no audit data
		expect(nodeDatumDao.getAuditInterval(TEST_NODE_ID, null)).andReturn(null);

		replayAll();

		service.execute();

		// verify search for users
		assertNotNull(filterCapture.getValue());
		assertEquals(userBillingData, filterCapture.getValue().getBillingData());

		// verify created account
		List<Account> accounts = accountCapture.getValues();
		assertNotNull("Created account", accounts);
		Account account = accounts.get(0);
		for ( int i = 0; i < accounts.size(); i++ ) {
			assertSame(accounts.get(0), account);
		}
		assertEquals("Account ID", TEST_ACCOUNT_ID, account.getAccountId());
		assertEquals("Country", TEST_LOCATION_COUNTRY, account.getCountry());
		assertEquals("Currency", TEST_ACCOUNT_CURRENCY, account.getCurrency());
		assertEquals("Email", TEST_USER_EMAIL, account.getEmail());
		assertEquals("ExternalKey", TEST_USER_ACCOUNT_KEY, account.getExternalKey());
		assertEquals("Name", TEST_USER_NAME, account.getName());
		assertEquals("TimeZone", TEST_NODE_TZ_OFFSET, account.getTimeZone());
	}

	@Test
	public void oneUserNoData() {
		// search for users configured to use killbill; find one
		Map<String, Object> killbillAccountFilter = userSearchBillingData();

		Map<String, Object> userBillingData = new HashMap<>(killbillAccountFilter);
		userBillingData.put(DatumMetricsDailyUsageUpdaterService.KILLBILL_ACCOUNT_KEY_DATA_PROP,
				TEST_USER_ACCOUNT_KEY);
		User user = new User(TEST_USER_ID, TEST_USER_EMAIL);
		user.setBillingData(userBillingData);
		user.setLocationId(TEST_LOCATION_ID);
		Capture<UserFilterCommand> filterCapture = new Capture<>();
		List<UserFilterMatch> usersWithAccounting = new ArrayList<>();
		UserMatch userMatch = new UserMatch(TEST_USER_ID, TEST_USER_EMAIL);
		userMatch.setBillingData(userBillingData);
		userMatch.setLocationId(TEST_LOCATION_ID);
		userMatch.setName(TEST_USER_NAME);
		usersWithAccounting.add(userMatch);
		FilterResults<UserFilterMatch> userAccountingSearchResults = new BasicFilterResults<>(
				usersWithAccounting, 1L, 0, 1);
		expect(userDao.findFiltered(capture(filterCapture), isNull(), eq(0), eq(DEFAULT_BATCH_SIZE)))
				.andReturn(userAccountingSearchResults);

		// get Killbill Account
		Account account = createTestAccount();
		expect(client.accountForExternalKey(TEST_USER_ACCOUNT_KEY)).andReturn(account);

		// now iterate over all user's nodes to look for usage
		SolarNode node = new SolarNode(TEST_NODE_ID, TEST_LOCATION_ID);
		UserNode userNode = new UserNode(user, node);
		List<UserNode> allUserNodes = Collections.singletonList(userNode);
		expect(userNodeDao.findUserNodesForUser(userMatch)).andReturn(allUserNodes);

		// FOR EACH UserNode here; we have just one node; but no audit data
		expect(nodeDatumDao.getAuditInterval(TEST_NODE_ID, null)).andReturn(null);

		replayAll();

		service.execute();

		// verify search for users
		assertNotNull(filterCapture.getValue());
		assertEquals(killbillAccountFilter, filterCapture.getValue().getBillingData());
	}

	@Test
	public void oneUserCatchupData() {
		// search for users configured to use killbill; find one
		Map<String, Object> killbillAccountFilter = userSearchBillingData();

		Map<String, Object> userBillingData = new HashMap<>(killbillAccountFilter);
		userBillingData.put(DatumMetricsDailyUsageUpdaterService.KILLBILL_ACCOUNT_KEY_DATA_PROP,
				TEST_USER_ACCOUNT_KEY);
		User user = new User(TEST_USER_ID, TEST_USER_EMAIL);
		user.setBillingData(userBillingData);
		user.setLocationId(TEST_LOCATION_ID);
		Capture<UserFilterCommand> filterCapture = new Capture<>();
		List<UserFilterMatch> usersWithAccounting = new ArrayList<>();
		UserMatch userMatch = new UserMatch(TEST_USER_ID, TEST_USER_EMAIL);
		userMatch.setBillingData(userBillingData);
		userMatch.setLocationId(TEST_LOCATION_ID);
		userMatch.setName(TEST_USER_NAME);
		usersWithAccounting.add(userMatch);
		FilterResults<UserFilterMatch> userAccountingSearchResults = new BasicFilterResults<>(
				usersWithAccounting, 1L, 0, 1);
		expect(userDao.findFiltered(capture(filterCapture), isNull(), eq(0), eq(DEFAULT_BATCH_SIZE)))
				.andReturn(userAccountingSearchResults);

		// get Killbill Account
		Account account = createTestAccount();
		expect(client.accountForExternalKey(TEST_USER_ACCOUNT_KEY)).andReturn(account);

		// now iterate over all user's nodes to look for usage
		SolarNode node = new SolarNode(TEST_NODE_ID, TEST_LOCATION_ID);
		UserNode userNode = new UserNode(user, node);
		List<UserNode> allUserNodes = Collections.singletonList(userNode);
		expect(userNodeDao.findUserNodesForUser(userMatch)).andReturn(allUserNodes);

		// FOR EACH UserNode here; we have just one node
		DateTime auditDataStart = new DateTime(2017, 1, 1, 0, 0, DateTimeZone.forOffsetHours(12));
		DateTime auditDataEnd = auditDataStart.plusDays(3);
		ReadableInterval auditInterval = new Interval(auditDataStart, auditDataEnd);
		expect(nodeDatumDao.getAuditInterval(TEST_NODE_ID, null)).andReturn(auditInterval);

		List<DateTime> dayList = new ArrayList<>(3);
		List<Long> countList = new ArrayList<>(3);
		for ( int i = 0; i < 3; i++ ) {
			dayList.add(auditDataStart.plusDays(i));
			countList.add((long) (Math.random() * 100000));
		}

		// now iterate over each DAY in audit interval, gathering usage values
		for ( ListIterator<DateTime> itr = dayList.listIterator(); itr.hasNext(); ) {
			DateTime day = itr.next();
			DatumFilterCommand filter = new DatumFilterCommand();
			filter.setNodeId(TEST_NODE_ID);
			filter.setStartDate(day);
			filter.setEndDate(day.plusDays(1));
			expect(nodeDatumDao.getAuditPropertyCountTotal(filter))
					.andReturn(countList.get(itr.previousIndex()));
		}

		// get the Bundle for this node, but it doesn't exist yet so it will be created
		expect(client.bundleForExternalKey(TEST_BUNDLE_KEY)).andReturn(null);

		// to decide the requestedDate, find the earliest date with node data and use that
		DateTime nodeDataStart = new DateTime(2015, 1, 1, 0, 0, DateTimeZone.forOffsetHours(12));
		DateTime nodeDataEnd = auditDataEnd.plusMinutes(15);
		ReadableInterval nodeDataInterval = new Interval(nodeDataStart, nodeDataEnd);
		expect(nodeDatumDao.getReportableInterval(TEST_NODE_ID, null)).andReturn(nodeDataInterval);

		Capture<Bundle> bundleCapture = new Capture<>();
		expect(client.createAccountBundle(eq(account), eq(nodeDataStart.toLocalDate()),
				capture(bundleCapture))).andReturn(TEST_BUNDLE_ID);

		Capture<Subscription> subscriptionCapture = new Capture<>();
		Capture<List<UsageRecord>> usageCapture = new Capture<>();
		client.addUsage(capture(subscriptionCapture), eq(DEFAULT_USAGE_UNIT_NAME),
				capture(usageCapture));

		// finally, store the "most recent usage" date for future processing
		expect(userDao.storeBillingDataProperty(TEST_USER_ID, KILLBILL_MOST_RECENT_USAGE_KEY_DATA_PROP,
				ISO_DATE_FORMATTER.print(auditDataEnd.toLocalDate()))).andReturn(1);

		replayAll();

		service.execute();

		// verify search for users
		assertNotNull(filterCapture.getValue());
		assertEquals(killbillAccountFilter, filterCapture.getValue().getBillingData());

		// verify bundle
		Bundle bundle = bundleCapture.getValue();
		assertNotNull(bundle);
		assertEquals("Account ID", TEST_ACCOUNT_ID, bundle.getAccountId());
		assertEquals("Bundle ID", TEST_BUNDLE_ID, bundle.getBundleId());
		assertEquals("External key", TEST_BUNDLE_KEY, bundle.getExternalKey());
		assertNotNull("Subscriptions", bundle.getSubscriptions());
		assertEquals("Subscription count", 1, bundle.getSubscriptions().size());
		Subscription subscription = bundle.getSubscriptions().get(0);
		assertEquals("Bill cycle day", DEFAULT_BILL_CYCLE_DAY, subscription.getBillCycleDayLocal());
		assertEquals("Plan name", DEFAULT_BASE_PLAN_NAME, subscription.getPlanName());
		assertEquals("Plan name", Subscription.BASE_PRODUCT_CATEGORY, subscription.getProductCategory());

		// verify usage
		List<UsageRecord> usage = usageCapture.getValue();
		assertNotNull("Usage", usage);
		assertEquals("Usage count", 3, usage.size());
		for ( ListIterator<Long> itr = countList.listIterator(); itr.hasNext(); ) {
			BigDecimal expectedCount = new BigDecimal(itr.next());
			UsageRecord rec = usage.get(itr.previousIndex());
			assertEquals("Usage amount " + itr.previousIndex(), expectedCount, rec.getAmount());
			assertEquals("Usage date " + itr.previousIndex(),
					dayList.get(itr.previousIndex()).toLocalDate(), rec.getRecordDate());
		}
	}

	@Test
	public void oneUserAddOneMoreDayDataMultipleUsersDifferentTimeZones() {
		// search for users configured to use killbill; find one
		Map<String, Object> killbillAccountFilter = userSearchBillingData();

		Map<String, Object> userBillingData = new HashMap<>(killbillAccountFilter);
		userBillingData.put(DatumMetricsDailyUsageUpdaterService.KILLBILL_ACCOUNT_KEY_DATA_PROP,
				TEST_USER_ACCOUNT_KEY);

		User user = new User(TEST_USER_ID, TEST_USER_EMAIL);
		user.setLocationId(TEST_LOCATION_ID);
		user.setBillingData(userBillingData);

		Map<String, Object> user2BillingData = new HashMap<>(killbillAccountFilter);
		user2BillingData.put(DatumMetricsDailyUsageUpdaterService.KILLBILL_ACCOUNT_KEY_DATA_PROP,
				TEST_USER2_ACCOUNT_KEY);

		User user2 = new User(TEST_USER2_ID, TEST_USER2_EMAIL);
		user2.setLocationId(TEST_LOCATION2_ID);
		user2.setBillingData(user2BillingData);

		Capture<UserFilterCommand> filterCapture = new Capture<>();
		List<UserFilterMatch> usersWithAccounting = new ArrayList<>();

		UserMatch userMatch = new UserMatch(TEST_USER_ID, TEST_USER_EMAIL);
		userMatch.setBillingData(userBillingData);
		userMatch.setLocationId(TEST_LOCATION_ID);
		userMatch.setName(TEST_USER_NAME);
		usersWithAccounting.add(userMatch);

		UserMatch user2Match = new UserMatch(TEST_USER2_ID, TEST_USER2_EMAIL);
		user2Match.setBillingData(user2BillingData);
		user2Match.setLocationId(TEST_LOCATION2_ID);
		user2Match.setName(TEST_USER2_NAME);
		usersWithAccounting.add(user2Match);

		FilterResults<UserFilterMatch> userAccountingSearchResults = new BasicFilterResults<>(
				usersWithAccounting, 2L, 0, 2);
		expect(userDao.findFiltered(capture(filterCapture), isNull(), eq(0), eq(DEFAULT_BATCH_SIZE)))
				.andReturn(userAccountingSearchResults);

		SolarNode node = new SolarNode(TEST_NODE_ID, TEST_LOCATION_ID);
		Account account = createTestAccount();
		Bundle bundle = new Bundle();
		bundle.setBundleId(TEST_BUNDLE_ID);
		bundle.setAccountId(TEST_ACCOUNT_ID);
		bundle.setExternalKey(TEST_BUNDLE_KEY);
		bundle.setSubscriptions(Collections.singletonList(new Subscription()));
		Capture<Subscription> subscriptionCapture = new Capture<>();
		Capture<List<UsageRecord>> usageCapture = new Capture<>();
		List<DateTime> dayList = new ArrayList<>();
		List<Long> countList = new ArrayList<>();
		processUser(user, userMatch, node, account, bundle, 1, subscriptionCapture, usageCapture,
				dayList, countList);
		// update the "most recent usage" date to the last day, to start processing there
		userBillingData.put(KILLBILL_MOST_RECENT_USAGE_KEY_DATA_PROP,
				ISO_DATE_FORMATTER.print(dayList.get(0).toLocalDate()));

		SolarNode node2 = new SolarNode(TEST_NODE2_ID, TEST_LOCATION2_ID);
		Account account2 = createTestAccount2();
		Bundle bundle2 = new Bundle();
		bundle2.setBundleId(TEST_BUNDLE2_ID);
		bundle2.setAccountId(TEST_ACCOUNT2_ID);
		bundle2.setExternalKey(TEST_BUNDLE2_KEY);
		bundle2.setSubscriptions(Collections.singletonList(new Subscription()));
		Capture<Subscription> subscription2Capture = new Capture<>();
		Capture<List<UsageRecord>> usage2Capture = new Capture<>();
		List<DateTime> day2List = new ArrayList<>();
		List<Long> count2List = new ArrayList<>();
		processUser(user2, user2Match, node2, account2, bundle2, 1, subscription2Capture, usage2Capture,
				day2List, count2List);
		// update the "most recent usage" date to the last day, to start processing there
		user2BillingData.put(KILLBILL_MOST_RECENT_USAGE_KEY_DATA_PROP,
				ISO_DATE_FORMATTER.print(day2List.get(0).toLocalDate()));

		replayAll();

		service.execute();

		// verify search for users
		assertNotNull(filterCapture.getValue());
		assertEquals(killbillAccountFilter, filterCapture.getValue().getBillingData());

		// verify usage
		List<UsageRecord> usage = usageCapture.getValue();
		assertNotNull("Usage", usage);
		assertEquals("Usage count", 1, usage.size());
		for ( ListIterator<Long> itr = countList.listIterator(); itr.hasNext(); ) {
			BigDecimal expectedCount = new BigDecimal(itr.next());
			UsageRecord rec = usage.get(itr.previousIndex());
			assertEquals("Usage amount " + itr.previousIndex(), expectedCount, rec.getAmount());
			assertEquals("Usage date " + itr.previousIndex(),
					dayList.get(itr.previousIndex()).toLocalDate(), rec.getRecordDate());
		}

		usage = usage2Capture.getValue();
		assertNotNull("Usage", usage);
		assertEquals("Usage count", 1, usage.size());
		for ( ListIterator<Long> itr = count2List.listIterator(); itr.hasNext(); ) {
			BigDecimal expectedCount = new BigDecimal(itr.next());
			UsageRecord rec = usage.get(itr.previousIndex());
			assertEquals("Usage amount " + itr.previousIndex(), expectedCount, rec.getAmount());
			assertEquals("Usage date " + itr.previousIndex(),
					day2List.get(itr.previousIndex()).toLocalDate(), rec.getRecordDate());
		}
	}

	private void processUser(User user, UserMatch userMatch, SolarNode node, Account account,
			Bundle bundle, int numDays, Capture<Subscription> subscriptionCapture,
			Capture<List<UsageRecord>> usageCapture, List<DateTime> dayList, List<Long> countList) {
		// get Killbill Account #1
		expect(client.accountForExternalKey(account.getExternalKey())).andReturn(account);

		// now iterate over all user's nodes to look for usage
		UserNode userNode = new UserNode(user, node);
		List<UserNode> allUserNodes = Collections.singletonList(userNode);
		expect(userNodeDao.findUserNodesForUser(userMatch)).andReturn(allUserNodes);

		DateTimeZone timeZone = DateTimeZone.forID(account.getTimeZone());

		// FOR EACH UserNode here; we have just one node
		DateTime auditDataStart = new DateTime(2017, 1, 1, 0, 0, timeZone);
		DateTime auditDataEnd = auditDataStart.plusDays(numDays);
		ReadableInterval auditInterval = new Interval(auditDataStart, auditDataEnd);
		expect(nodeDatumDao.getAuditInterval(node.getId(), null)).andReturn(auditInterval);

		for ( int i = 0; i < numDays; i++ ) {
			dayList.add(auditDataStart.plusDays(i));
			countList.add((long) (Math.random() * 100000));
		}

		// now iterate over each DAY in audit interval, gathering usage values
		for ( ListIterator<DateTime> itr = dayList.listIterator(); itr.hasNext(); ) {
			DateTime day = itr.next();
			DatumFilterCommand filter = new DatumFilterCommand();
			filter.setNodeId(node.getId());
			filter.setStartDate(day);
			filter.setEndDate(day.plusDays(1));
			expect(nodeDatumDao.getAuditPropertyCountTotal(filter))
					.andReturn(countList.get(itr.previousIndex()));
		}

		// get the Bundle for this node
		expect(client.bundleForExternalKey(bundle.getExternalKey())).andReturn(bundle);

		client.addUsage(capture(subscriptionCapture), eq(DEFAULT_USAGE_UNIT_NAME),
				capture(usageCapture));

		// finally, store the "most recent usage" date for future processing
		expect(userDao.storeBillingDataProperty(user.getId(), KILLBILL_MOST_RECENT_USAGE_KEY_DATA_PROP,
				ISO_DATE_FORMATTER.print(auditDataEnd.toLocalDate()))).andReturn(1);

	}

}
