/* ==================================================================
 * EventRule.java - Jun 3, 2011 1:35:37 PM
 * 
 * Copyright 2007-2011 SolarNetwork.net Dev Team
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
 * $Id$
 * ==================================================================
 */

package net.solarnetwork.central.dras.domain;

import java.io.Serializable;
import java.util.Set;

import org.joda.time.Duration;

import net.solarnetwork.central.domain.BaseEntity;

/**
 * An event rule.
 * 
 * @author matt
 * @version $Revision$
 */
public class EventRule extends BaseEntity implements Cloneable, Serializable {

	private static final long serialVersionUID = -4870404538222436199L;

	/**
	 * The rule type.
	 */
	public enum RuleKind {
		PRICE_ABSOLUTE, 
		PRICE_RELATIVE,
		PRICE_MULTIPLE,
		LOAD_LEVEL,
		LOAD_AMOUNT,
		LOAD_PERCENTAGE,
		GRID_RELIABILITY,
	}

	/**
	 * The rule schedule type.
	 */
	public enum ScheduleKind {
		NONE, DYNAMIC, STATIC,
	}

	private Long creator;
	private RuleKind kind;
	private String name;
	private Double min;
	private Double max;
	private ScheduleKind scheduleKind;
	private Set<Duration> schedule;
	private Set<Double> enumeration;
	
	/**
	 * Default constructor.
	 */
	public EventRule() {
		super();
	}
	
	/**
	 * Construct with ID.
	 * 
	 * @param id the ID
	 */
	public EventRule(Long id) {
		super();
		setId(id);
	}
	
	/**
	 * Construct with values.
	 * 
	 * @param id the ID
	 * @param kind the kind
	 * @param scheduleKind the schedule kind
	 */
	public EventRule(Long id, RuleKind kind, ScheduleKind scheduleKind) {
		this(id);
		setKind(kind);
		setScheduleKind(scheduleKind);
	}
	
	public Long getCreator() {
		return creator;
	}
	public void setCreator(Long creator) {
		this.creator = creator;
	}
	public RuleKind getKind() {
		return kind;
	}
	public void setKind(RuleKind kind) {
		this.kind = kind;
	}
	public Double getMin() {
		return min;
	}
	public void setMin(Double min) {
		this.min = min;
	}
	public Double getMax() {
		return max;
	}
	public void setMax(Double max) {
		this.max = max;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public ScheduleKind getScheduleKind() {
		return scheduleKind;
	}
	public void setScheduleKind(ScheduleKind scheduleKind) {
		this.scheduleKind = scheduleKind;
	}
	public Set<Duration> getSchedule() {
		return schedule;
	}
	public void setSchedule(Set<Duration> schedule) {
		this.schedule = schedule;
	}
	public Set<Double> getEnumeration() {
		return enumeration;
	}
	public void setEnumeration(Set<Double> enumeration) {
		this.enumeration = enumeration;
	}

}
