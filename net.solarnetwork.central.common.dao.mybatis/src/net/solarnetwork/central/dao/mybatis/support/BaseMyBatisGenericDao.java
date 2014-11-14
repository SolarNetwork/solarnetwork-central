/* ==================================================================
 * BaseMyBatisGenericDao.java - Nov 10, 2014 7:04:47 AM
 * 
 * Copyright 2007-2014 SolarNetwork.net Dev Team
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

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.solarnetwork.central.ValidationException;
import net.solarnetwork.central.dao.GenericDao;
import net.solarnetwork.central.domain.Entity;
import net.solarnetwork.central.domain.Identity;
import net.solarnetwork.central.domain.SortDescriptor;
import org.joda.time.DateTime;
import org.mybatis.spring.support.SqlSessionDaoSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.context.MessageSource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.Errors;

/**
 * Base implementation of {@link GenericDao} using MyBatis via
 * {@link SqlSessionDaoSupport}.
 * 
 * <p>
 * The default configuration of this class allows implementations to be used
 * with minimal configuration, by following some simple naming conventions on
 * MyBatis query names. The {@code queryForId}, {@code queryForAll},
 * {@code insert}, {@code update}, and {@code delete} query names all follow a
 * pattern using the name of the domain class as a parameter.
 * </p>
 * 
 * <p>
 * For example, if the domain class managed by this DAO is
 * {@code some.package.Foo} then the default query names used by this class will
 * be:
 * </p>
 * 
 * <ul>
 * <li>get-Foo-by-id</li>
 * <li>findall-Foo</li>
 * <li>insert-Foo</li>
 * <li>update-Foo</li>
 * <li>delete-Foo</li>
 * </ul>
 * 
 * <p>
 * The {@link #handleRelation(Long, List, Class, Map)} can be used by extending
 * classes to manage a <em>to-many</em> type relationship. The
 * {@code relationQueryForParent}, {@code relationInsert},
 * {@code relationUpdate}, and {@code relationDelete} query names follow the
 * same pattern as above, with the relation domain class name appended at the
 * end. For example, if the relation domain class was {@code some.package.Bar}
 * then the default relationship query names used by this class will be:
 * </p>
 * 
 * <ul>
 * <li>findall-Foo-Bar</li>
 * <li>insert-Foo-Bar</li>
 * <li>update-Foo-Bar</li>
 * <li>delete-Foo-Bar</li>
 * </ul>
 * 
 * <p>
 * The {@link #ID_PROPERTY}, {@link #INDEX_PROPERTY}, and
 * {@link #BEAN_OBJECT_PROPERTY} keys will be passed to the query as needed to
 * manage the related entity list.
 * </p>
 * 
 * <p>
 * The configurable properties of this class are:
 * </p>
 * 
 * <dl class="class-properties">
 * <dt>domainClass</dt>
 * <dd>The implementation Class managed by this DAO.</dd>
 * 
 * <dt>queryForId</dt>
 * <dd>The name of the MyBatis SQL query to load an entity based on its primary
 * key (a Long). Defaults to <code>get-<em>DomainClass</em>-for-id</code> where
 * <em>DatumClass</em> is the simple name of the configured {@code domainClass}.
 * </dd>
 * 
 * <dt>queryForAll</dt>
 * <dd>The name of the MyBatis SQL query to return a list of entities,
 * supporting a custom sort order. Defaults to
 * <code>findall-<em>DomainClass</em></code> where <em>DatumClass</em> is the
 * simple name of the configured {@code domainClass}.</dd>
 * 
 * <dt>insert</dt>
 * <dd>The name of the MyBatis SQL query to insert a new entity into the
 * database. Defaults to <code>insert-<em>DomainClass</em></code> where
 * <em>DatumClass</em> is the simple name of the configured {@code domainClass}.
 * </dd>
 * 
 * <dt>update</dt>
 * <dd>The name of the MyBatis SQL query to update an existing entity in the
 * database. Defaults to <code>update-<em>DomainClass</em></code> where
 * <em>DatumClass</em> is the simple name of the configured {@code domainClass}.
 * </dd>
 * 
 * <dt>delete</dt>
 * <dd>The name of the MyBatis SQL query to delete an existing entity from the
 * database. Defaults to <code>delete-<em>DomainClass</em></code> where
 * <em>DatumClass</em> is the simple name of the configured {@code domainClass}.
 * </dd>
 * 
 * </dl>
 * 
 * @param <T>
 *        The entity type this DAO supports.
 * @param <PK>
 *        The primary key type this DAO supports.
 * @author matt
 * @version 1.0
 */
