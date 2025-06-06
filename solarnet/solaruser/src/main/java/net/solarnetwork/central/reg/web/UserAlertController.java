/* ==================================================================
 * UserAlertController.java - 19/05/2015 7:35:10 pm
 *
 * Copyright 2007-2015 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.reg.web;

import static net.solarnetwork.domain.Result.success;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import net.solarnetwork.central.datum.biz.DatumMetadataBiz;
import net.solarnetwork.central.datum.v2.dao.BasicDatumCriteria;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumStreamMetadataId;
import net.solarnetwork.central.security.SecurityUser;
import net.solarnetwork.central.security.SecurityUtils;
import net.solarnetwork.central.user.biz.UserAlertBiz;
import net.solarnetwork.central.user.biz.UserBiz;
import net.solarnetwork.central.user.domain.UserAlert;
import net.solarnetwork.central.user.domain.UserAlertOptions;
import net.solarnetwork.central.user.domain.UserAlertSituationStatus;
import net.solarnetwork.central.user.domain.UserAlertStatus;
import net.solarnetwork.central.user.domain.UserAlertType;
import net.solarnetwork.domain.Result;
import net.solarnetwork.util.StringUtils;

/**
 * Controller for user alerts.
 *
 * @author matt
 * @version 2.3
 */
@GlobalServiceController
@RequestMapping("/u/sec/alerts")
public class UserAlertController extends ControllerSupport {

	private static final Pattern TIME_PAT = Pattern.compile("[0-2]?\\d:[0-5]\\d");

	private final UserBiz userBiz;
	private final UserAlertBiz userAlertBiz;
	private final DatumMetadataBiz datumMetadataBiz;

	@Autowired
	public UserAlertController(UserBiz userBiz, UserAlertBiz userAlertBiz,
			DatumMetadataBiz datumMetadataBiz) {
		super();
		this.userBiz = requireNonNullArgument(userBiz, "userBiz");
		this.userAlertBiz = requireNonNullArgument(userAlertBiz, "userAlertBiz");
		this.datumMetadataBiz = requireNonNullArgument(datumMetadataBiz, "datumMetadataBiz");
	}

	@ModelAttribute("nodeDataAlertTypes")
	public List<UserAlertType> nodeDataAlertTypes() {
		// now, only one alert type!
		return Collections.singletonList(UserAlertType.NodeStaleData);
	}

	@ModelAttribute("alertStatuses")
	public UserAlertStatus[] alertStatuses() {
		return UserAlertStatus.values();
	}

	/**
	 * View the main Alerts screen.
	 *
	 * @param model
	 *        The model object.
	 * @return The view name.
	 */
	@RequestMapping(value = "", method = RequestMethod.GET)
	public String view(Model model) {
		final SecurityUser user = SecurityUtils.getCurrentUser();
		List<UserAlert> alerts = userAlertBiz.userAlertsForUser(user.getUserId());
		if ( alerts != null ) {
			List<UserAlert> nodeDataAlerts = new ArrayList<>(alerts.size());
			for ( UserAlert alert : alerts ) {
				if ( alert.getType() == UserAlertType.NodeStaleData ) {
					nodeDataAlerts.add(alert);
				}
			}
			model.addAttribute("nodeDataAlerts", nodeDataAlerts);
		}
		model.addAttribute("userNodes", userBiz.getUserNodes(user.getUserId()));
		return "sec/alerts/view-alerts";
	}

	/**
	 * Get all available sources for a given node ID.
	 *
	 * @param nodeId
	 *        The ID of the node to get all available sources for.
	 * @param start
	 *        An optional start date to limit the query to.
	 * @param end
	 *        An optional end date to limit the query to.
	 * @return The found sources.
	 */
	@RequestMapping(value = "/node/{nodeId}/sources", method = RequestMethod.GET)
	@ResponseBody
	public Result<List<String>> availableSourcesForNode(@PathVariable("nodeId") Long nodeId,
			@RequestParam(value = "start", required = false) Instant start,
			@RequestParam(value = "end", required = false) Instant end) {
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setNodeId(nodeId);
		filter.setStartDate(start);
		filter.setEndDate(end);
		Set<ObjectDatumStreamMetadataId> data = datumMetadataBiz.findDatumStreamMetadataIds(filter);
		List<String> sourceIds = data.stream().map(ObjectDatumStreamMetadataId::getSourceId)
				.collect(Collectors.toList());
		return success(sourceIds);
	}

