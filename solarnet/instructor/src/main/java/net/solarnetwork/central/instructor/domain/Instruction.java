/* ==================================================================
 * Instruction.java - Mar 1, 2011 11:21:25 AM
 *
 * Copyright 2007 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.instructor.domain;

import java.io.Serial;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.annotation.JsonSetter;
import net.solarnetwork.central.dao.BaseEntity;
import net.solarnetwork.codec.JsonUtils;
import net.solarnetwork.domain.InstructionStatus.InstructionState;

/**
 * Domain object for an individual instruction.
 *
 * @author matt
 * @version 2.5
 */
public class Instruction extends BaseEntity {

	@Serial
	private static final long serialVersionUID = -1424063634197303751L;

	private String topic;
	private Instant instructionDate;
	private Instant statusDate;
	private InstructionState state = InstructionState.Unknown;
	private List<InstructionParameter> parameters;
	private Map<String, Object> resultParameters;
	private Instant expirationDate;

	private String resultParametersJson;

	/**
	 * Default constructor.
	 */
	public Instruction() {
		super();
	}

	/**
	 * Construct with data.
	 *
	 * @param topic
	 *        the topic
	 * @param instructionDate
	 *        the instruction date
	 */
	public Instruction(String topic, Instant instructionDate) {
		super();
		this.topic = topic;
		this.instructionDate = instructionDate;
	}

	/**
	 * Copy constructor.
	 *
	 * @param other
	 *        the instance to copy
	 * @since 1.2
	 */
	public Instruction(Instruction other) {
		this(other.getTopic(), other.getInstructionDate());
		setId(other.getId());
		setStatusDate(other.getStatusDate());
		setParameters(other.getParameters());
		setResultParameters(other.getResultParameters());
		setState(other.getState());
		setExpirationDate(other.getExpirationDate());
	}

	@Override
	public Instruction clone() {
		return (Instruction) super.clone();
	}

	/**
	 * Remove all parameters.
	 */
	public void clearParameters() {
		parameters.clear();
	}

	/**
	 * Add a parameter value.
	 *
	 * @param key
	 *        the key
	 * @param value
	 *        the value
	 */
	public void addParameter(String key, String value) {
		if ( parameters == null ) {
			parameters = new ArrayList<>(5);
		}
		parameters.add(new InstructionParameter(key, value));
	}

	/**
	 * Set a result parameter value.
	 *
	 * @param key
	 *        the key
	 * @param value
	 *        the value
	 */
	public void putResultParameter(String key, Object value) {
		Map<String, Object> map = resultParameters;
		if ( map == null ) {
			map = new LinkedHashMap<>();
			resultParameters = map;
		}
		map.put(key, value);
	}

	/**
	 * Get the topic.
	 *
	 * @return the topic
	 */
	public final String getTopic() {
		return topic;
	}

	/**
	 * Set the topic.
	 *
	 * @param topic
	 *        the topic to set
	 */
	public final void setTopic(String topic) {
		this.topic = topic;
	}

	/**
	 * Get the instruction date.
	 *
	 * @return the date
	 */
	public final Instant getInstructionDate() {
		return instructionDate;
	}

	/**
	 * Set the instruction date.
	 *
	 * @param instructionDate
	 *        the date to set
	 */
	public final void setInstructionDate(Instant instructionDate) {
		this.instructionDate = instructionDate;
	}

	/**
	 * Get the status date.
	 *
	 * @return the status date
	 */
	public final Instant getStatusDate() {
		return statusDate;
	}

	/**
	 * Set the status date.
	 *
	 * @param statusDate
	 *        the date to set
	 */
	public final void setStatusDate(Instant statusDate) {
		this.statusDate = statusDate;
	}

	/**
	 * Get the state.
	 *
	 * @return the state
	 */
	public final InstructionState getState() {
		return state;
	}

	/**
	 * Set the state.
	 *
	 * @param state
	 *        the state to set
	 */
	public final void setState(InstructionState state) {
		this.state = state;
	}

	/**
	 * Get the parameters.
	 *
	 * @return the parameters
	 */
	public final List<InstructionParameter> getParameters() {
		return parameters;
	}

