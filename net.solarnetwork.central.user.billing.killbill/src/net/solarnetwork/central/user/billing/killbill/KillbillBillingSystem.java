/* ==================================================================
 * KillbillBillingSystem.java - 25/08/2017 3:12:30 PM
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

import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import javax.cache.Cache;
import org.springframework.context.MessageSource;
import org.springframework.core.io.Resource;
import org.springframework.util.MimeType;
import net.solarnetwork.central.domain.FilterResults;
import net.solarnetwork.central.domain.SortDescriptor;
import net.solarnetwork.central.security.AuthorizationException;
import net.solarnetwork.central.security.AuthorizationException.Reason;
import net.solarnetwork.central.support.BasicFilterResults;
import net.solarnetwork.central.user.billing.biz.BillingSystem;
import net.solarnetwork.central.user.billing.domain.BillingSystemInfo;
import net.solarnetwork.central.user.billing.domain.Invoice;
import net.solarnetwork.central.user.billing.domain.InvoiceFilter;
import net.solarnetwork.central.user.billing.domain.InvoiceMatch;
import net.solarnetwork.central.user.billing.domain.LocalizedInvoiceItemInfo;
import net.solarnetwork.central.user.billing.domain.LocalizedInvoiceItemUsageRecordInfo;
import net.solarnetwork.central.user.billing.killbill.domain.Account;
import net.solarnetwork.central.user.billing.killbill.domain.CustomField;
import net.solarnetwork.central.user.billing.killbill.domain.InvoiceItem;
import net.solarnetwork.central.user.billing.killbill.domain.LocalizedInvoiceItem;
import net.solarnetwork.central.user.billing.killbill.domain.LocalizedUnitRecord;
import net.solarnetwork.central.user.billing.killbill.domain.Subscription;
import net.solarnetwork.central.user.billing.killbill.domain.SubscriptionUsageRecords;
import net.solarnetwork.central.user.billing.killbill.domain.UnitRecord;
import net.solarnetwork.central.user.billing.support.BasicBillingSystemInfo;
import net.solarnetwork.central.user.billing.support.LocalizedInvoiceItemUsageRecord;
import net.solarnetwork.central.user.dao.UserDao;
import net.solarnetwork.central.user.domain.User;

/**
 * Killbill implementation of {@link BillingSystem}.
 * 
 * @author matt
 * @version 1.2
 */
public class KillbillBillingSystem implements BillingSystem {

	/** The {@literal accounting} billing data value for Killbill. */
	public static final String ACCOUNTING_SYSTEM_KEY = "kb";

	private final UserDao userDao;
	private final KillbillClient client;
	private final MessageSource messageSource;

	private Cache<String, Subscription> subscriptionCache;

	/**
	 * Constructor.
	 * 
	 * @param client
	 *        the client to use
	 * @param userDao
	 *        the User DAO to use
	 */
	public KillbillBillingSystem(KillbillClient client, UserDao userDao, MessageSource messageSource) {
		super();
		this.userDao = userDao;
		this.client = client;
		this.messageSource = messageSource;
	}

	@Override
	public String getAccountingSystemKey() {
		return ACCOUNTING_SYSTEM_KEY;
	}

	@Override
	public boolean supportsAccountingSystemKey(String key) {
		return ACCOUNTING_SYSTEM_KEY.equals(key);
	}

	@Override
	public BillingSystemInfo getInfo(Locale locale) {
		return new BasicBillingSystemInfo(getAccountingSystemKey());
	}

	private Account accountForUser(Long userId) {
		User user = userDao.get(userId);
		if ( user == null ) {
			throw new AuthorizationException(Reason.UNKNOWN_OBJECT, userId);
		}
		String accountKey = (String) user
				.getInternalDataValue(UserDataProperties.KILLBILL_ACCOUNT_KEY_DATA_PROP);
		if ( accountKey == null ) {
			throw new AuthorizationException(Reason.UNKNOWN_OBJECT, userId);
		}
		return client.accountForExternalKey(accountKey);
	}

