/* ==================================================================
 * SnAccountInfo.java - 28 Sept 2026 8:04:55 am
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

import java.util.Collection;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.springframework.context.MessageSourceResolvable;
import com.fasterxml.jackson.annotation.JsonIgnore;
import net.solarnetwork.central.user.billing.biz.BillingSystemRegistrar;

/**
 * Account information record.
 *
 * @param systemKey
 *        the {@link BillingSystemRegistrar} the account is associated with
 * @param account
 *        the account information
 * @param entitlements
 *        the account entitlements
 * @param parentAccount
 *        if this account is the child of another account, the info for that
 *        parent account
 * @param childAccounts
 *        any child accounts, or {@code null} if there are none
 * @param consolidationProblem
 *        the problem with the invoice consolidation group of the account, as a
 *        message to resolve where it is shown, or {@code null} if the account is
 *        not in an invalid group
 * @author matt
 * @version 1.1
 */
public record SnAccountInfo(String systemKey, SnAccount<?, ?, ?> account,
		Set<SnFeatureEntitlement> entitlements, @Nullable SnLinkedAccountInfo parentAccount,
		@Nullable Collection<SnLinkedAccountInfo> childAccounts,
		@JsonIgnore @Nullable MessageSourceResolvable consolidationProblem) {

	/**
	 * Construct without any linked accounts.
	 *
	 * @param systemKey
	 *        the {@link BillingSystemRegistrar} the account is associated with
	 * @param account
	 *        the account information
	 * @param entitlements
	 *        the account entitlements
	 * @since 1.1
	 */
	public SnAccountInfo(String systemKey, SnAccount<?, ?, ?> account,
			Set<SnFeatureEntitlement> entitlements) {
		this(systemKey, account, entitlements, null, null, null);
	}

	/**
	 * Construct with linked accounts.
	 *
	 * @param systemKey
	 *        the {@link BillingSystemRegistrar} the account is associated with
	 * @param account
	 *        the account information
	 * @param entitlements
	 *        the account entitlements
	 * @param parentAccount
	 *        if this account is the child of another account, the info for
	 *        that parent account
	 * @param childAccounts
	 *        any child accounts, or {@code null} if there are none
	 * @since 1.1
	 */
	public SnAccountInfo(String systemKey, SnAccount<?, ?, ?> account,
			Set<SnFeatureEntitlement> entitlements, @Nullable SnLinkedAccountInfo parentAccount,
			@Nullable Collection<SnLinkedAccountInfo> childAccounts) {
		this(systemKey, account, entitlements, parentAccount, childAccounts, null);
	}

	/**
	 * Construct with a parent account.
	 *
	 * @param systemKey
	 *        the {@link BillingSystemRegistrar} the account is associated with
	 * @param account
	 *        the account information
	 * @param entitlements
	 *        the account entitlements
	 * @param parent
	 *        the parent account
	 * @since 1.1
	 */
	public static SnAccountInfo child(String systemKey, SnAccount<?, ?, ?> account,
			Set<SnFeatureEntitlement> entitlements, SnLinkedAccountInfo parent) {
		return new SnAccountInfo(systemKey, account, entitlements, parent, null);
	}

	/**
	 * Construct with a child accounts.
	 *
	 * @param systemKey
	 *        the {@link BillingSystemRegistrar} the account is associated with
	 * @param account
	 *        the account information
	 * @param entitlements
	 *        the account entitlements
	 * @param children
	 *        the children
	 * @since 1.1
	 */
	public static SnAccountInfo parent(String systemKey, SnAccount<?, ?, ?> account,
			Set<SnFeatureEntitlement> entitlements, Collection<SnLinkedAccountInfo> children) {
		return new SnAccountInfo(systemKey, account, entitlements, null, children);
	}

}
