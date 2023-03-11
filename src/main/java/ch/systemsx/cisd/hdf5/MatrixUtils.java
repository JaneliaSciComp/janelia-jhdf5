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

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Map;

import hdf.hdf5lib.exceptions.HDF5JavaException;

import ch.systemsx.cisd.base.mdarray.MDAbstractArray;
import ch.systemsx.cisd.base.mdarray.MDArray;

/**
 * Utilities for working with primitive matrices.
 * 
 * @author Bernd Rinn
 */
public final class MatrixUtils
{

    private MatrixUtils()
    {
        // Cannot be instantiated
    }

    /**
     * Helper method for creating <code>int[]</code> array for the block dimensions. 
     */
    public static int[] dims(int... dim)
    {
        return dim;
    }
    
    /**
     * Helper method for creating <code>long[]</code> array for the array dimensions. 
     */
    public static long[] dims(long... dim)
    {
        return dim;
    }
    
    /**
     * Helper method for creating <code>long[]</code> array for the array dimensions. 
     */
    public static long[] ldims(long... dim)
    {
        return dim;
    }
    
    static void checkMDArrayDimensions(final String name, final int[] dimensions,
            final MDAbstractArray<?> array)
    {
        if (Arrays.equals(dimensions, array.dimensions()) == false)
        {
            throw new IllegalArgumentException("The member '" + name + "' has dimensions "
                    + Arrays.toString(array.dimensions()) + " but is supposed to have dimensions "
                    + Arrays.toString(dimensions) + ".");
        }
    }

    static void checkMatrixDimensions(final String name, final int[] dimensions, final Object matrix)
    {
        final int dimX = Array.getLength(matrix);
        final int dimY = Array.getLength(Array.get(matrix, 0));
        if (dimensions.length != 2 || dimensions[0] != dimX || dimensions[1] != dimY)
        {
            throw new IllegalArgumentException("The member '" + name + "' has dimensions [" + dimX
                    + "," + dimY + "]." + " but is supposed to have dimensions "
                    + Arrays.toString(dimensions) + ".");
        }
    }

    static float[] flatten(float[][] matrix)
    {
        if (matrix.length == 0)
        {
            throw new IllegalArgumentException("Matrix must not have a length of 0.");
        }
        final int dimY = matrix.length;
        final int dimX = matrix[0].length;
        for (int i = 1; i < dimY; ++i)
        {
            if (matrix[i].length != dimX)
            {
                throw new IllegalArgumentException(
                        "All rows in matrix need to have the same number of columns.");
            }
        }
        final float[] result = new float[dimX * dimY];
        for (int i = 0; i < dimY; ++i)
        {
            System.arraycopy(matrix[i], 0, result, i * dimX, dimX);
        }
        return result;
    }

    static float[][] shapen(float[] matrixData, int[] dims)
    {
        final int dimY = dims[0];
        final int dimX = dims[1];
        final float[][] result = new float[dimY][dimX];
        for (int i = 0; i < dimY; ++i)
        {
            System.arraycopy(matrixData, i * dimX, result[i], 0, dimX);
        }
        return result;
    }

    static double[] flatten(double[][] matrix)
    {
        if (matrix.length == 0)
        {
            throw new IllegalArgumentException("Matrix must not have a length of 0.");
        }
        final int dimY = matrix.length;
        final int dimX = matrix[0].length;
        for (int i = 1; i < dimY; ++i)
        {
            if (matrix[i].length != dimX)
            {
                throw new IllegalArgumentException(
                        "All rows in matrix need to have the same number of columns.");
            }
        }
        final double[] result = new double[dimX * dimY];
        for (int i = 0; i < dimY; ++i)
        {
            System.arraycopy(matrix[i], 0, result, i * dimX, dimX);
        }
        return result;
    }

    static double[][] shapen(double[] matrixData, int[] dims)
    {
        final int dimY = dims[0];
        final int dimX = dims[1];
        final double[][] result = new double[dimY][dimX];
        for (int i = 0; i < dimY; ++i)
        {
            System.arraycopy(matrixData, i * dimX, result[i], 0, dimX);
        }
        return result;
    }

    static int[] flatten(int[][] matrix)
    {
        if (matrix.length == 0)
        {
            throw new IllegalArgumentException("Matrix must not have a length of 0.");
        }
        final int dimY = matrix.length;
        final int dimX = matrix[0].length;
        for (int i = 1; i < dimY; ++i)
        {
            if (matrix[i].length != dimX)
            {
                throw new IllegalArgumentException(
                        "All rows in matrix need to have the same number of columns.");
            }
        }
        final int[] result = new int[dimX * dimY];
        for (int i = 0; i < dimY; ++i)
        {
            System.arraycopy(matrix[i], 0, result, i * dimX, dimX);
        }
        return result;
    }

