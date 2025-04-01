/* ==================================================================
 * BasicUserAuthTokenFilter.java - 2/04/2025 9:02:30 am
 * 
 * Copyright 2025 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.dao;

import java.util.Arrays;
import java.util.Objects;
import net.solarnetwork.central.common.dao.BasicCoreCriteria;
import net.solarnetwork.central.common.dao.IdentifierCriteria;
import net.solarnetwork.dao.PaginationCriteria;

/**
 * Basic implementation of {@link UserAuthTokenFilter}.
 * 
 * @author matt
 * @version 1.0
 */
public class BasicUserAuthTokenFilter extends BasicCoreCriteria implements UserAuthTokenFilter {

	private Boolean active;
	private String[] identifiers;
	private String[] tokenTypes;

	/**
	 * Constructor.
	 */
	public BasicUserAuthTokenFilter() {
		super();
	}

	/**
	 * Copy constructor.
	 * 
	 * @param criteria
	 *        the criteria to copy
	 */
	public BasicUserAuthTokenFilter(PaginationCriteria criteria) {
		super(criteria);
	}

	/**
	 * Create a new filter instance for just the first identifier in a given
	 * filter.
	 * 
	 * @param filter
	 *        the filter ({@code null} allowed)
	 * @return the new filter instance, never {@code null}
	 */
	public static BasicUserAuthTokenFilter filterForIdentifier(UserAuthTokenFilter filter) {
		var result = new BasicUserAuthTokenFilter();
		if ( filter != null ) {
			result.setIdentifier(filter.getIdentifier());
		}
		return result;
	}

	@Override
	public BasicUserAuthTokenFilter clone() {
		return (BasicUserAuthTokenFilter) super.clone();
	}

	@Override
	public void copyFrom(PaginationCriteria criteria) {
		super.copyFrom(criteria);
		if ( criteria == null ) {
			return;
		}
		if ( criteria instanceof BasicUserAuthTokenFilter c ) {
			setActive(c.getActive());
			setIdentifiers(c.getIdentifiers());
			setTokenTypes(c.getTokenTypes());
		} else {
			if ( criteria instanceof IdentifierCriteria c ) {
				setIdentifiers(c.getIdentifiers());
			}
			if ( criteria instanceof UserAuthTokenFilter c ) {
				setTokenTypes(c.getTokenTypes());
			}
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Arrays.hashCode(identifiers);
		result = prime * result + Arrays.hashCode(tokenTypes);
		result = prime * result + Objects.hash(active);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( !super.equals(obj) ) {
			return false;
		}
		if ( !(obj instanceof BasicUserAuthTokenFilter) ) {
			return false;
		}
		BasicUserAuthTokenFilter other = (BasicUserAuthTokenFilter) obj;
		return Objects.equals(active, other.active) && Arrays.equals(identifiers, other.identifiers)
				&& Arrays.equals(tokenTypes, other.tokenTypes);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("BasicUserAuthTokenFilter{userIds=");
		builder.append(Arrays.toString(getUserIds()));
		if ( active != null ) {
			builder.append(", active=");
			builder.append(active);
		}
		if ( identifiers != null ) {
			builder.append(", identifiers=");
			builder.append(Arrays.toString(identifiers));
		}
		if ( tokenTypes != null ) {
			builder.append(", tokenTypes=");
			builder.append(Arrays.toString(tokenTypes));
		}
		builder.append("}");
		return builder.toString();
	}

	@Override
	public Boolean getActive() {
		return active;
	}

	/**
	 * Set the active criteria.
	 * 
	 * @param active
	 *        the criteria value to set
	 */
	public void setActive(Boolean active) {
		this.active = active;
	}

	@Override
	public String[] getIdentifiers() {
		return identifiers;
	}

	/**
	 * Set the identifier criteria.
	 * 
	 * @param identifiers
	 *        the identifiers to set
	 */
	public void setIdentifiers(String[] identifiers) {
		this.identifiers = identifiers;
	}

	/**
	 * Set a single identifier criteria.
	 * 
	 * @param identifier
	 *        the identifier to set
	 */
	public void setIdentifier(String identifier) {
		setIdentifiers(identifier != null ? new String[] { identifier } : null);
	}

	@Override
	public String[] getTokenTypes() {
		return tokenTypes;
	}

	/**
	 * Set the token type criteria.
	 * 
	 * @param types
	 *        the criteria to set
	 */
	public void setTokenTypes(String[] types) {
		this.tokenTypes = types;
	}

	/**
	 * Set a single token type criteria.
	 * 
	 * @param type
	 *        the type to set
	 */
	public void setTokenType(String type) {
		setTokenTypes(type != null ? new String[] { type } : null);
	}

}
