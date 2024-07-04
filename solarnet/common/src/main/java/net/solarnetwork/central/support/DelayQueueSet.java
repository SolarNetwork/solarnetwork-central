/* ==================================================================
 * DelayQueueSet.java - 30/05/2024 10:22:31 am
 * 
 * Copyright 2024 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.support;

import static java.util.concurrent.TimeUnit.NANOSECONDS;
import java.util.AbstractQueue;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A combination of {@link BlockingQueue} and {@link Set}.
 * 
 * <p>
 * Adapted from {@linkjava.util.concurrent.DelayQueue}
 * </p>
 * 
 * @author matt
 * @version 1.0
 */
public class DelayQueueSet<E extends Delayed> extends AbstractQueue<E> implements BlockingQueue<E> {

	private final transient ReentrantLock lock = new ReentrantLock();
	private final PriorityQueue<E> q = new PriorityQueue<E>();
	private final Set<E> s;

	/**
	 * Thread designated to wait for the element at the head of the queue. This
	 * variant of the Leader-Follower pattern
	 * (http://www.cs.wustl.edu/~schmidt/POSA/POSA2/) serves to minimize
	 * unnecessary timed waiting. When a thread becomes the leader, it waits
	 * only for the next delay to elapse, but other threads await indefinitely.
	 * The leader thread must signal some other thread before returning from
	 * take() or poll(...), unless some other thread becomes leader in the
	 * interim. Whenever the head of the queue is replaced with an element with
	 * an earlier expiration time, the leader field is invalidated by being
	 * reset to null, and some waiting thread, but not necessarily the current
	 * leader, is signalled. So waiting threads must be prepared to acquire and
	 * lose leadership while waiting.
	 */
	private Thread leader;

	/**
	 * Condition signalled when a newer element becomes available at the head of
	 * the queue or a new thread may need to become leader.
	 */
	private final Condition available = lock.newCondition();

	/**
	 * Creates a new {@code DelayQueue} that is initially empty.
	 */
	public DelayQueueSet() {
		this(256);
	}

	/**
	 * Creates a new {@code DelayQueue} that is initially empty.
	 * 
	 * @param capacity
	 *        an initial estimated capacity
	 */
	public DelayQueueSet(int capacity) {
		this(new HashSet<>(capacity));
	}

	/**
	 * Creates a new {@code DelayQueue} that is initially empty.
	 * 
	 * @param delegateSet
	 *        a specific set instance to use
	 */
	public DelayQueueSet(Set<E> delegateSet) {
		super();
		this.s = delegateSet;
	}

	/**
	 * Creates a {@code DelayQueue} initially containing the elements of the
	 * given collection of {@link Delayed} instances.
	 *
	 * @param c
	 *        the collection of elements to initially contain
	 * @throws NullPointerException
	 *         if the specified collection or any of its elements are null
	 */
	public DelayQueueSet(Collection<? extends E> c) {
		this(256);
		this.addAll(c);
	}

	/**
	 * Inserts the specified element into this delay queue.
	 *
	 * @param e
	 *        the element to add
	 * @return {@code true} (as specified by {@link Collection#add})
	 * @throws NullPointerException
	 *         if the specified element is null
	 */
	@Override
	public boolean add(E e) {
		return offer(e);
	}

