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
import java.io.PrintStream;
import java.io.PrintWriter;

/**
 * An exception for tunneling checked exception through code that doesn't expect it.
 * 
 * @author Bernd Rinn
 */
public class CheckedExceptionTunnel extends RuntimeException
{

    private static final long serialVersionUID = 1L;

    /**
     * Returns an unchecked exception from a <var>checkedException</var>.
     * 
     * @param checkedExceptionOrNull The checked exception to tunnel.
     */
    public CheckedExceptionTunnel(final Exception checkedExceptionOrNull)
    {
        super(checkedExceptionOrNull);

        assert (checkedExceptionOrNull instanceof RuntimeException) == false;
    }
    
    protected CheckedExceptionTunnel(final String msg)
    {
        super(msg);
    }

    protected CheckedExceptionTunnel()
    {
    }

    @Override
    public String getMessage()
    {
        if (getCause() != null && getCause().getMessage() != null)
        {
            return getCause().getMessage();
        }
        return super.getMessage();
    }

    @Override
    public String toString()
    {
        if (getCause() != null)
        {
            return getCause().toString();
        }
        return super.toString();
    }

    @Override
    public void printStackTrace(PrintStream s)
    {
        if (getCause() != null)
        {
            getCause().printStackTrace(s);
        } else
        {
            super.printStackTrace(s);
        }
    }

    @Override
    public void printStackTrace(PrintWriter s)
    {
        if (getCause() != null)
        {
            getCause().printStackTrace(s);
        } else
        {
            super.printStackTrace(s);
        }
    }

    /**
     * Like {@link #printStackTrace()}, but includes the tunnel's stacktrace as well.
     */
    public void printFullStackTrace()
    {
        printFullStackTrace(System.err);
    }

    /**
     * Like {@link #printStackTrace(PrintStream)}, but includes the tunnel's stacktrace as well.
     */
    public void printFullStackTrace(PrintStream s)
    {
        synchronized (s) {
            s.println(super.toString());
            StackTraceElement[] trace = getStackTrace();
            for (int i=0; i < trace.length; i++)
                s.println("\tat " + trace[i]);

            Throwable ourCause = getCause();
            if (ourCause != null)
            {
                printStackTraceAsCause(ourCause, s, trace);
            }
        }
    }

    /**
     * Print our stack trace as a cause for the specified stack trace.
     */
    private static void printStackTraceAsCause(Throwable cause, PrintStream s,
                                        StackTraceElement[] causedTrace)
    {
        final StackTraceElement[] trace = cause.getStackTrace();
        int m = trace.length-1, n = causedTrace.length-1;
        while (m >= 0 && n >=0 && trace[m].equals(causedTrace[n])) {
            m--; n--;
        }
        final int framesInCommon = trace.length - 1 - m;

        s.println("Caused by: " + cause);
        for (int i=0; i <= m; i++)
            s.println("\tat " + trace[i]);
        if (framesInCommon != 0)
            s.println("\t... " + framesInCommon + " more");

        final Throwable ourCauseesCause = cause.getCause();
        if (ourCauseesCause != null)
        {
            printStackTraceAsCause(ourCauseesCause, s, trace);
        }
    }

    /**
     * Like {@link #printStackTrace(PrintWriter)}, but includes the tunnel's stacktrace as well.
     */
    public void printFullStackTrace(PrintWriter s)
    {
        synchronized (s) {
            s.println(super.toString());
            StackTraceElement[] trace = getStackTrace();
            for (int i=0; i < trace.length; i++)
                s.println("\tat " + trace[i]);

            Throwable ourCause = getCause();
            if (ourCause != null)
            {
                printStackTraceAsCause(ourCause, s, trace);
            }
        }
    }

    /**
     * Print our stack trace as a cause for the specified stack trace.
     */
    private static void printStackTraceAsCause(Throwable cause, PrintWriter s,
                                        StackTraceElement[] causedTrace)
    {
        final StackTraceElement[] trace = cause.getStackTrace();
        int m = trace.length-1, n = causedTrace.length-1;
        while (m >= 0 && n >=0 && trace[m].equals(causedTrace[n])) {
            m--; n--;
        }
        final int framesInCommon = trace.length - 1 - m;

        s.println("Caused by: " + cause);
        for (int i=0; i <= m; i++)
            s.println("\tat " + trace[i]);
        if (framesInCommon != 0)
            s.println("\t... " + framesInCommon + " more");

        final Throwable ourCauseesCause = cause.getCause();
        if (ourCauseesCause != null)
        {
            printStackTraceAsCause(ourCauseesCause, s, trace);
        }
    }

    /**
     * Convenience wrapper for {@link #wrapIfNecessary(Exception)}. If <var>throwable</var> is an
     * {@link Error}, this method will not return but the error will be thrown.
     * 
     * @param throwable The exception to represent by the return value.
     * @return A {@link RuntimeException} representing the <var>throwable</var>.
     * @throws Error If <var>throwable</var> is an {@link Error} (except when it is a
     *             {@link ThreadDeath}, which returns a {@link InterruptedExceptionUnchecked}).
     */
    public final static RuntimeException wrapIfNecessary(final Throwable throwable) throws Error
    {
        if (throwable instanceof Error)
        {
            if (throwable instanceof ThreadDeath)
            {
                return new InterruptedExceptionUnchecked();
            } else
            {
                throw (Error) throwable;
            }
        }
        return wrapIfNecessary((Exception) throwable);
    }

    /**
     * Returns a {@link RuntimeException} from an <var>exception</var>. If <var>exception</var> is
     * already a {@link RuntimeException}, itself is returned, otherwise an appropriate unchecked
     * equivalent. If no unchecked equivalent exists, a {@link CheckedExceptionTunnel} is returned
     * with <var>exception</var> as checked exception argument.
     * 
     * @param exception The exception to represent by the return value.
     * @return A {@link RuntimeException} representing the <var>exception</var>.
     */
    public final static RuntimeException wrapIfNecessary(final Exception exception)
    {
        if (exception instanceof RuntimeException)
        {
            return (RuntimeException) exception;
        }
        if (exception instanceof IOException)
        {
            return new IOExceptionUnchecked((IOException) exception);
        }
        if (exception instanceof InterruptedException)
        {
            return new InterruptedExceptionUnchecked((InterruptedException) exception);
        }
        if (exception instanceof java.util.concurrent.TimeoutException)
        {
            return new TimeoutExceptionUnchecked((java.util.concurrent.TimeoutException) exception);
        }
        return new CheckedExceptionTunnel(exception);
    }

    /**
     * Returns the original exception before being wrapped, if the exception has been wrapped, or
     * <var>exception</var> otherwise.
     */
    public final static Exception unwrapIfNecessary(final Exception exception)
    {
        assert exception != null : "Exception not specified.";
        if (exception instanceof CheckedExceptionTunnel)
        {
            // We are sure that the wrapped exception is an 'Exception'.
            final Exception causeOrNull = (Exception) exception.getCause();
            if (causeOrNull != null)
            {
                return causeOrNull;
            }
        }
        return exception;
    }

    /**
     * Returns the original throwable before being wrapped, if the throwable has been wrapped, or
     * <var>exception</var> otherwise.
     */
    public final static Throwable unwrapIfNecessary(final Throwable throwable)
    {
        assert throwable != null : "Exception not specified.";
        if (throwable instanceof Error)
        {
            return throwable;
        } else
        {
            return unwrapIfNecessary((Exception) throwable);
        }
    }

}
