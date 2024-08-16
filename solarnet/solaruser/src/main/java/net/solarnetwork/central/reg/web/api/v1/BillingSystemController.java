/* ==================================================================
 * BillingSystemController.java - 16/08/2024 4:29:21 pm
 *
 * Copyright 2024 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.reg.web.api.v1;

import static net.solarnetwork.domain.Result.success;
import java.util.List;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import net.solarnetwork.central.user.billing.biz.BillingBiz;
import net.solarnetwork.central.user.billing.biz.BillingSystem;
import net.solarnetwork.central.user.billing.domain.BillingSystemInfo;
import net.solarnetwork.central.user.billing.domain.NamedCostTiers;
import net.solarnetwork.central.web.GlobalExceptionRestController;
import net.solarnetwork.domain.Result;

/**
 * Web service API for public billing system information.
 *
 * @author matt
 * @version 2.0
 */
@RestController("v1BillingSystemController")
@RequestMapping(value = { "/u/sec/billing", "/api/v1/pub/user/billing", "/api/v1/sec/user/billing" })
@GlobalExceptionRestController
public class BillingSystemController {

	private final BillingBiz billingBiz;

	/**
	 * Constructor.
	 *
	 * @param billingBiz
	 *        the billing biz to use
	 */
	public BillingSystemController(@Autowired(required = false) BillingBiz billingBiz) {
		super();
		this.billingBiz = billingBiz;
	}

	private BillingBiz billingBiz() {
		if ( billingBiz == null ) {
			throw new UnsupportedOperationException("Billing service not available.");
		}
		return billingBiz;
	}

	/**
	 * Get billing system info for the current user.
	 *
	 * @param locale
	 *        the Locale of the request
	 * @return the billing system info
	 */
	@RequestMapping(method = RequestMethod.GET, value = "/systemInfo")
	public Result<BillingSystemInfo> billingSystemInfoForKey(@RequestParam("key") String key,
			Locale locale) {
		BillingBiz biz = billingBiz();
		BillingSystem system = biz.billingSystemForKey(key);
		BillingSystemInfo info = (system != null ? system.getInfo(locale) : null);
		return success(info);
	}

	/**
	 * Get billing system info for the current user.
	 *
	 * @param locale
	 *        the Locale of the request
	 * @return the billing system info
	 */
	@RequestMapping(method = RequestMethod.GET, value = "/costs")
	public Result<List<? extends NamedCostTiers>> billingSystemTiers(@RequestParam("key") String key,
			Locale locale) {
		BillingBiz biz = billingBiz();
		BillingSystem system = biz.billingSystemForKey(key);
		List<? extends NamedCostTiers> tiers = (system != null ? system.namedCostTiers(locale) : null);
		return success(tiers);
	}

}
