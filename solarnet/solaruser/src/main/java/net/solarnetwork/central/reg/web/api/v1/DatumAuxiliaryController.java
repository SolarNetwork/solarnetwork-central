/* ==================================================================
 * DatumAuxiliaryController.java - 6/02/2019 4:00:49 pm
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

import static net.solarnetwork.web.jakarta.domain.Response.response;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import net.solarnetwork.central.datum.biz.DatumAuxiliaryBiz;
import net.solarnetwork.central.datum.domain.DatumAuxiliaryType;
import net.solarnetwork.central.datum.domain.DatumFilterCommand;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumAuxiliary;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumAuxiliaryFilterMatch;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumAuxiliaryPK;
import net.solarnetwork.central.domain.FilterResults;
import net.solarnetwork.central.reg.web.domain.DatumAuxiliaryMove;
import net.solarnetwork.central.security.SecurityUtils;
import net.solarnetwork.central.web.GlobalExceptionRestController;
import net.solarnetwork.web.jakarta.domain.Response;

/**
 * Web controller for datum auxiliary record management.
 * 
 * @author matt
 * @version 2.0
 * @since 1.35
 */
@GlobalExceptionRestController
@RestController("v1DatumAuxiliaryController")
@RequestMapping(value = { "/u/sec/datum/auxiliary", "/api/v1/sec/datum/auxiliary" })
public class DatumAuxiliaryController {

	private final DatumAuxiliaryBiz datumAuxiliaryBiz;

	/**
	 * Constructor.
	 * 
	 * @param datumAuxiliaryBiz
	 *        the biz to use
	 */
	@Autowired
	public DatumAuxiliaryController(DatumAuxiliaryBiz datumAuxiliaryBiz) {
		super();
		this.datumAuxiliaryBiz = datumAuxiliaryBiz;
	}

	/**
	 * Get a specific auxiliary record.
	 * 
	 * @param type
	 *        the type
	 * @param nodeId
	 *        the node ID
	 * @param date
	 *        the date
	 * @param sourceId
	 *        the source ID
	 * @return empty response
	 */
	@ResponseBody
	@RequestMapping(value = "/{type}/{node}/{date}/{source}", method = RequestMethod.GET)
	public Response<GeneralNodeDatumAuxiliary> viewNodeDatumAuxiliary(
			@PathVariable("type") DatumAuxiliaryType type, @PathVariable("node") Long nodeId,
			@PathVariable("date") Instant date, @PathVariable("source") String sourceId) {
		GeneralNodeDatumAuxiliary aux = datumAuxiliaryBiz.getGeneralNodeDatumAuxiliary(
				new GeneralNodeDatumAuxiliaryPK(nodeId, date, sourceId, type));
		return response(aux);
	}

	/**
	 * View a specific auxiliary record via web post.
	 * 
	 * @param type
	 *        the type
	 * @param nodeId
	 *        the node ID
	 * @param date
	 *        the date
	 * @param sourceId
	 *        the source ID
	 * @return empty response
	 */
	@ResponseBody
	@RequestMapping(value = { "", "/" }, method = RequestMethod.GET, params = "date")
	public Response<GeneralNodeDatumAuxiliary> viewNodeDatumAuxiliaryFormPost(
			@RequestParam(value = "type", required = false, defaultValue = "Reset") DatumAuxiliaryType type,
			@RequestParam("nodeId") Long nodeId, @RequestParam("date") Instant date,
			@RequestParam("sourceId") String sourceId) {
		return viewNodeDatumAuxiliary(type, nodeId, date, sourceId);
	}

	/**
	 * Find auxiliary records matching a search filter.
	 * 
	 * @param criteria
	 *        the search criteria
	 * @return the results
	 */
	@ResponseBody
	@RequestMapping(value = { "", "," }, method = RequestMethod.GET, params = "!date")
	public Response<FilterResults<GeneralNodeDatumAuxiliaryFilterMatch>> findNodeDatumAuxiliary(
			DatumFilterCommand criteria) {
		Long userId = SecurityUtils.getCurrentActorUserId();
		criteria.setUserId(userId);
		FilterResults<GeneralNodeDatumAuxiliaryFilterMatch> results = datumAuxiliaryBiz
				.findGeneralNodeDatumAuxiliary(criteria, criteria.getSortDescriptors(),
						criteria.getOffset(), criteria.getMax());
		return response(results);
	}