public abstract class BaseMyBatisGenericDao<T extends Entity<PK>, PK extends Serializable> extends
		BaseMyBatisDao implements GenericDao<T, PK> {

	/** Error code to report that a named query was not found. */
	public static final int ERROR_CODE_INVALID_QUERY = 1101;

	/** The query name used for {@link #get(Long)}. */
	public static final String QUERY_FOR_ID = "get-%s-for-id";

	/** The query name used for {@link #getAll(List)}. */
	public static final String QUERY_FOR_ALL = "findall-%s";

	/** The query name used for inserts in {@link #store(Object)}. */
	public static final String INSERT_OBJECT = "insert-%s";

	/** The query name used for updates in {@link #store(Object)}. */
	public static final String UPDATE_OBJECT = "update-%s";

	/** The query name used for updates in {@link #delete(Object)}. */
	public static final String DELETE_OBJECT = "delete-%s";

	/** The query property for any custom sort descriptors that are provided. */
	public static final String SORT_DESCRIPTORS_PROPERTY = "SortDescriptors";

	/** A query property for a general ID value. */
	public static final String ID_PROPERTY = "id";

	/** A query property for an array index value. */
	public static final String INDEX_PROPERTY = "index";

	/** A query property for a JavaBean style object. */
	public static final String BEAN_OBJECT_PROPERTY = "obj";

	/** A query property for a general date value. */
	public static final String DATE_PROPERTY = "date";

	public static final String RELATION_LIST_QUERY_FOR_PARENT = "findall-%s-";
	public static final String RELATION_OBJ_QUERY_FOR_PARENT = "get-%s-";
	public static final String RELATION_INSERT = "insert-%s-";
	public static final String RELATION_UPDATE = "update-%s-";
	public static final String RELATION_DELETE = "delete-%s-";

	public static final String CHILD_INSERT = "insert-";
	public static final String CHILD_UPDATE = "update-";
	public static final String CHILD_DELETE = "delete-";

	/** A class-level logger. */
	protected final Logger log = LoggerFactory.getLogger(getClass());

	private final Class<? extends T> domainClass;
	private final Class<? extends PK> pkClass;
	private String queryForId;
	private String queryForAll;
	private String insert;
	private String update;
	private String delete;
	private String relationQueryForParent;
	private String relationObjectQueryForParent;
	private String relationInsert;
	private String relationUpdate;
	private String relationDelete;
	private String childInsert;
	private String childUpdate;
	private String childDelete;

	private MessageSource messageSource;

	/**
	 * Constructor.
	 * 
	 * @param domainClass
	 *        the domain class
	 * @param pkClass
	 *        the primary key class
	 */
	public BaseMyBatisGenericDao(Class<? extends T> domainClass, Class<? extends PK> pkClass) {
		super();

		final String domainName = domainClass.getSimpleName();

		this.domainClass = domainClass;
		this.pkClass = pkClass;
		this.queryForId = String.format(QUERY_FOR_ID, domainName);
		this.queryForAll = String.format(QUERY_FOR_ALL, domainName);
		this.insert = String.format(INSERT_OBJECT, domainName);
		this.update = String.format(UPDATE_OBJECT, domainName);
		this.delete = String.format(DELETE_OBJECT, domainName);

		this.relationQueryForParent = String.format(RELATION_LIST_QUERY_FOR_PARENT, domainName);
		this.relationObjectQueryForParent = String.format(RELATION_OBJ_QUERY_FOR_PARENT, domainName);
		this.relationInsert = String.format(RELATION_INSERT, domainName);
		this.relationUpdate = String.format(RELATION_UPDATE, domainName);
		this.relationDelete = String.format(RELATION_DELETE, domainName);

		this.childInsert = CHILD_INSERT;
		this.childUpdate = CHILD_UPDATE;
		this.childDelete = CHILD_DELETE;
	}

	@Override
	public Class<? extends T> getObjectType() {
		return this.domainClass;
	}

	/**
	 * Get the primary key type used by this DAO.
	 * 
	 * @return the primary key type
	 */
	public Class<? extends PK> getPrimaryKeyType() {
		return this.pkClass;
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public T get(PK id) {
		return getSqlSession().selectOne(this.queryForId, id);
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public List<T> getAll(List<SortDescriptor> sortDescriptors) {
		List<T> results;
		if ( sortDescriptors != null && sortDescriptors.size() > 0 ) {
			Map<String, Object> params = new HashMap<String, Object>(1);
			params.put(SORT_DESCRIPTORS_PROPERTY, sortDescriptors);
			results = getSqlSession().selectList(this.queryForAll, params);
		} else {
			results = getSqlSession().selectList(this.queryForAll);
		}
		return results;
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public PK store(T datum) {
		if ( datum.getId() != null ) {
			return handleUpdate(datum);
		}
		preprocessInsert(datum);
		return handleInsert(datum);
	}

	/**
	 * Supporting method for handling the {@link #store(Entity)} method for
	 * entities that use assigned primary keys, where the default logic of
	 * handling insert versus update will not work.
	 * 
	 * <p>
	 * This implementation attempts to update the given entity first. If that
	 * does not actually update any rows, {@link #preprocessInsert(Entity)} is
	 * called, followed by an insert.
	 * </p>
	 * 
	 * @param datum
	 *        the datum to store
	 * @return the primary key
	 */
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	protected PK handleAssignedPrimaryKeyStore(T datum) {
		// try update, then insert if that fails
		if ( getSqlSession().update(getUpdate(), datum) == 0 ) {
			preprocessInsert(datum);
			getSqlSession().insert(getInsert(), datum);
		}
		return datum.getId();
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
	 * @param datum
	 *        the entity to be persisted
	 */
	protected void preprocessInsert(T datum) {
		if ( datum.getCreated() == null ) {
			BeanWrapper wrapper = PropertyAccessorFactory.forBeanPropertyAccess(datum);
			wrapper.setPropertyValue("created", new DateTime());
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
	 * @param datum
	 *        the datum to update
	 * @return {@link T#getId()}
	 */
	protected PK handleUpdate(T datum) {
		getSqlSession().update(this.update, datum);
		return datum.getId();
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
	 * @param datum
	 *        the datum to insert
	 * @return the result of the insert statement
	 */
	protected PK handleInsert(T datum) {
		int updated = getSqlSession().insert(this.insert, datum);
		log.debug("Insert of {} updated {} rows", datum, updated);
		return datum.getId();
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void delete(T domainObject) {
		if ( domainObject.getId() == null ) {
			return;
		}
		int result = getSqlSession().delete(this.delete, domainObject.getId());
		if ( result < 1 ) {
			log.warn("Delete [" + domainObject + "] did not affect any rows");
		} else if ( log.isInfoEnabled() ) {
			log.info("Deleted [" + domainObject + ']');
		}
	}

	/**
	 * Insert, update, and delete domain object list related to the entity
	 * managed by this DAO.
	 * 
	 * <p>
	 * This method will use the {@code relationQueryForParent},
	 * {@code relationInsert}, {@code relationUpdate}, and
	 * {@code relationDelete} query names configured on this class to persist a
	 * set of related objects to a single parent entity. The related objects are
	 * <em>not</em> assumed to have surrogate primary keys.
	 * <p>
	 * 
	 * <p>
	 * This method will pass the following query properties:
	 * </p>
	 * 
	 * <dl>
	 * <dt>{@link #ID_PROPERTY}</dt>
	 * <dd>The {@code parentId} value, passed to all queries.</dd>
	 * 
	 * <dt>{@link #INDEX_PROPERTY}</dt>
	 * <dd>The list index, starting at zero. This defines the ordering of the
	 * related objects. Note the {@code relationDelete} query must support
	 * <em>not</em> having this property set, which signals that <em>all</em>
	 * related objects should be deleted.</dd>
	 * 
	 * <dt>{@link #BEAN_OBJECT_PROPERTY}</dt>
	 * <dd>The related object to persist, passed to {@code relationInsert} and
	 * {@code relationUpdate} queries.</dd>
	 * </dl>
	 * 
	 * <p>
	 * Note that this method does not follow a pattern of deleting all related
	 * objects and then re-inserting objects. If there is a unique constraint
	 * defined on some columns, the constraint should be configured to defer the
	 * constraint check until after the end of the transaction. Otherwise
	 * constraint violations can occur while making the updates.
	 * </p>
	 * 
	 * @param <E>
	 *        the related object type
	 * @param parentId
	 *        the ID of the parent entity
	 * @param newList
	 *        the list of related objects to persist (may be <em>null</em>)
	 * @param relationClass
	 *        the Class of the related object
	 * @param additionalProperties
	 *        optional properties to pass to all queries
	 */
	protected <E> void handleRelation(Long parentId, List<E> newList, Class<? extends E> relationClass,
			Map<String, ?> additionalProperties) {
		if ( parentId == null ) {
			return;
		}
		final String domainName = relationClass.getSimpleName();
		final String findForParentQuery = this.relationQueryForParent + domainName;
		final String deleteRelationQuery = this.relationDelete + domainName;

		List<E> oldList = getSqlSession().selectList(findForParentQuery, parentId);

		Map<String, Object> sqlProperties = new HashMap<String, Object>(
				3 + (additionalProperties == null ? 0 : additionalProperties.size()));
		if ( additionalProperties != null ) {
			sqlProperties.putAll(additionalProperties);
		}
		sqlProperties.put(ID_PROPERTY, parentId);

		if ( newList == null || newList.size() == 0 ) {
			if ( oldList != null && oldList.size() > 0 ) {
				// short cut, delete all for user
				getSqlSession().delete(deleteRelationQuery, sqlProperties);
			}
		} else {
			final String insertRelationQuery = this.relationInsert + domainName;
			final String updateRelationQuery = this.relationUpdate + domainName;

			int index = 0;
			Iterator<E> newItr = newList.iterator();
			Iterator<E> oldItr = oldList == null ? null : oldList.iterator();
			while ( newItr.hasNext() ) {
				E newContact = newItr.next();
				E oldContact = (oldItr == null || !oldItr.hasNext() ? null : oldItr.next());
				sqlProperties.put(INDEX_PROPERTY, index);
				sqlProperties.put(BEAN_OBJECT_PROPERTY, newContact);
				if ( oldContact == null ) {
					// insert
					getSqlSession().insert(insertRelationQuery, sqlProperties);
				} else if ( !newContact.equals(oldContact) ) {
					// update
					getSqlSession().update(updateRelationQuery, sqlProperties);
				}
				index++;
			}
			while ( oldItr != null && oldItr.hasNext() ) {
				// delete
				oldItr.next();
				sqlProperties.put(INDEX_PROPERTY, index);
				getSqlSession().delete(deleteRelationQuery, sqlProperties);
				index++;
			}
		}
	}

	/**
	 * Insert, update, and delete domain object related to the entity managed by
	 * this DAO.
	 * 
	 * <p>
	 * This method will use the {@code relationObjectQueryForParent},
	 * {@code relationInsert}, {@code relationUpdate}, and
	 * {@code relationDelete} query names configured on this class to persist a
	 * related object to a single parent entity. The related object is
	 * <em>not</em> assumed to have surrogate primary keys.
	 * <p>
	 * 
	 * <p>
	 * This method will pass the following query properties:
	 * </p>
	 * 
	 * <dl>
	 * <dt>{@link #ID_PROPERTY}</dt>
	 * <dd>The {@code parentId} value, passed to all queries.</dd>
	 * 
	 * <dt>{@link #BEAN_OBJECT_PROPERTY}</dt>
	 * <dd>The related object to persist, passed to {@code relationInsert} and
	 * {@code relationUpdate} and {@code relationDelete} queries.</dd>
	 * </dl>
	 * 
	 * @param <E>
	 *        the related object type
	 * @param parentId
	 *        the ID of the parent entity
	 * @param newObject
	 *        the related object to persist (may be <em>null</em>)
	 * @param relationClass
	 *        the Class of the related object
	 * @param additionalProperties
	 *        optional properties to pass to all queries
	 */
	protected <E extends Identity<Long>> void handleRelation(Long parentId, E newObject,
			Class<? extends E> relationClass, Map<String, ?> additionalProperties) {
		if ( parentId == null ) {
			return;
		}
		final String domainName = relationClass.getSimpleName();
		final String findForParentQuery = this.relationObjectQueryForParent + domainName;

		Long oldObjectId = (Long) getSqlSession().selectOne(findForParentQuery, parentId);

		Map<String, Object> sqlProperties = new HashMap<String, Object>(
				2 + (additionalProperties == null ? 0 : additionalProperties.size()));
		if ( additionalProperties != null ) {
			sqlProperties.putAll(additionalProperties);
		}
		sqlProperties.put(ID_PROPERTY, parentId);
		sqlProperties.put(BEAN_OBJECT_PROPERTY, newObject);

		if ( newObject == null ) {
			if ( oldObjectId != null ) {
				final String deleteRelationQuery = this.relationDelete + domainName;
				getSqlSession().delete(deleteRelationQuery, sqlProperties);
			}
		} else if ( oldObjectId == null ) {
			final String insertRelationQuery = this.relationInsert + domainName;
			getSqlSession().insert(insertRelationQuery, sqlProperties);
		} else if ( !oldObjectId.equals(newObject.getId()) ) {
			final String updateRelationQuery = this.relationUpdate + domainName;
			getSqlSession().insert(updateRelationQuery, sqlProperties);
		}
	}

	/**
	 * Insert, update, and delete domain object child of the entity managed by
	 * this DAO.
	 * 
	 * <p>
	 * This method will use the {@code relationObjectQueryForParent},
	 * {@code childInsert}, {@code childUpdate}, and {@code childDelete} query
	 * names configured on this class to persist a related object to a single
	 * parent entity.
	 * <p>
	 * 
	 * <p>
	 * This method will pass the following query objects:
	 * </p>
	 * 
	 * <dl>
	 * <dt>relationObjectQueryForParent</dt>
	 * <dd>A Long, taken from ID value of the {@code parent}.</dd>
	 * 
	 * <dt>childDelete</dt>
	 * <dd>A Long, from the ID value of the {@code child}.</dd>
	 * 
	 * <dt>childInsert</dt>
	 * <dd>The {@code child} object. This query is expected to return the
	 * child's primary key in the form of a Long object.</dd>
	 * 
	 * <dt>childUpdate</dt>
	 * <dd>The {@code child} object.</dd>
	 * </dl>
	 * 
	 * @param <E>
	 *        the related object type
	 * @param parent
	 *        the parent entity
	 * @param child
	 *        the child object to persist (may be <em>null</em>)
	 * @param relationClass
	 *        the Class of the related object
	 */
	protected <E extends Identity<Long>> Long handleChildRelation(T parent, E child,
			Class<? extends E> relationClass) {
		if ( parent == null ) {
			return null;
		}
		final String domainName = relationClass.getSimpleName();
		final String findForParentQuery = this.relationObjectQueryForParent + domainName;

		if ( child == null ) {
			Long oldObjectId = null;
			if ( parent.getId() != null ) {
				oldObjectId = getSqlSession().selectOne(findForParentQuery, parent.getId());
			}
			if ( oldObjectId != null ) {
				final String deleteRelationQuery = this.childDelete + domainName;
				getSqlSession().delete(deleteRelationQuery, oldObjectId);
			}
			return null;
		} else if ( child.getId() == null ) {
			final String insertRelationQuery = this.childInsert + domainName;
			Object childId = getSqlSession().insert(insertRelationQuery, child);
			if ( childId instanceof Long ) {
				return (Long) childId;
			}
			return null;
		} else {
			final String updateRelationQuery = this.childUpdate + domainName;
			getSqlSession().insert(updateRelationQuery, child);
		}
		return child.getId();
	}

	/**
	 * Execute a task with a {@link SqlTemplateCallback}, providing standardized
	 * error message handling. This method will catch {@link SqlMapException}
	 * exceptions and attempt to map those to message codes. If the exception
	 * can be mapped, a new {@link ValidationException} will be thrown instead.
	 * 
	 * @param callback
	 *        the callback
	 * @param errorObject
	 *        the root validation error object
	 * @return the result of
	 *         {@link SqlSessionCallback#doWithSqlSession(org.apache.ibatis.session.SqlSession)}
	 * @see #mapSqlMapException(SqlMapException, Object)
	 */
	protected <R> R execute(final SqlSessionCallback<R> callback, final Object errorObject) {
		try {
			return callback.doWithSqlSession(getSqlSession());
		} catch ( RuntimeException e ) {
			throw mapSqlMapException(e, errorObject);
		}
	}

	/**
	 * Attempt to map a runtime, SQL related exception to some friendlier
	 * exception.
	 * 
	 * @param e
	 *        the original exception
	 * @param errorObject
	 *        a validation error object
	 * @return an exception, never <em>null</em> and might be the exception
	 *         instance passed in
	 */
	protected RuntimeException mapSqlMapException(final RuntimeException e, final Object errorObject) {
		RuntimeException result = e;
		Errors errors = new org.springframework.validation.BindException(errorObject, "filter");
		if ( e.getMessage().contains("no statement named") ) {
			errors.reject("error.unknown.query", "Unknown query");
		}
		if ( errors.hasErrors() ) {
			result = new ValidationException(errors, getMessageSource(), e);
		}
		return result;
	}

	/**
	 * Get a "domain" for member queries.
	 * 
	 * <p>
	 * This returns a composite string based from {@link #getDomainClass()} and
	 * the {@code memberClass} passed in.
	 * </p>
	 * 
	 * @param memberClass
	 *        the member class type
	 * @return domain key
	 */
	protected String getMemberDomainKey(Class<?> memberClass) {
		return memberClass.getSimpleName();
	}

	/**
	 * Append to a space-delimited string buffer.
	 * 
	 * <p>
	 * This is designed with full-text search in mind, for building up a query
	 * string.
	 * </p>
	 * 
	 * @param value
	 *        the value to append if not empty
	 * @param buf
	 *        the buffer to append to
	 * @return <em>true</em> if {@code value} was appended to {@code buf}
	 */
	protected final boolean spaceAppend(String value, StringBuilder buf) {
		if ( value == null ) {
			return false;
		}
		value = value.trim();
		if ( value.length() < 1 ) {
			return false;
		}
		if ( buf.length() > 0 ) {
			buf.append(' ');
		}
		buf.append(value);
		return true;
	}

	public String getQueryForId() {
		return queryForId;
	}

	public void setQueryForId(String queryForId) {
		this.queryForId = queryForId;
	}

	public String getInsert() {
		return insert;
	}

	public void setInsert(String insert) {
		this.insert = insert;
	}

	public String getUpdate() {
		return update;
	}

	public void setUpdate(String update) {
		this.update = update;
	}

	public Class<? extends T> getDomainClass() {
		return domainClass;
	}

	public String getQueryForAll() {
		return queryForAll;
	}

	public void setQueryForAll(String queryForAll) {
		this.queryForAll = queryForAll;
	}

	public String getDelete() {
		return delete;
	}

	public void setDelete(String delete) {
		this.delete = delete;
	}

	public String getRelationQueryForParent() {
		return relationQueryForParent;
	}

	public void setRelationQueryForParent(String relationQueryForParent) {
		this.relationQueryForParent = relationQueryForParent;
	}

	public String getRelationInsert() {
		return relationInsert;
	}

	public void setRelationInsert(String relationInsert) {
		this.relationInsert = relationInsert;
	}

	public String getRelationUpdate() {
		return relationUpdate;
	}

	public void setRelationUpdate(String relationUpdate) {
		this.relationUpdate = relationUpdate;
	}

	public String getRelationDelete() {
		return relationDelete;
	}

	public void setRelationDelete(String relationDelete) {
		this.relationDelete = relationDelete;
	}

	public String getRelationObjectQueryForParent() {
		return relationObjectQueryForParent;
	}

	public void setRelationObjectQueryForParent(String relationObjectQueryForParent) {
		this.relationObjectQueryForParent = relationObjectQueryForParent;
	}

	public String getChildInsert() {
		return childInsert;
	}

	public void setChildInsert(String childInsert) {
		this.childInsert = childInsert;
	}

	public String getChildUpdate() {
		return childUpdate;
	}

	public void setChildUpdate(String childUpdate) {
		this.childUpdate = childUpdate;
	}

	public String getChildDelete() {
		return childDelete;
	}

	public void setChildDelete(String childDelete) {
		this.childDelete = childDelete;
	}

	public MessageSource getMessageSource() {
		return messageSource;
	}

	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}
}
