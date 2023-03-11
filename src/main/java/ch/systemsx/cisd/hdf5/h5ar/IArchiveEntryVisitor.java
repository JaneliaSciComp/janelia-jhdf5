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

package ch.systemsx.cisd.hdf5.h5ar;

/**
 * A role to visit {@link ArchiveEntry}s.
 * 
 * @author Bernd Rinn
 */
public interface IArchiveEntryVisitor
{
    public final static IArchiveEntryVisitor DEFAULT_VISITOR = new IArchiveEntryVisitor()
    {
        @Override
        public void visit(ArchiveEntry entry)
        {
            System.out.println(entry.describeLink());
        }
    };

    public final static IArchiveEntryVisitor NONVERBOSE_VISITOR = new IArchiveEntryVisitor()
    {
        @Override
        public void visit(ArchiveEntry entry)
        {
            System.out.println(entry.describeLink(false));
        }
    };

    /**
     * Called for each archive <var>entry</var> which is visited.
     */
    public void visit(ArchiveEntry entry);
}