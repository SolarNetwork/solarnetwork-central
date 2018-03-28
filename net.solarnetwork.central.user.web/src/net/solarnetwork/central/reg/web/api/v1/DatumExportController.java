/* ==================================================================
 * DatumExportController.java - 29/03/2018 6:22:43 AM
 * 
 * Copyright 2018 SolarNetwork.net Dev Team
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
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import net.solarnetwork.central.security.SecurityUser;
import net.solarnetwork.central.security.SecurityUtils;
import net.solarnetwork.central.user.export.biz.UserExportBiz;
import net.solarnetwork.central.user.export.domain.UserDatumExportConfiguration;
import net.solarnetwork.util.OptionalService;
import net.solarnetwork.web.domain.Response;

/**
 * Web service API for datum export management.
 * 
 * @author matt
 * @version 1.0
 * @since 1.26
 */
@RestController("v1DatumExportController")
@RequestMapping(value = { "/sec/export", "/v1/sec/user/export" })
public class DatumExportController {

	private final OptionalService<UserExportBiz> exportBiz;

	/**
	 * Constructor.
	 * 
	 * @param billingBiz
	 *        the billing biz to use
	 */
	@Autowired
	public DatumExportController(@Qualifier("exportBiz") OptionalService<UserExportBiz> exportBiz) {
		super();
		this.exportBiz = exportBiz;
	}

	@ResponseBody
	@RequestMapping(value = "/configs", method = RequestMethod.GET)
	public Response<Object> foo() {
		SecurityUser actor = SecurityUtils.getCurrentUser();
		UserExportBiz biz = exportBiz.service();
		List<UserDatumExportConfiguration> configs = null;
		if ( biz != null ) {
			configs = biz.datumExportsForUser(actor.getUserId());
		}
		return response(configs);
	}

}
