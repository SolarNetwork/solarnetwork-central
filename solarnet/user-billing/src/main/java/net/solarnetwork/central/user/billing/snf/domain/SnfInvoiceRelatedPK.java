/* ==================================================================
 * SnfInvoiceRelatedPK.java - 28/05/2021 6:42:18 AM
 * 
 * Copyright 2021 SolarNetwork.net Dev Team
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

import java.io.Serializable;
import java.util.Objects;

/**
 * A primary key for an object related to an invoice.
 * 
 * @author matt
 * @version 1.0
 * @since 2.0
 */
public class SnfInvoiceRelatedPK implements Serializable, Cloneable, Comparable<SnfInvoiceRelatedPK> {

	private static final long serialVersionUID = -8685042516164424904L;

	private final Long id;
	private final Long invoiceId;

	/**
	 * Construct with values.
	 * 
	 * @param invoiceId
	 *        the user ID
	 * @param id
	 *        the ID
	 */
	public SnfInvoiceRelatedPK(Long invoiceId, Long id) {
		super();
		this.id = id;
		this.invoiceId = invoiceId;
	}

	/**
	 * Compare two {@code SnfInvoiceRelatedPK} objects. Keys are ordered based
	 * on:
	 * 
	 * <ol>
	 * <li>invoiceId</li>
	 * <li>id</li>
	 * </ol>
	 * 
	 * {@literal null} values will be sorted before non-{@literal null} values.
	 */
	@Override
	public int compareTo(SnfInvoiceRelatedPK o) {
		if ( o == null ) {
			return 1;
		}
		if ( o.invoiceId == null ) {
			return 1;
		} else if ( invoiceId == null ) {
			return -1;
		}
		int comparison = invoiceId.compareTo(o.invoiceId);
		if ( comparison != 0 ) {
			return comparison;
		}
		if ( o.id == null ) {
			return 1;
		} else if ( id == null ) {
			return -1;
		}
		return id.compareTo(o.id);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("SnfInvoiceRelatedPK{");
		if ( invoiceId != null ) {
			builder.append("invoiceId=");
			builder.append(invoiceId);
			builder.append(", ");
		}
		if ( id != null ) {
			builder.append("id=");
			builder.append(id);
		}
		builder.append("}");
		return builder.toString();
	}

	@Override
	protected SnfInvoiceRelatedPK clone() {
		try {
			return (SnfInvoiceRelatedPK) super.clone();
		} catch ( CloneNotSupportedException e ) {
			// shouldn't get here
			throw new RuntimeException(e);
		}
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, invoiceId);
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( !(obj instanceof SnfInvoiceRelatedPK) ) {
			return false;
		}
		SnfInvoiceRelatedPK other = (SnfInvoiceRelatedPK) obj;
		return Objects.equals(id, other.id) && Objects.equals(invoiceId, other.invoiceId);
	}

	/**
	 * Get the related ID.
	 * 
	 * @return the related ID
	 */
	public Long getId() {
		return id;
	}

	/**
	 * Get the invoice ID.
	 * 
	 * @return the invoice ID
	 */
	public Long getInvoiceId() {
		return invoiceId;
	}

}
