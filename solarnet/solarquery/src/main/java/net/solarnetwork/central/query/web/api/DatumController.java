/* ==================================================================
 * DatumController.java - Mar 22, 2013 4:36:00 PM
 * 
 * Copyright 2007-2013 SolarNetwork.net Dev Team
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

import static net.solarnetwork.central.query.config.DatumQueryBizConfig.DATUM_FILTER;
import java.time.Period;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.validation.SmartValidator;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import net.solarnetwork.central.ValidationException;
import net.solarnetwork.central.datum.domain.DatumFilterCommand;
import net.solarnetwork.central.datum.domain.DatumReadingType;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumFilter;
import net.solarnetwork.central.domain.FilterResults;
import net.solarnetwork.central.query.biz.QueryBiz;
import net.solarnetwork.central.web.GlobalExceptionRestController;
import net.solarnetwork.domain.datum.Aggregation;
import net.solarnetwork.web.domain.Response;

/**
 * Controller for querying datum related data.
 * 
 * @author matt
 * @version 3.4
 */
@Controller("v1DatumController")
@RequestMapping({ "/api/v1/sec/datum", "/api/v1/pub/datum" })
@GlobalExceptionRestController
public class DatumController {

	/** The {@code transientExceptionRetryCount} property default value. */
	public static final int DEFAULT_TRANSIENT_EXCEPTION_RETRY_COUNT = 1;

	/** The {@code transientExceptionRetryDelay} property default value. */
	public static final long DEFAULT_TRANSIENT_EXCEPTION_RETRY_DELAY = 2000L;

	private static final Logger log = LoggerFactory.getLogger(DatumController.class);

	private final QueryBiz queryBiz;
	private SmartValidator filterValidator;

	private int transientExceptionRetryCount = DEFAULT_TRANSIENT_EXCEPTION_RETRY_COUNT;
	private long transientExceptionRetryDelay = DEFAULT_TRANSIENT_EXCEPTION_RETRY_DELAY;

	/**
	 * Constructor.
	 * 
	 * @param queryBiz
	 *        the QueryBiz to use
	 */
	public DatumController(QueryBiz queryBiz) {
		super();
		this.queryBiz = queryBiz;
	}

	@ResponseBody
	@RequestMapping(value = "/list", method = RequestMethod.GET, params = "!type")
	public Response<FilterResults<?>> filterGeneralDatumData(final DatumFilterCommand cmd,
			BindingResult validationResult) {
		if ( filterValidator != null ) {
			filterValidator.validate(cmd, validationResult);
			if ( validationResult.hasErrors() ) {
				throw new ValidationException(validationResult);
			}
		}
		int retries = transientExceptionRetryCount;
		while ( true ) {
			try {
				FilterResults<?> results;
				if ( cmd.getAggregation() != null ) {
					results = queryBiz.findFilteredAggregateGeneralNodeDatum(cmd,
							cmd.getSortDescriptors(), cmd.getOffset(), cmd.getMax());
				} else {
					results = queryBiz.findFilteredGeneralNodeDatum(cmd, cmd.getSortDescriptors(),
							cmd.getOffset(), cmd.getMax());
				}
				return new Response<FilterResults<?>>(results);
			} catch ( TransientDataAccessException | DataAccessResourceFailureException e ) {
				if ( retries > 0 ) {
					log.warn(
							"Transient {} exception in /list request {}, will retry up to {} more times after a delay of {}ms: {}",
							e.getClass().getSimpleName(), cmd, retries, transientExceptionRetryDelay,
							e.toString());
					if ( transientExceptionRetryDelay > 0 ) {
						try {
							Thread.sleep(transientExceptionRetryDelay);
						} catch ( InterruptedException e2 ) {
							// ignore
						}
					}
				} else {
					throw e;
				}
			}
			retries--;
		}
	}

	@ResponseBody
	@RequestMapping(value = "/mostRecent", method = RequestMethod.GET, params = "!type")
	public Response<FilterResults<?>> getMostRecentGeneralNodeDatumData(final DatumFilterCommand cmd,
			BindingResult validationResult) {
		cmd.setMostRecent(true);
		return filterGeneralDatumData(cmd, validationResult);
	}

