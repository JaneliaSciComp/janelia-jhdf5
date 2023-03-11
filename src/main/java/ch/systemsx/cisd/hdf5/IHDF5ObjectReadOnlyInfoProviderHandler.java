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

import java.util.List;

import ch.systemsx.cisd.hdf5.HDF5DataTypeInformation.DataTypeInfoOptions;

/**
 * An interface for getting information on HDF5 objects like links, groups, data sets and data
 * types.
 * <p>
 * Obtain an object implementing this interface by calling {@link IHDF5Reader#object()}.
 * 
 * @author Bernd Rinn
 */
public interface IHDF5ObjectReadOnlyInfoProviderHandler
{
    // /////////////////////
    // Objects & Links
    // /////////////////////

    /**
     * Returns the link information for the given <var>objectPath</var>. If <var>objectPath</var>
     * does not exist, the link information will have a type {@link HDF5ObjectType#NONEXISTENT}.
     */
    public HDF5LinkInformation getLinkInformation(final String objectPath);

    /**
     * Returns the object information for the given <var>objectPath</var>. If <var>objectPath</var>
     * is a symbolic link, this method will return the type of the object that this link points to
     * rather than the type of the link. If <var>objectPath</var> does not exist, the object
     * information will have a type {@link HDF5ObjectType#NONEXISTENT} and the other fields will not
     * be set.
     */
    public HDF5ObjectInformation getObjectInformation(final String objectPath);

    /**
     * Returns the type of the given <var>objectPath<var>. If <var>followLink</var> is
     * <code>false</code> and <var>objectPath<var> is a symbolic link, this method will return the
     * type of the link rather than the type of the object that the link points to.
     */
    public HDF5ObjectType getObjectType(final String objectPath, boolean followLink);

    /**
     * Returns the type of the given <var>objectPath</var>. If <var>objectPath</var> is a symbolic
     * link, this method will return the type of the object that this link points to rather than the
     * type of the link, that is, it will follow symbolic links.
     */
    public HDF5ObjectType getObjectType(final String objectPath);

    /**
     * Returns <code>true</code>, if <var>objectPath</var> exists and <code>false</code> otherwise.
     * if <var>followLink</var> is <code>false</code> and <var>objectPath</var> is a symbolic link,
     * this method will return <code>true</code> regardless of whether the link target exists or
     * not.
     */
    public boolean exists(final String objectPath, boolean followLink);

    /**
     * Returns <code>true</code>, if <var>objectPath</var> exists and <code>false</code> otherwise.
     * If <var>objectPath</var> is a symbolic link, the method will return <code>true</code> if the
     * link target exists, that is, this method will follow symbolic links.
     */
    public boolean exists(final String objectPath);
    
    /**
     * Opens a data set for reading (reader and writer) or writing (writer).
     *  
     * @param objectPath The name (with path) of the data set to open
     */
    public HDF5DataSet openDataSet(final String objectPath);

    /**
     * Creates and returns an internal (house-keeping) version of <var>objectPath</var>.
     */
    public String toHouseKeepingPath(final String objectPath);

    /**
     * Returns <code>true</code> if <var>objectPath</var> denotes an internal (house-keeping)
     * object.
     */
    public boolean isHouseKeepingObject(final String objectPath);

    /**
     * Returns <code>true</code> if the <var>objectPath</var> exists and represents a group and
     * <code>false</code> otherwise. Note that if <var>followLink</var> is <code>false</code> this
     * method will return <code>false</code> if <var>objectPath</var> is a symbolic link that points
     * to a group.
     */
    public boolean isGroup(final String objectPath, boolean followLink);

    /**
     * Returns <code>true</code> if the <var>objectPath</var> exists and represents a group and
     * <code>false</code> otherwise. Note that if <var>objectPath</var> is a symbolic link, this
     * method will return <code>true</code> if the link target of the symbolic link is a group, that
     * is, this method will follow symbolic links.
     */
    public boolean isGroup(final String objectPath);

