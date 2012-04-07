/* ==================================================================
 * Constraint.java - Jun 21, 2011 4:29:05 PM
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
import java.util.List;

import org.joda.time.Duration;
import org.joda.time.LocalTime;

import net.solarnetwork.central.domain.BaseEntity;

/**
 * Domain object for DRAS constraints.
 * 
 * <p>Constraints are associated to various domain objects, such as programs, users, 
 * and participants. When events are initiated, the constraints form a basic set of 
 * rules that are reconciled to determine which participants can participate in the
 * event.</p>
 * 
 * @author matt
 * @version $Revision$
 */
public class Constraint extends BaseEntity implements Cloneable, Serializable, Match, Member {

	private static final long serialVersionUID = 8090211291853291101L;

	/** 
	 * Possible filter values for every constraint, indicating what 
	 * happens if constraint is not met.
	 */
	public enum FilterKind {
		ACCEPT, 
		REJECT, 
		FORCE, 
		RESTRICT
	}
	
	private LocalTime eventWindowStart;
	private LocalTime eventWindowEnd;
	private FilterKind eventWindowFilter;
	private Duration maxEventDuration;
	private FilterKind maxEventDurationFilter;
	private Duration notificationWindowMax;
	private Duration notificationWindowMin;
	private FilterKind notificationWindowFilter;
	private Integer maxConsecutiveDays;
	private FilterKind maxConsecutiveDaysFilter;
	private List<DateTimeWindow> blackoutDates;
	private FilterKind blackoutDatesFilter;
	private List<DateTimeWindow> validDates;
	private FilterKind validDatesFilter;
	
	/**
	 * Default constructor.
	 */
	public Constraint() {
		super();
	}
	
	/**
	 * Construct with an ID.
	 * 
	 * @param id the ID
	 */
	public Constraint(Long id) {
		super();
		setId(id);
	}
	
	public FilterKind getEventWindowFilter() {
		return eventWindowFilter;
	}
	public void setEventWindowFilter(FilterKind eventWindowFilter) {
		this.eventWindowFilter = eventWindowFilter;
	}
	public Duration getMaxEventDuration() {
		return maxEventDuration;
	}
	public void setMaxEventDuration(Duration maxEventDuration) {
		this.maxEventDuration = maxEventDuration;
	}
	public FilterKind getMaxEventDurationFilter() {
		return maxEventDurationFilter;
	}
	public void setMaxEventDurationFilter(FilterKind maxEventDurationFilter) {
		this.maxEventDurationFilter = maxEventDurationFilter;
	}
	public FilterKind getNotificationWindowFilter() {
		return notificationWindowFilter;
	}
	public void setNotificationWindowFilter(FilterKind notificationWindowFilter) {
		this.notificationWindowFilter = notificationWindowFilter;
	}
	public Integer getMaxConsecutiveDays() {
		return maxConsecutiveDays;
	}
	public void setMaxConsecutiveDays(Integer maxConsecutiveDays) {
		this.maxConsecutiveDays = maxConsecutiveDays;
	}
	public FilterKind getMaxConsecutiveDaysFilter() {
		return maxConsecutiveDaysFilter;
	}
	public void setMaxConsecutiveDaysFilter(FilterKind maxConsecutiveDaysFilter) {
		this.maxConsecutiveDaysFilter = maxConsecutiveDaysFilter;
	}
	public List<DateTimeWindow> getBlackoutDates() {
		return blackoutDates;
	}
	public void setBlackoutDates(List<DateTimeWindow> blackoutDates) {
		this.blackoutDates = blackoutDates;
	}
	public FilterKind getBlackoutDatesFilter() {
		return blackoutDatesFilter;
	}
	public void setBlackoutDatesFilter(FilterKind blackoutDatesFilter) {
		this.blackoutDatesFilter = blackoutDatesFilter;
	}
	public List<DateTimeWindow> getValidDates() {
		return validDates;
	}
	public void setValidDates(List<DateTimeWindow> validDates) {
		this.validDates = validDates;
	}
	public FilterKind getValidDatesFilter() {
		return validDatesFilter;
	}
	public void setValidDatesFilter(FilterKind validDatesFilter) {
		this.validDatesFilter = validDatesFilter;
	}
	public LocalTime getEventWindowStart() {
		return eventWindowStart;
	}
	public void setEventWindowStart(LocalTime eventWindowStart) {
		this.eventWindowStart = eventWindowStart;
	}
	public LocalTime getEventWindowEnd() {
		return eventWindowEnd;
	}
	public void setEventWindowEnd(LocalTime eventWindowEnd) {
		this.eventWindowEnd = eventWindowEnd;
	}
	public Duration getNotificationWindowMax() {
		return notificationWindowMax;
	}
	public void setNotificationWindowMax(Duration notificationWindowMax) {
		this.notificationWindowMax = notificationWindowMax;
	}
	public Duration getNotificationWindowMin() {
		return notificationWindowMin;
	}
	public void setNotificationWindowMin(Duration notificationWindowMin) {
		this.notificationWindowMin = notificationWindowMin;
	}

}