	/**
	 * Get <em>active</em> situations for a given node.
	 *
	 * @param nodeId
	 *        The ID of the node to get the situations for.
	 * @param locale
	 *        The request locale.
	 * @return The alerts with active situations.
	 * @since 1.1
	 */
	@RequestMapping(value = "/node/{nodeId}/situations", method = RequestMethod.GET)
	@ResponseBody
	public Result<List<UserAlert>> activeSituationsNode(@PathVariable("nodeId") Long nodeId,
			Locale locale) {
		List<UserAlert> results = userAlertBiz.alertSituationsForNode(nodeId);
		for ( UserAlert alert : results ) {
			populateUsefulAlertOptions(alert, locale);
		}
		return success(results);
	}

	/**
	 * Get a count of <em>active</em> situations for the active user.
	 *
	 * @return The count.
	 * @since 1.1
	 */
	@RequestMapping(value = "/user/situation/count", method = RequestMethod.GET)
	@ResponseBody
	public Result<Integer> activeSituationCount() {
		Long userId = SecurityUtils.getCurrentActorUserId();
		Integer count = userAlertBiz.alertSituationCountForUser(userId);
		return success(count);
	}

	/**
	 * Get <em>active</em> situations for the active user
	 *
	 * @param locale
	 *        The request locale.
	 * @return The alerts with active situations.
	 * @since 1.1
	 */
	@RequestMapping(value = "/user/situations", method = RequestMethod.GET)
	@ResponseBody
	public Result<List<UserAlert>> activeSituations(Locale locale) {
		Long userId = SecurityUtils.getCurrentActorUserId();
		List<UserAlert> results = userAlertBiz.alertSituationsForUser(userId);
		for ( UserAlert alert : results ) {
			populateUsefulAlertOptions(alert, locale);
		}
		return success(results);
	}

	/**
	 * Create or update an alert.
	 *
	 * @param model
	 *        The UserAlert details.
	 * @return The saved details.
	 */
	@RequestMapping(value = "/save", method = RequestMethod.POST)
	@ResponseBody
	public Result<UserAlert> addAlert(@RequestBody UserAlert model) {
		final SecurityUser user = SecurityUtils.getCurrentUser();
		UserAlert alert = new UserAlert();
		alert.setId(model.getId());
		alert.setNodeId(model.getNodeId());
		alert.setUserId(user.getUserId());
		alert.setCreated(Instant.now());
		alert.setStatus(model.getStatus() == null ? UserAlertStatus.Active : model.getStatus());
		alert.setType(model.getType() == null ? UserAlertType.NodeStaleData : model.getType());

		// reset validTo date to now, so alert re-processed
		alert.setValidTo(Instant.now());

		Map<String, Object> options = new HashMap<>();
		if ( model.getOptions() != null ) {
			for ( Map.Entry<String, Object> me : model.getOptions().entrySet() ) {
				if ( "ageMinutes".equalsIgnoreCase(me.getKey()) ) {
					// convert ageMinutes to age (seconds)
					Object v = model.getOptions().get("ageMinutes");
					double minutes = 1;
					try {
						minutes = Double.parseDouble(v.toString());
					} catch ( NumberFormatException e ) {
						// ignore
						log.warn("Alert option ageMinutes is not a number, setting to 1: [{}]", v);
					}
					options.put(UserAlertOptions.AGE_THRESHOLD, Math.round(minutes * 60.0));
				} else if ( UserAlertOptions.EMAIL_TOS.equalsIgnoreCase(me.getKey()) ) {
					Object val = me.getValue();
					if ( val instanceof String[] a ) {
						options.put(UserAlertOptions.EMAIL_TOS, Arrays.asList(a));
					} else if ( val instanceof List<?> l ) {
						options.put(UserAlertOptions.EMAIL_TOS, l);
					} else {
						Set<String> emails = StringUtils.commaDelimitedStringToSet(val.toString());
						if ( emails != null ) {
							options.put(UserAlertOptions.EMAIL_TOS, new ArrayList<>(emails));
						}
					}
				} else if ( "sources".equalsIgnoreCase(me.getKey()) && me.getValue() != null ) {
					// convert sources to List of String
					Set<String> sources = StringUtils
							.commaDelimitedStringToSet(me.getValue().toString());
					if ( sources != null ) {
						List<String> sourceList = new ArrayList<>(sources);
						options.put(UserAlertOptions.SOURCE_IDS, sourceList);
					}
				} else if ( "windows".equalsIgnoreCase(me.getKey())
						&& me.getValue() instanceof Collection ) {
					@SuppressWarnings("unchecked")
					Collection<Map<String, ?>> windows = (Collection<Map<String, ?>>) me.getValue();
					List<Map<String, Object>> windowsList = new ArrayList<>();
					for ( Map<String, ?> window : windows ) {
						Object timeStart = window.get("timeStart");
						Object timeEnd = window.get("timeEnd");

						if ( timeStart != null && timeEnd != null ) {
							String ts = timeStart.toString();
							String te = timeEnd.toString();
							if ( TIME_PAT.matcher(ts).matches() && TIME_PAT.matcher(te).matches() ) {
								Map<String, Object> win = new LinkedHashMap<>(2);
								win.put("timeStart", ts);
								win.put("timeEnd", te);
								windowsList.add(win);
							}
						}
					}
					if ( !windowsList.isEmpty() ) {
						options.put(UserAlertOptions.TIME_WINDOWS, windowsList);
					}
				}
			}
		}
		if ( !options.isEmpty() ) {
			alert.setOptions(options);
		}

		Long id = userAlertBiz.saveAlert(alert);
		alert.setId(id);
		return success(alert);
	}