    /**
     * Returns <code>true</code> if the <var>objectPath</var> exists and represents a data set and
     * <code>false</code> otherwise. Note that if <var>followLink</var> is <code>false</code> this
     * method will return <code>false</code> if <var>objectPath</var> is a symbolic link that points
     * to a data set.
     */
    public boolean isDataSet(final String objectPath, boolean followLink);

    /**
     * Returns <code>true</code> if the <var>objectPath</var> exists and represents a data set and
     * <code>false</code> otherwise. Note that if <var>objectPath</var> is a symbolic link, this
     * method will return <code>true</code> if the link target of the symbolic link is a data set,
     * that is, this method will follow symbolic links.
     */
    public boolean isDataSet(final String objectPath);

    /**
     * Returns <code>true</code> if the <var>objectPath</var> exists and represents a data type and
     * <code>false</code> otherwise. Note that if <var>followLink</var> is <code>false</code> this
     * method will return <code>false</code> if <var>objectPath</var> is a symbolic link that points
     * to a data type.
     */
    public boolean isDataType(final String objectPath, boolean followLink);

    /**
     * Returns <code>true</code> if the <var>objectPath</var> exists and represents a data type and
     * <code>false</code> otherwise. Note that if <var>objectPath</var> is a symbolic link, this
     * method will return <code>true</code> if the link target of the symbolic link is a data type,
     * that is, this method will follow symbolic links.
     */
    public boolean isDataType(final String objectPath);

    /**
     * Returns <code>true</code> if the <var>objectPath</var> exists and represents a soft link and
     * <code>false</code> otherwise.
     */
    public boolean isSoftLink(final String objectPath);

    /**
     * Returns <code>true</code> if the <var>objectPath</var> exists and represents an external link
     * and <code>false</code> otherwise.
     */
    public boolean isExternalLink(final String objectPath);

    /**
     * Returns <code>true</code> if the <var>objectPath</var> exists and represents either a soft
     * link or an external link and <code>false</code> otherwise.
     */
    public boolean isSymbolicLink(final String objectPath);

    /**
     * Returns the target of the symbolic link that <var>objectPath</var> points to, or
     * <code>null</code>, if <var>objectPath</var> is not a symbolic link.
     * <p>
     * Note that external links have a special format: They start with a prefix " <code>EXTERNAL::</code>", then comes the path of the external file
     * (beware that this part uses the native path separator, i.e. "\" on Windows). Finally, separated by "<code>::</code> ", the path of the link in
     * the external file is provided (this part always uses "/" as path separator).
     */
    public String tryGetSymbolicLinkTarget(final String objectPath);

    /**
     * Returns the filename of an external link, or <code>null</code>, if this link does not exist or is not an external link.
     */
    public String tryGetExternalLinkFilename(final String objectPath);
    
    /**
     * Returns the external link target of this link, or <code>null</code>, if this link does not exist or is not an external link.
     */
    public String tryGetExternalLinkTarget(final String objectPath);
    
    // /////////////////////
    // Attributes
    // /////////////////////

    /**
     * Returns <code>true</code>, if the <var>objectPath</var> has an attribute with name
     * <var>attributeName</var>.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param attributeName The name of the attribute to read.
     * @return <code>true</code>, if the attribute exists for the object.
     */
    public boolean hasAttribute(final String objectPath, final String attributeName);

    /**
     * Returns the names of the attributes of the given <var>objectPath</var>.
     * 
     * @param objectPath The name (including path information) of the object (data set or group) to
     *            return the attributes for.
     */
    public List<String> getAttributeNames(final String objectPath);

    /**
     * Returns the names of all attributes of the given <var>objectPath</var>.
     * <p>
     * This may include attributes that are used internally by the library and are not supposed to
     * be changed by application programmers.
     * 
     * @param objectPath The name (including path information) of the object (data set or group) to
     *            return the attributes for.
     */
    public List<String> getAllAttributeNames(final String objectPath);

    /**
     * Returns the information about a data set as a {@link HDF5DataTypeInformation} object.
     * 
     * @param objectPath The name (including path information) of the object that has the attribute
     *            to return information about.
     * @param attributeName The name of the attribute to get information about.
     */
    public HDF5DataTypeInformation getAttributeInformation(final String objectPath,
            final String attributeName);

