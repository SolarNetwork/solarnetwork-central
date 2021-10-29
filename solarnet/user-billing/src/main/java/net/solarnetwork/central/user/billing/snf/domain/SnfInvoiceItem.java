/* ==================================================================
 * SnfInvoiceItem.java - 20/07/2020 9:39:36 AM
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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonIgnore;
import net.solarnetwork.dao.BasicEntity;
import net.solarnetwork.domain.Differentiable;

/**
 * SNF invoice item entity.
 * 
 * @author matt
 * @version 1.1
 */
public class SnfInvoiceItem extends BasicEntity<UUID> implements Differentiable<SnfInvoiceItem> {

	private static final long serialVersionUID = 3844737823712570024L;

	/** A metadata key for a node ID. */
	public static final String META_NODE_ID = "nodeId";

	/** A metadata key for a usage tier breakdown. */
	public static final String META_TIER_BREAKDOWN = "tiers";

	/**
	 * The metadata key for a {@code Map} of usage information.
	 * 
	 * <p>
	 * The usage is stored with map keys that match the properties of the
	 * {@link UsageInfo} class.
	 * </p>
	 */
	public static final String META_USAGE = "usage";

	/**
	 * A metadata key for an "available credit" number value.
	 */
	public static final String META_AVAILABLE_CREDIT = "availableCredit";

	/**
	 * A default sort ordering for invoice items within an invoice.
	 */
	public static final Comparator<SnfInvoiceItem> DEFAULT_ITEM_ORDER = new SnfInvoiceItemDefaultComparator();

	private final Long invoiceId;
	private InvoiceItemType itemType;
	private String key;
	private BigDecimal amount;
	private BigDecimal quantity;
	private Map<String, Object> metadata;

	/**
	 * Create a new invoice item.
	 * 
	 * @param invoice
	 *        the invoice to associate this item with; it must already have a
	 *        valid ID defined
	 * @param type
	 *        the type
	 * @param key
	 *        the key
	 * @param quantity
	 *        the quantity
	 * @param amount
	 *        the amount
	 * @return the new item, never {@literal null}
	 */
	public static SnfInvoiceItem newItem(SnfInvoice invoice, InvoiceItemType type, String key,
			BigDecimal quantity, BigDecimal amount) {
		return newItem(invoice.getId().getId(), type, key, quantity, amount);
	}

	/**
	 * Create a new invoice item.
	 * 
	 * @param invoiceId
	 *        the invoice ID
	 * @param type
	 *        the type
	 * @param key
	 *        the key
	 * @param quantity
	 *        the quantity
	 * @param amount
	 *        the amount
	 * @return the new item, never {@literal null}
	 */
	public static SnfInvoiceItem newItem(Long invoiceId, InvoiceItemType type, String key,
			BigDecimal quantity, BigDecimal amount) {
		return newItem(invoiceId, type, key, quantity, amount, Instant.now(), null);
	}

	/**
	 * Create a new invoice item.
	 * 
	 * @param invoiceId
	 *        the invoice ID
	 * @param type
	 *        the type
	 * @param key
	 *        the key
	 * @param quantity
	 *        the quantity
	 * @param amount
	 *        the amount
	 * @param date
	 *        the date
	 * @return the new item, never {@literal null}
	 */
	public static SnfInvoiceItem newItem(Long invoiceId, InvoiceItemType type, String key,
			BigDecimal quantity, BigDecimal amount, Instant date) {
		return newItem(invoiceId, type, key, quantity, amount, date, null);
	}

	/**
	 * Create a new invoice item.
	 * 
	 * @param invoiceId
	 *        the invoice ID
	 * @param type
	 *        the type
	 * @param key
	 *        the key
	 * @param quantity
	 *        the quantity
	 * @param amount
	 *        the amount
	 * @param date
	 *        the date
	 * @param metadata
	 *        the metadata
	 * @return the new item, never {@literal null}
	 */
	public static SnfInvoiceItem newItem(Long invoiceId, InvoiceItemType type, String key,
			BigDecimal quantity, BigDecimal amount, Instant date, Map<String, Object> metadata) {
		SnfInvoiceItem item = new SnfInvoiceItem(UUID.randomUUID(), invoiceId, date);
		item.setItemType(type);
		item.setKey(key);
		item.setAmount(amount);
		item.setQuantity(quantity);
		item.setMetadata(metadata);
		return item;
	}

	/**
	 * Constructor.
	 * 
	 * @param invoiceId
	 *        the invoice ID
	 */
	public SnfInvoiceItem(Long invoiceId) {
		super(null, Instant.now());
		this.invoiceId = invoiceId;
	}

