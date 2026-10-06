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

import java.util.concurrent.TimeoutException;

/**
 * Exception thrown when a blocking operation times out. This is an unchecked equivalent of
 * {@link TimeoutException}, that is it is derived from {@link CheckedExceptionTunnel}.
 * 
 * @author Bernd Rinn
 */
public class TimeoutExceptionUnchecked extends CheckedExceptionTunnel
{

    private static final long serialVersionUID = 1L;

    public TimeoutExceptionUnchecked()
    {
    }

    public TimeoutExceptionUnchecked(String msg)
    {
        super(msg);
    }

    public TimeoutExceptionUnchecked(java.util.concurrent.TimeoutException cause)
    {
        super(cause);
    }

    @Override
    public TimeoutException getCause()
    {
        return (TimeoutException) super.getCause();
    }

}