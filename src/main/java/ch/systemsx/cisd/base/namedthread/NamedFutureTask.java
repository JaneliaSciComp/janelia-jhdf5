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

import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

/**
 * A {@link FutureTask} with a name.
 * 
 * @author Bernd Rinn
 */
class NamedFutureTask<V> extends FutureTask<V> implements NamedRunnable
{
    private final String name;
    
    private Thread thread;
    
    private String oldThreadName;

    NamedFutureTask(Callable<V> callable)
    {
        super(callable);
        this.name =
                (callable instanceof ICallableNameProvider) ? ((ICallableNameProvider) callable)
                        .getCallableName() : null;
    }

    NamedFutureTask(Runnable runnable, V result)
    {
        super(runnable, result);
        this.name =
                (runnable instanceof IRunnableNameProvider) ? ((IRunnableNameProvider) runnable)
                        .getRunnableName() : null;
    }

    void restoreThreadName()
    {
        if (this.thread != null)
        {
            this.thread.setName(oldThreadName);
            this.thread = null;
        }
    }

    void setThread(Thread thread)
    {
        this.thread = thread;
        this.oldThreadName = thread.getName();
    }

    @Override
    public String getRunnableName()
    {
        return name;
    }

}
