/* ==================================================================
 * AccountTask.java - 21/07/2020 6:11:22 AM
 *
 * Copyright 2020 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.billing.snf.domain;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.io.Serial;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import net.solarnetwork.dao.BasicUuidEntity;
import net.solarnetwork.domain.Differentiable;

/**
 * An account task entity.
 *
 * @author matt
 * @version 1.1
 */
public class AccountTask extends BasicUuidEntity implements Differentiable<AccountTask> {

	@Serial
	private static final long serialVersionUID = -6310585670232356068L;

	/**
	 * The task data property name for an entity ID value.
	 */
	public static final String ID_PARAM = "id";

	/**
	 * The task data property name for a user ID value.
	 */
	public static final String USER_ID_PARAM = "userId";

	private final AccountTaskType taskType;
	private final Long accountId;
	private final @Nullable Map<String, Object> taskData;

	/**
	 * Create a new task instance.
	 *
	 * @param date
	 *        the task date
	 * @param taskType
	 *        the task type
	 * @param accountId
	 *        the account ID
	 * @return the new task instance, with a new ID
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public static AccountTask newTask(Instant date, AccountTaskType taskType, Long accountId) {
		return newTask(date, taskType, accountId, null);
	}

	/**
	 * Create a new task instance.
	 *
	 * @param date
	 *        the task date
	 * @param taskType
	 *        the task type
	 * @param accountId
	 *        the account ID
	 * @param taskData
	 *        the task data, or {@literal null}
	 * @return the new task instance, with a new ID
	 * @throws IllegalArgumentException
	 *         if {@code date} or {@code taskType} or {@code accountId} are
	 *         {@literal null}
	 */
	public static AccountTask newTask(Instant date, AccountTaskType taskType, Long accountId,
			@Nullable Map<String, Object> taskData) {
		return new AccountTask(UUID.randomUUID(), requireNonNullArgument(date, "date"), taskType,
				accountId, taskData);
	}

	/**
	 * Constructor.
	 *
	 * @param taskType
	 *        the task type
	 * @param accountId
	 *        the account ID
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public AccountTask(final AccountTaskType taskType, Long accountId) {
		this(taskType, accountId, null);
	}

	/**
	 * Constructor.
	 *
	 * @param taskType
	 *        the task type
	 * @param accountId
	 *        the account ID
	 * @param taskData
	 *        the task data
	 * @throws IllegalArgumentException
	 *         if {@code taskType} or {@code accountId} are {@literal null}
	 */
	public AccountTask(final AccountTaskType taskType, Long accountId,
			@Nullable Map<String, Object> taskData) {
		this(null, null, taskType, accountId, taskData);
	}

	/**
	 * Constructor.
	 *
	 * @param id
	 *        the primary key
	 * @param created
	 *        the creation date
	 * @param taskType
	 *        the task type
	 * @param accountId
	 *        the account ID
	 * @param taskData
	 *        the task data
	 * @throws IllegalArgumentException
	 *         if {@code taskType} or {@code accountId} are {@literal null}
	 */
	public AccountTask(@Nullable UUID id, @Nullable Instant created, final AccountTaskType taskType,
			Long accountId, @Nullable Map<String, Object> taskData) {
		super(id, created);
		this.taskType = requireNonNullArgument(taskType, "taskType");
		this.accountId = requireNonNullArgument(accountId, "accountId");
		this.taskData = taskData;
	}

	/**
	 * Test if the properties of another entity are the same as in this
	 * instance.
	 *
	 * <p>
	 * The {@code id} and {@code created} properties are not compared by this
	 * method.
	 * </p>
	 *
	 * @param other
	 *        the other entity to compare to
	 * @return {@literal true} if the properties of this instance are equal to
	 *         the other
	 */
	public boolean isSameAs(@Nullable AccountTask other) {
		if ( other == null ) {
			return false;
		}
		// @formatter:off
		return Objects.equals(taskType, other.taskType)
				&& Objects.equals(accountId, other.accountId)
				&& Objects.equals(taskData, other.taskData);
		// @formatter:on
	}

	@Override
	public boolean differsFrom(@Nullable AccountTask other) {
		return !isSameAs(other);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("AccountTask{");
		if ( getId() != null ) {
			builder.append("id=");
			builder.append(getId());
			builder.append(", ");
		}
		if ( taskType != null ) {
			builder.append("taskType=");
			builder.append(taskType);
			builder.append(", ");
		}
		if ( accountId != null ) {
			builder.append("accountId=");
			builder.append(accountId);
			builder.append(", ");
		}
		if ( taskData != null ) {
			builder.append("taskData=");
			builder.append(taskData);
			builder.append(", ");
		}
		builder.append("}");
		return builder.toString();
	}

	/**
	 * Get the task type.
	 *
	 * @return the task type, never {@literal null}
	 */
	public final AccountTaskType getTaskType() {
		return taskType;
	}

	/**
	 * Get the account ID.
	 *
	 * @return the account ID, never {@literal null}
	 */
	public final Long getAccountId() {
		return accountId;
	}

	/**
	 * Get the task data.
	 *
	 * @return the task data
	 */
	public final @Nullable Map<String, Object> getTaskData() {
		return taskData;
	}

}
