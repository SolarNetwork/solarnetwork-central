/* ==================================================================
 * DatumMetricsDailyUsageUpdaterService.java - 22/08/2017 1:47:50 PM
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

package net.solarnetwork.central.user.billing.killbill;

import static net.solarnetwork.central.user.billing.domain.BillingDataConstants.filterForAccountingType;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.joda.time.ReadableInterval;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.central.dao.SolarLocationDao;
import net.solarnetwork.central.datum.dao.GeneralNodeDatumDao;
import net.solarnetwork.central.datum.domain.DatumFilterCommand;
import net.solarnetwork.central.domain.FilterResults;
import net.solarnetwork.central.domain.SolarLocation;
import net.solarnetwork.central.user.billing.killbill.domain.Account;
import net.solarnetwork.central.user.billing.killbill.domain.Bundle;
import net.solarnetwork.central.user.billing.killbill.domain.Subscription;
import net.solarnetwork.central.user.billing.killbill.domain.UsageRecord;
import net.solarnetwork.central.user.dao.UserDao;
import net.solarnetwork.central.user.dao.UserNodeDao;
import net.solarnetwork.central.user.domain.User;
import net.solarnetwork.central.user.domain.UserFilterCommand;
import net.solarnetwork.central.user.domain.UserFilterMatch;
import net.solarnetwork.central.user.domain.UserInfo;
import net.solarnetwork.central.user.domain.UserNode;

/**
 * FIXME
 * 
 * <p>
 * TODO
 * </p>
 * 
 * @author matt
 * @version 1.0
 */
public class DatumMetricsDailyUsageUpdaterService {

	/** The default currency map. */
	public static final Map<String, String> DEFAULT_CURRENCY_MAP = defaultCurrencyMap();

	/** The default payment method data. */
	public static final Map<String, Object> DEFAULT_PAMENT_METHOD_DATA = defaultPaymentMethodData();

	public static final String KILLBILL_ACCOUNTING_VALUE = "killbill";
	public static final String KILLBILL_ACCOUNT_KEY_DATA_PROP = "accountKey";
	public static final String KILLBILL_MOST_RECENT_USAGE_KEY_DATA_PROP = "mrUsageDate";

	/** The default base plan name. */
	public static final String DEFAULT_BASE_PLAN_NAME = "api-posted-datum-metric-monthly-usage";

	/** The default time zone offset (milliseconds from UTC) to use. */
	public static final int DEFAULT_TIMEZONE_OFFSET = 12 * 60 * 60 * 1000;

	/** The default bundle key template to use. */
	public static final String DEFAULT_BUNDLE_KEY_TEMPLATE = "IN_%s";

	/** The default bill cycle day. */
	public static final Integer DEFAULT_BILL_CYCLE_DAY = 5;

	/** The default batch size. */
	public static final int DEFAULT_BATCH_SIZE = 50;

	/** The default usage unit name. */
	public static final String DEFAULT_USAGE_UNIT_NAME = "DatumMetrics";

	private static final DateTimeFormatter ISO_DATE_FORMATTER = ISODateTimeFormat.date();

	private final SolarLocationDao locationDao;
	private final GeneralNodeDatumDao nodeDatumDao;
	private final UserDao userDao;
	private final UserNodeDao userNodeDao;
	private final KillbillClient client;
	private int batchSize = DEFAULT_BATCH_SIZE;
	private Map<String, String> countryCurrencyMap = DEFAULT_CURRENCY_MAP;
	private int timeZoneOffset = DEFAULT_TIMEZONE_OFFSET;
	private Map<String, Object> paymentMethodData = DEFAULT_PAMENT_METHOD_DATA;
	private String basePlanName = DEFAULT_BASE_PLAN_NAME;
	private String bundleKeyTemplate = DEFAULT_BUNDLE_KEY_TEMPLATE;
	private Integer billCycleDay = DEFAULT_BILL_CYCLE_DAY;
	private String usageUnitName = DEFAULT_USAGE_UNIT_NAME;

	private final Logger log = LoggerFactory.getLogger(getClass());

	private static final Map<String, String> defaultCurrencyMap() {
		Map<String, String> map = new HashMap<>();
		map.put("US", "USD");
		map.put("NZ", "NZD");
		return Collections.unmodifiableMap(map);
	}

