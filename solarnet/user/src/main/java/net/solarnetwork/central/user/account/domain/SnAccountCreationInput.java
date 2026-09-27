/* ==================================================================
 * SnAccountCreationInput.java - 26 Sept 2026 6:52:59 pm
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

package net.solarnetwork.central.user.account.domain;

import java.io.Serial;
import java.io.Serializable;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * DTO for account and address configuration.
 *
 * @author matt
 * @version 1.0
 */
@SuppressWarnings("MultipleNullnessAnnotations")
public class SnAccountCreationInput implements Serializable {

	@Serial
	private static final long serialVersionUID = -8617384371765834188L;

	private @Nullable String systemKey;

	@Valid
	@NotNull
	private @Nullable SnAccountInput account;

	@Valid
	@NotNull
	private @Nullable SnAddressInput address;

	private @Nullable Set<SnFeatureEntitlement> requestedEntitlements;

	/**
	 * Constructor.
	 */
	public SnAccountCreationInput() {
		super();
	}

	/**
	 * Get the system key.
	 * 
	 * @return the system key, or {@code null} to use the default billing system
	 */
	public final @Nullable String getSystemKey() {
		return systemKey;
	}

	/**
	 * Set the system key.
	 * 
	 * @param systemKey
	 *        the system key to set, or {@code null} to use the default billing
	 *        system
	 */
	public final void setSystemKey(@Nullable String systemKey) {
		this.systemKey = systemKey;
	}

	/**
	 * Get the account.
	 *
	 * @return the account
	 */
	public final @Nullable SnAccountInput getAccount() {
		return account;
	}

	/**
	 * Set the account.
	 *
	 * @param account
	 *        the account to set
	 */
	public final void setAccount(@Nullable SnAccountInput account) {
		this.account = account;
	}

	/**
	 * Get the address.
	 *
	 * @return the address
	 */
	public final @Nullable SnAddressInput getAddress() {
		return address;
	}

	/**
	 * Set the address.
	 *
	 * @param address
	 *        the address to set
	 */
	public final void setAddress(@Nullable SnAddressInput address) {
		this.address = address;
	}

	/**
	 * Get the requested feature entitlements.
	 *
	 * @return the entitlements
	 */
	public final @Nullable Set<SnFeatureEntitlement> getRequestedEntitlements() {
		return requestedEntitlements;
	}

	/**
	 * Set the requested feature entitlements.
	 *
	 * @param requestedEntitlements
	 *        the entitlements to set
	 */
	public final void setRequestedEntitlements(
			@Nullable Set<SnFeatureEntitlement> requestedEntitlements) {
		this.requestedEntitlements = requestedEntitlements;
	}

}
