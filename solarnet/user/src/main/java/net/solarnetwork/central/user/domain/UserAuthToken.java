/* ==================================================================
 * UserAuthToken.java - Dec 12, 2012 1:21:40 PM
 *
 * Copyright 2007-2012 SolarNetwork.net Dev Team
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
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.io.Serial;
import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import net.solarnetwork.central.dao.BaseStringEntity;
import net.solarnetwork.central.dao.UserRelatedEntity;
import net.solarnetwork.central.security.SecurityToken;
import net.solarnetwork.central.security.SecurityTokenStatus;
import net.solarnetwork.central.security.SecurityTokenType;
import net.solarnetwork.codec.jackson.JsonUtils;
import net.solarnetwork.domain.BasicSecurityPolicy;
import net.solarnetwork.domain.SecurityPolicy;
import net.solarnetwork.domain.SerializeIgnore;

/**
 * A user authorization token.
 *
 * @author matt
 * @version 3.1
 */
public class UserAuthToken extends BaseStringEntity implements UserRelatedEntity<String>, SecurityToken {

	@Serial
	private static final long serialVersionUID = -4544594854807498756L;

	private final Long userId;
	private @Nullable String name;
	private @Nullable String description;
	private @Nullable String authSecret;
	private SecurityTokenStatus status;
	private final SecurityTokenType type;
	private @Nullable SecurityPolicy policy;
	private @Nullable String policyJson;

	/**
	 * Create a new, active {@code ReadNodeData} token.
	 *
	 * @param token
	 *        the token value
	 * @param userId
	 *        the user ID
	 * @throws IllegalArgumentException
	 *         if any argument except {@code secret} is {@code null}
	 * @since 3.1
	 */
	public UserAuthToken(String token, Long userId) {
		this(token, userId, null, SecurityTokenType.ReadNodeData);
	}

	/**
	 * Create a new, active token.
	 *
	 * @param token
	 *        the token value
	 * @param userId
	 *        the user ID
	 * @param type
	 *        the type
	 * @throws IllegalArgumentException
	 *         if any argument except {@code secret} is {@code null}
	 * @since 3.1
	 */
	public UserAuthToken(String token, Long userId, SecurityTokenType type) {
		this(token, userId, null, type);
	}

	/**
	 * Create a new, active token.
	 *
	 * @param token
	 *        the token value
	 * @param userId
	 *        the user ID
	 * @param secret
	 *        the secret
	 * @param type
	 *        the type
	 * @throws IllegalArgumentException
	 *         if any argument except {@code secret} is {@code null}
	 */
	public UserAuthToken(String token, Long userId, @Nullable String secret, SecurityTokenType type) {
		super();
		setId(requireNonNullArgument(token, "token"));
		this.userId = requireNonNullArgument(userId, "userId");
		setAuthSecret(secret);
		this.status = SecurityTokenStatus.Active;
		this.type = type != null ? type : SecurityTokenType.ReadNodeData;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("UserAuthToken{userId=");
		builder.append(userId);
		builder.append(", id=");
		builder.append(getId());

		if ( name != null ) {
			builder.append(", name=");
			builder.append(name);
		}
		if ( status != null ) {
			builder.append(", status=");
			builder.append(status);
		}
		if ( type != null ) {
			builder.append(", type=");
			builder.append(type);
		}
		if ( policy != null ) {
			builder.append(", policy=");
			builder.append(getPolicy());
		}
		builder.append("}");
		return builder.toString();
	}

	/**
	 * Test if the information (name, description) in another token differs from
	 * this token.
	 *
	 * @param other
	 *        the token to compare to
	 * @return {@literal true} if the name or description differs
	 * @since 2.1
	 */
	@SuppressWarnings("ReferenceEquality")
	public boolean isInfoDifferent(UserAuthToken other) {
		if ( this == other ) {
			return false;
		}
		return !(Objects.equals(description, other.description) && Objects.equals(name, other.name));
	}

	@Override
	public final Long getUserId() {
		return userId;
	}

	/**
	 * Get the friendly name.
	 *
	 * @return the name
	 * @since 2.1
	 */
	public final @Nullable String getName() {
		return name;
	}

