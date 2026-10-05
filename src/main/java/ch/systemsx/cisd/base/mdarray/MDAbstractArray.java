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

package ch.systemsx.cisd.base.mdarray;

import java.io.Serializable;
import java.util.Iterator;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.ClassUtils;

/**
 * Base class of a multi-dimensional array. The <var>dimensions</var> of an array are provided
 * separately from the data as a <code>int[]</code>.
 * <p>
 * The array can grow or shrink in the first dimension (<var>dimensions[0]</var>). Dimensions
 * <code>1...n</code> are static. They are said to form a <i>hyper-row</i>.
 * 
 * @author Bernd Rinn
 */
public abstract class MDAbstractArray<T> implements Serializable,
        Iterable<MDAbstractArray<T>.ArrayEntry>
{
    private static final long serialVersionUID = 1L;

    protected final int[] dimensions;

    protected int hyperRowLength;

    protected int capacityHyperRows;

    protected int size;

    /**
     * A class to represent an entry (index and value) of a {@link MDArray}, used for iteration.
     */
    public class ArrayEntry
    {
        private final int linearIndex;

        ArrayEntry(int linearIndex)
        {
            this.linearIndex = linearIndex;
        }

        /**
         * The multi-dimensional index of this entry.
         */
        public int[] getIndex()
        {
            return computeReverseIndex(linearIndex);
        }

        /**
         * The linear index of this entry.
         */
        public int getLinearIndex()
        {
            return linearIndex;
        }

        /**
         * The value of this entry.
         */
        public T getValue()
        {
            return getAsObject(linearIndex);
        }
    }

    protected MDAbstractArray(int[] dimensions, int arrayLength, int capacityHyperRows)
    {
        assert dimensions != null;

        this.dimensions = dimensions;
        this.hyperRowLength = computeHyperRowLength(dimensions);
        if (hyperRowLength == 0)
        {
            this.capacityHyperRows = 0;
            this.size = 0;
        } else
        {
            if (arrayLength % hyperRowLength != 0)
            {
                throw new IllegalArgumentException("Actual array length " + arrayLength
                        + " does not match hyper-row length " + hyperRowLength + ".");
            }
            this.capacityHyperRows =
                    (capacityHyperRows > 0) ? capacityHyperRows : Math.max(dimensions[0],
                            arrayLength / hyperRowLength);
            this.size = dimensions[0] * hyperRowLength;
        }
    }

    protected MDAbstractArray(MDAbstractArray<T> template)
    {
        this.dimensions = template.dimensions.clone();
        this.hyperRowLength = template.hyperRowLength;
        this.capacityHyperRows = template.capacityHyperRows;
        this.size = template.size;
    }

    protected int computeHyperRowLength(@SuppressWarnings("hiding")
    int[] dimensions)
    {
        int hyperRowLen = 1;
        for (int i = 1; i < dimensions.length; ++i)
        {
            hyperRowLen *= dimensions[i];
        }
        return hyperRowLen;
    }

    /**
     * Returns the rank of the array.
     */
    public int rank()
    {
        return dimensions.length;
    }

    /**
     * Returns the extent of the array along its <var>dim</var>-th axis.
     */
    public int size(int dim)
    {
        assert dim < dimensions.length;

        return dimensions[dim];
    }

    /**
     * Returns a copy of the dimensions of the multi-dimensional array.
     */
    public int[] dimensions()
    {
        return dimensions.clone();
    }

    /**
     * Returns a copy of the dimensions of the multi-dimensional array as <code>long[]</code>.
     */
    public long[] longDimensions()
    {
        final long[] dimensionsCopy = new long[dimensions.length];
        for (int i = 0; i < dimensionsCopy.length; ++i)
        {
            dimensionsCopy[i] = dimensions[i];
        }
        return dimensionsCopy;
    }

    /**
     * Returns the number of elements in the array.
     */
    public int size()
    {
        return size;
    }
    
    /**
     * Returns the current number of hyper rows of of this array.
     */
    public int numberOfHyperRows()
    {
        return numberOfHyperRows();
    }

    /**
     * Return an object which has the same value as the element of the array specified by
     * <var>linearIndex</var>.
     * 
     * @param linearIndex The index in the linear array returned by {@link #getAsFlatArray()}.
     * @return The value at the specified index.
     */
    public abstract T getAsObject(int linearIndex);

    /**
     * Return an object which has the same value as the element of the array specified by
     * <var>indices</var>.
     */
    public abstract T getAsObject(int... indices);

    /**
     * Sets the element of the array specified by <var>indices</var> to the particular
     * <var>value</var>.
     */
    public abstract void setToObject(T value, int... indices);

    /**
     * Sets the element of the array specified by <var>linearIndex</var> to the particular
     * <var>value</var>.
     * 
     * @param value The new value to set.
     * @param linearIndex The index in the linear array returned by {@link #getAsFlatArray()}.
     */
    public abstract void setToObject(T value, int linearIndex);

    /**
     * Returns the array in flattened form. Changes to the returned object will change the
     * multi-dimensional array directly.
     */
    public abstract Object getAsFlatArray();

    /**
     * Returns a copy of the array in flattened form. Changes to the returned object will <i>not</i>
     * change the multi-dimensional array directly.
     */
    public abstract Object getCopyAsFlatArray();

    protected abstract void adaptCapacityHyperRows();

    /**
     * Returns the capacity of elements in the array.
     */
    public abstract int capacity();

    /**
     * Increase the number of hyper-rows by <var>count</var>. Doubles the capacity if needed.
     * 
     * @return The new number of rows.
     */
    public int incNumberOfHyperRows(int count)
    {
        dimensions[0] += count;
        if (dimensions[0] > capacityHyperRows)
        {
            capacityHyperRows = Math.max(capacityHyperRows * 2, dimensions[0]);
            adaptCapacityHyperRows();
        }
        size += count * hyperRowLength;
        return dimensions[0];
    }

    /**
     * Decrease the number of hyper-rows by <var>count</var>.
     * 
     * @return The new number of rows.
     */
    public int decNumberOfHyperRows(int count)
    {
        dimensions[0] -= count;
        size -= count * hyperRowLength;
        return dimensions[0];
    }

    /**
     * Computes the linear index for the multi-dimensional <var>indices</var> provided.
     */
    public int computeIndex(int... indices)
    {
        assert indices != null;
        assert indices.length == dimensions.length;

        int index = indices[0];
        for (int i = 1; i < indices.length; ++i)
        {
            index = index * dimensions[i] + indices[i];
        }
        return index;
    }

    /**
     * Computes the multi-dimensional index from the <var>linearIndex</var>.
     */
    public int[] computeReverseIndex(int linearIndex)
    {
        final int[] index = new int[dimensions.length];
        int workIndex = linearIndex;
        int blockSize = size;
        for (int i = 0; i < dimensions.length; ++i)
        {
            blockSize /= dimensions[i];
            index[i] = workIndex / blockSize;
            workIndex = workIndex - index[i] * blockSize;
        }
        return index;
    }

    /**
     * Computes the linear index for the two-dimensional (<var>indexX, indexY</var>) provided.
     */
    public int computeIndex(int indexX, int indexY)
    {
        assert 2 == dimensions.length;

        return dimensions[1] * indexX + indexY;
    }

    /**
     * Computes the linear index for the three-dimensional (<var>indexX, indexY, indexZ</var>)
     * provided.
     */
    public int computeIndex(int indexX, int indexY, int indexZ)
    {
        assert 3 == dimensions.length;

        return dimensions[2] * (dimensions[1] * indexX + indexY) + indexZ;
    }

    /**
     * Converts the <var>dimensions</var> from <code>long[]</code> to <code>int[]</code>.
     */
    public static int[] toInt(final long[] dimensions)
    {
        assert dimensions != null;

        final int[] result = new int[dimensions.length];
        for (int i = 0; i < result.length; ++i)
        {
            result[i] = (int) dimensions[i];
            if (result[i] != dimensions[i])
            {
                throw new IllegalArgumentException("Dimension " + i + "  is too large ("
                        + dimensions[i] + ")");
            }
        }
        return result;
    }

    /**
     * Converts the <var>dimensions</var> from <code>int[]</code> to <code>long[]</code>.
     */
    public static long[] toLong(final int[] dimensions)
    {
        assert dimensions != null;

        final long[] result = new long[dimensions.length];
        for (int i = 0; i < result.length; ++i)
        {
            result[i] = dimensions[i];
        }
        return result;
    }

    /**
     * Returns the one-dimensional length of the multi-dimensional array defined by
     * <var>dimensions</var>.
     * 
     * @throws IllegalArgumentException If <var>dimensions</var> overflow the <code>int</code> type.
     */
    public static int getLength(final int[] dimensions)
    {
        return getLength(dimensions, 0);
    }

    /**
     * Returns the one-dimensional length of the multi-dimensional array defined by
     * <var>dimensions</var>. If <code>capacityHyperRows > dimensions[0]</code>, then it will
     * replace <var>dimensions[0]</var> by <var>capacityHyperRows</var>
     * 
     * @throws IllegalArgumentException If <var>dimensions</var> overflow the <code>int</code> type.
     */
    public static int getLength(final int[] dimensions, int capacityHyperRows)
    {
        assert dimensions != null;

        if (dimensions.length == 0)
        {
            return 0;
        }
        long length = Math.max(capacityHyperRows, dimensions[0]);
        for (int i = 1; i < dimensions.length; ++i)
        {
            length *= dimensions[i];
        }
        int intLength = (int) length;
        if (length != intLength)
        {
            throw new IllegalArgumentException("Length is too large (" + length + ")");
        }
        return intLength;
    }

    /**
     * Returns the one-dimensional length of the multi-dimensional array defined by
     * <var>dimensions</var>.
     * 
     * @throws IllegalArgumentException If <var>dimensions</var> overflow the <code>int</code> type.
     */
    public static int getLength(final long[] dimensions)
    {
        return getLength(dimensions, 0);
    }

    /**
     * Returns the one-dimensional length of the multi-dimensional array defined by
     * <var>dimensions</var>. If <code>capacityHyperRows > dimensions[0]</code>, then it will
     * replace <var>dimensions[0]</var> by <var>capacityHyperRows</var>
     * 
     * @throws IllegalArgumentException If <var>dimensions</var> overflow the <code>int</code> type.
     */
    public static int getLength(final long[] dimensions, long capacityHyperRows)
    {
        assert dimensions != null;

        if (dimensions.length == 0) // NULL data space needs to be treated differently
        {
            return 0;
        }
        long length = Math.max(capacityHyperRows, dimensions[0]);
        for (int i = 1; i < dimensions.length; ++i)
        {
            length *= dimensions[i];
        }
        int intLength = (int) length;
        if (length != intLength)
        {
            throw new IllegalArgumentException("Length is too large (" + length + ")");
        }
        return intLength;
    }

    //
    // Iterable
    //

    @Override
    public Iterator<ArrayEntry> iterator()
    {
        return new Iterator<ArrayEntry>()
            {
                int linearIndex = 0;

                @Override
                public boolean hasNext()
                {
                    return linearIndex < size;
                }

                @Override
                public ArrayEntry next()
                {
                    final ArrayEntry next = new ArrayEntry(linearIndex);
                    ++linearIndex;
                    return next;
                }

                @Override
                public void remove()
                {
                    throw new UnsupportedOperationException();
                }

            };
    }

    //
    // Object
    //

    @Override
    public String toString()
    {
        final int length = getLength(dimensions, 0);
        final StringBuilder b = new StringBuilder();
        b.append(ClassUtils.getShortCanonicalName(this.getClass()));
        b.append('(');
        b.append(ArrayUtils.toString(dimensions));
        b.append(')');
        if (length <= 100)
        {
            b.append(": ");
            if (dimensions[0] < capacityHyperRows)
            {
                b.append(ArrayUtils.toString(getCopyAsFlatArray()));
            } else
            {
                b.append(ArrayUtils.toString(getAsFlatArray()));
            }
        }
        return b.toString();
    }

}
