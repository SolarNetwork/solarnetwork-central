/* ==================================================================
 * JodaLocalDateTimeTypeHandlerTests.java - 6/08/2018 12:37:48 PM
 * 
 * Copyright 2018 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.dao.mybatis.type.test;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import java.sql.Timestamp;
import org.apache.ibatis.session.SqlSession;
import org.joda.time.LocalDateTime;
import org.junit.Test;
import org.mybatis.spring.SqlSessionTemplate;
import net.solarnetwork.central.dao.mybatis.test.AbstractMyBatisDaoTestSupport;
import net.solarnetwork.central.dao.mybatis.type.JodaLocalDateTimeTypeHandler;

/**
 * Test cases for the {@link JodaLocalDateTimeTypeHandler} class.
 * 
 * @author matt
 * @version 1.0
 */
public class JodaLocalDateTimeTypeHandlerTests extends AbstractMyBatisDaoTestSupport {

	@SuppressWarnings({ "resource", "deprecation" })
	@Test
	public void testQueryParameter() {
		LocalDateTime date = new LocalDateTime(2018, 8, 1, 2, 30);
		SqlSession sqlSession = new SqlSessionTemplate(getSqlSessionFactory());
		Timestamp result = sqlSession.selectOne("test-set-local-date-time", date);
		assertThat("Local timestamp", result,
				equalTo(new Timestamp(2018 - 1900, 8 - 1, 1, 2, 30, 0, 0)));
	}

	@SuppressWarnings({ "resource" })
	@Test
	public void testResultParameter() {
		SqlSession sqlSession = new SqlSessionTemplate(getSqlSessionFactory());
		LocalDateTime result = sqlSession.selectOne("test-find-local-date-time");
		assertThat("Local timestamp", result, equalTo(new LocalDateTime(2018, 8, 1, 14, 40)));
	}

}
