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
import java.io.OutputStream;

import ch.systemsx.cisd.base.exceptions.IOExceptionUnchecked;

/**
 * An adapter for {@link IOutputStream} that extends {@link java.io.OutputStream}.
 *
 * @author Bernd Rinn
 */
public class AdapterIOutputStreamToOutputStream extends OutputStream
{

    private final IOutputStream delegate;
    
    public AdapterIOutputStreamToOutputStream(IOutputStream delegate)
    {
        this.delegate = delegate;
    }
    
    @Override
    public void write(int b) throws IOException
    {
        try
        {
            delegate.write(b);
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
    public void flush() throws IOException
    {
        try
        {
            delegate.flush();
        } catch (IOExceptionUnchecked ex)
        {
            throw ex.getCause();
        }
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException
    {
        try
        {
            delegate.write(b, off, len);
        } catch (IOExceptionUnchecked ex)
        {
            throw ex.getCause();
        }
    }

    @Override
    public void write(byte[] b) throws IOException
    {
        try
        {
            delegate.write(b);
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