	/**
	 * Inserts the specified element into this delay queue.
	 *
	 * @param e
	 *        the element to add
	 * @return {@code true}
	 * @throws NullPointerException
	 *         if the specified element is null
	 */
	@Override
	public boolean offer(E e) {
		final ReentrantLock lock = this.lock;
		lock.lock();
		try {
			if ( s.add(e) ) {
				q.offer(e);
				if ( q.peek() == e ) {
					leader = null;
					available.signal();
				}
			}
			return true;
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Inserts the specified element into this delay queue. As the queue is
	 * unbounded this method will never block.
	 *
	 * @param e
	 *        the element to add
	 * @throws NullPointerException
	 *         {@inheritDoc}
	 */
	@Override
	public void put(E e) {
		offer(e);
	}

	/**
	 * Inserts the specified element into this delay queue. As the queue is
	 * unbounded this method will never block.
	 *
	 * @param e
	 *        the element to add
	 * @param timeout
	 *        This parameter is ignored as the method never blocks
	 * @param unit
	 *        This parameter is ignored as the method never blocks
	 * @return {@code true}
	 * @throws NullPointerException
	 *         {@inheritDoc}
	 */
	@Override
	public boolean offer(E e, long timeout, TimeUnit unit) {
		return offer(e);
	}

	/**
	 * Retrieves and removes the <a href="#expired-head">expired head</a> of
	 * this queue, or returns {@code null} if this queue has no
	 * <a href="#expired">expired elements</a>.
	 *
	 * @return the <em>expired head</em> of this queue, or {@code null} if this
	 *         queue has no elements with an expired delay
	 */
	@Override
	public E poll() {
		final ReentrantLock lock = this.lock;
		lock.lock();
		try {
			E first = q.peek();
			return removed((first == null || first.getDelay(NANOSECONDS) > 0) ? null : q.poll());
		} finally {
			lock.unlock();
		}
	}

	private E removed(E el) {
		s.remove(el);
		return el;
	}

	/**
	 * Retrieves and removes the <a href="#expired-head">expired head</a> of
	 * this queue, waiting if necessary until an <a href="#expired">expired
	 * element</a> is available on this queue.
	 *
	 * @return the <em>expired head</em> of this queue
	 * @throws InterruptedException
	 *         {@inheritDoc}
	 */
	@Override
	public E take() throws InterruptedException {
		final ReentrantLock lock = this.lock;
		lock.lockInterruptibly();
		try {
			for ( ;; ) {
				E first = q.peek();
				if ( first == null )
					available.await();
				else {
					long delay = first.getDelay(NANOSECONDS);
					if ( delay <= 0L )
						return removed(q.poll());
					first = null; // don't retain ref while waiting
					if ( leader != null )
						available.await();
					else {
						Thread thisThread = Thread.currentThread();
						leader = thisThread;
						try {
							available.awaitNanos(delay);
						} finally {
							if ( leader == thisThread )
								leader = null;
						}
					}
				}
			}
		} finally {
			if ( leader == null && q.peek() != null )
				available.signal();
			lock.unlock();
		}
	}

	/**
	 * Retrieves and removes the <a href="#expired-head">expired head</a> of
	 * this queue, waiting if necessary until an <a href="#expired">expired
	 * element</a> is available on this queue, or the specified wait time
	 * expires.
	 *
	 * @return the <em>expired head</em> of this queue, or {@code null} if the
	 *         specified waiting time elapses before an element with an expired
	 *         delay becomes available
	 * @throws InterruptedException
	 *         {@inheritDoc}
	 */
	@Override
	public E poll(long timeout, TimeUnit unit) throws InterruptedException {
		long nanos = unit.toNanos(timeout);
		final ReentrantLock lock = this.lock;
		lock.lockInterruptibly();
		try {
			for ( ;; ) {
				E first = q.peek();
				if ( first == null ) {
					if ( nanos <= 0L )
						return null;
					else
						nanos = available.awaitNanos(nanos);
				} else {
					long delay = first.getDelay(NANOSECONDS);
					if ( delay <= 0L )
						return removed(q.poll());
					if ( nanos <= 0L )
						return null;
					first = null; // don't retain ref while waiting
					if ( nanos < delay || leader != null )
						nanos = available.awaitNanos(nanos);
					else {
						Thread thisThread = Thread.currentThread();
						leader = thisThread;
						try {
							long timeLeft = available.awaitNanos(delay);
							nanos -= delay - timeLeft;
						} finally {
							if ( leader == thisThread )
								leader = null;
						}
					}
				}
			}
		} finally {
			if ( leader == null && q.peek() != null )
				available.signal();
			lock.unlock();
		}
	}

	/**
	 * Retrieves and removes the <a href="#expired-head">expired head</a> of
	 * this queue, or throws an exception if this queue has no
	 * <a href="#expired">expired elements</a>.
	 *
	 * @return the <em>expired head</em> of this queue
	 * @throws NoSuchElementException
	 *         if this queue has no elements with an expired delay
	 */
	@Override
	public E remove() {
		return removed(super.remove());
	}

	/**
	 * Retrieves, but does not remove, the <a href="#head">head</a> of this
	 * queue, or returns {@code null} if this queue is empty. Unlike
	 * {@code poll}, if no expired elements are available in the queue, this
	 * method returns the element that will expire next, if one exists.
	 *
	 * @return the <em>head</em> of this queue, or {@code null} if this queue is
	 *         empty
	 */
	@Override
	public E peek() {
		final ReentrantLock lock = this.lock;
		lock.lock();
		try {
			return q.peek();
		} finally {
			lock.unlock();
		}
	}

	@Override
	public int size() {
		final ReentrantLock lock = this.lock;
		lock.lock();
		try {
			return q.size();
		} finally {
			lock.unlock();
		}
	}

	/**
	 * @throws UnsupportedOperationException
	 *         {@inheritDoc}
	 * @throws ClassCastException
	 *         {@inheritDoc}
	 * @throws NullPointerException
	 *         {@inheritDoc}
	 * @throws IllegalArgumentException
	 *         {@inheritDoc}
	 */
	@Override
	public int drainTo(Collection<? super E> c) {
		return drainTo(c, Integer.MAX_VALUE);
	}

	/**
	 * @throws UnsupportedOperationException
	 *         {@inheritDoc}
	 * @throws ClassCastException
	 *         {@inheritDoc}
	 * @throws NullPointerException
	 *         {@inheritDoc}
	 * @throws IllegalArgumentException
	 *         {@inheritDoc}
	 */
	@Override
	public int drainTo(Collection<? super E> c, int maxElements) {
		Objects.requireNonNull(c);
		if ( c == this )
			throw new IllegalArgumentException();
		if ( maxElements <= 0 )
			return 0;
		final ReentrantLock lock = this.lock;
		lock.lock();
		try {
			int n = 0;
			for ( E first; n < maxElements && (first = q.peek()) != null
					&& first.getDelay(NANOSECONDS) <= 0; ) {
				c.add(first); // In this order, in case add() throws.
				removed(q.poll());
				++n;
			}
			return n;
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Atomically removes all of the elements from this delay queue. The queue
	 * will be empty after this call returns. Elements with an unexpired delay
	 * are not waited for; they are simply discarded from the queue.
	 */
	@Override
	public void clear() {
		final ReentrantLock lock = this.lock;
		lock.lock();
		try {
			q.clear();
			s.clear();
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Always returns {@code Integer.MAX_VALUE} because a {@code DelayQueue} is
	 * not capacity constrained.
	 *
	 * @return {@code Integer.MAX_VALUE}
	 */
	@Override
	public int remainingCapacity() {
		return Integer.MAX_VALUE;
	}

	/**
	 * Returns an array containing all of the elements in this queue. The
	 * returned array elements are in no particular order.
	 *
	 * <p>
	 * The returned array will be "safe" in that no references to it are
	 * maintained by this queue. (In other words, this method must allocate a
	 * new array). The caller is thus free to modify the returned array.
	 *
	 * <p>
	 * This method acts as bridge between array-based and collection-based APIs.
	 *
	 * @return an array containing all of the elements in this queue
	 */
	@Override
	public Object[] toArray() {
		final ReentrantLock lock = this.lock;
		lock.lock();
		try {
			return q.toArray();
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Returns an array containing all of the elements in this queue; the
	 * runtime type of the returned array is that of the specified array. The
	 * returned array elements are in no particular order. If the queue fits in
	 * the specified array, it is returned therein. Otherwise, a new array is
	 * allocated with the runtime type of the specified array and the size of
	 * this queue.
	 *
	 * <p>
	 * If this queue fits in the specified array with room to spare (i.e., the
	 * array has more elements than this queue), the element in the array
	 * immediately following the end of the queue is set to {@code null}.
	 *
	 * <p>
	 * Like the {@link #toArray()} method, this method acts as bridge between
	 * array-based and collection-based APIs. Further, this method allows
	 * precise control over the runtime type of the output array, and may, under
	 * certain circumstances, be used to save allocation costs.
	 *
	 * <p>
	 * The following code can be used to dump a delay queue into a newly
	 * allocated array of {@code Delayed}:
	 *
	 * <pre> {@code
	 * 
	 * Delayed[] a = q.toArray(new Delayed[0]);
	 * }</pre>
	 *
	 * Note that {@code toArray(new Object[0])} is identical in function to
	 * {@code toArray()}.
	 *
	 * @param a
	 *        the array into which the elements of the queue are to be stored,
	 *        if it is big enough; otherwise, a new array of the same runtime
	 *        type is allocated for this purpose
	 * @return an array containing all of the elements in this queue
	 * @throws ArrayStoreException
	 *         if the runtime type of the specified array is not a supertype of
	 *         the runtime type of every element in this queue
	 * @throws NullPointerException
	 *         if the specified array is null
	 */
	@Override
	public <T> T[] toArray(T[] a) {
		final ReentrantLock lock = this.lock;
		lock.lock();
		try {
			return q.toArray(a);
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Removes a single instance of the specified element from this queue, if it
	 * is present, whether or not it has expired.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public boolean remove(Object o) {
		final ReentrantLock lock = this.lock;
		lock.lock();
		try {
			removed((E) o);
			return q.remove(o);
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Identity-based version for use in Itr.remove.
	 */
	@SuppressWarnings("unchecked")
	void removeEQ(Object o) {
		final ReentrantLock lock = this.lock;
		lock.lock();
		try {
			for ( Iterator<E> it = q.iterator(); it.hasNext(); ) {
				if ( o == it.next() ) {
					it.remove();
					removed((E) o);
					break;
				}
			}
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Returns an iterator over all the elements (both expired and unexpired) in
	 * this queue. The iterator does not return the elements in any particular
	 * order.
	 *
	 * <p>
	 * The returned iterator is <a href="package-summary.html#Weakly"><i>weakly
	 * consistent</i></a>.
	 *
	 * @return an iterator over the elements in this queue
	 */
	@Override
	public Iterator<E> iterator() {
		return new Itr(toArray());
	}

	/**
	 * Snapshot iterator that works off copy of underlying q array.
	 */
	private class Itr implements Iterator<E> {

		final Object[] array; // Array of all elements
		int cursor; // index of next element to return
		int lastRet; // index of last element, or -1 if no such

		Itr(Object[] array) {
			lastRet = -1;
			this.array = array;
		}

		@Override
		public boolean hasNext() {
			return cursor < array.length;
		}

		@Override
		@SuppressWarnings("unchecked")
		public E next() {
			if ( cursor >= array.length )
				throw new NoSuchElementException();
			return (E) array[lastRet = cursor++];
		}

		@Override
		public void remove() {
			if ( lastRet < 0 )
				throw new IllegalStateException();
			removeEQ(array[lastRet]);
			lastRet = -1;
		}
	}

}
