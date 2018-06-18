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

package net.solarnetwork.central.user.billing.killbill.jobs.test;

import static java.util.Collections.singletonList;
import static net.solarnetwork.central.user.billing.killbill.KillbillClient.ISO_DATE_FORMATTER;
import static net.solarnetwork.central.user.billing.killbill.jobs.DailyUsageUpdaterService.CUSTOM_FIELD_NODE_ID;
import static net.solarnetwork.central.user.billing.killbill.jobs.DailyUsageUpdaterService.DEFAULT_ACCOUNT_BILL_CYCLE_DAY;
import static net.solarnetwork.central.user.billing.killbill.jobs.DailyUsageUpdaterService.DEFAULT_BATCH_SIZE;
import static net.solarnetwork.central.user.billing.killbill.jobs.DailyUsageUpdaterService.DEFAULT_SUBSCRIPTION_BILL_CYCLE_DAY;
import static net.solarnetwork.central.user.billing.killbill.jobs.DailyUsageUpdaterService.EXTERNAL_PAYMENT_METHOD_DATA;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isNull;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.same;
import static org.easymock.EasyMock.verify;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.UUID;
import javax.cache.Cache;
import org.easymock.Capture;
import org.easymock.CaptureType;
import org.easymock.EasyMock;
import org.hamcrest.Matchers;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Interval;
import org.joda.time.LocalDate;
import org.joda.time.ReadableInterval;
import org.junit.After;
import org.junit.Assert;
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
import net.solarnetwork.central.user.billing.killbill.KillbillBillingSystem;
import net.solarnetwork.central.user.billing.killbill.KillbillClient;
import net.solarnetwork.central.user.billing.killbill.UserDataProperties;
import net.solarnetwork.central.user.billing.killbill.domain.Account;
import net.solarnetwork.central.user.billing.killbill.domain.Bundle;
import net.solarnetwork.central.user.billing.killbill.domain.CustomField;
import net.solarnetwork.central.user.billing.killbill.domain.Subscription;
import net.solarnetwork.central.user.billing.killbill.domain.TagDefinition;
import net.solarnetwork.central.user.billing.killbill.domain.UsageRecord;
import net.solarnetwork.central.user.billing.killbill.jobs.DailyUsageUpdaterService;
import net.solarnetwork.central.user.dao.UserDao;
import net.solarnetwork.central.user.dao.UserNodeDao;
import net.solarnetwork.central.user.domain.User;
import net.solarnetwork.central.user.domain.UserFilterCommand;
import net.solarnetwork.central.user.domain.UserFilterMatch;
import net.solarnetwork.central.user.domain.UserMatch;
import net.solarnetwork.central.user.domain.UserNode;

/**
 * Test cases for the {@link DailyUsageUpdaterService} class.
 * 
 * @author matt
 * @version 1.0
 */
public class DailyUsageUpdaterServiceTests {

	private static final String KILLBILL_DAILY_USAGE_PLAN_DATA_PROP = "kb_datumMetricsDailyUsage";
	private static final String KILLBILL_MOST_RECENT_USAGE_KEYS_DATA_PROP = "kb_mrUsageDates";

	private static final String DEFAULT_BASE_PLAN_NAME = "api-posted-datum-metric-monthly-usage";
	private static final String DEFAULT_USAGE_UNIT_NAME = "DatumMetrics";
	private static final String DEFAULT_AUDIT_PROP_NAME = "Property";

	private static final String KILLBILL_QUERY_ADDON_PLAN_DATA_PROP = "kb_datumQueryDailyUsage";
	private static final String KILLBILL_QUERY_ADDIN_MOST_RECENT_USAGE_KEYS_DATA_PROP = "kb_mrQueryUsageDates";
	private static final String ADDON_PLAN_NAME = "api-query-datum-monthly-usage";
	private static final String ADDON_AUDIT_PROP_NAME = "DatumQuery";
	private static final String QUERY_USAGE_UNIT_NAME = "DatumQuery";

	private static final Long TEST_USER_ID = -1L;
	private static final String TEST_USER_EMAIL = "test@localhost";
	private static final String TEST_USER_NAME = "John Doe";
	private static final String TEST_USER_ACCOUNT_KEY = "SN_" + TEST_USER_ID;
	private static final Long TEST_NODE_ID = -2L;
	private static final Long TEST_LOCATION_ID = -3L;
	private static final String TEST_LOCATION_COUNTRY = "NZ";
	private static final String TEST_NODE_TZ = "Pacific/Auckland";
	private static final String TEST_ACCOUNT_ID = "abc-123";
	private static final String TEST_ACCOUNT_CURRENCY = "NZD";
	private static final String TEST_PAYMENT_METHOD_ID = "def-234";
	private static final String TEST_BUNDLE_KEY = "IN_" + TEST_NODE_ID;
	private static final String TEST_BUNDLE_ID = "efg-345";
	private static final String TEST_SUBSCRIPTION_ID = "efg-345-678";

	private static final Long TEST_USER2_ID = -4L;
	private static final String TEST_USER2_EMAIL = "test2@localhost";
	private static final String TEST_USER2_NAME = "John2 Doe";
	private static final String TEST_USER2_ACCOUNT_KEY = "SN_" + TEST_USER2_ID;
	private static final Long TEST_NODE2_ID = -5L;
	private static final Long TEST_LOCATION2_ID = -6L;
	private static final String TEST_LOCATION2_COUNTRY = "US";
	private static final String TEST_NODE2_TZ = "America/Los_Angeles";
	private static final String TEST_ACCOUNT2_ID = "hij-456";
	private static final String TEST_ACCOUNT2_CURRENCY = "USD";
	private static final String TEST_BUNDLE2_KEY = "IN_" + TEST_NODE2_ID;
	private static final String TEST_BUNDLE2_ID = "klm-567";

	private DailyUsageUpdaterService service;

	private SolarLocationDao locationDao;
	private UserDao userDao;
	private UserNodeDao userNodeDao;
	private GeneralNodeDatumDao nodeDatumDao;
	private KillbillClient client;
	private Cache<String, TagDefinition> tagDefinitionCache;

	@SuppressWarnings("unchecked")
	@Before
	public void setup() {
		locationDao = EasyMock.createMock(SolarLocationDao.class);
		userDao = EasyMock.createMock(UserDao.class);
		userNodeDao = EasyMock.createMock(UserNodeDao.class);
		nodeDatumDao = EasyMock.createMock(GeneralNodeDatumDao.class);
		client = EasyMock.createMock(KillbillClient.class);
		service = new DailyUsageUpdaterService(locationDao, userDao, userNodeDao, nodeDatumDao, client);
		service.setBillingDataFilterPlanKey("kb_datumMetricsDailyUsage");
		service.setBillingDataMostRecentUsageKey("kb_mrUsageDates");
		service.setBasePlanName(DEFAULT_BASE_PLAN_NAME);
		service.setUsageUnitName(DEFAULT_USAGE_UNIT_NAME);

		tagDefinitionCache = EasyMock.createMock(Cache.class);
		service.setTagDefinitionCache(tagDefinitionCache);
	}

	private void replayAll() {
		replay(locationDao, userDao, userNodeDao, nodeDatumDao, client, tagDefinitionCache);
	}

	private void verifyAll() {
		verify(locationDao, userDao, userNodeDao, nodeDatumDao, client, tagDefinitionCache);
	}

