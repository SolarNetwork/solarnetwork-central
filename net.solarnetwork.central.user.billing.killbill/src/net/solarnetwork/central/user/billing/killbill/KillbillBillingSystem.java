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
import java.util.Locale;
import net.solarnetwork.central.domain.FilterResults;
import net.solarnetwork.central.domain.SortDescriptor;
import net.solarnetwork.central.security.AuthorizationException;
import net.solarnetwork.central.security.AuthorizationException.Reason;
import net.solarnetwork.central.support.BasicFilterResults;
import net.solarnetwork.central.user.billing.biz.BillingSystem;
import net.solarnetwork.central.user.billing.domain.BillingSystemInfo;
import net.solarnetwork.central.user.billing.domain.InvoiceFilter;
import net.solarnetwork.central.user.billing.domain.InvoiceMatch;
import net.solarnetwork.central.user.billing.killbill.domain.Account;
import net.solarnetwork.central.user.billing.support.BasicBillingSystemInfo;
import net.solarnetwork.central.user.dao.UserDao;
import net.solarnetwork.central.user.domain.User;

/**
 * Killbill implementation of {@link BillingSystem}.
 * 
 * @author matt
 * @version 1.0
 */
public class KillbillBillingSystem implements BillingSystem {

	/** The {@literal accounting} billing data value for Killbill. */
	public static final String ACCOUNTING_SYSTEM_KEY = "kb";

	private final UserDao userDao;
	private final KillbillClient client;

	/**
	 * Constructor.
	 * 
	 * @param client
	 *        the client to use
	 * @param userDao
	 *        the User DAO to use
	 */
	public KillbillBillingSystem(KillbillClient client, UserDao userDao) {
		super();
		this.userDao = userDao;
		this.client = client;
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

	@Override
	public FilterResults<InvoiceMatch> findFilteredInvoices(InvoiceFilter filter,
			List<SortDescriptor> sortDescriptors, Integer offset, Integer max) {
		User user = userDao.get(filter.getUserId());
		if ( user == null ) {
			throw new AuthorizationException(Reason.UNKNOWN_OBJECT, filter.getUserId());
		}
		String accountKey = (String) user
				.getInternalDataValue(UserDataProperties.KILLBILL_ACCOUNT_KEY_DATA_PROP);
		if ( accountKey == null ) {
			throw new AuthorizationException(Reason.UNKNOWN_OBJECT, filter.getUserId());
		}
		Account account = client.accountForExternalKey(accountKey);
		if ( account == null ) {
			return new BasicFilterResults<InvoiceMatch>(null);
		}
		FilterResults<InvoiceMatch> results;
		if ( filter.getUnpaid() != null && filter.getUnpaid().booleanValue() ) {
			@SuppressWarnings({ "rawtypes", "unchecked" })
			List<InvoiceMatch> invoices = (List) client.listInvoices(account, true);
			results = new BasicFilterResults<>(invoices, (long) invoices.size(), 0, invoices.size());
		} else {
			// TODO
			results = null;
		}
		return results;
	}

}
