/* ==================================================================
 * ObservableGenericWriteOnlyDaoTests.java - 20/03/2026 11:06:58 am
 * 
 * Copyright 2026 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.common.dao.test;

import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static org.assertj.core.api.BDDAssertions.and;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import net.solarnetwork.central.common.dao.GenericWriteOnlyDao;
import net.solarnetwork.central.common.dao.ObservableGenericWriteOnlyDao;
import net.solarnetwork.service.RemoteServiceException;

/**
 * Test cases for the {@link ObservableGenericWriteOnlyDao} class.
 * 
 * @author matt
 * @version 1.0
 */
@SuppressWarnings("static-access")
@ExtendWith(MockitoExtension.class)
public class ObservableGenericWriteOnlyDaoTests {

	@Mock
	private GenericWriteOnlyDao<Object, Object> delegateDao;

	@Mock
	private Function<Object, Future<?>> observer;

	private ObservableGenericWriteOnlyDao<Object, Object> dao;

	@BeforeEach
	public void setup() {
		dao = new ObservableGenericWriteOnlyDao<>(delegateDao, observer);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Test
	public void observe_throwsRemoteServiceException() {
		// GIVEN
		final var entity = new Object();
		final var daoResult = new Object();

		given(delegateDao.persist(same(entity))).willReturn(daoResult);

		final var obsEx = new RemoteServiceException(randomString());
		given(observer.apply(any())).willThrow(obsEx);

		// WHEN
		final Object result = dao.persist(entity);

		// THEN
		// @formatter:off
		then(observer).should(times(1)).apply(same(entity));
		
		and.then(result)
			.as("Result from delegate provided")
			.isSameAs(daoResult)
			;
		// @formatter:on
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void observe() {
		// GIVEN
		final var entity = new Object();
		final var daoResult = new Object();

		given(delegateDao.persist(same(entity))).willReturn(daoResult);

		var observerFuture = CompletableFuture.completedFuture(true);
		given(observer.apply(same(entity))).willReturn((Future) observerFuture);

		// WHEN
		final Object result = dao.persist(entity);

		// THEN
		// @formatter:off
		then(observer).should(times(1)).apply(same(entity));
		
		and.then(result)
			.as("Result from delegate provided")
			.isSameAs(daoResult)
			;
		// @formatter:on
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Test
	public void observe_futureThrowsRemoteServiceException() {
		// GIVEN
		final var entity = new Object();
		final var daoResult = new Object();

		given(delegateDao.persist(same(entity))).willReturn(daoResult);

		final var observerFuture = new CompletableFuture<Boolean>();
		observerFuture.completeExceptionally(new RemoteServiceException(randomString()));
		given(observer.apply(same(entity))).willReturn((Future) observerFuture);

		// WHEN
		final Object result = dao.persist(entity);

		// THEN
		// @formatter:off
		then(observer).should(times(1)).apply(same(entity));
		
		and.then(result)
			.as("Result from delegate provided")
			.isSameAs(daoResult)
			;
		// @formatter:on
	}

}
