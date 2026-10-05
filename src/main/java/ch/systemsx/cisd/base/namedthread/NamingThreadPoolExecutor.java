/*
 * Copyright 2007 - 2018 ETH Zuerich, CISD and SIS.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ch.systemsx.cisd.base.namedthread;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * A {@link ThreadPoolExecutor} that allows to attach names to the threads it manages. These names
 * can come either from {@link IRunnableNameProvider}s or {@link ICallableNameProvider}s, or, if
 * their standard counterparts are submitted, a default name is used.
 * 
 * @author Bernd Rinn
 */
public class NamingThreadPoolExecutor extends ThreadPoolExecutor
{

    /**
     * The default time (in milli-seconds) to keep threads alive that are above the core pool size.
     */
    public final static long DEFAULT_KEEP_ALIVE_TIME_MILLIS = 10000L;

    /**
     * Creates a new (caching) <tt>NamingThreadPoolExecutor</tt> with the given initial parameters.
     * This executor will create new threads as needed.
     * 
     * @param poolName the default name for new threads
     */
    public NamingThreadPoolExecutor(String poolName)
    {
        this(poolName, 0);
    }

    /**
     * Creates a new (caching) <tt>NamingThreadPoolExecutor</tt> with the given initial parameters.
     * This executor will create new threads as needed.
     * 
     * @param poolName The default name for new threads.
     * @param workQueueSize The size of the work queue (0 for no queue).
     * 
     */
    public NamingThreadPoolExecutor(String poolName, int workQueueSize)
    {
        super(1, Integer.MAX_VALUE, DEFAULT_KEEP_ALIVE_TIME_MILLIS, TimeUnit.MILLISECONDS,
                workQueueSize == 0 ? new SynchronousQueue<Runnable>()
                        : new LinkedBlockingQueue<Runnable>(workQueueSize),
                new NamingThreadFactory(poolName));
    }

