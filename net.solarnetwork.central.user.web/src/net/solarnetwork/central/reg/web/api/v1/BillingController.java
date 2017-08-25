/* ==================================================================
 * BillingController.java - 25/08/2017 7:32:16 AM
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

package net.solarnetwork.central.reg.web.api.v1;

import static net.solarnetwork.web.domain.Response.response;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import net.solarnetwork.central.security.SecurityUser;
import net.solarnetwork.central.security.SecurityUtils;
import net.solarnetwork.central.user.billing.biz.BillingBiz;
import net.solarnetwork.central.user.billing.biz.BillingSystem;
import net.solarnetwork.central.user.billing.domain.BillingSystemInfo;
import net.solarnetwork.central.web.support.WebServiceControllerSupport;
import net.solarnetwork.util.OptionalService;
import net.solarnetwork.web.domain.Response;

/**
 * Web service API for billing management.
 * 
 * @author matt
 * @version 1.0
 */
@RestController("v1BillingController")
@RequestMapping(value = { "/sec/billing", "/v1/sec/user/billing" })
public class BillingController extends WebServiceControllerSupport {

	private final OptionalService<BillingBiz> billingBiz;

	/**
	 * Constructor.
	 * 
	 * @param billingBiz
	 *        the billing biz to use
	 */
	@Autowired
	public BillingController(@Qualifier("billingBiz") OptionalService<BillingBiz> billingBiz) {
		super();
		this.billingBiz = billingBiz;
	}

	/**
	 * Get billing system info for the current user.
	 * 
	 * @param locale
	 *        the Locale of the request
	 * @return the billing system info
	 */
	@RequestMapping(method = RequestMethod.GET, value = "/systemInfo")
	public Response<BillingSystemInfo> billingSystemInfoForUser(Locale locale) {
		SecurityUser actor = SecurityUtils.getCurrentUser();
		BillingSystemInfo info = null;
		BillingBiz biz = billingBiz.service();
		if ( biz != null ) {
			BillingSystem system = biz.billingSystemForUser(actor.getUserId());
			info = (system != null ? system.getInfo(locale) : null);
		}
		return response(info);
	}

}
