/* ==================================================================
 * DatumMetadataController.java - Oct 3, 2014 2:13:51 PM
 * 
 * Copyright 2007-2014 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.in.web.api;

import static net.solarnetwork.web.domain.Response.response;
import net.solarnetwork.central.datum.domain.DatumFilterCommand;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumMetadataFilterMatch;
import net.solarnetwork.central.domain.FilterResults;
import net.solarnetwork.central.in.biz.DataCollectorBiz;
import net.solarnetwork.central.web.support.WebServiceControllerSupport;
import net.solarnetwork.domain.GeneralDatumMetadata;
import net.solarnetwork.web.domain.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Controller for datum metadata actions.
 * 
 * @author matt
 * @version 1.0
 */
@Controller("v1DatumMetadataController")
@RequestMapping({ "/api/v1/pub/datum/meta/{nodeId}", "/api/v1/sec/datum/meta/{nodeId}" })
public class DatumMetadataController extends WebServiceControllerSupport {

	private final DataCollectorBiz dataCollectorBiz;

	/**
	 * Constructor.
	 * 
	 * @param dataCollectorBiz
	 *        the DataCollectorBiz to use
	 */
	@Autowired
	public DatumMetadataController(DataCollectorBiz dataCollectorBiz) {
		super();
		this.dataCollectorBiz = dataCollectorBiz;
	}

	@InitBinder
	public void initBinder(WebDataBinder binder) {
		binder.setIgnoreInvalidFields(true);
	}

	@ResponseBody
	@RequestMapping(value = { "", "/" }, method = RequestMethod.GET)
	public Response<FilterResults<GeneralNodeDatumMetadataFilterMatch>> findMetadata(
			@PathVariable("nodeId") Long nodeId, DatumFilterCommand criteria) {
		return findMetadata(nodeId, null, criteria);
	}

	@ResponseBody
	@RequestMapping(value = { "/{sourceId}" }, method = RequestMethod.GET)
	public Response<FilterResults<GeneralNodeDatumMetadataFilterMatch>> findMetadata(
			@PathVariable("nodeId") Long nodeId, @PathVariable("sourceId") String sourceId,
			DatumFilterCommand criteria) {
		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setNodeId(nodeId);
		filter.setSourceId(sourceId);
		FilterResults<GeneralNodeDatumMetadataFilterMatch> results = dataCollectorBiz
				.findGeneralNodeDatumMetadata(filter, criteria.getSortDescriptors(),
						criteria.getOffset(), criteria.getMax());
		return response(results);
	}

	@ResponseBody
	@RequestMapping(value = { "", "/" }, method = RequestMethod.GET, params = { "sourceId" })
	public Response<FilterResults<GeneralNodeDatumMetadataFilterMatch>> findMetadataAlt(
			@PathVariable("nodeId") Long nodeId, @RequestParam("sourceId") String sourceId,
			DatumFilterCommand criteria) {
		return findMetadata(nodeId, sourceId, criteria);
	}

	@ResponseBody
	@RequestMapping(value = { "/{sourceId}" }, method = RequestMethod.POST)
	public Response<Object> addMetadata(@PathVariable("nodeId") Long nodeId,
			@PathVariable("sourceId") String sourceId, @RequestBody GeneralDatumMetadata meta) {
		dataCollectorBiz.addGeneralNodeDatumMetadata(nodeId, sourceId, meta);
		return response(null);
	}

	@ResponseBody
	@RequestMapping(value = { "", "/" }, method = RequestMethod.POST, params = { "sourceId" })
	public Response<Object> addMetadataAlt(@PathVariable("nodeId") Long nodeId,
			@RequestParam("sourceId") String sourceId, @RequestBody GeneralDatumMetadata meta) {
		return addMetadata(nodeId, sourceId, meta);
	}

}
