/* ==================================================================
 * DatumStreamController.java - 29/04/2022 10:44:01 AM
 * 
 * Copyright 2022 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.query.web.api;

import java.io.IOException;
import java.io.OutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.SmartValidator;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import net.solarnetwork.central.datum.domain.StreamDatumFilterCommand;
import net.solarnetwork.central.query.biz.QueryBiz;
import net.solarnetwork.central.web.GlobalExceptionRestController;

/**
 * Controller for querying datum stream related data.
 * 
 * @author matt
 * @version 1.0
 */
@Controller("v1DatumStreamController")
@RequestMapping({ "/api/v1/sec/datum/stream", "/api/v1/pub/datum/stream" })
@GlobalExceptionRestController
public class DatumStreamController {

	/** The {@code transientExceptionRetryCount} property default value. */
	public static final int DEFAULT_TRANSIENT_EXCEPTION_RETRY_COUNT = 1;

	/** The {@code transientExceptionRetryDelay} property default value. */
	public static final long DEFAULT_TRANSIENT_EXCEPTION_RETRY_DELAY = 2000L;

	private static final Logger log = LoggerFactory.getLogger(DatumController.class);

	private final QueryBiz queryBiz;
	private SmartValidator readingFilterValidator;

	/**
	 * Constructor.
	 * 
	 * @param queryBiz
	 *        the QueryBiz to use
	 */
	@Autowired
	public DatumStreamController(QueryBiz queryBiz) {
		this.queryBiz = queryBiz;
	}

	/**
	 * Query for a listing of datum.
	 * 
	 * @param cmd
	 * @return
	 */
	@ResponseBody
	@RequestMapping(value = "/list", method = RequestMethod.GET)
	public void filterGeneralDatumData(final StreamDatumFilterCommand cmd, OutputStream out)
			throws IOException {
		// TODO
	}

}
