/* ==================================================================
 * NodesController.java - 28/05/2018 2:34:24 PM
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

package net.solarnetwork.central.query.web.api;

import static net.solarnetwork.domain.Result.success;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import net.solarnetwork.central.datum.domain.DatumFilterCommand;
import net.solarnetwork.central.datum.domain.NodeSourcePK;
import net.solarnetwork.central.query.biz.QueryBiz;
import net.solarnetwork.central.security.SecurityActor;
import net.solarnetwork.central.security.SecurityUtils;
import net.solarnetwork.central.web.GlobalExceptionRestController;
import net.solarnetwork.domain.Result;

/**
 * Controller for querying node related data.
 * 
 * @author matt
 * @version 1.1
 */
@Controller("v1NodesController")
@RequestMapping("/api/v1/sec/nodes")
@GlobalExceptionRestController
public class NodesController {

	/** The {@code transientExceptionRetryCount} property default value. */
	public static final int DEFAULT_TRANSIENT_EXCEPTION_RETRY_COUNT = 1;

	/** The {@code transientExceptionRetryDelay} property default value. */
	public static final long DEFAULT_TRANSIENT_EXCEPTION_RETRY_DELAY = 2000L;

	private final QueryBiz queryBiz;

	private static final Logger log = LoggerFactory.getLogger(NodesController.class);

	private int transientExceptionRetryCount = DEFAULT_TRANSIENT_EXCEPTION_RETRY_COUNT;
	private long transientExceptionRetryDelay = DEFAULT_TRANSIENT_EXCEPTION_RETRY_DELAY;

	/**
	 * Constructor.
	 * 
	 * @param queryBiz
	 *        the QueryBiz to use
	 */
	@Autowired
	public NodesController(QueryBiz queryBiz) {
		super();
		this.queryBiz = queryBiz;
	}

	/**
	 * Get a listing of nodes for the active user.
	 * 
	 * @return The list of nodes available to the active user.
	 */
	@RequestMapping(value = { "", "/" }, method = RequestMethod.GET)
	@ResponseBody
	public Result<Set<Long>> getAvailableNodes() {
		SecurityActor actor = SecurityUtils.getCurrentActor();
		int retries = transientExceptionRetryCount;
		while ( true ) {
			try {
				Set<Long> result = queryBiz.findAvailableNodes(actor);
				return success(result);
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
	 * Find available node sources that match a search criteria.
	 * 
	 * @param filter
	 *        the search criteria
	 * @return the matching set of node sources
	 * @since 1.1
	 */
	@ResponseBody
	@RequestMapping(value = "/sources", method = RequestMethod.GET)
	public Result<Set<NodeSourcePK>> findAvailableSources(DatumFilterCommand filter) {
		SecurityActor actor = SecurityUtils.getCurrentActor();
		int retries = transientExceptionRetryCount;
		while ( true ) {
			try {
				Set<NodeSourcePK> result = queryBiz.findAvailableSources(actor, filter);
				return success(result);
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

	/**
	 * Get the length of time, in milliseconds, to sleep before retrying a
	 * request after a transient exception.
	 * 
	 * @return the delay, in milliseconds; defaults to
	 *         {@link #DEFAULT_TRANSIENT_EXCEPTION_RETRY_DELAY}
	 * @since 1.1
	 */
	public long getTransientExceptionRetryDelay() {
		return transientExceptionRetryDelay;
	}

	/**
	 * Set the length of time, in milliseconds, to sleep before retrying a
	 * request after a transient exception.
	 * 
	 * @param transientExceptionRetryDelay
	 *        the delay to set
	 * @since 1.1
	 */
	public void setTransientExceptionRetryDelay(long transientExceptionRetryDelay) {
		this.transientExceptionRetryDelay = transientExceptionRetryDelay;
	}

}
