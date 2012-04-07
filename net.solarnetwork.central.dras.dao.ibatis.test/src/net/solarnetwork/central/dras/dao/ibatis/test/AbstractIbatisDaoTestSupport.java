/* ==================================================================
 * AbstractIbatisDaoTestSupport.java - Jun 3, 2011 8:22:52 PM
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

package net.solarnetwork.central.dras.dao.ibatis.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import net.solarnetwork.central.domain.Identity;
import net.solarnetwork.central.dras.domain.Constraint;
import net.solarnetwork.central.dras.domain.DateTimeWindow;
import net.solarnetwork.central.dras.domain.Fee;
import net.solarnetwork.central.dras.domain.Constraint.FilterKind;
import net.solarnetwork.central.test.AbstractCentralTransactionalTest;

import org.apache.commons.codec.digest.DigestUtils;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.LocalTime;
import org.joda.time.Period;
import org.joda.time.ReadableDateTime;
import org.junit.Before;
import org.springframework.test.context.ContextConfiguration;

/**
 * Base test class for Ibatis DRAS DAO tests.
 * 
 * @author matt
 * @version $Revision$
 */
@ContextConfiguration
public class AbstractIbatisDaoTestSupport extends AbstractCentralTransactionalTest {
	
	public static final Long TEST_USER_ID = Long.valueOf(-99);
	public static final String TEST_USERNAME = "unittest";
	public static final Long TEST_EFFECTIVE_ID = -9999L;
	public static final Long TEST_LOCATION_ID = -9998L;
	public static final String TEST_LOCATION_NAME = "Test Location";
	public static final Long TEST_CAPABILITY_ID = -9997L;
	public static final Long TEST_PARTICIPANT_ID = -9996L;
	public static final Long TEST_PARTICIPANT_GROUP_ID = -9995L;
	public static final Long TEST_EVENT_RULE_ID = -9994L;
	public static final String TEST_EVENT_RULE_NAME = "Test Event Rule";
	public static final Long TEST_EVENT_TARGETS_ID = -9993L;
	public static final Long TEST_EVENT_ID = -9992L;
	public static final Long TEST_PROGRAM_ID = -9991L;
	public static final String TEST_PROGRAM_NAME = "Test Program";
	public static final ReadableDateTime TEST_PROGRAM_DATE = new DateTime(2001,1,1,12,0,0,0);
	public static final Long TEST_GROUP_ID = -9990L;
	public static final String TEST_GROUPNAME = "Test Group";
	public static final Long TEST_OUTBOUND_MAIL_ID = -9989L;
	public static final String TEST_OUTBOUND_MAIL_MESSAGE_ID = "test.message.190837oijdlksj039u73s";
	
	protected void validateMembers(Set<? extends Identity<?>> src, 
			Set<? extends Identity<?>> found) {
		assertNotNull(found);
		assertEquals("number of members", src.size(), found.size());
		for ( Identity<?> member : src ) {
			assertTrue("contains member " +member.getId(), found.contains(member));
		}
	}

	/**
	 * Setup the database before each test.
	 */
	@Before
	public void setupInTransaction() {		
		setupTestUser();
		setupTestEffective();
	}
	
	/**
	 * Insert a test role into solardras.dras_role
	 * 
	 * @param role the role
	 * @param desc the role description
	 */
	protected void setupTestRole(String role, String desc) {
		simpleJdbcTemplate.update(
				"insert into solardras.dras_role (rolename,description) values (?,?)", 
				role, desc);
	}
	
	/**
	 * Insert a test Effective into the solardras.effective table.
	 */
	protected void setupTestEffective() {
		simpleJdbcTemplate.update(
				"insert into solardras.effective (id, creator) values (?,?)", 
				TEST_EFFECTIVE_ID,TEST_USER_ID);
	}
	
	/**
	 * Insert a test user into the solardras.dras_user table.
	 */
	protected void setupTestUser() {
		setupTestUser(TEST_USER_ID, TEST_USERNAME);
	}

	/**
	 * Insert a test user into the solardras.dras_user table.
	 * 
	 * @param id the user ID
	 * @param username the user username
	 */
	protected void setupTestUser(Long id, String username) {
		simpleJdbcTemplate.update(
				"insert into solardras.dras_user (id,username,passwd,disp_name,enabled) values (?,?,?,?,?)", 
				id, username, DigestUtils.sha256Hex("password"), "Unit Test", Boolean.TRUE);
	}
	
	/**
	 * Insert a test user group into the solardras.dras_user_group table.
	 * 
	 * @param id the group ID
	 * @param name the group name
	 * @param locationId the location ID
	 */
	protected void setupTestUserGroup(Long id, String name, Long locationId) {
		simpleJdbcTemplate.update(
				"insert into solardras.dras_user_group (id,groupname,loc_id,enabled) values (?,?,?,?)", 
				id, name, locationId, Boolean.TRUE);

	}
	
