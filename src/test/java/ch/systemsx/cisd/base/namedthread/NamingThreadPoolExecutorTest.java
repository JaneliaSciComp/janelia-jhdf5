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

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.testng.annotations.Test;

import ch.systemsx.cisd.base.tests.Retry10;

/**
 * Test cases for the {@link NamingThreadPoolExecutor}.
 * 
 * @author Bernd Rinn
 */
public class NamingThreadPoolExecutorTest
{

    private final static String name = "This is the pool name";

    @Test
    public void testNamedPool() throws Throwable
    {
        final ThreadPoolExecutor eservice =
                new NamingThreadPoolExecutor(name).corePoolSize(1).maximumPoolSize(2).daemonize();
        assertEquals(1, eservice.getCorePoolSize());
        assertEquals(2, eservice.getMaximumPoolSize());
        final Future<?> future = eservice.submit(new Runnable()
            {
                @Override
                public void run()
                {
                    assertEquals(name + "-T1", Thread.currentThread().getName());
                }
            });
        try
        {
            future.get(200L, TimeUnit.MILLISECONDS);
        } catch (ExecutionException ex)
        {
            throw ex.getCause();
        }
    }

    @Test
    public void testDaemonize()
    {
        final NamingThreadPoolExecutor eservice =
                new NamingThreadPoolExecutor(name).corePoolSize(1).maximumPoolSize(2);
        assertFalse(eservice.getThreadFactory().isCreateDaemonThreads());
        eservice.daemonize();
        assertTrue(eservice.getThreadFactory().isCreateDaemonThreads());
    }

    @Test
    public void testSetNamedThreadFactory()
    {
        final NamingThreadPoolExecutor eservice =
                new NamingThreadPoolExecutor(name).corePoolSize(1).maximumPoolSize(2).daemonize();
        final NamingThreadFactory factory = new NamingThreadFactory("name");
        eservice.setThreadFactory(factory);
        assertEquals(factory, eservice.getThreadFactory());
    }

