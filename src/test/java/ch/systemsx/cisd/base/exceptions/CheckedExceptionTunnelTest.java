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

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import org.apache.commons.lang3.StringUtils;
import org.testng.annotations.Test;

import static org.testng.AssertJUnit.*;

/**
 * Test cases for {@link CheckedExceptionTunnel}
 * 
 * @author Bernd Rinn
 */
public class CheckedExceptionTunnelTest
{

    @Test
    public void testGetMessage()
    {
        final IOException ioe = new IOException("This is the message");
        final RuntimeException re = CheckedExceptionTunnel.wrapIfNecessary(ioe);
        assertEquals("This is the message", re.getMessage());
    }

    @Test
    public void testToString()
    {
        final InterruptedException ioe = new InterruptedException("Somehow got interrupted");
        final RuntimeException re = CheckedExceptionTunnel.wrapIfNecessary(ioe);
        assertEquals("java.lang.InterruptedException: Somehow got interrupted", re.toString());
    }

    @Test
    public void testPrintStackTrace()
    {
        final InterruptedException ie = new InterruptedException("Somehow got interrupted");
        ie.fillInStackTrace();
        final RuntimeException re = CheckedExceptionTunnel.wrapIfNecessary(ie);
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        re.printStackTrace(pw);
        final String[] lines = StringUtils.split(sw.toString(), '\n');
        assertEquals("java.lang.InterruptedException: Somehow got interrupted", lines[0]);
        assertTrue(
                lines[1],
                lines[1].startsWith("\tat ch.systemsx.cisd.base.exceptions.CheckedExceptionTunnelTest."
                        + "testPrintStackTrace(CheckedExceptionTunnelTest.java"));
    }

    @Test
    public void testPrintFullStackTrace()
    {
        final Exception e = new Exception("Some exceptional condition");
        e.fillInStackTrace();
        final CheckedExceptionTunnel re = new CheckedExceptionTunnel(e);
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        re.printFullStackTrace();
        re.printFullStackTrace(pw);
        final String[] lines = StringUtils.split(sw.toString(), '\n');
        assertEquals(
                "ch.systemsx.cisd.base.exceptions.CheckedExceptionTunnel: Some exceptional condition",
                lines[0]);
        assertTrue(
                lines[1],
                lines[1].startsWith("\tat ch.systemsx.cisd.base.exceptions.CheckedExceptionTunnelTest."
                        + "testPrintFullStackTrace(CheckedExceptionTunnelTest.java:"));
    }
}