	/**
	 * Set the parameters.
	 *
	 * @param parameters
	 *        the parameters to set
	 */
	public final void setParameters(List<InstructionParameter> parameters) {
		this.parameters = parameters;
	}

	/**
	 * Get the instruction parameters as a single-valued map.
	 *
	 * @return the parameters as a map, or {@literal null} if
	 *         {@link #getParameters()} is {@literal null}
	 * @since 1.3
	 */
	@JsonIgnore
	public Map<String, String> getParams() {
		List<InstructionParameter> l = getParameters();
		if ( l == null ) {
			return null;
		}
		Map<String, String> params = new LinkedHashMap<>(l.size());
		for ( InstructionParameter p : l ) {
			if ( p.getName() != null && p.getValue() != null ) {
				params.merge(p.getName(), p.getValue(), String::concat);
			}
		}
		return params;
	}

	/**
	 * Set the instruction parameters as a single-valued map.
	 *
	 * <p>
	 * This completely replaces any existing parameters set via
	 * {@link #setParameters(List)}.
	 * </p>
	 *
	 * @param params
	 *        the parameters to set
	 * @since 1.3
	 */
	@JsonSetter("params")
	public void setParams(Map<String, String> params) {
		List<InstructionParameter> l = null;
		if ( params != null ) {
			l = new ArrayList<>(params.size());
			for ( Map.Entry<String, String> e : params.entrySet() ) {
				if ( e.getKey() != null && e.getValue() != null ) {
					l.add(new InstructionParameter(e.getKey(), e.getValue()));
				}
			}
		}
		setParameters(l);
	}

	/**
	 * Get the result parameters.
	 *
	 * @return the result parameters
	 */
	@JsonIgnore
	@SuppressWarnings("unchecked")
	public Map<String, Object> getResultParameters() {
		Map<String, Object> map = this.resultParameters;
		if ( map != null ) {
			return map;
		}
		String json = resultParametersJson;
		if ( json != null ) {
			map = JsonUtils.getObjectFromJSON(json, Map.class);
			this.resultParameters = map;
		}
		return map;
	}

	/**
	 * Set the result parameters.
	 *
	 * @param resultParameters
	 *        the parameters to set
	 */
	@JsonIgnore
	public void setResultParameters(Map<String, Object> resultParameters) {
		this.resultParameters = resultParameters;
		resultParametersJson = null;
	}

	/**
	 * Get the result parameters object as a JSON string.
	 *
	 * @return a JSON encoded string, never <em>null</em>
	 */
	@JsonGetter("resultParameters")
	@JsonRawValue
	public String getResultParametersJson() {
		if ( resultParametersJson != null ) {
			return resultParametersJson;
		}
		Map<String, Object> map = getResultParameters();
		if ( map == null || map.isEmpty() ) {
			return null;
		}
		String json = JsonUtils.getJSONString(map, null);
		resultParametersJson = json;
		return json;
	}

	/**
	 * Set the result parameters object via a JSON string.
	 *
	 * <p>
	 * This method will remove any previously created result parameters and
	 * replace it with the values parsed from the JSON.
	 * </p>
	 *
	 * @param json
	 *        the JSON to set
	 */
	@JsonSetter("resultParameters")
	@JsonRawValue
	public void setResultParametersJson(String json) {
		resultParametersJson = json;
		resultParameters = null;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Instruction{id=");
		builder.append(getId());
		builder.append(", topic=");
		builder.append(topic);
		builder.append(", state=");
		builder.append(state);
		builder.append(", parameters=");
		builder.append(parameters);
		builder.append("}");
		return builder.toString();
	}

	/**
	 * Get the expiration date.
	 * 
	 * @return the expiration date
	 */
	public Instant getExpirationDate() {
		return expirationDate;
	}

	/**
	 * Set the expiration date.
	 * 
	 * <p>
	 * This date represents the point in time that a "pending" instruction can
	 * be automatically transitioned to the {@code Declined} state, adding an
	 * appropriate {@code message} result property.
	 * </p>
	 * 
	 * @param expirationDate
	 *        the expiration date to set
	 */
	public void setExpirationDate(Instant expirationDate) {
		this.expirationDate = expirationDate;
	}

}
