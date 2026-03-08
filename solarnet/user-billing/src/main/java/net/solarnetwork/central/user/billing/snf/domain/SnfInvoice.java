/* ==================================================================
 * SnfInvoice.java - 20/07/2020 9:37:06 AM
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

import static net.solarnetwork.util.ObjectUtils.nonnull;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.io.Serial;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import com.fasterxml.jackson.annotation.JsonIgnore;
import net.solarnetwork.central.dao.UserRelatedEntity;
import net.solarnetwork.central.user.domain.UserLongPK;
import net.solarnetwork.dao.BasicEntity;
import net.solarnetwork.domain.Differentiable;

/**
 * SNF invoice entity.
 *
 * @author matt
 * @version 1.2
 */
public class SnfInvoice extends BasicEntity<UserLongPK>
		implements UserRelatedEntity<UserLongPK>, Differentiable<SnfInvoice> {

	@Serial
	private static final long serialVersionUID = 4095505242827622567L;

	/**
	 * Comparator that sorts {@link SnfInvoice} objects by {@code startDate} in
	 * ascending order.
	 */
	public static final Comparator<SnfInvoice> SORT_BY_DATE = new SnfInvoiceStartDateComparator();

	private final Long accountId;
	private final LocalDate startDate;
	private final LocalDate endDate;
	private final String currencyCode;
	private @Nullable Address address;
	private @Nullable Set<SnfInvoiceItem> items;
	private @Nullable Set<SnfInvoiceNodeUsage> usages;

	/**
	 * Compare {@link SnfInvoice} instances by start date in ascending order.
	 */
	public static final class SnfInvoiceStartDateComparator implements Comparator<SnfInvoice> {

		@Override
		public int compare(SnfInvoice o1, SnfInvoice o2) {
			int result = o1.startDate.compareTo(o2.startDate);
			if ( result == 0 ) {
				result = nonnull(o1.getId(), "Left ID").compareTo(nonnull(o2.getId(), "Right ID"));
			}
			return result;
		}

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
	 * @param startDate
	 *        the start date
	 * @param endDate
	 *        the end date
	 * @param currencyCode
	 *        the currency code
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public SnfInvoice(UserLongPK id, Long accountId, Instant created, LocalDate startDate,
			LocalDate endDate, String currencyCode) {
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
	 *        the account ID
	 * @param userId
	 *        the user ID
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
	public SnfInvoice(Long accountId, Long userId, Instant created, LocalDate startDate,
			LocalDate endDate, String currencyCode) {
		this(new UserLongPK(userId, null), accountId, created, startDate, endDate, currencyCode);
	}

	/**
	 * Constructor.
	 *
	 * @param id
	 *        the UUID ID
	 * @param userId
	 *        the user ID
	 * @param accountId
	 *        the account ID
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
	public SnfInvoice(Long id, Long userId, Long accountId, Instant created, LocalDate startDate,
			LocalDate endDate, String currencyCode) {
		this(new UserLongPK(userId, id), accountId, created, startDate, endDate, currencyCode);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("SnfInvoice{");
		builder.append("id=");
		builder.append(getId() != null ? getId().getId() : null);
		builder.append(", accountId=");
		builder.append(accountId);
		builder.append(", startDate=");
		builder.append(startDate);
		builder.append(", itemCount=");
		builder.append(getItemCount());
		builder.append(", totalAmount=");
		builder.append(getTotalAmount().setScale(2, RoundingMode.HALF_UP));
		builder.append("}");
		return builder.toString();
	}

	/**
	 * Get the total amount of all invoice items.
	 *
	 * @return the total cost, never {@code null}
	 */
	public BigDecimal getTotalAmount() {
		BigDecimal result = BigDecimal.ZERO;
		Set<SnfInvoiceItem> items = getItems();
		if ( items != null ) {
			result = items.stream().map(SnfInvoiceItem::getAmount).reduce(result, BigDecimal::add);
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
		UserLongPK id = getId();
		return (id != null && id.getId() != null && id.getUserId() != null);
	}

	@Override
	public final Long getUserId() {
		return nonnull(getId(), "id").getUserId();
	}

	/**
	 * Set the user ID.
	 *
	 * @param userId
	 *        the user ID
	 */
	public final void setUserId(Long userId) {
		final UserLongPK id = getId();
		if ( id != null ) {
			id.setUserId(userId);
		}
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
		return nonnull(nonnull(getId(), "Invoice PK").getId(), "Invoice ID");
	}

	/**
	 * Set the invoice ID.
	 *
	 * @param invoiceId
	 *        the invoice ID to set
	 * @throws IllegalStateException
	 *         if the invoice primary key is not available
	 */
	@JsonIgnore
	public final void setInvoiceId(Long invoiceId) {
		nonnull(getId(), "Invoice PK").setId(invoiceId);
	}

	/**
	 * Test if the properties of another entity are the same as in this
	 * instance.
	 *
	 * <p>
	 * The {@code id} and {@code created} properties are not compared by this
	 * method. The {@code address} and {@code items} values are compared by
	 * primary key values only.
	 * </p>
	 *
	 * @param other
	 *        the other entity to compare to
	 * @return {@literal true} if the properties of this instance are equal to
	 *         the other
	 */
	@SuppressWarnings("ReferenceEquality")
	public boolean isSameAs(@Nullable SnfInvoice other) {
		if ( other == null ) {
			return false;
		}
		// @formatter:off
		boolean same = Objects.equals(accountId, other.accountId)
				&& Objects.equals(currencyCode, other.currencyCode)
				&& Objects.equals(startDate, other.startDate)
				&& Objects.equals(endDate, other.endDate);
		// @formatter:on
		if ( !same ) {
			return false;
		}
		if ( address != other.address ) {
			if ( address == null || other.address == null
					|| !Objects.equals(address.getId(), other.address.getId()) ) {
				return false;
			}
		}
		if ( items == other.items ) {
			return true;
		}
		if ( getItemCount() != other.getItemCount() ) {
			return false;
		}
		Map<UUID, SnfInvoiceItem> otherItems = other.itemMap();
		if ( items != null ) {
			for ( SnfInvoiceItem item : items ) {
				SnfInvoiceItem otherItem = otherItems.remove(item.getId());
				if ( item.differsFrom(otherItem) ) {
					return false;
				}
			}
		}
		if ( !otherItems.isEmpty() ) {
			return false;
		}
		if ( usages == other.usages ) {
			return true;
		}
		if ( getUsagesCount() != other.getUsagesCount() ) {
			return false;
		}
		Map<Long, SnfInvoiceNodeUsage> otherUsages = other.usageMap();
		if ( usages != null ) {
			for ( SnfInvoiceNodeUsage usage : usages ) {
				SnfInvoiceNodeUsage otherUsage = otherUsages.remove(usage.getNodeId());
				if ( usage.differsFrom(otherUsage) ) {
					return false;
				}
			}
		}
		return otherUsages.isEmpty();
	}

	@Override
	public boolean differsFrom(@Nullable SnfInvoice other) {
		return !isSameAs(other);
	}

	/**
	 * Get a map of invoice items using their ID as map keys.
	 *
	 * @return the map
	 */
	public final Map<UUID, SnfInvoiceItem> itemMap() {
		if ( items == null ) {
			return Collections.emptyMap();
		}
		return items.stream().collect(Collectors.toMap(SnfInvoiceItem::getId, e -> e));
	}

	/**
	 * Add an invoice item.
	 *
	 * @param item
	 *        the item to add
	 */
	public final void addItem(SnfInvoiceItem item) {
		Set<SnfInvoiceItem> items = getItems();
		if ( items == null ) {
			items = new LinkedHashSet<>(4);
			setItems(items);
		}
		items.add(item);
	}

	/**
	 * Get the account ID.
	 *
	 * @return the account ID
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
	 * @return the currency code
	 */
	public final String getCurrencyCode() {
		return currencyCode;
	}

	/**
	 * Get the number of invoice items.
	 *
	 * @return the number of items
	 */
	public final int getItemCount() {
		return (items != null ? items.size() : 0);
	}

	/**
	 * Get all invoice items as a map using item key values as map keys.
	 *
	 * @return the map, or {@code null} if {@link #getItems()} returns
	 *         {@code null}
	 */
	public final @Nullable Map<String, SnfInvoiceItem> getItemsByKey() {
		Map<String, SnfInvoiceItem> result = null;
		Set<SnfInvoiceItem> items = getItems();
		if ( items != null ) {
			result = new LinkedHashMap<>(items.size());
			for ( SnfInvoiceItem item : items ) {
				if ( item.getKey() != null ) {
					result.put(item.getKey(), item);
				}
			}
		}
		return result;
	}

	/**
	 * Get the invoice items.
	 *
	 * @return the items
	 */
	public final @Nullable Set<SnfInvoiceItem> getItems() {
		return items;
	}

	/**
	 * Set the invoice items.
	 *
	 * @param items
	 *        the items to set
	 */
	public final void setItems(@Nullable Set<SnfInvoiceItem> items) {
		this.items = items;
	}

	/**
	 * Get the node usage records.
	 *
	 * @return the usage records
	 * @since 1.1
	 */
	public final @Nullable Set<SnfInvoiceNodeUsage> getUsages() {
		return usages;
	}

	/**
	 * Set the node usage records.
	 *
	 * @param usages
	 *        the usages to set
	 * @since 1.1
	 */
	public final void setUsages(@Nullable Set<SnfInvoiceNodeUsage> usages) {
		this.usages = usages;
	}

	/**
	 * Get the node usage record count.
	 *
	 * @return the count
	 * @since 1.1
	 */
	public final int getUsagesCount() {
		return (usages != null ? usages.size() : 0);
	}

	/**
	 * Get a map of invoice usages using their ID as map keys.
	 *
	 * @return the map
	 */
	public final Map<Long, SnfInvoiceNodeUsage> usageMap() {
		if ( usages == null ) {
			return Collections.emptyMap();
		}
		return usages.stream().collect(Collectors.toMap(SnfInvoiceNodeUsage::getNodeId, e -> e));
	}

}
