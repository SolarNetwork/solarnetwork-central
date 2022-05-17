/* ==================================================================
 * GlobalServiceControllerAdvice.java - 13/02/2017 10:25:07 AM
 * 
 * Copyright 2007-2017 SolarNetwork.net Dev Team
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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import net.solarnetwork.central.datum.imp.biz.DatumImportBiz;
import net.solarnetwork.central.user.event.biz.UserEventHookBiz;
import net.solarnetwork.central.user.expire.biz.UserExpireBiz;
import net.solarnetwork.central.user.export.biz.UserExportBiz;
import net.solarnetwork.central.user.ocpp.biz.UserOcppBiz;

/**
 * Add global services to all MVC controllers.
 * 
 * @author matt
 * @version 2.1
 * @since 1.26
 */
@ControllerAdvice(annotations = { GlobalServiceController.class })
public class GlobalServiceControllerAdvice {

	/**
	 * The model attribute name for the {@link UserExpireBiz}.
	 * 
	 * @since 1.1
	 */
	public static final String EXPIRE_BIZ_ATTRIBUTE = "expireBiz";

	/** The model attribute name for the {@link UserExportBiz}. */
	public static final String EXPORT_BIZ_ATTRIBUTE = "exportBiz";

	/**
	 * The model attribute name for the {@link DatumImportBiz}.
	 * 
	 * @since 1.2
	 */
	public static final String IMPORT_BIZ_ATTRIBUTE = "importBiz";

	/**
	 * The model attribute name for the {@link UserEventHookBiz}.
	 * 
	 * @since 1.3
	 */
	public static final String EVENT_HOOK_BIZ_ATTRIBUTE = "eventHookBiz";

	/**
	 * The model attribute name for the {@link UserOcppBiz}.
	 * 
	 * @since 2.1
	 */
	public static final String OCPP_BIZ_ATTRIBUTE = "ocppBiz";

	@Autowired(required = false)
	private UserExpireBiz expireBiz;

	@Autowired(required = false)
	private UserExportBiz exportBiz;

	@Autowired(required = false)
	private DatumImportBiz importBiz;

	@Autowired(required = false)
	private UserEventHookBiz eventHookBiz;

	@Autowired(required = false)
	private UserOcppBiz userOcppBiz;

	@ModelAttribute(value = EXPORT_BIZ_ATTRIBUTE)
	public UserExportBiz exportBiz() {
		return exportBiz;
	}

	/**
	 * The expire service.
	 * 
	 * @return the service
	 * @since 1.1
	 */
	@ModelAttribute(value = EXPIRE_BIZ_ATTRIBUTE)
	public UserExpireBiz expireBiz() {
		return expireBiz;
	}

	/**
	 * The import service.
	 * 
	 * @return the service
	 * @since 1.2
	 */
	@ModelAttribute(value = IMPORT_BIZ_ATTRIBUTE)
	public DatumImportBiz importBiz() {
		return importBiz;
	}

	/**
	 * The event hook service.
	 * 
	 * @return the service
	 * @since 1.3
	 */
	@ModelAttribute(value = EVENT_HOOK_BIZ_ATTRIBUTE)
	public UserEventHookBiz eventHookBiz() {
		return eventHookBiz;
	}

	/**
	 * The user OCPP service.
	 * 
	 * @return the service
	 * @since 2.1
	 */
	@ModelAttribute(value = OCPP_BIZ_ATTRIBUTE)
	public UserOcppBiz ocppBiz() {
		return userOcppBiz;
	}

}
