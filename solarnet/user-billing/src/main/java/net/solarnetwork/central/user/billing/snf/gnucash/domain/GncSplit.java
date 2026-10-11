/* ==================================================================
 * GncSplit.java - 11/10/2026 2:44:32 pm
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

package net.solarnetwork.central.user.billing.snf.gnucash.domain;

import java.time.Instant;
import org.jspecify.annotations.Nullable;

/**
 * A GnuCash split: one account's part of a transaction.
 *
 * @param guid
 *        the GUID
 * @param txGuid
 *        the GUID of the transaction
 * @param accountGuid
 *        the GUID of the account
 * @param memo
 *        the memo
 * @param action
 *        the action
 * @param reconcileState
 *        the reconcile state, such as {@link #RECONCILE_NO}
 * @param reconcileDate
 *        the reconcile date; GnuCash uses the epoch when not reconciled
 * @param value
 *        the value, in the transaction currency
 * @param quantity
 *        the amount, in the account commodity
 * @param lotGuid
 *        the GUID of the lot the split belongs to
 * @author matt
 * @version 1.0
 */
public record GncSplit(String guid, String txGuid, String accountGuid, String memo, String action,
		String reconcileState, Instant reconcileDate, GncNumeric value, GncNumeric quantity,
		@Nullable String lotGuid) {

	/** The reconcile state of a split that is not reconciled. */
	public static final String RECONCILE_NO = "n";

	/**
	 * Create a split that is not reconciled.
	 *
	 * @param guid
	 *        the GUID
	 * @param txGuid
	 *        the GUID of the transaction
	 * @param accountGuid
	 *        the GUID of the account
	 * @param memo
	 *        the memo
	 * @param action
	 *        the action
	 * @param value
	 *        the value, in the transaction currency
	 * @param quantity
	 *        the amount, in the account commodity
	 * @param lotGuid
	 *        the GUID of the lot the split belongs to
	 * @return the split
	 */
	public static GncSplit unreconciled(String guid, String txGuid, String accountGuid, String memo,
			String action, GncNumeric value, GncNumeric quantity, @Nullable String lotGuid) {
		return new GncSplit(guid, txGuid, accountGuid, memo, action, RECONCILE_NO, Instant.EPOCH,
				value, quantity, lotGuid);
	}

}
