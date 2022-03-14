/* ==================================================================
 * NodeMetadataController.java - 14/11/2016 8:48:21 AM
 * 
 * Copyright 2007-2016 SolarNetwork.net Dev Team
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

import static net.solarnetwork.central.security.SecurityUtils.authorizedNodeIdsForCurrentActor;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import static net.solarnetwork.web.domain.Response.response;
import java.util.NoSuchElementException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import net.solarnetwork.central.biz.SolarNodeMetadataBiz;
import net.solarnetwork.central.dao.SolarNodeOwnershipDao;
import net.solarnetwork.central.datum.domain.DatumFilterCommand;
import net.solarnetwork.central.domain.FilterResults;
import net.solarnetwork.central.domain.SolarNodeMetadataFilterMatch;
import net.solarnetwork.central.web.GlobalExceptionRestController;
import net.solarnetwork.web.domain.Response;

/**
 * Controller for read-only node metadata access.
 * 
 * @author matt
 * @version 2.0
 */
@Controller("v1NodeMetadataController")
@RequestMapping({ "/api/v1/pub/nodes/meta", "/api/v1/sec/nodes/meta" })
@GlobalExceptionRestController
public class NodeMetadataController {

	/** The {@code transientExceptionRetryCount} property default value. */
	public static final int DEFAULT_TRANSIENT_EXCEPTION_RETRY_COUNT = 1;

	private static final Logger log = LoggerFactory.getLogger(NodeMetadataController.class);

	private final SolarNodeOwnershipDao nodeOwnershipDao;
	private final SolarNodeMetadataBiz solarNodeMetadataBiz;
	private int transientExceptionRetryCount = DEFAULT_TRANSIENT_EXCEPTION_RETRY_COUNT;

	/**
	 * Constructor.
	 * 
	 * @param nodeOwnershipDao
	 *        the node ownership DAO to use
	 * @param solarNodeMetadataBiz
	 *        the SolarNodeMetadataBiz to use
	 */
	@Autowired
	public NodeMetadataController(SolarNodeOwnershipDao nodeOwnershipDao,
			SolarNodeMetadataBiz solarNodeMetadataBiz) {
		super();
		this.nodeOwnershipDao = requireNonNullArgument(nodeOwnershipDao, "nodeOwnershipDao");
		this.solarNodeMetadataBiz = requireNonNullArgument(solarNodeMetadataBiz, "solarNodeMetadataBiz");
	}

	@InitBinder
	public void initBinder(WebDataBinder binder) {
		binder.setIgnoreInvalidFields(true);
	}

	/**
	 * Find all metadata for any number of node IDs.
	 * 
	 * @param criteria
	 *        any sort or limit criteria
	 * @return the results
	 */
	@ResponseBody
	@RequestMapping(value = { "", "/" }, method = RequestMethod.GET)
	public Response<FilterResults<SolarNodeMetadataFilterMatch>> findMetadata(
			DatumFilterCommand criteria) {
		int retries = transientExceptionRetryCount;
		while ( true ) {
			try {
				if ( criteria.getNodeId() == null ) {
					// default to all nodes for actor
					criteria.setNodeIds(authorizedNodeIdsForCurrentActor(nodeOwnershipDao));
				}
				FilterResults<SolarNodeMetadataFilterMatch> results = solarNodeMetadataBiz
						.findSolarNodeMetadata(criteria, criteria.getSortDescriptors(),
								criteria.getOffset(), criteria.getMax());
				return response(results);
			} catch ( TransientDataAccessException | DataAccessResourceFailureException e ) {
				if ( retries > 0 ) {
					log.warn("Transient {} exception, will retry up to {} more times.",
							e.getClass().getSimpleName(), retries, e);
				} else {
					throw e;
				}
			}
			retries--;
		}
	}

	/**
	 * Find all metadata for a specific node ID.
	 * 
	 * @param nodeId
	 *        the node ID to find
	 * @return the results
	 */
	@ResponseBody
	@RequestMapping(value = { "/{nodeId}" }, method = RequestMethod.GET)
	public Response<SolarNodeMetadataFilterMatch> getMetadata(@PathVariable("nodeId") Long nodeId) {
		int retries = transientExceptionRetryCount;
		while ( true ) {
			try {
				DatumFilterCommand criteria = new DatumFilterCommand();
				criteria.setNodeId(nodeId);
				FilterResults<SolarNodeMetadataFilterMatch> results = solarNodeMetadataBiz
						.findSolarNodeMetadata(criteria, null, null, null);
				SolarNodeMetadataFilterMatch result = null;
				if ( results != null ) {
					try {
						result = results.iterator().next();
					} catch ( NoSuchElementException e ) {
						// ignore
					}
				}
				return response(result);
			} catch ( TransientDataAccessException | DataAccessResourceFailureException e ) {
				if ( retries > 0 ) {
					log.warn("Transient {} exception, will retry up to {} more times.",
							e.getClass().getSimpleName(), retries, e);
				} else {
					throw e;
				}
			}
			retries--;
		}
	}

	/**
	 * Get the number of retry attempts for transient DAO exceptions.
	 * 
	 * @return the retry count; defaults to
	 *         {@link #DEFAULT_TRANSIENT_EXCEPTION_RETRY_COUNT}.
	 * @since 1.1
	 */
	public int getTransientExceptionRetryCount() {
		return transientExceptionRetryCount;
	}

	/**
	 * Set the number of retry attempts for transient DAO exceptions.
	 * 
	 * @param transientExceptionRetryCount
	 *        the retry count, or {@literal 0} for no retries
	 * @since 1.1
	 */
	public void setTransientExceptionRetryCount(int transientExceptionRetryCount) {
		this.transientExceptionRetryCount = transientExceptionRetryCount;
	}
}
