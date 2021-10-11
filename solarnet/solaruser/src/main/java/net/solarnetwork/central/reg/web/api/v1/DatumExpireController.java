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

import static net.solarnetwork.web.domain.Response.response;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import net.solarnetwork.central.datum.domain.DatumFilterCommand;
import net.solarnetwork.central.datum.domain.DatumRecordCounts;
import net.solarnetwork.central.reg.web.domain.DatumExpireFullConfigurations;
import net.solarnetwork.central.security.SecurityUtils;
import net.solarnetwork.central.user.expire.biz.UserDatumDeleteBiz;
import net.solarnetwork.central.user.expire.biz.UserExpireBiz;
import net.solarnetwork.central.user.expire.domain.DataConfiguration;
import net.solarnetwork.central.user.expire.domain.DatumDeleteJobInfo;
import net.solarnetwork.central.user.expire.domain.DatumDeleteJobState;
import net.solarnetwork.central.user.expire.domain.UserDataConfiguration;
import net.solarnetwork.central.web.GlobalExceptionRestController;
import net.solarnetwork.domain.LocalizedServiceInfo;
import net.solarnetwork.service.OptionalService;
import net.solarnetwork.web.domain.Response;

/**
 * Web service API for datum expire management.
 * 
 * @author matt
 * @version 2.0
 * @since 1.29
 */
@GlobalExceptionRestController
@RestController("v1DatumExpireController")
@RequestMapping(value = { "/sec/expire", "/v1/sec/user/expire" })
public class DatumExpireController {

	private final OptionalService<UserExpireBiz> expireBiz;
	private final OptionalService<UserDatumDeleteBiz> datumDeleteBiz;

	// FIXME: private String[] requestDateFormats = new String[] { DEFAULT_DATE_TIME_FORMAT, ALT_DATE_TIME_FORMAT,
	//		ALT_TIMESTAMP_FORMAT, DEFAULT_DATE_FORMAT };

	/**
	 * Constructor.
	 * 
	 * @param expireBiz
	 *        the expire service to use
	 * @param datumDeleteBiz
	 *        the datum delete service to use
	 */
	@Autowired
	public DatumExpireController(@Qualifier("expireBiz") OptionalService<UserExpireBiz> expireBiz,
			@Qualifier("datumDeleteBiz") OptionalService<UserDatumDeleteBiz> datumDeleteBiz) {
		super();
		this.expireBiz = expireBiz;
		this.datumDeleteBiz = datumDeleteBiz;
	}

	/**
	 * Web binder initialization.
	 * 
	 * @param binder
	 *        the binder to initialize
	 */
	@InitBinder
	public void initBinder(WebDataBinder binder) {
		/*
		 * FIXME: binder.registerCustomEditor(DateTime.class, new
		 * JodaDateFormatEditor(this.requestDateFormats,
		 * TimeZone.getTimeZone("UTC")));
		 * binder.registerCustomEditor(LocalDateTime.class, new
		 * JodaDateFormatEditor(this.requestDateFormats, null,
		 * ParseMode.LocalDateTime));
		 */
	}

	public void setRequestDateFormats(String[] requestDateFormats) {
		// FIXME: this.requestDateFormats = requestDateFormats;
	}

	@ResponseBody
	@RequestMapping(value = "/services/aggregation", method = RequestMethod.GET)
	public Response<Iterable<LocalizedServiceInfo>> availableAggregationServices(Locale locale) {
		final UserExpireBiz biz = expireBiz.service();
		Iterable<LocalizedServiceInfo> result = null;
		if ( biz != null ) {
			result = biz.availableAggregationTypes(locale);
		}
		return response(result);
	}

	@ResponseBody
	@RequestMapping(value = "/configs", method = RequestMethod.GET)
	public Response<DatumExpireFullConfigurations> viewDataConfigurations() {
		final UserExpireBiz biz = expireBiz.service();
		final Long userId = SecurityUtils.getCurrentActorUserId();

		List<UserDataConfiguration> dataConfigs = Collections.emptyList();
		if ( biz != null ) {
			dataConfigs = biz.configurationsForUser(userId, UserDataConfiguration.class);
		}

		return response(new DatumExpireFullConfigurations(dataConfigs));
	}

