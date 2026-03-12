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

import static java.math.BigInteger.ZERO;
import static java.util.Arrays.stream;
import java.io.Serial;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import org.jspecify.annotations.Nullable;
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
 * @version 3.0
 */
public class NodeUsage extends BasicLongEntity
		implements InvoiceUsageRecord<Long>, Differentiable<NodeUsage>, NodeUsages {

	@Serial
	private static final long serialVersionUID = 6848689122993706234L;

	private @Nullable String description;
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

	private BigInteger @Nullable [] datumPropertiesInTiers;
	private BigInteger @Nullable [] datumOutTiers;
	private BigInteger @Nullable [] datumDaysStoredTiers;
	private BigInteger @Nullable [] instructionsIssuedTiers;
	private BigInteger @Nullable [] fluxDataInTiers;
	private BigInteger @Nullable [] fluxDataOutTiers;
	private BigInteger @Nullable [] ocppChargersTiers;
	private BigInteger @Nullable [] oscpCapacityGroupsTiers;
	private BigInteger @Nullable [] oscpCapacityTiers;
	private BigInteger @Nullable [] dnp3DataPointsTiers;
	private BigInteger @Nullable [] oauthClientCredentialsTiers;
	private BigInteger @Nullable [] cloudIntegrationsDataTiers;
	private NodeUsageCost @Nullable [] costsTiers;

	/**
	 * Constructor.
	 *
	 * <p>
	 * This creates a {@code null} node ID, for usage not associated with a
	 * specific node.
	 * </p>
	 */
	public NodeUsage() {
		this(null, null);
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
	public NodeUsage(@Nullable Long nodeId, @Nullable Instant created) {
		super(nodeId, created);
		this.datumPropertiesIn = ZERO;
		this.datumOut = ZERO;
		this.datumDaysStored = ZERO;
		this.instructionsIssued = ZERO;
		this.fluxDataIn = ZERO;
		this.fluxDataOut = ZERO;
		this.ocppChargers = ZERO;
		this.oscpCapacityGroups = ZERO;
		this.oscpCapacity = ZERO;
		this.dnp3DataPoints = ZERO;
		this.oauthClientCredentials = ZERO;
		this.cloudIntegrationsData = ZERO;
		this.totalCost = BigDecimal.ZERO;
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
	public boolean isSameAs(@Nullable NodeUsage other) {
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
	public boolean differsFrom(@Nullable NodeUsage other) {
		return !isSameAs(other);
	}

	@Override
	public @Nullable Long getUsageKey() {
		return getNodeId();
	}

	@Override
	public final @Nullable String getDescription() {
		return description;
	}

	/**
	 * Set an optional description, such as a node name.
	 *
	 * @param description
	 *        the description to set
	 * @since 2.1
	 */
	public final void setDescription(@Nullable String description) {
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
	public final @Nullable Long getNodeId() {
		return getId();
	}

	/**
	 * Get the node usage costs.
	 *
	 * @return the costs, never {@code null}
	 */
	@JsonIgnore
	public final NodeUsageCost getCosts() {
		return costs;
	}

	/**
	 * Get the count of datum properties added.
	 *
	 * @return the count, never {@code null}
	 */
	public final BigInteger getDatumPropertiesIn() {
		return datumPropertiesIn;
	}

	/**
	 * Set the count of datum properties added.
	 *
	 * @param datumPropertiesIn
	 *        the count to set; if {@code null} then {@literal 0} will be stored
	 */
	public final void setDatumPropertiesIn(@Nullable BigInteger datumPropertiesIn) {
		if ( datumPropertiesIn == null ) {
			datumPropertiesIn = ZERO;
		}
		this.datumPropertiesIn = datumPropertiesIn;
	}

	/**
	 * Get the cost of datum properties added.
	 *
	 * @return the cost
	 */
	public final BigDecimal getDatumPropertiesInCost() {
		return costs.getDatumPropertiesInCost();
	}

	/**
	 * Set the cost of datum properties added.
	 *
	 * @param datumPropertiesInCost
	 *        the cost to set
	 */
	public final void setDatumPropertiesInCost(BigDecimal datumPropertiesInCost) {
		costs.setDatumPropertiesInCost(datumPropertiesInCost);
	}

	/**
	 * Get the count of datum stored per day (accumulating).
	 *
	 * @return the count
	 */
	public final BigInteger getDatumDaysStored() {
		return datumDaysStored;
	}

	/**
	 * Set the count of datum stored per day (accumulating).
	 *
	 * @param datumDaysStored
	 *        the count to set; if {@code null} then {@literal 0} will be stored
	 */
	public final void setDatumDaysStored(BigInteger datumDaysStored) {
		if ( datumDaysStored == null ) {
			datumDaysStored = ZERO;
		}
		this.datumDaysStored = datumDaysStored;
	}

	/**
	 * Get the cost of datum stored per day (accumulating).
	 *
	 * @return the cost
	 */
	public final BigDecimal getDatumDaysStoredCost() {
		return costs.getDatumDaysStoredCost();
	}

	/**
	 * Set the cost of datum stored per day (accumulating).
	 *
	 * @param datumDaysStoredCost
	 *        the cost to set
	 */
	public final void setDatumDaysStoredCost(BigDecimal datumDaysStoredCost) {
		costs.setDatumDaysStoredCost(datumDaysStoredCost);
	}

	/**
	 * Get the count of datum queried.
	 *
	 * @return the count
	 */
	public final BigInteger getDatumOut() {
		return datumOut;
	}

	/**
	 * Set the count of datum queried.
	 *
	 * @param datumOut
	 *        the count to set; if {@code null} then {@literal 0} will be stored
	 */
	public final void setDatumOut(BigInteger datumOut) {
		if ( datumOut == null ) {
			datumOut = ZERO;
		}
		this.datumOut = datumOut;
	}

	/**
	 * Get the cost of datum queried.
	 *
	 * @return the cost
	 */
	public final BigDecimal getDatumOutCost() {
		return costs.getDatumOutCost();
	}

	/**
	 * Set the cost of datum queried.
	 *
	 * @param datumOutCost
	 *        the cost to set
	 */
	public final void setDatumOutCost(BigDecimal datumOutCost) {
		costs.setDatumOutCost(datumOutCost);
	}

	/**
	 * Get the overall cost.
	 *
	 * @return the cost, never {@code null}
	 */
	public final BigDecimal getTotalCost() {
		return totalCost;
	}

	/**
	 * Set the overall cost.
	 *
	 * @param totalCost
	 *        the cost to set; if {@code null} then {@literal 0} will be stored
	 */
	public final void setTotalCost(BigDecimal totalCost) {
		this.totalCost = totalCost != null ? totalCost : BigDecimal.ZERO;
	}

	/**
	 * Get the count of OCPP Chargers.
	 *
	 * @return the count
	 */
	public final BigInteger getOcppChargers() {
		return ocppChargers;
	}

	/**
	 * Set the count of OCPP Chargers.
	 *
	 * @param ocppChargers
	 *        the count to set; if {@code null} then {@literal 0} will be stored
	 */
	public final void setOcppChargers(BigInteger ocppChargers) {
		if ( ocppChargers == null ) {
			ocppChargers = ZERO;
		}
		this.ocppChargers = ocppChargers;
	}

	/**
	 * Get the cost of OCPP Chargers.
	 *
	 * @return the cost
	 */
	public final BigDecimal getOcppChargersCost() {
		return costs.getOcppChargersCost();
	}

	/**
	 * Set the cost of OCPP Chargers.
	 *
	 * @param ocppChargersCost
	 *        the cost to set
	 */
	public final void setOcppChargersCost(BigDecimal ocppChargersCost) {
		costs.setOcppChargersCost(ocppChargersCost);
	}

	/**
	 * Get the count of OSCP Capacity Groups.
	 *
	 * @return the count
	 */
	public final BigInteger getOscpCapacityGroups() {
		return oscpCapacityGroups;
	}

	/**
	 * Set the count of OSCP Capacity Groups.
	 *
	 * @param oscpCapacityGroups
	 *        the count to set; if {@code null} then {@literal 0} will be stored
	 */
	public final void setOscpCapacityGroups(BigInteger oscpCapacityGroups) {
		if ( oscpCapacityGroups == null ) {
			oscpCapacityGroups = ZERO;
		}
		this.oscpCapacityGroups = oscpCapacityGroups;
	}

	/**
	 * Get the cost of OSCP Capacity Groups.
	 *
	 * @return the cost
	 */
	public final BigDecimal getOscpCapacityGroupsCost() {
		return costs.getOscpCapacityGroupsCost();
	}

	/**
	 * Set the cost of OSCP Capacity Groups.
	 *
	 * @param oscpCapacityGroupsCost
	 *        the cost to set
	 */
	public final void setOscpCapacityGroupsCost(BigDecimal oscpCapacityGroupsCost) {
		costs.setOscpCapacityGroupsCost(oscpCapacityGroupsCost);
	}

	private void prepCostsTiers(Object @Nullable [] array) {
		final int len = Math.max(costsTiers != null ? costsTiers.length : 0,
				array != null ? array.length : 0);
		costsTiers = ArrayUtils.arrayWithLength(costsTiers, len, NodeUsageCost.class, null);
	}

	private static BigDecimal @Nullable [] getTierCostValues(NodeUsageCost @Nullable [] costsTiers,
			Function<NodeUsageCost, BigDecimal> f) {
		BigDecimal[] result = null;
		if ( costsTiers != null ) {
			result = stream(costsTiers).map(f).toArray(BigDecimal[]::new);
		}
		return result;
	}

	private static BigInteger @Nullable [] decimalsToIntegers(BigDecimal @Nullable [] decimals) {
		BigInteger[] ints = null;
		if ( decimals != null ) {
			ints = new BigInteger[decimals.length];
			for ( int i = 0, len = decimals.length; i < len; i++ ) {
				ints[i] = decimals[i].toBigInteger();
			}
		}
		return ints;
	}

	@SuppressWarnings("MixedMutabilityReturnType")
	private static List<NamedCost> tiersCostBreakdown(BigInteger @Nullable [] counts,
			NodeUsageCost @Nullable [] costsTiers, Function<NodeUsageCost, BigDecimal> f) {
		if ( counts == null || counts.length < 1 ) {
			return List.of();
		}
		List<NamedCost> result = new ArrayList<>(counts.length);
		for ( int i = 0; i < counts.length; i++ ) {
			BigInteger q = counts[i];
			if ( ZERO.compareTo(q) == 0 ) {
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
	 * @return the map, never {@code null}
	 */
	public final Map<String, List<NamedCost>> getTiersCostBreakdown() {
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
	 * @return the map, never {@code null}
	 */
	public final Map<String, UsageInfo> getUsageInfo() {
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
	public final NodeUsageCost @Nullable [] getCostsTiers() {
		return costsTiers;
	}

	/**
	 * Set the node usage costs.
	 *
	 * @param costTiers
	 *        the costs to set
	 */
	public final void setCostsTiers(NodeUsageCost @Nullable [] costTiers) {
		this.costsTiers = costTiers;
	}

	/**
	 * Get the node datum properties tier cost breakdown.
	 *
	 * @return the costs, never {@code null}
	 */
	@JsonIgnore
	public final List<NamedCost> getDatumPropertiesInTiersCostBreakdown() {
		return tiersCostBreakdown(datumPropertiesInTiers, costsTiers,
				NodeUsageCost::getDatumPropertiesInCost);
	}

	/**
	 * Get the count of datum properties added, per tier.
	 *
	 * @return the counts
	 */
	public final BigInteger @Nullable [] getDatumPropertiesInTiers() {
		return datumPropertiesInTiers;
	}

	/**
	 * Set the count of datum properties added, per tier.
	 *
	 * @param datumPropertiesInTiers
	 *        the counts to set
	 */
	public final void setDatumPropertiesInTiers(BigInteger @Nullable [] datumPropertiesInTiers) {
		this.datumPropertiesInTiers = datumPropertiesInTiers;
	}

	/**
	 * Set the count of datum properties added, per tier, as decimal values.
	 *
	 * @param datumPropertiesInTiers
	 *        the counts to set
	 */
	public final void setDatumPropertiesInTiersNumeric(BigDecimal @Nullable [] datumPropertiesInTiers) {
		this.datumPropertiesInTiers = decimalsToIntegers(datumPropertiesInTiers);
	}

	/**
	 * Get the cost of datum properties added, per tier.
	 *
	 * @return the costs
	 */
	public final BigDecimal @Nullable [] getDatumPropertiesInCostTiers() {
		return getTierCostValues(costsTiers, NodeUsageCost::getDatumPropertiesInCost);
	}

	/**
	 * Set the cost of datum properties added.
	 *
	 * @param datumPropertiesInCost
	 *        the cost to set
	 */
	public final void setDatumPropertiesInCostTiers(BigDecimal @Nullable [] datumPropertiesInCost) {
		prepCostsTiers(datumPropertiesInCost);
		if ( costsTiers == null ) {
			return;
		}
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
	 * @return the costs, never {@code null}
	 */
	@JsonIgnore
	public final List<NamedCost> getDatumDaysStoredTiersCostBreakdown() {
		return tiersCostBreakdown(datumDaysStoredTiers, costsTiers,
				NodeUsageCost::getDatumDaysStoredCost);
	}

	/**
	 * Get the count of datum stored per day (accumulating), per tier.
	 *
	 * @return the counts
	 */
	public final BigInteger @Nullable [] getDatumDaysStoredTiers() {
		return datumDaysStoredTiers;
	}

	/**
	 * Set the count of datum stored per day (accumulating), per tier.
	 *
	 * @param datumDaysStoredTiers
	 *        the counts to set
	 */
	public final void setDatumDaysStoredTiers(BigInteger @Nullable [] datumDaysStoredTiers) {
		this.datumDaysStoredTiers = datumDaysStoredTiers;
	}

	/**
	 * Set the count of datum stored per day (accumulating), per tier, as
	 * decimals.
	 *
	 * @param datumDaysStoredTiers
	 *        the counts to set
	 */
	public final void setDatumDaysStoredTiersNumeric(BigDecimal @Nullable [] datumDaysStoredTiers) {
		this.datumDaysStoredTiers = decimalsToIntegers(datumDaysStoredTiers);
	}

	/**
	 * Get the cost of datum stored per day (accumulating).
	 *
	 * @return the cost
	 */
	public final BigDecimal @Nullable [] getDatumDaysStoredCostTiers() {
		return getTierCostValues(costsTiers, NodeUsageCost::getDatumDaysStoredCost);
	}

	/**
	 * Set the cost of datum stored per day (accumulating).
	 *
	 * @param datumDaysStoredCostTiers
	 *        the costs to set
	 */
	public final void setDatumDaysStoredCostTiers(BigDecimal @Nullable [] datumDaysStoredCostTiers) {
		prepCostsTiers(datumDaysStoredCostTiers);
		if ( costsTiers == null ) {
			return;
		}
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
	 * @return the costs, never {@code null}
	 */
	@JsonIgnore
	public final List<NamedCost> getDatumOutTiersCostBreakdown() {
		return tiersCostBreakdown(datumOutTiers, costsTiers, NodeUsageCost::getDatumOutCost);
	}

	/**
	 * Get the count of datum queried.
	 *
	 * @return the count
	 */
	public final BigInteger @Nullable [] getDatumOutTiers() {
		return datumOutTiers;
	}

	/**
	 * Set the count of datum queried, per tier.
	 *
	 * @param datumOutTiers
	 *        the counts to set
	 */
	public final void setDatumOutTiers(BigInteger @Nullable [] datumOutTiers) {
		this.datumOutTiers = datumOutTiers;
	}

	/**
	 * Set the count of datum queried, per tier, as decimals.
	 *
	 * @param datumOutTiers
	 *        the counts to set
	 */
	public final void setDatumOutTiersNumeric(BigDecimal @Nullable [] datumOutTiers) {
		this.datumOutTiers = decimalsToIntegers(datumOutTiers);
	}

	/**
	 * Get the cost of datum queried, per tier.
	 *
	 * @return the costs
	 */
	public final BigDecimal @Nullable [] getDatumOutCostTiers() {
		return getTierCostValues(costsTiers, NodeUsageCost::getDatumOutCost);
	}

	/**
	 * Set the cost of datum queried, per tier.
	 *
	 * @param datumOutCostTiers
	 *        the costs to set
	 */
	public final void setDatumOutCostTiers(BigDecimal @Nullable [] datumOutCostTiers) {
		prepCostsTiers(datumOutCostTiers);
		if ( costsTiers == null ) {
			return;
		}
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
	 * @return the costs, never {@code null}
	 */
	@JsonIgnore
	public final List<NamedCost> getOcppChargersTiersCostBreakdown() {
		return tiersCostBreakdown(ocppChargersTiers, costsTiers, NodeUsageCost::getOcppChargersCost);
	}

	/**
	 * Get the count of OCPP Chargers, per tier.
	 *
	 * @return the counts
	 */
	public final BigInteger @Nullable [] getOcppChargersTiers() {
		return ocppChargersTiers;
	}

	/**
	 * Set the count of OCPP Chargers, per tier.
	 *
	 * @param ocppChargersTiers
	 *        the counts to set
	 */
	public final void setOcppChargersTiers(BigInteger @Nullable [] ocppChargersTiers) {
		this.ocppChargersTiers = ocppChargersTiers;
	}

	/**
	 * Set the count of OCPP Chargers, per tier, as decimals.
	 *
	 * @param ocppChargersTiers
	 *        the counts to set
	 */
	public final void setOcppChargersTiersNumeric(BigDecimal @Nullable [] ocppChargersTiers) {
		this.ocppChargersTiers = decimalsToIntegers(ocppChargersTiers);
	}

	/**
	 * Get the cost of OCPP Chargers, per tier.
	 *
	 * @return the cost
	 */
	public final BigDecimal @Nullable [] getOcppChargersCostTiers() {
		return getTierCostValues(costsTiers, NodeUsageCost::getOcppChargersCost);
	}

	/**
	 * Set the cost of OCPP Chargers, per tier.
	 *
	 * @param ocppChargersCostTiers
	 *        the costs to set
	 */
	public final void setOcppChargersCostTiers(BigDecimal @Nullable [] ocppChargersCostTiers) {
		prepCostsTiers(ocppChargersCostTiers);
		if ( costsTiers == null ) {
			return;
		}
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
	 * @return the costs, never {@code null}
	 */
	@JsonIgnore
	public final List<NamedCost> getOscpCapacityGroupsTiersCostBreakdown() {
		return tiersCostBreakdown(oscpCapacityGroupsTiers, costsTiers,
				NodeUsageCost::getOscpCapacityGroupsCost);
	}

	/**
	 * Get the count of OSCP Capacity Groups, per tier.
	 *
	 * @return the counts
	 */
	public final BigInteger @Nullable [] getOscpCapacityGroupsTiers() {
		return oscpCapacityGroupsTiers;
	}

	/**
	 * Set the count of OSCP Capacity Groups, per tier.
	 *
	 * @param oscpCapacityGroupsTiers
	 *        the counts to set
	 */
	public final void setOscpCapacityGroupsTiers(BigInteger @Nullable [] oscpCapacityGroupsTiers) {
		this.oscpCapacityGroupsTiers = oscpCapacityGroupsTiers;
	}

	/**
	 * Set the count of OSCP Capacity Groups, per tier, as decimals.
	 *
	 * @param oscpCapacityGroupsTiers
	 *        the counts to set
	 */
	public final void setOscpCapacityGroupsTiersNumeric(
			BigDecimal @Nullable [] oscpCapacityGroupsTiers) {
		this.oscpCapacityGroupsTiers = decimalsToIntegers(oscpCapacityGroupsTiers);
	}

	/**
	 * Get the cost of OSCP Capacity Groups, per tier.
	 *
	 * @return the cost
	 */
	public final BigDecimal @Nullable [] getOscpCapacityGroupsCostTiers() {
		return getTierCostValues(costsTiers, NodeUsageCost::getOscpCapacityGroupsCost);
	}

	/**
	 * Set the cost of OSCP Capacity Groups, per tier.
	 *
	 * @param oscpCapacityGroupsCostTiers
	 *        the costs to set
	 */
	public final void setOscpCapacityGroupsCostTiers(
			BigDecimal @Nullable [] oscpCapacityGroupsCostTiers) {
		prepCostsTiers(oscpCapacityGroupsCostTiers);
		if ( costsTiers == null ) {
			return;
		}
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
	public final BigInteger getDnp3DataPoints() {
		return dnp3DataPoints;
	}

	/**
	 * Set the count of DNP3 Data Points.
	 *
	 * @param dnp3DataPoints
	 *        the count to set; if {@code null} then {@literal 0} will be stored
	 * @since 2.3
	 */
	public final void setDnp3DataPoints(BigInteger dnp3DataPoints) {
		if ( dnp3DataPoints == null ) {
			dnp3DataPoints = ZERO;
		}
		this.dnp3DataPoints = dnp3DataPoints;
	}

	/**
	 * Get the cost of DNP3 Data Points.
	 *
	 * @return the cost
	 * @since 2.3
	 */
	public final BigDecimal getDnp3DataPointsCost() {
		return costs.getDnp3DataPointsCost();
	}

	/**
	 * Set the cost of DNP3 Data Points.
	 *
	 * @param dnp3DataPointsCost
	 *        the cost to set
	 * @since 2.3
	 */
	public final void setDnp3DataPointsCost(BigDecimal dnp3DataPointsCost) {
		costs.setDnp3DataPointsCost(dnp3DataPointsCost);
	}

	/**
	 * Get the DNP3 Data Points tier cost breakdown.
	 *
	 * @return the costs, never {@code null}
	 * @since 2.3
	 */
	@JsonIgnore
	public final List<NamedCost> getDnp3DataPointsTiersCostBreakdown() {
		return tiersCostBreakdown(dnp3DataPointsTiers, costsTiers, NodeUsageCost::getDnp3DataPointsCost);
	}

	/**
	 * Get the count of DNP3 Data Points, per tier.
	 *
	 * @return the counts
	 * @since 2.3
	 */
	public final BigInteger @Nullable [] getDnp3DataPointsTiers() {
		return dnp3DataPointsTiers;
	}

	/**
	 * Set the count of DNP3 Data Points, per tier.
	 *
	 * @param dnp3DataPointsTiers
	 *        the counts to set
	 * @since 2.3
	 */
	public final void setDnp3DataPointsTiers(BigInteger @Nullable [] dnp3DataPointsTiers) {
		this.dnp3DataPointsTiers = dnp3DataPointsTiers;
	}

	/**
	 * Set the count of DNP3 Data Points, per tier, as decimals.
	 *
	 * @param dnp3DataPointsTiers
	 *        the counts to set
	 * @since 2.3
	 */
	public final void setDnp3DataPointsTiersNumeric(BigDecimal @Nullable [] dnp3DataPointsTiers) {
		this.dnp3DataPointsTiers = decimalsToIntegers(dnp3DataPointsTiers);
	}

	/**
	 * Get the cost of DNP3 Data Points, per tier.
	 *
	 * @return the cost
	 * @since 2.3
	 */
	public final BigDecimal @Nullable [] getDnp3DataPointsCostTiers() {
		return getTierCostValues(costsTiers, NodeUsageCost::getDnp3DataPointsCost);
	}

	/**
	 * Set the cost of DNP3 Data Points, per tier.
	 *
	 * @param dnp3DataPointsCostTiers
	 *        the costs to set
	 * @since 2.3
	 */
	public final void setDnp3DataPointsCostTiers(BigDecimal @Nullable [] dnp3DataPointsCostTiers) {
		prepCostsTiers(dnp3DataPointsCostTiers);
		if ( costsTiers == null ) {
			return;
		}
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
	public final BigInteger getInstructionsIssued() {
		return instructionsIssued;
	}

	/**
	 * Set the count of Instructions issued.
	 *
	 * @param instructionsIssued
	 *        the count to set; if {@code null} then {@literal 0} will be stored
	 * @since 2.4
	 */
	public final void setInstructionsIssued(BigInteger instructionsIssued) {
		if ( instructionsIssued == null ) {
			instructionsIssued = ZERO;
		}
		this.instructionsIssued = instructionsIssued;
	}

	/**
	 * Get the cost of Instructions issued.
	 *
	 * @return the cost
	 * @since 2.4
	 */
	public final BigDecimal getInstructionsIssuedCost() {
		return costs.getInstructionsIssuedCost();
	}

	/**
	 * Set the cost of Instructions issued.
	 *
	 * @param instructionsIssuedCost
	 *        the cost to set
	 * @since 2.4
	 */
	public final void setInstructionsIssuedCost(BigDecimal instructionsIssuedCost) {
		costs.setInstructionsIssuedCost(instructionsIssuedCost);
	}

	/**
	 * Get the Instructions issued tier cost breakdown.
	 *
	 * @return the costs, never {@code null}
	 * @since 2.4
	 */
	@JsonIgnore
	public final List<NamedCost> getInstructionsIssuedTiersCostBreakdown() {
		return tiersCostBreakdown(instructionsIssuedTiers, costsTiers,
				NodeUsageCost::getInstructionsIssuedCost);
	}

	/**
	 * Get the count of Instructions issued, per tier.
	 *
	 * @return the counts
	 * @since 2.4
	 */
	public final BigInteger @Nullable [] getInstructionsIssuedTiers() {
		return instructionsIssuedTiers;
	}

	/**
	 * Set the count of Instructions issued, per tier.
	 *
	 * @param instructionsIssuedTiers
	 *        the counts to set
	 * @since 2.4
	 */
	public final void setInstructionsIssuedTiers(BigInteger @Nullable [] instructionsIssuedTiers) {
		this.instructionsIssuedTiers = instructionsIssuedTiers;
	}

	/**
	 * Set the count of Instructions issued, per tier, as decimals.
	 *
	 * @param instructionsIssuedTiers
	 *        the counts to set
	 * @since 2.4
	 */
	public final void setInstructionsIssuedTiersNumeric(
			BigDecimal @Nullable [] instructionsIssuedTiers) {
		this.instructionsIssuedTiers = decimalsToIntegers(instructionsIssuedTiers);
	}

	/**
	 * Get the cost of Instructions issued, per tier.
	 *
	 * @return the cost
	 * @since 2.4
	 */
	public final BigDecimal @Nullable [] getInstructionsIssuedCostTiers() {
		return getTierCostValues(costsTiers, NodeUsageCost::getInstructionsIssuedCost);
	}

	/**
	 * Set the cost of Instructions issued, per tier.
	 *
	 * @param instructionsIssuedCostTiers
	 *        the costs to set
	 * @since 2.4
	 */
	public final void setInstructionsIssuedCostTiers(
			BigDecimal @Nullable [] instructionsIssuedCostTiers) {
		prepCostsTiers(instructionsIssuedCostTiers);
		if ( costsTiers == null ) {
			return;
		}
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
	public final BigInteger getOscpCapacity() {
		return oscpCapacity;
	}

	/**
	 * Set the count of OSCP capacity .
	 *
	 * @param oscpCapacity
	 *        the count to set; if {@code null} then {@literal 0} will be stored
	 * @since 2.5
	 */
	public final void setOscpCapacity(BigInteger oscpCapacity) {
		if ( oscpCapacity == null ) {
			oscpCapacity = ZERO;
		}
		this.oscpCapacity = oscpCapacity;
	}

	/**
	 * Get the cost of OSCP capacity.
	 *
	 * @return the cost
	 * @since 2.5
	 */
	public final BigDecimal getOscpCapacityCost() {
		return costs.getOscpCapacityCost();
	}

	/**
	 * Set the cost of OSCP capacity.
	 *
	 * @param oscpCapacityCost
	 *        the cost to set
	 * @since 2.5
	 */
	public final void setOscpCapacityCost(BigDecimal oscpCapacityCost) {
		costs.setOscpCapacityCost(oscpCapacityCost);
	}

	/**
	 * Get the OSCP capacity tier cost breakdown.
	 *
	 * @return the costs, never {@code null}
	 * @since 2.5
	 */
	@JsonIgnore
	public final List<NamedCost> getOscpCapacityTiersCostBreakdown() {
		return tiersCostBreakdown(oscpCapacityTiers, costsTiers, NodeUsageCost::getOscpCapacityCost);
	}

	/**
	 * Get the OSCP capacity, per tier.
	 *
	 * @return the counts
	 * @since 2.5
	 */
	public final BigInteger @Nullable [] getOscpCapacityTiers() {
		return oscpCapacityTiers;
	}

	/**
	 * Set the OSCP capacity, per tier.
	 *
	 * @param oscpCapacityTiers
	 *        the counts to set
	 * @since 2.5
	 */
	public final void setOscpCapacityTiers(BigInteger @Nullable [] oscpCapacityTiers) {
		this.oscpCapacityTiers = oscpCapacityTiers;
	}

	/**
	 * Set the OSCP capacity, per tier, as decimals.
	 *
	 * @param oscpCapacityTiers
	 *        the counts to set
	 * @since 2.5
	 */
	public final void setOscpCapacityTiersNumeric(BigDecimal @Nullable [] oscpCapacityTiers) {
		this.oscpCapacityTiers = decimalsToIntegers(oscpCapacityTiers);
	}

	/**
	 * Get the cost of OSCP capacity, per tier.
	 *
	 * @return the cost
	 * @since 2.5
	 */
	public final BigDecimal @Nullable [] getOscpCapacityCostTiers() {
		return getTierCostValues(costsTiers, NodeUsageCost::getOscpCapacityCost);
	}

	/**
	 * Set the cost of OSCP capacity, per tier.
	 *
	 * @param oscpCapacityCostTiers
	 *        the costs to set
	 * @since 2.5
	 */
	public final void setOscpCapacityCostTiers(BigDecimal @Nullable [] oscpCapacityCostTiers) {
		prepCostsTiers(oscpCapacityCostTiers);
		if ( costsTiers == null ) {
			return;
		}
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
	public final BigInteger getFluxDataIn() {
		return fluxDataIn;
	}

	/**
	 * Set the count of SolarFlux data in.
	 *
	 * @param fluxDataIn
	 *        the count to set; if {@code null} then {@literal 0} will be stored
	 * @since 2.6
	 */
	public final void setFluxDataIn(BigInteger fluxDataIn) {
		if ( fluxDataIn == null ) {
			fluxDataIn = ZERO;
		}
		this.fluxDataIn = fluxDataIn;
	}

	/**
	 * Get the cost of SolarFlux data in.
	 *
	 * @return the cost
	 * @since 2.6
	 */
	public final BigDecimal getFluxDataInCost() {
		return costs.getFluxDataInCost();
	}

	/**
	 * Set the cost of SolarFlux data in.
	 *
	 * @param fluxDataInCost
	 *        the cost to set
	 * @since 2.6
	 */
	public final void setFluxDataInCost(BigDecimal fluxDataInCost) {
		costs.setFluxDataInCost(fluxDataInCost);
	}

	/**
	 * Get the SolarFlux data in tier cost breakdown.
	 *
	 * @return the costs, never {@code null}
	 * @since 2.6
	 */
	@JsonIgnore
	public final List<NamedCost> getFluxDataInTiersCostBreakdown() {
		return tiersCostBreakdown(fluxDataInTiers, costsTiers, NodeUsageCost::getFluxDataInCost);
	}

	/**
	 * Get the SolarFlux data in, per tier.
	 *
	 * @return the counts
	 * @since 2.6
	 */
	public final BigInteger @Nullable [] getFluxDataInTiers() {
		return fluxDataInTiers;
	}

	/**
	 * Set the SolarFlux data in, per tier.
	 *
	 * @param fluxDataInTiers
	 *        the counts to set
	 * @since 2.6
	 */
	public final void setFluxDataInTiers(BigInteger @Nullable [] fluxDataInTiers) {
		this.fluxDataInTiers = fluxDataInTiers;
	}

	/**
	 * Set the SolarFlux data in, per tier, as decimals.
	 *
	 * @param fluxDataInTiers
	 *        the counts to set
	 * @since 2.6
	 */
	public final void setFluxDataInTiersNumeric(BigDecimal @Nullable [] fluxDataInTiers) {
		this.fluxDataInTiers = decimalsToIntegers(fluxDataInTiers);
	}

	/**
	 * Get the cost of SolarFlux data in, per tier.
	 *
	 * @return the cost
	 * @since 2.6
	 */
	public final BigDecimal @Nullable [] getFluxDataInCostTiers() {
		return getTierCostValues(costsTiers, NodeUsageCost::getFluxDataInCost);
	}

	/**
	 * Set the cost of SolarFlux data in, per tier.
	 *
	 * @param fluxDataInCostTiers
	 *        the costs to set
	 * @since 2.6
	 */
	public final void setFluxDataInCostTiers(BigDecimal @Nullable [] fluxDataInCostTiers) {
		prepCostsTiers(fluxDataInCostTiers);
		if ( costsTiers == null ) {
			return;
		}
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
	public final BigInteger getFluxDataOut() {
		return fluxDataOut;
	}

	/**
	 * Set the count of SolarFlux data out.
	 *
	 * @param fluxDataOut
	 *        the count to set; if {@code null} then {@literal 0} will be stored
	 * @since 2.6
	 */
	public final void setFluxDataOut(BigInteger fluxDataOut) {
		if ( fluxDataOut == null ) {
			fluxDataOut = ZERO;
		}
		this.fluxDataOut = fluxDataOut;
	}

	/**
	 * Get the cost of SolarFlux data out.
	 *
	 * @return the cost
	 * @since 2.6
	 */
	public final BigDecimal getFluxDataOutCost() {
		return costs.getFluxDataOutCost();
	}

	/**
	 * Set the cost of SolarFlux data out.
	 *
	 * @param fluxDataOutCost
	 *        the cost to set
	 * @since 2.6
	 */
	public final void setFluxDataOutCost(BigDecimal fluxDataOutCost) {
		costs.setFluxDataOutCost(fluxDataOutCost);
	}

	/**
	 * Get the SolarFlux data out tier cost breakdown.
	 *
	 * @return the costs, never {@code null}
	 * @since 2.6
	 */
	@JsonIgnore
	public final List<NamedCost> getFluxDataOutTiersCostBreakdown() {
		return tiersCostBreakdown(fluxDataOutTiers, costsTiers, NodeUsageCost::getFluxDataOutCost);
	}

	/**
	 * Get the SolarFlux data out, per tier.
	 *
	 * @return the counts
	 * @since 2.6
	 */
	public final BigInteger @Nullable [] getFluxDataOutTiers() {
		return fluxDataOutTiers;
	}

	/**
	 * Set the SolarFlux data out, per tier.
	 *
	 * @param fluxDataOutTiers
	 *        the counts to set
	 * @since 2.6
	 */
	public final void setFluxDataOutTiers(BigInteger @Nullable [] fluxDataOutTiers) {
		this.fluxDataOutTiers = fluxDataOutTiers;
	}

	/**
	 * Set the SolarFlux data out, per tier, as decimals.
	 *
	 * @param fluxDataOutTiers
	 *        the counts to set
	 * @since 2.6
	 */
	public final void setFluxDataOutTiersNumeric(BigDecimal @Nullable [] fluxDataOutTiers) {
		this.fluxDataOutTiers = decimalsToIntegers(fluxDataOutTiers);
	}

	/**
	 * Get the cost of SolarFlux data out, per tier.
	 *
	 * @return the cost
	 * @since 2.6
	 */
	public final BigDecimal @Nullable [] getFluxDataOutCostTiers() {
		return getTierCostValues(costsTiers, NodeUsageCost::getFluxDataOutCost);
	}

	/**
	 * Set the cost of SolarFlux data out, per tier.
	 *
	 * @param fluxDataOutCostTiers
	 *        the costs to set
	 * @since 2.6
	 */
	public final void setFluxDataOutCostTiers(BigDecimal @Nullable [] fluxDataOutCostTiers) {
		prepCostsTiers(fluxDataOutCostTiers);
		if ( costsTiers == null ) {
			return;
		}
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
	public final BigInteger getOauthClientCredentials() {
		return oauthClientCredentials;
	}

	/**
	 * Set the count of OAuth client credentials.
	 *
	 * @param oauthClientCredentials
	 *        the count to set; if {@code null} then {@literal 0} will be stored
	 * @since 2.6
	 */
	public final void setOauthClientCredentials(BigInteger oauthClientCredentials) {
		if ( oauthClientCredentials == null ) {
			oauthClientCredentials = ZERO;
		}
		this.oauthClientCredentials = oauthClientCredentials;
	}

	/**
	 * Get the cost of OAuth client credentials.
	 *
	 * @return the cost
	 * @since 2.6
	 */
	public final BigDecimal getOauthClientCredentialsCost() {
		return costs.getOauthClientCredentialsCost();
	}

	/**
	 * Set the cost of OAuth client credentials.
	 *
	 * @param oauthClientCredentialsCost
	 *        the cost to set
	 * @since 2.6
	 */
	public final void setOauthClientCredentialsCost(BigDecimal oauthClientCredentialsCost) {
		costs.setOauthClientCredentialsCost(oauthClientCredentialsCost);
	}

	/**
	 * Get the OAuth client credentials tier cost breakdown.
	 *
	 * @return the costs, never {@code null}
	 * @since 2.6
	 */
	@JsonIgnore
	public final List<NamedCost> getOauthClientCredentialsTiersCostBreakdown() {
		return tiersCostBreakdown(oauthClientCredentialsTiers, costsTiers,
				NodeUsageCost::getOauthClientCredentialsCost);
	}

	/**
	 * Get the OAuth client credentials, per tier.
	 *
	 * @return the counts
	 * @since 2.6
	 */
	public final BigInteger @Nullable [] getOauthClientCredentialsTiers() {
		return oauthClientCredentialsTiers;
	}

	/**
	 * Set the OAuth client credentials, per tier.
	 *
	 * @param oauthClientCredentialsTiers
	 *        the counts to set
	 * @since 2.6
	 */
	public final void setOauthClientCredentialsTiers(
			BigInteger @Nullable [] oauthClientCredentialsTiers) {
		this.oauthClientCredentialsTiers = oauthClientCredentialsTiers;
	}

	/**
	 * Set the OAuth client credentials, per tier, as decimals.
	 *
	 * @param oauthClientCredentialsTiers
	 *        the counts to set
	 * @since 2.6
	 */
	public final void setOauthClientCredentialsTiersNumeric(
			BigDecimal @Nullable [] oauthClientCredentialsTiers) {
		this.oauthClientCredentialsTiers = decimalsToIntegers(oauthClientCredentialsTiers);
	}

	/**
	 * Get the cost of OAuth client credentials, per tier.
	 *
	 * @return the cost
	 * @since 2.6
	 */
	public final BigDecimal @Nullable [] getOauthClientCredentialsCostTiers() {
		return getTierCostValues(costsTiers, NodeUsageCost::getOauthClientCredentialsCost);
	}

	/**
	 * Set the cost of OAuth client credentials, per tier.
	 *
	 * @param oauthClientCredentialsCostTiers
	 *        the costs to set
	 * @since 2.6
	 */
	public final void setOauthClientCredentialsCostTiers(
			BigDecimal @Nullable [] oauthClientCredentialsCostTiers) {
		prepCostsTiers(oauthClientCredentialsCostTiers);
		if ( costsTiers == null ) {
			return;
		}
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
	public final BigInteger getCloudIntegrationsData() {
		return cloudIntegrationsData;
	}

	/**
	 * Set the count of Cloud Integrations data.
	 *
	 * @param cloudIntegrationsData
	 *        the count to set; if {@code null} then {@literal 0} will be stored
	 * @since 2.7
	 */
	public final void setCloudIntegrationsData(BigInteger cloudIntegrationsData) {
		if ( cloudIntegrationsData == null ) {
			cloudIntegrationsData = ZERO;
		}
		this.cloudIntegrationsData = cloudIntegrationsData;
	}

	/**
	 * Get the cost of Cloud Integrations data.
	 *
	 * @return the cost
	 * @since 2.7
	 */
	public final BigDecimal getCloudIntegrationsDataCost() {
		return costs.getCloudIntegrationsDataCost();
	}

	/**
	 * Set the cost of Cloud Integrations data.
	 *
	 * @param cloudIntegrationsDataCost
	 *        the cost to set
	 * @since 2.7
	 */
	public final void setCloudIntegrationsDataCost(BigDecimal cloudIntegrationsDataCost) {
		costs.setCloudIntegrationsDataCost(cloudIntegrationsDataCost);
	}

	/**
	 * Get the Cloud Integrations data tier cost breakdown.
	 *
	 * @return the costs, never {@code null}
	 * @since 2.7
	 */
	@JsonIgnore
	public final List<NamedCost> getCloudIntegrationsDataTiersCostBreakdown() {
		return tiersCostBreakdown(cloudIntegrationsDataTiers, costsTiers,
				NodeUsageCost::getCloudIntegrationsDataCost);
	}

	/**
	 * Get the Cloud Integrations data, per tier.
	 *
	 * @return the counts
	 * @since 2.7
	 */
	public final BigInteger @Nullable [] getCloudIntegrationsDataTiers() {
		return cloudIntegrationsDataTiers;
	}

	/**
	 * Set the Cloud Integrations data, per tier.
	 *
	 * @param cloudIntegrationsDataTiers
	 *        the counts to set
	 * @since 2.7
	 */
	public final void setCloudIntegrationsDataTiers(BigInteger @Nullable [] cloudIntegrationsDataTiers) {
		this.cloudIntegrationsDataTiers = cloudIntegrationsDataTiers;
	}

	/**
	 * Set the Cloud Integrations data, per tier, as decimals.
	 *
	 * @param cloudIntegrationsDataTiers
	 *        the counts to set
	 * @since 2.7
	 */
	public final void setCloudIntegrationsDataTiersNumeric(
			BigDecimal @Nullable [] cloudIntegrationsDataTiers) {
		this.cloudIntegrationsDataTiers = decimalsToIntegers(cloudIntegrationsDataTiers);
	}

	/**
	 * Get the cost of Cloud Integrations data, per tier.
	 *
	 * @return the cost
	 * @since 2.7
	 */
	public final BigDecimal @Nullable [] getCloudIntegrationsDataCostTiers() {
		return getTierCostValues(costsTiers, NodeUsageCost::getCloudIntegrationsDataCost);
	}

	/**
	 * Set the cost of Cloud Integrations data, per tier.
	 *
	 * @param cloudIntegrationsDataCostTiers
	 *        the costs to set
	 * @since 2.7
	 */
	public final void setCloudIntegrationsDataCostTiers(
			BigDecimal @Nullable [] cloudIntegrationsDataCostTiers) {
		prepCostsTiers(cloudIntegrationsDataCostTiers);
		if ( costsTiers == null ) {
			return;
		}
		for ( int i = 0; i < costsTiers.length; i++ ) {
			BigDecimal val = (cloudIntegrationsDataCostTiers != null
					&& i < cloudIntegrationsDataCostTiers.length ? cloudIntegrationsDataCostTiers[i]
							: null);
			costsTiers[i].setCloudIntegrationsDataCost(val);
		}
	}

}
