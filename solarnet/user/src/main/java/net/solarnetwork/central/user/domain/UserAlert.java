/* ==================================================================
 * UserAlert.java - 15/05/2015 11:31:07 am
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

package net.solarnetwork.central.user.domain;

import static net.solarnetwork.util.ObjectUtils.nonnull;
import java.io.Serial;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import net.solarnetwork.central.dao.BaseEntity;
import net.solarnetwork.central.dao.UserRelatedEntity;
import net.solarnetwork.codec.jackson.JsonUtils;
import net.solarnetwork.domain.SerializeIgnore;

/**
 * An alert condition definition. User alerts are designed to cover conditions
 * such as
 *
 * <ul>
 * <li>node <em>X</em> has not posted data in <em>Y</em> hours</li>
 * <li>node <em>X</em> posted value <em>V</em> for property <em>A</em> greater
 * than <em>T</em></li>
 * <li>location <em>L</em> posted value <em>V</em> for property <em>A</em> less
 * than <em>T</em></li>
 * </ul>
 *
 * @author matt
 * @version 2.3
 */
@JsonPropertyOrder({ "id", "created", "userId", "nodeId", "type", "status", "validTo", "options" })
public class UserAlert extends BaseEntity implements UserRelatedEntity<Long> {

	@Serial
	private static final long serialVersionUID = -912662853889560214L;

	private @Nullable Long userId;
	private @Nullable UserAlertType type;
	private @Nullable UserAlertStatus status;
	private @Nullable Long nodeId;
	private @Nullable Instant validTo;
	private @Nullable Map<String, Object> options;
	private @Nullable String optionsJson;

	// transient
	private @Nullable UserAlertSituation situation;

	/**
	 * Constructor.
	 */
	public UserAlert() {
		super();
	}

	@Override
	public UserAlert clone() {
		return (UserAlert) super.clone();
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("UserAlert{id=");
		builder.append(getId());
		builder.append(", userId=");
		builder.append(userId);
		builder.append(", type=");
		builder.append(type);
		builder.append(", status=");
		builder.append(status);
		if ( nodeId != null ) {
			builder.append(", ");
			builder.append("nodeId=");
			builder.append(nodeId);
		}
		builder.append("}");
		return builder.toString();
	}

	/**
	 * Get the options object as a JSON string.
	 *
	 * <p>
	 * This method will ignore {@code null} values.
	 * </p>
	 *
	 * @return a JSON encoded string, never {@code null}
	 */
	@SerializeIgnore
	@JsonIgnore
	public final @Nullable String getOptionsJson() {
		if ( optionsJson == null ) {
			optionsJson = JsonUtils.getJSONString(options, "{}");
		}
		return optionsJson;
	}

	/**
	 * Set the options object via a JSON string.
	 *
	 * <p>
	 * This method will remove any previously created options and replace it
	 * with the values parsed from the JSON. All floating point values will be
	 * converted to {@link BigDecimal} instances.
	 * </p>
	 *
	 * @param json
	 *        the JSON to set
	 */
	@JsonProperty
	// @JsonProperty needed because of @JsonIgnore on getter
	public final void setOptionsJson(@Nullable String json) {
		optionsJson = json;
		options = null;
	}

	@Override
	public final Long getUserId() {
		return nonnull(userId, "userId");
	}

	public final void setUserId(Long userId) {
		this.userId = userId;
	}

	public final @Nullable UserAlertType getType() {
		return type;
	}

	public final void setType(@Nullable UserAlertType type) {
		this.type = type;
	}

	public final @Nullable UserAlertStatus getStatus() {
		return status;
	}

	public final void setStatus(@Nullable UserAlertStatus status) {
		this.status = status;
	}

	public final @Nullable Long getNodeId() {
		return nodeId;
	}

	public final void setNodeId(@Nullable Long nodeId) {
		this.nodeId = nodeId;
	}

	public final @Nullable Instant getValidTo() {
		return validTo;
	}

	public final void setValidTo(@Nullable Instant validTo) {
		this.validTo = validTo;
	}

	@SuppressWarnings("unchecked")
	@JsonProperty
	public final @Nullable Map<String, Object> getOptions() {
		if ( options == null && optionsJson != null ) {
			options = JsonUtils.getObjectFromJSON(optionsJson, Map.class);
		}
		return options;
	}

	/**
	 * Set the options instance to use.
	 *
	 * <p>
	 * This will replace any value set previously via
	 * {@link #setOptionsJson(String)} as well.
	 * </p>
	 *
	 * @param options
	 *        the samples instance to set
	 */
	@JsonProperty
	public final void setOptions(@Nullable Map<String, Object> options) {
		this.options = options;
		optionsJson = null;
	}

	/**
	 * Get the {@link UserAlertOptions#EMAIL_TOS} list as an array.
	 *
	 * @return the email list, or {@code null} if the option is not available
	 * @since 2.1
	 */
	public final String @Nullable [] optionEmailTos() {
		String[] result = null;
		if ( options != null ) {
			Object o = options.get(UserAlertOptions.EMAIL_TOS);
			if ( o instanceof Collection<?> c ) {
				result = c.stream().map(Object::toString).toArray(String[]::new);
			} else if ( o instanceof String[] a ) {
				result = a;
			} else if ( o != null ) {
				result = new String[] { o.toString() };
			}
		}
		return result;
	}

	/**
	 * Get the {@link UserAlertOptions#SOURCE_IDS} list.
	 *
	 * @return the source ID list, or {@code null} if the option is not
	 *         available
	 * @since 2.2
	 */
	public final @Nullable List<String> optionSourceIds() {
		List<String> result = null;
		if ( options != null ) {
			Object o = options.get(UserAlertOptions.SOURCE_IDS);
			if ( o instanceof Collection<?> c ) {
				result = c.stream().map(Object::toString).collect(Collectors.toList());
			} else if ( o instanceof String[] a ) {
				result = Arrays.asList(a);
			} else if ( o != null ) {
				result = Collections.singletonList(o.toString());
			}
		}
		return result;
	}

	public final @Nullable UserAlertSituation getSituation() {
		return situation;
	}

	public final void setSituation(@Nullable UserAlertSituation situation) {
		this.situation = situation;
	}

}
