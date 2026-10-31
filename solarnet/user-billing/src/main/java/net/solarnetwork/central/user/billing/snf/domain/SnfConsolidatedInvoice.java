/* ==================================================================
 * SnfConsolidatedInvoice.java - 15/10/2026 8:40:12 am
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

package net.solarnetwork.central.user.billing.snf.domain;

import static net.solarnetwork.central.domain.UserLongCompositePK.unassignedEntityIdKey;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.io.Serial;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import com.fasterxml.jackson.annotation.JsonIgnore;
import net.solarnetwork.central.dao.UserRelatedEntity;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.dao.BasicEntity;
import net.solarnetwork.domain.CopyingIdentity;
import net.solarnetwork.domain.Differentiable;

/**
 * SNF consolidated invoice entity.
 *
 * <p>
 * A consolidated invoice groups the invoices of a set of linked accounts for
 * one period, and is billed to one of them: the primary account. It has no
 * items of its own. Its user ID and account ID are those of the primary
 * account, and its invoice ID comes from the same sequence as account invoices,
 * so its invoice number is unique among all invoices.
 * </p>
 *
 * <p>
 * The {@code invoiceIds} property holds the IDs of the account invoices that
 * make up the consolidated invoice, in invoice ID order. The {@code invoices}
 * property can hold the account invoices themselves, when they have been
 * loaded.
 * </p>
 *
 * @author matt
 * @version 1.0
 */