	@After
	public void teardown() {
		verifyAll();
	}

	private Account createTestAccount() {
		Account account = new Account();
		account.setAccountId(TEST_ACCOUNT_ID);
		account.setBillCycleDayLocal(DEFAULT_ACCOUNT_BILL_CYCLE_DAY);
		account.setCountry(TEST_LOCATION_COUNTRY);
		account.setCurrency(TEST_ACCOUNT_CURRENCY);
		account.setEmail(TEST_USER_EMAIL);
		account.setExternalKey(TEST_USER_ACCOUNT_KEY);
		account.setPaymentMethodId(TEST_PAYMENT_METHOD_ID);
		account.setTimeZone(TEST_NODE_TZ);
		return account;
	}

	private Account createTestAccount2() {
		Account account = new Account();
		account.setAccountId(TEST_ACCOUNT2_ID);
		account.setBillCycleDayLocal(DEFAULT_ACCOUNT_BILL_CYCLE_DAY);
		account.setCountry(TEST_LOCATION2_COUNTRY);
		account.setCurrency(TEST_ACCOUNT2_CURRENCY);
		account.setEmail(TEST_USER2_EMAIL);
		account.setExternalKey(TEST_USER2_ACCOUNT_KEY);
		account.setPaymentMethodId(TEST_PAYMENT_METHOD_ID);
		account.setTimeZone(TEST_NODE2_TZ);
		return account;
	}