    static int[][] shapen(int[] matrixData, int[] dims)
    {
        final int dimY = dims[0];
        final int dimX = dims[1];
        final int[][] result = new int[dimY][dimX];
        for (int i = 0; i < dimY; ++i)
        {
            System.arraycopy(matrixData, i * dimX, result[i], 0, dimX);
        }
        return result;
    }

    static long[] flatten(long[][] matrix)
    {
        if (matrix.length == 0)
        {
            throw new IllegalArgumentException("Matrix must not have a length of 0.");
        }
        final int dimY = matrix.length;
        final int dimX = matrix[0].length;
        for (int i = 1; i < dimY; ++i)
        {
            if (matrix[i].length != dimX)
            {
                throw new IllegalArgumentException(
                        "All rows in matrix need to have the same number of columns.");
            }
        }
        final long[] result = new long[dimX * dimY];
        for (int i = 0; i < dimY; ++i)
        {
            System.arraycopy(matrix[i], 0, result, i * dimX, dimX);
        }
        return result;
    }

    static long[][] shapen(long[] matrixData, int[] dims)
    {
        final int dimY = dims[0];
        final int dimX = dims[1];
        final long[][] result = new long[dimY][dimX];
        for (int i = 0; i < dimY; ++i)
        {
            System.arraycopy(matrixData, i * dimX, result[i], 0, dimX);
        }
        return result;
    }

    static short[] flatten(short[][] matrix)
    {
        if (matrix.length == 0)
        {
            throw new IllegalArgumentException("Matrix must not have a length of 0.");
        }
        final int dimY = matrix.length;
        final int dimX = matrix[0].length;
        for (int i = 1; i < dimY; ++i)
        {
            if (matrix[i].length != dimX)
            {
                throw new IllegalArgumentException(
                        "All rows in matrix need to have the same number of columns.");
            }
        }
        final short[] result = new short[dimX * dimY];
        for (int i = 0; i < dimY; ++i)
        {
            System.arraycopy(matrix[i], 0, result, i * dimX, dimX);
        }
        return result;
    }

    static short[][] shapen(short[] matrixData, int[] dims)
    {
        final int dimY = dims[0];
        final int dimX = dims[1];
        final short[][] result = new short[dimY][dimX];
        for (int i = 0; i < dimY; ++i)
        {
            System.arraycopy(matrixData, i * dimX, result[i], 0, dimX);
        }
        return result;
    }

    static byte[] flatten(byte[][] matrix)
    {
        if (matrix.length == 0)
        {
            throw new IllegalArgumentException("Matrix must not have a length of 0.");
        }
        final int dimY = matrix.length;
        final int dimX = matrix[0].length;
        for (int i = 1; i < dimY; ++i)
        {
            if (matrix[i].length != dimX)
            {
                throw new IllegalArgumentException(
                        "All rows in matrix need to have the same number of columns.");
            }
        }
        final byte[] result = new byte[dimX * dimY];
        for (int i = 0; i < dimY; ++i)
        {
            System.arraycopy(matrix[i], 0, result, i * dimX, dimX);
        }
        return result;
    }

    static byte[][] shapen(byte[] matrixData, int[] dims)
    {
        final int dimY = dims[0];
        final int dimX = dims[1];
        final byte[][] result = new byte[dimY][dimX];
        for (int i = 0; i < dimY; ++i)
        {
            System.arraycopy(matrixData, i * dimX, result[i], 0, dimX);
        }
        return result;
    }

    static boolean incrementIdx(int[] idx, int[] dims, int[] offset)
    {
        int d = idx.length - 1;
        while (++idx[d] >= offset[d] + dims[d])
        {
            idx[d] = offset[d];
            if (d == 0)
            {
                return false;
            } else
            {
                --d;
            }
        }
        return true;
    }

    static int[] concat(int[] array1, int[] array2)
    {
        if (array1.length == 0)
        {
            return array2;
        }
        if (array2.length == 0)
        {
            return array1;
        }
        final int[] result = new int[array1.length + array2.length];
        System.arraycopy(array1, 0, result, 0, array1.length);
        System.arraycopy(array2, 0, result, array1.length, array2.length);
        return result;
    }

    static long[] concat(long[] array1, int[] array2)
    {
        if (array1.length == 0)
        {
            return MDArray.toLong(array2);
        }
        if (array2.length == 0)
        {
            return array1;
        }
        final long[] result = new long[array1.length + array2.length];
        System.arraycopy(array1, 0, result, 0, array1.length);
        for (int i = 0; i < array2.length; ++i)
        {
            result[array1.length + i] = array2[i];
        }
        return result;
    }

    static int cardinalityBoundIndices(Map<?, ?> boundIndices)
    {
        return boundIndices.size();
    }

    static int cardinalityBoundIndices(long[] boundIndices)
    {
        int card = 0;
        for (int i = 0; i < boundIndices.length; ++i)
        {
            if (boundIndices[i] >= 0)
            {
                ++card;
            }
        }
        return card;
    }