	/**
	 * Query for reading datum.
	 * 
	 * @param cmd
	 *        the filter
	 * @param readingType
	 *        the reading type
	 * @param tolerance
	 *        the query tolerance
	 * @param validationResult
	 *        a result for validations
	 * @return the results
	 * @since 2.3
	 */
	@ResponseBody
	@RequestMapping(value = "/reading", method = RequestMethod.GET)
	public Response<FilterResults<?>> datumReading(final DatumFilterCommand cmd,
			@RequestParam("readingType") DatumReadingType readingType,
			@RequestParam(value = "tolerance", required = false, defaultValue = "P1M") Period tolerance,
			BindingResult validationResult) {
		if ( filterValidator != null ) {
			filterValidator.validate(cmd, validationResult, readingType, tolerance);
			if ( validationResult.hasErrors() ) {
				throw new ValidationException(validationResult);
			}
		}
		int retries = transientExceptionRetryCount;
		while ( true ) {
			try {
				FilterResults<?> results;
				if ( cmd.getAggregation() != null && cmd.getAggregation() != Aggregation.None ) {
					results = queryBiz.findFilteredAggregateReading(cmd, readingType, tolerance,
							cmd.getSortDescriptors(), cmd.getOffset(), cmd.getMax());
				} else {
					results = queryBiz.findFilteredReading(cmd, readingType, tolerance);
				}
				return new Response<FilterResults<?>>(results);
			} catch ( TransientDataAccessException | DataAccessResourceFailureException e ) {
				if ( retries > 0 ) {
					log.warn(
							"Transient {} exception in /reading request {}, will retry up to {} more times after a delay of {}ms: {}",
							e.getClass().getSimpleName(), cmd, retries, transientExceptionRetryDelay,
							e.toString());
					if ( transientExceptionRetryDelay > 0 ) {
						try {
							Thread.sleep(transientExceptionRetryDelay);
						} catch ( InterruptedException e2 ) {
							// ignore
						}
					}
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
	 * @since 2.6
	 */
	public int getTransientExceptionRetryCount() {
		return transientExceptionRetryCount;
	}

	/**
	 * Set the number of retry attempts for transient DAO exceptions.
	 * 
	 * @param transientExceptionRetryCount
	 *        the retry count, or {@literal 0} for no retries
	 * @since 2.6
	 */
	public void setTransientExceptionRetryCount(int transientExceptionRetryCount) {
		this.transientExceptionRetryCount = transientExceptionRetryCount;
	}

	/**
	 * Get the filter validator to use.
	 * 
	 * @return the validator
	 * @since 2.9
	 */
	public SmartValidator getFilterValidator() {
		return filterValidator;
	}

	/**
	 * Set the filter validator to use.
	 * 
	 * @param filterValidator
	 *        the valiadtor to set
	 * @throws IllegalArgumentException
	 *         if {@code validator} does not support the
	 *         {@link GeneralNodeDatumFilter} class
	 * @since 3.3
	 */
	@Autowired
	@Qualifier(DATUM_FILTER)
	public void setFilterValidator(SmartValidator filterValidator) {
		if ( filterValidator != null && !filterValidator.supports(GeneralNodeDatumFilter.class) ) {
			throw new IllegalArgumentException(
					"The Validator must support the GeneralNodeDatumFilter class.");
		}
		this.filterValidator = filterValidator;
	}

	/**
	 * Get the length of time, in milliseconds, to sleep before retrying a
	 * request after a transient exception.
	 * 
	 * @return the delay, in milliseconds; defaults to
	 *         {@link #DEFAULT_TRANSIENT_EXCEPTION_RETRY_DELAY}
	 * @since 3.1
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
	 * @since 3.1
	 */
	public void setTransientExceptionRetryDelay(long transientExceptionRetryDelay) {
		this.transientExceptionRetryDelay = transientExceptionRetryDelay;
	}

}
