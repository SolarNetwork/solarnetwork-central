/* ==================================================================
 * UserSecretBizTests.java - 22/03/2025 9:30:30 am
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

package net.solarnetwork.central.user.biz.dao.test;

import static java.time.ZoneOffset.UTC;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.threeten.extra.MutableClock;
import net.solarnetwork.central.user.biz.UserSecretBiz;
import net.solarnetwork.central.user.dao.UserSecretEntityDao;
import net.solarnetwork.central.user.domain.UserSecretEntity;

/**
 * Test cases for the {@link UserSecretBiz} class.
 * 
 * @author matt
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
public class UserSecretBizTests {

	@Mock
	private UserSecretEntityDao secretDao;

	@Captor
	private ArgumentCaptor<UserSecretEntity> secretEntityCaptor;

	private MutableClock clock = MutableClock.of(Instant.now().truncatedTo(ChronoUnit.DAYS), UTC);

	@BeforeEach
	public void setup() {

	}

}
