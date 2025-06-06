/* ==================================================================
 * DatumImportController.java - 7/11/2018 6:57:54 AM
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

package net.solarnetwork.central.reg.web.api.v1;

import static java.util.Collections.singleton;
import static net.solarnetwork.domain.Result.error;
import static net.solarnetwork.domain.Result.success;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumComponents;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumPK;
import net.solarnetwork.central.datum.imp.biz.DatumImportBiz;
import net.solarnetwork.central.datum.imp.biz.DatumImportException;
import net.solarnetwork.central.datum.imp.biz.DatumImportInputFormatService;
import net.solarnetwork.central.datum.imp.biz.DatumImportValidationException;
import net.solarnetwork.central.datum.imp.domain.BasicConfiguration;
import net.solarnetwork.central.datum.imp.domain.BasicDatumImportPreviewRequest;
import net.solarnetwork.central.datum.imp.domain.BasicDatumImportRequest;
import net.solarnetwork.central.datum.imp.domain.DatumImportReceipt;
import net.solarnetwork.central.datum.imp.domain.DatumImportState;
import net.solarnetwork.central.datum.imp.domain.DatumImportStatus;
import net.solarnetwork.central.datum.imp.support.BasicDatumImportResource;
import net.solarnetwork.central.security.AuthorizationException;
import net.solarnetwork.central.security.SecurityUtils;
import net.solarnetwork.central.web.GlobalExceptionRestController;
import net.solarnetwork.dao.FilterResults;
import net.solarnetwork.domain.LocalizedServiceInfo;
import net.solarnetwork.domain.Result;
import net.solarnetwork.web.jakarta.support.MultipartFileResource;

/**
 * Web service API for datum import management.
 *
 * @author matt
 * @version 1.1
 * @since 1.33
 */
@GlobalExceptionRestController
@RestController("v1DatumImportController")
@RequestMapping(value = { "/u/sec/import", "/api/v1/sec/user/import" })
public class DatumImportController {

	private final Logger log = LoggerFactory.getLogger(DatumImportController.class);

	private final DatumImportBiz importBiz;

	/**
	 * Constructor.
	 *
	 * @param importBiz
	 *        the import biz to use
	 */
	public DatumImportController(@Autowired(required = false) DatumImportBiz importBiz) {
		super();
		this.importBiz = importBiz;
	}

	/**
	 * Handle an {@link DatumImportValidationException}.
	 *
	 * @param e
	 *        the exception
	 * @return an error response object
	 */
	@ExceptionHandler(DatumImportValidationException.class)
	@ResponseBody
	@ResponseStatus(code = HttpStatus.UNPROCESSABLE_ENTITY)
	public Result<?> handleDatumImportValidationException(DatumImportValidationException e) {
		log.debug("DatumImportValidationException in {} controller", getClass().getSimpleName(), e);
		return datumImportExceptionResponse(e, "DI.00400");
	}

	/**
	 * Handle an {@link DatumImportValidationException}.
	 *
	 * @param e
	 *        the exception
	 * @return an error response object
	 */
	@ExceptionHandler(DatumImportException.class)
	@ResponseBody
	public Result<?> handleDatumImportException(DatumImportException e) {
		log.debug("DatumImportException in {} controller", getClass().getSimpleName(), e);
		return datumImportExceptionResponse(e, "DI.00401");
	}

	private static Result<Object> datumImportExceptionResponse(DatumImportException e, String code) {
		Throwable cause = e;
		while ( cause.getCause() != null ) {
			cause = cause.getCause();
		}
		StringBuilder buf = new StringBuilder(e.getMessage());
		if ( cause != e ) {
			buf.append(" Root cause: ").append(cause);
		}
		Map<String, Object> data = new LinkedHashMap<>(4);
		if ( e.getLoadedCount() != null ) {
			data.put("loadedCount", e.getLoadedCount());
		}
		if ( e.getLineNumber() != null ) {
			data.put("lineNumber", e.getLineNumber());
		}
		if ( e.getLine() != null ) {
			data.put("line", e.getLine());
		}
		return new Result<>(Boolean.FALSE, code, buf.toString(), data);
	}

