/* ==================================================================
 * DatumExpireController.java - 10/07/2018 12:29:45 PM
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

package net.solarnetwork.central.reg.web;

import javax.annotation.Resource;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import net.solarnetwork.central.user.expire.biz.UserDatumDeleteBiz;
import net.solarnetwork.service.OptionalService;

/**
 * Controller for the expire page.
 * 
 * @author matt
 * @version 2.0
 */
@GlobalServiceController
public class DatumExpireController {

	/**
	 * The model attribute name for the {@code UserDatumDeleteBiz}.
	 * 
	 * @since 1.1
	 */
	public static final String DATUM_DELETE_BIZ_ATTRIBUTE = "datumDeleteBiz";

	@Resource(name = "datumDeleteBiz")
	private OptionalService<UserDatumDeleteBiz> datumDeleteBiz;

	@RequestMapping(value = "/sec/expire", method = RequestMethod.GET)
	public String home() {
		return "expire/expire";
	}

	/**
	 * The datum delete service.
	 * 
	 * @return the service
	 * @since 1.1
	 */
	@ModelAttribute(value = DATUM_DELETE_BIZ_ATTRIBUTE)
	public UserDatumDeleteBiz datumDeleteBiz() {
		final UserDatumDeleteBiz biz = (datumDeleteBiz != null ? datumDeleteBiz.service() : null);
		return biz;
	}

}
