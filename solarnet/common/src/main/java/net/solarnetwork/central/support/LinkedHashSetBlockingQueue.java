/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.solarnetwork.central.support;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.util.AbstractQueue;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.SequencedSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import org.jspecify.annotations.Nullable;

/**
 * A blocking queue implementation backed by a linked hash set for predictable
 * iteration order and constant time addition, removal and contains operations.
 *
 * <p>
 * Because the queue is backed by a set, elements are <b>de-duplicated</b>:
 * adding an element already queued leaves the queue unchanged, and does not
 * move the queued element towards the tail. The add methods still report
 * success in that case, so {@link #offer(Object)} and
 * {@link #offer(Object, long, TimeUnit)} return {@code true} and
 * {@link #put(Object)} returns normally without the queue having grown. One
 * exception applies: when the queue is full {@code offer()} returns
 * {@code false} without inspecting the element, even if that element is
 * already queued.
 * </p>
 *
 * <p>
 * All mutating and inspecting operations are guarded by a single lock, so
 * this class is thread safe. The {@link #iterator()} returned is <i>weakly
 * consistent</i>: it iterates a snapshot of the elements taken when the
 * iterator was created, never throws
 * {@link java.util.ConcurrentModificationException}, and does not reflect
 * changes made after the snapshot was taken. Its {@link Iterator#remove()}
 * removes the last returned element from this queue, if still present. The
 * inherited methods that iterate, such as {@link #toArray()} and
 * {@link #toString()}, inherit those semantics.
 * </p>
 *
 * <p>
 * When a delegate set is provided, this queue assumes exclusive ownership
 * of it: modifying the delegate directly will corrupt the queue size
 * accounting.
 * </p>
 *
 * <p>
 * Adapted from the Apache Marmotta project and
 * {@code java.util.LinkedBlockingQueue}.
 * </p>
 *
 * @author Sebastian Schaffert
 * @author matt
 * @version 1.1
 */
public class LinkedHashSetBlockingQueue<E> extends AbstractQueue<E> implements BlockingQueue<E> {

	/** The queue maximum size. */
	private final int capacity;

	/** Current number of elements */
	private final AtomicInteger count = new AtomicInteger(0);

	/** Lock held by take, poll, put, offer, etc */
	private final ReentrantLock lock = new ReentrantLock();

	/** Wait queue for waiting takes */
	private final Condition notEmpty = lock.newCondition();

	/** Wait queue for waiting puts */
	private final Condition notFull = lock.newCondition();

	private final SequencedSet<E> delegate;

	/**
	 * Constructor.
	 *
	 * @param capacity
	 *        the queue capacity; a capacity of {@literal 0} creates a queue
	 *        that can never accept an element, so {@code offer()} always
	 *        returns {@code false} and {@code put()} blocks forever
	 * @throws IllegalArgumentException
	 *         if {@code capacity} is less than {@literal 0}
	 */
	public LinkedHashSetBlockingQueue(int capacity) {
		this(new LinkedHashSet<>(requireValidCapacity(capacity)), capacity);
	}

	/**
	 * Constructor.
	 *
	 * @param delegate
	 *        the delegate; this queue assumes exclusive ownership of this set
	 * @param capacity
	 *        the queue capacity; a capacity of {@literal 0} creates a queue
	 *        that can never accept an element, so {@code offer()} always
	 *        returns {@code false} and {@code put()} blocks forever
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}, or {@code capacity} is less than
	 *         {@literal 0}
	 */
	public LinkedHashSetBlockingQueue(SequencedSet<E> delegate, int capacity) {
		this.delegate = requireNonNullArgument(delegate, "delegate");
		this.capacity = requireValidCapacity(capacity);
	}

	private static int requireValidCapacity(int capacity) {
		if ( capacity < 0 ) {
			throw new IllegalArgumentException("The capacity argument must not be negative.");
		}
		return capacity;
	}

	/**
	 * Signal the threads waiting on a queue whose size has just changed.
	 *
	 * <p>
	 * Must be called while holding {@link #lock}, passing the resulting size.
	 * Signalling whenever an element or a slot <i>is available</i>, rather than
	 * only on the empty to non-empty and full to non-full transitions, is what
	 * keeps a hand-off from being lost when an add turns out to be a duplicate
	 * and so consumes no capacity, and it passes the signal on to the next
	 * waiter while more remains. Only lock holders change the size, so the
	 * caller's value is current.
	 * </p>
	 *
	 * @param size
	 *        the queue size after the change
	 */
	private void signalAvailable(int size) {
		if ( size > 0 ) {
			notEmpty.signal();
		}
		if ( size < capacity ) {
			notFull.signal();
		}
	}

	/**
	 * Add an element and signal any waiting threads.
	 *
	 * <p>
	 * Must be called while holding {@link #lock}, with the queue not full.
	 * </p>
	 *
	 * @param e
	 *        the element to add
	 */
	private void addAndSignal(E e) {
		signalAvailable(delegate.add(e) ? count.incrementAndGet() : count.get());
	}

	/**
	 * Remove the head element and signal any waiting threads.
	 *
	 * <p>
	 * Must be called while holding {@link #lock}, with the queue not empty.
	 * </p>
	 *
	 * @return the removed element
	 */
	private E removeFirstAndSignal() {
		final E x = delegate.removeFirst();
		signalAvailable(count.decrementAndGet());
		return x;
	}

	@Override
	public boolean offer(E e) {
		if ( e == null ) {
			throw new NullPointerException();
		}
		final AtomicInteger count = this.count;
		if ( count.get() >= capacity ) {
			return false;
		}
		final ReentrantLock lock = this.lock;
		lock.lock();
		try {
			if ( count.get() >= capacity ) {
				return false;
			}
			addAndSignal(e);
		} finally {
			lock.unlock();
		}
		return true;
	}

