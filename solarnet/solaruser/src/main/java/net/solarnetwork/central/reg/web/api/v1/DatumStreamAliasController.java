/* ==================================================================
 * DatumStreamAliasController.java - 1/04/2026 6:20:46 am
 *
 * Copyright 2026 SolarNetwork.net Dev Team
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

import static net.solarnetwork.central.domain.EntityConstants.UNASSIGNED_LONG_ID;
import static net.solarnetwork.central.domain.EntityConstants.UNASSIGNED_UUID_ID;
import static net.solarnetwork.central.security.SecurityUtils.getCurrentActorUserId;
import static net.solarnetwork.central.web.WebUtils.uriWithoutHost;
import static net.solarnetwork.domain.Result.success;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.fromMethodCall;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;
import net.solarnetwork.central.datum.v2.dao.BasicDatumCriteria;
import net.solarnetwork.central.datum.v2.dao.BasicObjectDatumStreamAliasGrantFilter;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumStreamAliasEntity;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumStreamAliasGrantEntity;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumStreamAliasGrantStream;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumStreamAliasMatchType;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.domain.UserLongStringCompositePK;
import net.solarnetwork.central.user.datum.stream.biz.UserDatumStreamAliasBiz;
import net.solarnetwork.central.user.datum.stream.domain.ObjectDatumStreamAliasEntityInput;
import net.solarnetwork.central.user.datum.stream.domain.ObjectDatumStreamAliasGrantEntityInput;
import net.solarnetwork.central.web.GlobalExceptionRestController;
import net.solarnetwork.dao.FilterResults;
import net.solarnetwork.domain.Result;

/**
 * Controller for managing datum stream aliases.
 *
 * @author matt
 * @version 1.1
 */
@GlobalExceptionRestController
@RestController("v1DatumStreamAliasController")
@RequestMapping(value = "/api/v1/sec/datum/stream/alias")
public class DatumStreamAliasController {

	private final UserDatumStreamAliasBiz userDatumStreamAliasBiz;

