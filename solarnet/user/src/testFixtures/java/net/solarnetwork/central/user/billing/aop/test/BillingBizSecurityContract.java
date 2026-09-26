/* ==================================================================
 * BillingBizSecurityContract.java - 22/09/2026 10:45:38 am
 *
 * Copyright 2026 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.billing.aop.test;

import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import java.util.Locale;
import org.springframework.util.MimeTypeUtils;
import net.solarnetwork.central.test.aop.SecurityContract;
import net.solarnetwork.central.test.tenant.TestTenant;
import net.solarnetwork.central.test.tenant.TestTenants;
import net.solarnetwork.central.user.billing.biz.BillingBiz;
import net.solarnetwork.central.user.billing.domain.BasicInvoiceGenerationOptions;
import net.solarnetwork.central.user.billing.domain.InvoiceFilterCommand;

/**
 * Security contract for {@link BillingBiz}.
 *
 * <p>
 * The contract is enforced by {@code BillingSecurityAspect}.
 * </p>
 *
 * @author matt
 * @version 1.0
 */
public final class BillingBizSecurityContract {

	private BillingBizSecurityContract() {
		// not available
	}

	/**
	 * Get the contract.
	 *
	 * @param tenants
	 *        the tenants
	 * @return the contract
	 */
	public static SecurityContract<BillingBiz> contract(TestTenants tenants) {
		final TestTenant a = tenants.a();
		final String invoiceId = randomString();
		final InvoiceFilterCommand filter = new InvoiceFilterCommand();
		filter.setUserId(a.userId());

		// @formatter:off
		return SecurityContract.forApi(BillingBiz.class, tenants)
				.userRead(biz -> biz.billingSystemForUser(a.userId()))
				.exempt("availableBillingSystems", "global billing systems listing")
				.exempt("billingSystemForKey", "global billing system lookup")
				.exempt("defaultBillingSystem", "global default billing system lookup")
				.userRead(biz -> biz.getInvoice(a.userId(), invoiceId, Locale.ENGLISH))
				.userRead(biz -> biz.findFilteredInvoices(filter, null, null, null))
				.userRead(biz -> biz.renderInvoice(a.userId(), invoiceId, MimeTypeUtils.TEXT_HTML,
						Locale.ENGLISH))
				.userRead(biz -> biz.getPreviewInvoice(a.userId(),
						new BasicInvoiceGenerationOptions(false), Locale.ENGLISH))
				.userRead(biz -> biz.previewInvoice(a.userId(),
						new BasicInvoiceGenerationOptions(false), MimeTypeUtils.TEXT_HTML,
						Locale.ENGLISH))
				.build();
		// @formatter:on
	}

}