	/**
	 * Get a list of all available input format services.
	 *
	 * @param locale
	 *        the locale to use
	 * @return the services
	 */
	@ResponseBody
	@RequestMapping(value = "/services/input", method = RequestMethod.GET)
	public Result<List<LocalizedServiceInfo>> availableInputFormatServices(Locale locale) {
		List<LocalizedServiceInfo> result = null;
		if ( importBiz != null ) {
			Iterable<DatumImportInputFormatService> services = importBiz.availableInputFormatServices();
			result = new ArrayList<>();
			for ( DatumImportInputFormatService s : services ) {
				result.add(s.getLocalizedServiceInfo(locale));
			}
		}
		return success(result);
	}

	/**
	 * Upload a datum import configuration with associated data.
	 *
	 * <p>
	 * The actor must have an associated user ID as provided by
	 * {@link SecurityUtils#getCurrentActorUserId()}.
	 * </p>
	 *
	 * @param config
	 *        the import configuration
	 * @param data
	 *        the data to import
	 * @return a status entity
	 */
	@ResponseBody
	@RequestMapping(value = { "/jobs", "/jobs/" }, method = RequestMethod.POST,
			consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public Result<DatumImportReceipt> createJob(@RequestPart("config") BasicConfiguration config,
			@RequestPart("data") MultipartFile data) {
		DatumImportReceipt result = null;
		if ( importBiz != null ) {
			Long userId = SecurityUtils.getCurrentActorUserId();
			BasicDatumImportResource resource = new BasicDatumImportResource(
					new MultipartFileResource(data), data.getContentType());
			if ( config.getInputConfig() != null ) {
				config.getInputConfig().setUserId(userId);
			}
			BasicDatumImportRequest request = new BasicDatumImportRequest(config, userId);
			try {
				result = importBiz.submitDatumImportRequest(request, resource);
			} catch ( IOException e ) {
				throw new RuntimeException(e);
			}
		}
		return success(result);
	}

	/**
	 * Preview a previously staged import request.
	 *
	 * @param jobId
	 *        the ID of the staged import job to preview
	 * @param count
	 *        the maximum number of datum to preview
	 * @return an asynchronous result
	 */
	@ResponseBody
	@RequestMapping(value = "/jobs/{id}/preview", method = RequestMethod.GET)
	public Callable<Result<FilterResults<GeneralNodeDatumComponents, GeneralNodeDatumPK>>> previewStagedImport(
			@PathVariable("id") String jobId,
			@RequestParam(value = "count", required = false, defaultValue = "100") int count) {
		final Future<FilterResults<GeneralNodeDatumComponents, GeneralNodeDatumPK>> future;
		if ( importBiz != null ) {
			Long userId = SecurityUtils.getCurrentActorUserId();
			BasicDatumImportPreviewRequest req = new BasicDatumImportPreviewRequest(userId, jobId,
					count);
			future = importBiz.previewStagedImportRequest(req);
		} else {
			future = null;
		}
		// we have to wrap our FilterResults response with a Response; hence the Callable result here
		return () -> {
			if ( future == null ) {
				return error(null, "Import service not available");
			}
			try {
				FilterResults<GeneralNodeDatumComponents, GeneralNodeDatumPK> result = future.get();
				return success(result);
			} catch ( ExecutionException e ) {
				Throwable t = e.getCause();
				if ( t instanceof AuthorizationException ae ) {
					if ( ae.getId() instanceof Long ) {
						// treat as node ID, and re-throw as validation exception for preview
						throw new DatumImportValidationException(
								"Import not allowed for node " + ae.getId());
					}
				}
				if ( t instanceof Exception ex ) {
					throw ex;
				}
				throw e;
			}
		};
	}

	/**
	 * Get the status for a specific job.
	 *
	 * <p>
	 * The actor must have an associated user ID as provided by
	 * {@link SecurityUtils#getCurrentActorUserId()}.
	 * </p>
	 *
	 * @param states
	 *        an optional array of states to limit the results to, or
	 *        {@literal null} to include all states
	 * @return the statuses
	 */
	@ResponseBody
	@RequestMapping(value = "/jobs", method = RequestMethod.GET)
	public Result<Collection<DatumImportStatus>> jobStatusesForUser(
			@RequestParam(value = "states", required = false) DatumImportState[] states) {
		Collection<DatumImportStatus> result = null;
		if ( importBiz != null ) {
			Set<DatumImportState> stateFilter = null;
			if ( states != null && states.length > 0 ) {
				stateFilter = new HashSet<>(states.length);
				Collections.addAll(stateFilter, states);
				stateFilter = EnumSet.copyOf(stateFilter);
			}
			Long userId = SecurityUtils.getCurrentActorUserId();
			result = importBiz.datumImportJobStatusesForUser(userId, stateFilter);
		}
		return success(result);
	}

	/**
	 * Get the status for a specific job.
	 *
	 * <p>
	 * The actor must have an associated user ID as provided by
	 * {@link SecurityUtils#getCurrentActorUserId()}.
	 * </p>
	 *
	 * @param id
	 *        the ID of the job to get the status for
	 * @return the status
	 */
	@ResponseBody
	@RequestMapping(value = "/jobs/{id}", method = RequestMethod.GET)
	public Result<DatumImportStatus> jobStatus(@PathVariable("id") String id) {
		DatumImportStatus result = null;
		if ( importBiz != null ) {
			Long userId = SecurityUtils.getCurrentActorUserId();
			result = importBiz.datumImportJobStatusForUser(userId, id);
		}
		return success(result);
	}

	/**
	 * Change the state of a job from {@code Staged} to {@code Queued}.
	 *
	 * <p>
	 * The actor must have an associated user ID as provided by
	 * {@link SecurityUtils#getCurrentActorUserId()}.
	 * </p>
	 *
	 * @param id
	 *        the ID of the job to get the status for
	 * @return the status
	 */
	@ResponseBody
	@RequestMapping(value = "/jobs/{id}/confirm", method = RequestMethod.POST)
	public Result<DatumImportStatus> confirmStagedJob(@PathVariable("id") String id) {
		DatumImportStatus result = null;
		if ( importBiz != null ) {
			Long userId = SecurityUtils.getCurrentActorUserId();
			result = importBiz.updateDatumImportJobStateForUser(userId, id, DatumImportState.Queued,
					Collections.singleton(DatumImportState.Staged));
		}
		return success(result);
	}

	/**
	 * Update the configuration of a job.
	 *
	 * <p>
	 * The actor must have an associated user ID as provided by
	 * {@link SecurityUtils#getCurrentActorUserId()}.
	 * </p>
	 *
	 * @param id
	 *        the ID of the job to update
	 * @param config
	 *        the configuration to save with the job
	 * @return the status
	 */
	@ResponseBody
	@RequestMapping(value = "/jobs/{id}", method = RequestMethod.POST)
	public Result<DatumImportStatus> updateJob(@PathVariable("id") String id,
			@RequestBody BasicConfiguration config) {
		DatumImportStatus result = null;
		if ( importBiz != null ) {
			Long userId = SecurityUtils.getCurrentActorUserId();
			result = importBiz.updateDatumImportJobConfigurationForUser(userId, id, config);
		}
		return success(result);
	}

	/**
	 * Retract a job from executing.
	 *
	 * <p>
	 * The actor must have an associated user ID as provided by
	 * {@link SecurityUtils#getCurrentActorUserId()}.
	 * </p>
	 *
	 * @param id
	 *        the ID of the job to retract
	 * @return the status
	 */
	@ResponseBody
	@RequestMapping(value = "/jobs/{id}", method = RequestMethod.DELETE)
	public Result<DatumImportStatus> retractJob(@PathVariable("id") String id) {
		DatumImportStatus result = null;
		if ( importBiz != null ) {
			Long userId = SecurityUtils.getCurrentActorUserId();
			result = importBiz.updateDatumImportJobStateForUser(userId, id, DatumImportState.Retracted,
					EnumSet.of(DatumImportState.Staged, DatumImportState.Queued,
							DatumImportState.Claimed));
			if ( result != null ) {
				importBiz.deleteDatumImportJobsForUser(userId, singleton(id));
			}
		}
		return success(result);
	}
}
