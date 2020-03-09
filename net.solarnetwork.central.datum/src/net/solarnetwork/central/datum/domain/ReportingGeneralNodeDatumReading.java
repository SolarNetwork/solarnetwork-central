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
import net.solarnetwork.domain.GeneralNodeDatumSamples;
import net.solarnetwork.util.SerializeIgnore;

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
 * @version 1.0
 * @since 1.36
 */
@JsonPropertyOrder({ "created", "nodeId", "sourceId", "localDate", "localTime" })
public class ReportingGeneralNodeDatumReading extends ReportingGeneralNodeDatum {

	private static final long serialVersionUID = -3743593391188435695L;

	private GeneralNodeDatumSamples samplesFinal;
	private String sampleJsonFinal;
	private GeneralNodeDatumSamples samplesStart;
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
				data.put(key + "_start", me.getValue());
			}
		}

		Map<String, ?> dataFinal = getSampleDataFinal();
		if ( dataFinal != null ) {
			for ( Map.Entry<String, ?> me : dataFinal.entrySet() ) {
				String key = me.getKey();
				if ( key == null ) {
					continue;
				}
				data.put(key + "_end", me.getValue());
			}
		}

		return data;
	}

	/**
	 * Get the final {@link GeneralNodeDatumSamples} object as a JSON string.
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
	 * Set the final {@link GeneralNodeDatumSamples} object via a JSON string.
	 * 
	 * <p>
	 * This method will remove any previously created final
	 * GeneralNodeDatumSamples and replace it with the values parsed from the
	 * JSON. All floating point values will be converted to {@link BigDecimal}
	 * instances.
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
	public GeneralNodeDatumSamples getSamplesFinal() {
		if ( samplesFinal == null && sampleJsonFinal != null ) {
			samplesFinal = DatumUtils.getObjectFromJSON(sampleJsonFinal, GeneralNodeDatumSamples.class);
		}
		return samplesFinal;
	}

	/**
	 * Set the final {@link GeneralNodeDatumSamples} instance to use.
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
	public void setSamplesFinal(GeneralNodeDatumSamples samples) {
		samplesFinal = samples;
		sampleJsonFinal = null;
	}

	/**
	 * Convenience method for final
	 * {@link GeneralNodeDatumSamples#getSampleData()}.
	 * 
	 * @return the sample data, or {@literal null} if none available
	 */
	@SerializeIgnore
	@JsonIgnore
	public Map<String, ?> getSampleDataFinal() {
		GeneralNodeDatumSamples s = getSamplesFinal();
		return (s == null ? null : s.getSampleData());
	}

	/**
	 * Get the start {@link GeneralNodeDatumSamples} object as a JSON string.
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
	 * Set the start {@link GeneralNodeDatumSamples} object via a JSON string.
	 * 
	 * <p>
	 * This method will remove any previously created start
	 * GeneralNodeDatumSamples and replace it with the values parsed from the
	 * JSON. All floating point values will be converted to {@link BigDecimal}
	 * instances.
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
	public GeneralNodeDatumSamples getSamplesStart() {
		if ( samplesStart == null && sampleJsonStart != null ) {
			samplesStart = DatumUtils.getObjectFromJSON(sampleJsonStart, GeneralNodeDatumSamples.class);
		}
		return samplesStart;
	}

	/**
	 * Set the start {@link GeneralNodeDatumSamples} instance to use.
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
	public void setSamplesStart(GeneralNodeDatumSamples samples) {
		samplesStart = samples;
		sampleJsonStart = null;
	}

	/**
	 * Convenience method for final
	 * {@link GeneralNodeDatumSamples#getSampleData()}.
	 * 
	 * @return the sample data, or {@literal null} if none available
	 */
	@JsonIgnore
	@SerializeIgnore
	public Map<String, ?> getSampleDataStart() {
		GeneralNodeDatumSamples s = getSamplesStart();
		return (s == null ? null : s.getSampleData());
	}

}
