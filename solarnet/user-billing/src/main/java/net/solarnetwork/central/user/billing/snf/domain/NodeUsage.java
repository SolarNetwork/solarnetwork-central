/* ==================================================================
 * NodeUsage.java - 22/07/2020 10:05:31 AM
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

import static java.util.Arrays.stream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import com.fasterxml.jackson.annotation.JsonIgnore;
import net.solarnetwork.central.user.billing.domain.InvoiceItemUsageRecord;
import net.solarnetwork.central.user.billing.domain.InvoiceUsageRecord;
import net.solarnetwork.dao.BasicLongEntity;
import net.solarnetwork.domain.Differentiable;
import net.solarnetwork.util.ArrayUtils;

/**
 * Usage details for a single node.
 *
 * <p>
 * The {@link #getId()} represents the node ID this usage relates to.
 * </p>
 *
 * <p>
 * The usage date range is defined in whatever associated entity refers to this
 * usage. For example for an {@link SnfInvoice} the date range would be that of
 * the invoice. The costs are shown in the system's <i>normalized</i> currency,
 * and must be translated into real currencies elsewhere.
 * </p>
 *
 * @author matt
 * @version 2.7
 */
public class NodeUsage extends BasicLongEntity
		implements InvoiceUsageRecord<Long>, Differentiable<NodeUsage>, NodeUsages {

	private static final long serialVersionUID = 6848689122993706234L;

	/**
	 * Comparator that sorts {@link NodeUsage} objects by {@code id} in
	 * ascending order.
	 */
	public static final Comparator<NodeUsage> SORT_BY_NODE_ID = new NodeUsageNodeIdComparator();

	private String description;
	private BigInteger datumPropertiesIn;
	private BigInteger datumOut;
	private BigInteger datumDaysStored;
	private BigInteger instructionsIssued;
	private BigInteger fluxDataIn;
	private BigInteger fluxDataOut;
	private BigInteger ocppChargers;
	private BigInteger oscpCapacityGroups;
	private BigInteger oscpCapacity;
	private BigInteger dnp3DataPoints;
	private BigInteger oauthClientCredentials;
	private BigInteger cloudIntegrationsData;
	private final NodeUsageCost costs;
	private BigDecimal totalCost;

	private BigInteger[] datumPropertiesInTiers;
	private BigInteger[] datumOutTiers;
	private BigInteger[] datumDaysStoredTiers;
	private BigInteger[] instructionsIssuedTiers;
	private BigInteger[] fluxDataInTiers;
	private BigInteger[] fluxDataOutTiers;
	private BigInteger[] ocppChargersTiers;
	private BigInteger[] oscpCapacityGroupsTiers;
	private BigInteger[] oscpCapacityTiers;
	private BigInteger[] dnp3DataPointsTiers;
	private BigInteger[] oauthClientCredentialsTiers;
	private BigInteger[] cloudIntegrationsDataTiers;
	private NodeUsageCost[] costsTiers;

	/**
	 * Compare {@link NodeUsage} instances by node ID in ascending order.
	 */
	public static final class NodeUsageNodeIdComparator implements Comparator<NodeUsage> {

		@Override
		public int compare(NodeUsage o1, NodeUsage o2) {
			return o1.compareTo(o2.getId());
		}

	}

	/**
	 * Constructor.
	 *
	 * <p>
	 * This creates a {@literal null} node ID, for usage not associated with a
	 * specific node.
	 * </p>
	 */
	public NodeUsage() {
		this(null);
	}

	/**
	 * Constructor.
	 *
	 * @param nodeId
	 *        the node ID
	 */
	public NodeUsage(Long nodeId) {
		this(nodeId, null);
	}

	/**
	 * Constructor.
	 *
	 * @param nodeId
	 *        the node ID
	 * @param created
	 *        the creation date
	 */
	public NodeUsage(Long nodeId, Instant created) {
		super(nodeId, created);
		setDatumPropertiesIn(BigInteger.ZERO);
		setDatumOut(BigInteger.ZERO);
		setDatumDaysStored(BigInteger.ZERO);
		setInstructionsIssued(BigInteger.ZERO);
		setFluxDataIn(BigInteger.ZERO);
		setFluxDataOut(BigInteger.ZERO);
		setOcppChargers(BigInteger.ZERO);
		setOscpCapacityGroups(BigInteger.ZERO);
		setOscpCapacity(BigInteger.ZERO);
		setDnp3DataPoints(BigInteger.ZERO);
		setOauthClientCredentials(BigInteger.ZERO);
		setCloudIntegrationsData(BigInteger.ZERO);
		setTotalCost(BigDecimal.ZERO);
		this.costs = new NodeUsageCost();
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("NodeUsage{");
		if ( getNodeId() != null ) {
			builder.append("nodeId=");
			builder.append(getNodeId());
			builder.append(", ");
		}
		builder.append("datumPropertiesIn=");
		builder.append(datumPropertiesIn);
		builder.append(", datumOut=");
		builder.append(datumOut);
		builder.append(", datumDaysStored=");
		builder.append(datumDaysStored);
		builder.append(", instructionsIssued=");
		builder.append(instructionsIssued);
		builder.append(", fluxDataIn=");
		builder.append(fluxDataIn);
		builder.append(", fluxDataOut=");
		builder.append(fluxDataOut);
		builder.append(", oscpCapacityGroups=");
		builder.append(oscpCapacityGroups);
		builder.append(", oscpCapacity=");
		builder.append(oscpCapacity);
		builder.append(", datumPropertiesInCost=");
		builder.append(costs.getDatumPropertiesInCost());
		builder.append(", datumOutCost=");
		builder.append(costs.getDatumOutCost());
		builder.append(", datumDaysStoredCost=");
		builder.append(costs.getDatumDaysStoredCost());
		builder.append(", instructionsIssuedCost=");
		builder.append(costs.getInstructionsIssuedCost());
		builder.append(", ocppChargersCost=");
		builder.append(costs.getOcppChargersCost());
		builder.append(", oscpCapacityGroupsCost=");
		builder.append(costs.getOscpCapacityGroupsCost());
		builder.append(", oscpCapacityCost=");
		builder.append(costs.getOscpCapacityCost());
		builder.append(", dnp3DataPointsCost=");
		builder.append(costs.getDnp3DataPointsCost());
		builder.append(", oauthClientCredentials=");
		builder.append(oauthClientCredentials);
		builder.append(", cloudIntegrationsData=");
		builder.append(cloudIntegrationsData);
		builder.append(", totalCost=");
		builder.append(totalCost);
		builder.append("}");
		return builder.toString();
	}

	/**
	 * Test if the properties of another entity are the same as in this
	 * instance.
	 *
	 * <p>
	 * The {@code nodeId} and all {@code cost} properties are not compared by
	 * this method.
	 * </p>
	 *
	 * @param other
	 *        the other entity to compare to
	 * @return {@literal true} if the properties of this instance are equal to
	 *         the other
	 */
	public boolean isSameAs(NodeUsage other) {
		if ( other == null ) {
			return false;
		}
		// @formatter:off
		return Objects.equals(datumPropertiesIn, other.datumPropertiesIn)
				&& Objects.equals(datumOut, other.datumOut)
				&& Objects.equals(datumDaysStored, other.datumDaysStored)
				&& Objects.equals(instructionsIssued, other.instructionsIssued)
				&& Objects.equals(fluxDataIn, other.fluxDataIn)
				&& Objects.equals(fluxDataOut, other.fluxDataOut)
				&& Objects.equals(ocppChargers, other.ocppChargers)
				&& Objects.equals(oscpCapacityGroups, other.oscpCapacityGroups)
				&& Objects.equals(oscpCapacity, other.oscpCapacity)
				&& Objects.equals(dnp3DataPoints, other.dnp3DataPoints)
				&& Objects.equals(oauthClientCredentials, other.oauthClientCredentials)
				&& Objects.equals(cloudIntegrationsData, other.cloudIntegrationsData)
				;
		// @formatter:on
	}

	@Override
	public boolean differsFrom(NodeUsage other) {
		return !isSameAs(other);
	}

	@Override
	public Long getUsageKey() {
		return getNodeId();
	}

	@Override
	public String getDescription() {
		return description;
	}

	/**
	 * Set an optional description, such as a node name.
	 *
	 * @param description
	 *        the description to set
	 * @since 2.1
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	@Override
	public List<InvoiceItemUsageRecord> getUsageRecords() {
		List<InvoiceItemUsageRecord> recs = new ArrayList<>(3);
		recs.add(new UsageInfo(DATUM_PROPS_IN_KEY, new BigDecimal(datumPropertiesIn),
				costs.getDatumPropertiesInCost()));
		recs.add(new UsageInfo(DATUM_OUT_KEY, new BigDecimal(datumOut), costs.getDatumOutCost()));
		recs.add(new UsageInfo(DATUM_DAYS_STORED_KEY, new BigDecimal(datumDaysStored),
				costs.getDatumDaysStoredCost()));
		recs.add(new UsageInfo(INSTRUCTIONS_ISSUED_KEY, new BigDecimal(instructionsIssued),
				costs.getInstructionsIssuedCost()));
		recs.add(new UsageInfo(FLUX_DATA_IN_KEY, new BigDecimal(fluxDataIn), costs.getFluxDataInCost()));
		return recs;
	}

	/**
	 * Get the node ID.
	 *
	 * @return the node ID
	 */
	public Long getNodeId() {
		return getId();
	}

	/**
	 * Get the node usage costs.
	 *
	 * @return the costs, never {@literal null}
	 */
	@JsonIgnore
	public NodeUsageCost getCosts() {
		return costs;
	}

	/**
	 * Get the count of datum properties added.
	 *
	 * @return the count, never {@literal null}
	 */
	public BigInteger getDatumPropertiesIn() {
		return datumPropertiesIn;
	}

	/**
	 * Set the count of datum properties added.
	 *
	 * @param datumPropertiesIn
	 *        the count to set; if {@literal null} then {@literal 0} will be
	 *        stored
	 */
	public void setDatumPropertiesIn(BigInteger datumPropertiesIn) {
		if ( datumPropertiesIn == null ) {
			datumPropertiesIn = BigInteger.ZERO;
		}
		this.datumPropertiesIn = datumPropertiesIn;
	}

	/**
	 * Get the cost of datum properties added.
	 *
	 * @return the cost
	 */
	public BigDecimal getDatumPropertiesInCost() {
		return costs.getDatumPropertiesInCost();
	}

	/**
	 * Set the cost of datum properties added.
	 *
	 * @param datumPropertiesInCost
	 *        the cost to set
	 */
	public void setDatumPropertiesInCost(BigDecimal datumPropertiesInCost) {
		costs.setDatumPropertiesInCost(datumPropertiesInCost);
	}

	/**
	 * Get the count of datum stored per day (accumulating).
	 *
	 * @return the count
	 */
	public BigInteger getDatumDaysStored() {
		return datumDaysStored;
	}

	/**
	 * Set the count of datum stored per day (accumulating).
	 *
	 * @param datumDaysStored
	 *        the count to set; if {@literal null} then {@literal 0} will be
	 *        stored
	 */
	public void setDatumDaysStored(BigInteger datumDaysStored) {
		if ( datumDaysStored == null ) {
			datumDaysStored = BigInteger.ZERO;
		}
		this.datumDaysStored = datumDaysStored;
	}

	/**
	 * Get the cost of datum stored per day (accumulating).
	 *
	 * @return the cost
	 */
	public BigDecimal getDatumDaysStoredCost() {
		return costs.getDatumDaysStoredCost();
	}

	/**
	 * Set the cost of datum stored per day (accumulating).
	 *
	 * @param datumDaysStoredCost
	 *        the cost to set
	 */
	public void setDatumDaysStoredCost(BigDecimal datumDaysStoredCost) {
		costs.setDatumDaysStoredCost(datumDaysStoredCost);
	}

	/**
	 * Get the count of datum queried.
	 *
	 * @return the count
	 */
	public BigInteger getDatumOut() {
		return datumOut;
	}

	/**
	 * Set the count of datum queried.
	 *
	 * @param datumOut
	 *        the count to set; if {@literal null} then {@literal 0} will be
	 *        stored
	 */
	public void setDatumOut(BigInteger datumOut) {
		if ( datumOut == null ) {
			datumOut = BigInteger.ZERO;
		}
		this.datumOut = datumOut;
	}

	/**
	 * Get the cost of datum queried.
	 *
	 * @return the cost
	 */
	public BigDecimal getDatumOutCost() {
		return costs.getDatumOutCost();
	}

	/**
	 * Set the cost of datum queried.
	 *
	 * @param datumOutCost
	 *        the cost to set
	 */
	public void setDatumOutCost(BigDecimal datumOutCost) {
		costs.setDatumOutCost(datumOutCost);
	}

	/**
	 * Get the overall cost.
	 *
	 * @return the cost, never {@literal null}
	 */
	public BigDecimal getTotalCost() {
		return totalCost;
	}

	/**
	 * Set the overall cost.
	 *
	 * @param totalCost
	 *        the cost to set; if {@literal null} then {@literal 0} will be
	 *        stored
	 */
	public void setTotalCost(BigDecimal totalCost) {
		this.totalCost = totalCost != null ? totalCost : BigDecimal.ZERO;
	}

	/**
	 * Get the count of OCPP Chargers.
	 *
	 * @return the count
	 */
	public BigInteger getOcppChargers() {
		return ocppChargers;
	}

	/**
	 * Set the count of OCPP Chargers.
	 *
	 * @param ocppChargers
	 *        the count to set; if {@literal null} then {@literal 0} will be
	 *        stored
	 */
	public void setOcppChargers(BigInteger ocppChargers) {
		if ( ocppChargers == null ) {
			ocppChargers = BigInteger.ZERO;
		}
		this.ocppChargers = ocppChargers;
	}

	/**
	 * Get the cost of OCPP Chargers.
	 *
	 * @return the cost
	 */
	public BigDecimal getOcppChargersCost() {
		return costs.getOcppChargersCost();
	}

	/**
	 * Set the cost of OCPP Chargers.
	 *
	 * @param ocppChargersCost
	 *        the cost to set
	 */
	public void setOcppChargersCost(BigDecimal ocppChargersCost) {
		costs.setOcppChargersCost(ocppChargersCost);
	}

	/**
	 * Get the count of OSCP Capacity Groups.
	 *
	 * @return the count
	 */
	public BigInteger getOscpCapacityGroups() {
		return oscpCapacityGroups;
	}

	/**
	 * Set the count of OSCP Capacity Groups.
	 *
	 * @param oscpCapacityGroups
	 *        the count to set; if {@literal null} then {@literal 0} will be
	 *        stored
	 */
	public void setOscpCapacityGroups(BigInteger oscpCapacityGroups) {
		if ( oscpCapacityGroups == null ) {
			oscpCapacityGroups = BigInteger.ZERO;
		}
		this.oscpCapacityGroups = oscpCapacityGroups;
	}

	/**
	 * Get the cost of OSCP Capacity Groups.
	 *
	 * @return the cost
	 */
	public BigDecimal getOscpCapacityGroupsCost() {
		return costs.getOscpCapacityGroupsCost();
	}

	/**
	 * Set the cost of OSCP Capacity Groups.
	 *
	 * @param oscpCapacityGroupsCost
	 *        the cost to set
	 */
	public void setOscpCapacityGroupsCost(BigDecimal oscpCapacityGroupsCost) {
		costs.setOscpCapacityGroupsCost(oscpCapacityGroupsCost);
	}

	private void prepCostsTiers(Object[] array) {
		final int len = Math.max(costsTiers != null ? costsTiers.length : 0,
				array != null ? array.length : 0);
		costsTiers = ArrayUtils.arrayWithLength(costsTiers, len, NodeUsageCost.class, null);
	}

	private static BigDecimal[] getTierCostValues(NodeUsageCost[] costsTiers,
			Function<NodeUsageCost, BigDecimal> f) {
		BigDecimal[] result = null;
		if ( costsTiers != null ) {
			result = stream(costsTiers).map(f).toArray(BigDecimal[]::new);
		}
		return result;
	}

	private static BigInteger[] decimalsToIntegers(BigDecimal[] decimals) {
		BigInteger[] ints = null;
		if ( decimals != null ) {
			ints = new BigInteger[decimals.length];
			for ( int i = 0, len = decimals.length; i < len; i++ ) {
				ints[i] = decimals[i].toBigInteger();
			}
		}
		return ints;
	}

	private static List<NamedCost> tiersCostBreakdown(BigInteger[] counts, NodeUsageCost[] costsTiers,
			Function<NodeUsageCost, BigDecimal> f) {
		if ( counts == null || counts.length < 1 ) {
			return Collections.emptyList();
		}
		List<NamedCost> result = new ArrayList<>(counts != null ? counts.length : 4);
		for ( int i = 0; i < counts.length; i++ ) {
			BigInteger q = counts[i];
			if ( BigInteger.ZERO.compareTo(q) == 0 ) {
				break;
			}
			BigDecimal c = (costsTiers != null && i < costsTiers.length ? f.apply(costsTiers[i]) : null);
			result.add(NamedCost.forTier((i + 1), q, c));
		}
		return result;
	}

	/**
	 * Get the overall usage tiers breakdown.
	 *
	 * <p>
	 * The resulting map contains the following keys:
	 * </p>
	 * <ol>
	 * <li>{@link #DATUM_PROPS_IN_KEY}</li>
	 * <li>{@link #DATUM_OUT_KEY}</li>
	 * <li>{@link #DATUM_DAYS_STORED_KEY}</li>
	 * <li>{@link #INSTRUCTIONS_ISSUED_KEY}</li>
	 * <li>{@link #FLUX_DATA_IN_KEY}</li>
	 * <li>{@link #FLUX_DATA_OUT_KEY}</li>
	 * <li>{@link #OCPP_CHARGERS_KEY}</li>
	 * <li>{@link #OSCP_CAPACITY_GROUPS_KEY}</li>
	 * <li>{@link #OSCP_CAPACITY_KEY}</li>
	 * <li>{@link #DNP3_DATA_POINTS_KEY}</li>
	 * <li>{@link #OAUTH_CLIENT_CREDENTIALS_KEY}</li>
	 * <li>{@link #CLOUD_INTEGRATIONS_DATA_KEY}</li>
	 * </ol>
	 *
	 * @return the map, never {@literal null}
	 */
	public Map<String, List<NamedCost>> getTiersCostBreakdown() {
		Map<String, List<NamedCost>> result = new LinkedHashMap<>(4);
		result.put(DATUM_PROPS_IN_KEY, getDatumPropertiesInTiersCostBreakdown());
		result.put(DATUM_OUT_KEY, getDatumOutTiersCostBreakdown());
		result.put(DATUM_DAYS_STORED_KEY, getDatumDaysStoredTiersCostBreakdown());
		result.put(INSTRUCTIONS_ISSUED_KEY, getInstructionsIssuedTiersCostBreakdown());
		result.put(FLUX_DATA_IN_KEY, getFluxDataInTiersCostBreakdown());
		result.put(FLUX_DATA_OUT_KEY, getFluxDataOutTiersCostBreakdown());
		result.put(OCPP_CHARGERS_KEY, getOcppChargersTiersCostBreakdown());
		result.put(OSCP_CAPACITY_GROUPS_KEY, getOscpCapacityGroupsTiersCostBreakdown());
		result.put(OSCP_CAPACITY_KEY, getOscpCapacityTiersCostBreakdown());
		result.put(DNP3_DATA_POINTS_KEY, getDnp3DataPointsTiersCostBreakdown());
		result.put(OAUTH_CLIENT_CREDENTIALS_KEY, getOauthClientCredentialsTiersCostBreakdown());
		result.put(CLOUD_INTEGRATIONS_DATA_KEY, getCloudIntegrationsDataTiersCostBreakdown());
		return result;
	}

	/**
	 * Get the overall usage information.
	 *
	 * <p>
	 * The resulting map contains the following keys:
	 * </p>
	 * <ol>
	 * <li>{@link #DATUM_PROPS_IN_KEY}</li>
	 * <li>{@link #DATUM_OUT_KEY}</li>
	 * <li>{@link #DATUM_DAYS_STORED_KEY}</li>
	 * <li>{@link #INSTRUCTIONS_ISSUED_KEY}</li>
	 * <li>{@link #FLUX_DATA_IN_KEY}</li>
	 * <li>{@link #FLUX_DATA_OUT_KEY}</li>
	 * <li>{@link #OCPP_CHARGERS_KEY}</li>
	 * <li>{@link #OSCP_CAPACITY_GROUPS_KEY}</li>
	 * <li>{@link #OSCP_CAPACITY_KEY}</li>
	 * <li>{@link #DNP3_DATA_POINTS_KEY}</li>
	 * <li>{@link #OAUTH_CLIENT_CREDENTIALS_KEY}</li>
	 * <li>{@link #CLOUD_INTEGRATIONS_DATA_KEY}</li>
	 * </ol>
	 *
	 * @return the map, never {@literal null}
	 */
	public Map<String, UsageInfo> getUsageInfo() {
		Map<String, UsageInfo> result = new LinkedHashMap<>(4);
		result.put(DATUM_PROPS_IN_KEY, new UsageInfo(DATUM_PROPS_IN_KEY,
				new BigDecimal(datumPropertiesIn), costs.getDatumPropertiesInCost()));
		result.put(DATUM_OUT_KEY,
				new UsageInfo(DATUM_OUT_KEY, new BigDecimal(datumOut), costs.getDatumOutCost()));
		result.put(DATUM_DAYS_STORED_KEY, new UsageInfo(DATUM_DAYS_STORED_KEY,
				new BigDecimal(datumDaysStored), costs.getDatumDaysStoredCost()));
		result.put(INSTRUCTIONS_ISSUED_KEY, new UsageInfo(INSTRUCTIONS_ISSUED_KEY,
				new BigDecimal(instructionsIssued), costs.getInstructionsIssuedCost()));
		result.put(FLUX_DATA_IN_KEY,
				new UsageInfo(FLUX_DATA_IN_KEY, new BigDecimal(fluxDataIn), costs.getFluxDataInCost()));
		result.put(FLUX_DATA_OUT_KEY, new UsageInfo(FLUX_DATA_OUT_KEY, new BigDecimal(fluxDataOut),
				costs.getFluxDataOutCost()));
		result.put(OCPP_CHARGERS_KEY, new UsageInfo(OCPP_CHARGERS_KEY, new BigDecimal(ocppChargers),
				costs.getOcppChargersCost()));
		result.put(OSCP_CAPACITY_GROUPS_KEY, new UsageInfo(OSCP_CAPACITY_GROUPS_KEY,
				new BigDecimal(oscpCapacityGroups), costs.getOscpCapacityGroupsCost()));
		result.put(OSCP_CAPACITY_KEY, new UsageInfo(OSCP_CAPACITY_KEY, new BigDecimal(oscpCapacity),
				costs.getOscpCapacityCost()));
		result.put(DNP3_DATA_POINTS_KEY, new UsageInfo(DNP3_DATA_POINTS_KEY,
				new BigDecimal(dnp3DataPoints), costs.getDnp3DataPointsCost()));
		result.put(OAUTH_CLIENT_CREDENTIALS_KEY, new UsageInfo(OAUTH_CLIENT_CREDENTIALS_KEY,
				new BigDecimal(oauthClientCredentials), costs.getOauthClientCredentialsCost()));
		result.put(CLOUD_INTEGRATIONS_DATA_KEY, new UsageInfo(CLOUD_INTEGRATIONS_DATA_KEY,
				new BigDecimal(cloudIntegrationsData), costs.getCloudIntegrationsDataCost()));
		return result;
	}

	/**
	 * Get the node usage costs, per tier.
	 *
	 * @return the costs per tier
	 */
	@JsonIgnore
	public NodeUsageCost[] getCostsTiers() {
		return costsTiers;
	}

	/**
	 * Set the node usage costs.
	 *
	 * @param costTiers
	 *        the costs to set
	 */
	public void setCostsTiers(NodeUsageCost[] costTiers) {
		this.costsTiers = costTiers;
	}

	/**
	 * Get the node datum properties tier cost breakdown.
	 *
	 * @return the costs, never {@literal null}
	 */
	@JsonIgnore
	public List<NamedCost> getDatumPropertiesInTiersCostBreakdown() {
		return tiersCostBreakdown(datumPropertiesInTiers, costsTiers,
				NodeUsageCost::getDatumPropertiesInCost);
	}

	/**
	 * Get the count of datum properties added, per tier.
	 *
	 * @return the counts
	 */
	public BigInteger[] getDatumPropertiesInTiers() {
		return datumPropertiesInTiers;
	}

	/**
	 * Set the count of datum properties added, per tier.
	 *
	 * @param datumPropertiesInTiers
	 *        the counts to set
	 */
	public void setDatumPropertiesInTiers(BigInteger[] datumPropertiesInTiers) {
		this.datumPropertiesInTiers = datumPropertiesInTiers;
	}

	/**
	 * Set the count of datum properties added, per tier, as decimal values.
	 *
	 * @param datumPropertiesInTiers
	 *        the counts to set
	 */
	public void setDatumPropertiesInTiersNumeric(BigDecimal[] datumPropertiesInTiers) {
		this.datumPropertiesInTiers = decimalsToIntegers(datumPropertiesInTiers);
	}

	/**
	 * Get the cost of datum properties added, per tier.
	 *
	 * @return the costs
	 */
	public BigDecimal[] getDatumPropertiesInCostTiers() {
		return getTierCostValues(costsTiers, NodeUsageCost::getDatumPropertiesInCost);
	}

	/**
	 * Set the cost of datum properties added.
	 *
	 * @param datumPropertiesInCost
	 *        the cost to set
	 */
	public void setDatumPropertiesInCostTiers(BigDecimal[] datumPropertiesInCost) {
		prepCostsTiers(datumPropertiesInCost);
		for ( int i = 0; i < costsTiers.length; i++ ) {
			BigDecimal val = (datumPropertiesInCost != null && i < datumPropertiesInCost.length
					? datumPropertiesInCost[i]
					: null);
			costsTiers[i].setDatumPropertiesInCost(val);
		}
	}

	/**
	 * Get the node datum properties tier cost breakdown.
	 *
	 * @return the costs, never {@literal null}
	 */
	@JsonIgnore
	public List<NamedCost> getDatumDaysStoredTiersCostBreakdown() {
		return tiersCostBreakdown(datumDaysStoredTiers, costsTiers,
				NodeUsageCost::getDatumDaysStoredCost);
	}

	/**
	 * Get the count of datum stored per day (accumulating), per tier.
	 *
	 * @return the counts
	 */
	public BigInteger[] getDatumDaysStoredTiers() {
		return datumDaysStoredTiers;
	}

	/**
	 * Set the count of datum stored per day (accumulating), per tier.
	 *
	 * @param datumDaysStoredTiers
	 *        the counts to set
	 */
	public void setDatumDaysStoredTiers(BigInteger[] datumDaysStoredTiers) {
		this.datumDaysStoredTiers = datumDaysStoredTiers;
	}

	/**
	 * Set the count of datum stored per day (accumulating), per tier, as
	 * decimals.
	 *
	 * @param datumDaysStoredTiers
	 *        the counts to set
	 */
	public void setDatumDaysStoredTiersNumeric(BigDecimal[] datumDaysStoredTiers) {
		this.datumDaysStoredTiers = decimalsToIntegers(datumDaysStoredTiers);
	}

	/**
	 * Get the cost of datum stored per day (accumulating).
	 *
	 * @return the cost
	 */
	public BigDecimal[] getDatumDaysStoredCostTiers() {
		return getTierCostValues(costsTiers, NodeUsageCost::getDatumDaysStoredCost);
	}

	/**
	 * Set the cost of datum stored per day (accumulating).
	 *
	 * @param datumDaysStoredCostTiers
	 *        the costs to set
	 */
	public void setDatumDaysStoredCostTiers(BigDecimal[] datumDaysStoredCostTiers) {
		prepCostsTiers(datumDaysStoredCostTiers);
		for ( int i = 0; i < costsTiers.length; i++ ) {
			BigDecimal val = (datumDaysStoredCostTiers != null && i < datumDaysStoredCostTiers.length
					? datumDaysStoredCostTiers[i]
					: null);
			costsTiers[i].setDatumDaysStoredCost(val);
		}
	}

	/**
	 * Get the node datum properties tier cost breakdown.
	 *
	 * @return the costs, never {@literal null}
	 */
	@JsonIgnore
	public List<NamedCost> getDatumOutTiersCostBreakdown() {
		return tiersCostBreakdown(datumOutTiers, costsTiers, NodeUsageCost::getDatumOutCost);
	}

	/**
	 * Get the count of datum queried.
	 *
	 * @return the count
	 */
	public BigInteger[] getDatumOutTiers() {
		return datumOutTiers;
	}

	/**
	 * Set the count of datum queried, per tier.
	 *
	 * @param datumOutTiers
	 *        the counts to set
	 */
	public void setDatumOutTiers(BigInteger[] datumOutTiers) {
		this.datumOutTiers = datumOutTiers;
	}

	/**
	 * Set the count of datum queried, per tier, as decimals.
	 *
	 * @param datumOutTiers
	 *        the counts to set
	 */
	public void setDatumOutTiersNumeric(BigDecimal[] datumOutTiers) {
		this.datumOutTiers = decimalsToIntegers(datumOutTiers);
	}

	/**
	 * Get the cost of datum queried, per tier.
	 *
	 * @return the costs
	 */
	public BigDecimal[] getDatumOutCostTiers() {
		return getTierCostValues(costsTiers, NodeUsageCost::getDatumOutCost);
	}

	/**
	 * Set the cost of datum queried, per tier.
	 *
	 * @param datumOutCostTiers
	 *        the costs to set
	 */
	public void setDatumOutCostTiers(BigDecimal[] datumOutCostTiers) {
		prepCostsTiers(datumOutCostTiers);
		for ( int i = 0; i < costsTiers.length; i++ ) {
			BigDecimal val = (datumOutCostTiers != null && i < datumOutCostTiers.length
					? datumOutCostTiers[i]
					: null);
			costsTiers[i].setDatumOutCost(val);
		}
	}

	/**
	 * Get the OCPP Chargers tier cost breakdown.
	 *
	 * @return the costs, never {@literal null}
	 */
	@JsonIgnore
	public List<NamedCost> getOcppChargersTiersCostBreakdown() {
		return tiersCostBreakdown(ocppChargersTiers, costsTiers, NodeUsageCost::getOcppChargersCost);
	}

	/**
	 * Get the count of OCPP Chargers, per tier.
	 *
	 * @return the counts
	 */
	public BigInteger[] getOcppChargersTiers() {
		return ocppChargersTiers;
	}

	/**
	 * Set the count of OCPP Chargers, per tier.
	 *
	 * @param ocppChargersTiers
	 *        the counts to set
	 */
	public void setOcppChargersTiers(BigInteger[] ocppChargersTiers) {
		this.ocppChargersTiers = ocppChargersTiers;
	}

	/**
	 * Set the count of OCPP Chargers, per tier, as decimals.
	 *
	 * @param ocppChargersTiers
	 *        the counts to set
	 */
	public void setOcppChargersTiersNumeric(BigDecimal[] ocppChargersTiers) {
		this.ocppChargersTiers = decimalsToIntegers(ocppChargersTiers);
	}

	/**
	 * Get the cost of OCPP Chargers, per tier.
	 *
	 * @return the cost
	 */
	public BigDecimal[] getOcppChargersCostTiers() {
		return getTierCostValues(costsTiers, NodeUsageCost::getOcppChargersCost);
	}

	/**
	 * Set the cost of OCPP Chargers, per tier.
	 *
	 * @param ocppChargersCostTiers
	 *        the costs to set
	 */
	public void setOcppChargersCostTiers(BigDecimal[] ocppChargersCostTiers) {
		prepCostsTiers(ocppChargersCostTiers);
		for ( int i = 0; i < costsTiers.length; i++ ) {
			BigDecimal val = (ocppChargersCostTiers != null && i < ocppChargersCostTiers.length
					? ocppChargersCostTiers[i]
					: null);
			costsTiers[i].setOcppChargersCost(val);
		}
	}

	/**
	 * Get the OSCP Capacity Groups tier cost breakdown.
	 *
	 * @return the costs, never {@literal null}
	 */
	@JsonIgnore
	public List<NamedCost> getOscpCapacityGroupsTiersCostBreakdown() {
		return tiersCostBreakdown(oscpCapacityGroupsTiers, costsTiers,
				NodeUsageCost::getOscpCapacityGroupsCost);
	}

	/**
	 * Get the count of OSCP Capacity Groups, per tier.
	 *
	 * @return the counts
	 */
	public BigInteger[] getOscpCapacityGroupsTiers() {
		return oscpCapacityGroupsTiers;
	}

	/**
	 * Set the count of OSCP Capacity Groups, per tier.
	 *
	 * @param oscpCapacityGroupsTiers
	 *        the counts to set
	 */
	public void setOscpCapacityGroupsTiers(BigInteger[] oscpCapacityGroupsTiers) {
		this.oscpCapacityGroupsTiers = oscpCapacityGroupsTiers;
	}

	/**
	 * Set the count of OSCP Capacity Groups, per tier, as decimals.
	 *
	 * @param oscpCapacityGroupsTiers
	 *        the counts to set
	 */
	public void setOscpCapacityGroupsTiersNumeric(BigDecimal[] oscpCapacityGroupsTiers) {
		this.oscpCapacityGroupsTiers = decimalsToIntegers(oscpCapacityGroupsTiers);
	}

	/**
	 * Get the cost of OSCP Capacity Groups, per tier.
	 *
	 * @return the cost
	 */
	public BigDecimal[] getOscpCapacityGroupsCostTiers() {
		return getTierCostValues(costsTiers, NodeUsageCost::getOscpCapacityGroupsCost);
	}

	/**
	 * Set the cost of OSCP Capacity Groups, per tier.
	 *
	 * @param oscpCapacityGroupsCostTiers
	 *        the costs to set
	 */
	public void setOscpCapacityGroupsCostTiers(BigDecimal[] oscpCapacityGroupsCostTiers) {
		prepCostsTiers(oscpCapacityGroupsCostTiers);
		for ( int i = 0; i < costsTiers.length; i++ ) {
			BigDecimal val = (oscpCapacityGroupsCostTiers != null
					&& i < oscpCapacityGroupsCostTiers.length ? oscpCapacityGroupsCostTiers[i] : null);
			costsTiers[i].setOscpCapacityGroupsCost(val);
		}
	}

	/**
	 * Get the count of DNP3 Data Points.
	 *
	 * @return the count
	 * @since 2.3
	 */
	public BigInteger getDnp3DataPoints() {
		return dnp3DataPoints;
	}

	/**
	 * Set the count of DNP3 Data Points.
	 *
	 * @param dnp3DataPoints
	 *        the count to set; if {@literal null} then {@literal 0} will be
	 *        stored
	 * @since 2.3
	 */
	public void setDnp3DataPoints(BigInteger dnp3DataPoints) {
		if ( dnp3DataPoints == null ) {
			dnp3DataPoints = BigInteger.ZERO;
		}
		this.dnp3DataPoints = dnp3DataPoints;
	}

	/**
	 * Get the cost of DNP3 Data Points.
	 *
	 * @return the cost
	 * @since 2.3
	 */
	public BigDecimal getDnp3DataPointsCost() {
		return costs.getDnp3DataPointsCost();
	}

	/**
	 * Set the cost of DNP3 Data Points.
	 *
	 * @param dnp3DataPointsCost
	 *        the cost to set
	 * @since 2.3
	 */
	public void setDnp3DataPointsCost(BigDecimal dnp3DataPointsCost) {
		costs.setDnp3DataPointsCost(dnp3DataPointsCost);
	}

	/**
	 * Get the DNP3 Data Points tier cost breakdown.
	 *
	 * @return the costs, never {@literal null}
	 * @since 2.3
	 */
	@JsonIgnore
	public List<NamedCost> getDnp3DataPointsTiersCostBreakdown() {
		return tiersCostBreakdown(dnp3DataPointsTiers, costsTiers, NodeUsageCost::getDnp3DataPointsCost);
	}

	/**
	 * Get the count of DNP3 Data Points, per tier.
	 *
	 * @return the counts
	 * @since 2.3
	 */
	public BigInteger[] getDnp3DataPointsTiers() {
		return dnp3DataPointsTiers;
	}

	/**
	 * Set the count of DNP3 Data Points, per tier.
	 *
	 * @param dnp3DataPointsTiers
	 *        the counts to set
	 * @since 2.3
	 */
	public void setDnp3DataPointsTiers(BigInteger[] dnp3DataPointsTiers) {
		this.dnp3DataPointsTiers = dnp3DataPointsTiers;
	}

	/**
	 * Set the count of DNP3 Data Points, per tier, as decimals.
	 *
	 * @param dnp3DataPointsTiers
	 *        the counts to set
	 * @since 2.3
	 */
	public void setDnp3DataPointsTiersNumeric(BigDecimal[] dnp3DataPointsTiers) {
		this.dnp3DataPointsTiers = decimalsToIntegers(dnp3DataPointsTiers);
	}

	/**
	 * Get the cost of DNP3 Data Points, per tier.
	 *
	 * @return the cost
	 * @since 2.3
	 */
	public BigDecimal[] getDnp3DataPointsCostTiers() {
		return getTierCostValues(costsTiers, NodeUsageCost::getDnp3DataPointsCost);
	}

	/**
	 * Set the cost of DNP3 Data Points, per tier.
	 *
	 * @param dnp3DataPointsCostTiers
	 *        the costs to set
	 * @since 2.3
	 */
	public void setDnp3DataPointsCostTiers(BigDecimal[] dnp3DataPointsCostTiers) {
		prepCostsTiers(dnp3DataPointsCostTiers);
		for ( int i = 0; i < costsTiers.length; i++ ) {
			BigDecimal val = (dnp3DataPointsCostTiers != null && i < dnp3DataPointsCostTiers.length
					? dnp3DataPointsCostTiers[i]
					: null);
			costsTiers[i].setDnp3DataPointsCost(val);
		}
	}

	/**
	 * Get the count of Instructions issued.
	 *
	 * @return the count
	 * @since 2.4
	 */
	public BigInteger getInstructionsIssued() {
		return instructionsIssued;
	}

	/**
	 * Set the count of Instructions issued.
	 *
	 * @param instructionsIssued
	 *        the count to set; if {@literal null} then {@literal 0} will be
	 *        stored
	 * @since 2.4
	 */
	public void setInstructionsIssued(BigInteger instructionsIssued) {
		if ( instructionsIssued == null ) {
			instructionsIssued = BigInteger.ZERO;
		}
		this.instructionsIssued = instructionsIssued;
	}

	/**
	 * Get the cost of Instructions issued.
	 *
	 * @return the cost
	 * @since 2.4
	 */
	public BigDecimal getInstructionsIssuedCost() {
		return costs.getInstructionsIssuedCost();
	}

	/**
	 * Set the cost of Instructions issued.
	 *
	 * @param instructionsIssuedCost
	 *        the cost to set
	 * @since 2.4
	 */
	public void setInstructionsIssuedCost(BigDecimal instructionsIssuedCost) {
		costs.setInstructionsIssuedCost(instructionsIssuedCost);
	}

	/**
	 * Get the Instructions issued tier cost breakdown.
	 *
	 * @return the costs, never {@literal null}
	 * @since 2.4
	 */
	@JsonIgnore
	public List<NamedCost> getInstructionsIssuedTiersCostBreakdown() {
		return tiersCostBreakdown(instructionsIssuedTiers, costsTiers,
				NodeUsageCost::getInstructionsIssuedCost);
	}

	/**
	 * Get the count of Instructions issued, per tier.
	 *
	 * @return the counts
	 * @since 2.4
	 */
	public BigInteger[] getInstructionsIssuedTiers() {
		return instructionsIssuedTiers;
	}

	/**
	 * Set the count of Instructions issued, per tier.
	 *
	 * @param instructionsIssuedTiers
	 *        the counts to set
	 * @since 2.4
	 */
	public void setInstructionsIssuedTiers(BigInteger[] instructionsIssuedTiers) {
		this.instructionsIssuedTiers = instructionsIssuedTiers;
	}

	/**
	 * Set the count of Instructions issued, per tier, as decimals.
	 *
	 * @param instructionsIssuedTiers
	 *        the counts to set
	 * @since 2.4
	 */
	public void setInstructionsIssuedTiersNumeric(BigDecimal[] instructionsIssuedTiers) {
		this.instructionsIssuedTiers = decimalsToIntegers(instructionsIssuedTiers);
	}

	/**
	 * Get the cost of Instructions issued, per tier.
	 *
	 * @return the cost
	 * @since 2.4
	 */
	public BigDecimal[] getInstructionsIssuedCostTiers() {
		return getTierCostValues(costsTiers, NodeUsageCost::getInstructionsIssuedCost);
	}

	/**
	 * Set the cost of Instructions issued, per tier.
	 *
	 * @param instructionsIssuedCostTiers
	 *        the costs to set
	 * @since 2.4
	 */
	public void setInstructionsIssuedCostTiers(BigDecimal[] instructionsIssuedCostTiers) {
		prepCostsTiers(instructionsIssuedCostTiers);
		for ( int i = 0; i < costsTiers.length; i++ ) {
			BigDecimal val = (instructionsIssuedCostTiers != null
					&& i < instructionsIssuedCostTiers.length ? instructionsIssuedCostTiers[i] : null);
			costsTiers[i].setInstructionsIssuedCost(val);
		}
	}

	/**
	 * Get the OSCP capacity.
	 *
	 * @return the count
	 * @since 2.5
	 */
	public BigInteger getOscpCapacity() {
		return oscpCapacity;
	}

	/**
	 * Set the count of OSCP capacity .
	 *
	 * @param oscpCapacity
	 *        the count to set; if {@literal null} then {@literal 0} will be
	 *        stored
	 * @since 2.5
	 */
	public void setOscpCapacity(BigInteger oscpCapacity) {
		if ( oscpCapacity == null ) {
			oscpCapacity = BigInteger.ZERO;
		}
		this.oscpCapacity = oscpCapacity;
	}

	/**
	 * Get the cost of OSCP capacity.
	 *
	 * @return the cost
	 * @since 2.5
	 */
	public BigDecimal getOscpCapacityCost() {
		return costs.getOscpCapacityCost();
	}

	/**
	 * Set the cost of OSCP capacity.
	 *
	 * @param oscpCapacityCost
	 *        the cost to set
	 * @since 2.5
	 */
	public void setOscpCapacityCost(BigDecimal oscpCapacityCost) {
		costs.setOscpCapacityCost(oscpCapacityCost);
	}

	/**
	 * Get the OSCP capacity tier cost breakdown.
	 *
	 * @return the costs, never {@literal null}
	 * @since 2.5
	 */
	@JsonIgnore
	public List<NamedCost> getOscpCapacityTiersCostBreakdown() {
		return tiersCostBreakdown(oscpCapacityTiers, costsTiers, NodeUsageCost::getOscpCapacityCost);
	}

	/**
	 * Get the OSCP capacity, per tier.
	 *
	 * @return the counts
	 * @since 2.5
	 */
	public BigInteger[] getOscpCapacityTiers() {
		return oscpCapacityTiers;
	}

	/**
	 * Set the OSCP capacity, per tier.
	 *
	 * @param oscpCapacityTiers
	 *        the counts to set
	 * @since 2.5
	 */
	public void setOscpCapacityTiers(BigInteger[] oscpCapacityTiers) {
		this.oscpCapacityTiers = oscpCapacityTiers;
	}

	/**
	 * Set the OSCP capacity, per tier, as decimals.
	 *
	 * @param oscpCapacityTiers
	 *        the counts to set
	 * @since 2.5
	 */
	public void setOscpCapacityTiersNumeric(BigDecimal[] oscpCapacityTiers) {
		this.oscpCapacityTiers = decimalsToIntegers(oscpCapacityTiers);
	}

	/**
	 * Get the cost of OSCP capacity, per tier.
	 *
	 * @return the cost
	 * @since 2.5
	 */
	public BigDecimal[] getOscpCapacityCostTiers() {
		return getTierCostValues(costsTiers, NodeUsageCost::getOscpCapacityCost);
	}

	/**
	 * Set the cost of OSCP capacity, per tier.
	 *
	 * @param oscpCapacityCostTiers
	 *        the costs to set
	 * @since 2.5
	 */
	public void setOscpCapacityCostTiers(BigDecimal[] oscpCapacityCostTiers) {
		prepCostsTiers(oscpCapacityCostTiers);
		for ( int i = 0; i < costsTiers.length; i++ ) {
			BigDecimal val = (oscpCapacityCostTiers != null && i < oscpCapacityCostTiers.length
					? oscpCapacityCostTiers[i]
					: null);
			costsTiers[i].setOscpCapacityCost(val);
		}
	}

	/**
	 * Get the SolarFlux data in.
	 *
	 * @return the count
	 * @since 2.6
	 */
	public BigInteger getFluxDataIn() {
		return fluxDataIn;
	}

	/**
	 * Set the count of SolarFlux data in.
	 *
	 * @param fluxDataIn
	 *        the count to set; if {@literal null} then {@literal 0} will be
	 *        stored
	 * @since 2.6
	 */
	public void setFluxDataIn(BigInteger fluxDataIn) {
		if ( fluxDataIn == null ) {
			fluxDataIn = BigInteger.ZERO;
		}
		this.fluxDataIn = fluxDataIn;
	}

	/**
	 * Get the cost of SolarFlux data in.
	 *
	 * @return the cost
	 * @since 2.6
	 */
	public BigDecimal getFluxDataInCost() {
		return costs.getFluxDataInCost();
	}

	/**
	 * Set the cost of SolarFlux data in.
	 *
	 * @param fluxDataInCost
	 *        the cost to set
	 * @since 2.6
	 */
	public void setFluxDataInCost(BigDecimal fluxDataInCost) {
		costs.setFluxDataInCost(fluxDataInCost);
	}

	/**
	 * Get the SolarFlux data in tier cost breakdown.
	 *
	 * @return the costs, never {@literal null}
	 * @since 2.6
	 */
	@JsonIgnore
	public List<NamedCost> getFluxDataInTiersCostBreakdown() {
		return tiersCostBreakdown(fluxDataInTiers, costsTiers, NodeUsageCost::getFluxDataInCost);
	}

	/**
	 * Get the SolarFlux data in, per tier.
	 *
	 * @return the counts
	 * @since 2.6
	 */
	public BigInteger[] getFluxDataInTiers() {
		return fluxDataInTiers;
	}

	/**
	 * Set the SolarFlux data in, per tier.
	 *
	 * @param fluxDataInTiers
	 *        the counts to set
	 * @since 2.6
	 */
	public void setFluxDataInTiers(BigInteger[] fluxDataInTiers) {
		this.fluxDataInTiers = fluxDataInTiers;
	}

	/**
	 * Set the SolarFlux data in, per tier, as decimals.
	 *
	 * @param fluxDataInTiers
	 *        the counts to set
	 * @since 2.6
	 */
	public void setFluxDataInTiersNumeric(BigDecimal[] fluxDataInTiers) {
		this.fluxDataInTiers = decimalsToIntegers(fluxDataInTiers);
	}

	/**
	 * Get the cost of SolarFlux data in, per tier.
	 *
	 * @return the cost
	 * @since 2.6
	 */
	public BigDecimal[] getFluxDataInCostTiers() {
		return getTierCostValues(costsTiers, NodeUsageCost::getFluxDataInCost);
	}

	/**
	 * Set the cost of SolarFlux data in, per tier.
	 *
	 * @param fluxDataInCostTiers
	 *        the costs to set
	 * @since 2.6
	 */
	public void setFluxDataInCostTiers(BigDecimal[] fluxDataInCostTiers) {
		prepCostsTiers(fluxDataInCostTiers);
		for ( int i = 0; i < costsTiers.length; i++ ) {
			BigDecimal val = (fluxDataInCostTiers != null && i < fluxDataInCostTiers.length
					? fluxDataInCostTiers[i]
					: null);
			costsTiers[i].setFluxDataInCost(val);
		}
	}

	/**
	 * Get the SolarFlux data out.
	 *
	 * @return the count
	 * @since 2.6
	 */
	public BigInteger getFluxDataOut() {
		return fluxDataOut;
	}

	/**
	 * Set the count of SolarFlux data out.
	 *
	 * @param fluxDataOut
	 *        the count to set; if {@literal null} then {@literal 0} will be
	 *        stored
	 * @since 2.6
	 */
	public void setFluxDataOut(BigInteger fluxDataOut) {
		if ( fluxDataOut == null ) {
			fluxDataOut = BigInteger.ZERO;
		}
		this.fluxDataOut = fluxDataOut;
	}

	/**
	 * Get the cost of SolarFlux data out.
	 *
	 * @return the cost
	 * @since 2.6
	 */
	public BigDecimal getFluxDataOutCost() {
		return costs.getFluxDataOutCost();
	}

	/**
	 * Set the cost of SolarFlux data out.
	 *
	 * @param fluxDataOutCost
	 *        the cost to set
	 * @since 2.6
	 */
	public void setFluxDataOutCost(BigDecimal fluxDataOutCost) {
		costs.setFluxDataOutCost(fluxDataOutCost);
	}

	/**
	 * Get the SolarFlux data out tier cost breakdown.
	 *
	 * @return the costs, never {@literal null}
	 * @since 2.6
	 */
	@JsonIgnore
	public List<NamedCost> getFluxDataOutTiersCostBreakdown() {
		return tiersCostBreakdown(fluxDataOutTiers, costsTiers, NodeUsageCost::getFluxDataOutCost);
	}

	/**
	 * Get the SolarFlux data out, per tier.
	 *
	 * @return the counts
	 * @since 2.6
	 */
	public BigInteger[] getFluxDataOutTiers() {
		return fluxDataOutTiers;
	}

	/**
	 * Set the SolarFlux data out, per tier.
	 *
	 * @param fluxDataOutTiers
	 *        the counts to set
	 * @since 2.6
	 */
	public void setFluxDataOutTiers(BigInteger[] fluxDataOutTiers) {
		this.fluxDataOutTiers = fluxDataOutTiers;
	}

	/**
	 * Set the SolarFlux data out, per tier, as decimals.
	 *
	 * @param fluxDataOutTiers
	 *        the counts to set
	 * @since 2.6
	 */
	public void setFluxDataOutTiersNumeric(BigDecimal[] fluxDataOutTiers) {
		this.fluxDataOutTiers = decimalsToIntegers(fluxDataOutTiers);
	}

	/**
	 * Get the cost of SolarFlux data out, per tier.
	 *
	 * @return the cost
	 * @since 2.6
	 */
	public BigDecimal[] getFluxDataOutCostTiers() {
		return getTierCostValues(costsTiers, NodeUsageCost::getFluxDataOutCost);
	}

	/**
	 * Set the cost of SolarFlux data out, per tier.
	 *
	 * @param fluxDataOutCostTiers
	 *        the costs to set
	 * @since 2.6
	 */
	public void setFluxDataOutCostTiers(BigDecimal[] fluxDataOutCostTiers) {
		prepCostsTiers(fluxDataOutCostTiers);
		for ( int i = 0; i < costsTiers.length; i++ ) {
			BigDecimal val = (fluxDataOutCostTiers != null && i < fluxDataOutCostTiers.length
					? fluxDataOutCostTiers[i]
					: null);
			costsTiers[i].setFluxDataOutCost(val);
		}
	}

	/**
	 * Get the OAuth client credentials.
	 *
	 * @return the count
	 * @since 2.6
	 */
	public BigInteger getOauthClientCredentials() {
		return oauthClientCredentials;
	}

	/**
	 * Set the count of OAuth client credentials.
	 *
	 * @param oauthClientCredentials
	 *        the count to set; if {@literal null} then {@literal 0} will be
	 *        stored
	 * @since 2.6
	 */
	public void setOauthClientCredentials(BigInteger oauthClientCredentials) {
		if ( oauthClientCredentials == null ) {
			oauthClientCredentials = BigInteger.ZERO;
		}
		this.oauthClientCredentials = oauthClientCredentials;
	}

	/**
	 * Get the cost of OAuth client credentials.
	 *
	 * @return the cost
	 * @since 2.6
	 */
	public BigDecimal getOauthClientCredentialsCost() {
		return costs.getOauthClientCredentialsCost();
	}

	/**
	 * Set the cost of OAuth client credentials.
	 *
	 * @param oauthClientCredentialsCost
	 *        the cost to set
	 * @since 2.6
	 */
	public void setOauthClientCredentialsCost(BigDecimal oauthClientCredentialsCost) {
		costs.setOauthClientCredentialsCost(oauthClientCredentialsCost);
	}

	/**
	 * Get the OAuth client credentials tier cost breakdown.
	 *
	 * @return the costs, never {@literal null}
	 * @since 2.6
	 */
	@JsonIgnore
	public List<NamedCost> getOauthClientCredentialsTiersCostBreakdown() {
		return tiersCostBreakdown(oauthClientCredentialsTiers, costsTiers,
				NodeUsageCost::getOauthClientCredentialsCost);
	}

	/**
	 * Get the OAuth client credentials, per tier.
	 *
	 * @return the counts
	 * @since 2.6
	 */
	public BigInteger[] getOauthClientCredentialsTiers() {
		return oauthClientCredentialsTiers;
	}

	/**
	 * Set the OAuth client credentials, per tier.
	 *
	 * @param oauthClientCredentialsTiers
	 *        the counts to set
	 * @since 2.6
	 */
	public void setOauthClientCredentialsTiers(BigInteger[] oauthClientCredentialsTiers) {
		this.oauthClientCredentialsTiers = oauthClientCredentialsTiers;
	}

	/**
	 * Set the OAuth client credentials, per tier, as decimals.
	 *
	 * @param oauthClientCredentialsTiers
	 *        the counts to set
	 * @since 2.6
	 */
	public void setOauthClientCredentialsTiersNumeric(BigDecimal[] oauthClientCredentialsTiers) {
		this.oauthClientCredentialsTiers = decimalsToIntegers(oauthClientCredentialsTiers);
	}

	/**
	 * Get the cost of OAuth client credentials, per tier.
	 *
	 * @return the cost
	 * @since 2.6
	 */
	public BigDecimal[] getOauthClientCredentialsCostTiers() {
		return getTierCostValues(costsTiers, NodeUsageCost::getOauthClientCredentialsCost);
	}

	/**
	 * Set the cost of OAuth client credentials, per tier.
	 *
	 * @param oauthClientCredentialsCostTiers
	 *        the costs to set
	 * @since 2.6
	 */
	public void setOauthClientCredentialsCostTiers(BigDecimal[] oauthClientCredentialsCostTiers) {
		prepCostsTiers(oauthClientCredentialsCostTiers);
		for ( int i = 0; i < costsTiers.length; i++ ) {
			BigDecimal val = (oauthClientCredentialsCostTiers != null
					&& i < oauthClientCredentialsCostTiers.length ? oauthClientCredentialsCostTiers[i]
							: null);
			costsTiers[i].setOauthClientCredentialsCost(val);
		}
	}

	/**
	 * Get the Cloud Integrations data.
	 *
	 * @return the count
	 * @since 2.7
	 */
	public BigInteger getCloudIntegrationsData() {
		return cloudIntegrationsData;
	}

	/**
	 * Set the count of Cloud Integrations data.
	 *
	 * @param cloudIntegrationsData
	 *        the count to set; if {@literal null} then {@literal 0} will be
	 *        stored
	 * @since 2.7
	 */
	public void setCloudIntegrationsData(BigInteger cloudIntegrationsData) {
		if ( cloudIntegrationsData == null ) {
			cloudIntegrationsData = BigInteger.ZERO;
		}
		this.cloudIntegrationsData = cloudIntegrationsData;
	}

	/**
	 * Get the cost of Cloud Integrations data.
	 *
	 * @return the cost
	 * @since 2.7
	 */
	public BigDecimal getCloudIntegrationsDataCost() {
		return costs.getCloudIntegrationsDataCost();
	}

	/**
	 * Set the cost of Cloud Integrations data.
	 *
	 * @param cloudIntegrationsDataCost
	 *        the cost to set
	 * @since 2.7
	 */
	public void setCloudIntegrationsDataCost(BigDecimal cloudIntegrationsDataCost) {
		costs.setCloudIntegrationsDataCost(cloudIntegrationsDataCost);
	}

	/**
	 * Get the Cloud Integrations data tier cost breakdown.
	 *
	 * @return the costs, never {@literal null}
	 * @since 2.7
	 */
	@JsonIgnore
	public List<NamedCost> getCloudIntegrationsDataTiersCostBreakdown() {
		return tiersCostBreakdown(cloudIntegrationsDataTiers, costsTiers,
				NodeUsageCost::getCloudIntegrationsDataCost);
	}

	/**
	 * Get the Cloud Integrations data, per tier.
	 *
	 * @return the counts
	 * @since 2.7
	 */
	public BigInteger[] getCloudIntegrationsDataTiers() {
		return cloudIntegrationsDataTiers;
	}

	/**
	 * Set the Cloud Integrations data, per tier.
	 *
	 * @param cloudIntegrationsDataTiers
	 *        the counts to set
	 * @since 2.7
	 */
	public void setCloudIntegrationsDataTiers(BigInteger[] cloudIntegrationsDataTiers) {
		this.cloudIntegrationsDataTiers = cloudIntegrationsDataTiers;
	}

	/**
	 * Set the Cloud Integrations data, per tier, as decimals.
	 *
	 * @param cloudIntegrationsDataTiers
	 *        the counts to set
	 * @since 2.7
	 */
	public void setCloudIntegrationsDataTiersNumeric(BigDecimal[] cloudIntegrationsDataTiers) {
		this.cloudIntegrationsDataTiers = decimalsToIntegers(cloudIntegrationsDataTiers);
	}

	/**
	 * Get the cost of Cloud Integrations data, per tier.
	 *
	 * @return the cost
	 * @since 2.7
	 */
	public BigDecimal[] getCloudIntegrationsDataCostTiers() {
		return getTierCostValues(costsTiers, NodeUsageCost::getCloudIntegrationsDataCost);
	}

	/**
	 * Set the cost of Cloud Integrations data, per tier.
	 *
	 * @param cloudIntegrationsDataCostTiers
	 *        the costs to set
	 * @since 2.7
	 */
	public void setCloudIntegrationsDataCostTiers(BigDecimal[] cloudIntegrationsDataCostTiers) {
		prepCostsTiers(cloudIntegrationsDataCostTiers);
		for ( int i = 0; i < costsTiers.length; i++ ) {
			BigDecimal val = (cloudIntegrationsDataCostTiers != null
					&& i < cloudIntegrationsDataCostTiers.length ? cloudIntegrationsDataCostTiers[i]
							: null);
			costsTiers[i].setCloudIntegrationsDataCost(val);
		}
	}

}
