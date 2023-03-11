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

package ch.systemsx.cisd.hdf5;

/**
 * A configurator for a {@link IHDF5Reader}.
 * <p>
 * Obtain an object implementing this interface by calling {@link IHDF5Factory#configureForReading(java.io.File)}.
 * <p>
 * If you want the reader to perform numeric conversions, call {@link #performNumericConversions()}
 * before calling {@link #reader()}.
 * 
 * @author Bernd Rinn
 */
public interface IHDF5ReaderConfigurator
{

    /**
     * Returns <code>true</code>, if this platform supports numeric conversions.
     */
    public boolean platformSupportsNumericConversions();

    /**
     * Will try to perform numeric conversions where appropriate if supported by the platform.
     * <p>
     * <strong>Numeric conversions can be platform dependent and are not available on all platforms.
     * Be advised not to rely on numeric conversions if you can help it!</strong>
     */
    public IHDF5ReaderConfigurator performNumericConversions();

    /**
     * Switches off automatic dereferencing of unresolved references. Use this when you need to
     * access file names that start with \0. The down-side of switching off automatic dereferencing
     * is that you can't provide references as obtained by
     * {@link IHDF5ReferenceReader#read(String, boolean)} with
     * <code>resolveName=false</code> in places where a dataset path is required.
     * <br>
     * <i>Note: automatic dereferencing is switched on by default.</i>
     */
    public IHDF5ReaderConfigurator noAutoDereference();
    
    /**
     * Returns an {@link IHDF5Reader} based on this configuration.
     */
    public IHDF5Reader reader();

}
