/* ==================================================================
 * DatumStreamMetadataController.java - 22/11/2021 7:26:19 AM
 * 
 * Copyright 2021 SolarNetwork.net Dev Team
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

import static net.solarnetwork.domain.Result.result;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import net.solarnetwork.central.datum.biz.DatumStreamMetadataBiz;
import net.solarnetwork.central.datum.v2.dao.BasicDatumCriteria;
import net.solarnetwork.central.datum.v2.dao.ObjectStreamCriteria;
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadata;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumStreamMetadataId;
import net.solarnetwork.central.security.SecurityActor;
import net.solarnetwork.central.security.SecurityUtils;
import net.solarnetwork.central.web.GlobalExceptionRestController;
import net.solarnetwork.domain.Result;
import net.solarnetwork.domain.datum.ObjectDatumKind;
import net.solarnetwork.util.ObjectUtils;

/**
 * Controller for accessing datum stream metadata.
 * 
 * @author matt
 * @version 1.0
 */
@GlobalExceptionRestController
@Controller("v1DatumStreamMetadataController")
@RequestMapping(value = "/api/v1/sec/datum/stream/meta")
public class DatumStreamMetadataController {

	private final DatumStreamMetadataBiz datumStreamMetadataBiz;

	/**
	 * Constructor.
	 * 
	 * @param datumStreamMetadataBiz
	 *        the stream metadata biz to use
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public DatumStreamMetadataController(DatumStreamMetadataBiz datumStreamMetadataBiz) {
		super();
		this.datumStreamMetadataBiz = ObjectUtils.requireNonNullArgument(datumStreamMetadataBiz,
				"datumStreamMetadataBiz");
	}

	@InitBinder
	public void initBinder(WebDataBinder binder) {
		binder.setIgnoreInvalidFields(true);
	}

	/**
	 * Find node datum stream metadata.
	 * 
	 * @param criteria
	 *        any search criteria, supporting only
	 *        {@link net.solarnetwork.central.datum.v2.dao.ObjectStreamCriteria}
	 *        properties
	 * @return the results, never {@literal null}
	 */
	@ResponseBody
	@RequestMapping(value = { "/node" }, method = RequestMethod.GET)
	public Result<List<ObjectDatumStreamMetadata>> findNodeMetadata(BasicDatumCriteria criteria) {
		criteria.setObjectKind(ObjectDatumKind.Node);
		return result(findMetadata(criteria));
	}

	/**
	 * Find location datum stream metadata IDs.
	 * 
	 * @param criteria
	 *        any search criteria, supporting only
	 *        {@link net.solarnetwork.central.datum.v2.dao.ObjectStreamCriteria}
	 *        properties
	 * @return the results, never {@literal null}
	 */
	@ResponseBody
	@RequestMapping(value = { "/loc" }, method = RequestMethod.GET)
	public Result<List<ObjectDatumStreamMetadata>> findLocationMetadata(BasicDatumCriteria criteria) {
		criteria.setObjectKind(ObjectDatumKind.Location);
		return result(findMetadata(criteria));
	}

	private List<ObjectDatumStreamMetadata> findMetadata(ObjectStreamCriteria criteria) {
		SecurityActor actor = SecurityUtils.getCurrentActor();
		return datumStreamMetadataBiz.findDatumStreamMetadata(actor, criteria);
	}

	/**
	 * Find node datum stream metadata IDs.
	 * 
	 * @param criteria
	 *        any search criteria, supporting only
	 *        {@link net.solarnetwork.central.datum.v2.dao.ObjectStreamCriteria}
	 *        properties
	 * @return the results, never {@literal null}
	 */
	@ResponseBody
	@RequestMapping(value = { "/node/ids" }, method = RequestMethod.GET)
	public Result<List<ObjectDatumStreamMetadataId>> findNodeMetadataIds(BasicDatumCriteria criteria) {
		criteria.setObjectKind(ObjectDatumKind.Node);
		return result(findMetadataIds(criteria));
	}

	/**
	 * Find location datum stream metadata IDs.
	 * 
	 * @param criteria
	 *        any search criteria, supporting only
	 *        {@link net.solarnetwork.central.datum.v2.dao.ObjectStreamCriteria}
	 *        properties
	 * @return the results, never {@literal null}
	 */
	@ResponseBody
	@RequestMapping(value = { "/loc/ids" }, method = RequestMethod.GET)
	public Result<List<ObjectDatumStreamMetadataId>> findLocationMetadataIds(
			BasicDatumCriteria criteria) {
		criteria.setObjectKind(ObjectDatumKind.Location);
		return result(findMetadataIds(criteria));
	}

	private List<ObjectDatumStreamMetadataId> findMetadataIds(ObjectStreamCriteria criteria) {
		SecurityActor actor = SecurityUtils.getCurrentActor();
		return datumStreamMetadataBiz.findDatumStreamMetadataIds(actor, criteria);
	}

}
