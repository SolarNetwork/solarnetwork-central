/* ==================================================================
 * SnfInvoiceNodeUsage.java - 28/05/2021 6:37:24 AM
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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.solarnetwork.central.user.billing.domain.InvoiceItemUsageRecord;
import net.solarnetwork.central.user.billing.domain.InvoiceUsageRecord;
import net.solarnetwork.dao.BasicEntity;
import net.solarnetwork.domain.Differentiable;

/**
 * Invoice node usage details.
 * 
 * @author matt
 * @version 1.4
 */
public class SnfInvoiceNodeUsage extends BasicEntity<SnfInvoiceRelatedPK>
		implements Differentiable<SnfInvoiceNodeUsage>, InvoiceUsageRecord<Long> {

	private static final long serialVersionUID = -2984725016470283705L;

	private final String description;
	private final BigInteger datumPropertiesIn;
	private final BigInteger datumOut;
	private final BigInteger datumDaysStored;
	private final BigInteger instructionsIssued;

	/**
	 * Create new instance.
	 * 
	 * @param invoiceId
	 *        the invoice ID
	 * @param nodeId
	 *        the node ID
	 * @param created
	 *        the creation date
	 * @param datumPropertiesIn
	 *        the datum properties in count
	 * @param datumOut
	 *        the datum out count
	 * @param datumDaysStored
	 *        the datum days stored count
	 * @param instructionsIssued
	 *        the instructions issued count
	 * @return the new instance
	 */
	public static SnfInvoiceNodeUsage nodeUsage(Long invoiceId, Long nodeId, Instant created,
			long datumPropertiesIn, long datumOut, long datumDaysStored, long instructionsIssued) {
		return new SnfInvoiceNodeUsage(invoiceId, nodeId, created, BigInteger.valueOf(datumPropertiesIn),
				BigInteger.valueOf(datumOut), BigInteger.valueOf(datumDaysStored),
				BigInteger.valueOf(instructionsIssued));
	}

	/**
	 * Constructor.
	 * 
	 * <p>
	 * Any {@literal null} count will be stored as {@link BigInteger#ZERO}.
	 * </p>
	 * 
	 * @param invoiceId
	 *        the invoice ID
	 * @param nodeId
	 *        the node ID
	 * @param created
	 *        the creation date
	 * @param datumPropertiesIn
	 *        the datum properties in count
	 * @param datumOut
	 *        the datum out count
	 * @param datumDaysStored
	 *        the datum days stored count
	 */
	public SnfInvoiceNodeUsage(Long invoiceId, Long nodeId, Instant created,
			BigInteger datumPropertiesIn, BigInteger datumOut, BigInteger datumDaysStored) {
		this(new SnfInvoiceRelatedPK(invoiceId, nodeId), created, datumPropertiesIn, datumOut,
				datumDaysStored);
	}

	/**
	 * Constructor.
	 * 
	 * <p>
	 * Any {@literal null} count will be stored as {@link BigInteger#ZERO}.
	 * </p>
	 * 
	 * @param invoiceId
	 *        the invoice ID
	 * @param nodeId
	 *        the node ID
	 * @param created
	 *        the creation date
	 * @param datumPropertiesIn
	 *        the datum properties in count
	 * @param datumOut
	 *        the datum out count
	 * @param datumDaysStored
	 *        the datum days stored count
	 * @param datumDaysStored
	 *        the datum days stored count
	 * @since 1.4
	 */
	public SnfInvoiceNodeUsage(Long invoiceId, Long nodeId, Instant created,
			BigInteger datumPropertiesIn, BigInteger datumOut, BigInteger datumDaysStored,
			BigInteger instructionsIssued) {
		this(new SnfInvoiceRelatedPK(invoiceId, nodeId), created, datumPropertiesIn, datumOut,
				datumDaysStored, instructionsIssued);
	}

	/**
	 * Constructor.
	 * 
	 * <p>
	 * Any {@literal null} count will be stored as {@link BigInteger#ZERO}.
	 * </p>
	 * 
	 * @param invoiceId
	 *        the invoice ID
	 * @param nodeId
	 *        the node ID
	 * @param created
	 *        the creation date
	 * @param description
	 *        the description
	 * @param datumPropertiesIn
	 *        the datum properties in count
	 * @param datumOut
	 *        the datum out count
	 * @param datumDaysStored
	 *        the datum days stored count
	 */
	public SnfInvoiceNodeUsage(Long invoiceId, Long nodeId, Instant created, String description,
			BigInteger datumPropertiesIn, BigInteger datumOut, BigInteger datumDaysStored) {
		this(new SnfInvoiceRelatedPK(invoiceId, nodeId), created, description, datumPropertiesIn,
				datumOut, datumDaysStored);
	}

	/**
	 * Constructor.
	 * 
	 * <p>
	 * Any {@literal null} count will be stored as {@link BigInteger#ZERO}.
	 * </p>
	 * 
	 * @param invoiceId
	 *        the invoice ID
	 * @param nodeId
	 *        the node ID
	 * @param created
	 *        the creation date
	 * @param description
	 *        the description
	 * @param datumPropertiesIn
	 *        the datum properties in count
	 * @param datumOut
	 *        the datum out count
	 * @param datumDaysStored
	 *        the datum days stored count
	 * @param instructionsIssued
	 *        the instruction issued count
	 * @since 1.4
	 */
	public SnfInvoiceNodeUsage(Long invoiceId, Long nodeId, Instant created, String description,
			BigInteger datumPropertiesIn, BigInteger datumOut, BigInteger datumDaysStored,
			BigInteger instructionsIssued) {
		this(new SnfInvoiceRelatedPK(invoiceId, nodeId), created, description, datumPropertiesIn,
				datumOut, datumDaysStored, instructionsIssued);
	}

	/**
	 * Constructor.
	 * 
	 * <p>
	 * Any {@literal null} count will be stored as {@link BigInteger#ZERO}.
	 * </p>
	 * 
	 * @param id
	 *        the ID; the related ID is a node ID
	 * @param created
	 *        the creation date
	 * @param datumPropertiesIn
	 *        the datum properties in count
	 * @param datumOut
	 *        the datum out count
	 * @param datumDaysStored
	 *        the datum days stored count
	 */
	public SnfInvoiceNodeUsage(SnfInvoiceRelatedPK id, Instant created, BigInteger datumPropertiesIn,
			BigInteger datumOut, BigInteger datumDaysStored) {
		this(id, created, (String) null, datumPropertiesIn, datumOut, datumDaysStored);
	}

	/**
	 * Constructor.
	 * 
	 * <p>
	 * Any {@literal null} count will be stored as {@link BigInteger#ZERO}.
	 * </p>
	 * 
	 * @param id
	 *        the ID; the related ID is a node ID
	 * @param created
	 *        the creation date
	 * @param datumPropertiesIn
	 *        the datum properties in count
	 * @param datumOut
	 *        the datum out count
	 * @param datumDaysStored
	 *        the datum days stored count
	 * @param instructionsIssued
	 *        the instructions issued count
	 * @since 1.4
	 */
	public SnfInvoiceNodeUsage(SnfInvoiceRelatedPK id, Instant created, BigInteger datumPropertiesIn,
			BigInteger datumOut, BigInteger datumDaysStored, BigInteger instructionsIssued) {
		this(id, created, null, datumPropertiesIn, datumOut, datumDaysStored, instructionsIssued);
	}

	/**
	 * Constructor.
	 * 
	 * <p>
	 * Any {@literal null} count will be stored as {@link BigInteger#ZERO}.
	 * </p>
	 * 
	 * @param id
	 *        the ID; the related ID is a node ID
	 * @param description
	 *        the description
	 * @param created
	 *        the creation date
	 * @param datumPropertiesIn
	 *        the datum properties in count
	 * @param datumOut
	 *        the datum out count
	 * @param datumDaysStored
	 *        the datum days stored count
	 */
	public SnfInvoiceNodeUsage(SnfInvoiceRelatedPK id, Instant created, String description,
			BigInteger datumPropertiesIn, BigInteger datumOut, BigInteger datumDaysStored) {
		this(id, created, description, datumPropertiesIn, datumOut, datumDaysStored, null);
	}

	/**
	 * Constructor.
	 * 
	 * <p>
	 * Any {@literal null} count will be stored as {@link BigInteger#ZERO}.
	 * </p>
	 * 
	 * @param id
	 *        the ID; the related ID is a node ID
	 * @param description
	 *        the description
	 * @param created
	 *        the creation date
	 * @param datumPropertiesIn
	 *        the datum properties in count
	 * @param datumOut
	 *        the datum out count
	 * @param datumDaysStored
	 *        the datum days stored count
	 * @param instructionsIssued
	 *        the instructions issued count
	 * @since 1.4
	 */
	public SnfInvoiceNodeUsage(SnfInvoiceRelatedPK id, Instant created, String description,
			BigInteger datumPropertiesIn, BigInteger datumOut, BigInteger datumDaysStored,
			BigInteger instructionsIssued) {
		super(id, created);
		this.description = description;
		this.datumPropertiesIn = (datumPropertiesIn != null ? datumPropertiesIn : BigInteger.ZERO);
		this.datumOut = (datumOut != null ? datumOut : BigInteger.ZERO);
		this.datumDaysStored = (datumDaysStored != null ? datumDaysStored : BigInteger.ZERO);
		this.instructionsIssued = instructionsIssued != null ? instructionsIssued : BigInteger.ZERO;
	}

	/**
	 * Test if the properties of another entity are the same as in this
	 * instance.
	 * 
	 * <p>
	 * The {@code created} properties are not compared by this method.
	 * </p>
	 * 
	 * @param other
	 *        the other entity to compare to
	 * @return {@literal true} if the properties of this instance are equal to
	 *         the other
	 */
	public boolean isSameAs(SnfInvoiceNodeUsage other) {
		if ( other == null ) {
			return false;
		}
		// @formatter:off
		return Objects.equals(getId(), other.getId())
				&& Objects.equals(datumPropertiesIn, other.datumPropertiesIn)
				&& Objects.equals(datumOut, other.datumOut)
				&& Objects.equals(datumDaysStored, other.datumDaysStored)
				&& Objects.equals(instructionsIssued, other.instructionsIssued);
		// @formatter:on
	}

	@Override
	public boolean differsFrom(SnfInvoiceNodeUsage other) {
		return !isSameAs(other);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("SnfInvoiceNodeUsage{");
		if ( getInvoiceId() != null ) {
			builder.append("invoiceId=");
			builder.append(getInvoiceId());
			builder.append(", ");
		}
		if ( getNodeId() != null ) {
			builder.append("nodeId=");
			builder.append(getNodeId());
			builder.append(", ");
		}
		if ( datumPropertiesIn.compareTo(BigInteger.ZERO) > 0 ) {
			builder.append("datumPropertiesIn=");
			builder.append(datumPropertiesIn);
			builder.append(", ");
		}
		if ( datumOut.compareTo(BigInteger.ZERO) > 0 ) {
			builder.append("datumOut=");
			builder.append(datumOut);
			builder.append(", ");
		}
		if ( datumDaysStored.compareTo(BigInteger.ZERO) > 0 ) {
			builder.append("datumDaysStored=");
			builder.append(datumDaysStored);
		}
		if ( instructionsIssued.compareTo(BigInteger.ZERO) > 0 ) {
			builder.append("instructionsIssued=");
			builder.append(instructionsIssued);
		}
		builder.append("}");
		return builder.toString();
	}

	@Override
	public Long getUsageKey() {
		return getNodeId();
	}

	@Override
	public List<InvoiceItemUsageRecord> getUsageRecords() {
		List<InvoiceItemUsageRecord> recs = new ArrayList<>(3);
		recs.add(new UsageInfo(NodeUsages.DATUM_PROPS_IN_KEY, new BigDecimal(datumPropertiesIn)));
		recs.add(new UsageInfo(NodeUsages.DATUM_OUT_KEY, new BigDecimal(datumOut)));
		recs.add(new UsageInfo(NodeUsages.DATUM_DAYS_STORED_KEY, new BigDecimal(datumDaysStored)));
		recs.add(new UsageInfo(NodeUsages.INSTRUCTIONS_ISSUED_KEY, new BigDecimal(instructionsIssued)));
		return recs;
	}

	/**
	 * Get the invoice ID.
	 * 
	 * <p>
	 * This is an alias for {@link SnfInvoiceRelatedPK#getInvoiceId()}.
	 * </p>
	 * 
	 * @return the invoice ID
	 */
	public Long getInvoiceId() {
		SnfInvoiceRelatedPK id = getId();
		return (id != null ? id.getInvoiceId() : null);
	}

	/**
	 * Get the node ID.
	 * 
	 * <p>
	 * This is an alias for {@link SnfInvoiceRelatedPK#getId()}.
	 * </p>
	 * 
	 * @return the node ID
	 */
	public Long getNodeId() {
		SnfInvoiceRelatedPK id = getId();
		return (id != null ? id.getId() : null);
	}

	/**
	 * Get the datum properties in count.
	 * 
	 * @return the count, never {@literal null}
	 */
	public BigInteger getDatumPropertiesIn() {
		return datumPropertiesIn;
	}

	/**
	 * Get the datum out count.
	 * 
	 * @return the count, never {@literal null}
	 */
	public BigInteger getDatumOut() {
		return datumOut;
	}

	/**
	 * Get the datum days stored count.
	 * 
	 * @return the count, never {@literal null}
	 */
	public BigInteger getDatumDaysStored() {
		return datumDaysStored;
	}

	@Override
	public String getDescription() {
		return description;
	}

	/**
	 * Get the instructions issued count.
	 * 
	 * @return the instructionsIssued
	 * @since 1.4
	 */
	public BigInteger getInstructionsIssued() {
		return instructionsIssued;
	}

}