	/**
	 * Constructor.
	 * 
	 * @param id
	 *        the invoice item ID
	 * @param invoiceId
	 *        the invoice ID
	 * @param created
	 *        the creation date
	 */
	public SnfInvoiceItem(UUID id, Long invoiceId, Instant created) {
		super(id, created);
		this.invoiceId = invoiceId;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("SnfInvoiceItem{invoiceId=");
		builder.append(invoiceId);
		builder.append(", id=");
		builder.append(getId());
		builder.append(", key=");
		builder.append(key);
		builder.append(", itemType=");
		builder.append(itemType);
		builder.append(", amount=");
		builder.append(amount);
		builder.append(", quantity=");
		builder.append(quantity);
		builder.append(", metadata=");
		builder.append(metadata);
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
	public boolean isSameAs(SnfInvoiceItem other) {
		if ( other == null ) {
			return false;
		}
		// @formatter:off
		return Objects.equals(invoiceId, other.invoiceId)
				&& Objects.equals(key, other.key)
				&& Objects.equals(itemType, other.itemType)
				&& Objects.equals(quantity, other.quantity)
				&& (amount == other.amount) || (amount != null && amount.compareTo(other.amount) == 0)
				&& Objects.equals(metadata, other.metadata);
		// @formatter:on
	}

	@Override
	public boolean differsFrom(SnfInvoiceItem other) {
		return !isSameAs(other);
	}

	/**
	 * Get usage information, if available.
	 * 
	 * @return the usage info, or {@literal null} if none available
	 */
	@SuppressWarnings("unchecked")
	@JsonIgnore
	public UsageInfo getUsageInfo() {
		Map<String, ?> usage = (metadata != null ? (Map<String, ?>) metadata.get(META_USAGE) : null);
		List<Map<String, ?>> tiers = (metadata != null
				? (List<Map<String, ?>>) metadata.get(META_TIER_BREAKDOWN)
				: null);
		return UsageInfo.of(usage, tiers);
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
	 * Get the item key.
	 * 
	 * <p>
	 * This is a unique identifier defined externally, such as a product key.
	 * </p>
	 * 
	 * @return the key
	 */
	public String getKey() {
		return key;
	}

	/**
	 * Set the item key.
	 * 
	 * @param key
	 *        the key to set
	 */
	public void setKey(String key) {
		this.key = key;
	}

	/**
	 * Get the item type.
	 * 
	 * @return the type
	 */
	public InvoiceItemType getItemType() {
		return itemType;
	}

	/**
	 * Set the item type.
	 * 
	 * @param itemType
	 *        the type to set
	 */
	public void setItemType(InvoiceItemType itemType) {
		this.itemType = itemType;
	}

	/**
	 * Get the amount.
	 * 
	 * <p>
	 * This value represents the total cost for the associated
	 * {@link #getQuantity()}. Thus the individual quantity cost is derived via
	 * {@code amount / quantity}. The {@link #getUnitQuantityAmount()} returns
	 * the individual quantity cost.
	 * </p>
	 * 
	 * @return the amount, never {@literal null}
	 */
	public BigDecimal getAmount() {
		return amount != null ? amount : BigDecimal.ZERO;
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
	 * Get the individual quantity cost.
	 * 
	 * <p>
	 * This returns {@code amount / quantity}. If {@code quantity} is
	 * {@literal 0} then {@code amount} is returned.
	 * </p>
	 * 
	 * @return the cost per individual quantity
	 */
	public BigDecimal getUnitQuantityAmount() {
		BigDecimal amount = getAmount();
		BigDecimal quantity = getQuantity();
		if ( quantity.compareTo(BigDecimal.ZERO) == 0 ) {
			return amount;
		}
		return amount.divide(quantity);
	}

	/**
	 * Get the quantity.
	 * 
	 * @return the quantity, never {@literal null}
	 */
	public BigDecimal getQuantity() {
		return quantity != null ? quantity : BigDecimal.ONE;
	}

	/**
	 * Set the quantity.
	 * 
	 * @param quantity
	 *        the quantity to set
	 */
	public void setQuantity(BigDecimal quantity) {
		this.quantity = quantity;
	}

	/**
	 * The item metadata.
	 * 
	 * @return the metadata
	 */
	public Map<String, Object> getMetadata() {
		return metadata;
	}

	/**
	 * The item metadata.
	 * 
	 * @param metadata
	 *        the metadata to set
	 */
	public void setMetadata(Map<String, Object> metadata) {
		this.metadata = metadata;
	}

}