	/**
	 * Constructor.
	 *
	 * @param userDatumStreamAliasBiz
	 *        the stream metadata biz to use
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public DatumStreamAliasController(UserDatumStreamAliasBiz userDatumStreamAliasBiz) {
		super();
		this.userDatumStreamAliasBiz = requireNonNullArgument(userDatumStreamAliasBiz,
				"userDatumStreamAliasBiz");
	}

	@RequestMapping(value = { "", "/" }, method = RequestMethod.GET)
	public Result<FilterResults<ObjectDatumStreamAliasEntity, UUID>> listAliases(
			BasicDatumCriteria filter) {
		var result = userDatumStreamAliasBiz.listAliases(getCurrentActorUserId(), filter);
		return success(result);
	}

	@RequestMapping(value = { "", "/" }, method = RequestMethod.POST)
	public ResponseEntity<Result<ObjectDatumStreamAliasEntity>> createAlias(
			@Valid @RequestBody ObjectDatumStreamAliasEntityInput input) {
		var result = userDatumStreamAliasBiz.saveAlias(getCurrentActorUserId(), UNASSIGNED_UUID_ID,
				input);
		URI loc = uriWithoutHost(
				fromMethodCall(on(DatumStreamAliasController.class).getAlias(result.id())));
		return ResponseEntity.created(loc).body(success(result));
	}

	@RequestMapping(value = "/bulk", method = RequestMethod.POST)
	public Result<List<ObjectDatumStreamAliasEntity>> createAliases(
			@Valid @RequestBody List<ObjectDatumStreamAliasEntityInput> inputs) {
		return success(userDatumStreamAliasBiz.saveAliases(getCurrentActorUserId(), inputs));
	}

	@RequestMapping(value = { "", "/" }, method = RequestMethod.DELETE)
	public Result<Void> deleteAliases(BasicDatumCriteria filter) {
		userDatumStreamAliasBiz.deleteAliases(getCurrentActorUserId(), filter);
		return success();
	}

	@RequestMapping(value = "/{aliasId}", method = RequestMethod.GET)
	public Result<ObjectDatumStreamAliasEntity> getAlias(@PathVariable("aliasId") UUID id) {
		return success(userDatumStreamAliasBiz.aliasForUser(getCurrentActorUserId(), id));
	}

	@RequestMapping(value = "/{aliasId}", method = RequestMethod.PUT)
	public Result<ObjectDatumStreamAliasEntity> updateAlias(@PathVariable("aliasId") UUID id,
			@Valid @RequestBody ObjectDatumStreamAliasEntityInput input) {
		return success(userDatumStreamAliasBiz.saveAlias(getCurrentActorUserId(), id, input));
	}

	@RequestMapping(value = "/{aliasId}", method = RequestMethod.DELETE)
	public Result<Void> deleteAlias(@PathVariable("aliasId") UUID id) {
		final var filter = new BasicDatumCriteria();
		filter.setStreamAliasMatchType(ObjectDatumStreamAliasMatchType.AliasOnly);
		filter.setStreamId(id);
		userDatumStreamAliasBiz.deleteAliases(getCurrentActorUserId(), filter);
		return success();
	}

	@RequestMapping(value = "/grants", method = RequestMethod.GET)
	public Result<FilterResults<ObjectDatumStreamAliasGrantEntity, UserLongCompositePK>> listAliasGrants(
			BasicObjectDatumStreamAliasGrantFilter filter) {
		return success(userDatumStreamAliasBiz.listAliasGrants(getCurrentActorUserId(), filter));
	}

	@RequestMapping(value = "/grants", method = RequestMethod.POST)
	public ResponseEntity<Result<ObjectDatumStreamAliasGrantEntity>> createAliasGrant(
			@Valid @RequestBody ObjectDatumStreamAliasGrantEntityInput input) {
		var result = userDatumStreamAliasBiz.saveAliasGrant(getCurrentActorUserId(), UNASSIGNED_LONG_ID,
				input);
		URI loc = uriWithoutHost(
				fromMethodCall(on(DatumStreamAliasController.class).getAliasGrant(result.getGrantId())));
		return ResponseEntity.created(loc).body(success(result));
	}

	@RequestMapping(value = "/grants/received", method = RequestMethod.GET)
	public Result<FilterResults<ObjectDatumStreamAliasGrantEntity, UserLongCompositePK>> listReceivedAliasGrants(
			BasicObjectDatumStreamAliasGrantFilter filter) {
		return success(userDatumStreamAliasBiz.listReceivedAliasGrants(getCurrentActorUserId(), filter));
	}

	@RequestMapping(value = "/grants/received/streams", method = RequestMethod.GET)
	public Result<FilterResults<ObjectDatumStreamAliasGrantStream, UserLongStringCompositePK>> listAvailableAliasStreams(
			@RequestParam("objectId") Long objectId, BasicObjectDatumStreamAliasGrantFilter filter) {
		return success(userDatumStreamAliasBiz.listAvailableAliasStreams(getCurrentActorUserId(),
				objectId, filter));
	}

	@RequestMapping(value = "/grants/{grantId}", method = RequestMethod.GET)
	public Result<ObjectDatumStreamAliasGrantEntity> getAliasGrant(
			@PathVariable("grantId") Long grantId) {
		return success(userDatumStreamAliasBiz.aliasGrantForUser(getCurrentActorUserId(), grantId));
	}

	@RequestMapping(value = "/grants/{grantId}", method = RequestMethod.PUT)
	public Result<ObjectDatumStreamAliasGrantEntity> updateAliasGrant(
			@PathVariable("grantId") Long grantId,
			@Valid @RequestBody ObjectDatumStreamAliasGrantEntityInput input) {
		return success(userDatumStreamAliasBiz.saveAliasGrant(getCurrentActorUserId(), grantId, input));
	}

	@RequestMapping(value = "/grants/{grantId}/enabled/{enabled}", method = RequestMethod.POST)
	public Result<ObjectDatumStreamAliasGrantEntity> enableAliasGrant(
			@PathVariable("grantId") Long grantId, @PathVariable("enabled") boolean enabled) {
		return success(userDatumStreamAliasBiz.updateAliasGrantEnabled(getCurrentActorUserId(), grantId,
				enabled));
	}

	@RequestMapping(value = "/grants/{grantId}", method = RequestMethod.DELETE)
	public Result<Void> deleteAliasGrant(@PathVariable("grantId") Long grantId) {
		userDatumStreamAliasBiz.deleteAliasGrant(getCurrentActorUserId(), grantId);
		return success();
	}

}
