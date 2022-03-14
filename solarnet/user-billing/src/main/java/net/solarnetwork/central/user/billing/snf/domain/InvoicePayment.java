/* ==================================================================
 * InvoicePayment.java - 29/07/2020 10:52:56 AM
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
 * Invoice payment entity.
 * 
 * @author matt
 * @version 2.0
 */
public class InvoicePayment extends BasicEntity<UserUuidPK>
		implements Differentiable<InvoicePayment>, UserRelatedEntity<UserUuidPK> {

	private static final long serialVersionUID = -8828483045770314273L;

	/**
	 * Comparator that sorts {@link Payment} objects by {@code created} in
	 * ascending order.
	 */
	public static final Comparator<InvoicePayment> SORT_BY_DATE = new InvoicePaymentDateComparator();

	/**
	 * Compare {@link InvoicePayment} instances by start date in ascending
	 * order.
	 */
	public static final class InvoicePaymentDateComparator implements Comparator<InvoicePayment> {

		@Override
		public int compare(InvoicePayment o1, InvoicePayment o2) {
			int result = o1.getCreated().compareTo(o2.getCreated());
			if ( result == 0 ) {
				result = o1.getId().compareTo(o2.getId());
			}
			return result;
		}

	}

	private final Long accountId;
	private final UUID paymentId;
	private final Long invoiceId;
	private BigDecimal amount;

	/**
	 * Constructor.
	 * 
	 * @param accountId
	 *        the account ID
	 * @param paymentId
	 *        the payment ID
	 * @param invoiceId
	 *        the invoice ID
	 */
	public InvoicePayment(Long accountId, UUID paymentId, Long invoiceId) {
		this(new UserUuidPK(), accountId, paymentId, invoiceId, Instant.now());
	}

	/**
	 * Constructor.
	 * 
	 * @param id
	 *        the ID
	 * @param accountId
	 *        the account ID
	 * @param paymentId
	 *        the payment ID
	 * @param invoiceId
	 *        the invoice ID
	 * @param created
	 *        the creation date
	 */
	public InvoicePayment(UserUuidPK id, Long accountId, UUID paymentId, Long invoiceId,
			Instant created) {
		super(id, created);
		this.accountId = accountId;
		this.paymentId = paymentId;
		this.invoiceId = invoiceId;
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
	 * @param paymentId
	 *        the payment ID
	 * @param invoiceId
	 *        the invoice ID
	 * @param created
	 *        the creation date
	 */
	public InvoicePayment(UUID id, Long userId, Long accountId, UUID paymentId, Long invoiceId,
			Instant created) {
		this(new UserUuidPK(userId, id), accountId, paymentId, invoiceId, created);
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
		builder.append(", paymentId=");
		builder.append(paymentId);
		builder.append(", invoiceId=");
		builder.append(invoiceId);
		builder.append(", amount=");
		builder.append(amount);
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
	public boolean isSameAs(InvoicePayment other) {
		if ( other == null ) {
			return false;
		}
		// @formatter:off
		return Objects.equals(accountId, other.accountId)
				&& Objects.equals(paymentId, other.paymentId)
				&& Objects.equals(invoiceId, other.invoiceId)
				&& (amount == other.amount) || (amount != null && amount.compareTo(other.amount) == 0);
		// @formatter:on
	}

	@Override
	public boolean differsFrom(InvoicePayment other) {
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
	 * Get the payment ID.
	 * 
	 * @return the payment ID
	 */
	public UUID getPaymentId() {
		return paymentId;
	}

	/**
	 * Get the invoice ID.
	 * 
	 * @return the invoice ID
	 */
	public Long getInvoiceId() {
		return invoiceId;
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

}
