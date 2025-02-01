/* ==================================================================
 * DatumMaintenanceController.java - 10/04/2019 11:54:16 am
 *
 * Copyright 2019 SolarNetwork.net Dev Team
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import net.solarnetwork.central.datum.biz.DatumMaintenanceBiz;
import net.solarnetwork.central.datum.domain.DatumFilterCommand;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumKindPK;
import net.solarnetwork.central.datum.domain.StaleAggregateDatum;
import net.solarnetwork.central.web.GlobalExceptionRestController;
import net.solarnetwork.dao.FilterResults;
import net.solarnetwork.domain.Result;

/**
 * Web controller for datum maintenance functions.
 *
 * @author matt
 * @version 2.0
 * @since 1.39
 */
@GlobalExceptionRestController
@RestController("v1DatumMaintenanceController")
@RequestMapping(value = { "/u/sec/datum/maint", "/api/v1/sec/datum/maint" })
public class DatumMaintenanceController {

	private final DatumMaintenanceBiz datumMaintenanceBiz;

	/**
	 * Constructor.
	 *
	 * @param datumMaintenanceBiz
	 *        the biz to use
	 */
	@Autowired
	public DatumMaintenanceController(DatumMaintenanceBiz datumMaintenanceBiz) {
		super();
		this.datumMaintenanceBiz = datumMaintenanceBiz;
	}

	/**
	 * Find datum aggregates maarked as "stale".
	 *
	 * <p>
	 * The following criteria should be specified at a minimum:
	 * </p>
	 *
	 * <ul>
	 * <li>node ID(s)</li>
	 * <li>source ID(s)</li>
	 * <li>start date (inclusive)</li>
	 * <li>end date (exclusive)</li>
	 * </ul>
	 *
	 * @param criteria
	 *        the datum criteria to mark
	 * @return empty response
	 * @since 1.1
	 */
	@ResponseBody
	@RequestMapping(value = "/agg/stale", method = RequestMethod.GET)
	public Result<FilterResults<StaleAggregateDatum, GeneralNodeDatumKindPK>> findStaleAggregatesDatum(
			DatumFilterCommand criteria) {
		FilterResults<StaleAggregateDatum, GeneralNodeDatumKindPK> results = datumMaintenanceBiz
				.findStaleAggregateDatum(criteria, criteria.getSortDescriptors(), criteria.getOffset(),
						criteria.getMax());
		return success(results);
	}

	/**
	 * Mark a set of datum aggregates as "stale" so they are re-computed.
	 *
	 * <p>
	 * The following criteria should be specified at a minimum:
	 * </p>
	 *
	 * <ul>
	 * <li>node ID(s)</li>
	 * <li>source ID(s)</li>
	 * <li>start date (inclusive)</li>
	 * <li>end date (exclusive)</li>
	 * </ul>
	 *
	 * @param criteria
	 *        the datum criteria to mark
	 * @return empty response
	 */
	@ResponseBody
	@RequestMapping(value = { "/agg/stale", "/agg/mark-stale" }, method = RequestMethod.POST)
	public Result<Void> markDatumAggregatesStale(DatumFilterCommand criteria) {
		datumMaintenanceBiz.markDatumAggregatesStale(criteria);
		return success();
	}
}