	/**
	 * Assign a user to a user group.
	 * 
	 * @param groupId the group ID
	 * @param userId the user ID
	 * @param effectiveId the effective ID
	 */
	protected void assignUserToGroup(Long groupId, Long userId, Long effectiveId) {
		simpleJdbcTemplate.update(
				"insert into solardras.dras_user_group_member (ugr_id,usr_id,eff_id) values (?,?,?)", 
				groupId, userId, effectiveId);
	}

	/**
	 * Assign a user to a program.
	 * 
	 * @param programId the program ID
	 * @param userId the user ID
	 * @param effectiveId the effective ID
	 */
	protected void assignUserToProgram(Long programId, Long userId, Long effectiveId) {
		simpleJdbcTemplate.update(
				"insert into solardras.program_user (pro_id,usr_id,eff_id) values (?,?,?)", 
				programId, userId, effectiveId);
	}

	/**
	 * Insert a test location into the solardras.loc table.
	 */
	protected void setupTestLocation() {
		setupTestLocation(TEST_LOCATION_ID, TEST_LOCATION_NAME);
	}

	/**
	 * Insert a test location into the solardras.loc table.
	 */
	protected void setupTestLocation(Long id, String name) {
		simpleJdbcTemplate.update(
				"insert into solardras.loc (id,loc_name,country,locality,postal_code) "
				+"values (?,?,?,?,?)", 
				id, name, "NZ", "Wellington", "6000");
	}

	/**
	 * Insert a test capability into the solardras.capability table.
	 */
	protected void setupTestCapability() {
		simpleJdbcTemplate.update(
				"insert into solardras.capability (id,dr_kind,max_power,max_energy,max_var) "
				+"values (?,?,?,?,?)", 
				TEST_CAPABILITY_ID, "Test", 11, 12, 13);
	}
	
	/**
	 * Insert a test participant into the solardras.participant table.
	 * 
	 * <p>Note the test location must exist before calling this method.</p>
	 */
	protected void setupTestParticipant() {
		setupTestParticipant(TEST_PARTICIPANT_ID, TEST_USER_ID, TEST_LOCATION_ID);
	}
	
	/**
	 * Insert a test participant into the solardras.participant table.
	 * 
	 * <p>Note the test location must exist before calling this method.</p>
	 * 
	 * @param participantId the ID to assign
	 * @param userId the owner of the participant
	 * @param locationId the location ID
	 */
	protected void setupTestParticipant(Long participantId, Long userId, Long locationId) {
		simpleJdbcTemplate.update(
				"insert into solardras.participant (id,creator,usr_id,loc_id) "
				+"values (?,?,?,?)", 
				participantId, TEST_USER_ID, userId, locationId);
	}
	
	/**
	 * Insert a test participant group into the solardras.participant_group table.
	 * 
	 * <p>Note the test location and participant must exist before calling this method.</p>
	 */
	protected void setupTestParticipantGroup() {
		simpleJdbcTemplate.update(
				"insert into solardras.participant_group (id,creator,loc_id) "
				+"values (?,?,?)", 
				TEST_PARTICIPANT_GROUP_ID, TEST_USER_ID, TEST_LOCATION_ID);
	}
	
	/**
	 * Assign a participant to a participant group.
	 * 
	 * @param participanGroupId the group ID
	 * @param participantId the participant ID
	 * @param effectiveId the effective ID
	 */
	protected void assignParticipantToParticipantGroup(Long participanGroupId, Long participantId, Long effectiveId) {
		simpleJdbcTemplate.update(
				"insert into solardras.participant_group_member (pgr_id,par_id,eff_id) values (?,?,?)", 
				participanGroupId, participantId, effectiveId);
	}

	/**
	 * Insert a test event rule into the solardras.event_rule table.
	 */
	protected void setupTestEventRule() {
		setupTestEventRule(TEST_EVENT_RULE_ID, TEST_EVENT_RULE_NAME, null, null);
	}
	
	/**
	 * Insert a test event rule into the solardras.event_rule table.
	 * 
	 * @param id the ID
	 * @param name the name
	 */
	protected void setupTestEventRule(Long id, String name, Set<Double> enums, Set<Duration> sched) {
		simpleJdbcTemplate.update(
				"insert into solardras.event_rule (id,creator,rule_name,min_value,max_value) "
				+"values (?,?,?,?,?)", 
				id, TEST_USER_ID, name, 3, 4);
		if ( enums != null ) {
			for ( Double d : enums ) {
				simpleJdbcTemplate.update(
						"insert into solardras.event_rule_enum (evr_id,target_value) values (?,?)", 
						id, d);
			}
		}
		if ( sched != null ) {
			for ( Duration d : sched ) {
				simpleJdbcTemplate.update(
						"insert into solardras.event_rule_schedule (evr_id,event_offset) values (?, CAST(? AS INTERVAL))", 
						id, d.toString());
			}
		}
	}