	/**
	 * Set the friendly name.
	 *
	 * @param name
	 *        the name to set
	 * @since 2.1
	 */
	public final void setName(@Nullable String name) {
		this.name = name;
	}

	/**
	 * Get the description.
	 *
	 * @return the description
	 * @since 2.1
	 */
	public final @Nullable String getDescription() {
		return description;
	}

	/**
	 * Set the description.
	 *
	 * @param description
	 *        the description to set
	 * @since 2.1
	 */
	public final void setDescription(@Nullable String description) {
		this.description = description;
	}

	/**
	 * Get the ID value.
	 *
	 * <p>
	 * This is just an alias for {@link BaseStringEntity#getId()}.
	 * </p>
	 *
	 * @return the auth token
	 */
	@SerializeIgnore
	@JsonIgnore
	public final String getAuthToken() {
		return getToken();
	}

	public final @Nullable String getAuthSecret() {
		return authSecret;
	}

	public final void setAuthSecret(@Nullable String authSecret) {
		this.authSecret = authSecret;
	}

	public final SecurityTokenStatus getStatus() {
		return status;
	}

	public final void setStatus(SecurityTokenStatus status) {
		this.status = status;
	}

	public final SecurityTokenType getType() {
		return type;
	}

	/**
	 * Get the node IDs included in the policy, if available.
	 *
	 * @return node IDs, or {@code null}
	 */
	@SerializeIgnore
	@JsonIgnore
	public final @Nullable Set<Long> getNodeIds() {
		SecurityPolicy p = getPolicy();
		return (p == null ? null : p.getNodeIds());
	}

	/**
	 * Get the {@link BasicSecurityPolicy}.
	 *
	 * <p>
	 * If {@link #setPolicyJson(String)} has been previously called, this will
	 * parse that JSON into a {@code BasicSecurityPolicy} instance and return
	 * that.
	 * </p>
	 *
	 * @return the policy
	 */
	@Override
	public final @Nullable SecurityPolicy getPolicy() {
		if ( policy == null && policyJson != null ) {
			policy = JsonUtils.getObjectFromJSON(policyJson, SecurityPolicy.class);
		}
		return policy;
	}

	/**
	 * Set the {@link BasicSecurityPolicy} instance to use.
	 *
	 * <p>
	 * This will replace any value set previously via
	 * {@link #setPolicyJson(String)} as well.
	 * </p>
	 *
	 * @param policy
	 *        the policy instance to set
	 */
	public final void setPolicy(@Nullable SecurityPolicy policy) {
		this.policy = policy;
		policyJson = null;
	}

	/**
	 * Get the {@link BasicSecurityPolicy} object as a JSON string.
	 *
	 * <p>
	 * This method will ignore {@code null} values.
	 * </p>
	 *
	 * @return a JSON encoded string, or {@code null}
	 */
	@SerializeIgnore
	@JsonIgnore
	public final @Nullable String getPolicyJson() {
		if ( policyJson == null ) {
			policyJson = JsonUtils.getJSONString(policy, null);
		}
		return policyJson;
	}

	/**
	 * Set the {@link BasicSecurityPolicy} object via a JSON string.
	 *
	 * <p>
	 * This method will remove any previously set {@code BasicSecurityPolicy}
	 * and replace it with the values parsed from the JSON.
	 * </p>
	 *
	 * @param json
	 *        The policy JSON to set.
	 */
	@JsonProperty // @JsonProperty needed because of @JsonIgnore on getter
	public final void setPolicyJson(@Nullable String json) {
		policyJson = json;
		policy = null;
	}

	/**
	 * Test if the token has expired.
	 *
	 * @return {@literal true} if the token has expired
	 * @since 1.3
	 */
	public final boolean isExpired() {
		SecurityPolicy policy = getPolicy();
		return (policy != null && !policy.isValidAt(Instant.now()));
	}

	@JsonIgnore
	@Override
	public final boolean isAuthenticatedWithToken() {
		return true;
	}

	@JsonIgnore
	@Override
	public final String getToken() {
		return nonnull(getId(), "token");
	}

	@JsonIgnore
	@Override
	public final SecurityTokenType getTokenType() {
		return getType();
	}

}
