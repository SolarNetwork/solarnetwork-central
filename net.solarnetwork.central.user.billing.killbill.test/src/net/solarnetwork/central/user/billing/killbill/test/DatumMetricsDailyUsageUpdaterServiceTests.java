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

import static net.solarnetwork.central.user.billing.killbill.DatumMetricsDailyUsageUpdaterService.DEFAULT_BATCH_SIZE;
import static net.solarnetwork.central.user.billing.killbill.DatumMetricsDailyUsageUpdaterService.DEFAULT_PAMENT_METHOD_DATA;
import static net.solarnetwork.central.user.billing.killbill.DatumMetricsDailyUsageUpdaterService.KILLBILL_ACCOUNT_KEY_DATA_PROP;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isNull;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.easymock.Capture;
import org.easymock.CaptureType;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.central.dao.SolarLocationDao;
import net.solarnetwork.central.datum.dao.GeneralNodeDatumDao;
import net.solarnetwork.central.domain.FilterResults;
import net.solarnetwork.central.domain.SolarLocation;
import net.solarnetwork.central.domain.SolarNode;
import net.solarnetwork.central.support.BasicFilterResults;
import net.solarnetwork.central.user.billing.domain.BillingDataConstants;
import net.solarnetwork.central.user.billing.killbill.DatumMetricsDailyUsageUpdaterJob;
import net.solarnetwork.central.user.billing.killbill.DatumMetricsDailyUsageUpdaterService;
import net.solarnetwork.central.user.billing.killbill.KillbillClient;
import net.solarnetwork.central.user.billing.killbill.domain.Account;
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
	}

	private static final Long TEST_USER_ID = -1L;
	private static final String TEST_USER_EMAIL = "test@localhost";
	private static final String TEST_USER_NAME = "John Doe";
	private static final String TEST_USER_ACCOUNT_KEY = "SN" + TEST_USER_ID;
	private static final Long TEST_NODE_ID = -2L;
	private static final Long TEST_LOCATION_ID = -3L;
	private static final String TEST_LOCATION_COUNTRY = "NZ";
	private static final String TEST_NODE_TZ = "Pacific/Auckland";
	private static final String TEST_NODE_TZ_OFFSET = "+12:00";
	private static final String TEST_ACCOUNT_ID = "abc-123";
	private static final String TEST_PAYMENT_METHOD_ID = "def-234";

	@Test
	public void oneUserNoAccountNoData() {
		// search for users configured to use killbill; find one
		Map<String, Object> userBillingData = new HashMap<>(4);
		userBillingData.put(BillingDataConstants.ACCOUNTING_DATA_PROP,
				DatumMetricsDailyUsageUpdaterService.KILLBILL_ACCOUNTING_VALUE);
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
		assertEquals("Currency", "NZD", account.getCurrency());
		assertEquals("Email", account.getEmail(), TEST_USER_EMAIL);
		assertEquals("ExternalKey", TEST_USER_ACCOUNT_KEY, account.getExternalKey());
		assertEquals("Name", TEST_USER_NAME, account.getName());
		assertEquals("TimeZone", TEST_NODE_TZ_OFFSET, account.getTimeZone());

	}

}
