/* ==================================================================
 * MyBatisGenericDaoSupport.java - 24/02/2020 5:39:15 pm
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

package net.solarnetwork.central.dao.mybatis.support;

import static java.util.Collections.singletonMap;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import org.apache.ibatis.executor.BatchResult;
import org.mybatis.spring.support.SqlSessionDaoSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import net.solarnetwork.dao.Entity;
import net.solarnetwork.dao.GenericDao;
import net.solarnetwork.domain.SortDescriptor;

/**
 * Base implementation of {@link GenericDao} using MyBatis via
 * {@link SqlSessionDaoSupport}.
 * 
 * @author matt
 * @version 1.1
 * @since 2.1
 */
public abstract class BaseMyBatisGenericDaoSupport<T extends Entity<K>, K> extends BaseMyBatisDao
		implements GenericDao<T, K> {

	/** Error code to report that a named query was not found. */
	public static final int ERROR_CODE_INVALID_QUERY = 1101;

	/** The query name used for {@link GenericDao#get(Object)}. */
	public static final String QUERY_FOR_ID = "get-%s-for-id";

	/** The query name used for {@link GenericDao#getAll(List)}. */
	public static final String QUERY_FOR_ALL = "findall-%s";

	/** The query name used for inserts in {@link GenericDao#save(Object)}. */
	public static final String INSERT_OBJECT = "insert-%s";

	/** The query name used for updates in {@link GenericDao#save(Object)}. */
	public static final String UPDATE_OBJECT = "update-%s";

	/** The query name used for updates in {@link GenericDao#delete(Object)}. */
	public static final String DELETE_OBJECT = "delete-%s";

	/** The query property for any custom sort descriptors that are provided. */
	public static final String SORT_DESCRIPTORS_PROPERTY = "SortDescriptors";

	/** The query property for a filter (search criteria) object. */
	public static final String FILTER_PROPERTY = "filter";

	/** A class-level logger. */
	protected final Logger log = LoggerFactory.getLogger(getClass());

	private final Class<? extends T> objectType;
	private final Class<? extends K> keyType;
	private String queryForId;
	private String queryForAll;
	private String insert;
	private String update;
	private String delete;

	/**
	 * Constructor.
	 * 
	 * @param objectType
	 *        the entity type
	 * @param keyType
	 *        the key type
	 * @throws IllegalArgumentException
	 *         if any parameter is {@literal null}
	 */
	public BaseMyBatisGenericDaoSupport(Class<? extends T> objectType, Class<? extends K> keyType) {
		super();
		if ( objectType == null ) {
			throw new IllegalArgumentException("The objectType parameter must not be null.");
		}
		if ( keyType == null ) {
			throw new IllegalArgumentException("The keyType parameter must not be null.");
		}
		this.objectType = objectType;
		this.keyType = keyType;

		final String domainName = objectType.getSimpleName();
		this.queryForId = String.format(QUERY_FOR_ID, domainName);
		this.queryForAll = String.format(QUERY_FOR_ALL, domainName);
		this.insert = String.format(INSERT_OBJECT, domainName);
		this.update = String.format(UPDATE_OBJECT, domainName);
		this.delete = String.format(DELETE_OBJECT, domainName);
	}

	/**
	 * Get the main domain object type.
	 * 
	 * @return the object type, never {@literal null}
	 */
	@Override
	public Class<? extends T> getObjectType() {
		return objectType;
	}

	/**
	 * Get the primary key type.
	 * 
	 * @return the key type, never {@literal null}
	 */
	public Class<? extends K> getKeyType() {
		return keyType;
	}

	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	@Override
	public T get(K id) {
		return getSqlSession().selectOne(this.queryForId, id);
	}

	// Propagation.REQUIRED for server-side cursors
	@Transactional(readOnly = true, propagation = Propagation.REQUIRED)
	@Override
	public Collection<T> getAll(List<SortDescriptor> sorts) {
		List<T> results;
		if ( sorts != null && sorts.size() > 0 ) {
			results = getSqlSession().selectList(this.queryForAll,
					singletonMap(SORT_DESCRIPTORS_PROPERTY, sorts));
		} else {
			results = getSqlSession().selectList(this.queryForAll);
		}
		return results;
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public K save(T entity) {
		if ( isAssignedPrimaryKeys() ) {
			return saveWithAssignedPrimaryKey(entity);
		}
		if ( entity.getId() != null ) {
			return handleUpdate(entity);
		}
		preprocessInsert(entity);
		return handleInsert(entity);
	}

	/**
	 * Save an entity that uses an assigned primary key.
	 * 
	 * <p>
	 * This method is called by {@code #save(Entity)} when
	 * {@link #isAssignedPrimaryKeys()} returns {@literal true}.
	 * </p>
	 * 
	 * @param entity
	 *        the entity to save
	 * @return the primary key
	 */
	protected K saveWithAssignedPrimaryKey(T entity) {
		// try update, then insert if that fails
		int count = getLastUpdateCount(getSqlSession().update(getUpdate(), entity));
		if ( count == 0 ) {
			preprocessInsert(entity);
			getSqlSession().insert(getInsert(), entity);
		}
		return entity.getId();
	}

	/**
	 * Get the last updated count, supporting batch operations.
	 * 
	 * @param count
	 *        the last returned count from calling {@code SqlSession#update()}
	 * @return the count, extracted from batch updates if necessary
	 */
	protected int getLastUpdateCount(int count) {
		if ( count < 0 ) {
			List<BatchResult> updates = getSqlSession().flushStatements();
			if ( updates != null && !updates.isEmpty() ) {
				BatchResult br = updates.get(updates.size() - 1);
				int[] counts = br.getUpdateCounts();
				if ( counts != null && counts.length > 0 ) {
					count = counts[counts.length - 1];
				}
			}
		}
		return count;
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void delete(T entity) {
		if ( entity.getId() == null ) {
			return;
		}
		int result = getSqlSession().delete(this.delete, entity.getId());
		if ( result < 1 ) {
			log.warn("Delete [{}] did not affect any rows.", entity);
		} else if ( log.isInfoEnabled() ) {
			log.debug("Deleted [{}]", entity);
		}
	}

	/**
	 * Process a new unsaved entity for persisting.
	 * 
	 * <p>
	 * This implementation will set the value of the {@code created} bean
	 * property of the datum instance to the current time if
	 * {@link T#getCreated()} is null. Extending classes may want to extend or
	 * modify this behavior.
	 * </p>
	 * 
	 * @param entity
	 *        the entity to be persisted
	 */
	protected void preprocessInsert(T entity) {
		if ( entity.getCreated() == null ) {
			BeanWrapper wrapper = PropertyAccessorFactory.forBeanPropertyAccess(entity);
			wrapper.setPropertyValue("created", Instant.now());
		}
	}

	/**
	 * Process the update of a persisted entity.
	 * 
	 * <p>
	 * This implementation merely calls
	 * {@link SqlMapClientTemplate#update(String, Object)} using the
	 * {@link #getUpdate()} SqlMap.
	 * </p>
	 * 
	 * @param entity
	 *        the datum to update
	 * @return {@link T#getId()}
	 */
	protected K handleUpdate(T entity) {
		getSqlSession().update(this.update, entity);
		return entity.getId();
	}

	/**
	 * Process the insert of a persisted entity.
	 * 
	 * <p>
	 * This implementation calls
	 * {@link SqlMapClientTemplate#insert(String, Object)} using the
	 * {@link #getInsert()} SqlMap.
	 * </p>
	 * 
	 * @param entity
	 *        the datum to insert
	 * @return the result of the insert statement
	 */
	protected K handleInsert(T entity) {
		int updated = getSqlSession().insert(this.insert, entity);
		log.debug("Insert of {} updated {} rows", entity, updated);
		return entity.getId();
	}

	/**
	 * Tell if entities used assigned primary keys.
	 * 
	 * <p>
	 * This method returns {@literal false}. Extending classes can override to
	 * change the setting.
	 * </p>
	 * 
	 * @return {@literal true} if entities use assigned primary keys, or
	 *         {@literal false} if keys are generated by the database
	 */
	protected boolean isAssignedPrimaryKeys() {
		return false;
	}

	/**
	 * Get the query name to query by primary key.
	 * 
	 * @return the query name; defaults to {@link #QUERY_FOR_ID}
	 */
	public String getQueryForId() {
		return queryForId;
	}

	/**
	 * Set the query name to query by primary key.
	 * 
	 * @param queryForId
	 *        the query name to set
	 */
	public void setQueryForId(String queryForId) {
		this.queryForId = queryForId;
	}

	/**
	 * Get the query name to query for all entities.
	 * 
	 * @return the query name; defaults to {@link #QUERY_FOR_ALL}
	 */
	public String getQueryForAll() {
		return queryForAll;
	}

	/**
	 * Set the query name to query for all entities.
	 * 
	 * @param queryForAll
	 *        the query name to set
	 */
	public void setQueryForAll(String queryForAll) {
		this.queryForAll = queryForAll;
	}

	/**
	 * Get the query name to insert an entity.
	 * 
	 * @return the query name; defaults to {@link #INSERT_OBJECT}
	 */
	public String getInsert() {
		return insert;
	}

	/**
	 * Set the query name to insert an entity.
	 * 
	 * @param insert
	 *        the query name to set
	 */
	public void setInsert(String insert) {
		this.insert = insert;
	}

	/**
	 * Set the query name to update an entity.
	 * 
	 * @param update
	 *        the query name to set
	 */
	public void setUpdate(String update) {
		this.update = update;
	}

	/**
	 * Get the query name to update an entity.
	 * 
	 * @return the query name; defaults to {@link #UPDATE_OBJECT}
	 */
	public String getUpdate() {
		return update;
	}

	/**
	 * Get the query name to delete an entity.
	 * 
	 * @return the query name; defaults to {@link #DELETE_OBJECT}
	 */
	public String getDelete() {
		return delete;
	}

	/**
	 * Set the query name to delete an entity.
	 * 
	 * @param delete
	 *        the delete to set
	 */
	public void setDelete(String delete) {
		this.delete = delete;
	}

}
