/* ==================================================================
 * IbatisConstraintDaoTest.java - Jun 21, 2011 8:22:49 PM
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
import static org.junit.Assert.assertNull;
import net.solarnetwork.central.dras.dao.ConstraintDao;
import net.solarnetwork.central.dras.domain.Constraint;
import net.solarnetwork.central.dras.domain.Constraint.FilterKind;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Test case for {@link ConstraintDao}.
 * 
 * @author matt
 * @version $Revision$
 */
public class IbatisConstraintDaoTest extends AbstractIbatisDaoTestSupport {

	@Autowired private ConstraintDao constraintDao;
	
	private Constraint lastConstraint;
	
	@Before
	public void setup() {
		lastConstraint = null;
	}
	
	private void validateConstraint(Constraint expected, Constraint found) {
		assertNotNull("Constraint should exist", found);
		assertEquals(expected.getId(), found.getId());
		
		assertEquals(expected.getBlackoutDates(), found.getBlackoutDates());
		assertEquals(expected.getBlackoutDatesFilter(), found.getBlackoutDatesFilter());
		assertEquals(expected.getEventWindowEnd(), found.getEventWindowEnd());
		assertEquals(expected.getEventWindowFilter(), found.getEventWindowFilter());
		assertEquals(expected.getEventWindowStart(), found.getEventWindowStart());
		assertEquals(expected.getMaxConsecutiveDays(), found.getMaxConsecutiveDays());
		assertEquals(expected.getMaxConsecutiveDaysFilter(), found.getMaxConsecutiveDaysFilter());
		assertEquals(expected.getMaxEventDuration(), found.getMaxEventDuration());
		assertEquals(expected.getMaxEventDurationFilter(), found.getMaxEventDurationFilter());
		assertEquals(expected.getNotificationWindowFilter(), found.getNotificationWindowFilter());
		assertEquals(expected.getNotificationWindowMax(), found.getNotificationWindowMax());
		assertEquals(expected.getNotificationWindowMin(), found.getNotificationWindowMin());
		assertEquals(expected.getValidDates(), found.getValidDates());
		assertEquals(expected.getValidDatesFilter(), found.getValidDatesFilter());
	}
	
	@Test
	public void insertConstraint() {
		Constraint c = createConstraint();
		
		Long id = constraintDao.store(c);
		assertNotNull(id);
		
		c.setId(id);
		lastConstraint = c;
	}

	@Test
	public void getConstraintById() {
		insertConstraint();
		Constraint entity = constraintDao.get(lastConstraint.getId());
		validateConstraint(lastConstraint, entity);
	}
	
	@Test
	public void getNonExistingConstraintById() {
		Constraint constraint = constraintDao.get(-99999L);
		assertNull(constraint);
	}
	
	@Test
	public void updateConstraint() {
		insertConstraint();
		
		assertEquals(FilterKind.ACCEPT, lastConstraint.getEventWindowFilter());
		lastConstraint.setEventWindowFilter(FilterKind.REJECT);
		
		constraintDao.store(lastConstraint);
		
		Constraint entity = constraintDao.get(lastConstraint.getId());
		validateConstraint(lastConstraint, entity);
	}

	
}