    /**
     * Creates a new <tt>NamingThreadPoolExecutor</tt> with the given initial parameters.
     * 
     * @param poolName the default name for new threads
     * @param corePoolSize the number of threads to keep in the pool, even if they are idle.
     * @param maximumPoolSize the maximum number of threads to allow in the pool.
     * @param keepAliveTime when the number of threads is greater than the core, this is the maximum
     *            time that excess idle threads will wait for new tasks before terminating.
     * @param unit the time unit for the keepAliveTime argument.
     * @param workQueue the queue to use for holding tasks before they are executed. This queue will
     *            hold only the <tt>Runnable</tt> tasks submitted by the <tt>execute</tt> method.
     * @param handler the handler to use when execution is blocked because the thread bounds and
     *            queue capacities are reached.
     * @throws IllegalArgumentException if corePoolSize, or keepAliveTime less than zero, or if
     *             maximumPoolSize less than or equal to zero, or if corePoolSize greater than
     *             maximumPoolSize.
     * @throws NullPointerException if <tt>workQueue</tt> or <tt>threadFactory</tt> or
     *             <tt>handler</tt> are null.
     */
    public NamingThreadPoolExecutor(String poolName, int corePoolSize, int maximumPoolSize,
            long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue,
            RejectedExecutionHandler handler)
    {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue,
                new NamingThreadFactory(poolName), handler);
    }

    /**
     * Creates a new <tt>NamingThreadPoolExecutor</tt> with the given initial parameters.
     * 
     * @param poolName the default name for new threads
     * @param corePoolSize the number of threads to keep in the pool, even if they are idle.
     * @param maximumPoolSize the maximum number of threads to allow in the pool.
     * @param keepAliveTime when the number of threads is greater than the core, this is the maximum
     *            time that excess idle threads will wait for new tasks before terminating.
     * @param unit the time unit for the keepAliveTime argument.
     * @param workQueue the queue to use for holding tasks before they are executed. This queue will
     *            hold only the <tt>Runnable</tt> tasks submitted by the <tt>execute</tt> method.
     * @throws IllegalArgumentException if corePoolSize, or keepAliveTime less than zero, or if
     *             maximumPoolSize less than or equal to zero, or if corePoolSize greater than
     *             maximumPoolSize.
     * @throws NullPointerException if <tt>workQueue</tt> or <tt>threadFactory</tt> are null.
     */
    public NamingThreadPoolExecutor(String poolName, int corePoolSize, int maximumPoolSize,
            long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue)
    {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue,
                new NamingThreadFactory(poolName));
    }

    /**
     * Creates a new <tt>NamingThreadPoolExecutor</tt> with the given initial parameters.
     * 
     * @param corePoolSize the number of threads to keep in the pool, even if they are idle.
     * @param maximumPoolSize the maximum number of threads to allow in the pool.
     * @param keepAliveTime when the number of threads is greater than the core, this is the maximum
     *            time that excess idle threads will wait for new tasks before terminating.
     * @param unit the time unit for the keepAliveTime argument.
     * @param workQueue the queue to use for holding tasks before they are executed. This queue will
     *            hold only the <tt>Runnable</tt> tasks submitted by the <tt>execute</tt> method.
     * @param threadFactory the factory to use when the executor creates a new thread.
     * @param handler the handler to use when execution is blocked because the thread bounds and
     *            queue capacities are reached.
     * @throws IllegalArgumentException if corePoolSize, or keepAliveTime less than zero, or if
     *             maximumPoolSize less than or equal to zero, or if corePoolSize greater than
     *             maximumPoolSize.
     * @throws NullPointerException if <tt>workQueue</tt> or <tt>threadFactory</tt> or
     *             <tt>handler</tt> are null.
     */
    public NamingThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime,
            TimeUnit unit, BlockingQueue<Runnable> workQueue, NamingThreadFactory threadFactory,
            RejectedExecutionHandler handler)
    {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler);
    }

    /**
     * Creates a new <tt>NamingThreadPoolExecutor</tt> with the given initial parameters.
     * 
     * @param corePoolSize the number of threads to keep in the pool, even if they are idle.
     * @param maximumPoolSize the maximum number of threads to allow in the pool.
     * @param keepAliveTime when the number of threads is greater than the core, this is the maximum
     *            time that excess idle threads will wait for new tasks before terminating.
     * @param unit the time unit for the keepAliveTime argument.
     * @param workQueue the queue to use for holding tasks before they are executed. This queue will
     *            hold only the <tt>Runnable</tt> tasks submitted by the <tt>execute</tt> method.
     * @param threadFactory the factory to use when the executor creates a new thread.
     * @throws IllegalArgumentException if corePoolSize, or keepAliveTime less than zero, or if
     *             maximumPoolSize less than or equal to zero, or if corePoolSize greater than
     *             maximumPoolSize.
     * @throws NullPointerException if <tt>workQueue</tt> or <tt>threadFactory</tt> are null.
     */
    public NamingThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime,
            TimeUnit unit, BlockingQueue<Runnable> workQueue, NamingThreadFactory threadFactory)
    {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory);
    }

    /**
     * Sets the thread factory of this pool executor to daemon creation mode.
     * <p>
     * This method is supposed to be used in chaining mode, i.e.
     * 
     * <pre>
     * final ExecutorService executor = new NamingThreadPoolExecutor(&quot;name&quot;).daemonize();
     * </pre>
     * 
     * @return This class itself.
     */
    public NamingThreadPoolExecutor daemonize()
    {
        getThreadFactory().setCreateDaemonThreads(true);
        return this;
    }

    /**
     * Same as {@link #setCorePoolSize(int)}, but returns the object itself for chaining.
     */
    public NamingThreadPoolExecutor corePoolSize(int corePoolSize)
    {
        setCorePoolSize(corePoolSize);
        return this;
    }

    /**
     * Same as {@link #setMaximumPoolSize(int)}, but returns the object itself for chaining.
     */
    public NamingThreadPoolExecutor maximumPoolSize(int maximumPoolSize)
    {
        setMaximumPoolSize(maximumPoolSize);
        return this;
    }

    /**
     * Same as {@link #setKeepAliveTime(long, TimeUnit)}, but uses always
     * {@link TimeUnit#MILLISECONDS} and returns the object itself for chaining.
     */
    public NamingThreadPoolExecutor keepAliveTime(long keepAliveTimeMillis)
    {
        setKeepAliveTime(keepAliveTimeMillis, TimeUnit.MILLISECONDS);
        return this;
    }

    /**
     * If <var>addPoolName</var> is <code>true</code>, the threads will contain the pool name as the
     * first part of the thread names.
     */
    public NamingThreadPoolExecutor addPoolName(boolean addPoolName)
    {
        getThreadFactory().setAddPoolName(addPoolName);
        return this;
    }

    @Override
    public NamingThreadFactory getThreadFactory()
    {
        return (NamingThreadFactory) super.getThreadFactory();
    }

    /**
     * Sets the thread factory of this pool executor.
     */
    public void setThreadFactory(NamingThreadFactory threadFactory)
    {
        super.setThreadFactory(threadFactory);
    }

    /**
     * @deprecated Use {@link #setThreadFactory(NamingThreadFactory)} instead!
     */
    @Override
    @Deprecated
    public void setThreadFactory(ThreadFactory threadFactory)
    {
        if (threadFactory instanceof NamingThreadFactory == false)
        {
            throw new IllegalArgumentException("thread factory is of type '"
                    + threadFactory.getClass().getCanonicalName() + ", but needs to be of type "
                    + NamingThreadFactory.class.getCanonicalName());
        }
        super.setThreadFactory(threadFactory);
    }

    @Override
    protected void beforeExecute(Thread t, Runnable r)
    {
        if (r instanceof IRunnableNameProvider == false)
        {
            return;
        }
        final String runnableName = ((IRunnableNameProvider) r).getRunnableName();
        if (runnableName == null)
        {
            return;
        }
        if (r instanceof NamedFutureTask<?>)
        {
            ((NamedFutureTask<?>) r).setThread(t);
        }
        if (t instanceof PoolNameThread)
        {
            ((PoolNameThread) t).setRunnableName(runnableName);
        } else
        {
            t.setName(runnableName);
        }
        super.beforeExecute(t, r);
    }

    @Override
    protected void afterExecute(Runnable r, Throwable t)
    {
        if (r instanceof NamedFutureTask<?>)
        {
            ((NamedFutureTask<?>) r).restoreThreadName();
        }
        super.afterExecute(r, t);
    }

    @Override
    public Future<?> submit(Runnable task)
    {
        if (task == null)
        {
            throw new NullPointerException();
        }

        final FutureTask<Object> ftask = new NamedFutureTask<Object>(task, null);
        execute(ftask);
        return ftask;
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result)
    {
        if (task == null)
        {
            throw new NullPointerException();
        }

        final FutureTask<T> ftask = new NamedFutureTask<T>(task, result);
        execute(ftask);
        return ftask;
    }

    @Override
    public <T> Future<T> submit(Callable<T> task)
    {
        if (task == null)
        {
            throw new NullPointerException();
        }
        final FutureTask<T> ftask = new NamedFutureTask<T>(task);
        execute(ftask);
        return ftask;
    }

}