	@Override
	public void put(E e) throws InterruptedException {
		if ( e == null ) {
			throw new NullPointerException();
		}
		final AtomicInteger count = this.count;
		final ReentrantLock lock = this.lock;
		lock.lockInterruptibly();
		try {
			while ( count.get() >= capacity ) {
				notFull.await();
			}
			addAndSignal(e);
		} finally {
			lock.unlock();
		}
	}

	@Override
	public boolean offer(E e, long timeout, TimeUnit unit) throws InterruptedException {
		if ( e == null ) {
			throw new NullPointerException();
		}
		long nanos = unit.toNanos(timeout);
		final AtomicInteger count = this.count;
		final ReentrantLock lock = this.lock;
		lock.lockInterruptibly();
		try {
			while ( count.get() >= capacity ) {
				if ( nanos <= 0 ) {
					return false;
				}
				nanos = notFull.awaitNanos(nanos);
			}
			addAndSignal(e);
		} finally {
			lock.unlock();
		}
		return true;
	}

	@Override
	public E take() throws InterruptedException {
		final E x;
		final AtomicInteger count = this.count;
		final ReentrantLock lock = this.lock;
		lock.lockInterruptibly();
		try {
			while ( count.get() == 0 ) {
				notEmpty.await();
			}
			x = removeFirstAndSignal();
		} finally {
			lock.unlock();
		}
		return x;
	}

	@Override
	public @Nullable E poll(long timeout, TimeUnit unit) throws InterruptedException {
		final E x;
		long nanos = unit.toNanos(timeout);
		final AtomicInteger count = this.count;
		final ReentrantLock lock = this.lock;
		lock.lockInterruptibly();
		try {
			while ( count.get() == 0 ) {
				if ( nanos <= 0 ) {
					return null;
				}
				nanos = notEmpty.awaitNanos(nanos);
			}
			x = removeFirstAndSignal();
		} finally {
			lock.unlock();
		}
		return x;
	}

	@Override
	public int remainingCapacity() {
		return capacity - size();
	}

	@Override
	public int drainTo(Collection<? super E> c) {
		return drainTo(c, Integer.MAX_VALUE);
	}

	@SuppressWarnings("ReferenceEquality")
	@Override
	public int drainTo(Collection<? super E> c, int maxElements) {
		if ( c == null ) {
			throw new NullPointerException();
		}
		if ( c == this ) {
			throw new IllegalArgumentException();
		}
		if ( maxElements <= 0 ) {
			return 0;
		}
		final AtomicInteger count = this.count;
		final ReentrantLock lock = this.lock;
		lock.lock();
		try {
			final int n = Math.min(maxElements, count.get());
			int i = 0;
			try {
				final Iterator<E> it = delegate.iterator();
				while ( i < n && it.hasNext() ) {
					c.add(it.next());
					it.remove();
					i++;
				}
			} finally {
				// restore the count, even if c.add() threw
				if ( i > 0 ) {
					count.getAndAdd(-i);
					notFull.signalAll();
				}
			}
			return i;
		} finally {
			lock.unlock();
		}
	}

	@Override
	public @Nullable E poll() {
		final AtomicInteger count = this.count;
		if ( count.get() == 0 ) {
			return null;
		}
		final E x;
		final ReentrantLock lock = this.lock;
		lock.lock();
		try {
			if ( count.get() == 0 ) {
				return null;
			}
			x = removeFirstAndSignal();
		} finally {
			lock.unlock();
		}
		return x;
	}

	@Override
	public @Nullable E peek() {
		final AtomicInteger count = this.count;
		if ( count.get() == 0 ) {
			return null;
		}
		final ReentrantLock lock = this.lock;
		lock.lock();
		try {
			return delegate.isEmpty() ? null : delegate.getFirst();
		} finally {
			lock.unlock();
		}
	}

	@Override
	public boolean contains(@Nullable Object o) {
		if ( o == null ) {
			return false;
		}
		final ReentrantLock lock = this.lock;
		lock.lock();
		try {
			return delegate.contains(o);
		} finally {
			lock.unlock();
		}
	}

	@Override
	public Iterator<E> iterator() {
		final List<E> snapshot;
		final ReentrantLock lock = this.lock;
		lock.lock();
		try {
			snapshot = new ArrayList<>(delegate);
		} finally {
			lock.unlock();
		}
		return new Iterator<>() {

			private final Iterator<E> it = snapshot.iterator();
			private @Nullable E lastReturned;

			@Override
			public boolean hasNext() {
				return it.hasNext();
			}

			@Override
			public E next() {
				final E next = it.next();
				lastReturned = next;
				return next;
			}

			@Override
			public void remove() {
				final E last = lastReturned;
				if ( last == null ) {
					throw new IllegalStateException();
				}
				lastReturned = null;
				LinkedHashSetBlockingQueue.this.remove(last);
			}
		};
	}

	@Override
	public int size() {
		return count.get();
	}

	@Override
	public boolean remove(@Nullable Object o) {
		if ( o == null ) {
			return false;
		}
		final AtomicInteger count = this.count;
		final ReentrantLock lock = this.lock;
		lock.lock();
		try {
			if ( delegate.remove(o) ) {
				if ( count.decrementAndGet() < capacity ) {
					notFull.signal();
				}
				return true;
			}
		} finally {
			lock.unlock();
		}

		return false;
	}

	@Override
	public void clear() {
		final AtomicInteger count = this.count;
		final ReentrantLock lock = this.lock;
		lock.lock();
		try {
			delegate.clear();
			count.set(0);
			notFull.signalAll();
		} finally {
			lock.unlock();
		}
	}

}
