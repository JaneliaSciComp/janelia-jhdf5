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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Arrays;

import org.apache.commons.lang3.ArrayUtils;

/**
 * A multi-dimensional array of generic type <code>T</code>.
 * 
 * @author Bernd Rinn
 */
public class MDArray<T> extends MDAbstractArray<T>
{
    private static final long serialVersionUID = 1L;

    private T[] flattenedArray;

    /**
     * Creates an empty {@link MDArray} with the <var>dimensions</var>. Convenience method if
     * <var>dimensions</var> are available as {@code long[]}.
     */
    public MDArray(Class<T> componentClass, long[] dimensions)
    {
        this(createArray(componentClass, getLength(dimensions, 0)), toInt(dimensions), false);
    }

    /**
     * Creates an empty {@link MDArray} with the <var>dimensions</var>. If
     * <code>capacityHyperRows > dimensions[0]</code>, then it will create an array with a capacity
     * of <var>capacityHyperRows</var> hyper-rows. Convenience method if <var>dimensions</var> are
     * available as {@code long[]}.
     */
    public MDArray(Class<T> componentClass, long[] dimensions, long capacityHyperRows)
    {
        this(createArray(componentClass, getLength(dimensions, capacityHyperRows)),
                toInt(dimensions), false);
    }

    /**
     * Creates a {@link MDArray} from the given {@code flattenedArray} and {@code dimensions}. It is
     * checked that the arguments are compatible. Convenience method if <var>dimensions</var> are
     * available as {@code long[]}.
     */
    public MDArray(T[] flattenedArray, long[] dimensions)
    {
        this(flattenedArray, toInt(dimensions), true);
    }

    /**
     * Creates a {@link MDArray} from the given <var>flattenedArray</var> and <var>dimensions</var>.
     * If <var>checkDimensions</var> is {@code true}, it is checked that the arguments are
     * compatible. Convenience method if <var>dimensions</var> are available as {@code long[]}.
     */
    public MDArray(T[] flattenedArray, long[] dimensions, boolean checkdimensions)
    {
        this(flattenedArray, toInt(dimensions), checkdimensions);
    }

    /**
     * Creates an empty {@link MDArray} with the <var>dimensions</var>.
     */
    public MDArray(Class<T> componentClass, int[] dimensions)
    {
        this(createArray(componentClass, getLength(dimensions, 0)), dimensions, false);
    }

    /**
     * Creates an empty {@link MDArray} with the <var>dimensions</var>. If
     * <code>capacityHyperRows > dimensions[0]</code>, then it will create an array with a capacity
     * of <var>capacityHyperRows</var> hyper-rows.
     */
    public MDArray(Class<T> componentClass, int[] dimensions, int capacityHyperRows)
    {
        this(createArray(componentClass, getLength(dimensions, capacityHyperRows)), dimensions,
                false);
    }

    /**
     * Creates a {@link MDArray} from the given {@code flattenedArray} and {@code dimensions}. It is
     * checked that the arguments are compatible.
     */
    public MDArray(T[] flattenedArray, int[] dimensions)
    {
        this(flattenedArray, dimensions, true);
    }

    /**
     * Creates a {@link MDArray} from the given <var>flattenedArray</var> and <var>dimensions</var>.
     * If <var>checkDimensions</var> is {@code true}, it is checked that the arguments are
     * compatible.
     */
    public MDArray(T[] flattenedArray, int[] dimensions, boolean checkdimensions)
    {
        super(dimensions, flattenedArray.length, 0);
        assert flattenedArray != null;

        if (checkdimensions)
        {
            final int expectedLength = getLength(dimensions, 0);
            if (flattenedArray.length != expectedLength)
            {
                throw new IllegalArgumentException("Actual array length " + flattenedArray.length
                        + " does not match expected length " + expectedLength + ".");
            }
        }
        this.flattenedArray = flattenedArray;
    }

    @SuppressWarnings("unchecked")
    private static <V> V[] createArray(Class<V> componentClass, final int vectorLength)
    {
        final V[] value = (V[]) java.lang.reflect.Array.newInstance(componentClass, vectorLength);
        return value;
    }

    @Override
    public int capacity()
    {
        return flattenedArray.length;
    }

    @Override
    public T getAsObject(int... indices)
    {
        return get(indices);
    }

    @Override
    public void setToObject(T value, int... indices)
    {
        set(value, indices);
    }

    @Override
    public T getAsObject(int linearIndex)
    {
        return get(linearIndex);
    }

    @Override
    public void setToObject(T value, int linearIndex)
    {
        set(value, linearIndex);
    }

    @Override
    public T[] getAsFlatArray()
    {
        return flattenedArray;
    }

