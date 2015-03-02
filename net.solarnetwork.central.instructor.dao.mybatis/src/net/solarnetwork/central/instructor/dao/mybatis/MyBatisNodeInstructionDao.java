/* ==================================================================
 * MyBatisNodeInstructionDao.java - Nov 12, 2014 6:33:35 AM
 * 
 * Copyright 2007-2014 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.instructor.dao.mybatis;

import net.solarnetwork.central.dao.mybatis.support.BaseMyBatisFilterableDao;
import net.solarnetwork.central.domain.EntityMatch;
import net.solarnetwork.central.instructor.dao.NodeInstructionDao;
import net.solarnetwork.central.instructor.domain.InstructionFilter;
import net.solarnetwork.central.instructor.domain.InstructionParameter;
import net.solarnetwork.central.instructor.domain.NodeInstruction;

/**
 * MyBatis implementation of {@link NodeInstructionDao}.
 * 
 * @author matt
 * @version 1.0
 */
public class MyBatisNodeInstructionDao extends
		BaseMyBatisFilterableDao<NodeInstruction, EntityMatch, InstructionFilter, Long> implements
		NodeInstructionDao {

	/**
	 * Default constructor.
	 */
	public MyBatisNodeInstructionDao() {
		super(NodeInstruction.class, Long.class, EntityMatch.class);
	}

	@Override
	protected Long handleInsert(NodeInstruction datum) {
		Long result = super.handleInsert(datum);
		handleRelation(result, datum.getParameters(), InstructionParameter.class, null);
		return result;
	}
}