	/**
	 * Insert a test event rule into the solardras.event_rule table.
	 */
	protected void setupTestEventTargets(Long eventRuleId) {
		simpleJdbcTemplate.update(
				"insert into solardras.event_target (id,evr_id,end_offset) "
				+"values (?,?,CAST(? AS interval))", 
				TEST_EVENT_TARGETS_ID, eventRuleId, "PT1H");
		simpleJdbcTemplate.update(
				"insert into solardras.event_target_value (eta_id,event_offset,target_value) "
				+"values (?,CAST(? AS interval),?)",
				TEST_EVENT_TARGETS_ID, "PT20M", 10000);
		simpleJdbcTemplate.update(
				"insert into solardras.event_target_value (eta_id,event_offset,target_value) "
				+"values (?,CAST(? AS interval),?)",
				TEST_EVENT_TARGETS_ID, "PT40M", 8000);
	}
	
	/**
	 * Insert a test event into the solardras.program table.
	 */
	protected void setupTestProgram(Long programId, String programName) {
		simpleJdbcTemplate.update(
				"insert into solardras.program (id,creator,pro_name,enabled) values (?,?,?,?)", 
				programId, TEST_USER_ID, programName, Boolean.TRUE);

	}
	
	/**
	 * Insert a test event into the solardras.event table.
	 */
	protected void setupTestEvent(Long eventId, Long programId) {
		DateTime eventDate = new DateTime(TEST_PROGRAM_DATE);
		simpleJdbcTemplate.update(
				"insert into solardras.program_event (id,creator,pro_id,notif_date,start_date,end_date,test) "
				+"values (?,?,?,?,?,?,?)", 
				eventId, TEST_USER_ID, programId,
				new java.sql.Timestamp(eventDate.minusHours(2).getMillis()),
				new java.sql.Timestamp(eventDate.getMillis()),
				new java.sql.Timestamp(eventDate.plusHours(1).getMillis()),
				Boolean.TRUE);
	}
	
	protected void setupTestOutboundMail(Long mailId, String messageId) {
		simpleJdbcTemplate.update(
				"insert into solardras.outbound_mail (id,creator,to_address,message_id,message) "
				+"values (?,?,ARRAY [?],?,?)", 
				mailId, TEST_USER_ID, "nobody@localhost.localdomain",
				TEST_OUTBOUND_MAIL_MESSAGE_ID, "Test message.");
	}	

	protected Constraint createConstraint() {
		Constraint constraint = new Constraint();
		
		List<DateTimeWindow> blackoutDates = new ArrayList<DateTimeWindow>(2);
		blackoutDates.add(new DateTimeWindow(
				new DateTime(2011, 1, 1, 0, 0, 0, 0),
				new DateTime(2011, 1, 2, 0, 0, 0, 0)));
		blackoutDates.add(new DateTimeWindow(
				new DateTime(2011, 2, 1, 0, 0, 0, 0),
				new DateTime(2011, 2, 2, 0, 0, 0, 0)));
		constraint.setBlackoutDates(blackoutDates);
		constraint.setBlackoutDatesFilter(FilterKind.RESTRICT);
		
		constraint.setEventWindowStart(new LocalTime(12, 0));
		constraint.setEventWindowEnd(new LocalTime(14, 0));
		constraint.setEventWindowFilter(FilterKind.ACCEPT);
		
		constraint.setMaxConsecutiveDays(23);
		constraint.setMaxConsecutiveDaysFilter(FilterKind.REJECT);
		
		constraint.setMaxEventDuration(new Period(4, 0, 0, 0).toStandardDuration());
		constraint.setMaxEventDurationFilter(FilterKind.ACCEPT);
		
		constraint.setNotificationWindowMax(new Period(4, 0, 0, 0).toStandardDuration());
		constraint.setNotificationWindowMin(new Period(2, 0, 0, 0).toStandardDuration());
		constraint.setNotificationWindowFilter(FilterKind.RESTRICT);
		
		List<DateTimeWindow> validDates = new ArrayList<DateTimeWindow>(2);
		validDates.add(new DateTimeWindow(
				new DateTime(2011, 1, 1, 0, 0, 0, 0),
				new DateTime(2011, 1, 2, 0, 0, 0, 0)));
		validDates.add(new DateTimeWindow(
				new DateTime(2011, 2, 1, 0, 0, 0, 0),
				new DateTime(2011, 2, 2, 0, 0, 0, 0)));
		constraint.setValidDates(validDates);
		constraint.setValidDatesFilter(FilterKind.FORCE);
		
		return constraint;
	}
	
	protected Fee createFee() {
		Fee f = new Fee();
		f.setAvailableFee(1L);
		f.setAvailablePeriod(new Period(24, 0, 0, 0));
		f.setCancelFee(2L);
		f.setCurrency("NZD");
		f.setDeliveryFee(4L);
		f.setEstablishFee(5L);
		f.setEventFee(6L);
		return f;
	}
	
}