	private static final Map<String, Object> defaultPaymentMethodData() {
		Map<String, Object> map = new HashMap<>();
		map.put("pluginName", "__EXTERNAL_PAYMENT__");
		map.put("pluginInfo", new HashMap<String, Object>());
		return Collections.unmodifiableMap(map);
	}

	/**
	 * Constructor.
	 * 
	 * @param locationDao
	 *        the {@link SolarLocationDao} to use
	 * @param userDao
	 *        the {@link UserDao} to use
	 * @param userNodeDao
	 *        the {@link UserNodeDao} to use
	 * @param nodeDatumDao
	 *        the {@link GeneralNodeDatumDao} to use
	 * @param client
	 *        the {@link KillbillClient} to use
	 */
	public DatumMetricsDailyUsageUpdaterService(SolarLocationDao locationDao, UserDao userDao,
			UserNodeDao userNodeDao, GeneralNodeDatumDao nodeDatumDao, KillbillClient client) {
		this.locationDao = locationDao;
		this.userDao = userDao;
		this.userNodeDao = userNodeDao;
		this.nodeDatumDao = nodeDatumDao;
		this.client = client;
	}

	/**
	 * Execute the task.
	 */
	public void execute() {
		// iterate over users configured to use Killbill
		UserFilterCommand criteria = filterForAccountingType(KILLBILL_ACCOUNTING_VALUE);
		final int max = this.batchSize;
		int offset = 0;
		FilterResults<UserFilterMatch> userResults;
		do {
			userResults = userDao.findFiltered(criteria, null, offset, max);
			for ( UserFilterMatch match : userResults ) {
				Map<String, Object> billingData = match.getBillingData();
				String accountKey;
				if ( billingData.get(KILLBILL_ACCOUNT_KEY_DATA_PROP) instanceof String ) {
					accountKey = (String) billingData.get(KILLBILL_ACCOUNT_KEY_DATA_PROP);
				} else {
					// assign account key
					accountKey = "SN" + match.getId();
					userDao.storeBillingDataProperty(match.getId(), KILLBILL_ACCOUNT_KEY_DATA_PROP,
							accountKey);
				}
				processOneAccount(match, accountKey);
			}
		} while ( userResults.getStartingOffset() != null && userResults.getReturnedResultCount() != null
				&& userResults.getTotalResults() != null && (userResults.getStartingOffset()
						+ userResults.getReturnedResultCount() < userResults.getTotalResults()) );
	}

	private void processOneAccount(UserInfo user, String accountKey) {
		log.debug("Processing account {} for user {}", accountKey, user.getEmail());
		Account account = client.accountForExternalKey(accountKey);
		if ( account == null ) {
			// create new account
			if ( user.getLocationId() == null ) {
				log.error("Location ID not available on user {} ({}): cannot create Killbill account",
						user.getId(), user.getEmail());
				return;
			}
			SolarLocation loc = locationDao.get(user.getLocationId());
			if ( loc == null ) {
				log.error("Location {} not available for user {} ({}): cannot create Killbill account",
						user.getLocationId(), user.getId(), user.getEmail());
				return;
			}
			log.info("Creating new account in Killbill for user {}", user.getEmail());
			account = new Account();
			account.setCountry(loc.getCountry());
			account.setCurrency(this.countryCurrencyMap.get(loc.getCountry()));
			account.setEmail(user.getEmail());
			account.setExternalKey(accountKey);
			account.setName(user.getName());
			account.setTimeZone(accountTimeZoneString(loc.getTimeZoneId()));
			String accountId = client.createAccount(account);
			account.setAccountId(accountId);
		}

		// verify payment method is configured on account
		if ( account.getPaymentMethodId() == null ) {
			account.setPaymentMethodId(
					client.addPaymentMethodToAccount(account, paymentMethodData, true));
		}

		// get or create bundles for all nodes
		DateTimeZone timeZone = DateTimeZone.forTimeZone(timeZoneForAccount(account));
		List<UserNode> userNodes = userNodeDao
				.findUserNodesForUser((user instanceof User ? (User) user : userDao.get(user.getId())));

		for ( UserNode userNode : userNodes ) {
			processOneUserNode(account, userNode, timeZone);
		}
	}

