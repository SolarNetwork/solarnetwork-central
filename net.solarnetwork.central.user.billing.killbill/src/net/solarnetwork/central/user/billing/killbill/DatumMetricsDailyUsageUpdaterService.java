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

import static net.solarnetwork.central.user.billing.killbill.KillbillClient.ISO_DATE_FORMATTER;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.stream.Collectors;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.joda.time.ReadableInterval;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.central.dao.SolarLocationDao;
import net.solarnetwork.central.datum.dao.GeneralNodeDatumDao;
import net.solarnetwork.central.datum.domain.DatumFilterCommand;
import net.solarnetwork.central.domain.FilterResults;
import net.solarnetwork.central.domain.SolarLocation;
import net.solarnetwork.central.user.billing.domain.BillingDataConstants;
import net.solarnetwork.central.user.billing.killbill.domain.Account;
import net.solarnetwork.central.user.billing.killbill.domain.Bundle;
import net.solarnetwork.central.user.billing.killbill.domain.CustomField;
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
 * Post daily usage data to Killbill for SolarNetwork users subscribed to this
 * service.
 * 
 * <p>
 * To be subscribed to this service, a {@link User} must have billing data with
 * the following:
 * </p>
 * 
 * <dl>
 * <dt><code>accounting</code></dt>
 * <dd><code>kb</code></dd>
 * <dt><code>kb_datumMetricsDailyUsage</code></dt>
 * <dd><code>true</code></dd>
 * </dl>
 * 
 * <p>
 * For users meeting that criteria, a billing data key
 * <code>kb_accountKey</code> will be used as the Killbill account external key
 * value to associate with the user. If <code>kb_accountKey</code> does not
 * exist, a new Killbill account will be created for the user, and an external
 * key will be assigned and saved to the billing data key
 * <code>kb_accountKey</code> for future use (e.g. {@literal SN_123}, see
 * {@link #setAccountKeyTemplate(String)}).
 * </p>
 * 
 * <p>
 * All nodes associated with their SolarNetwork account will be subscribed to a
 * Killbill bundle using a bundle external key based on the node's ID (e.g.
 * {@literal IN_234}, see {@link #setBundleKeyTemplate(String)}). The bundle is
 * expected to contain a subscription to the {@code basePlanName} configured on
 * this service. If the bundle does not exist, it will be created with a single
 * subscription to {@code basePlanName}. The subscription
 * <code>requestedDate</code> will be set to the earliest available datum date
 * for the node, essentially starting the plan when the node first posts data.
 * The subscription billing day will be set to the {@code accountBillCycleDay}
 * configured on this service.
 * </p>
 * 
 * <p>
 * For each node, daily aggregate usage data will be posted to Killbill using
 * the <code>property_count</code> datum audit data available for that node (see
 * {@link GeneralNodeDatumDao#getAuditPropertyCountTotal()}). This service will
 * calculate the usage for all days before the current one and post the usage
 * records to Killbill. The usage unit will be the {@code usageUnitName}
 * configured on this service.
 * </p>
 * 
 * <p>
 * When all nodes for a given user have been processed, the current date is
 * saved in the user's billing data key {@code kb_mrUsageDate}. The next time
 * this service runs it will use that date as the earliest day to upload usage
 * data for.
 * </p>
 * 
 * @author matt
 * @version 1.0
 */
public class DatumMetricsDailyUsageUpdaterService {

	/** The default currency map of country codes to currency codes. */
	public static final Map<String, String> DEFAULT_CURRENCY_MAP = defaultCurrencyMap();

	/**
	 * The billing data key that signals this updater service should be used via
	 * a boolean flag.
	 */
	public static final String KILLBILL_DAILY_USAGE_PLAN_DATA_PROP = "kb_datumMetricsDailyUsage";

	/**
	 * The billing data key that holds the "most recent usage date" to start
	 * from.
	 */
	public static final String KILLBILL_MOST_RECENT_USAGE_KEY_DATA_PROP = "kb_mrUsageDate";

	/** The default base plan name. */
	public static final String DEFAULT_BASE_PLAN_NAME = "api-posted-datum-metric-monthly-usage";

	/** The default time zone. */
	public static final String DEFAULT_TIMEZONE = "Pacific/Auckland";

	/** The default account key template to use. */
	public static final String DEFAULT_ACCOUNT_KEY_TEMPLATE = "SN_%s";

	/** The default bundle key template to use. */
	public static final String DEFAULT_BUNDLE_KEY_TEMPLATE = "IN_%s";

	/** The default account bill cycle day. */
	public static final Integer DEFAULT_ACCOUNT_BILL_CYCLE_DAY = 1;

	/** The default account locale to use. */
	public static final String DEFAULT_ACCOUNT_LOCALE = "en_NZ";

	/** The default usage subscription bill cycle day. */
	public static final Integer DEFAULT_SUBSCRIPTION_BILL_CYCLE_DAY = 1;

	/** The default batch size. */
	public static final int DEFAULT_BATCH_SIZE = 50;

	/** The default usage unit name. */
	public static final String DEFAULT_USAGE_UNIT_NAME = "DatumMetrics";

	/** The custom field name for a SolarNode ID. */
	public static final String CUSTOM_FIELD_NODE_ID = "nodeId";

	private final SolarLocationDao locationDao;
	private final GeneralNodeDatumDao nodeDatumDao;
	private final UserDao userDao;
	private final UserNodeDao userNodeDao;
	private final KillbillClient client;
	private int batchSize = DEFAULT_BATCH_SIZE;
	private Map<String, String> countryCurrencyMap = DEFAULT_CURRENCY_MAP;
	private String timeZone = DEFAULT_TIMEZONE;
	private Map<String, Object> paymentMethodData;
	private String basePlanName = DEFAULT_BASE_PLAN_NAME;
	private String accountKeyTemplate = DEFAULT_ACCOUNT_KEY_TEMPLATE;
	private String bundleKeyTemplate = DEFAULT_BUNDLE_KEY_TEMPLATE;
	private Integer accountBillCycleDay = DEFAULT_ACCOUNT_BILL_CYCLE_DAY;
	private String accountDefaultLocale = DEFAULT_ACCOUNT_LOCALE;
	private Integer subscriptionBillCycleDay = DEFAULT_SUBSCRIPTION_BILL_CYCLE_DAY;
	private String usageUnitName = DEFAULT_USAGE_UNIT_NAME;

	private final Logger log = LoggerFactory.getLogger(getClass());

	private static final Map<String, String> defaultCurrencyMap() {
		Map<String, String> map = new HashMap<>();
		map.put("US", "USD");
		map.put("NZ", "NZD");
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
		Map<String, Object> billingDataFilter = new HashMap<>();
		billingDataFilter.put(BillingDataConstants.ACCOUNTING_DATA_PROP,
				KillbillBillingSystem.ACCOUNTING_SYSTEM_KEY);
		billingDataFilter.put(KILLBILL_DAILY_USAGE_PLAN_DATA_PROP, true);
		UserFilterCommand criteria = new UserFilterCommand();
		criteria.setInternalData(billingDataFilter);
		final int max = this.batchSize;
		int offset = 0;
		FilterResults<UserFilterMatch> userResults;
		do {
			userResults = userDao.findFiltered(criteria, null, offset, max);
			for ( UserFilterMatch match : userResults ) {
				Map<String, Object> billingData = match.getInternalData();
				String accountKey;
				if ( billingData
						.get(UserDataProperties.KILLBILL_ACCOUNT_KEY_DATA_PROP) instanceof String ) {
					accountKey = (String) billingData
							.get(UserDataProperties.KILLBILL_ACCOUNT_KEY_DATA_PROP);
				} else {
					// assign account key
					accountKey = String.format(accountKeyTemplate, match.getId());
					userDao.storeInternalData(match.getId(), Collections.singletonMap(
							UserDataProperties.KILLBILL_ACCOUNT_KEY_DATA_PROP, accountKey));
				}
				processOneAccount(match, accountKey);
			}
		} while ( userResults.getStartingOffset() != null && userResults.getReturnedResultCount() != null
				&& userResults.getTotalResults() != null && (userResults.getStartingOffset()
						+ userResults.getReturnedResultCount() < userResults.getTotalResults()) );
	}

	private Locale localeForUser(UserInfo user, SolarLocation loc) {
		String country = loc.getCountry();
		String[] defaultLangCountry = accountDefaultLocale.split("_");
		assert defaultLangCountry.length > 1;
		Locale defaultLocale = new Locale(defaultLangCountry[0], defaultLangCountry[1]);
		List<Locale> localesForCountry = Arrays.stream(Locale.getAvailableLocales())
				.filter(locale -> country.equals(locale.getCountry())).collect(Collectors.toList());
		if ( localesForCountry.isEmpty() ) {
			return defaultLocale;
		}
		localesForCountry.sort(new Comparator<Locale>() {

			@Override
			public int compare(Locale l, Locale r) {
				// sort those for just language first
				int a = l.getVariant().length();
				int b = r.getVariant().length();
				if ( a == 0 && b > 0 ) {
					return -1;
				} else if ( b == 0 && a > 0 ) {
					return 1;
				}
				a = l.getLanguage().length();
				b = r.getLanguage().length();
				if ( a == 0 && b > 0 ) {
					return -1;
				} else if ( b == 0 && a > 0 ) {
					return 1;
				}
				return l.getCountry().compareTo(r.getCountry());
			}

		});
		return localesForCountry.get(0);
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
			account.setBillCycleDayLocal(accountBillCycleDay);
			account.setCountry(loc.getCountry());
			account.setCurrency(this.countryCurrencyMap.get(loc.getCountry()));
			account.setEmail(user.getEmail());
			account.setExternalKey(accountKey);
			account.setName(user.getName());
			account.setTimeZone(accountTimeZoneString(loc.getTimeZoneId()));

			Locale userLocale = localeForUser(user, loc);
			String locale = userLocale.getLanguage();
			if ( userLocale.getCountry().length() > 0 ) {
				locale += "_" + userLocale.getCountry();
			}
			account.setLocale(locale);

			String accountId = client.createAccount(account);
			account.setAccountId(accountId);
		}

		// verify payment method is configured on account if configured on this service
		if ( account.getPaymentMethodId() == null && paymentMethodData != null ) {
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
		// get the range of available audit data for this node, to help know when to start/stop
		ReadableInterval auditInterval = nodeDatumDao.getAuditInterval(userNode.getNode().getId(), null);

		String mostRecentUsageDate = (String) userNode.getUser().getInternalData()
				.get(KILLBILL_MOST_RECENT_USAGE_KEY_DATA_PROP);
		DateTime usageStartDay = null;
		if ( mostRecentUsageDate != null ) {
			LocalDate mrDate = ISO_DATE_FORMATTER.parseLocalDate(mostRecentUsageDate);
			usageStartDay = mrDate.toDateTimeAtStartOfDay(timeZone);
		} else if ( auditInterval != null ) {
			usageStartDay = auditInterval.getStart().withZone(timeZone).dayOfMonth().roundFloorCopy();
		}
		if ( usageStartDay == null ) {
			log.debug("No usage start date available for user {} node {}", userNode.getUser().getEmail(),
					userNode.getNode().getId());
			return;
		}

		DateTime usageEndDay;
		if ( auditInterval != null ) {
			usageEndDay = auditInterval.getEnd().withZone(timeZone).dayOfMonth().roundCeilingCopy();
		} else {
			usageEndDay = new DateTime(timeZone).dayOfMonth().roundCeilingCopy();
		}

		// got usage start date; get the bundle for this node
		List<UsageRecord> recordsToAdd = new ArrayList<>();
		DateTime usageCurrDay = new DateTime(usageStartDay);
		DatumFilterCommand criteria = new DatumFilterCommand();
		criteria.setNodeId(userNode.getNode().getId());
		while ( usageCurrDay.isBefore(usageEndDay) ) {
			DateTime nextDay = usageCurrDay.plusDays(1);
			criteria.setStartDate(usageCurrDay);
			criteria.setEndDate(nextDay);
			long dayUsage = nodeDatumDao.getAuditPropertyCountTotal(criteria);
			if ( dayUsage > 0 ) {
				UsageRecord record = new UsageRecord();
				record.setRecordDate(usageCurrDay.toLocalDate());
				record.setAmount(new BigDecimal(dayUsage));
				recordsToAdd.add(record);
			}
			usageCurrDay = nextDay;
		}

		String nextStartDate = ISO_DATE_FORMATTER.print(usageEndDay.toLocalDate());
		if ( !recordsToAdd.isEmpty() ) {
			Bundle bundle = bundleForUserNode(userNode, account);
			Subscription subscription = (bundle != null ? bundle.subscriptionWithPlanName(basePlanName)
					: null);
			if ( subscription != null ) {
				if ( log.isInfoEnabled() ) {
					log.info("Adding {} {} usage to user {} node {} between {} and {}",
							recordsToAdd.size(), this.usageUnitName, userNode.getUser().getEmail(),
							userNode.getNode().getId(), usageStartDay.toLocalDate(),
							usageEndDay.toLocalDate());
				}
				client.addUsage(subscription, nextStartDate, this.usageUnitName, recordsToAdd);
			}
		} else if ( log.isDebugEnabled() ) {
			log.debug("No {} usage to add for user {} node {} between {} and {}", this.usageUnitName,
					userNode.getUser().getEmail(), userNode.getNode().getId(),
					usageStartDay.toLocalDate(), usageEndDay.toLocalDate());
		}

		// store the last processed date so we can pick up there next time
		userDao.storeInternalData(userNode.getUser().getId(),
				Collections.singletonMap(KILLBILL_MOST_RECENT_USAGE_KEY_DATA_PROP, nextStartDate));
	}

	private Bundle bundleForUserNode(UserNode userNode, Account account) {
		final String bundleKey = String.format(this.bundleKeyTemplate, userNode.getNode().getId());
		Bundle bundle = client.bundleForExternalKey(account, bundleKey);
		if ( bundle == null ) {
			// create it now
			bundle = new Bundle();
			bundle.setAccountId(account.getAccountId());
			bundle.setExternalKey(bundleKey);

			Subscription base = new Subscription();
			base.setBillCycleDayLocal(this.subscriptionBillCycleDay);
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
			String bundleId = client.createBundle(account, requestedDate, bundle);
			log.info("Created bundle {} with ID {} with plan {} for user {} node {}", bundleKey,
					bundleId, basePlanName, userNode.getUser().getEmail(), userNode.getNode().getId());

			// to pick up subscription ID; re-get bundle now
			bundle = client.bundleForExternalKey(account, bundleKey);

			// add node ID as metadata to the subscription
			Subscription sub = (bundle != null && bundle.getSubscriptions() != null
					&& !bundle.getSubscriptions().isEmpty() ? bundle.getSubscriptions().get(0) : null);
			if ( sub != null ) {
				CustomField field = new CustomField(CUSTOM_FIELD_NODE_ID,
						userNode.getNode().getId().toString());
				String fieldListId = client.createSubscriptionCustomFields(
						bundle.getSubscriptions().get(0).getSubscriptionId(),
						Collections.singletonList(field));
				log.debug("Added user {} node ID {} custom field {} to subscription {}",
						userNode.getUser().getEmail(), userNode.getNode().getId(), fieldListId);
			} else {
				log.warn(
						"Subscription ID not available for bundle {}; cannot add nodeId {} custom field",
						bundleId, userNode.getNode().getId());
			}

		}
		return bundle;
	}

	private String accountTimeZoneString(String timeZoneId) {
		TimeZone tz = accountTimeZone(timeZoneId);
		return tz.getID();
	}

	private TimeZone accountTimeZone(String timeZoneId) {
		TimeZone tz = null;
		if ( timeZoneId != null ) {
			tz = TimeZone.getTimeZone(timeZoneId);
		} else {
			tz = TimeZone.getTimeZone(this.timeZone);
		}
		return tz;
	}

	private TimeZone timeZoneForAccount(Account account) {
		if ( account != null && account.getTimeZone() != null ) {
			return TimeZone.getTimeZone(account.getTimeZone());
		}
		return TimeZone.getTimeZone(this.timeZone);
	}

	/**
	 * Set the batch size to process users with.
	 * 
	 * <p>
	 * This is the maximum number of user records to fetch from the database and
	 * process at a time, e.g. a result page size. The service will iterate over
	 * all result pages to process all users.
	 * </p>
	 * 
	 * @param batchSize
	 *        the user record batch size to set
	 */
	public void setBatchSize(int batchSize) {
		this.batchSize = batchSize;
	}

	/**
	 * Set a mapping of country codes to currency codes.
	 * 
	 * <p>
	 * This is used to assign a currency to new accounts, using the country of
	 * the SolarNetwork user.
	 * </p>
	 * 
	 * @param countryCurrencyMap
	 *        the map to set; defaults to {@link #DEFAULT_CURRENCY_MAP}
	 */
	public void setCountryCurrencyMap(Map<String, String> countryCurrencyMap) {
		assert countryCurrencyMap != null;
		this.countryCurrencyMap = countryCurrencyMap;
	}

	/**
	 * Set a time zone to use for users without a time zone in their location.
	 * 
	 * @param timeZone
	 *        the time zone to use; defaults to {@link #DEFAULT_TIMEZONE}
	 */
	public void setTimeZone(String timeZone) {
		this.timeZone = timeZone != null ? timeZone : DEFAULT_TIMEZONE;
	}

	/**
	 * Set the default payment method data to use when creating one for an
	 * account that does not already have a payment method set.
	 * 
	 * @param paymentMethodData
	 *        the payment method data to set
	 */
	public void setPaymentMethodData(Map<String, Object> paymentMethodData) {
		this.paymentMethodData = paymentMethodData;
	}

	/**
	 * Set the base Killbill plan name to use when creating a bundle for an
	 * account.
	 * 
	 * @param basePlanName
	 *        the base plan name to set
	 */
	public void setBasePlanName(String basePlanName) {
		this.basePlanName = basePlanName;
	}

	/**
	 * Set the string format template to use when generating an account key.
	 * 
	 * <p>
	 * This template is expected to accept a single string parameter (a user's
	 * ID).
	 * </p>
	 * 
	 * @param template
	 *        the account key template to set; defaults to
	 *        {@link #DEFAULT_ACCOUNT_KEY_TEMPLATE}
	 */
	public void setAccountKeyTemplate(String template) {
		this.accountKeyTemplate = template;
	}

	/**
	 * Set the string format template to use when generating a bundle key.
	 * 
	 * <p>
	 * This template is expected to accept a single string parameter (a node's
	 * ID).
	 * </p>
	 * 
	 * @param template
	 *        the bundle key template to set; Defaults to
	 *        {@link #DEFAULT_BUNDLE_KEY_TEMPLATE}
	 */
	public void setBundleKeyTemplate(String template) {
		this.bundleKeyTemplate = template;
	}

	/**
	 * Set the bill cycle day to use.
	 * 
	 * @param billCycleDay
	 *        the account bill cycle day to set; defaults to
	 *        {@link #DEFAULT_ACCOUNT_BILL_CYCLE_DAY}
	 */
	public void setAccountBillCycleDay(Integer billCycleDay) {
		this.accountBillCycleDay = billCycleDay;
	}

	/**
	 * Set the bill cycle day to use.
	 * 
	 * @param billCycleDay
	 *        the subscription bill cycle day to set; defaults to
	 *        {@link #DEFAULT_ACCOUNT_BILL_CYCLE_DAY}
	 */
	public void setSubscriptionBillCycleDay(Integer billCycleDay) {
		this.subscriptionBillCycleDay = billCycleDay;
	}

	/**
	 * Set the Killbill usage unit name to use when posting usage records.
	 * 
	 * @param usageUnitName
	 *        the usage unit name; defaults to {@link #DEFAULT_USAGE_UNIT_NAME}
	 */
	public void setUsageUnitName(String usageUnitName) {
		this.usageUnitName = usageUnitName;
	}

	/**
	 * Set the locale ID to use by default if one cannot be determined from a
	 * {@link User}.
	 * 
	 * @param accountDefaultLocale
	 *        the account default locale to set
	 */
	public void setAccountDefaultLocale(String accountDefaultLocale) {
		this.accountDefaultLocale = accountDefaultLocale;
	}

}