    static void createFullBlockDimensionsAndOffset(int[] blockDimensions, long[] offsetOrNull,
            Map<Integer, Long> boundIndices, final long[] fullDimensions,
            final int[] fullBlockDimensions, final long[] fullOffset)
    {
        createFullBlockDimensionsAndOffset(blockDimensions, offsetOrNull, boundIndices,
                fullDimensions.length, fullDimensions, fullBlockDimensions, fullOffset);
    }

    static void createFullBlockDimensionsAndOffset(int[] blockDimensions, long[] offsetOrNull,
            Map<Integer, Long> boundIndices, final int fullRank, final int[] fullBlockDimensions,
            final long[] fullOffset)
    {
        createFullBlockDimensionsAndOffset(blockDimensions, offsetOrNull, boundIndices, fullRank,
                null, fullBlockDimensions, fullOffset);
    }

    static void createFullBlockDimensionsAndOffset(int[] blockDimensions, long[] offsetOrNull,
            Map<Integer, Long> boundIndices, final int fullRank, final long[] fullDimensionsOrNull,
            final int[] fullBlockDimensions, final long[] fullOffset)
    {
        int j = 0;
        for (int i = 0; i < fullRank; ++i)
        {
            final Long boundIndexOrNull = boundIndices.get(i);
            if (boundIndexOrNull == null)
            {
                if (blockDimensions[j] < 0 && fullDimensionsOrNull != null)
                {
                    blockDimensions[j] = (int) fullDimensionsOrNull[i];
                }
                fullBlockDimensions[i] = blockDimensions[j];
                fullOffset[i] = (offsetOrNull == null) ? 0 : offsetOrNull[j];
                ++j;
            } else
            {
                fullBlockDimensions[i] = 1;
                fullOffset[i] = boundIndexOrNull;
            }
        }
    }

    static void createFullBlockDimensionsAndOffset(int[] blockDimensions, long[] offsetOrNull,
            long[] boundIndices, final long[] fullDimensions, final int[] fullBlockDimensions,
            final long[] fullOffset)
    {
        createFullBlockDimensionsAndOffset(blockDimensions, offsetOrNull, boundIndices,
                fullDimensions.length, fullDimensions, fullBlockDimensions, fullOffset);
    }

    static void createFullBlockDimensionsAndOffset(int[] blockDimensions, long[] offsetOrNull,
            long[] boundIndices, final int fullRank, final int[] fullBlockDimensions,
            final long[] fullOffset)
    {
        createFullBlockDimensionsAndOffset(blockDimensions, offsetOrNull, boundIndices, fullRank,
                null, fullBlockDimensions, fullOffset);
    }

    static void createFullBlockDimensionsAndOffset(int[] blockDimensions, long[] offsetOrNull,
            long[] boundIndices, final int fullRank, final long[] fullDimensionsOrNull,
            final int[] fullBlockDimensions, final long[] fullOffset)
    {
        int j = 0;
        for (int i = 0; i < fullRank; ++i)
        {
            final long boundIndex = boundIndices[i];
            if (boundIndex < 0)
            {
                if (blockDimensions[j] < 0 && fullDimensionsOrNull != null)
                {
                    blockDimensions[j] = (int) fullDimensionsOrNull[i];
                }
                fullBlockDimensions[i] = blockDimensions[j];
                fullOffset[i] = (offsetOrNull == null) ? 0 : offsetOrNull[j];
                ++j;
            } else
            {
                fullBlockDimensions[i] = 1;
                fullOffset[i] = boundIndex;
            }
        }
    }

    static void checkBoundIndices(String objectPath, long[] dimensions, int cardBoundIndices)
            throws HDF5JavaException
    {
        if (cardBoundIndices > dimensions.length)
        {
            throw new HDF5JavaException("Dataset " + objectPath + ": more bound indices (#"
                    + cardBoundIndices + ") than dataset dimensions (#" + dimensions.length + ")");
        }
    }

    static void checkBoundIndices(String objectPath, long[] dimensions, long[] boundIndices)
            throws HDF5JavaException
    {
        if (dimensions.length != boundIndices.length)
        {
            throw new HDF5JavaException("Dataset " + objectPath + ": boundIndices array (#"
                    + boundIndices.length + ") differs from dataset dimensions (#"
                    + dimensions.length + ")");
        }
    }

    static void checkBoundIndices(String objectPath, long[] dimensions, int[] blockDimensions,
            int cardBoundIndices) throws HDF5JavaException
    {
        if (dimensions.length != blockDimensions.length + cardBoundIndices)
        {
            throw new HDF5JavaException("Dataset " + objectPath
                    + ": cardinality of bound indices (#" + cardBoundIndices
                    + ") plus rank of blocks (#" + blockDimensions.length
                    + ") not equal to rank of dataset (#" + dimensions.length + ")");
        }
    }

}
