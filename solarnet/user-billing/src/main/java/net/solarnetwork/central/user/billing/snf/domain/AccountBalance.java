/* ==================================================================
 * AccountBalance.java - 29/07/2020 8:56:01 AM
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

import java.math.BigDecimal;
import java.time.Instant;
import net.solarnetwork.central.user.dao.UserRelatedEntity;
import net.solarnetwork.central.user.domain.UserLongPK;
import net.solarnetwork.dao.BasicEntity;
import net.solarnetwork.domain.Differentiable;

/**
 * Account balance entity.
 * 
 * <p>
 * The ID component of {@link #getId()} is an {@link Account} ID.
 * </p>
 * 
 * @author matt
 * @version 1.1
 */
public class AccountBalance extends BasicEntity<UserLongPK>
		implements UserRelatedEntity<UserLongPK>, Differentiable<AccountBalance> {

	private static final long serialVersionUID = -7116991858593931605L;

	/** A key to use for credit used in an invoice. */
	public static final String ACCOUNT_CREDIT_KEY = "account-credit";

	private final BigDecimal chargeTotal;
	private final BigDecimal paymentTotal;
	private final BigDecimal availableCredit;

	/**
	 * Constructor.
	 * 
	 * @param accountId
	 *        the account ID
	 */
	public AccountBalance(Long accountId) {
		this(new UserLongPK(null, accountId), Instant.now(), BigDecimal.ZERO, BigDecimal.ZERO,
				BigDecimal.ZERO);
	}

	/**
	 * Constructor.
	 * 
	 * @param id
	 *        the ID
	 * @param created
	 *        the creation date
	 * @param chargeTotal
	 *        the charge total; {@literal null} will be stored as {@literal 0}
	 * @param paymentTotal
	 *        the payment total; {@literal null} will be stored as {@literal 0}
	 * @param availableCredit
	 *        the available credit; {@literal null} will be stored as
	 *        {@literal 0}
	 */
	public AccountBalance(UserLongPK id, Instant created, BigDecimal chargeTotal,
			BigDecimal paymentTotal, BigDecimal availableCredit) {
		super(id, created);
		this.chargeTotal = (chargeTotal != null ? chargeTotal : BigDecimal.ZERO);
		this.paymentTotal = (paymentTotal != null ? paymentTotal : BigDecimal.ZERO);
		this.availableCredit = (availableCredit != null ? availableCredit : BigDecimal.ZERO);
	}

	/**
	 * Constructor.
	 * 
	 * @param accountId
	 *        the account ID
	 * @param userId
	 *        the user ID
	 * @param created
	 *        the creation date
	 * @param chargeTotal
	 *        the charge total; {@literal null} will be stored as {@literal 0}
	 * @param paymentTotal
	 *        the payment total; {@literal null} will be stored as {@literal 0}
	 * @param availableCredit
	 *        the available credit; {@literal null} will be stored as
	 *        {@literal 0}
	 */
	public AccountBalance(Long accountId, Long userId, Instant created, BigDecimal chargeTotal,
			BigDecimal paymentTotal, BigDecimal availableCredit) {
		this(new UserLongPK(userId, accountId), created, chargeTotal, paymentTotal, availableCredit);
	}

	@Override
	public boolean hasId() {
		UserLongPK id = getId();
		return (id != null && id.getId() != null && id.getUserId() != null);
	}

	@Override
	public Long getUserId() {
		final UserLongPK id = getId();
		return id != null ? id.getUserId() : null;
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
	public boolean isSameAs(AccountBalance other) {
		if ( other == null ) {
			return false;
		}
		// @formatter:off
		return (chargeTotal == other.chargeTotal) 
					|| (chargeTotal != null && chargeTotal.compareTo(other.chargeTotal) == 0)
				&& (paymentTotal == other.paymentTotal) 
					|| (paymentTotal != null && paymentTotal.compareTo(other.paymentTotal) == 0)
				&& (availableCredit == other.availableCredit) 
					|| (availableCredit != null && availableCredit.compareTo(other.availableCredit) == 0);
		// @formatter:on
	}

	@Override
	public boolean differsFrom(AccountBalance other) {
		return !isSameAs(other);
	}

	/**
	 * Get the charge total.
	 * 
	 * @return the charge total
	 */
	public BigDecimal getChargeTotal() {
		return chargeTotal;
	}

	/**
	 * Get the payment total.
	 * 
	 * @return the payment total
	 */
	public BigDecimal getPaymentTotal() {
		return paymentTotal;
	}

	/**
	 * Get the available credit.
	 * 
	 * @return the available credit
	 */
	public BigDecimal getAvailableCredit() {
		return availableCredit;
	}

}