	private Map<String, Object> userSearchBillingData() {
		Map<String, Object> killbillAccountFilter = new HashMap<>();
		killbillAccountFilter.put(BillingDataConstants.ACCOUNTING_DATA_PROP,
				KillbillBillingSystem.ACCOUNTING_SYSTEM_KEY);
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
				filterCapture.getValue().getInternalData());
	}

	@Test
	public void oneUserNoAccountNoData() {
		// search for users configured to use killbill; find one
		Map<String, Object> userBillingData = userSearchBillingData();
		User user = new User(TEST_USER_ID, TEST_USER_EMAIL);
		user.setInternalData(userBillingData);
		user.setLocationId(TEST_LOCATION_ID);
		Capture<UserFilterCommand> filterCapture = new Capture<>();
		List<UserFilterMatch> usersWithAccounting = new ArrayList<>();
		UserMatch userMatch = new UserMatch(TEST_USER_ID, TEST_USER_EMAIL);
		userMatch.setInternalData(userBillingData);
		userMatch.setLocationId(TEST_LOCATION_ID);
		userMatch.setName(TEST_USER_NAME);
		usersWithAccounting.add(userMatch);
		FilterResults<UserFilterMatch> userAccountingSearchResults = new BasicFilterResults<>(
				usersWithAccounting, 1L, 0, 1);
		expect(userDao.findFiltered(capture(filterCapture), isNull(), eq(0), eq(DEFAULT_BATCH_SIZE)))
				.andReturn(userAccountingSearchResults);

		// configure the account key based on user ID because it's not already configured
		userDao.storeInternalData(TEST_USER_ID, Collections
				.singletonMap(UserDataProperties.KILLBILL_ACCOUNT_KEY_DATA_PROP, TEST_USER_ACCOUNT_KEY));

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

		// assign tags to account; have to look up definitions first

		// each tag definition should be looked for in cache; when not found call to get all definitions
		expect(tagDefinitionCache.get("MANUAL_PAY")).andReturn(null);

		List<TagDefinition> tagDefinitions = Arrays.asList(
				new TagDefinition("123-456-789", "AUTO_PAY_OFF"),
				new TagDefinition("234-345-456", "MANUAL_PAY"),
				new TagDefinition("345-456-567", "PARTNER"));
		expect(client.getTagDefinitions()).andReturn(tagDefinitions);

		for ( TagDefinition tagDef : tagDefinitions ) {
			// cache every definition for later user
			expect(tagDefinitionCache.putIfAbsent(tagDef.getName(), tagDef)).andReturn(true);
		}

		client.addTagsToAccount(capture(accountCapture), eq(Collections.singleton("234-345-456")));

		// add external payment method
		expect(client.addPaymentMethodToAccount(capture(accountCapture),
				eq(EXTERNAL_PAYMENT_METHOD_DATA), eq(true))).andReturn(TEST_PAYMENT_METHOD_ID);

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
		assertEquals(userBillingData, filterCapture.getValue().getInternalData());

		// verify created account
		List<Account> accounts = accountCapture.getValues();
		assertNotNull("Created account", accounts);
		Account account = accounts.get(0);
		for ( int i = 0; i < accounts.size(); i++ ) {
			assertSame(accounts.get(0), account);
		}
		assertEquals("Account ID", TEST_ACCOUNT_ID, account.getAccountId());
		assertEquals("Bill cycle day", DEFAULT_ACCOUNT_BILL_CYCLE_DAY, account.getBillCycleDayLocal());
		assertEquals("Country", TEST_LOCATION_COUNTRY, account.getCountry());
		assertEquals("Currency", TEST_ACCOUNT_CURRENCY, account.getCurrency());
		assertEquals("Email", TEST_USER_EMAIL, account.getEmail());
		assertEquals("ExternalKey", TEST_USER_ACCOUNT_KEY, account.getExternalKey());
		assertEquals("Name", TEST_USER_NAME, account.getName());
		assertEquals("TimeZone", TEST_NODE_TZ, account.getTimeZone());
		assertEquals("Locale", "en_NZ", account.getLocale());
	}

	@Test
	public void oneUserNoAccountNoDataWithAddOn() {
		// configure add-on subscription support
		service.setAddOnPlanName(ADDON_PLAN_NAME);
		service.setUsageUnitName(QUERY_USAGE_UNIT_NAME);
		service.setBillingDataFilterPlanKey(KILLBILL_QUERY_ADDON_PLAN_DATA_PROP);
		service.setBillingDataMostRecentUsageKey(KILLBILL_QUERY_ADDIN_MOST_RECENT_USAGE_KEYS_DATA_PROP);
		service.setAuditPropertyKey(ADDON_AUDIT_PROP_NAME);

		// search for users configured to use killbill; find one
		Map<String, Object> userBillingData = userSearchBillingData();
		userBillingData.remove(KILLBILL_DAILY_USAGE_PLAN_DATA_PROP);
		userBillingData.put(KILLBILL_QUERY_ADDON_PLAN_DATA_PROP, true);
		User user = new User(TEST_USER_ID, TEST_USER_EMAIL);
		user.setInternalData(userBillingData);
		user.setLocationId(TEST_LOCATION_ID);
		Capture<UserFilterCommand> filterCapture = new Capture<>();
		List<UserFilterMatch> usersWithAccounting = new ArrayList<>();
		UserMatch userMatch = new UserMatch(TEST_USER_ID, TEST_USER_EMAIL);
		userMatch.setInternalData(userBillingData);
		userMatch.setLocationId(TEST_LOCATION_ID);
		userMatch.setName(TEST_USER_NAME);
		usersWithAccounting.add(userMatch);
		FilterResults<UserFilterMatch> userAccountingSearchResults = new BasicFilterResults<>(
				usersWithAccounting, 1L, 0, 1);
		expect(userDao.findFiltered(capture(filterCapture), isNull(), eq(0), eq(DEFAULT_BATCH_SIZE)))
				.andReturn(userAccountingSearchResults);

		// configure the account key based on user ID because it's not already configured
		userDao.storeInternalData(TEST_USER_ID, Collections
				.singletonMap(UserDataProperties.KILLBILL_ACCOUNT_KEY_DATA_PROP, TEST_USER_ACCOUNT_KEY));

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

		// assign tags to account; have to look up definitions first

		// each tag definition should be looked for in cache; when not found call to get all definitions
		expect(tagDefinitionCache.get("MANUAL_PAY")).andReturn(null);

		List<TagDefinition> tagDefinitions = Arrays.asList(
				new TagDefinition("123-456-789", "AUTO_PAY_OFF"),
				new TagDefinition("234-345-456", "MANUAL_PAY"),
				new TagDefinition("345-456-567", "PARTNER"));
		expect(client.getTagDefinitions()).andReturn(tagDefinitions);

		for ( TagDefinition tagDef : tagDefinitions ) {
			// cache every definition for later user
			expect(tagDefinitionCache.putIfAbsent(tagDef.getName(), tagDef)).andReturn(true);
		}

		client.addTagsToAccount(capture(accountCapture), eq(Collections.singleton("234-345-456")));

		// add external payment method
		expect(client.addPaymentMethodToAccount(capture(accountCapture),
				eq(EXTERNAL_PAYMENT_METHOD_DATA), eq(true))).andReturn(TEST_PAYMENT_METHOD_ID);

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
		assertEquals(userBillingData, filterCapture.getValue().getInternalData());

		// verify created account
		List<Account> accounts = accountCapture.getValues();
		assertNotNull("Created account", accounts);
		Account account = accounts.get(0);
		for ( int i = 0; i < accounts.size(); i++ ) {
			assertSame(accounts.get(0), account);
		}
		assertEquals("Account ID", TEST_ACCOUNT_ID, account.getAccountId());
		assertEquals("Bill cycle day", DEFAULT_ACCOUNT_BILL_CYCLE_DAY, account.getBillCycleDayLocal());
		assertEquals("Country", TEST_LOCATION_COUNTRY, account.getCountry());
		assertEquals("Currency", TEST_ACCOUNT_CURRENCY, account.getCurrency());
		assertEquals("Email", TEST_USER_EMAIL, account.getEmail());
		assertEquals("ExternalKey", TEST_USER_ACCOUNT_KEY, account.getExternalKey());
		assertEquals("Name", TEST_USER_NAME, account.getName());
		assertEquals("TimeZone", TEST_NODE_TZ, account.getTimeZone());
		assertEquals("Locale", "en_NZ", account.getLocale());
	}

	@Test
	public void oneUserNoAccountNoDataBelgium() {

		Map<String, String> currencyMap = new HashMap<>();
		currencyMap.put("BE", "EUR");
		service.setCountryCurrencyMap(currencyMap);

		// search for users configured to use killbill; find one
		Map<String, Object> userBillingData = userSearchBillingData();
		User user = new User(TEST_USER_ID, TEST_USER_EMAIL);
		user.setInternalData(userBillingData);
		user.setLocationId(TEST_LOCATION_ID);
		Capture<UserFilterCommand> filterCapture = new Capture<>();
		List<UserFilterMatch> usersWithAccounting = new ArrayList<>();
		UserMatch userMatch = new UserMatch(TEST_USER_ID, TEST_USER_EMAIL);
		userMatch.setInternalData(userBillingData);
		userMatch.setLocationId(TEST_LOCATION_ID);
		userMatch.setName(TEST_USER_NAME);
		usersWithAccounting.add(userMatch);
		FilterResults<UserFilterMatch> userAccountingSearchResults = new BasicFilterResults<>(
				usersWithAccounting, 1L, 0, 1);
		expect(userDao.findFiltered(capture(filterCapture), isNull(), eq(0), eq(DEFAULT_BATCH_SIZE)))
				.andReturn(userAccountingSearchResults);

		// configure the account key based on user ID because it's not already configured
		userDao.storeInternalData(TEST_USER_ID, Collections
				.singletonMap(UserDataProperties.KILLBILL_ACCOUNT_KEY_DATA_PROP, TEST_USER_ACCOUNT_KEY));

		// look for Killbill Account
		expect(client.accountForExternalKey(TEST_USER_ACCOUNT_KEY)).andReturn(null);

		// because Account not found, create one now, using the user's location for time zone
		SolarLocation location = new SolarLocation();
		location.setId(TEST_LOCATION_ID);
		location.setCountry("BE");
		location.setTimeZoneId("Europe/Brussels");
		expect(locationDao.get(TEST_LOCATION_ID)).andReturn(location);
		Capture<Account> accountCapture = new Capture<>(CaptureType.ALL);
		expect(client.createAccount(capture(accountCapture))).andReturn(TEST_ACCOUNT_ID);

		// assign tags to account; have to look up definitions first

		// each tag definition should be looked for in cache; when not found call to get all definitions
		expect(tagDefinitionCache.get("MANUAL_PAY")).andReturn(null);

		List<TagDefinition> tagDefinitions = Arrays.asList(
				new TagDefinition("123-456-789", "AUTO_PAY_OFF"),
				new TagDefinition("234-345-456", "MANUAL_PAY"),
				new TagDefinition("345-456-567", "PARTNER"));
		expect(client.getTagDefinitions()).andReturn(tagDefinitions);

		for ( TagDefinition tagDef : tagDefinitions ) {
			// cache every definition for later user
			expect(tagDefinitionCache.putIfAbsent(tagDef.getName(), tagDef)).andReturn(true);
		}

		client.addTagsToAccount(capture(accountCapture), eq(Collections.singleton("234-345-456")));

		// add external payment method
		expect(client.addPaymentMethodToAccount(capture(accountCapture),
				eq(EXTERNAL_PAYMENT_METHOD_DATA), eq(true))).andReturn(TEST_PAYMENT_METHOD_ID);

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
		assertEquals(userBillingData, filterCapture.getValue().getInternalData());

		// verify created account
		List<Account> accounts = accountCapture.getValues();
		assertNotNull("Created account", accounts);
		Account account = accounts.get(0);
		for ( int i = 0; i < accounts.size(); i++ ) {
			assertSame(accounts.get(0), account);
		}
		assertEquals("Account ID", TEST_ACCOUNT_ID, account.getAccountId());
		assertEquals("Bill cycle day", DEFAULT_ACCOUNT_BILL_CYCLE_DAY, account.getBillCycleDayLocal());
		assertEquals("Country", "BE", account.getCountry());
		assertEquals("Currency", "EUR", account.getCurrency());
		assertEquals("Email", TEST_USER_EMAIL, account.getEmail());
		assertEquals("ExternalKey", TEST_USER_ACCOUNT_KEY, account.getExternalKey());
		assertEquals("Name", TEST_USER_NAME, account.getName());
		assertEquals("TimeZone", "Europe/Brussels", account.getTimeZone());
		assertEquals("Locale", "fr_BE", account.getLocale());
	}

	@Test
	public void oneUserNoData() {
		// search for users configured to use killbill; find one
		Map<String, Object> killbillAccountFilter = userSearchBillingData();

		Map<String, Object> userBillingData = new HashMap<>(killbillAccountFilter);
		userBillingData.put(UserDataProperties.KILLBILL_ACCOUNT_KEY_DATA_PROP, TEST_USER_ACCOUNT_KEY);
		User user = new User(TEST_USER_ID, TEST_USER_EMAIL);
		user.setInternalData(userBillingData);
		user.setLocationId(TEST_LOCATION_ID);
		Capture<UserFilterCommand> filterCapture = new Capture<>();
		List<UserFilterMatch> usersWithAccounting = new ArrayList<>();
		UserMatch userMatch = new UserMatch(TEST_USER_ID, TEST_USER_EMAIL);
		userMatch.setInternalData(userBillingData);
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
		assertEquals(killbillAccountFilter, filterCapture.getValue().getInternalData());
	}

	@Test
	public void oneUserCatchupData() {
		// search for users configured to use killbill; find one
		Map<String, Object> killbillAccountFilter = userSearchBillingData();

		Map<String, Object> userBillingData = new HashMap<>(killbillAccountFilter);
		userBillingData.put(UserDataProperties.KILLBILL_ACCOUNT_KEY_DATA_PROP, TEST_USER_ACCOUNT_KEY);
		User user = new User(TEST_USER_ID, TEST_USER_EMAIL);
		user.setInternalData(userBillingData);
		user.setLocationId(TEST_LOCATION_ID);
		Capture<UserFilterCommand> filterCapture = new Capture<>();
		List<UserFilterMatch> usersWithAccounting = new ArrayList<>();
		UserMatch userMatch = new UserMatch(TEST_USER_ID, TEST_USER_EMAIL);
		userMatch.setInternalData(userBillingData);
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
		DateTime auditDataStart = new DateTime(2017, 1, 1, 0, 0, DateTimeZone.forID(TEST_NODE_TZ));
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
			filter.setDataPath(DEFAULT_AUDIT_PROP_NAME);
			expect(nodeDatumDao.getAuditCountTotal(filter))
					.andReturn(countList.get(itr.previousIndex()));
		}

		// get the Bundle for this node, but it doesn't exist yet so it will be created
		expect(client.bundleForExternalKey(account, TEST_BUNDLE_KEY)).andReturn(null);

		// to decide the requestedDate, find the earliest date with node data and use that
		DateTime nodeDataStart = new DateTime(2015, 1, 1, 0, 0, DateTimeZone.forID(TEST_NODE_TZ));
		DateTime nodeDataEnd = auditDataEnd.plusMinutes(15);
		ReadableInterval nodeDataInterval = new Interval(nodeDataStart, nodeDataEnd);
		expect(nodeDatumDao.getReportableInterval(TEST_NODE_ID, null)).andReturn(nodeDataInterval);

		Capture<Bundle> bundleCapture = new Capture<>();
		expect(client.createBundle(eq(account), eq(nodeDataStart.toLocalDate()), capture(bundleCapture)))
				.andReturn(TEST_BUNDLE_ID);

		// immediately after creation, get the Bundle so we have the subscription ID
		Bundle bundle = new Bundle();
		bundle.setAccountId(TEST_ACCOUNT_ID);
		bundle.setBundleId(TEST_BUNDLE_ID);
		bundle.setExternalKey(TEST_BUNDLE_KEY);
		Subscription subscription = new Subscription(TEST_SUBSCRIPTION_ID);
		subscription.setPlanName(DEFAULT_BASE_PLAN_NAME);
		bundle.setSubscriptions(Collections.singletonList(subscription));
		expect(client.bundleForExternalKey(account, TEST_BUNDLE_KEY)).andReturn(bundle);

		// then add the custom field for the node ID
		expect(client.createSubscriptionCustomFields(TEST_SUBSCRIPTION_ID,
				singletonList(new CustomField(CUSTOM_FIELD_NODE_ID, TEST_NODE_ID.toString()))))
						.andReturn("test-field-list-id");

		Capture<List<UsageRecord>> usageCapture = new Capture<>();
		client.addUsage(same(subscription), eq(ISO_DATE_FORMATTER.print(auditDataEnd.toLocalDate())),
				eq(DEFAULT_USAGE_UNIT_NAME), capture(usageCapture));

		// finally, store the "most recent usage" date for future processing
		userDao.storeInternalData(TEST_USER_ID,
				Collections.singletonMap(KILLBILL_MOST_RECENT_USAGE_KEYS_DATA_PROP,
						Collections.singletonMap(TEST_NODE_ID.toString(),
								ISO_DATE_FORMATTER.print(auditDataEnd.toLocalDate()))));

		replayAll();

		service.execute();

		// verify search for users
		assertNotNull(filterCapture.getValue());
		assertEquals(killbillAccountFilter, filterCapture.getValue().getInternalData());

		// verify bundle
		Bundle capturedBundle = bundleCapture.getValue();
		assertNotNull(capturedBundle);
		assertEquals("Account ID", TEST_ACCOUNT_ID, capturedBundle.getAccountId());
		Assert.assertNull("Bundle ID", capturedBundle.getBundleId());
		assertEquals("External key", TEST_BUNDLE_KEY, capturedBundle.getExternalKey());
		assertNotNull("Subscriptions", capturedBundle.getSubscriptions());
		assertEquals("Subscription count", 1, capturedBundle.getSubscriptions().size());
		Subscription capturedSubscription = capturedBundle.getSubscriptions().get(0);
		assertEquals("Subscription bill cycle day", DEFAULT_SUBSCRIPTION_BILL_CYCLE_DAY,
				capturedSubscription.getBillCycleDayLocal());
		assertEquals("Plan name", DEFAULT_BASE_PLAN_NAME, capturedSubscription.getPlanName());
		assertEquals("Plan name", Subscription.BASE_PRODUCT_CATEGORY,
				capturedSubscription.getProductCategory());

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
	public void oneUserCatchupDataAddOn() {
		// configure add-on subscription support
		service.setAddOnPlanName(ADDON_PLAN_NAME);
		service.setUsageUnitName(QUERY_USAGE_UNIT_NAME);
		service.setBillingDataFilterPlanKey(KILLBILL_QUERY_ADDON_PLAN_DATA_PROP);
		service.setBillingDataMostRecentUsageKey(KILLBILL_QUERY_ADDIN_MOST_RECENT_USAGE_KEYS_DATA_PROP);
		service.setAuditPropertyKey(ADDON_AUDIT_PROP_NAME);

		// search for users configured to use killbill; find one
		Map<String, Object> killbillAccountFilter = userSearchBillingData();
		killbillAccountFilter.remove(KILLBILL_DAILY_USAGE_PLAN_DATA_PROP);
		killbillAccountFilter.put(KILLBILL_QUERY_ADDON_PLAN_DATA_PROP, true);

		Map<String, Object> userBillingData = new HashMap<>(killbillAccountFilter);
		userBillingData.put(UserDataProperties.KILLBILL_ACCOUNT_KEY_DATA_PROP, TEST_USER_ACCOUNT_KEY);
		User user = new User(TEST_USER_ID, TEST_USER_EMAIL);
		user.setInternalData(userBillingData);
		user.setLocationId(TEST_LOCATION_ID);
		Capture<UserFilterCommand> filterCapture = new Capture<>();
		List<UserFilterMatch> usersWithAccounting = new ArrayList<>();
		UserMatch userMatch = new UserMatch(TEST_USER_ID, TEST_USER_EMAIL);
		userMatch.setInternalData(userBillingData);
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
		DateTime auditDataStart = new DateTime(2017, 1, 1, 0, 0, DateTimeZone.forID(TEST_NODE_TZ));
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
			filter.setDataPath(ADDON_AUDIT_PROP_NAME);
			expect(nodeDatumDao.getAuditCountTotal(filter))
					.andReturn(countList.get(itr.previousIndex()));
		}

		// get the Bundle for this node, but it doesn't exist yet so it will be created
		expect(client.bundleForExternalKey(account, TEST_BUNDLE_KEY)).andReturn(null);

		// to decide the requestedDate, find the earliest date with node data and use that
		DateTime nodeDataStart = new DateTime(2015, 1, 1, 0, 0, DateTimeZone.forID(TEST_NODE_TZ));
		DateTime nodeDataEnd = auditDataEnd.plusMinutes(15);
		ReadableInterval nodeDataInterval = new Interval(nodeDataStart, nodeDataEnd);
		expect(nodeDatumDao.getReportableInterval(TEST_NODE_ID, null)).andReturn(nodeDataInterval);

		Capture<Bundle> bundleCapture = new Capture<>();
		expect(client.createBundle(eq(account), eq(nodeDataStart.toLocalDate()), capture(bundleCapture)))
				.andReturn(TEST_BUNDLE_ID);

		// immediately after creation, get the Bundle so we have the subscription ID
		Bundle bundle = new Bundle();
		bundle.setAccountId(TEST_ACCOUNT_ID);
		bundle.setBundleId(TEST_BUNDLE_ID);
		bundle.setExternalKey(TEST_BUNDLE_KEY);

		Subscription subscription = new Subscription(TEST_SUBSCRIPTION_ID);
		subscription.setPlanName(DEFAULT_BASE_PLAN_NAME);

		final String addOnSubId = UUID.randomUUID().toString();
		Subscription addOnSubscription = new Subscription(addOnSubId);
		addOnSubscription.setPlanName(ADDON_PLAN_NAME);

		bundle.setSubscriptions(Arrays.asList(subscription, addOnSubscription));
		expect(client.bundleForExternalKey(account, TEST_BUNDLE_KEY)).andReturn(bundle);

		// then add the custom field for the node ID to base subscription
		expect(client.createSubscriptionCustomFields(TEST_SUBSCRIPTION_ID,
				singletonList(new CustomField(CUSTOM_FIELD_NODE_ID, TEST_NODE_ID.toString()))))
						.andReturn("test-field-list-id");

		// then add the custom field for the node ID to add on subscription
		expect(client.createSubscriptionCustomFields(addOnSubId,
				singletonList(new CustomField(CUSTOM_FIELD_NODE_ID, TEST_NODE_ID.toString()))))
						.andReturn("test-field-list-id");

		Capture<List<UsageRecord>> usageCapture = new Capture<>();
		client.addUsage(same(addOnSubscription),
				eq(ISO_DATE_FORMATTER.print(auditDataEnd.toLocalDate())), eq(QUERY_USAGE_UNIT_NAME),
				capture(usageCapture));

		// finally, store the "most recent usage" date for future processing
		userDao.storeInternalData(TEST_USER_ID,
				Collections.singletonMap(KILLBILL_QUERY_ADDIN_MOST_RECENT_USAGE_KEYS_DATA_PROP,
						Collections.singletonMap(TEST_NODE_ID.toString(),
								ISO_DATE_FORMATTER.print(auditDataEnd.toLocalDate()))));

		replayAll();

		service.execute();

		// verify search for users
		assertNotNull(filterCapture.getValue());
		assertEquals(killbillAccountFilter, filterCapture.getValue().getInternalData());

		// verify bundle
		Bundle capturedBundle = bundleCapture.getValue();
		assertNotNull(capturedBundle);
		assertEquals("Account ID", TEST_ACCOUNT_ID, capturedBundle.getAccountId());
		Assert.assertNull("Bundle ID", capturedBundle.getBundleId());
		assertEquals("External key", TEST_BUNDLE_KEY, capturedBundle.getExternalKey());
		assertNotNull("Subscriptions", capturedBundle.getSubscriptions());
		assertEquals("Subscription count", 2, capturedBundle.getSubscriptions().size());

		Subscription capturedSubscription = capturedBundle.getSubscriptions().get(0);
		assertEquals("Subscription bill cycle day", DEFAULT_SUBSCRIPTION_BILL_CYCLE_DAY,
				capturedSubscription.getBillCycleDayLocal());
		assertEquals("Plan name", DEFAULT_BASE_PLAN_NAME, capturedSubscription.getPlanName());
		assertEquals("Plan name", Subscription.BASE_PRODUCT_CATEGORY,
				capturedSubscription.getProductCategory());

		capturedSubscription = capturedBundle.getSubscriptions().get(1);
		assertEquals("Subscription bill cycle day", DEFAULT_SUBSCRIPTION_BILL_CYCLE_DAY,
				capturedSubscription.getBillCycleDayLocal());
		assertEquals("Plan name", ADDON_PLAN_NAME, capturedSubscription.getPlanName());
		assertEquals("Plan name", Subscription.ADDON_PRODUCT_CATEGORY,
				capturedSubscription.getProductCategory());

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
	public void oneUserCatchupDataAddOnCreated() {
		// configure add-on subscription support
		service.setAddOnPlanName(ADDON_PLAN_NAME);
		service.setUsageUnitName(QUERY_USAGE_UNIT_NAME);
		service.setBillingDataFilterPlanKey(KILLBILL_QUERY_ADDON_PLAN_DATA_PROP);
		service.setBillingDataMostRecentUsageKey(KILLBILL_QUERY_ADDIN_MOST_RECENT_USAGE_KEYS_DATA_PROP);
		service.setAuditPropertyKey(ADDON_AUDIT_PROP_NAME);

		// search for users configured to use killbill; find one
		Map<String, Object> killbillAccountFilter = userSearchBillingData();
		killbillAccountFilter.remove(KILLBILL_DAILY_USAGE_PLAN_DATA_PROP);
		killbillAccountFilter.put(KILLBILL_QUERY_ADDON_PLAN_DATA_PROP, true);

		Map<String, Object> userBillingData = new HashMap<>(killbillAccountFilter);
		userBillingData.put(UserDataProperties.KILLBILL_ACCOUNT_KEY_DATA_PROP, TEST_USER_ACCOUNT_KEY);
		User user = new User(TEST_USER_ID, TEST_USER_EMAIL);
		user.setInternalData(userBillingData);
		user.setLocationId(TEST_LOCATION_ID);
		Capture<UserFilterCommand> filterCapture = new Capture<>();
		List<UserFilterMatch> usersWithAccounting = new ArrayList<>();
		UserMatch userMatch = new UserMatch(TEST_USER_ID, TEST_USER_EMAIL);
		userMatch.setInternalData(userBillingData);
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
		DateTime auditDataStart = new DateTime(2017, 1, 1, 0, 0, DateTimeZone.forID(TEST_NODE_TZ));
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
			filter.setDataPath(ADDON_AUDIT_PROP_NAME);
			expect(nodeDatumDao.getAuditCountTotal(filter))
					.andReturn(countList.get(itr.previousIndex()));
		}

		// get the Bundle for this node, which exists but does not include the add on
		Bundle bundle = new Bundle();
		bundle.setAccountId(TEST_ACCOUNT_ID);
		bundle.setBundleId(TEST_BUNDLE_ID);
		bundle.setExternalKey(TEST_BUNDLE_KEY);

		Subscription subscription = new Subscription(TEST_SUBSCRIPTION_ID);
		subscription.setPlanName(DEFAULT_BASE_PLAN_NAME);

		bundle.setSubscriptions(Collections.singletonList(subscription));

		expect(client.bundleForExternalKey(account, TEST_BUNDLE_KEY)).andReturn(bundle);

		final String addOnSubId = UUID.randomUUID().toString();

		// create entitlement for addon subscription now
		Capture<Subscription> subscriptionCaptor = new Capture<>();
		expect(client.addSubscriptionToBundle(eq(account), eq(TEST_BUNDLE_ID),
				anyObject(LocalDate.class), capture(subscriptionCaptor))).andReturn(addOnSubId);

		// then add the custom field for the node ID to add on subscription
		expect(client.createSubscriptionCustomFields(addOnSubId,
				singletonList(new CustomField(CUSTOM_FIELD_NODE_ID, TEST_NODE_ID.toString()))))
						.andReturn("test-field-list-id");

		Capture<List<UsageRecord>> usageCapture = new Capture<>();
		Capture<Subscription> usageSubscriptionCaptor = new Capture<>();
		client.addUsage(capture(usageSubscriptionCaptor),
				eq(ISO_DATE_FORMATTER.print(auditDataEnd.toLocalDate())), eq(QUERY_USAGE_UNIT_NAME),
				capture(usageCapture));

		// finally, store the "most recent usage" date for future processing
		userDao.storeInternalData(TEST_USER_ID,
				Collections.singletonMap(KILLBILL_QUERY_ADDIN_MOST_RECENT_USAGE_KEYS_DATA_PROP,
						Collections.singletonMap(TEST_NODE_ID.toString(),
								ISO_DATE_FORMATTER.print(auditDataEnd.toLocalDate()))));

		replayAll();

		service.execute();

		// verify search for users
		assertNotNull(filterCapture.getValue());
		assertEquals(killbillAccountFilter, filterCapture.getValue().getInternalData());

		// verify created subscription
		Subscription addOnSub = subscriptionCaptor.getValue();
		assertThat("Created sub plan", addOnSub.getPlanName(), equalTo(ADDON_PLAN_NAME));
		assertThat("Created sub product cat", addOnSub.getProductCategory(),
				equalTo(Subscription.ADDON_PRODUCT_CATEGORY));

		// verify usage sub
		assertThat("Usage sub is created sub", usageSubscriptionCaptor.getValue(),
				sameInstance(addOnSub));

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
	public void oneUserCatchupDataToday() {
		// search for users configured to use killbill; find one
		Map<String, Object> killbillAccountFilter = userSearchBillingData();

		Map<String, Object> userBillingData = new HashMap<>(killbillAccountFilter);
		userBillingData.put(UserDataProperties.KILLBILL_ACCOUNT_KEY_DATA_PROP, TEST_USER_ACCOUNT_KEY);
		User user = new User(TEST_USER_ID, TEST_USER_EMAIL);
		user.setInternalData(userBillingData);
		user.setLocationId(TEST_LOCATION_ID);
		Capture<UserFilterCommand> filterCapture = new Capture<>();
		List<UserFilterMatch> usersWithAccounting = new ArrayList<>();
		UserMatch userMatch = new UserMatch(TEST_USER_ID, TEST_USER_EMAIL);
		userMatch.setInternalData(userBillingData);
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

		// we have audit data starting 2 days ago at start of day, through now
		DateTime auditDataStart = new DateTime(DateTimeZone.forID(TEST_NODE_TZ)).dayOfMonth()
				.roundFloorCopy().minusDays(2);
		DateTime auditDataEnd = new DateTime(DateTimeZone.forID(TEST_NODE_TZ)).hourOfDay()
				.roundCeilingCopy();
		ReadableInterval auditInterval = new Interval(auditDataStart, auditDataEnd);
		expect(nodeDatumDao.getAuditInterval(TEST_NODE_ID, null)).andReturn(auditInterval);

		List<DateTime> dayList = new ArrayList<>(3);
		List<Long> countList = new ArrayList<>(3);
		for ( int i = 0; i < 3; i++ ) {
			dayList.add(auditDataStart.plusDays(i));
			countList.add((long) (Math.random() * 100000));
		}

		// now iterate over each DAY in audit interval EXCEPT today, gathering usage values
		for ( int i = 0; i < 2; i++ ) {
			DateTime day = dayList.get(i);
			DatumFilterCommand filter = new DatumFilterCommand();
			filter.setNodeId(TEST_NODE_ID);
			filter.setStartDate(day);
			filter.setEndDate(day.plusDays(1));
			filter.setDataPath(DEFAULT_AUDIT_PROP_NAME);
			expect(nodeDatumDao.getAuditCountTotal(filter)).andReturn(countList.get(i));
		}

		// get the Bundle for this node
		Bundle bundle = new Bundle();
		bundle.setBundleId(TEST_BUNDLE_ID);
		bundle.setAccountId(TEST_ACCOUNT_ID);
		bundle.setExternalKey(TEST_BUNDLE_KEY);
		bundle.setSubscriptions(
				Collections.singletonList(Subscription.withPlanName(DEFAULT_BASE_PLAN_NAME)));
		expect(client.bundleForExternalKey(account, TEST_BUNDLE_KEY)).andReturn(bundle);

		Capture<List<UsageRecord>> usageCapture = new Capture<>();
		client.addUsage(same(bundle.getSubscriptions().get(0)),
				eq(ISO_DATE_FORMATTER.print(auditDataEnd.toLocalDate())), eq(DEFAULT_USAGE_UNIT_NAME),
				capture(usageCapture));

		// finally, store the "most recent usage" date for future processing
		userDao.storeInternalData(TEST_USER_ID,
				Collections.singletonMap(KILLBILL_MOST_RECENT_USAGE_KEYS_DATA_PROP,
						Collections.singletonMap(TEST_NODE_ID.toString(),
								ISO_DATE_FORMATTER.print(auditDataEnd.toLocalDate()))));

		replayAll();

		service.execute();

		// verify search for users
		assertNotNull(filterCapture.getValue());
		assertEquals(killbillAccountFilter, filterCapture.getValue().getInternalData());

		// verify usage
		List<UsageRecord> usage = usageCapture.getValue();
		assertNotNull("Usage", usage);
		assertEquals("Usage count", 2, usage.size());
		for ( int i = 0; i < 2; i++ ) {
			BigDecimal expectedCount = new BigDecimal(countList.get(i));
			UsageRecord rec = usage.get(i);
			assertEquals("Usage amount " + i, expectedCount, rec.getAmount());
			assertEquals("Usage date " + i, dayList.get(i).toLocalDate(), rec.getRecordDate());
		}
	}

	@Test
	public void oneUserAddMoreDataToday() {
		// search for users configured to use killbill; find one
		Map<String, Object> killbillAccountFilter = userSearchBillingData();

		Map<String, Object> userBillingData = new HashMap<>(killbillAccountFilter);
		userBillingData.put(UserDataProperties.KILLBILL_ACCOUNT_KEY_DATA_PROP, TEST_USER_ACCOUNT_KEY);

		// we have usage data published up to start of TODAY
		DateTime auditDataStart = new DateTime(DateTimeZone.forID(TEST_NODE_TZ)).dayOfMonth()
				.roundFloorCopy();
		userBillingData.put(KILLBILL_MOST_RECENT_USAGE_KEYS_DATA_PROP, Collections.singletonMap(
				TEST_NODE_ID.toString(), ISO_DATE_FORMATTER.print(auditDataStart.toLocalDate())));

		User user = new User(TEST_USER_ID, TEST_USER_EMAIL);
		user.setInternalData(userBillingData);
		user.setLocationId(TEST_LOCATION_ID);
		Capture<UserFilterCommand> filterCapture = new Capture<>();
		List<UserFilterMatch> usersWithAccounting = new ArrayList<>();
		UserMatch userMatch = new UserMatch(TEST_USER_ID, TEST_USER_EMAIL);
		userMatch.setInternalData(userBillingData);
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

		// we have partial audit data available for today
		DateTime auditDataEnd = new DateTime(DateTimeZone.forID(TEST_NODE_TZ)).hourOfDay()
				.roundCeilingCopy();
		ReadableInterval auditInterval = new Interval(auditDataStart, auditDataEnd);
		expect(nodeDatumDao.getAuditInterval(TEST_NODE_ID, null)).andReturn(auditInterval);

		// since we are still on TODAY, there can be NO usage to add, so just skip

		replayAll();

		service.execute();

		// verify search for users
		assertNotNull(filterCapture.getValue());
		assertEquals(killbillAccountFilter, filterCapture.getValue().getInternalData());
	}

	@Test
	public void oneUserAddMoreDataMultipleNodes() {
		// search for users configured to use killbill; find one
		Map<String, Object> killbillAccountFilter = userSearchBillingData();

		Map<String, Object> userBillingData = new HashMap<>(killbillAccountFilter);
		userBillingData.put(UserDataProperties.KILLBILL_ACCOUNT_KEY_DATA_PROP, TEST_USER_ACCOUNT_KEY);
		Map<String, String> userNodeMostRecentKeys = new HashMap<>();
		userBillingData.put(KILLBILL_MOST_RECENT_USAGE_KEYS_DATA_PROP, userNodeMostRecentKeys);

		// we have usage data published up to 2017-01-01, for TWO nodes
		userNodeMostRecentKeys.put(TEST_NODE_ID.toString(),
				ISO_DATE_FORMATTER.print(new LocalDate(2017, 1, 1)));
		userNodeMostRecentKeys.put(TEST_NODE2_ID.toString(),
				ISO_DATE_FORMATTER.print(new LocalDate(2017, 1, 1)));

		User user = new User(TEST_USER_ID, TEST_USER_EMAIL);
		user.setInternalData(userBillingData);
		user.setLocationId(TEST_LOCATION_ID);
		Capture<UserFilterCommand> filterCapture = new Capture<>();
		List<UserFilterMatch> usersWithAccounting = new ArrayList<>();
		UserMatch userMatch = new UserMatch(TEST_USER_ID, TEST_USER_EMAIL);
		userMatch.setInternalData(userBillingData);
		userMatch.setLocationId(TEST_LOCATION_ID);
		userMatch.setName(TEST_USER_NAME);
		usersWithAccounting.add(userMatch);
		FilterResults<UserFilterMatch> userAccountingSearchResults = new BasicFilterResults<>(
				usersWithAccounting, 1L, 0, 1);
		expect(userDao.findFiltered(capture(filterCapture), isNull(), eq(0), eq(DEFAULT_BATCH_SIZE)))
				.andReturn(userAccountingSearchResults);

		// get Killbill Account
		Account account = createTestAccount();

		// now iterate over all user's nodes to look for usage
		SolarNode node = new SolarNode(TEST_NODE_ID, TEST_LOCATION_ID);
		SolarNode node2 = new SolarNode(TEST_NODE2_ID, TEST_LOCATION2_ID);

		Bundle bundle = new Bundle();
		bundle.setBundleId(TEST_BUNDLE_ID);
		bundle.setAccountId(TEST_ACCOUNT_ID);
		bundle.setExternalKey(TEST_BUNDLE_KEY);
		bundle.setSubscriptions(
				Collections.singletonList(Subscription.withPlanName(DEFAULT_BASE_PLAN_NAME)));

		Bundle bundle2 = new Bundle();
		bundle2.setBundleId(TEST_BUNDLE2_ID);
		bundle2.setAccountId(TEST_ACCOUNT_ID);
		bundle2.setExternalKey(TEST_BUNDLE2_KEY);
		bundle2.setSubscriptions(
				Collections.singletonList(Subscription.withPlanName(DEFAULT_BASE_PLAN_NAME)));

		Capture<List<UsageRecord>> usageCapture = new Capture<>(CaptureType.ALL);
		List<List<DateTime>> dayLists = new ArrayList<>();
		List<List<Long>> countLists = new ArrayList<>();

		// update the "most recent usage" date to the last day, to start processing there
		processUser(user, userMatch, Arrays.asList(node, node2), userNodeMostRecentKeys, account,
				Arrays.asList(bundle, bundle2), 1, usageCapture, dayLists, countLists);

		// reset the "most recent usage" date to the last day for start of replay, to start processing there
		userNodeMostRecentKeys.put(TEST_NODE_ID.toString(),
				ISO_DATE_FORMATTER.print(dayLists.get(0).get(0).toLocalDate()));
		userNodeMostRecentKeys.put(TEST_NODE2_ID.toString(),
				ISO_DATE_FORMATTER.print(dayLists.get(0).get(0).toLocalDate()));

		replayAll();

		service.execute();

		// verify search for users
		assertNotNull(filterCapture.getValue());
		assertEquals(killbillAccountFilter, filterCapture.getValue().getInternalData());

		// verify usage
		List<List<UsageRecord>> usages = usageCapture.getValues();
		assertThat(usages, Matchers.hasSize(2));
		int idx = 0;
		for ( List<UsageRecord> usage : usages ) {
			assertEquals("Usage count", 1, usage.size());
			List<Long> countList = countLists.get(idx);
			List<DateTime> dayList = dayLists.get(idx);
			for ( ListIterator<Long> itr = countList.listIterator(); itr.hasNext(); ) {
				BigDecimal expectedCount = new BigDecimal(itr.next());
				UsageRecord rec = usage.get(itr.previousIndex());
				assertEquals("Usage amount " + itr.previousIndex(), expectedCount, rec.getAmount());
				assertEquals("Usage date " + itr.previousIndex(),
						dayList.get(itr.previousIndex()).toLocalDate(), rec.getRecordDate());
			}
			idx++;
		}
	}

	@Test
	public void oneUserAddOneMoreDayDataMultipleUsersDifferentTimeZones() {
		// search for users configured to use killbill; find one
		Map<String, Object> killbillAccountFilter = userSearchBillingData();

		Map<String, Object> userBillingData = new HashMap<>(killbillAccountFilter);
		userBillingData.put(UserDataProperties.KILLBILL_ACCOUNT_KEY_DATA_PROP, TEST_USER_ACCOUNT_KEY);

		User user = new User(TEST_USER_ID, TEST_USER_EMAIL);
		user.setLocationId(TEST_LOCATION_ID);
		user.setInternalData(userBillingData);

		Map<String, Object> user2BillingData = new HashMap<>(killbillAccountFilter);
		user2BillingData.put(UserDataProperties.KILLBILL_ACCOUNT_KEY_DATA_PROP, TEST_USER2_ACCOUNT_KEY);

		User user2 = new User(TEST_USER2_ID, TEST_USER2_EMAIL);
		user2.setLocationId(TEST_LOCATION2_ID);
		user2.setInternalData(user2BillingData);

		Capture<UserFilterCommand> filterCapture = new Capture<>();
		List<UserFilterMatch> usersWithAccounting = new ArrayList<>();

		UserMatch userMatch = new UserMatch(TEST_USER_ID, TEST_USER_EMAIL);
		userMatch.setInternalData(userBillingData);
		userMatch.setLocationId(TEST_LOCATION_ID);
		userMatch.setName(TEST_USER_NAME);
		usersWithAccounting.add(userMatch);

		UserMatch user2Match = new UserMatch(TEST_USER2_ID, TEST_USER2_EMAIL);
		user2Match.setInternalData(user2BillingData);
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
		bundle.setSubscriptions(
				Collections.singletonList(Subscription.withPlanName(DEFAULT_BASE_PLAN_NAME)));
		Capture<List<UsageRecord>> usageCapture = new Capture<>();
		List<List<DateTime>> dayLists = new ArrayList<>();
		List<List<Long>> countLists = new ArrayList<>();

		// update the "most recent usage" date to the last day, to start processing there
		Map<String, String> userNodeMostRecentKeys = new HashMap<>();
		processUser(user, userMatch, Collections.singletonList(node), userNodeMostRecentKeys, account,
				Collections.singletonList(bundle), 1, usageCapture, dayLists, countLists);
		userNodeMostRecentKeys.put(TEST_NODE_ID.toString(),
				ISO_DATE_FORMATTER.print(dayLists.get(0).get(0).toLocalDate()));
		userBillingData.put(KILLBILL_MOST_RECENT_USAGE_KEYS_DATA_PROP, userNodeMostRecentKeys);

		SolarNode node2 = new SolarNode(TEST_NODE2_ID, TEST_LOCATION2_ID);
		Account account2 = createTestAccount2();
		Bundle bundle2 = new Bundle();
		bundle2.setBundleId(TEST_BUNDLE2_ID);
		bundle2.setAccountId(TEST_ACCOUNT2_ID);
		bundle2.setExternalKey(TEST_BUNDLE2_KEY);
		bundle2.setSubscriptions(
				Collections.singletonList(Subscription.withPlanName(DEFAULT_BASE_PLAN_NAME)));
		Capture<List<UsageRecord>> usage2Capture = new Capture<>();
		List<List<DateTime>> day2Lists = new ArrayList<>();
		List<List<Long>> count2Lists = new ArrayList<>();

		Map<String, String> user2NodeMostRecentKeys = new HashMap<>();
		processUser(user2, user2Match, Collections.singletonList(node2), user2NodeMostRecentKeys,
				account2, Collections.singletonList(bundle2), 1, usage2Capture, day2Lists, count2Lists);
		// update the "most recent usage" date to the last day, to start processing there
		user2NodeMostRecentKeys.put(TEST_NODE2_ID.toString(),
				ISO_DATE_FORMATTER.print(day2Lists.get(0).get(0).toLocalDate()));
		user2BillingData.put(KILLBILL_MOST_RECENT_USAGE_KEYS_DATA_PROP, user2NodeMostRecentKeys);

		replayAll();

		service.execute();

		// verify search for users
		assertNotNull(filterCapture.getValue());
		assertEquals(killbillAccountFilter, filterCapture.getValue().getInternalData());

		// verify usage
		List<UsageRecord> usage = usageCapture.getValue();
		assertNotNull("Usage", usage);
		assertEquals("Usage count", 1, usage.size());
		List<Long> countList = countLists.get(0);
		List<DateTime> dayList = dayLists.get(0);
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
		List<Long> count2List = count2Lists.get(0);
		List<DateTime> day2List = day2Lists.get(0);
		for ( ListIterator<Long> itr = count2List.listIterator(); itr.hasNext(); ) {
			BigDecimal expectedCount = new BigDecimal(itr.next());
			UsageRecord rec = usage.get(itr.previousIndex());
			assertEquals("Usage amount " + itr.previousIndex(), expectedCount, rec.getAmount());
			assertEquals("Usage date " + itr.previousIndex(),
					day2List.get(itr.previousIndex()).toLocalDate(), rec.getRecordDate());
		}
	}

	private void processUser(User user, UserMatch userMatch, List<SolarNode> nodes,
			Map<String, String> userNodeMostRecentKeys, Account account, List<Bundle> bundles,
			int numDays, Capture<List<UsageRecord>> usageCapture, List<List<DateTime>> dayLists,
			List<List<Long>> countLists) {
		// get Killbill Account #1
		expect(client.accountForExternalKey(account.getExternalKey())).andReturn(account);

		// now iterate over all user's nodes to look for usage
		List<UserNode> allUserNodes = new ArrayList<>(nodes.size());
		for ( SolarNode node : nodes ) {
			UserNode userNode = new UserNode(user, node);
			allUserNodes.add(userNode);
		}
		expect(userNodeDao.findUserNodesForUser(userMatch)).andReturn(allUserNodes);

		DateTimeZone timeZone = DateTimeZone.forID(account.getTimeZone());

		// FOR EACH UserNode here
		int idx = 0;
		for ( UserNode userNode : allUserNodes ) {
			SolarNode node = userNode.getNode();
			Bundle bundle = bundles.get(idx);
			DateTime auditDataStart = new DateTime(2017, 1, 1, 0, 0, timeZone);
			DateTime auditDataEnd = auditDataStart.plusDays(numDays);
			ReadableInterval auditInterval = new Interval(auditDataStart, auditDataEnd);
			expect(nodeDatumDao.getAuditInterval(node.getId(), null)).andReturn(auditInterval);

			List<Long> countList = new ArrayList<>(numDays);
			List<DateTime> dayList = new ArrayList<>(numDays);
			for ( int i = 0; i < numDays; i++ ) {
				dayList.add(auditDataStart.plusDays(i));
				countList.add((long) (Math.random() * 100000));
			}
			countLists.add(countList);
			dayLists.add(dayList);

			// now iterate over each DAY in audit interval, gathering usage values
			for ( ListIterator<DateTime> itr = dayList.listIterator(); itr.hasNext(); ) {
				DateTime day = itr.next();
				DatumFilterCommand filter = new DatumFilterCommand();
				filter.setNodeId(node.getId());
				filter.setStartDate(day);
				filter.setEndDate(day.plusDays(1));
				filter.setDataPath(DEFAULT_AUDIT_PROP_NAME);
				expect(nodeDatumDao.getAuditCountTotal(filter))
						.andReturn(countList.get(itr.previousIndex()));
			}

			// get the Bundle for this node
			expect(client.bundleForExternalKey(account, bundle.getExternalKey())).andReturn(bundle);

			client.addUsage(same(bundle.getSubscriptions().get(0)),
					eq(ISO_DATE_FORMATTER.print(auditDataEnd.toLocalDate())),
					eq(DEFAULT_USAGE_UNIT_NAME), capture(usageCapture));

			// finally, store the "most recent usage" date for future processing
			userNodeMostRecentKeys.put(node.getId().toString(),
					ISO_DATE_FORMATTER.print(auditDataEnd.toLocalDate()));
			userDao.storeInternalData(user.getId(),
					Collections.singletonMap(KILLBILL_MOST_RECENT_USAGE_KEYS_DATA_PROP,
							new HashMap<String, String>(userNodeMostRecentKeys)));
			idx++;
		}
	}

}
