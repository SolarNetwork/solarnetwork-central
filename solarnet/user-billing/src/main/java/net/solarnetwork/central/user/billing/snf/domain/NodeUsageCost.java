/* ==================================================================
 * NodeUsageCost.java - 22/07/2020 2:11:05 PM
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

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * Costs associated with node usage.
 *
 * <p>
 * This object is used in different contexts, such as tiered pricing schedules
 * and general node usage calculations.
 * </p>
 *
 * @author matt
 * @version 1.6
 */
public class NodeUsageCost implements Serializable {

	private static final long serialVersionUID = -1787655430294334300L;

	private BigDecimal datumPropertiesInCost;
	private BigDecimal datumDaysStoredCost;
	private BigDecimal datumOutCost;
	private BigDecimal instructionsIssuedCost;
	private BigDecimal fluxDataInCost;
	private BigDecimal fluxDataOutCost;
	private BigDecimal ocppChargersCost;
	private BigDecimal oscpCapacityGroupsCost;
	private BigDecimal oscpCapacityCost;
	private BigDecimal dnp3DataPointsCost;
	private BigDecimal oauthClientCredentialsCost;
	private BigDecimal cloudIntegrationsDataCost;

	/**
	 * Constructor.
	 */
	public NodeUsageCost() {
		this(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
				BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
				BigDecimal.ZERO);
	}

	/**
	 * Constructor.
	 *
	 * <p>
	 * This constructor converts all costs to {@link BigDecimal} values.
	 * </p>
	 *
	 * @param datumPropertiesInCost
	 *        the properties in cost
	 * @param datumOutCost
	 *        the datum out cost
	 * @param datumDaysStoredCost
	 *        the days stored cost
	 */
	public NodeUsageCost(String datumPropertiesInCost, String datumOutCost, String datumDaysStoredCost) {
		this(new BigDecimal(datumPropertiesInCost), new BigDecimal(datumOutCost),
				new BigDecimal(datumDaysStoredCost));
	}

	/**
	 * Constructor.
	 *
	 * <p>
	 * This constructor converts all costs to {@link BigDecimal} values.
	 * </p>
	 *
	 * @param datumPropertiesInCost
	 *        the properties in cost
	 * @param datumOutCost
	 *        the datum out cost
	 * @param datumDaysStoredCost
	 *        the days stored cost
	 * @param ocppChargersCost
	 *        the OCPP Chargers cost
	 * @param oscpCapacityGroupsCost
	 *        the OSCP Capacity Groups cost
	 * @since 1.1
	 */
	public NodeUsageCost(String datumPropertiesInCost, String datumOutCost, String datumDaysStoredCost,
			String ocppChargersCost, String oscpCapacityGroupsCost) {
		this(new BigDecimal(datumPropertiesInCost), new BigDecimal(datumOutCost),
				new BigDecimal(datumDaysStoredCost), new BigDecimal(ocppChargersCost),
				new BigDecimal(oscpCapacityGroupsCost));
	}

	/**
	 * Constructor.
	 *
	 * <p>
	 * This constructor converts all costs to {@link BigDecimal} values.
	 * </p>
	 *
	 * @param datumPropertiesInCost
	 *        the properties in cost
	 * @param datumOutCost
	 *        the datum out cost
	 * @param datumDaysStoredCost
	 *        the days stored cost
	 * @param ocppChargersCost
	 *        the OCPP Chargers cost
	 * @param oscpCapacityGroupsCost
	 *        the OSCP Capacity Groups cost
	 * @param dnp3DataPointsCost
	 *        the DNP3 Data Points cost
	 * @since 1.2
	 */
	public NodeUsageCost(String datumPropertiesInCost, String datumOutCost, String datumDaysStoredCost,
			String ocppChargersCost, String oscpCapacityGroupsCost, String dnp3DataPointsCost) {
		this(new BigDecimal(datumPropertiesInCost), new BigDecimal(datumOutCost),
				new BigDecimal(datumDaysStoredCost), new BigDecimal(ocppChargersCost),
				new BigDecimal(oscpCapacityGroupsCost), new BigDecimal(dnp3DataPointsCost));
	}