    @Override
    public T[] getCopyAsFlatArray()
    {
        return toTArray(ArrayUtils.subarray(flattenedArray, 0, dimensions[0] * hyperRowLength));
    }

    @Override
    protected void adaptCapacityHyperRows()
    {
        final T[] oldArray = this.flattenedArray;
        this.flattenedArray =
                toTArray(createArray(oldArray.getClass().getComponentType(), capacityHyperRows
                        * hyperRowLength));
        System.arraycopy(oldArray, 0, flattenedArray, 0,
                Math.min(oldArray.length, flattenedArray.length));
    }

    @SuppressWarnings("unchecked")
    private T[] toTArray(Object obj)
    {
        return (T[]) obj;
    }

    /**
     * Returns the value of array at the position defined by <var>indices</var>.
     */
    public T get(int... indices)
    {
        return flattenedArray[computeIndex(indices)];
    }

    /**
     * Returns the value of a one-dimensional array at the position defined by <var>index</var>.
     * <p>
     * <b>Do not call for arrays other than one-dimensional!</b>
     */
    public T get(int index)
    {
        return flattenedArray[index];
    }

    /**
     * Returns the value of a two-dimensional array at the position defined by <var>indexX</var> and
     * <var>indexY</var>.
     * <p>
     * <b>Do not call for arrays other than two-dimensional!</b>
     */
    public T get(int indexX, int indexY)
    {
        return flattenedArray[computeIndex(indexX, indexY)];
    }

    /**
     * Returns the value of a three-dimensional array at the position defined by <var>indexX</var>,
     * <var>indexY</var> and <var>indexZ</var>.
     * <p>
     * <b>Do not call for arrays other than three-dimensional!</b>
     */
    public T get(int indexX, int indexY, int indexZ)
    {
        return flattenedArray[computeIndex(indexX, indexY, indexZ)];
    }

    /**
     * Sets the <var>value</var> of array at the position defined by <var>indices</var>.
     */
    public void set(T value, int... indices)
    {
        flattenedArray[computeIndex(indices)] = value;
    }

    /**
     * Sets the <var>value</var> of a one-dimension array at the position defined by
     * <var>index</var>.
     * <p>
     * <b>Do not call for arrays other than one-dimensional!</b>
     */
    public void set(T value, int index)
    {
        flattenedArray[index] = value;
    }

    /**
     * Sets the <var>value</var> of a two-dimensional array at the position defined by
     * <var>indexX</var> and <var>indexY</var>.
     * <p>
     * <b>Do not call for arrays other than two-dimensional!</b>
     */
    public void set(T value, int indexX, int indexY)
    {
        flattenedArray[computeIndex(indexX, indexY)] = value;
    }

    /**
     * Sets the <var>value</var> of a three-dimensional array at the position defined by
     * <var>indexX</var>, <var>indexY</var> and <var>indexZ</var>.
     * <p>
     * <b>Do not call for arrays other than three-dimensional!</b>
     */
    public void set(T value, int indexX, int indexY, int indexZ)
    {
        flattenedArray[computeIndex(indexX, indexY, indexZ)] = value;
    }

    /**
     * Returns the component type of this array.
     */
    @SuppressWarnings("unchecked")
    public Class<T> getComponentClass()
    {
        return (Class<T>) flattenedArray.getClass().getComponentType();
    }

    //
    // Object
    //

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + Arrays.hashCode(getValuesAsFlatArray());
        result = prime * result + Arrays.hashCode(dimensions);
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        if (obj == null)
        {
            return false;
        }
        if (getClass() != obj.getClass())
        {
            return false;
        }
        final MDArray<T> other = toMDArray(obj);
        if (Arrays.equals(getValuesAsFlatArray(), other.getValuesAsFlatArray()) == false)
        {
            return false;
        }
        if (Arrays.equals(dimensions, other.dimensions) == false)
        {
            return false;
        }
        return true;
    }

    private T[] getValuesAsFlatArray()
    {
        return (dimensions[0] < capacityHyperRows) ? getCopyAsFlatArray() : getAsFlatArray(); 
    }
    
    @SuppressWarnings("unchecked")
    private MDArray<T> toMDArray(Object obj)
    {
        return (MDArray<T>) obj;
    }

    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException
    {
        stream.defaultReadObject();
        if (hyperRowLength == 0)
        {
            this.hyperRowLength = computeHyperRowLength(dimensions);
        }
        if (capacityHyperRows == 0)
        {
            this.capacityHyperRows = dimensions[0];
        }
        if (size == 0)
        {
            this.size = hyperRowLength * dimensions[0];
        }
    }

}
