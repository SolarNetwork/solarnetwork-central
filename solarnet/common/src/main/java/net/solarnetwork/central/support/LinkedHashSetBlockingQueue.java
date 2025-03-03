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

import java.util.AbstractQueue;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.SequencedSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A blocking queue implementation backed by a linked hash set for predictable
 * iteration order and constant time addition, removal and contains operations.
 *
 * <p>
 * Adapted from the Apache Marmotta project and
 * {@code java.util.LinkedBlockingQueue}.
 * </p>
 *
 * @author Sebastian Schaffert
 * @author matt
 * @version 1.0
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
	 *        the queue capacity
	 */
	public LinkedHashSetBlockingQueue(int capacity) {
		this(new LinkedHashSet<>(capacity), capacity);
	}

	/**
	 * Constructor.
	 *
	 * @param delegate
	 *        the delegate
	 * @param capacity
	 *        the queue capacity
	 */
	public LinkedHashSetBlockingQueue(SequencedSet<E> delegate, int capacity) {
		this.delegate = delegate;
		this.capacity = capacity;
	}

	@Override
	public boolean offer(E e) {
		if ( e == null ) {
			throw new NullPointerException();
		}
		final AtomicInteger count = this.count;
		if ( count.get() == capacity ) {
			return false;
		}
		int c = -1;
		final ReentrantLock lock = this.lock;
		lock.lock();
		try {
			if ( count.get() < capacity ) {
				final boolean wasAdded = delegate.add(e);
				c = wasAdded ? count.getAndIncrement() : count.get();
				if ( c + 1 < capacity ) {
					notFull.signal();
				}
			}
			if ( c == 0 ) {
				notEmpty.signal();
			}
		} finally {
			lock.unlock();
		}
		return c >= 0;
	}

	@Override
	public void put(E e) throws InterruptedException {
		if ( e == null ) {
			throw new NullPointerException();
		}

		final int c;
		final AtomicInteger count = this.count;
		final ReentrantLock lock = this.lock;
		lock.lockInterruptibly();
		try {
			while ( count.get() == capacity ) {
				notFull.await();
			}
			final boolean wasAdded = delegate.add(e);
			c = wasAdded ? count.getAndIncrement() : count.get();
			if ( c + 1 < capacity ) {
				notFull.signal();
			}
			if ( c == 0 ) {
				notEmpty.signal();
			}
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
		final int c;
		final AtomicInteger count = this.count;
		final ReentrantLock lock = this.lock;
		lock.lockInterruptibly();
		try {
			while ( count.get() == capacity ) {
				if ( nanos <= 0 ) {
					return false;
				}
				nanos = notFull.awaitNanos(nanos);
			}
			final boolean wasAdded = delegate.add(e);
			c = wasAdded ? count.getAndIncrement() : count.get();
			if ( c + 1 < capacity ) {
				notFull.signal();
			}
			if ( c == 0 ) {
				notEmpty.signal();
			}
		} finally {
			lock.unlock();
		}
		return true;
	}

	@Override
	public E take() throws InterruptedException {
		E x;
		final int c;
		final AtomicInteger count = this.count;
		final ReentrantLock lock = this.lock;
		lock.lockInterruptibly();
		try {
			while ( count.get() == 0 ) {
				notEmpty.await();
			}
			x = delegate.removeFirst();
			c = count.getAndDecrement();
			if ( c > 1 ) {
				notEmpty.signal();
			}
			if ( c == capacity ) {
				notFull.signal();
			}
		} finally {
			lock.unlock();
		}
		return x;
	}

	@Override
	public E poll(long timeout, TimeUnit unit) throws InterruptedException {
		E x;
		final int c;
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
			x = delegate.removeFirst();
			c = count.getAndDecrement();
			if ( c > 1 ) {
				notEmpty.signal();
			}
			if ( c == capacity ) {
				notFull.signal();
			}
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

	@Override
	public int drainTo(Collection<? super E> c, int maxElements) {
		if ( c == null ) {
			throw new NullPointerException();
		}
		if ( c == this ) {
			throw new IllegalArgumentException();
		}
		final AtomicInteger count = this.count;
		final ReentrantLock lock = this.lock;
		boolean signalNotFull = false;
		lock.lock();
		try {
			int n = Math.min(maxElements, count.get());
			Iterator<E> it = delegate.iterator();
			for ( int i = 0; i < n && it.hasNext(); i++ ) {
				E x = it.next();
				c.add(x);
				it.remove();
				signalNotFull = true;
			}
			count.getAndAdd(-n);
			if ( signalNotFull ) {
				notFull.signal();
			}
			return n;
		} finally {
			lock.unlock();
		}
	}

	@Override
	public E poll() {
		final AtomicInteger count = this.count;
		if ( count.get() == 0 ) {
			return null;
		}
		final E x;
		final int c;
		final ReentrantLock lock = this.lock;
		lock.lock();
		try {
			if ( count.get() == 0 ) {
				return null;
			}
			x = delegate.removeFirst();
			c = count.getAndDecrement();
			if ( c > 1 ) {
				notEmpty.signal();
			}
			if ( c == capacity ) {
				notFull.signal();
			}
		} finally {
			lock.unlock();
		}
		return x;
	}

	@Override
	public E peek() {
		final AtomicInteger count = this.count;
		if ( count.get() == 0 ) {
			return null;
		}
		final ReentrantLock lock = this.lock;
		lock.lock();
		try {
			Iterator<E> it = delegate.iterator();
			if ( it.hasNext() ) {
				return it.next();
			} else {
				return null;
			}
		} finally {
			lock.unlock();
		}
	}

	@Override
	public Iterator<E> iterator() {
		final ReentrantLock lock = this.lock;
		final Iterator<E> it = delegate.iterator();
		return new Iterator<>() {

			@Override
			public boolean hasNext() {
				lock.lock();
				try {
					return it.hasNext();
				} finally {
					lock.unlock();
				}
			}

			@Override
			public E next() {
				lock.lock();
				try {
					return it.next();
				} finally {
					lock.unlock();
				}
			}

			@Override
			public void remove() {
				lock.lock();
				try {
					it.remove();

					// remove counter
					count.getAndDecrement();
				} finally {
					lock.unlock();
				}
			}
		};
	}

	@Override
	public int size() {
		return count.get();
	}

	@Override
	public boolean remove(Object o) {
		if ( o == null ) {
			return false;
		}
		final AtomicInteger count = this.count;
		final ReentrantLock lock = this.lock;
		lock.lock();
		try {
			if ( delegate.remove(o) ) {
				if ( count.getAndDecrement() == capacity ) {
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
		} finally {
			lock.unlock();
		}
	}

}
