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
import static net.solarnetwork.domain.Result.success;
import java.time.Duration;
import java.time.Instant;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.validation.SmartValidator;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.Explode;
import io.swagger.v3.oas.annotations.enums.ParameterStyle;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import net.solarnetwork.central.ValidationException;
import net.solarnetwork.central.datum.domain.AggregateGeneralNodeDatumFilter;
import net.solarnetwork.central.datum.domain.DatumFilterCommand;
import net.solarnetwork.central.datum.domain.DatumReadingType;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumFilter;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumFilterMatch;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumPK;
import net.solarnetwork.central.datum.domain.ReportingGeneralNodeDatumMatch;
import net.solarnetwork.central.query.biz.QueryBiz;
import net.solarnetwork.central.web.BaseTransientDataAccessRetryController;
import net.solarnetwork.central.web.GlobalExceptionRestController;
import net.solarnetwork.central.web.WebUtils;
import net.solarnetwork.dao.FilterResults;
import net.solarnetwork.domain.Result;
import net.solarnetwork.domain.datum.Aggregation;

/**
 * Controller for querying datum related data.
 *
 * @author matt
 * @version 3.6
 */
@Controller("v1DatumController")
@RequestMapping({ "/api/v1/sec/datum", "/api/v1/pub/datum" })
@Tag(name = "datum", description = "Methods to query datum streams.")
@GlobalExceptionRestController
public class DatumController extends BaseTransientDataAccessRetryController {

	/** The {@code mostRecentStartPeriod} property default value. */
	public static final Duration DEFAULT_MOST_RECENT_START_PERIOD = Duration.ofDays(90);

	private final QueryBiz queryBiz;
	private SmartValidator filterValidator;

	private Duration mostRecentStartPeriod = DEFAULT_MOST_RECENT_START_PERIOD;

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

	private void populateMostRecentImplicitStartDate(final DatumFilterCommand cmd) {
		if ( mostRecentStartPeriod != null && cmd.isMostRecent() && cmd.getStartDate() == null
				&& cmd.getLocalStartDate() == null ) {
			// add implicit start date, to speed up query
			cmd.setStartDate(Instant.now().truncatedTo(ChronoUnit.DAYS)
					.minusSeconds(mostRecentStartPeriod.getSeconds()));
		}
	}

	@Operation(operationId = "datumList", summary = "List node datum matching filter criteria",
			description = "Query for node datum that match criteria like node ID, source ID, and date range.",
			parameters = @Parameter(name = "criteria", description = """
					The search criteria. A maximum result count will be enforced.
					""", schema = @Schema(implementation = AggregateGeneralNodeDatumFilter.class),
					style = ParameterStyle.FORM, explode = Explode.TRUE))
	@ResponseBody
	@RequestMapping(value = "/list", method = RequestMethod.GET, params = "!type")
	public Result<FilterResults<? extends GeneralNodeDatumFilterMatch, GeneralNodeDatumPK>> filterGeneralDatumData(
			final HttpServletRequest req, final DatumFilterCommand criteria,
			BindingResult validationResult) {
		if ( filterValidator != null ) {
			filterValidator.validate(criteria, validationResult);
			if ( validationResult.hasErrors() ) {
				throw new ValidationException(validationResult);
			}
		}
		populateMostRecentImplicitStartDate(criteria);
		return WebUtils.doWithTransientDataAccessExceptionRetry(() -> {
			FilterResults<? extends GeneralNodeDatumFilterMatch, GeneralNodeDatumPK> results;
			if ( criteria.getAggregation() != null ) {
				results = queryBiz.findFilteredAggregateGeneralNodeDatum(criteria,
						criteria.getSortDescriptors(), criteria.getOffset(), criteria.getMax());
			} else {
				results = queryBiz.findFilteredGeneralNodeDatum(criteria, criteria.getSortDescriptors(),
						criteria.getOffset(), criteria.getMax());
			}
			return success(results);
		}, req, getTransientExceptionRetryCount(), getTransientExceptionRetryDelay(), log);
	}

