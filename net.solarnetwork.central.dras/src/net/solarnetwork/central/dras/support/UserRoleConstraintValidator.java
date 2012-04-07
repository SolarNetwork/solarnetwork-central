/* ==================================================================
 * UserRoleConstraintValidator.java - Jun 11, 2011 2:27:40 PM
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

package net.solarnetwork.central.dras.support;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import net.solarnetwork.central.dras.biz.UserBiz;
import net.solarnetwork.central.dras.dao.UserDao;
import net.solarnetwork.central.dras.domain.UserRole;

import org.springframework.beans.factory.annotation.Autowired;

/**
 * Validate a set of role IDs against the available roles returned by
 * {@link UserDao#getAllUserRoles()}.
 * 
 * @author matt
 * @version $Revision$
 */
public class UserRoleConstraintValidator implements ConstraintValidator<ValidUserRole, Collection<String>> {

	@Autowired private UserBiz userBiz;
	private Set<String> validRoles;
		
	@Override
	public void initialize(ValidUserRole constraint) {
		if ( validRoles == null ) {
			validRoles = new LinkedHashSet<String>(10);
			Set<UserRole> availableRoles = userBiz.getAllUserRoles();
			for ( UserRole role : availableRoles ) {
				validRoles.add(role.getId());
			}
		}
	}

	@Override
	public boolean isValid(Collection<String> roles, ConstraintValidatorContext context) {
		if ( roles == null ) {
			return true;
		}
		for ( String role : roles ) {
			if ( !validRoles.contains(role) ) {
				context.buildConstraintViolationWithTemplate("invalid role").addConstraintViolation();
			}
		}
		return false;
	}

}
