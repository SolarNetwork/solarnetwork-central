/* ==================================================================
 * DatumExpireController.java - 10/07/2018 12:00:38 PM
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

import static net.solarnetwork.domain.Result.error;
import static net.solarnetwork.domain.Result.success;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import net.solarnetwork.central.datum.domain.DatumFilterCommand;
import net.solarnetwork.central.datum.domain.DatumRecordCounts;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumId;
import net.solarnetwork.central.reg.web.domain.DatumExpireFullConfigurations;
import net.solarnetwork.central.security.SecurityUtils;
import net.solarnetwork.central.user.expire.biz.UserDatumDeleteBiz;
import net.solarnetwork.central.user.expire.biz.UserExpireBiz;
import net.solarnetwork.central.user.expire.domain.DataConfiguration;
import net.solarnetwork.central.user.expire.domain.DatumDeleteJobInfo;
import net.solarnetwork.central.user.expire.domain.DatumDeleteJobState;
import net.solarnetwork.central.user.expire.domain.ExpireUserDataConfiguration;
import net.solarnetwork.central.web.GlobalExceptionRestController;
import net.solarnetwork.domain.LocalizedServiceInfo;
import net.solarnetwork.domain.Result;

/**
 * Web service API for datum expire management.
 *
 * @author matt
 * @version 2.2
 * @since 1.29
 */
@GlobalExceptionRestController
@RestController("v1DatumExpireController")
@RequestMapping(value = { "/u/sec/expire", "/api/v1/sec/user/expire" })
public class DatumExpireController {

	private final UserExpireBiz expireBiz;
	private final UserDatumDeleteBiz datumDeleteBiz;

	/**
	 * Constructor.
	 *
	 * @param expireBiz
	 *        the expire service to use
	 * @param datumDeleteBiz
	 *        the datum delete service to use
	 */
	public DatumExpireController(@Autowired(required = false) UserExpireBiz expireBiz,
			@Autowired(required = false) UserDatumDeleteBiz datumDeleteBiz) {
		super();
		this.expireBiz = expireBiz;
		this.datumDeleteBiz = datumDeleteBiz;
	}

	@ResponseBody
	@RequestMapping(value = "/services/aggregation", method = RequestMethod.GET)
	public Result<Iterable<LocalizedServiceInfo>> availableAggregationServices(Locale locale) {
		Iterable<LocalizedServiceInfo> result = null;
		if ( expireBiz != null ) {
			result = expireBiz.availableAggregationTypes(locale);
		}
		return success(result);
	}

	@ResponseBody
	@RequestMapping(value = "/configs", method = RequestMethod.GET)
	public Result<DatumExpireFullConfigurations> viewDataConfigurations() {
		final Long userId = SecurityUtils.getCurrentActorUserId();

		List<ExpireUserDataConfiguration> dataConfigs = Collections.emptyList();
		if ( expireBiz != null ) {
			dataConfigs = expireBiz.configurationsForUser(userId, ExpireUserDataConfiguration.class);
		}

		return success(new DatumExpireFullConfigurations(dataConfigs));
	}

	@ResponseBody
	@RequestMapping(value = "/configs/data", method = RequestMethod.POST)
	public Result<DataConfiguration> saveDataConfiguration(
			@RequestBody ExpireUserDataConfiguration config) {
		if ( expireBiz != null ) {
			if ( config.getUserId() == null ) {
				config.setUserId(SecurityUtils.getCurrentActorUserId());
			}
			if ( config.getCreated() == null ) {
				config.setCreated(Instant.now());
			}
			Long id = expireBiz.saveConfiguration(config);
			if ( id != null ) {
				config.setId(id);
				return success(config);
			}
		}
		return error();
	}