    @Test
    public void testSetThreadFactory()
    {
        final ThreadPoolExecutor eservice =
                new NamingThreadPoolExecutor(name).corePoolSize(1).maximumPoolSize(2).daemonize();
        final ThreadFactory factory = new NamingThreadFactory("name");
        eservice.setThreadFactory(factory);
        assertEquals(factory, eservice.getThreadFactory());
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testSetThreadFactoryFailed()
    {
        final ThreadPoolExecutor eservice =
                new NamingThreadPoolExecutor(name).corePoolSize(1).maximumPoolSize(2).daemonize();
        final ThreadFactory factory = new ThreadFactory()
            {
                @Override
                public Thread newThread(Runnable r)
                {
                    return null; // Doesn't matter, never used
                }
            };
        // It needs to be NamingThreadFactory, thus it will throw an IllegalArgumentException.
        eservice.setThreadFactory(factory);
    }

    @Test(groups = "slow")
    public void testThreadDefaultNames() throws Throwable
    {
        final int max = 10;
        final ThreadPoolExecutor eservice =
                new NamingThreadPoolExecutor(name).corePoolSize(max).maximumPoolSize(max).daemonize();
        assertEquals(max, eservice.getCorePoolSize());
        assertEquals(max, eservice.getMaximumPoolSize());
        final Set<String> expectedThreadNameSet = new HashSet<String>();
        for (int i = 1; i <= max; ++i)
        {
            expectedThreadNameSet.add(name + "-T" + i);
        }
        final Set<String> threadNameSet = Collections.synchronizedSet(new HashSet<String>());
        final Set<Future<?>> futureSet = new HashSet<Future<?>>();
        for (int i = 0; i < max; ++i)
        {
            futureSet.add(eservice.submit(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        threadNameSet.add(Thread.currentThread().getName());
                        try
                        {
                            Thread.sleep(20L);
                        } catch (InterruptedException ex)
                        {
                            fail("We got interrupted.");
                        }
                    }
                }));
        }
        for (Future<?> future : futureSet)
        {
            try
            {
                future.get(400L, TimeUnit.MILLISECONDS);
            } catch (ExecutionException ex)
            {
                throw ex.getCause();
            }
        }
        assertEquals(expectedThreadNameSet, threadNameSet);
    }

    @Test(groups = "slow")
    public void testSubmitNamedRunnable() throws Throwable
    {
        final String runnableName = "This is the special runnable name";
        final ThreadPoolExecutor eservice =
                new NamingThreadPoolExecutor(name).corePoolSize(1).maximumPoolSize(1).daemonize();
        assertEquals(1, eservice.getCorePoolSize());
        assertEquals(1, eservice.getMaximumPoolSize());
        final Future<?> future = eservice.submit(new NamedRunnable()
            {
                @Override
                public void run()
                {
                    assertEquals(name + "-T1::" + runnableName, Thread.currentThread().getName());
                }

                @Override
                public String getRunnableName()
                {
                    return runnableName;
                }
            });
        try
        {
            future.get(200L, TimeUnit.MILLISECONDS);
        } catch (ExecutionException ex)
        {
            throw ex.getCause();
        }
    }

    @Test(groups = "slow")
    public void testExecuteNamedRunnable() throws Throwable
    {
        final String runnableName = "This is the special runnable name";
        final ThreadPoolExecutor eservice =
                new NamingThreadPoolExecutor(name).corePoolSize(1).maximumPoolSize(1).daemonize();
        assertEquals(1, eservice.getCorePoolSize());
        assertEquals(1, eservice.getMaximumPoolSize());
        final Semaphore sem = new Semaphore(0);
        eservice.execute(new NamedRunnable()
            {
                @Override
                public void run()
                {
                    assertEquals(name + "-T1::" + runnableName, Thread.currentThread().getName());
                    sem.release();
                }

                @Override
                public String getRunnableName()
                {
                    return runnableName;
                }
            });
        assertTrue(sem.tryAcquire(200L, TimeUnit.MILLISECONDS));
    }

    interface MyRunnable extends Runnable, IRunnableNameProvider
    {
    }
    
    @Test(groups = "slow")
    public void testExecuteNamedMyRunnable() throws Throwable
    {
        final String runnableName = "This is the special runnable name";
        final ThreadPoolExecutor eservice =
                new NamingThreadPoolExecutor(name).corePoolSize(1).maximumPoolSize(1).daemonize();
        assertEquals(1, eservice.getCorePoolSize());
        assertEquals(1, eservice.getMaximumPoolSize());
        final Semaphore sem = new Semaphore(0);
        eservice.execute(new MyRunnable()
            {
                @Override
                public void run()
                {
                    assertEquals(name + "-T1::" + runnableName, Thread.currentThread().getName());
                    sem.release();
                }

                @Override
                public String getRunnableName()
                {
                    return runnableName;
                }
            });
        assertTrue(sem.tryAcquire(200L, TimeUnit.MILLISECONDS));
    }

    @Test(groups = "slow")
    public void testSubmitNamedCallable() throws Throwable
    {
        final String callableName = "This is the special callable name";
        final ThreadPoolExecutor eservice =
                new NamingThreadPoolExecutor(name).corePoolSize(1).maximumPoolSize(1).daemonize();
        assertEquals(1, eservice.getCorePoolSize());
        assertEquals(1, eservice.getMaximumPoolSize());
        final Future<?> future = eservice.submit(new NamedCallable<Object>()
            {
                @Override
                public Object call() throws Exception
                {
                    assertEquals(name + "-T1::" + callableName, Thread.currentThread().getName());
                    return null;
                }

                @Override
                public String getCallableName()
                {
                    return callableName;
                }
            });
        try
        {
            future.get(200L, TimeUnit.MILLISECONDS);
        } catch (ExecutionException ex)
        {
            throw ex.getCause();
        }
    }

    interface MyCallable extends Callable<Object>, ICallableNameProvider
    {
    }
    
    @Test(groups = "slow")
    public void testSubmitMyNamedCallable() throws Throwable
    {
        final String callableName = "This is the special callable name";
        final ThreadPoolExecutor eservice =
                new NamingThreadPoolExecutor(name).corePoolSize(1).maximumPoolSize(1).daemonize();
        assertEquals(1, eservice.getCorePoolSize());
        assertEquals(1, eservice.getMaximumPoolSize());
        final Future<?> future = eservice.submit(new MyCallable()
            {
                @Override
                public Object call() throws Exception
                {
                    assertEquals(name + "-T1::" + callableName, Thread.currentThread().getName());
                    return null;
                }

                @Override
                public String getCallableName()
                {
                    return callableName;
                }
            });
        try
        {
            future.get(200L, TimeUnit.MILLISECONDS);
        } catch (ExecutionException ex)
        {
            throw ex.getCause();
        }
    }

    @Test(groups = "slow", retryAnalyzer = Retry10.class)
    public void testSubmitNamedCallables() throws Throwable
    {
        final String callableName1 = "This is the first special callable name";
        final ThreadPoolExecutor eservice =
                new NamingThreadPoolExecutor(name).corePoolSize(1).maximumPoolSize(1).daemonize();
        assertEquals(1, eservice.getCorePoolSize());
        assertEquals(1, eservice.getMaximumPoolSize());
        final Future<?> future1 = eservice.submit(new NamedCallable<Object>()
            {
                @Override
                public Object call() throws Exception
                {
                    assertEquals(name + "-T1::" + callableName1, Thread.currentThread().getName());
                    return null;
                }

                @Override
                public String getCallableName()
                {
                    return callableName1;
                }
            });
        try
        {
            future1.get(200L, TimeUnit.MILLISECONDS);
        } catch (ExecutionException ex)
        {
            throw ex.getCause();
        }
        // On Linux x64, Java 1.6 we get a RejectedExecutionException if we continue immediately.
        Thread.sleep(200L);
        final String callableName2 = "This is the second special callable name";
        final Future<?> future2 = eservice.submit(new NamedCallable<Object>()
            {
                @Override
                public Object call() throws Exception
                {
                    assertEquals(name + "-T1::" + callableName2, Thread.currentThread().getName());
                    return null;
                }

                @Override
                public String getCallableName()
                {
                    return callableName2;
                }
            });
        try
        {
            future2.get(200L, TimeUnit.MILLISECONDS);
        } catch (ExecutionException ex)
        {
            throw ex.getCause();
        }
    }


    @Test(groups = "slow", retryAnalyzer = Retry10.class)
    public void testSubmitQueuedNamedCallables() throws Throwable
    {
        final String callableName1 = "This is the first special callable name";
        final ThreadPoolExecutor eservice =
                new NamingThreadPoolExecutor(name, 1).corePoolSize(1).maximumPoolSize(1).daemonize();
        assertEquals(1, eservice.getCorePoolSize());
        assertEquals(1, eservice.getMaximumPoolSize());
        final Future<?> future1 = eservice.submit(new NamedCallable<Object>()
            {
                @Override
                public Object call() throws Exception
                {
                    assertEquals(name + "-T1::" + callableName1, Thread.currentThread().getName());
                    Thread.sleep(100L);
                    return null;
                }

                @Override
                public String getCallableName()
                {
                    return callableName1;
                }
            });
        final Future<?> future2 = eservice.submit(new NamedCallable<Object>()
                {
                    @Override
                    public Object call() throws Exception
                    {
                        assertEquals(name + "-T1::" + callableName1, Thread.currentThread().getName());
                        Thread.sleep(100L);
                        return null;
                    }

                    @Override
                    public String getCallableName()
                    {
                        return callableName1;
                    }
                });
        try
        {
            future1.get(200L, TimeUnit.MILLISECONDS);
        } catch (ExecutionException ex)
        {
            throw ex.getCause();
        }
        try
        {
            future2.get(200L, TimeUnit.MILLISECONDS);
        } catch (ExecutionException ex)
        {
            throw ex.getCause();
        }
    }

    public static void main(String[] args) throws Throwable
    {
        System.out.println("Test class: " + NamingThreadPoolExecutorTest.class.getSimpleName());
        System.out.println();
        final NamingThreadPoolExecutorTest test = new NamingThreadPoolExecutorTest();
        for (Method m : NamingThreadPoolExecutorTest.class.getMethods())
        {
            final Test testAnnotation = m.getAnnotation(Test.class);
            if (testAnnotation == null)
            {
                continue;
            }
            if (m.getParameterTypes().length == 0)
            {
                System.out.println("Running " + m.getName());
                try
                {
                    m.invoke(test);
                } catch (InvocationTargetException wrapperThrowable)
                {
                    final Throwable th = wrapperThrowable.getCause();
                    boolean exceptionFound = false;
                    for (Class<?> expectedExClazz : testAnnotation.expectedExceptions())
                    {
                        if (expectedExClazz == th.getClass())
                        {
                            exceptionFound = true;
                            break;
                        }
                    }
                    if (exceptionFound == false)
                    {
                        throw th;
                    }
                }
            }
        }
        System.out.println("Tests OK!");
    }

}