	private void populateUsefulAlertOptions(UserAlert alert, Locale locale) {
		if ( alert == null ) {
			return;
		}
		// to aid UI, populate some useful display properties
		if ( alert.getSituation() != null ) {
			DateTimeFormatter fmt = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.LONG,
					FormatStyle.SHORT);
			if ( locale != null ) {
				fmt = fmt.withLocale(locale);
			}
			alert.getOptions().put("situationDate",
					fmt.format(alert.getSituation().getCreated().atOffset(ZoneOffset.UTC)));
			if ( alert.getSituation().getNotified() != null ) {
				alert.getOptions().put("situationNotificationDate",
						fmt.format(alert.getSituation().getNotified().atOffset(ZoneOffset.UTC)));
			}
		}
		if ( alert.getOptions() != null ) {
			Map<String, Object> options = alert.getOptions();
			if ( options.containsKey(UserAlertOptions.SOURCE_IDS) ) {
				@SuppressWarnings("unchecked")
				Collection<String> sources = (Collection<String>) options
						.get(UserAlertOptions.SOURCE_IDS);
				options.put("sources", StringUtils.commaDelimitedStringFromCollection(sources));
			}
		}
	}

	/**
	 * View an alert with the most recent active situation populated.
	 *
	 * @param alertId
	 *        The ID of the alert to view.
	 * @param locale
	 *        The request locale.
	 * @return The alert.
	 */
	@RequestMapping(value = "/situation/{alertId}", method = RequestMethod.GET)
	@ResponseBody
	public Result<UserAlert> viewSituation(@PathVariable("alertId") Long alertId, Locale locale) {
		UserAlert alert = userAlertBiz.alertSituation(alertId);
		populateUsefulAlertOptions(alert, locale);
		return success(alert);
	}

	/**
	 * Update an active alert situation's status.
	 *
	 * @param alertId
	 *        The ID of the alert with the active situation.
	 * @param status
	 *        The situation status to set.
	 * @param locale
	 *        The request locale.
	 * @return The updated alert.
	 */
	@RequestMapping(value = "/situation/{alertId}/resolve", method = RequestMethod.POST)
	@ResponseBody
	public Result<UserAlert> resolveSituation(@PathVariable("alertId") Long alertId,
			@RequestParam("status") UserAlertSituationStatus status, Locale locale) {
		UserAlert alert = userAlertBiz.updateSituationStatus(alertId, status);
		populateUsefulAlertOptions(alert, locale);
		return success(alert);
	}

	/**
	 * Delete an alert.
	 *
	 * @param alertId
	 *        The ID of the alert to delete.
	 * @return The result.
	 */
	@RequestMapping(value = "/{alertId}", method = RequestMethod.DELETE)
	@ResponseBody
	public Result<Object> deleteAlert(@PathVariable("alertId") Long alertId) {
		userAlertBiz.deleteAlert(alertId);
		return success();
	}
}