	@ResponseBody
	@RequestMapping(value = "/configs/data", method = RequestMethod.POST)
	public Response<DataConfiguration> saveDataConfiguration(@RequestBody UserDataConfiguration config) {
		final UserExpireBiz biz = expireBiz.service();
		if ( biz != null ) {
			if ( config.getUserId() == null ) {
				config.setUserId(SecurityUtils.getCurrentActorUserId());
			}
			if ( config.getCreated() == null ) {
				config.setCreated(Instant.now());
			}
			Long id = biz.saveConfiguration(config);
			if ( id != null ) {
				config.setId(id);
				return response(config);
			}
		}
		return new Response<DataConfiguration>(false, null, null, null);
	}

	@ResponseBody
	@RequestMapping(value = "/configs/data/{id}", method = RequestMethod.DELETE)
	public Response<Void> deleteDataConfiguration(@PathVariable("id") Long id) {
		final UserExpireBiz biz = expireBiz.service();
		if ( biz != null ) {
			Long userId = SecurityUtils.getCurrentActorUserId();
			UserDataConfiguration config = biz.configurationForUser(userId, UserDataConfiguration.class,
					id);
			if ( config != null ) {
				biz.deleteConfiguration(config);
			}
		}
		return response(null);
	}

	@ResponseBody
	@RequestMapping(value = "/configs/data/{id}/preview", method = RequestMethod.GET)
	public Response<DatumRecordCounts> previewDataConfiguration(@PathVariable("id") Long id) {
		final UserExpireBiz biz = expireBiz.service();
		DatumRecordCounts counts = null;
		if ( biz != null ) {
			Long userId = SecurityUtils.getCurrentActorUserId();
			UserDataConfiguration config = biz.configurationForUser(userId, UserDataConfiguration.class,
					id);
			if ( config != null ) {
				counts = biz.countExpiredDataForConfiguration(config);
			}
		}
		return response(counts);
	}

	@ResponseBody
	@RequestMapping(value = "/datum-delete", method = RequestMethod.POST)
	public Response<DatumRecordCounts> previewDataDelete(DatumFilterCommand filter) {
		final UserDatumDeleteBiz biz = datumDeleteBiz.service();
		DatumRecordCounts counts = null;
		if ( biz != null ) {
			Long userId = SecurityUtils.getCurrentActorUserId();
			filter.setUserIds(new Long[] { userId });
			counts = biz.countDatumRecords(filter);
		}
		return response(counts);
	}

	@ResponseBody
	@RequestMapping(value = "/datum-delete/confirm", method = RequestMethod.POST)
	public Response<DatumDeleteJobInfo> confirmDataDelete(DatumFilterCommand filter) {
		final UserDatumDeleteBiz biz = datumDeleteBiz.service();
		DatumDeleteJobInfo result = null;
		if ( biz != null ) {
			Long userId = SecurityUtils.getCurrentActorUserId();
			filter.setUserIds(new Long[] { userId });
			result = biz.submitDatumDeleteRequest(filter);
		}
		return response(result);
	}

	@ResponseBody
	@RequestMapping(value = "/datum-delete/jobs", method = RequestMethod.GET)
	public Response<Collection<DatumDeleteJobInfo>> jobsForUser(
			@RequestParam(value = "states", required = false) DatumDeleteJobState[] states) {
		final UserDatumDeleteBiz biz = datumDeleteBiz.service();
		Collection<DatumDeleteJobInfo> result = null;
		if ( biz != null ) {
			Set<DatumDeleteJobState> stateFilter = null;
			if ( states != null && states.length > 0 ) {
				stateFilter = new HashSet<>(states.length);
				for ( DatumDeleteJobState state : states ) {
					stateFilter.add(state);
				}
				stateFilter = EnumSet.copyOf(stateFilter);
			}
			Long userId = SecurityUtils.getCurrentActorUserId();
			result = biz.datumDeleteJobsForUser(userId, stateFilter);
		}
		return response(result);
	}

	@ResponseBody
	@RequestMapping(value = "/datum-delete/jobs/{id}", method = RequestMethod.GET)
	public Response<DatumDeleteJobInfo> jobStatus(@PathVariable("id") String id) {
		final UserDatumDeleteBiz biz = datumDeleteBiz.service();
		DatumDeleteJobInfo result = null;
		if ( biz != null ) {
			Long userId = SecurityUtils.getCurrentActorUserId();
			result = biz.datumDeleteJobForUser(userId, id);
		}
		return response(result);
	}

}