    /**
     * Returns the information about a data set as a {@link HDF5DataTypeInformation} object.
     * 
     * @param objectPath The name (including path information) of the object that has the attribute
     *            to return information about.
     * @param attributeName The name of the attribute to get information about.
     * @param dataTypeInfoOptions The options on which information to get about the member data
     *            types.
     */
    public HDF5DataTypeInformation getAttributeInformation(final String objectPath,
            final String attributeName, final DataTypeInfoOptions dataTypeInfoOptions);

    /**
     * Returns the information about a data set as a {@link HDF5DataSetInformation} object. It is a
     * failure condition if the <var>objectPath</var> does not exist or does not identify a data
     * set.
     * 
     * @param objectPath The name (including path information) of the data set to return information
     *            about.
     */
    public HDF5DataSetInformation getDataSetInformation(final String objectPath);

    // /////////////////////
    // Data Sets
    // /////////////////////

    /**
     * Returns the information about a data set as a {@link HDF5DataSetInformation} object. It is a
     * failure condition if the <var>objectPath</var> does not exist or does not identify a data
     * set.
     * 
     * @param objectPath The name (including path information) of the data set to return information
     *            about.
     * @param dataTypeInfoOptions The options on which information to get about the member data
     *            types.
     */
    public HDF5DataSetInformation getDataSetInformation(final String objectPath,
            final DataTypeInfoOptions dataTypeInfoOptions);

    /**
     * Returns the total size (in bytes) of <var>objectPath</var>. It is a failure condition if the
     * <var>objectPath</var> does not exist or does not identify a data set. This method follows
     * symbolic links.
     */
    public long getSize(final String objectPath);

    /**
     * Returns the total number of elements of <var>objectPath</var>. It is a failure condition if
     * the <var>objectPath</var> does not exist or does not identify a data set. This method follows
     * symbolic links.
     */
    public long getNumberOfElements(final String objectPath);

    /**
     * Returns the size of one element of <var>objectPath</var>. It is a failure condition if the
     * <var>objectPath</var> does not exist or does not identify a data set. This method follows
     * symbolic links.
     */
    public int getElementSize(final String objectPath);

    /**
     * Returns the rank of the space of <var>objectPath</var> (0 if this is a scalar space). It is a
     * failure condition if the <var>objectPath</var> does not exist or does not identify a data
     * set. This method follows symbolic links.
     */
    public int getSpaceRank(final String objectPath);

    /**
     * Returns the dimensions of the space of <var>objectPath</var> (empty if this is a scalar
     * space). It is a failure condition if the <var>objectPath</var> does not exist or does not
     * identify a data set. This method follows symbolic links.
     */
    public long[] getSpaceDimensions(final String objectPath);

    /**
     * Returns the rank of the array of <var>objectPath</var> (0 if this is no array type). It is a
     * failure condition if the <var>objectPath</var> does not exist or does not identify a data
     * set. This method follows symbolic links.
     */
    public int getArrayRank(final String objectPath);

    /**
     * Returns the dimensions of <var>objectPath</var>(empty if this isno array type). It is a
     * failure condition if the <var>objectPath</var> does not exist or does not identify a data
     * set. This method follows symbolic links.
     */
    public int[] getArrayDimensions(final String objectPath);

    /**
     * Returns the rank of this data set of <var>objectPath</var>. This combines the space rank and
     * the array rank into one rank. It is a failure condition if the <var>objectPath</var> does not
     * exist or does not identify a data set. This method follows symbolic links.
     */
    public int getRank(final String objectPath);

    /**
     * Returns the dimensions of <var>objectPath</var>. This combines the space dimensions and the
     * array dimensions into one rank. It is a failure condition if the <var>objectPath</var> does
     * not exist or does not identify a data set. This method follows symbolic links.
     */
    public long[] getDimensions(final String objectPath);

    // /////////////////////
    // Copies
    // /////////////////////

