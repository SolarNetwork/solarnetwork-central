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

import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import net.solarnetwork.central.dao.BaseStringEntity;
import net.solarnetwork.central.dao.UserRelatedEntity;
import net.solarnetwork.central.security.BasicSecurityPolicy;
import net.solarnetwork.central.security.SecurityPolicy;
import net.solarnetwork.central.security.SecurityToken;
import net.solarnetwork.central.security.SecurityTokenStatus;
import net.solarnetwork.central.security.SecurityTokenType;
import net.solarnetwork.codec.JsonUtils;
import net.solarnetwork.domain.SerializeIgnore;

/**
 * A user authorization token.
 * 
 * @author matt
 * @version 2.2
 */
public class UserAuthToken extends BaseStringEntity implements UserRelatedEntity<String>, SecurityToken {

	private static final long serialVersionUID = -4544594854807498756L;

	private Long userId;
	private String name;
	private String description;
	private String authSecret;
	private SecurityTokenStatus status;
	private SecurityTokenType type;
	private BasicSecurityPolicy policy;
	private String policyJson;

	/**
	 * Default constructor.
	 */
	public UserAuthToken() {
		super();
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
	 */
	public UserAuthToken(String token, Long userId, String secret, SecurityTokenType type) {
		super();
		setId(token);
		setUserId(userId);
		setAuthSecret(secret);
		setStatus(SecurityTokenStatus.Active);
		setType(type);
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
	public boolean isInfoDifferent(UserAuthToken other) {
		if ( this == other ) {
			return false;
		}
		return !(Objects.equals(description, other.description) && Objects.equals(name, other.name));
	}

	@Override
	public Long getUserId() {
		return userId;
	}

	public void setUserId(Long userId) {
		this.userId = userId;
	}

	/**
	 * Get the friendly name.
	 * 
	 * @return the name
	 * @since 2.1
	 */
	public String getName() {
		return name;
	}

	/**
	 * Set the friendly name.
	 * 
	 * @param name
	 *        the name to set
	 * @since 2.1
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Get the description.
	 * 
	 * @return the description
	 * @since 2.1
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Set the description.
	 * 
	 * @param description
	 *        the description to set
	 * @since 2.1
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * Get the ID value.
	 * 
	 * This is just an alias for {@link BaseStringEntity#getId()}.
	 * 
	 * @return the auth token
	 */
	@SerializeIgnore
	@JsonIgnore
	public String getAuthToken() {
		return getId();
	}

	/**
	 * Set the ID value.
	 * 
	 * This is just an alias for {@link BaseStringEntity#setId(String)}.
	 * 
	 * @param authToken
	 *        the auth token
	 */
	public void setAuthToken(String authToken) {
		setId(authToken);
	}

	public String getAuthSecret() {
		return authSecret;
	}

	public void setAuthSecret(String authSecret) {
		this.authSecret = authSecret;
	}

	public SecurityTokenStatus getStatus() {
		return status;
	}

	public void setStatus(SecurityTokenStatus status) {
		this.status = status;
	}

	public SecurityTokenType getType() {
		return type;
	}

	public void setType(SecurityTokenType type) {
		this.type = type;
	}

	/**
	 * Get the node IDs included in the policy, if available.
	 * 
	 * @return node IDs, or {@code null}
	 */
	@SerializeIgnore
	@JsonIgnore
	public Set<Long> getNodeIds() {
		BasicSecurityPolicy p = getPolicy();
		return (p == null ? null : p.getNodeIds());
	}

	/**
	 * Get the {@link BasicSecurityPolicy}.
	 * 
	 * If {@link #setPolicyJson(String)} has been previously called, this will
	 * parse that JSON into a {@code BasicSecurityPolicy} instance and return
	 * that.
	 * 
	 * @return the policy
	 */
	@Override
	public BasicSecurityPolicy getPolicy() {
		if ( policy == null && policyJson != null ) {
			policy = JsonUtils.getObjectFromJSON(policyJson, BasicSecurityPolicy.class);
		}
		return policy;
	}

	/**
	 * Set the {@link BasicSecurityPolicy} instance to use.
	 * 
	 * This will replace any value set previously via
	 * {@link #setPolicyJson(String)} as well.
	 * 
	 * @param policy
	 *        the policy instance to set
	 */
	public void setPolicy(BasicSecurityPolicy policy) {
		this.policy = policy;
		policyJson = null;
	}

	/**
	 * Get the {@link BasicSecurityPolicy} object as a JSON string.
	 * 
	 * This method will ignore <em>null</em> values.
	 * 
	 * @return a JSON encoded string, or {@code null}
	 */
	@SerializeIgnore
	@JsonIgnore
	public String getPolicyJson() {
		if ( policyJson == null ) {
			policyJson = JsonUtils.getJSONString(policy, null);
		}
		return policyJson;
	}

	/**
	 * Set the {@link BasicSecurityPolicy} object via a JSON string.
	 * 
	 * This method will remove any previously set {@code BasicSecurityPolicy}
	 * and replace it with the values parsed from the JSON.
	 * 
	 * @param json
	 *        The policy JSON to set.
	 */
	@JsonProperty // @JsonProperty needed because of @JsonIgnore on getter
	public void setPolicyJson(String json) {
		policyJson = json;
		policy = null;
	}

	/**
	 * Test if the token has expired.
	 * 
	 * @return {@literal true} if the token has expired
	 * @since 1.3
	 */
	public boolean isExpired() {
		SecurityPolicy policy = getPolicy();
		return (policy != null && !policy.isValidAt(Instant.now()));
	}

	@JsonIgnore
	@Override
	public boolean isAuthenticatedWithToken() {
		return true;
	}

	@JsonIgnore
	@Override
	public String getToken() {
		return getId();
	}

	@JsonIgnore
	@Override
	public SecurityTokenType getTokenType() {
		return getType();
	}

}
