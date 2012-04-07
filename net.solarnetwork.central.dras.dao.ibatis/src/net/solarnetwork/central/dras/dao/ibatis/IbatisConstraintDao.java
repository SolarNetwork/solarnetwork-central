/* ==================================================================
 * IbatisConstraintDao.java - Jun 21, 2011 5:10:33 PM
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

package net.solarnetwork.central.dras.dao.ibatis;

import java.util.ArrayList;
import java.util.List;

import net.solarnetwork.central.dras.dao.ConstraintDao;
import net.solarnetwork.central.dras.domain.Constraint;
import net.solarnetwork.central.dras.domain.DateTimeWindow;

/**
 * Ibatis implementation of {@link ConstraintDao}.
 * 
 * @author matt
 * @version $Revision$
 */
public class IbatisConstraintDao extends DrasIbatisGenericDaoSupport<Constraint>
implements ConstraintDao {

	/**
	 * Default constructor.
	 */
	public IbatisConstraintDao() {
		super(Constraint.class);
	}
	
	private void handleWindowRelations(Long parentId, Constraint datum) {
		List<ConstraintWindow> windows = new ArrayList<ConstraintWindow>();
		if ( datum.getBlackoutDates() != null ) {
			for ( DateTimeWindow w : datum.getBlackoutDates() ) {
				windows.add(new ConstraintWindow(ConstraintWindowKind.BLACKOUT, w));
			}
		}
		if ( datum.getValidDates() != null ) {
			for ( DateTimeWindow w : datum.getValidDates() ) {
				windows.add(new ConstraintWindow(ConstraintWindowKind.VALID, w));
			}
		}
		handleRelation(parentId, windows, ConstraintWindow.class, null);
	}

	@Override
	protected Long handleUpdate(Constraint datum) {
		Long result = super.handleUpdate(datum);
		handleWindowRelations(result, datum);
		return result;
	}

	@Override
	protected Long handleInsert(Constraint datum) {
		Long result = super.handleInsert(datum);
		handleWindowRelations(result, datum);
		return result;
	}
	
	/** The list of DateTypeWindow objects supported. */
	public enum ConstraintWindowKind {
		VALID,
		BLACKOUT
	}
	
	/**
	 * Extension of {@link DateTimeWindow} used to differentiate between 
	 * blackout and valid list objects during insert/update operations.
	 */
	public static class ConstraintWindow extends DateTimeWindow {
		
		private static final long serialVersionUID = 3468192610949477204L;

		private ConstraintWindowKind kind;
		
		private ConstraintWindow(ConstraintWindowKind kind, DateTimeWindow window) {
			this.kind = kind;
			setStartDate(window.getStartDate());
			setEndDate(window.getEndDate());
		}
		
		public ConstraintWindowKind getKind() {
			return kind;
		}
		
	}
	
}
