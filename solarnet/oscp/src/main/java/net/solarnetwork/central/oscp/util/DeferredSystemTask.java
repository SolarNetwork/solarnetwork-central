/* ==================================================================
 * DeferredTask.java - 19/08/2022 9:27:30 pm
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

package net.solarnetwork.central.oscp.util;

import static net.solarnetwork.central.oscp.domain.OscpUserEvents.eventForConfiguration;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.transaction.support.TransactionTemplate;
import net.solarnetwork.central.biz.UserEventAppenderBiz;
import net.solarnetwork.central.domain.LogEventInfo;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.oscp.dao.ExternalSystemConfigurationDao;
import net.solarnetwork.central.oscp.domain.BaseOscpExternalSystemConfiguration;
import net.solarnetwork.central.oscp.domain.ExternalSystemConfigurationException;
import net.solarnetwork.central.oscp.domain.OscpRole;
import net.solarnetwork.central.oscp.http.ExternalSystemClient;

/**
 * Abstract {@link Runnable} to help with OSCP system related task execution.
 * 
 * @author matt
 * @version 1.0
 */
public abstract class DeferredSystemTask<C extends BaseOscpExternalSystemConfiguration<C>>
		implements Runnable {

	/** The {@code conditionTimeout} property default value. */
	public static final long DEFAULT_CONDITION_TIMEOUT = 60_000L;

	/** The {@code startDelay} property default value. */
	public static final long DEFAULT_START_DELAY = 1_000L;

	/** The {@code startDelayRandomness} property default value. */
	public static final long DEFAULT_START_DELAY_RANDOMNESS = 1_000L;

	/** The {@code retryDelay} property default value. */
	public static final long DEFAULT_RETRY_DELAY = 5_000L;

	/** The {@code remainingTries} property default value. */
	public static final int DEFAULT_TRIES = 3;

	protected final Logger log = LoggerFactory.getLogger(getClass());

	protected final Future<?> condition;
	protected final String name;
	protected final OscpRole role;
	protected final UserLongCompositePK configId;
	protected final ExternalSystemConfigurationDao<C> dao;
	protected final ExternalSystemClient systemBiz;
	protected final UserEventAppenderBiz userEventAppenderBiz;
	protected final Executor executor;
	protected final TaskScheduler taskScheduler;
	protected final TransactionTemplate txTemplate;

	private String[] errorTags;
	private String[] successTags;
	private long conditionTimeout = DEFAULT_CONDITION_TIMEOUT;
	private long startDelay = DEFAULT_START_DELAY;
	private long startDelayRandomness = DEFAULT_START_DELAY_RANDOMNESS;
	private long retryDelay = DEFAULT_RETRY_DELAY;
	private int remainingTries = DEFAULT_TRIES;
	private int tries = 0;
	private C configuration;
	private SystemTaskContext<C> context;

	/**
	 * Constructor.
	 * 
	 * @param name
	 *        a display name for events and logs
	 * @param condition
	 *        the starting condition, that must complete before the task work
	 *        begins
	 * @param role
	 *        the OSCP role
	 * @param configId
	 *        the system configuration ID
	 * @param dao
	 *        the DAO
	 * @param systemBiz
	 *        the system service
	 * @param userEventAppenderBiz
	 *        the optional user event service
	 * @param executor
	 *        an optional executor, required for re-trying
	 * @param taskScheduler
	 *        an optional task scheduler for delaying retries with
	 * @param txTemplate
	 *        an optional transaction template to run the task in
	 */
	public DeferredSystemTask(String name, Future<?> condition, OscpRole role,
			UserLongCompositePK configId, ExternalSystemConfigurationDao<C> dao,
			ExternalSystemClient systemBiz, UserEventAppenderBiz userEventAppenderBiz, Executor executor,
			TaskScheduler taskScheduler, TransactionTemplate txTemplate) {
		super();
		this.name = requireNonNullArgument(name, "name");
		this.condition = requireNonNullArgument(condition, "condition");
		this.role = requireNonNullArgument(role, "role");
		this.configId = requireNonNullArgument(configId, "configId");
		this.dao = requireNonNullArgument(dao, "dao");
		this.systemBiz = requireNonNullArgument(systemBiz, "systemBiz");
		this.userEventAppenderBiz = userEventAppenderBiz;
		this.executor = executor;
		this.taskScheduler = taskScheduler;
		this.txTemplate = txTemplate;
	}

	/**
	 * Set the remaining tries count.
	 * 
	 * @param tries
	 *        the number of tries remaining
	 * @return this instance, for method chaining
	 */
	public DeferredSystemTask<C> withRemainingTries(int tries) {
		this.remainingTries = tries;
		return this;
	}

	/**
	 * Set the condition timeout.
	 * 
	 * @param ms
	 *        the millisecond condition timeout, or {@code 0} to wait forever
	 * @return this instance, for method chaining
	 */
	public DeferredSystemTask<C> withConditionTimeout(long ms) {
		this.conditionTimeout = ms;
		return this;
	}

	/**
	 * Set the start delay.
	 * 
	 * @param ms
	 *        the millisecond start delay (after the condition completes)
	 * @return this instance, for method chaining
	 */
	public DeferredSystemTask<C> withStartDelay(long ms) {
		this.startDelay = ms;
		return this;
	}

	/**
	 * Set the start delay randomness.
	 * 
	 * @param ms
	 *        a maximum random millisecond start delay added to the configured
	 *        {@code startDelay},
	 * @return this instance, for method chaining
	 */
	public DeferredSystemTask<C> withStartDelayRandomness(long ms) {
		this.startDelayRandomness = ms;
		return this;
	}

	/**
	 * Set the retry delay.
	 * 
	 * @param ms
	 *        the millisecond retry delay
	 * @return this instance, for method chaining
	 */
	public DeferredSystemTask<C> withRetryDelay(long ms) {
		this.retryDelay = ms;
		return this;
	}

	/**
	 * Set the user event error tags.
	 * 
	 * @param errorTags
	 *        the user event tags to use for an error event
	 * @return this instance, for method chaining
	 */
	public DeferredSystemTask<C> withErrorEventTags(String[] errorTags) {
		this.errorTags = errorTags;
		return this;
	}

	/**
	 * Set the user event success tags.
	 * 
	 * @param succesTags
	 *        the user event tags to use for a success event
	 * @return this instance, for method chaining
	 */
	public DeferredSystemTask<C> withSuccessEventTags(String[] successTags) {
		this.successTags = successTags;
		return this;
	}

	@Override
	public final void run() {
		tries++;
		final TransactionTemplate tt = this.txTemplate;
		try {
			log.debug("Waiting for external system ready signal");
			try {
				if ( conditionTimeout > 0 ) {
					condition.get(conditionTimeout, TimeUnit.MILLISECONDS);
				} else {
					condition.get();
				}
				if ( startDelay > 0 || startDelayRandomness > 0 ) {
					long delay = startDelay;
					if ( startDelayRandomness > 0 ) {
						SecureRandom rng;
						try {
							rng = SecureRandom.getInstance("SHA1PRNG");
						} catch ( NoSuchAlgorithmException e ) {
							rng = new SecureRandom();
						}
						delay += rng.nextLong(startDelayRandomness);
					}
					log.debug("[{}] task sleeping for {}ms before start...", name, delay);
					Thread.sleep(delay);
				}
			} catch ( InterruptedException e ) {
				// ignore
			}
			if ( tt != null ) {
				tt.executeWithoutResult((t) -> {
					try {
						doWork();
					} catch ( Exception e ) {
						if ( e instanceof RuntimeException r ) {
							throw r;
						}
						throw new RuntimeException(e);
					}
				});
			} else {
				doWork();
			}
		} catch ( ExternalSystemConfigurationException e ) {
			log.warn(e.getMessage());
			if ( userEventAppenderBiz != null ) {
				userEventAppenderBiz.addEvent(e.getConfig().getUserId(), e.getEvent());
			}
		} catch ( Exception e ) {
			if ( --remainingTries > 0 && executor != null ) {
				var msg = "[%s] task with %s %s failed; will re-try up to %d more times: %s"
						.formatted(name, role, configId.ident(), remainingTries, e.getMessage());
				log.warn(msg);
				if ( userEventAppenderBiz != null ) {
					LogEventInfo event;
					if ( configuration != null ) {
						event = eventForConfiguration(configuration, errorTags, msg);
					} else {
						event = eventForConfiguration(configId, errorTags, msg);
					}
					userEventAppenderBiz.addEvent(configId.getUserId(), event);
				}
				if ( taskScheduler != null && retryDelay > 0 ) {
					taskScheduler.schedule(() -> {
						executor.execute(DeferredSystemTask.this);
					}, Instant.now().plusMillis(tries * retryDelay));
				} else {
					executor.execute(this);
				}
			} else {
				var msg = "[%s] task with %s %s failed; tried %d times: %s".formatted(name, role,
						configId.ident(), tries, e.getMessage());
				log.warn(msg, e);
				if ( userEventAppenderBiz != null ) {
					LogEventInfo event = eventForConfiguration(configId, errorTags, msg);
					userEventAppenderBiz.addEvent(configId.getUserId(), event);
				}
			}
		}
	}

	/**
	 * Perform the task.
	 * 
	 * @throws Exception
	 *         if an error occurs
	 */
	protected abstract void doWork() throws Exception;

	/**
	 * Load the configuration entity.
	 * 
	 * @param lock
	 *        {@literal true} to lock the configuration in the current
	 *        transaction
	 * @param errorTags
	 *        error tags to include in a user event if an error occurs
	 * @return the configuration
	 * @throws ExternalSystemConfigurationException
	 *         if the configuration is not found
	 */
	protected C configuration(boolean lock) {
		if ( this.configuration != null ) {
			return this.configuration;
		}
		C config = (lock ? dao.getForUpdate(configId) : dao.get(configId));
		if ( config == null ) {
			var msg = "[%s] task with %s %s failed because the configuration does not exist; perhaps it was deleted."
					.formatted(name, role, configId.ident());
			LogEventInfo event = eventForConfiguration(config, errorTags, "Configuration not found");
			throw new ExternalSystemConfigurationException(role, config, event, msg);
		}
		this.configuration = config;
		return config;
	}

	/**
	 * Get the task context.
	 * 
	 * @param errorTags
	 *        error tags to include in a user event if an error occurs
	 * @return the context
	 * @throws ExternalSystemConfigurationException
	 *         if the configuration is not found
	 */
	protected SystemTaskContext<C> context(String... extraErrorTags) {
		if ( this.context != null ) {
			return context;
		}
		SystemTaskContext<C> ctx = new SystemTaskContext<>(name, role, configuration(true), errorTags,
				successTags, dao);
		this.context = ctx;
		return ctx;
	}

	/**
	 * Make a HTTP post to the external system.
	 * 
	 * @param restOps
	 *        the REST template
	 * @param path
	 *        the URL path
	 * @param body
	 *        the HTTP post body
	 * @param extraErrorTags
	 *        error tags to include in a user event if an error occurs
	 */
	protected void post(String path, Object body, String... extraErrorTags) {
		SystemTaskContext<C> ctx = context(extraErrorTags);
		systemBiz.systemExchange(ctx, HttpMethod.POST, () -> path, body);
	}

}