	/**
	 * Constructor.
	 *
	 * <p>
	 * This constructor converts all costs to {@link BigDecimal} values.
	 * </p>
	 *
	 * @param datumPropertiesInCost
	 *        the properties in cost
	 * @param datumOutCost
	 *        the datum out cost
	 * @param datumDaysStoredCost
	 *        the days stored cost
	 * @param instructionsIssuedCost
	 *        the instructions issued code
	 * @param ocppChargersCost
	 *        the OCPP Chargers cost
	 * @param oscpCapacityGroupsCost
	 *        the OSCP Capacity Groups cost
	 * @param dnp3DataPointsCost
	 *        the DNP3 Data Points cost
	 * @since 1.3
	 */
	public NodeUsageCost(String datumPropertiesInCost, String datumOutCost, String datumDaysStoredCost,
			String instructionsIssuedCost, String ocppChargersCost, String oscpCapacityGroupsCost,
			String dnp3DataPointsCost) {
		this(new BigDecimal(datumPropertiesInCost), new BigDecimal(datumOutCost),
				new BigDecimal(datumDaysStoredCost), new BigDecimal(instructionsIssuedCost),
				new BigDecimal(ocppChargersCost), new BigDecimal(oscpCapacityGroupsCost),
				new BigDecimal(dnp3DataPointsCost));
	}

	/**
	 * Constructor.
	 *
	 * <p>
	 * This constructor converts all costs to {@link BigDecimal} values.
	 * </p>
	 *
	 * @param datumPropertiesInCost
	 *        the properties in cost
	 * @param datumOutCost
	 *        the datum out cost
	 * @param datumDaysStoredCost
	 *        the days stored cost
	 * @param instructionsIssuedCost
	 *        the instructions issued code
	 * @param ocppChargersCost
	 *        the OCPP Chargers cost
	 * @param oscpCapacityGroupsCost
	 *        the OSCP Capacity Groups cost
	 * @param oscpCapacityCost
	 *        the OSCP capacity cost
	 * @param dnp3DataPointsCost
	 *        the DNP3 Data Points cost
	 * @since 1.4
	 */
	public NodeUsageCost(String datumPropertiesInCost, String datumOutCost, String datumDaysStoredCost,
			String instructionsIssuedCost, String ocppChargersCost, String oscpCapacityGroupsCost,
			String oscpCapacityCost, String dnp3DataPointsCost) {
		this(new BigDecimal(datumPropertiesInCost), new BigDecimal(datumOutCost),
				new BigDecimal(datumDaysStoredCost), new BigDecimal(instructionsIssuedCost),
				new BigDecimal(ocppChargersCost), new BigDecimal(oscpCapacityGroupsCost),
				new BigDecimal(oscpCapacityCost), new BigDecimal(dnp3DataPointsCost));
	}

	/**
	 * Constructor.
	 *
	 * @param datumPropertiesInCost
	 *        the properties in cost
	 * @param datumOutCost
	 *        the datum out cost
	 * @param datumDaysStoredCost
	 *        the days stored cost
	 */
	public NodeUsageCost(BigDecimal datumPropertiesInCost, BigDecimal datumOutCost,
			BigDecimal datumDaysStoredCost) {
		this(datumPropertiesInCost, datumOutCost, datumDaysStoredCost, null, null);
	}

	/**
	 * Constructor.
	 *
	 * @param datumPropertiesInCost
	 *        the properties in cost
	 * @param datumOutCost
	 *        the datum out cost
	 * @param datumDaysStoredCost
	 *        the days stored cost
	 * @param ocppChargersCost
	 *        the OCPP Chargers cost
	 * @param oscpCapacityGroupsCost
	 *        the OSCP Capacity Groups cost
	 * @since 1.1
	 */
	public NodeUsageCost(BigDecimal datumPropertiesInCost, BigDecimal datumOutCost,
			BigDecimal datumDaysStoredCost, BigDecimal ocppChargersCost,
			BigDecimal oscpCapacityGroupsCost) {
		this(datumPropertiesInCost, datumOutCost, datumDaysStoredCost, ocppChargersCost,
				oscpCapacityGroupsCost, null);
	}

