/* ==================================================================
 * ChargePointActionStatusKey.java - 16/11/2022 5:08:46 pm
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

package net.solarnetwork.central.ocpp.domain;

import java.io.Serializable;
import java.util.Objects;
import net.solarnetwork.central.domain.BasePK;
import net.solarnetwork.util.ObjectUtils;

/**
 * A primary key for a Charge Point action status.
 * 
 * @author matt
 * @version 1.0
 */
public class ChargePointActionStatusKey extends BasePK
		implements Serializable, Cloneable, Comparable<ChargePointActionStatusKey> {

	private static final long serialVersionUID = 635218697257392391L;

	/** The user ID. */
	private final long userId;

	/** The charge point ID. */
	private final long chargePointId;

	/** The connector ID. */
	private final int connectorId;

	/** The action name. */
	private final String action;

	/**
	 * Create a new key instance.
	 * 
	 * @param userId
	 *        the user ID
	 * @param chargePointId
	 *        the Charge Point ID
	 * @param connectorId
	 *        the connector ID, or {@literal 0} for the charger itself
	 * @param action
	 *        the action name
	 * @return the new key
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public static ChargePointActionStatusKey keyFor(long userId, long chargePointId, int connectorId,
			String action) {
		return new ChargePointActionStatusKey(userId, chargePointId, connectorId, action);
	}

	/**
	 * Constructor.
	 * 
	 * @param userId
	 *        the user ID
	 * @param chargePointId
	 *        the Charge Point ID
	 * @param connectorId
	 *        the connector ID, or {@literal 0} for the charger itself
	 * @param action
	 *        the action name
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public ChargePointActionStatusKey(long userId, long chargePointId, int connectorId, String action) {
		super();
		this.userId = userId;
		this.chargePointId = chargePointId;
		this.connectorId = connectorId;
		this.action = ObjectUtils.requireNonNullArgument(action, "action");
	}

	@Override
	public int compareTo(ChargePointActionStatusKey o) {
		int result = Long.compare(userId, o.userId);
		if ( result != 0 ) {
			return result;
		}
		result = Long.compare(chargePointId, o.chargePointId);
		if ( result != 0 ) {
			return result;
		}
		result = Integer.compare(connectorId, o.connectorId);
		if ( result != 0 ) {
			return result;
		}
		return result = action.compareTo(o.action);
	}

	@Override
	protected void populateIdValue(StringBuilder buf) {
		buf.append("u=").append(userId);
		buf.append(";cp=").append(chargePointId);
		buf.append(";c=").append(connectorId);
		buf.append(";a=").append(action);
	}

	@Override
	protected void populateStringValue(StringBuilder buf) {
		buf.append("userId=").append(userId);
		buf.append(", chargePointId=").append(chargePointId);
		buf.append("; connectorId=").append(connectorId);
		buf.append("; action=").append(action);
	}

	@Override
	public ChargePointActionStatusKey clone() {
		return (ChargePointActionStatusKey) super.clone();
	}

	@Override
	public int hashCode() {
		return Objects.hash(userId, chargePointId, connectorId, action);
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( !(obj instanceof ChargePointActionStatusKey) ) {
			return false;
		}
		ChargePointActionStatusKey other = (ChargePointActionStatusKey) obj;
		return userId == other.userId && chargePointId == other.chargePointId
				&& connectorId == other.connectorId && action.equals(other.action);
	}

	/**
	 * Get the user ID.
	 * 
	 * @return the user ID
	 */
	public long getUserId() {
		return userId;
	}

	/**
	 * Get the Charge Point ID.
	 * 
	 * @return the Charge Point ID
	 */
	public long getChargePointId() {
		return chargePointId;
	}

	/**
	 * Get the connector ID.
	 * 
	 * @return the connector ID, or {@literal 0} for the charger itself
	 */
	public int getConnectorId() {
		return connectorId;
	}

	/**
	 * Get the action.
	 * 
	 * @return the action, never {@literal null}
	 */
	public String getAction() {
		return action;
	}

}