	/**
	 * Store (add/update) a datum auxiliary record.
	 * 
	 * @param auxiliary
	 *        the record to add
	 * @return empty result
	 */
	@ResponseBody
	@RequestMapping(value = { "", "/" }, method = RequestMethod.POST, consumes = "!"
			+ MediaType.APPLICATION_FORM_URLENCODED_VALUE)
	public Response<Void> storeNodeDatumAuxiliary(@RequestBody GeneralNodeDatumAuxiliary auxiliary) {
		datumAuxiliaryBiz.storeGeneralNodeDatumAuxiliary(auxiliary);
		return response(null);
	}

	/**
	 * Store (add/update) a datum auxiliary record via a web form post.
	 * 
	 * @param auxiliary
	 *        the record to add
	 * @return empty result
	 */
	@ResponseBody
	@RequestMapping(value = { "",
			"/" }, method = RequestMethod.POST, consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
	public Response<Void> storeNodeDatumAuxiliaryFormPost(GeneralNodeDatumAuxiliary auxiliary) {
		return storeNodeDatumAuxiliary(auxiliary);
	}

	/**
	 * Move (delete/insert) a datum auxiliary record.
	 * 
	 * @param move
	 *        the record to move
	 * @return empty result
	 */
	@ResponseBody
	@RequestMapping(value = "/move", method = RequestMethod.POST, consumes = "!"
			+ MediaType.APPLICATION_FORM_URLENCODED_VALUE)
	public Response<Boolean> moveNodeDatumAuxiliary(@RequestBody DatumAuxiliaryMove move) {
		boolean moved = datumAuxiliaryBiz.moveGeneralNodeDatumAuxiliary(move.getFrom(), move.getTo());
		return response(moved);
	}

	/**
	 * Move (delete/insert) a datum auxiliary record via a web form post.
	 * 
	 * @param move
	 *        the record to move
	 * @return empty result
	 */
	@ResponseBody
	@RequestMapping(value = "/move", method = RequestMethod.POST, consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
	public Response<Boolean> moveNodeDatumAuxiliaryFormPost(DatumAuxiliaryMove move) {
		return moveNodeDatumAuxiliary(move);
	}

	/**
	 * Remove a specific auxiliary record.
	 * 
	 * @param type
	 *        the type
	 * @param nodeId
	 *        the node ID
	 * @param date
	 *        the date
	 * @param sourceId
	 *        the source ID
	 * @return empty response
	 */
	@ResponseBody
	@RequestMapping(value = "/{type}/{node}/{date}/{source}", method = RequestMethod.DELETE)
	public Response<Void> removeNodeDatumAuxiliary(@PathVariable("type") DatumAuxiliaryType type,
			@PathVariable("node") Long nodeId, @PathVariable("date") Instant date,
			@PathVariable("source") String sourceId) {
		datumAuxiliaryBiz.removeGeneralNodeDatumAuxiliary(
				new GeneralNodeDatumAuxiliaryPK(nodeId, date, sourceId, type));
		return response(null);
	}

	/**
	 * Remove a datum auxiliary record.
	 * 
	 * @param id
	 *        the ID of the record to delete
	 * @return empty result
	 */
	@ResponseBody
	@RequestMapping(value = { "", "/" }, method = RequestMethod.DELETE, params = { "!nodeId", "!date",
			"!sourceId" })
	public Response<Void> removeNodeDatumAuxiliary(@RequestBody GeneralNodeDatumAuxiliaryPK id) {
		datumAuxiliaryBiz.removeGeneralNodeDatumAuxiliary(id);
		return response(null);
	}

	/**
	 * Remove a specific auxiliary record via web post.
	 * 
	 * @param type
	 *        the type
	 * @param nodeId
	 *        the node ID
	 * @param date
	 *        the date
	 * @param sourceId
	 *        the source ID
	 * @return empty response
	 */
	@ResponseBody
	@RequestMapping(value = { "", "/" }, method = RequestMethod.DELETE, params = { "nodeId", "date",
			"sourceId" })
	public Response<Void> removeNodeDatumAuxiliaryFormPost(
			@RequestParam(value = "type", required = false, defaultValue = "Reset") DatumAuxiliaryType type,
			@RequestParam("nodeId") Long nodeId, @RequestParam("date") Instant date,
			@RequestParam("sourceId") String sourceId) {
		return removeNodeDatumAuxiliary(type, nodeId, date, sourceId);
	}

}
