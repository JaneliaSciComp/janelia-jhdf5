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

import ch.systemsx.cisd.base.exceptions.IOExceptionUnchecked;

/**
 * An interface for {@link java.io.InputStream}.
 *
 * @author Bernd Rinn
 */
public interface IInputStream extends AutoCloseable
{

    /**
     * @see java.io.InputStream#read()
     */
    public int read() throws IOExceptionUnchecked;
    
    /**
     * @see java.io.InputStream#read(byte[])
     */
    public int read(byte b[]) throws IOExceptionUnchecked;
    
    /**
     * @see java.io.InputStream#read(byte[], int, int)
     */
    public int read(byte b[], int off, int len) throws IOExceptionUnchecked;
    
    /**
     * @see java.io.InputStream#skip(long)
     */
    public long skip(long n) throws IOExceptionUnchecked;
    
    /**
     * @see java.io.InputStream#available()
     */
    public int available() throws IOExceptionUnchecked;

    /**
     * @see java.io.InputStream#close()
     */
    public void close() throws IOExceptionUnchecked;
    
    /**
     * @see java.io.InputStream#mark(int)
     */
    public void mark(int readlimit);
    
    /**
     * @see java.io.InputStream#reset()
     */
    public void reset() throws IOExceptionUnchecked;

    /**
     * @see java.io.InputStream#markSupported()
     */
    public boolean markSupported();

}