    /**
     * Copies the <var>sourceObject</var> to the <var>destinationObject</var> of the HDF5 file
     * represented by the <var>destinationWriter</var>. If <var>destiantionObject</var> ends with
     * "/", it will be considered a group and the name of <var>sourceObject</var> will be appended.
     */
    public void copy(String sourceObject, IHDF5Writer destinationWriter, String destinationObject);

    /**
     * Copies the <var>sourceObject</var> to the root group of the HDF5 file represented by the
     * <var>destinationWriter</var>.
     */
    public void copy(String sourceObject, IHDF5Writer destinationWriter);

    /**
     * Copies all objects of the file represented by this reader to the root group of the HDF5 file
     * represented by the <var>destinationWriter</var>.
     */
    public void copyAll(IHDF5Writer destinationWriter);

    // /////////////////////
    // Groups
    // /////////////////////

    /**
     * Returns the members of <var>groupPath</var>. The order is <i>not</i> well defined.
     * 
     * @param groupPath The path of the group to get the members for.
     * @throws IllegalArgumentException If <var>groupPath</var> is not a group.
     */
    public List<String> getGroupMembers(final String groupPath);

    /**
     * Returns all members of <var>groupPath</var>, including internal groups that may be used by
     * the library to do house-keeping. The order is <i>not</i> well defined.
     * 
     * @param groupPath The path of the group to get the members for.
     * @throws IllegalArgumentException If <var>groupPath</var> is not a group.
     */
    public List<String> getAllGroupMembers(final String groupPath);

    /**
     * Returns the paths of the members of <var>groupPath</var> (including the parent). The order is
     * <i>not</i> well defined.
     * 
     * @param groupPath The path of the group to get the member paths for.
     * @throws IllegalArgumentException If <var>groupPath</var> is not a group.
     */
    public List<String> getGroupMemberPaths(final String groupPath);

    /**
     * Returns the link information about the members of <var>groupPath</var>. The order is
     * <i>not</i> well defined.
     * 
     * @param groupPath The path of the group to get the members for.
     * @param readLinkTargets If <code>true</code>, for symbolic links the link targets will be
     *            available via {@link HDF5LinkInformation#tryGetSymbolicLinkTarget()}.
     * @throws IllegalArgumentException If <var>groupPath</var> is not a group.
     */
    public List<HDF5LinkInformation> getGroupMemberInformation(final String groupPath,
            boolean readLinkTargets);

    /**
     * Returns the link information about all members of <var>groupPath</var>. The order is
     * <i>not</i> well defined.
     * <p>
     * This may include attributes that are used internally by the library and are not supposed to
     * be changed by application programmers.
     * 
     * @param groupPath The path of the group to get the members for.
     * @param readLinkTargets If <code>true</code>, the link targets will be read for symbolic
     *            links.
     * @throws IllegalArgumentException If <var>groupPath</var> is not a group.
     */
    public List<HDF5LinkInformation> getAllGroupMemberInformation(final String groupPath,
            boolean readLinkTargets);

    // /////////////////////
    // Types
    // /////////////////////

    /**
     * Returns the data type variant of <var>objectPath</var>, or <code>null</code>, if no type
     * variant is defined for this <var>objectPath</var>.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @return The data type variant or <code>null</code>.
     */
    public HDF5DataTypeVariant tryGetTypeVariant(final String objectPath);

    /**
     * Returns the data type variant of <var>attributeName</var> of object <var>objectPath</var>, or
     * <code>null</code>, if no type variant is defined for this <var>objectPath</var>.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @return The data type variant or <code>null</code>.
     */
    public HDF5DataTypeVariant tryGetTypeVariant(final String objectPath, String attributeName);

    /**
     * Returns the path of the data type of the data set <var>objectPath</var>, or <code>null</code>
     * , if this data set is not of a named data type.
     */
    public String tryGetDataTypePath(final String objectPath);

    /**
     * Returns the path of the data <var>type</var>, or <code>null</code>, if <var>type</var> is not
     * a named data type.
     */
    public String tryGetDataTypePath(HDF5DataType type);

}
