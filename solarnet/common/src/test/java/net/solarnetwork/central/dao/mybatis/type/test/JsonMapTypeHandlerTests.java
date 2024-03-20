/* ==================================================================
 * JsonMapTypeHandlerTests.java - 5/06/2020 10:56:43 am
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
import static org.hamcrest.Matchers.equalTo;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.ibatis.session.SqlSession;
import org.junit.Test;
import org.mybatis.spring.SqlSessionTemplate;
import net.solarnetwork.central.dao.mybatis.test.AbstractMyBatisDaoTestSupport;

/**
 * Test cases for the
 * {@link net.solarnetwork.central.dao.mybatis.type.JsonMapTypeHandler} class.
 * 
 * @author matt
 * @version 2.0
 */
public class JsonMapTypeHandlerTests extends AbstractMyBatisDaoTestSupport {

	@Test
	public void testQueryParameter() {
		// GIVEN
		Map<String, Object> m = new LinkedHashMap<>(2);
		m.put("foo", "bar");
		JsonMapBean bean = new JsonMapBean();
		bean.setMap(m);

		// WHEN
		String result = null;
		try (SqlSession sqlSession = new SqlSessionTemplate(getSqlSessionFactory())) {
			result = sqlSession.selectOne("test-set-json-map", bean);
		}

		// THEN
		assertThat("Map set as query parameter as JSON object", result, equalTo("{\"foo\":\"bar\"}"));
	}

	@Test
	public void testResultParameter() {
		// WHEN
		JsonMapBean result = null;
		try (SqlSession sqlSession = new SqlSessionTemplate(getSqlSessionFactory())) {
			result = sqlSession.selectOne("test-get-json-map");
		}

		// THEN
		assertThat("Result map", result.getMap(), equalTo(Collections.singletonMap("foo", "bar")));
	}

}