	/**
	 * Constructor.
	 *
	 * @param datumPropertiesInCost
	 *        the properties in cost
	 * @param datumOutCost
	 *        the datum out cost
	 * @param datumDaysStoredCost
	 *        the days stored cost
	 * @param ocppChargersCost
	 *        the OCPP Chargers cost
	 * @param oscpCapacityGroupsCost
	 *        the OSCP Capacity Groups cost
	 * @param dnp3DataPointsCost
	 *        the DNP3 Data Points cost
	 * @since 1.2
	 */
	public NodeUsageCost(BigDecimal datumPropertiesInCost, BigDecimal datumOutCost,
			BigDecimal datumDaysStoredCost, BigDecimal ocppChargersCost,
			BigDecimal oscpCapacityGroupsCost, BigDecimal dnp3DataPointsCost) {
		this(datumPropertiesInCost, datumOutCost, datumDaysStoredCost, null, ocppChargersCost,
				oscpCapacityGroupsCost, dnp3DataPointsCost);
	}

	/**
	 * Constructor.
	 *
	 * @param datumPropertiesInCost
	 *        the properties in cost
	 * @param datumOutCost
	 *        the datum out cost
	 * @param datumDaysStoredCost
	 *        the days stored cost
	 * @param instructionsIssuedCost
	 *        the instructions issued cost
	 * @param ocppChargersCost
	 *        the OCPP Chargers cost
	 * @param oscpCapacityGroupsCost
	 *        the OSCP Capacity Groups cost
	 * @param dnp3DataPointsCost
	 *        the DNP3 Data Points cost
	 * @since 1.3
	 */
	public NodeUsageCost(BigDecimal datumPropertiesInCost, BigDecimal datumOutCost,
			BigDecimal datumDaysStoredCost, BigDecimal instructionsIssuedCost,
			BigDecimal ocppChargersCost, BigDecimal oscpCapacityGroupsCost,
			BigDecimal dnp3DataPointsCost) {
		this(datumPropertiesInCost, datumOutCost, datumDaysStoredCost, instructionsIssuedCost, null,
				null, ocppChargersCost, oscpCapacityGroupsCost, null, dnp3DataPointsCost, null);
	}

	/**
	 * Constructor.
	 *
	 * @param datumPropertiesInCost
	 *        the properties in cost
	 * @param datumOutCost
	 *        the datum out cost
	 * @param datumDaysStoredCost
	 *        the days stored cost
	 * @param instructionsIssuedCost
	 *        the instructions issued cost
	 * @param ocppChargersCost
	 *        the OCPP Chargers cost
	 * @param oscpCapacityGroupsCost
	 *        the OSCP Capacity Groups cost
	 * @param oscpCapacityCost
	 *        the OSCP capacity cost
	 * @param dnp3DataPointsCost
	 *        the DNP3 Data Points cost
	 * @since 1.4
	 */
	public NodeUsageCost(BigDecimal datumPropertiesInCost, BigDecimal datumOutCost,
			BigDecimal datumDaysStoredCost, BigDecimal instructionsIssuedCost,
			BigDecimal ocppChargersCost, BigDecimal oscpCapacityGroupsCost, BigDecimal oscpCapacityCost,
			BigDecimal dnp3DataPointsCost) {
		this(datumPropertiesInCost, datumOutCost, datumDaysStoredCost, instructionsIssuedCost, null,
				null, ocppChargersCost, oscpCapacityGroupsCost, oscpCapacityCost, dnp3DataPointsCost,
				null);
	}

	/**
	 * Constructor.
	 *
	 * @param datumPropertiesInCost
	 *        the properties in cost
	 * @param datumOutCost
	 *        the datum out cost
	 * @param datumDaysStoredCost
	 *        the days stored cost
	 * @param instructionsIssuedCost
	 *        the instructions issued cost
	 * @param ocppChargersCost
	 *        the OCPP Chargers cost
	 * @param oscpCapacityGroupsCost
	 *        the OSCP Capacity Groups cost
	 * @param oscpCapacityCost
	 *        the OSCP capacity cost
	 * @param dnp3DataPointsCost
	 *        the DNP3 Data Points cost
	 * @since 1.5
	 */
	public NodeUsageCost(BigDecimal datumPropertiesInCost, BigDecimal datumOutCost,
			BigDecimal datumDaysStoredCost, BigDecimal instructionsIssuedCost, BigDecimal fluxDataInCost,
			BigDecimal fluxDataOutCost, BigDecimal ocppChargersCost, BigDecimal oscpCapacityGroupsCost,
			BigDecimal oscpCapacityCost, BigDecimal dnp3DataPointsCost,
			BigDecimal oauthClientCredentialsCost) {
		this(datumPropertiesInCost, datumOutCost, datumDaysStoredCost, instructionsIssuedCost,
				fluxDataInCost, fluxDataOutCost, ocppChargersCost, oscpCapacityGroupsCost,
				oscpCapacityCost, dnp3DataPointsCost, oauthClientCredentialsCost, null);
	}

