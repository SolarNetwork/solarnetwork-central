/* ==================================================================
 * Payment.java - 29/07/2020 7:01:06 AM
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
import java.util.Comparator;
import java.util.Objects;
import java.util.UUID;
import net.solarnetwork.central.dao.UserUuidPK;
import net.solarnetwork.central.user.dao.UserRelatedEntity;
import net.solarnetwork.dao.BasicEntity;
import net.solarnetwork.domain.Differentiable;

/**
 * Payment entity.
 * 
 * @author matt
 * @version 2.0
 */
public class Payment extends BasicEntity<UserUuidPK>
		implements Differentiable<Payment>, UserRelatedEntity<UserUuidPK> {

	private static final long serialVersionUID = 3181781610133454160L;

	/**
	 * Comparator that sorts {@link Payment} objects by {@code created} in
	 * ascending order.
	 */
	public static final Comparator<Payment> SORT_BY_DATE = new PaymentDateComparator();

	/**
	 * Compare {@link Payment} instances by start date in ascending order.
	 */
	public static final class PaymentDateComparator implements Comparator<Payment> {

		@Override
		public int compare(Payment o1, Payment o2) {
			int result = o1.getCreated().compareTo(o2.getCreated());
			if ( result == 0 ) {
				result = o1.getId().compareTo(o2.getId());
			}
			return result;
		}

	}

	private final Long accountId;
	private PaymentType paymentType;
	private BigDecimal amount;
	private String currencyCode;
	private String externalKey;
	private String reference;

	/**
	 * Constructor.
	 * 
	 * @param accountId
	 *        the account ID
	 */
	public Payment(Long accountId) {
		super(new UserUuidPK(), Instant.now());
		this.accountId = accountId;
	}

	/**
	 * Constructor.
	 * 
	 * @param id
	 *        the ID
	 * @param accountId
	 *        the account ID
	 * @param created
	 *        the creation date
	 */
	public Payment(UserUuidPK id, Long accountId, Instant created) {
		super(id, created);
		this.accountId = accountId;
	}

	/**
	 * Constructor.
	 * 
	 * @param id
	 *        the ID
	 * @param userId
	 *        the user ID
	 * @param accountId
	 *        the account ID
	 * @param created
	 *        the creation date
	 */
	public Payment(UUID id, Long userId, Long accountId, Instant created) {
		this(new UserUuidPK(userId, id), accountId, created);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Payment{id=");
		builder.append(getId());
		builder.append(", created=");
		builder.append(getCreated());
		builder.append(", accountId=");
		builder.append(accountId);
		builder.append(", paymentType=");
		builder.append(paymentType);
		builder.append(", amount=");
		builder.append(amount);
		builder.append(", currencyCode=");
		builder.append(currencyCode);
		builder.append(", externalKey=");
		builder.append(externalKey);
		builder.append(", reference=");
		builder.append(reference);
		builder.append("}");
		return builder.toString();
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
	public boolean isSameAs(Payment other) {
		if ( other == null ) {
			return false;
		}
		// @formatter:off
		return Objects.equals(accountId, other.accountId)
				&& (amount == other.amount) || (amount != null && amount.compareTo(other.amount) == 0)
				&& Objects.equals(currencyCode, other.currencyCode)
				&& Objects.equals(externalKey, other.externalKey)
				&& Objects.equals(paymentType, other.paymentType)
				&& Objects.equals(reference, other.reference);
		// @formatter:on
	}

	@Override
	public boolean differsFrom(Payment other) {
		return !isSameAs(other);
	}

	@Override
	public boolean hasId() {
		UserUuidPK id = getId();
		return (id != null && id.getId() != null && id.getUserId() != null);
	}

	@Override
	public Long getUserId() {
		final UserUuidPK id = getId();
		return id != null ? id.getUserId() : null;
	}

	/**
	 * Get the account ID.
	 * 
	 * @return the account ID
	 */
	public Long getAccountId() {
		return accountId;
	}

	/**
	 * Get the payment type.
	 * 
	 * @return the type
	 */
	public PaymentType getPaymentType() {
		return paymentType;
	}

	/**
	 * Set the payment type.
	 * 
	 * @param paymentType
	 *        the type to set
	 */
	public void setPaymentType(PaymentType paymentType) {
		this.paymentType = paymentType;
	}

	/**
	 * Get the amount.
	 * 
	 * @return the amount
	 */
	public BigDecimal getAmount() {
		return amount;
	}

	/**
	 * Set the amount.
	 * 
	 * @param amount
	 *        the amount to set
	 */
	public void setAmount(BigDecimal amount) {
		this.amount = amount;
	}

	/**
	 * Get the currency code.
	 * 
	 * @return the currency code
	 */
	public String getCurrencyCode() {
		return currencyCode;
	}

	/**
	 * Set the currency code.
	 * 
	 * @param currencyCode
	 *        the currency code to set
	 */
	public void setCurrencyCode(String currencyCode) {
		this.currencyCode = currencyCode;
	}

	/**
	 * Get the external key.
	 * 
	 * <p>
	 * This refers to a payment identifier in some external system, for cross
	 * referencing.
	 * </p>
	 * 
	 * @return the external key
	 */
	public String getExternalKey() {
		return externalKey;
	}

	/**
	 * Set the external key.
	 * 
	 * @param externalKey
	 *        the key to set
	 */
	public void setExternalKey(String externalKey) {
		this.externalKey = externalKey;
	}

	/**
	 * Get the reference.
	 * 
	 * <p>
	 * This is arbitrary information associated with the payment.
	 * </p>
	 * 
	 * @return the reference
	 */
	public String getReference() {
		return reference;
	}

	/**
	 * Set the reference.
	 * 
	 * @param reference
	 *        the reference to set
	 */
	public void setReference(String reference) {
		this.reference = reference;
	}

}