	@Operation(operationId = "datumListMostRecent",
			summary = "List the most recently posted node datum matching filter criteria",
			description = """
					Query for node datum that match criteria like node ID, source ID, and date range.
					The most recently posted datum for each matching source will be returned.
					""",
			parameters = @Parameter(name = "criteria", description = """
					The search criteria. A maximum result count will be enforced.
					""", schema = @Schema(implementation = AggregateGeneralNodeDatumFilter.class),
					style = ParameterStyle.FORM, explode = Explode.TRUE))
	@ResponseBody
	@RequestMapping(value = "/mostRecent", method = RequestMethod.GET, params = "!type")
	public Result<FilterResults<? extends GeneralNodeDatumFilterMatch, GeneralNodeDatumPK>> getMostRecentGeneralNodeDatumData(
			final HttpServletRequest req, final DatumFilterCommand criteria,
			BindingResult validationResult) {
		criteria.setMostRecent(true);
		return filterGeneralDatumData(req, criteria, validationResult);
	}

	/**
	 * Query for reading datum.
	 * 
	 * @param criteria
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
	@Operation(operationId = "readingDatumList",
			summary = "List node reading style datum matching filter criteria",
			description = """
					Query for node datum that match criteria like node ID, source ID, and date range, returning
					reading style results.
					""",
			parameters = { @Parameter(name = "criteria", description = """
					The search criteria. A maximum result count will be enforced.
					""", schema = @Schema(implementation = AggregateGeneralNodeDatumFilter.class),
					style = ParameterStyle.FORM, explode = Explode.TRUE),
					@Parameter(name = "readingType", description = """
							The desired reading type.
							"""), @Parameter(name = "tolerance", description = """
							The reading tolerance, used by some reading types.
							"""), })
	@ResponseBody
	@RequestMapping(value = "/reading", method = RequestMethod.GET)
	public Result<FilterResults<ReportingGeneralNodeDatumMatch, GeneralNodeDatumPK>> datumReading(
			final HttpServletRequest req, final DatumFilterCommand criteria,
			@RequestParam("readingType") DatumReadingType readingType,
			@RequestParam(value = "tolerance", required = false, defaultValue = "P1M") Period tolerance,
			BindingResult validationResult) {
		if ( filterValidator != null ) {
			filterValidator.validate(criteria, validationResult, readingType, tolerance);
			if ( validationResult.hasErrors() ) {
				throw new ValidationException(validationResult);
			}
		}
		return WebUtils.doWithTransientDataAccessExceptionRetry(() -> {
			FilterResults<ReportingGeneralNodeDatumMatch, GeneralNodeDatumPK> results;
			if ( criteria.getAggregation() != null && criteria.getAggregation() != Aggregation.None ) {
				results = queryBiz.findFilteredAggregateReading(criteria, readingType, tolerance,
						criteria.getSortDescriptors(), criteria.getOffset(), criteria.getMax());
			} else {
				results = queryBiz.findFilteredReading(criteria, readingType, tolerance);
			}
			return success(results);
		}, req, getTransientExceptionRetryCount(), getTransientExceptionRetryDelay(), log);
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
	 *        the validator to set
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
	 * Get the length of time to use to determine an implicit start date in most
	 * recent queries.
	 *
	 * @return the mostRecentStartPeriod the duration
	 * @since 3.5
	 */
	public Duration getMostRecentStartPeriod() {
		return mostRecentStartPeriod;
	}

	/**
	 * Set the length of time to use to determine an implicit start date in most
	 * recent queries.
	 *
	 * @param mostRecentStartPeriod
	 *        the period to set
	 * @since 3.5
	 */
	public void setMostRecentStartPeriod(Duration mostRecentStartPeriod) {
		this.mostRecentStartPeriod = mostRecentStartPeriod;
	}

}