	/**
	 * Constructor.
	 *
	 * @param datumPropertiesInCost
	 *        the properties in cost
	 * @param datumOutCost
	 *        the datum out cost
	 * @param datumDaysStoredCost
	 *        the days stored cost
	 * @param instructionsIssuedCost
	 *        the instructions issued cost
	 * @param ocppChargersCost
	 *        the OCPP Chargers cost
	 * @param oscpCapacityGroupsCost
	 *        the OSCP Capacity Groups cost
	 * @param oscpCapacityCost
	 *        the OSCP capacity cost
	 * @param dnp3DataPointsCost
	 *        the DNP3 Data Points cost
	 * @param cloudIntegrationsDataCost
	 *        the Cloud Integrations data cost
	 * @since 1.6
	 */
	public NodeUsageCost(BigDecimal datumPropertiesInCost, BigDecimal datumOutCost,
			BigDecimal datumDaysStoredCost, BigDecimal instructionsIssuedCost, BigDecimal fluxDataInCost,
			BigDecimal fluxDataOutCost, BigDecimal ocppChargersCost, BigDecimal oscpCapacityGroupsCost,
			BigDecimal oscpCapacityCost, BigDecimal dnp3DataPointsCost,
			BigDecimal oauthClientCredentialsCost, BigDecimal cloudIntegrationsDataCost) {
		super();
		setDatumPropertiesInCost(datumPropertiesInCost);
		setDatumOutCost(datumOutCost);
		setDatumDaysStoredCost(datumDaysStoredCost);
		setInstructionsIssuedCost(instructionsIssuedCost);
		setFluxDataInCost(fluxDataInCost);
		setFluxDataOutCost(fluxDataOutCost);
		setOcppChargersCost(ocppChargersCost);
		setOscpCapacityGroupsCost(oscpCapacityGroupsCost);
		setOscpCapacityCost(oscpCapacityCost);
		setDnp3DataPointsCost(dnp3DataPointsCost);
		setOauthClientCredentialsCost(oauthClientCredentialsCost);
		setCloudIntegrationsDataCost(cloudIntegrationsDataCost);
	}

	@Override
	public int hashCode() {
		return Objects.hash(datumDaysStoredCost, datumOutCost, datumPropertiesInCost, ocppChargersCost,
				oscpCapacityGroupsCost, oscpCapacityCost, dnp3DataPointsCost, oauthClientCredentialsCost,
				cloudIntegrationsDataCost);
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( !(obj instanceof NodeUsageCost) ) {
			return false;
		}
		NodeUsageCost other = (NodeUsageCost) obj;
		return Objects.equals(datumDaysStoredCost, other.datumDaysStoredCost)
				&& Objects.equals(datumOutCost, other.datumOutCost)
				&& Objects.equals(datumPropertiesInCost, other.datumPropertiesInCost)
				&& Objects.equals(ocppChargersCost, other.ocppChargersCost)
				&& Objects.equals(oscpCapacityGroupsCost, other.oscpCapacityGroupsCost)
				&& Objects.equals(oscpCapacityCost, other.oscpCapacityCost)
				&& Objects.equals(dnp3DataPointsCost, other.dnp3DataPointsCost)
				&& Objects.equals(oauthClientCredentialsCost, other.oauthClientCredentialsCost)
				&& Objects.equals(cloudIntegrationsDataCost, other.cloudIntegrationsDataCost);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("NodeUsage{");
		builder.append("datumPropertiesInCost=");
		builder.append(datumPropertiesInCost);
		builder.append(", datumOutCost=");
		builder.append(datumOutCost);
		builder.append(", datumDaysStoredCost=");
		builder.append(datumDaysStoredCost);
		builder.append(", ocppChargersCost=");
		builder.append(ocppChargersCost);
		builder.append(", oscpCapacityGroupsCost=");
		builder.append(oscpCapacityGroupsCost);
		builder.append(", oscpCapacityCost=");
		builder.append(oscpCapacityCost);
		builder.append(", dnp3DataPointsCost=");
		builder.append(dnp3DataPointsCost);
		builder.append(", oauthClientCredentialsCost=");
		builder.append(oauthClientCredentialsCost);
		builder.append(", cloudIntegrationsDataCost=");
		builder.append(cloudIntegrationsDataCost);
		builder.append("}");
		return builder.toString();
	}

