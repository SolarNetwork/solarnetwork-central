/* ==================================================================
 * DaoBillingBizTests.java - 25/08/2017 3:28:09 PM
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

package net.solarnetwork.central.user.billing.biz.dao.test;

import static org.assertj.core.api.BDDAssertions.then;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import net.solarnetwork.central.test.CommonTestUtils;
import net.solarnetwork.central.user.billing.biz.BillingSystem;
import net.solarnetwork.central.user.billing.biz.dao.DaoBillingBiz;
import net.solarnetwork.central.user.billing.domain.BillingDataConstants;
import net.solarnetwork.central.user.billing.domain.Invoice;
import net.solarnetwork.central.user.billing.domain.InvoiceFilterCommand;
import net.solarnetwork.central.user.billing.domain.InvoiceMatch;
import net.solarnetwork.central.user.dao.UserDao;
import net.solarnetwork.central.user.domain.User;
import net.solarnetwork.dao.BasicFilterResults;
import net.solarnetwork.dao.FilterResults;

/**
 * Test cases for the {@link DaoBillingBiz} class.
 * 
 * @author matt
 * @version 1.1
 */
@ExtendWith(MockitoExtension.class)
public class DaoBillingBizTests {

	@Mock
	private UserDao userDao;

	@Mock
	private BillingSystem system1;

	private DaoBillingBiz service;

	@BeforeEach
	public void setup() {
		List<BillingSystem> systems = new ArrayList<>(1);
		systems.add(system1);

		service = new DaoBillingBiz(userDao, systems);
	}

	@Test
	public void systemForKey_found() {
		// GIVEN
		final String key = CommonTestUtils.randomString();
		given(system1.supportsAccountingSystemKey(key)).willReturn(true);

		// WHEN
		BillingSystem result = service.billingSystemForKey(key);

		// THEN
		then(result).as("Accounting system for matching key returned").isSameAs(system1);
	}

	@Test
	public void systemForKey_notFound() {
		// GIVEN
		final String key = CommonTestUtils.randomString();
		given(system1.supportsAccountingSystemKey(key)).willReturn(false);

		// WHEN
		BillingSystem result = service.billingSystemForKey(key);

		// THEN
		then(result).as("Null returned when no matching key found").isNull();
	}

	private void givenUser(Long userId, String systemKey) {
		final User user = new User(userId, "test@localhost");
		if ( systemKey != null ) {
			user.putInternalDataValue(BillingDataConstants.ACCOUNTING_DATA_PROP, systemKey);
		}
		given(userDao.get(userId)).willReturn(user);
	}

	@Test
	public void consolidatedInvoices_delegated() {
		// GIVEN
		final Long userId = randomLong();
		final String key = randomString();
		givenUser(userId, key);
		given(system1.supportsAccountingSystemKey(key)).willReturn(true);

		final var filter = new InvoiceFilterCommand();
		filter.setUserId(userId);
		final FilterResults<InvoiceMatch, String> matches = new BasicFilterResults<>(List.of());
		given(system1.findFilteredConsolidatedInvoices(filter, null, null, null)).willReturn(matches);
		final Invoice invoice = mock(Invoice.class);
		given(system1.getConsolidatedInvoice(userId, "4", Locale.ENGLISH)).willReturn(invoice);

		// WHEN
		FilterResults<InvoiceMatch, String> findResult = service.findFilteredConsolidatedInvoices(filter,
				null, null, null);
		Invoice getResult = service.getConsolidatedInvoice(userId, "4", Locale.ENGLISH);

		// THEN
		then(findResult).as("Results from the user's billing system returned").isSameAs(matches);
		then(getResult).as("Invoice from the user's billing system returned").isSameAs(invoice);
	}

	@Test
	public void consolidatedInvoices_noBillingSystem() {
		// GIVEN
		final Long userId = randomLong();
		givenUser(userId, null);

		final var filter = new InvoiceFilterCommand();
		filter.setUserId(userId);

		// WHEN
		FilterResults<InvoiceMatch, String> findResult = service.findFilteredConsolidatedInvoices(filter,
				null, null, null);
		Invoice getResult = service.getConsolidatedInvoice(userId, "4", Locale.ENGLISH);

		// THEN
		then(findResult.getReturnedResultCount()).as("No results without a billing system").isZero();
		then(getResult).as("No invoice without a billing system").isNull();
	}

}
