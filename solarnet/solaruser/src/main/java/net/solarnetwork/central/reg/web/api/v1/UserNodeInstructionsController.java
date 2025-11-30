/* ==================================================================
 * UserNodeInstructionsController.java - 16/11/2025 3:37:54 pm
 *
 * Copyright 2025 SolarNetwork.net Dev Team
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

import static net.solarnetwork.central.security.SecurityUtils.getCurrentActorUserId;
import static net.solarnetwork.central.web.WebUtils.uriWithoutHost;
import static net.solarnetwork.domain.Result.success;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.fromMethodCall;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;
import java.net.URI;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;
import net.solarnetwork.central.domain.BasicClaimableJobState;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.user.biz.UserNodeInstructionBiz;
import net.solarnetwork.central.user.config.SolarNetUserConfiguration;
import net.solarnetwork.central.user.dao.BasicUserNodeInstructionTaskFilter;
import net.solarnetwork.central.user.domain.UserNodeInstructionTaskEntity;
import net.solarnetwork.central.user.domain.UserNodeInstructionTaskEntityInput;
import net.solarnetwork.central.user.domain.UserNodeInstructionTaskSimulationOutput;
import net.solarnetwork.central.user.domain.UserNodeInstructionTaskStateInput;
import net.solarnetwork.central.web.GlobalExceptionRestController;
import net.solarnetwork.dao.FilterResults;
import net.solarnetwork.domain.Result;

/**
 * Web service API for user node instruction task management.
 *
 * @author matt
 * @version 1.0
 */
@Profile(SolarNetUserConfiguration.USER_INSTRUCTIONS)
@GlobalExceptionRestController
@RestController("v1UserNodeInstructionsController")
@RequestMapping(value = { "/api/v1/sec/user/instr" })
public class UserNodeInstructionsController {

	private final UserNodeInstructionBiz biz;

	/**
	 * Constructor.
	 *
	 * @param userInstructionBiz
	 *        the user instruction biz
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public UserNodeInstructionsController(UserNodeInstructionBiz userInstructionBiz) {
		super();
		this.biz = requireNonNullArgument(userInstructionBiz, "userInstructionBiz");
	}

	/*-=======================
	 * Simulate Instruction
	 *-======================= */

	@RequestMapping(value = "/simulate", method = RequestMethod.POST)
	public Result<UserNodeInstructionTaskSimulationOutput> simulateUserNodeInstructionTask(
			@Valid @RequestBody UserNodeInstructionTaskEntityInput input) {
		var result = biz.simulateControlInstructionTask(getCurrentActorUserId(), input);
		return success(result);
	}

	/*-=======================
	 * Instruction Tasks
	 *-======================= */

	@RequestMapping(value = "/tasks", method = RequestMethod.GET)
	public Result<FilterResults<UserNodeInstructionTaskEntity, UserLongCompositePK>> listUserNodeInstructionTasks(
			BasicUserNodeInstructionTaskFilter filter) {
		var result = biz.listControlInstructionTasksForUser(getCurrentActorUserId(), filter);
		return success(result);
	}

	@RequestMapping(value = "/tasks", method = RequestMethod.POST)
	public ResponseEntity<Result<UserNodeInstructionTaskEntity>> createUserNodeInstructionTask(
			@Valid @RequestBody UserNodeInstructionTaskEntityInput input) {
		var id = UserLongCompositePK.unassignedEntityIdKey(getCurrentActorUserId());
		var result = biz.saveControlInstructionTask(id, input);
		URI loc = uriWithoutHost(fromMethodCall(on(UserNodeInstructionsController.class)
				.getUserNodeInstructionTask(result.getConfigId())));
		return ResponseEntity.created(loc).body(success(result));
	}

	@RequestMapping(value = "/tasks/{taskId}", method = RequestMethod.GET)
	public Result<UserNodeInstructionTaskEntity> getUserNodeInstructionTask(
			@PathVariable("taskId") Long taskId) {
		final var filter = new BasicUserNodeInstructionTaskFilter();
		filter.setTaskId(taskId);
		var result = biz.listControlInstructionTasksForUser(getCurrentActorUserId(), filter);
		return success(result.getReturnedResultCount() > 0 ? result.iterator().next() : null);
	}

	@RequestMapping(value = "/tasks/{taskId}", method = RequestMethod.PUT)
	public Result<UserNodeInstructionTaskEntity> updateUserNodeInstructionTask(
			@PathVariable("taskId") Long taskId,
			@Valid @RequestBody UserNodeInstructionTaskEntityInput input) {
		var id = new UserLongCompositePK(getCurrentActorUserId(), taskId);
		BasicClaimableJobState[] requiredStates = null;
		if ( input.getRequiredStates() != null && !input.getRequiredStates().isEmpty() ) {
			requiredStates = input.getRequiredStates().toArray(BasicClaimableJobState[]::new);
		}
		return success(biz.saveControlInstructionTask(id, input, requiredStates));
	}

	@RequestMapping(value = "/tasks/{taskId}", method = RequestMethod.DELETE)
	public Result<Void> deleteUserNodeInstructionTask(@PathVariable("taskId") Long taskId) {
		var id = new UserLongCompositePK(getCurrentActorUserId(), taskId);
		biz.deleteControlInstructionTask(id);
		return success();
	}

	@RequestMapping(value = "/tasks/{taskId}/state", method = RequestMethod.POST)
	public Result<UserNodeInstructionTaskEntity> updateUserNodeInstructionTaskState(
			@PathVariable("taskId") Long taskId,
			@Valid @RequestBody UserNodeInstructionTaskStateInput input) {
		var id = new UserLongCompositePK(getCurrentActorUserId(), taskId);
		BasicClaimableJobState[] requiredStates = null;
		if ( input.getRequiredStates() != null && !input.getRequiredStates().isEmpty() ) {
			requiredStates = input.getRequiredStates().toArray(BasicClaimableJobState[]::new);
		}
		return success(biz.updateControlInstructionTaskState(id, input.getState(), requiredStates));
	}

}