	/**
	 * Get the cost of datum properties added.
	 *
	 * @return the cost, never {@literal null}
	 */
	public BigDecimal getDatumPropertiesInCost() {
		return datumPropertiesInCost;
	}

	/**
	 * Set the cost of datum properties added.
	 *
	 * @param datumPropertiesInCost
	 *        the cost to set (null will be stored as {@literal 0}
	 */
	public void setDatumPropertiesInCost(BigDecimal datumPropertiesInCost) {
		if ( datumPropertiesInCost == null ) {
			datumPropertiesInCost = BigDecimal.ZERO;
		}
		this.datumPropertiesInCost = datumPropertiesInCost;
	}

	/**
	 * Get the cost of datum stored per day (accumulating).
	 *
	 * @return the cost, never {@literal null}
	 */
	public BigDecimal getDatumDaysStoredCost() {
		return datumDaysStoredCost;
	}

	/**
	 * Set the cost of datum stored per day (accumulating).
	 *
	 * @param datumDaysStoredCost
	 *        the cost to set (null will be stored as {@literal 0}
	 */
	public void setDatumDaysStoredCost(BigDecimal datumDaysStoredCost) {
		if ( datumDaysStoredCost == null ) {
			datumDaysStoredCost = BigDecimal.ZERO;
		}
		this.datumDaysStoredCost = datumDaysStoredCost;
	}

	/**
	 * Get the cost of datum queried.
	 *
	 * @return the cost, never {@literal null}
	 */
	public BigDecimal getDatumOutCost() {
		return datumOutCost;
	}

	/**
	 * Set the cost of datum queried.
	 *
	 * @param datumOutCost
	 *        the cost to set (null will be stored as {@literal 0}
	 */
	public void setDatumOutCost(BigDecimal datumOutCost) {
		if ( datumOutCost == null ) {
			datumOutCost = BigDecimal.ZERO;
		}
		this.datumOutCost = datumOutCost;
	}

	/**
	 * Get the instructions issued cost.
	 *
	 * @return the cost
	 */
	public BigDecimal getInstructionsIssuedCost() {
		return instructionsIssuedCost;
	}

	/**
	 * Set the instructions issued cost.
	 *
	 * @param instructionsIssuedCost
	 *        the cost to set
	 */
	public void setInstructionsIssuedCost(BigDecimal instructionsIssuedCost) {
		this.instructionsIssuedCost = instructionsIssuedCost;
	}

	/**
	 * Get the OCPP Chargers cost.
	 *
	 * @return the cost, never {@literal null}
	 */
	public BigDecimal getOcppChargersCost() {
		return ocppChargersCost;
	}

	/**
	 * Set the OCPP Chargers cost.
	 *
	 * @param ocppChargersCost
	 *        the cost to set
	 */
	public void setOcppChargersCost(BigDecimal ocppChargersCost) {
		if ( ocppChargersCost == null ) {
			ocppChargersCost = BigDecimal.ZERO;
		}
		this.ocppChargersCost = ocppChargersCost;
	}

	/**
	 * Get the OSCP Capacity Groups cost.
	 *
	 * @return the cost, never {@literal null}
	 */
	public BigDecimal getOscpCapacityGroupsCost() {
		return oscpCapacityGroupsCost;
	}

	/**
	 * Set the OSCP Capacity Groups cost.
	 *
	 * @param oscpCapacityGroupsCost
	 *        the cost to set
	 */
	public void setOscpCapacityGroupsCost(BigDecimal oscpCapacityGroupsCost) {
		if ( oscpCapacityGroupsCost == null ) {
			oscpCapacityGroupsCost = BigDecimal.ZERO;
		}
		this.oscpCapacityGroupsCost = oscpCapacityGroupsCost;
	}