	private void processOneUserNode(Account account, UserNode userNode, DateTimeZone timeZone) {
		DatumFilterCommand criteria = new DatumFilterCommand();
		criteria.setNodeId(userNode.getNode().getId());

		DateTime usageStartDay = null;
		DateTime usageEndDay = new DateTime(timeZone).dayOfMonth().roundFloorCopy();

		// get the range of available audit data for this node, to help know when to start/stop
		ReadableInterval auditInterval = nodeDatumDao.getAuditInterval(userNode.getNode().getId(), null);

		String mostRecentUsageDate = (String) userNode.getUser().getBillingData()
				.get(KILLBILL_MOST_RECENT_USAGE_KEY_DATA_PROP);
		if ( mostRecentUsageDate != null ) {
			LocalDate mrDate = ISO_DATE_FORMATTER.parseLocalDate(mostRecentUsageDate);
			usageStartDay = mrDate.toDateTimeAtStartOfDay(timeZone);
		} else if ( auditInterval != null ) {
			usageStartDay = auditInterval.getStart().withZone(timeZone).dayOfMonth().roundFloorCopy();
			usageEndDay = auditInterval.getEnd().withZone(timeZone).dayOfMonth().roundFloorCopy();
		}
		if ( usageStartDay == null ) {
			log.debug("No usage start date available for user {} node {}", userNode.getUser().getEmail(),
					userNode.getNode().getId());
			return;
		}

		// got usage start date; get the bundle for this node
		List<UsageRecord> recordsToAdd = new ArrayList<>();
		while ( usageStartDay.isBefore(usageEndDay) ) {
			DateTime nextDay = usageStartDay.plusDays(1);
			criteria.setStartDate(usageStartDay);
			criteria.setEndDate(nextDay);
			long dayUsage = nodeDatumDao.getAuditPropertyCountTotal(criteria);
			if ( dayUsage > 0 ) {
				UsageRecord record = new UsageRecord();
				record.setRecordDate(usageStartDay.toLocalDate());
				record.setAmount(new BigDecimal(dayUsage));
				recordsToAdd.add(record);
			}
			usageStartDay = nextDay;
		}

		if ( !recordsToAdd.isEmpty() ) {
			Bundle bundle = bundleForUserNode(userNode, account);
			if ( bundle != null && bundle.getSubscriptions() != null
					&& !bundle.getSubscriptions().isEmpty() ) {
				log.info("Adding {} usage to user {} node {} between {}-{}",
						userNode.getUser().getEmail(), this.usageUnitName, userNode.getNode().getId(),
						recordsToAdd.get(0).getRecordDate(),
						recordsToAdd.get(recordsToAdd.size() - 1).getRecordDate());
				client.addUsage(bundle.getSubscriptions().get(0), this.usageUnitName, recordsToAdd);
			}
		} else {
			log.debug("No {} usage to add for user {} node {} between {}-{}",
					userNode.getUser().getEmail(), this.usageUnitName, userNode.getNode().getId(),
					recordsToAdd.get(recordsToAdd.size() - 1).getRecordDate());
		}

		// store the last processed date so we can pick up there next time
		userDao.storeBillingDataProperty(userNode.getUser().getId(),
				KILLBILL_MOST_RECENT_USAGE_KEY_DATA_PROP,
				ISO_DATE_FORMATTER.print(usageStartDay.toLocalDate()));
	}

	private Bundle bundleForUserNode(UserNode userNode, Account account) {
		final String bundleKey = String.format(this.bundleKeyTemplate, userNode.getNode().getId());
		Bundle bundle = client.bundleForExternalKey(bundleKey);
		if ( bundle == null ) {
			// create it now
			bundle = new Bundle();
			bundle.setAccountId(account.getAccountId());
			bundle.setExternalKey(bundleKey);

			Subscription base = new Subscription();
			base.setBillCycleDayLocal(this.billCycleDay);
			base.setPlanName(basePlanName);
			base.setProductCategory(Subscription.BASE_PRODUCT_CATEGORY);

			bundle.setSubscriptions(Arrays.asList(base));

			// find requestedDate based on earliest data date
			DateTime oldestPostedDatumDate = null;
			ReadableInterval dataInterval = nodeDatumDao
					.getReportableInterval(userNode.getNode().getId(), null);
			if ( dataInterval != null && dataInterval.getStart() != null ) {
				oldestPostedDatumDate = dataInterval.getStart();
			}

			LocalDate requestedDate = (oldestPostedDatumDate != null ? oldestPostedDatumDate
					.withZone(DateTimeZone.forTimeZone(timeZoneForAccount(account))).toLocalDate()
					: null);
			String bundleId = client.createAccountBundle(account, requestedDate, bundle);
			bundle.setBundleId(bundleId);
		}
		return bundle;
	}