public class SnfConsolidatedInvoice extends BasicEntity<UserLongCompositePK>
		implements UserRelatedEntity<UserLongCompositePK>,
		CopyingIdentity<SnfConsolidatedInvoice, UserLongCompositePK>,
		Differentiable<SnfConsolidatedInvoice> {

	@Serial
	private static final long serialVersionUID = -4316707467787468183L;

	private final Long accountId;
	private final LocalDate startDate;
	private final LocalDate endDate;
	private final String currencyCode;
	private @Nullable Address address;
	private @Nullable List<UserLongCompositePK> invoiceIds;
	private @Nullable List<SnfInvoice> invoices;

	// used by DAO insert to return assigned key
	private transient @Nullable Long configId;

	/**
	 * Constructor.
	 *
	 * @param id
	 *        the ID
	 * @param accountId
	 *        the primary account ID
	 * @param created
	 *        the creation date
	 * @param startDate
	 *        the start date
	 * @param endDate
	 *        the end date
	 * @param currencyCode
	 *        the currency code
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public SnfConsolidatedInvoice(UserLongCompositePK id, Long accountId, Instant created,
			LocalDate startDate, LocalDate endDate, String currencyCode) {
		super(requireNonNullArgument(id, "id"), requireNonNullArgument(created, "created"));
		this.accountId = requireNonNullArgument(accountId, "accountId");
		this.startDate = requireNonNullArgument(startDate, "startDate");
		this.endDate = requireNonNullArgument(endDate, "endDate");
		this.currencyCode = requireNonNullArgument(currencyCode, "currencyCode");
	}

	/**
	 * Constructor.
	 *
	 * @param accountId
	 *        the primary account ID
	 * @param userId
	 *        the primary account user ID
	 * @param created
	 *        the creation date
	 * @param startDate
	 *        the start date
	 * @param endDate
	 *        the end date
	 * @param currencyCode
	 *        the currency code
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public SnfConsolidatedInvoice(Long accountId, Long userId, Instant created, LocalDate startDate,
			LocalDate endDate, String currencyCode) {
		this(unassignedEntityIdKey(userId), accountId, created, startDate, endDate, currencyCode);
	}

	/**
	 * Constructor.
	 *
	 * @param id
	 *        the invoice ID
	 * @param userId
	 *        the primary account user ID
	 * @param accountId
	 *        the primary account ID
	 * @param created
	 *        the creation date
	 * @param startDate
	 *        the start date
	 * @param endDate
	 *        the end date
	 * @param currencyCode
	 *        the currency code
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public SnfConsolidatedInvoice(Long id, Long userId, Long accountId, Instant created,
			LocalDate startDate, LocalDate endDate, String currencyCode) {
		this(new UserLongCompositePK(userId, id), accountId, created, startDate, endDate, currencyCode);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("SnfConsolidatedInvoice{");
		builder.append("invoiceId=");
		if ( id().entityIdIsAssigned() ) {
			builder.append(id().getEntityId());
		}
		builder.append(", accountId=");
		builder.append(accountId);
		builder.append(", startDate=");
		builder.append(startDate);
		builder.append(", invoiceIds=");
		builder.append(invoiceIds);
		if ( invoices != null ) {
			builder.append(", totalAmount=");
			builder.append(getTotalAmount().setScale(2, RoundingMode.HALF_UP));
		}
		builder.append("}");
		return builder.toString();
	}

	/**
	 * Get the total amount of all account invoices.
	 *
	 * @return the total amount of the loaded account invoices, or {@code 0} if
	 *         they have not been loaded
	 * @see #getInvoices()
	 */
	public BigDecimal getTotalAmount() {
		BigDecimal result = BigDecimal.ZERO;
		final List<SnfInvoice> invoices = getInvoices();
		if ( invoices != null ) {
			result = invoices.stream().map(SnfInvoice::getTotalAmount).reduce(result, BigDecimal::add);
		}
		return result;
	}

	/**
	 * Get the invoice time zone.
	 *
	 * <p>
	 * This returns the time zone of the configured address.
	 * </p>
	 *
	 * @return the time zone, or {@code null} if not available
	 */
	@JsonIgnore
	public @Nullable ZoneId getTimeZone() {
		Address addr = getAddress();
		return (addr != null ? addr.getTimeZone() : null);
	}

	@Override
	public boolean hasId() {
		return id().entityIdIsAssigned() || configId != null;
	}

	@Override
	public final Long getUserId() {
		return id().getUserId();
	}

	/**
	 * Get the invoice ID.
	 *
	 * @return the invoice ID
	 * @throws IllegalStateException
	 *         if the invoice ID is not available
	 */
	@JsonIgnore
	public final Long getInvoiceId() {
		final var pk = id();
		Long invoiceId = (pk.entityIdIsAssigned() ? pk.getEntityId() : configId);
		if ( invoiceId != null ) {
			return invoiceId;
		}
		throw new IllegalStateException("Invoice ID is not available.");
	}

	/**
	 * Set the invoice ID.
	 *
	 * @param invoiceId
	 *        the invoice ID to set
	 * @throws IllegalArgumentException
	 *         if the invoice is already assigned and {@code invoiceId} differs
	 */
	@JsonIgnore
	public final void setInvoiceId(Long invoiceId) {
		final var pk = id();
		if ( pk.entityIdIsAssigned() ) {
			if ( !pk.getEntityId().equals(invoiceId) ) {
				throw new IllegalArgumentException("Cannot change the invoice ID from %d to %d."
						.formatted(pk.getEntityId(), invoiceId));
			}
			return;
		}
		this.configId = invoiceId;
	}

	@Override
	public SnfConsolidatedInvoice copyWithId(UserLongCompositePK id) {
		var copy = new SnfConsolidatedInvoice(requireNonNullArgument(id, "id"), accountId, created(),
				startDate, endDate, currencyCode);
		copyTo(copy);
		return copy;
	}

	@Override
	public void copyTo(SnfConsolidatedInvoice other) {
		if ( other == null ) {
			return;
		}
		if ( address != null ) {
			other.address = address.clone();
		}
		if ( invoiceIds != null ) {
			other.invoiceIds = new ArrayList<>(invoiceIds);
		}
		if ( invoices != null ) {
			other.invoices = new ArrayList<>(invoices);
		}
	}

	@Override
	public SnfConsolidatedInvoice clone() {
		return (SnfConsolidatedInvoice) super.clone();
	}

	/**
	 * Test if the properties of another entity are the same as in this
	 * instance.
	 *
	 * <p>
	 * The {@code id}, {@code created}, and {@code invoices} properties are not
	 * compared by this method. The {@code address} property is compared by ID
	 * only.
	 * </p>
	 *
	 * @param other
	 *        the other entity to compare to
	 * @return {@literal true} if the properties of this instance are equal to
	 *         the other
	 */
	@SuppressWarnings("ReferenceEquality")
	public boolean isSameAs(@Nullable SnfConsolidatedInvoice other) {
		if ( other == null ) {
			return false;
		}
		// @formatter:off
		boolean same = Objects.equals(accountId, other.accountId)
				&& Objects.equals(currencyCode, other.currencyCode)
				&& Objects.equals(startDate, other.startDate)
				&& Objects.equals(endDate, other.endDate)
				&& Objects.equals(getInvoiceIds(), other.getInvoiceIds());
		// @formatter:on
		if ( !same ) {
			return false;
		}
		if ( address != other.address ) {
			return (address != null && other.address != null
					&& Objects.equals(address.getId(), other.address.getId()));
		}
		return true;
	}

	@Override
	public boolean differsFrom(@Nullable SnfConsolidatedInvoice other) {
		return !isSameAs(other);
	}

	/**
	 * Get the primary account ID.
	 *
	 * @return the account ID, never {@code null}
	 */
	public final Long getAccountId() {
		return accountId;
	}

	/**
	 * Get the address.
	 *
	 * @return the address
	 */
	public final @Nullable Address getAddress() {
		return address;
	}

	/**
	 * Set the address.
	 *
	 * @param address
	 *        the address to set
	 */
	public final void setAddress(@Nullable Address address) {
		this.address = address;
	}

	/**
	 * Get the starting date.
	 *
	 * @return the starting date (inclusive)
	 */
	public final LocalDate getStartDate() {
		return startDate;
	}

	/**
	 * Get the ending date.
	 *
	 * @return the ending date (exclusive)
	 */
	public final LocalDate getEndDate() {
		return endDate;
	}

	/**
	 * Get the currency code.
	 *
	 * @return the currency code, never {@code null}
	 */
	public final String getCurrencyCode() {
		return currencyCode;
	}

	/**
	 * Get the IDs of the account invoices that make up this consolidated
	 * invoice.
	 *
	 * @return the account invoice IDs, in invoice ID order
	 */
	public final @Nullable List<UserLongCompositePK> getInvoiceIds() {
		return invoiceIds;
	}

	/**
	 * Set the IDs of the account invoices that make up this consolidated
	 * invoice.
	 *
	 * @param invoiceIds
	 *        the account invoice IDs to set
	 */
	public final void setInvoiceIds(@Nullable List<UserLongCompositePK> invoiceIds) {
		this.invoiceIds = invoiceIds;
	}

	/**
	 * Get the account invoices that make up this consolidated invoice.
	 *
	 * @return the account invoices, or {@code null} if not loaded
	 */
	public final @Nullable List<SnfInvoice> getInvoices() {
		return invoices;
	}

	/**
	 * Set the account invoices that make up this consolidated invoice.
	 *
	 * <p>
	 * This also replaces the account invoice IDs with the IDs of the given
	 * invoices.
	 * </p>
	 *
	 * @param invoices
	 *        the account invoices to set
	 */
	public final void setInvoices(@Nullable List<SnfInvoice> invoices) {
		this.invoices = invoices;
		if ( invoices != null ) {
			List<UserLongCompositePK> ids = new ArrayList<>(invoices.size());
			for ( SnfInvoice invoice : invoices ) {
				ids.add(invoice.id());
			}
			this.invoiceIds = ids;
		}
	}

	/**
	 * Get the number of account invoices.
	 *
	 * @return the number of account invoices
	 */
	public final int getInvoiceCount() {
		return (invoiceIds != null ? invoiceIds.size() : 0);
	}

}
