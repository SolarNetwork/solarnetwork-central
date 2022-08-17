/* ==================================================================
 * SelectAuthRoleInfoTests.java - 17/08/2022 11:27:22 am
 * 
 * Copyright 2022 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.oscp.dao.jdbc.sql.test;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.oscp.dao.jdbc.sql.SelectAuthRoleInfo;
import net.solarnetwork.central.oscp.domain.OscpRole;

/**
 * FIXME
 * 
 * <p>
 * TODO
 * </p>
 * 
 * @author matt
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
public class SelectAuthRoleInfoTests {

	@Mock
	private Connection con;

	@Mock
	private PreparedStatement stmt;

	@Captor
	private ArgumentCaptor<String> sqlCaptor;

	private void givenPrepStatement() throws SQLException {
		given(con.prepareStatement(any())).willReturn(stmt);
	}

	private void thenPrepStatement(PreparedStatement result, UserLongCompositePK authId)
			throws SQLException {
		then(result).should().setObject(1, authId.getUserId(), Types.BIGINT);
		then(result).should().setObject(2, authId.getEntityId(), Types.BIGINT);
	}

	@Test
	public void flexibilityProvider_sql() {
		// GIVEN
		UserLongCompositePK authId = new UserLongCompositePK(randomUUID().getMostSignificantBits(),
				randomUUID().getMostSignificantBits());

		// WHEN
		String sql = new SelectAuthRoleInfo(OscpRole.FlexibilityProvider, authId).getSql();

		// THEN
		assertThat("SQL generated", sql, is(equalTo(
				"SELECT user_id, entity_id, role_alias FROM solaroscp.conf_id_for_fp_id(?, ?)")));
	}

	@Test
	public void flexibilityProvider_prep() throws SQLException {
		// GIVEN
		UserLongCompositePK authId = new UserLongCompositePK(randomUUID().getMostSignificantBits(),
				randomUUID().getMostSignificantBits());
		givenPrepStatement();

		// WHEN
		PreparedStatement result = new SelectAuthRoleInfo(OscpRole.FlexibilityProvider, authId)
				.createPreparedStatement(con);

		// THEN
		then(con).should().prepareStatement(sqlCaptor.capture());
		assertThat("Generated SQL", sqlCaptor.getValue(), is(equalTo(
				"SELECT user_id, entity_id, role_alias FROM solaroscp.conf_id_for_fp_id(?, ?)")));
		assertThat("Connection statement returned", result, sameInstance(stmt));
		thenPrepStatement(result, authId);
	}

}