	@Override
	public FilterResults<InvoiceMatch> findFilteredInvoices(InvoiceFilter filter,
			List<SortDescriptor> sortDescriptors, Integer offset, Integer max) {
		Account account = accountForUser(filter.getUserId());
		if ( account == null ) {
			return new BasicFilterResults<InvoiceMatch>(null);
		}
		FilterResults<InvoiceMatch> results;
		if ( filter.getUnpaid() != null && filter.getUnpaid().booleanValue() ) {
			@SuppressWarnings({ "rawtypes", "unchecked" })
			List<InvoiceMatch> invoices = (List) client.listInvoices(account, true);
			results = new BasicFilterResults<>(invoices, (long) invoices.size(), 0, invoices.size());
		} else {
			@SuppressWarnings({ "rawtypes", "unchecked" })
			FilterResults<InvoiceMatch> filterResults = (FilterResults) client.findInvoices(account,
					filter, sortDescriptors, offset, max);
			results = filterResults;
		}
		return results;
	}

	private Subscription getSubscription(String subscriptionId) {
		if ( subscriptionId == null ) {
			return null;
		}
		Subscription result = null;
		Cache<String, Subscription> cache = subscriptionCache;
		if ( cache != null ) {
			result = cache.get(subscriptionId);
		}
		if ( result == null ) {
			result = client.getSubscription(subscriptionId);
			if ( result != null ) {
				// populate custom fields
				List<CustomField> fields = client.customFieldsForSubscription(subscriptionId);
				result.setCustomFields(fields);
				if ( cache != null ) {
					cache.putIfAbsent(subscriptionId, result);
				}
			}
		}
		return result;
	}

	@Override
	public Invoice getInvoice(Long userId, String invoiceId, Locale locale) {
		Account account = accountForUser(userId);
		net.solarnetwork.central.user.billing.killbill.domain.Invoice invoice = client
				.getInvoice(account, invoiceId, true, false);

		// populate usage records for appropriate items
		if ( invoice.getInvoiceItems() != null ) {
			for ( ListIterator<InvoiceItem> itr = invoice.getItems().listIterator(); itr.hasNext(); ) {
				InvoiceItem item = itr.next();

				// populate usage
				if ( "USAGE".equals(item.getItemType()) && item.getSubscriptionId() != null ) {
					SubscriptionUsageRecords records = client.usageRecordsForSubscription(
							item.getSubscriptionId(), item.getStartDate(), item.getEndDate());
					if ( records != null ) {
						item.setUsageRecords(records.getRolledUpUnits());
						if ( locale != null ) {
							for ( ListIterator<UnitRecord> recItr = item.getUsageRecords()
									.listIterator(); recItr.hasNext(); ) {
								UnitRecord rec = recItr.next();
								String unitType = messageSource.getMessage(rec.getUnitType(), null, null,
										locale);
								LocalizedInvoiceItemUsageRecordInfo locInfo = new LocalizedInvoiceItemUsageRecord(
										rec, locale, unitType);
								rec = new LocalizedUnitRecord(rec, locInfo);
								recItr.set(rec);
							}
						}
					}
				}

				// poplate metadata
				Subscription subscription = getSubscription(item.getSubscriptionId());
				if ( subscription != null ) {
					item.setCustomFields(subscription.getCustomFields());
				}

				if ( locale != null ) {
					String desc = messageSource.getMessage(item.getPlanName(), null, null, locale);
					LocalizedInvoiceItemInfo locInfo = new net.solarnetwork.central.user.billing.support.LocalizedInvoiceItem(
							item, locale, desc);
					item = new LocalizedInvoiceItem(item, locInfo);
					itr.set(item);
				}
			}
		}
		return invoice;
	}

	@Override
	public Resource renderInvoice(Long userId, String invoiceId, MimeType outputType, Locale locale) {
		// verify first that account owns the requested invoice
		Account account = accountForUser(userId);
		net.solarnetwork.central.user.billing.killbill.domain.Invoice invoice = client
				.getInvoice(account, invoiceId, false, false);
		if ( invoice == null || !account.getAccountId().equals(invoice.getAccountId()) ) {
			return null;
		}
		return client.renderInvoice(invoiceId, outputType, locale);
	}

	/**
	 * Set a cache to use for subscriptions.
	 * 
	 * @param subscriptionCache
	 *        the cache to set
	 */
	public void setSubscriptionCache(Cache<String, Subscription> subscriptionCache) {
		this.subscriptionCache = subscriptionCache;
	}

}
