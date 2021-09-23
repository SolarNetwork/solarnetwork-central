/* ==================================================================
 * ReportingGeneralNodeDatumReading.java - 13/02/2019 1:41:27 pm
 * 
 * Copyright 2019 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.datum.domain;

import java.math.BigDecimal;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import net.solarnetwork.central.datum.support.DatumUtils;
import net.solarnetwork.domain.SerializeIgnore;
import net.solarnetwork.domain.datum.DatumSamples;

/**
 * Extension of {@link ReportingGeneralNodeDatum} geared towards reading
 * reporting data.
 * 
 * <p>
 * This data model uses the existing {@link GeneralNodeDatum#getSamples()} data
 * to hold the reading difference values.
 * </p>
 * 
 * @author matt
 * @version 2.0
 * @since 1.36
 */
@JsonPropertyOrder({ "created", "nodeId", "sourceId", "localDate", "localTime" })
public class ReportingGeneralNodeDatumReading extends ReportingGeneralNodeDatum implements ReadingDatum {

	private static final long serialVersionUID = 9141977325189089319L;

	private DatumSamples samplesFinal;
	private String sampleJsonFinal;
	private DatumSamples samplesStart;
	private String sampleJsonStart;

	/**
	 * Constructor.
	 */
	public ReportingGeneralNodeDatumReading() {
		super();
	}

	/**
	 * Get a merged map of all sample data, including start/final data.
	 * 
	 * <p>
	 * The {@link #getSampleDataStart()} entries will be included, with keys
	 * suffixed with {@literal _start}. Similarly the
	 * {@link #getSampleDataFinal()} entries will be included, with keys
	 * suffixed with {@literal _end}.
	 * </p>
	 * 
	 * @return a map with all sample data combined
	 */
	@JsonUnwrapped
	@JsonAnyGetter
	@Override
	public Map<String, ?> getSampleData() {
		@SuppressWarnings({ "unchecked", "rawtypes" })
		Map<String, Object> data = (Map) super.getSampleData();

		Map<String, ?> dataStart = getSampleDataStart();
		if ( dataStart != null ) {
			for ( Map.Entry<String, ?> me : dataStart.entrySet() ) {
				String key = me.getKey();
				if ( key == null ) {
					continue;
				}
				data.put(key + START_PROPERTY_SUFFIX, me.getValue());
			}
		}

		Map<String, ?> dataFinal = getSampleDataFinal();
		if ( dataFinal != null ) {
			for ( Map.Entry<String, ?> me : dataFinal.entrySet() ) {
				String key = me.getKey();
				if ( key == null ) {
					continue;
				}
				data.put(key + FINAL_PROPERTY_SUFFIX, me.getValue());
			}
		}

		return data;
	}

	/**
	 * Get the final {@link DatumSamples} object as a JSON string.
	 * 
	 * <p>
	 * This method will ignore {@literal null} values.
	 * </p>
	 * 
	 * @return a JSON encoded string, never {@literal null}
	 */
	@SerializeIgnore
	@JsonIgnore
	public String getSampleJsonFinal() {
		if ( sampleJsonFinal == null ) {
			sampleJsonFinal = DatumUtils.getJSONString(samplesFinal, "{}");
		}
		return sampleJsonFinal;
	}

	/**
	 * Set the final {@link DatumSamples} object via a JSON string.
	 * 
	 * <p>
	 * This method will remove any previously created final DatumSamples and
	 * replace it with the values parsed from the JSON. All floating point
	 * values will be converted to {@link BigDecimal} instances.
	 * </p>
	 * 
	 * @param json
	 *        the JSON to set
	 */
	@JsonProperty
	// @JsonProperty needed because of @JsonIgnore on getter
	public void setSampleJsonFinal(String json) {
		sampleJsonFinal = json;
		samplesFinal = null;
	}

	@SerializeIgnore
	@JsonIgnore
	public DatumSamples getSamplesFinal() {
		if ( samplesFinal == null && sampleJsonFinal != null ) {
			samplesFinal = DatumUtils.getObjectFromJSON(sampleJsonFinal, DatumSamples.class);
		}
		return samplesFinal;
	}

	/**
	 * Set the final {@link DatumSamples} instance to use.
	 * 
	 * <p>
	 * This will replace any value set previously via
	 * {@link #setSampleJsonFinal(String)} as well.
	 * </p>
	 * 
	 * @param samples
	 *        the samples instance to set
	 */
	@JsonSetter("final")
	public void setSamplesFinal(DatumSamples samples) {
		samplesFinal = samples;
		sampleJsonFinal = null;
	}

	/**
	 * Convenience method for final {@link DatumSamples#getSampleData()}.
	 * 
	 * @return the sample data, or {@literal null} if none available
	 */
	@Override
	@SerializeIgnore
	@JsonIgnore
	public Map<String, ?> getSampleDataFinal() {
		DatumSamples s = getSamplesFinal();
		return (s == null ? null : s.getSampleData());
	}

	/**
	 * Get the start {@link DatumSamples} object as a JSON string.
	 * 
	 * <p>
	 * This method will ignore {@literal null} values.
	 * </p>
	 * 
	 * @return a JSON encoded string, never {@literal null}
	 */
	@SerializeIgnore
	@JsonIgnore
	public String getSampleJsonStart() {
		if ( sampleJsonStart == null ) {
			sampleJsonStart = DatumUtils.getJSONString(samplesStart, "{}");
		}
		return sampleJsonStart;
	}

	/**
	 * Set the start {@link DatumSamples} object via a JSON string.
	 * 
	 * <p>
	 * This method will remove any previously created start DatumSamples and
	 * replace it with the values parsed from the JSON. All floating point
	 * values will be converted to {@link BigDecimal} instances.
	 * </p>
	 * 
	 * @param json
	 *        the JSON to set
	 */
	@JsonProperty
	// @JsonProperty needed because of @JsonIgnore on getter
	public void setSampleJsonStart(String json) {
		sampleJsonStart = json;
		samplesStart = null;
	}

	@SerializeIgnore
	@JsonIgnore
	public DatumSamples getSamplesStart() {
		if ( samplesStart == null && sampleJsonStart != null ) {
			samplesStart = DatumUtils.getObjectFromJSON(sampleJsonStart, DatumSamples.class);
		}
		return samplesStart;
	}

	/**
	 * Set the start {@link DatumSamples} instance to use.
	 * 
	 * <p>
	 * This will replace any value set previously via
	 * {@link #setSampleJsonStart(String)} as well.
	 * </p>
	 * 
	 * @param samples
	 *        the samples instance to set
	 */
	@JsonSetter("start")
	public void setSamplesStart(DatumSamples samples) {
		samplesStart = samples;
		sampleJsonStart = null;
	}

	/**
	 * Convenience method for final {@link DatumSamples#getSampleData()}.
	 * 
	 * @return the sample data, or {@literal null} if none available
	 */
	@Override
	@JsonIgnore
	@SerializeIgnore
	public Map<String, ?> getSampleDataStart() {
		DatumSamples s = getSamplesStart();
		return (s == null ? null : s.getSampleData());
	}

}