	private String accountTimeZoneString(String timeZoneId) {
		// killbill uses absolute offsets; get the non-DST time zone for the location
		TimeZone tz = accountTimeZone(timeZoneId);
		int tzOffset = tz.getRawOffset();
		int tzOffsetHours = tzOffset / (60 * 60 * 1000);
		int tzOffsetMins = Math.abs((tzOffset - (tzOffsetHours * 60 * 60 * 1000)) / (60 * 1000));
		return String.format("%+02d:%02d", tzOffsetHours, tzOffsetMins);
	}

	private TimeZone accountTimeZone(String timeZoneId) {
		TimeZone tz = null;
		if ( timeZoneId != null ) {
			tz = TimeZone.getTimeZone(timeZoneId);
		} else {
			tz = TimeZone.getTimeZone(TimeZone.getAvailableIDs(timeZoneOffset)[0]);
		}
		return tz;
	}

	private TimeZone timeZoneForAccount(Account account) {
		if ( account != null && account.getTimeZone() != null ) {
			return TimeZone.getTimeZone("GMT" + account.getTimeZone());
		}
		return accountTimeZone(null);
	}

	/**
	 * Set the batch size.
	 * 
	 * @param batchSize
	 *        the batch size to set
	 */
	public void setBatchSize(int batchSize) {
		this.batchSize = batchSize;
	}

	/**
	 * Set a mapping of country keys to currency keys.
	 * 
	 * @param countryCurrencyMap
	 *        the map to set; defaults to {@link #DEFAULT_CURRENCY_MAP}
	 */
	public void setCountryCurrencyMap(Map<String, String> countryCurrencyMap) {
		assert countryCurrencyMap != null;
		this.countryCurrencyMap = countryCurrencyMap;
	}

	/**
	 * Set a time zone offset to use for users without a time zone in their
	 * location.
	 * 
	 * @param timeZoneOffset
	 *        the milliseconds offset from UTC to use; defaults to
	 *        {@link #DEFAULT_TIMEZONE_OFFSET} (UTC+12:00)
	 */
	public void setTimeZoneOffset(int timeZoneOffset) {
		this.timeZoneOffset = timeZoneOffset;
	}

	/**
	 * Set the default payment method data to use.
	 * 
	 * @param paymentMethodData
	 *        the payment method data to set; defaults to
	 *        {@link #DEFAULT_PAMENT_METHOD_DATA}
	 */
	public void setPaymentMethodData(Map<String, Object> paymentMethodData) {
		this.paymentMethodData = paymentMethodData;
	}

	/**
	 * Set the base Killbill plan name to use.
	 * 
	 * @param basePlanName
	 *        the base plan name to set
	 */
	public void setBasePlanName(String basePlanName) {
		this.basePlanName = basePlanName;
	}

	/**
	 * Set the string format template to use when generating the bundle key.
	 * 
	 * <p>
	 * This template is expected to accept a single string parameter (a node's
	 * ID).
	 * </p>
	 * 
	 * @param bundleKeyTemplate
	 *        the bundle key template to set
	 */
	public void setBundleKeyTemplate(String bundleKeyTemplate) {
		this.bundleKeyTemplate = bundleKeyTemplate;
	}

	/**
	 * Set the bill cycle day to use.
	 * 
	 * @param billCycleDay
	 *        the bill cycle day to set
	 */
	public void setBillCycleDay(Integer billCycleDay) {
		this.billCycleDay = billCycleDay;
	}

	/**
	 * Set the Killbill usage unit name to use.
	 * 
	 * @param usageUnitName
	 *        the usage unit name
	 */
	public void setUsageUnitName(String usageUnitName) {
		this.usageUnitName = usageUnitName;
	}

}