	@ResponseBody
	@RequestMapping(value = "/configs/data/{id}", method = RequestMethod.DELETE)
	public Result<Void> deleteDataConfiguration(@PathVariable("id") Long id) {
		if ( expireBiz != null ) {
			Long userId = SecurityUtils.getCurrentActorUserId();
			ExpireUserDataConfiguration config = expireBiz.configurationForUser(userId,
					ExpireUserDataConfiguration.class, id);
			if ( config != null ) {
				expireBiz.deleteConfiguration(config);
			}
		}
		return success();
	}

	@ResponseBody
	@RequestMapping(value = "/configs/data/{id}/preview", method = RequestMethod.GET)
	public Result<DatumRecordCounts> previewDataConfiguration(@PathVariable("id") Long id) {
		DatumRecordCounts counts = null;
		if ( expireBiz != null ) {
			Long userId = SecurityUtils.getCurrentActorUserId();
			ExpireUserDataConfiguration config = expireBiz.configurationForUser(userId,
					ExpireUserDataConfiguration.class, id);
			if ( config != null ) {
				counts = expireBiz.countExpiredDataForConfiguration(config);
			}
		}
		return success(counts);
	}

	@ResponseBody
	@RequestMapping(value = "/datum-delete", method = RequestMethod.POST)
	public Result<DatumRecordCounts> previewDataDelete(DatumFilterCommand filter) {
		DatumRecordCounts counts = null;
		if ( datumDeleteBiz != null ) {
			Long userId = SecurityUtils.getCurrentActorUserId();
			filter.setUserId(userId);
			counts = datumDeleteBiz.countDatumRecords(filter);
		}
		return success(counts);
	}

	@ResponseBody
	@RequestMapping(value = "/datum-delete/confirm", method = RequestMethod.POST)
	public Result<DatumDeleteJobInfo> confirmDataDelete(DatumFilterCommand filter) {
		DatumDeleteJobInfo result = null;
		if ( datumDeleteBiz != null ) {
			Long userId = SecurityUtils.getCurrentActorUserId();
			filter.setUserId(userId);
			result = datumDeleteBiz.submitDatumDeleteRequest(filter);
		}
		return success(result);
	}

	@ResponseBody
	@RequestMapping(value = "/datum-delete/jobs", method = RequestMethod.GET)
	public Result<Collection<DatumDeleteJobInfo>> jobsForUser(
			@RequestParam(value = "states", required = false) DatumDeleteJobState[] states) {
		Collection<DatumDeleteJobInfo> result = null;
		if ( datumDeleteBiz != null ) {
			Set<DatumDeleteJobState> stateFilter = null;
			if ( states != null && states.length > 0 ) {
				stateFilter = new HashSet<>(states.length);
				Collections.addAll(stateFilter, states);
				stateFilter = EnumSet.copyOf(stateFilter);
			}
			Long userId = SecurityUtils.getCurrentActorUserId();
			result = datumDeleteBiz.datumDeleteJobsForUser(userId, stateFilter);
		}
		return success(result);
	}

	@ResponseBody
	@RequestMapping(value = "/datum-delete/jobs/{id}", method = RequestMethod.GET)
	public Result<DatumDeleteJobInfo> jobStatus(@PathVariable("id") String id) {
		DatumDeleteJobInfo result = null;
		if ( datumDeleteBiz != null ) {
			Long userId = SecurityUtils.getCurrentActorUserId();
			result = datumDeleteBiz.datumDeleteJobForUser(userId, id);
		}
		return success(result);
	}

	/**
	 * Delete datum matching a set of IDs.
	 *
	 * @param ids
	 *        the IDs to match
	 * @return the IDds of the datum that were deleted
	 */
	@ResponseBody
	@RequestMapping(value = "/datum-delete/ids", method = RequestMethod.POST)
	public Result<Set<ObjectDatumId>> confirmDataDelete(@RequestBody Set<ObjectDatumId> ids) {
		Set<ObjectDatumId> result = null;
		if ( datumDeleteBiz != null ) {
			Long userId = SecurityUtils.getCurrentActorUserId();
			result = datumDeleteBiz.deleteDatum(userId, ids);
		}
		return success(result);
	}

}
