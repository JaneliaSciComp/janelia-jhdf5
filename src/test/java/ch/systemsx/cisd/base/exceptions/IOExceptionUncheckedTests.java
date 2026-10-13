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

package ch.systemsx.cisd.base.exceptions;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.testng.annotations.Test;

import ch.systemsx.cisd.base.exceptions.CheckedExceptionTunnel;
import ch.systemsx.cisd.base.exceptions.IOExceptionUnchecked;

/**
 * Test cases for {@link IOExceptionUnchecked}.
 *
 * @author Bernd Rinn
 */
public class IOExceptionUncheckedTests
{
    @SuppressWarnings("resource")
    private void generateFileNotFoundException() throws IOExceptionUnchecked
    {
        try
        {
            new FileInputStream(new File("doesnt.exist"));
        } catch (IOException ex)
        {
            throw CheckedExceptionTunnel.wrapIfNecessary(ex);
        }
    }
    
    @Test
    public void testWrapUnwrapIOException()
    {
        try
        {
            generateFileNotFoundException();
            fail("No IOException thrown");
        } catch (IOExceptionUnchecked e)
        {
            final Exception ex = CheckedExceptionTunnel.unwrapIfNecessary(e);
            assertTrue(ex instanceof FileNotFoundException);
            assertTrue(ex.getMessage().startsWith("doesnt.exist"));
        }
    }
    
    @Test
    public void testWrapUnwrapNonIOException()
    {
        try
        {
            throw new IOExceptionUnchecked(new IllegalStateException("Don't like this state"));
        } catch (CheckedExceptionTunnel e)
        {
            final Exception ex = CheckedExceptionTunnel.unwrapIfNecessary(e);
            assertTrue(ex instanceof IOException);
            assertEquals("IllegalStateException: Don't like this state", ex.getMessage());
            assertNotNull(ex.getCause());
            assertTrue(ex.getCause() instanceof IllegalStateException);
            assertEquals("Don't like this state", ex.getCause().getMessage());
        }
    }

    @Test
    public void testWrapUnwrapIOExceptionGivingMsg()
    {
        try
        {
            throw new IOExceptionUnchecked("Some message");
        } catch (CheckedExceptionTunnel e)
        {
            final Exception ex = CheckedExceptionTunnel.unwrapIfNecessary(e);
            assertTrue(ex instanceof IOException);
            assertEquals("Some message", ex.getMessage());
            assertNull(ex.getCause());
        }
    }

    public static void main(String[] args) throws Throwable
    {
        System.out.println("Test class: " + IOExceptionUncheckedTests.class.getSimpleName());
        System.out.println();
        final IOExceptionUncheckedTests test = new IOExceptionUncheckedTests();
        for (Method m : IOExceptionUncheckedTests.class.getMethods())
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