	/**
	 * Get the OSCP capacity cost.
	 *
	 * @return the cost, never {@literal null}
	 * @since 1.4
	 */
	public BigDecimal getOscpCapacityCost() {
		return oscpCapacityCost;
	}

	/**
	 * Set the OSCP capacity cost.
	 *
	 * @param oscpCapacityCost
	 *        the cost to set
	 * @since 1.4
	 */
	public void setOscpCapacityCost(BigDecimal oscpCapacityCost) {
		if ( oscpCapacityCost == null ) {
			oscpCapacityCost = BigDecimal.ZERO;
		}
		this.oscpCapacityCost = oscpCapacityCost;
	}

	/**
	 * Get the DNP3 Data Points cost.
	 *
	 * @return the cost, never {@literal null}
	 * @since 1.2
	 */
	public BigDecimal getDnp3DataPointsCost() {
		return dnp3DataPointsCost;
	}

	/**
	 * Set the DNP3 Data Points cost.
	 *
	 * @param dnp3DataPointsCost
	 *        the cost to set
	 * @since 1.2
	 */
	public void setDnp3DataPointsCost(BigDecimal dnp3DataPointsCost) {
		if ( dnp3DataPointsCost == null ) {
			dnp3DataPointsCost = BigDecimal.ZERO;
		}
		this.dnp3DataPointsCost = dnp3DataPointsCost;
	}

	/**
	 * Get the SolarFlux data in cost.
	 *
	 * @return the cost, never {@literal null}
	 * @since 1.5
	 */
	public final BigDecimal getFluxDataInCost() {
		return fluxDataInCost;
	}

	/**
	 * Set the SolarFlux data in cost.
	 *
	 * @param fluxDataInCost
	 *        the cost to set
	 * @since 1.5
	 */
	public final void setFluxDataInCost(BigDecimal fluxDataInCost) {
		if ( fluxDataInCost == null ) {
			fluxDataInCost = BigDecimal.ZERO;
		}
		this.fluxDataInCost = fluxDataInCost;
	}

	/**
	 * Get the SolarFlux data out cost.
	 *
	 * @return the cost, never {@literal null}
	 * @since 1.5
	 */
	public final BigDecimal getFluxDataOutCost() {
		return fluxDataOutCost;
	}

	/**
	 * Set the SolarFlux data out cost.
	 *
	 * @param fluxDataOutCost
	 *        the cost to set
	 * @since 1.5
	 */
	public final void setFluxDataOutCost(BigDecimal fluxDataOutCost) {
		if ( fluxDataOutCost == null ) {
			fluxDataOutCost = BigDecimal.ZERO;
		}
		this.fluxDataOutCost = fluxDataOutCost;
	}

	/**
	 * Get the OAuth client credentials cost.
	 *
	 * @return the cost, never {@literal null}
	 * @since 1.5
	 */
	public final BigDecimal getOauthClientCredentialsCost() {
		return oauthClientCredentialsCost;
	}

	/**
	 * Set the OAuth client credentials cost.
	 *
	 * @param oauthClientCredentialsCost
	 *        the cost to set
	 * @since 1.5
	 */
	public final void setOauthClientCredentialsCost(BigDecimal oauthClientCredentialsCost) {
		if ( oauthClientCredentialsCost == null ) {
			oauthClientCredentialsCost = BigDecimal.ZERO;
		}
		this.oauthClientCredentialsCost = oauthClientCredentialsCost;
	}

	/**
	 * Get the Cloud Integrations data cost.
	 *
	 * @return the cost, never {@literal null}
	 * @since 1.6
	 */
	public final BigDecimal getCloudIntegrationsDataCost() {
		return cloudIntegrationsDataCost;
	}

	/**
	 * Set the Cloud Integrations data cost.
	 *
	 * @param cloudIntegrationsDataCost
	 *        the cost to set
	 * @since 1.6
	 */
	public final void setCloudIntegrationsDataCost(BigDecimal cloudIntegrationsDataCost) {
		if ( cloudIntegrationsDataCost == null ) {
			cloudIntegrationsDataCost = BigDecimal.ZERO;
		}
		this.cloudIntegrationsDataCost = cloudIntegrationsDataCost;
	}

}
