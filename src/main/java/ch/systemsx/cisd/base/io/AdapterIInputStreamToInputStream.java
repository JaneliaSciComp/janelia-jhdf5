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

package ch.systemsx.cisd.base.io;

import java.io.IOException;
import java.io.InputStream;

import ch.systemsx.cisd.base.exceptions.IOExceptionUnchecked;

/**
 * An adapter for {@link IInputStream} that extends {@link java.io.InputStream}.
 *
 * @author Bernd Rinn
 */
public class AdapterIInputStreamToInputStream extends InputStream
{

    private final IInputStream delegate;
    
    public AdapterIInputStreamToInputStream(IInputStream delegate)
    {
        this.delegate = delegate;
    }
    
    @Override
    public int available() throws IOException
    {
        try
        {
            return delegate.available();
        } catch (IOExceptionUnchecked ex)
        {
            throw ex.getCause();
        }
    }

    @Override
    public void close() throws IOException
    {
        try
        {
            delegate.close();
        } catch (IOExceptionUnchecked ex)
        {
            throw ex.getCause();
        }
    }

    @Override
    public synchronized void mark(int readlimit)
    {
        delegate.mark(readlimit);
    }

    @Override
    public boolean markSupported()
    {
        return delegate.markSupported();
    }

    @Override
    public int read() throws IOException
    {
        try
        {
            return delegate.read();
        } catch (IOExceptionUnchecked ex)
        {
            throw ex.getCause();
        }
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException
    {
        try
        {
            return delegate.read(b, off, len);
        } catch (IOExceptionUnchecked ex)
        {
            throw ex.getCause();
        }
    }

    @Override
    public int read(byte[] b) throws IOException
    {
        try
        {
            return delegate.read(b);
        } catch (IOExceptionUnchecked ex)
        {
            throw ex.getCause();
        }
    }

    @Override
    public synchronized void reset() throws IOException
    {
        try
        {
            delegate.reset();
        } catch (IOExceptionUnchecked ex)
        {
            throw ex.getCause();
        }
    }

    @Override
    public long skip(long n) throws IOException
    {
        try
        {
            return delegate.skip(n);
        } catch (IOExceptionUnchecked ex)
        {
            throw ex.getCause();
        }
    }

    //
    // Object
    //
    
    @Override
    public boolean equals(Object obj)
    {
        return delegate.equals(obj);
    }

    @Override
    public int hashCode()
    {
        return delegate.hashCode();
    }

    @Override
    public String toString()
    {
        return delegate.toString();
    }

}
