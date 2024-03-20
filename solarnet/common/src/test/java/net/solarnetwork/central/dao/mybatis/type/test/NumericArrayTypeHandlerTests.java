/* ==================================================================
 * NumericArrayTypeHandlerTests.java - 23/07/2020 2:59:39 PM
 * 
 * Copyright 2020 SolarNetwork.net Dev Team
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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayContaining;
import java.math.BigDecimal;
import org.apache.ibatis.session.SqlSession;
import org.junit.Test;
import org.mybatis.spring.SqlSessionTemplate;
import net.solarnetwork.central.dao.mybatis.test.AbstractMyBatisDaoTestSupport;
import net.solarnetwork.central.dao.mybatis.type.NumericArrayTypeHandler;

/**
 * Test cases for the {@link NumericArrayTypeHandler} class.
 * 
 * @author matt
 * @version 2.0
 */
public class NumericArrayTypeHandlerTests extends AbstractMyBatisDaoTestSupport {

	@Test
	public void testResultParameter() {
		// WHEN
		SqlSession sqlSession = new SqlSessionTemplate(getSqlSessionFactory());
		BeanWithArrays result = sqlSession.selectOne("test-get-numeric-array");

		// THEN
		// @formatter:off
		assertThat("Result array", result.getBigDecimals(), arrayContaining(
				new BigDecimal("1.234"),
				new BigDecimal("2.345"),
				new BigDecimal("3.456")
				));
		// @formatter:on
	}

	@Test
	public void testQueryParameter() {
		// GIVEN
		// @formatter:off
		BigDecimal[] data = new BigDecimal[] {
				new BigDecimal("1.234"),
				new BigDecimal("2.345"),
				new BigDecimal("3.456"),
		};
		// @formatter:on

		// WHEN
		SqlSession sqlSession = new SqlSessionTemplate(getSqlSessionFactory());
		BeanWithArrays bean = new BeanWithArrays();
		bean.setBigDecimals(data);
		BeanWithArrays result = sqlSession.selectOne("test-set-numeric-array", bean);

		// THEN
		assertThat("Result array", result.getBigDecimals(), arrayContaining(data));
	}
}
