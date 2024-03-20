/* ==================================================================
 * BasicOcppCriteria.java - 17/11/2022 9:09:41 am
 * 
 * Copyright 2022 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.ocpp.dao;

import java.time.Instant;
import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import net.solarnetwork.central.common.dao.BasicCoreCriteria;
import net.solarnetwork.dao.DateRangeCriteria;
import net.solarnetwork.dao.PaginationCriteria;
import net.solarnetwork.ocpp.domain.ChargeSessionEndReason;

/**
 * Basic implementation of OCPP criteria APIs.
 * 
 * @author matt
 * @version 1.3
 */
public class BasicOcppCriteria extends BasicCoreCriteria
		implements ChargePointStatusFilter, ChargePointActionStatusFilter, ChargeSessionFilter {

	private Long[] chargePointIds;
	private String[] identifiers;
	private Integer[] evseIds;
	private Integer[] connectorIds;
	private String[] actions;
	private Instant startDate;
	private Instant endDate;
	private UUID[] chargeSessionIds;
	private Boolean active;
	private String[] transactionIds;
	private ChargeSessionEndReason[] endReasons;

	/**
	 * Copy the properties of another criteria into this instance.
	 * 
	 * <p>
	 * This method will test for conformance to all the various criteria
	 * interfaces implemented by this class, and copy those properties as well.
	 * </p>
	 * 
	 * @param criteria
	 *        the criteria to copy
	 */
	@Override
	public void copyFrom(PaginationCriteria criteria) {
		super.copyFrom(criteria);
		if ( criteria instanceof BasicOcppCriteria c ) {
			setChargePointIds(c.chargePointIds);
			setIdentifiers(c.identifiers);
			setEvseIds(c.evseIds);
			setConnectorIds(c.connectorIds);
			setActions(c.actions);
			setStartDate(c.startDate);
			setEndDate(c.endDate);
			setChargeSessionIds(c.chargeSessionIds);
			setActive(c.active);
			setTransactionIds(c.transactionIds);
			setEndReasons(c.endReasons);
		} else {
			if ( criteria instanceof ChargePointCriteria c ) {
				setChargePointIds(c.getChargePointIds());
			}
			if ( criteria instanceof IdentifierCriteria c ) {
				setIdentifiers(c.getIdentifiers());
			}
			if ( criteria instanceof ChargePointConnectorCriteria c ) {
				setEvseIds(c.getEvseIds());
				setConnectorIds(c.getConnectorIds());
			}
			if ( criteria instanceof ActionCriteria c ) {
				setActions(c.getActions());
			}
			if ( criteria instanceof DateRangeCriteria c ) {
				setStartDate(c.getStartDate());
				setEndDate(c.getEndDate());
			}
			if ( criteria instanceof ChargeSessionCriteria c ) {
				setChargeSessionIds(c.getChargeSessionIds());
				setActive(c.getActive());
			}
			if ( criteria instanceof ChargeSessionTransactionCriteria c ) {
				setTransactionIds(c.getTransactionIds());
			}
			if ( criteria instanceof ChargeSessionEndReasonCriteria c ) {
				setEndReasons(c.getEndReasons());
			}
		}
	}

	/**
	 * Create a copy of a criteria.
	 * 
	 * @param criteria
	 *        the criteria to copy
	 * @return the copy
	 */
	public static BasicOcppCriteria copy(PaginationCriteria criteria) {
		BasicOcppCriteria c = new BasicOcppCriteria();
		c.copyFrom(criteria);
		return c;
	}

	@Override
	public BasicOcppCriteria clone() {
		return (BasicOcppCriteria) super.clone();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Arrays.hashCode(actions);
		result = prime * result + Arrays.hashCode(chargePointIds);
		result = prime * result + Arrays.hashCode(identifiers);
		result = prime * result + Arrays.hashCode(evseIds);
		result = prime * result + Arrays.hashCode(connectorIds);
		result = prime * result + Objects.hash(endDate, startDate, active);
		result = prime * result + Arrays.hashCode(chargeSessionIds);
		result = prime * result + Arrays.hashCode(transactionIds);
		result = prime * result + Arrays.hashCode(endReasons);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( !super.equals(obj) ) {
			return false;
		}
		if ( !(obj instanceof BasicOcppCriteria) ) {
			return false;
		}
		BasicOcppCriteria other = (BasicOcppCriteria) obj;
		return Arrays.equals(actions, other.actions)
				&& Arrays.equals(chargePointIds, other.chargePointIds)
				&& Arrays.equals(identifiers, other.identifiers) && Arrays.equals(evseIds, other.evseIds)
				&& Arrays.equals(connectorIds, other.connectorIds)
				&& Objects.equals(endDate, other.endDate) && Objects.equals(startDate, other.startDate)
				&& Arrays.equals(chargeSessionIds, other.chargeSessionIds)
				&& Objects.equals(active, other.active)
				&& Arrays.equals(transactionIds, other.transactionIds)
				&& Arrays.equals(endReasons, other.endReasons);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("BasicOcppCriteria{");
		if ( getUserIds() != null ) {
			builder.append("userIds=");
			builder.append(Arrays.toString(getUserIds()));
			builder.append(", ");
		}
		if ( chargePointIds != null ) {
			builder.append("chargePointIds=");
			builder.append(Arrays.toString(chargePointIds));
			builder.append(", ");
		}
		if ( identifiers != null ) {
			builder.append("identifiers=");
			builder.append(Arrays.toString(identifiers));
			builder.append(", ");
		}
		if ( evseIds != null ) {
			builder.append("evseIds=");
			builder.append(Arrays.toString(evseIds));
			builder.append(", ");
		}
		if ( connectorIds != null ) {
			builder.append("connectorIds=");
			builder.append(Arrays.toString(connectorIds));
			builder.append(", ");
		}
		if ( chargeSessionIds != null ) {
			builder.append("chargeSessionIds=");
			builder.append(Arrays.toString(chargeSessionIds));
			builder.append(", ");
		}
		if ( active != null ) {
			builder.append("active=");
			builder.append(active);
			builder.append(", ");
		}
		if ( transactionIds != null ) {
			builder.append("transactionIds=");
			builder.append(Arrays.toString(transactionIds));
			builder.append(", ");
		}
		if ( endReasons != null ) {
			builder.append("endReasons=");
			builder.append(Arrays.toString(endReasons));
			builder.append(", ");
		}
		if ( actions != null ) {
			builder.append("actions=");
			builder.append(Arrays.toString(actions));
			builder.append(", ");
		}
		if ( startDate != null ) {
			builder.append("startDate=");
			builder.append(startDate);
			builder.append(", ");
		}
		if ( endDate != null ) {
			builder.append("endDate=");
			builder.append(endDate);
		}
		builder.append("}");
		return builder.toString();
	}

	/**
	 * Set a single charge point ID.
	 * 
	 * <p>
	 * This is a convenience method for requests that use a single charge point
	 * ID at a time. The charge point ID is still stored on the
	 * {@code chargePointIds} array, just as the first value. Calling this
	 * method replaces any existing {@code chargePointIds} value with a new
	 * array containing just the ID passed into this method.
	 * </p>
	 * 
	 * @param chargePointId
	 *        the ID of the charge point
	 */
	@JsonSetter
	public void setChargePointId(Long chargePointId) {
		this.chargePointIds = (chargePointId == null ? null : new Long[] { chargePointId });
	}

	@Override
	@JsonIgnore
	public Long getChargePointId() {
		return (this.chargePointIds == null || this.chargePointIds.length < 1 ? null
				: this.chargePointIds[0]);
	}

	@Override
	public Long[] getChargePointIds() {
		return chargePointIds;
	}

	/**
	 * Set a list of charge point IDs to filter on.
	 * 
	 * @param chargePointIds
	 *        The charge point IDs to filter on.
	 */
	public void setChargePointIds(Long[] chargePointIds) {
		this.chargePointIds = chargePointIds;
	}

	/**
	 * Set a single EVSE ID.
	 * 
	 * <p>
	 * This is a convenience method for requests that use a single EVSE ID at a
	 * time. The EVSE ID is still stored on the {@code evseIds} array, just as
	 * the first value. Calling this method replaces any existing
	 * {@code evseIds} value with a new array containing just the ID passed into
	 * this method.
	 * </p>
	 * 
	 * @param connectorId
	 *        the ID of the charge point
	 * @since 1.2
	 */
	@JsonSetter
	public void setEvseId(Integer evseId) {
		this.evseIds = (evseId == null ? null : new Integer[] { evseId });
	}

	@Override
	@JsonIgnore
	public Integer getEvseId() {
		return (this.evseIds == null || this.evseIds.length < 1 ? null : this.evseIds[0]);
	}

	@Override
	public Integer[] getEvseIds() {
		return evseIds;
	}

	/**
	 * Set a list of EVSE IDs to filter on.
	 * 
	 * @param evseIds
	 *        The EVSE IDs to filter on.
	 * @since 1.2
	 */
	public void setEvseIds(Integer[] evseIds) {
		this.evseIds = evseIds;
	}

	/**
	 * Set a single connector ID.
	 * 
	 * <p>
	 * This is a convenience method for requests that use a single connector ID
	 * at a time. The connector ID is still stored on the {@code connectorIds}
	 * array, just as the first value. Calling this method replaces any existing
	 * {@code connectorIds} value with a new array containing just the ID passed
	 * into this method.
	 * </p>
	 * 
	 * @param connectorId
	 *        the ID of the charge point
	 */
	@JsonSetter
	public void setConnectorId(Integer connectorId) {
		this.connectorIds = (connectorId == null ? null : new Integer[] { connectorId });
	}

	@Override
	@JsonIgnore
	public Integer getConnectorId() {
		return (this.connectorIds == null || this.connectorIds.length < 1 ? null : this.connectorIds[0]);
	}

	@Override
	public Integer[] getConnectorIds() {
		return connectorIds;
	}

	/**
	 * Set a list of charge point IDs to filter on.
	 * 
	 * @param connectorIds
	 *        The connector IDs to filter on.
	 */
	public void setConnectorIds(Integer[] connectorIds) {
		this.connectorIds = connectorIds;
	}

	/**
	 * Set an identifier.
	 * 
	 * <p>
	 * This is a convenience method for requests that use a single identifier at
	 * a time. The identifier is still stored on the {@code identifiers} array,
	 * just as the first value. Calling this method replaces any existing
	 * {@code identifiers} value with a new array containing just the ID passed
	 * into this method.
	 * </p>
	 * 
	 * @param identifier
	 *        the identifier to set
	 */
	@JsonSetter
	public void setIdentifier(String identifier) {
		this.identifiers = (identifier == null ? null : new String[] { identifier });
	}

	@Override
	@JsonIgnore
	public String getIdentifier() {
		return (this.identifiers == null || this.identifiers.length < 1 ? null : this.identifiers[0]);
	}

	@Override
	public String[] getIdentifiers() {
		return identifiers;
	}

	/**
	 * Set a list of identifiers to filter on.
	 * 
	 * @param identifiers
	 *        The identifiers to filter on.
	 */
	public void setIdentifiers(String[] identifiers) {
		this.identifiers = identifiers;
	}

	/**
	 * Set an action.
	 * 
	 * <p>
	 * This is a convenience method for requests that use a single action at a
	 * time. The action is still stored on the {@code actions} array, just as
	 * the first value. Calling this method replaces any existing
	 * {@code actions} value with a new array containing just the ID passed into
	 * this method.
	 * </p>
	 * 
	 * @param action
	 *        the action to set
	 */
	@JsonSetter
	public void setAction(String action) {
		this.actions = (action == null ? null : new String[] { action });
	}

	@Override
	@JsonIgnore
	public String getAction() {
		return (this.actions == null || this.actions.length < 1 ? null : this.actions[0]);
	}

	@Override
	public String[] getActions() {
		return actions;
	}

	/**
	 * Set a list of actions to filter on.
	 * 
	 * @param actions
	 *        The actions to filter on.
	 */
	public void setActions(String[] actions) {
		this.actions = actions;
	}

	@Override
	public Instant getStartDate() {
		return startDate;
	}

	/**
	 * Set the start date.
	 * 
	 * @param startDate
	 *        the date to set
	 */
	public void setStartDate(Instant startDate) {
		this.startDate = startDate;
	}

	@Override
	public Instant getEndDate() {
		return endDate;
	}

	/**
	 * Set the end date.
	 * 
	 * @param endDate
	 *        the date to set
	 */
	public void setEndDate(Instant endDate) {
		this.endDate = endDate;
	}

	@Override
	@JsonIgnore
	public UUID getChargeSessionId() {
		return (this.chargeSessionIds == null || this.chargeSessionIds.length < 1 ? null
				: this.chargeSessionIds[0]);
	}

	/**
	 * Set a single charge session ID.
	 * 
	 * <p>
	 * This is a convenience method for requests that use a single connector ID
	 * at a time. The connector ID is still stored on the
	 * {@code chargeSessionIds} array, just as the first value. Calling this
	 * method replaces any existing {@code chargeSessionIds} value with a new
	 * array containing just the ID passed into this method.
	 * </p>
	 * 
	 * @param chargeSessionId
	 *        the ID of the charge session
	 */
	@JsonSetter
	public void setChargeSessionId(UUID chargeSessionId) {
		this.chargeSessionIds = (chargeSessionId == null ? null : new UUID[] { chargeSessionId });
	}

	@Override
	public UUID[] getChargeSessionIds() {
		return chargeSessionIds;
	}

	/**
	 * Set the charge session IDs.
	 * 
	 * @param chargeSessionIds
	 *        the charge session IDs to set
	 */
	public void setChargeSessionIds(UUID[] chargeSessionIds) {
		this.chargeSessionIds = chargeSessionIds;
	}

	@Override
	@JsonIgnore
	public String getTransactionId() {
		return (this.transactionIds == null || this.transactionIds.length < 1 ? null
				: this.transactionIds[0]);
	}

	/**
	 * Set a single transaction ID.
	 * 
	 * <p>
	 * This is a convenience method for requests that use one transaction ID at
	 * a time. The transaction ID is still stored on the {@code transactionIds}
	 * array, just as the first value. Calling this method replaces any existing
	 * {@code transactionIds} value with a new array containing just the ID
	 * passed into this method.
	 * </p>
	 * 
	 * @param transactionId
	 *        the ID of the transaction
	 */
	@JsonSetter
	public void setTransactionId(String transactionId) {
		this.transactionIds = (transactionId == null ? null : new String[] { transactionId });
	}

	@Override
	public String[] getTransactionIds() {
		return transactionIds;
	}

	/**
	 * Set the transaction IDs.
	 * 
	 * @param transactionIds
	 *        the transaction IDs to set
	 */
	public void setTransactionIds(String[] transactionIds) {
		this.transactionIds = transactionIds;
	}

	@Override
	@JsonIgnore
	public ChargeSessionEndReason getEndReason() {
		return (this.endReasons == null || this.endReasons.length < 1 ? null : this.endReasons[0]);
	}

	/**
	 * Set a single charge session end reason.
	 * 
	 * <p>
	 * This is a convenience method for requests that use one end reason at a
	 * time. The end reason is still stored on the {@code endReasons} array,
	 * just as the first value. Calling this method replaces any existing
	 * {@code endReasons} value with a new array containing just the ID passed
	 * into this method.
	 * </p>
	 * 
	 * @param chargeSessionEndReason
	 *        the end reason
	 */
	@JsonSetter
	public void setEndReason(ChargeSessionEndReason endReasons) {
		this.endReasons = (endReasons == null ? null : new ChargeSessionEndReason[] { endReasons });
	}

	@Override
	public ChargeSessionEndReason[] getEndReasons() {
		return endReasons;
	}

	@JsonIgnore
	@Override
	public Integer[] getEndReasonCodes() {
		return ChargeSessionFilter.super.getEndReasonCodes();
	}

	/**
	 * Set the charge session end reasons.
	 * 
	 * @param endReasons
	 *        the end reasons to set
	 */
	public void setEndReasons(ChargeSessionEndReason[] endReasons) {
		this.endReasons = endReasons;
	}

	@Override
	public Boolean getActive() {
		return active;
	}

	/**
	 * Set the active status.
	 * 
	 * @param active
	 *        the active to set
	 */
	public void setActive(Boolean active) {
		this.active = active;
	}

}
