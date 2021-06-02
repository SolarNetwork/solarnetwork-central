/* ==================================================================
 * DaoBillingBiz.java - 25/08/2017 3:14:38 PM
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

package net.solarnetwork.central.user.billing.biz.dao;

import java.util.List;
import java.util.Locale;
import org.springframework.core.io.Resource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MimeType;
import net.solarnetwork.central.domain.FilterResults;
import net.solarnetwork.central.domain.SortDescriptor;
import net.solarnetwork.central.support.BasicFilterResults;
import net.solarnetwork.central.user.billing.biz.BillingBiz;
import net.solarnetwork.central.user.billing.biz.BillingSystem;
import net.solarnetwork.central.user.billing.domain.BillingDataConstants;
import net.solarnetwork.central.user.billing.domain.Invoice;
import net.solarnetwork.central.user.billing.domain.InvoiceFilter;
import net.solarnetwork.central.user.billing.domain.InvoiceGenerationOptions;
import net.solarnetwork.central.user.billing.domain.InvoiceMatch;
import net.solarnetwork.central.user.dao.UserDao;
import net.solarnetwork.central.user.domain.User;
import net.solarnetwork.util.OptionalServiceCollection;

/**
 * DAO based implementation of {@link BillingBiz} that delegates responsibility
 * to the {@link BillingSystem} configured for each user.
 * 
 * @author matt
 * @version 1.2
 */
public class DaoBillingBiz implements BillingBiz {

	private final UserDao userDao;
	private final OptionalServiceCollection<BillingSystem> billingSystems;

	/**
	 * Constructor.
	 * 
	 * @param userDao
	 *        the UserDao to use
	 * @param billingSystems
	 *        the billing systems
	 */
	public DaoBillingBiz(UserDao userDao, OptionalServiceCollection<BillingSystem> billingSystems) {
		super();
		this.userDao = userDao;
		this.billingSystems = billingSystems;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * <p>
	 * This implementation returns the first available system where
	 * {@link BillingSystem#supportsAccountingSystemKey(String)} returns
	 * {@literal true} for the user's internal data
	 * {@link BillingDataConstants#ACCOUNTING_DATA_PROP} value.
	 * </p>
	 */
	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public BillingSystem billingSystemForUser(Long userId) {
		User user = userDao.get(userId);
		if ( user != null ) {
			Object systemKey = user.getInternalDataValue(BillingDataConstants.ACCOUNTING_DATA_PROP);
			if ( systemKey != null ) {
				for ( BillingSystem bs : billingSystems.services() ) {
					if ( bs.supportsAccountingSystemKey(systemKey.toString()) ) {
						return bs;
					}
				}
			}
		}
		return null;
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public FilterResults<InvoiceMatch> findFilteredInvoices(InvoiceFilter filter,
			List<SortDescriptor> sortDescriptors, Integer offset, Integer max) {
		BillingSystem system = billingSystemForUser(filter.getUserId());
		if ( system == null ) {
			return new BasicFilterResults<>(null, 0L, 0, 0);
		}
		return system.findFilteredInvoices(filter, sortDescriptors, offset, max);
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public Invoice getInvoice(Long userId, String invoiceId, Locale locale) {
		BillingSystem system = billingSystemForUser(userId);
		if ( system == null ) {
			return null;
		}
		return system.getInvoice(userId, invoiceId, locale);
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public Resource renderInvoice(Long userId, String invoiceId, MimeType outputType, Locale locale) {
		BillingSystem system = billingSystemForUser(userId);
		if ( system == null ) {
			return null;
		}
		return system.renderInvoice(userId, invoiceId, outputType, locale);
	}

	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	@Override
	public Invoice getPreviewInvoice(Long userId, InvoiceGenerationOptions options, Locale locale) {
		BillingSystem system = billingSystemForUser(userId);
		if ( system == null ) {
			return null;
		}
		return system.getPreviewInvoice(userId, options, locale);
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public Resource previewInvoice(Long userId, InvoiceGenerationOptions options, MimeType outputType,
			Locale locale) {
		BillingSystem system = billingSystemForUser(userId);
		if ( system == null ) {
			return null;
		}
		return system.previewInvoice(userId, options, outputType, locale);
	}

}
