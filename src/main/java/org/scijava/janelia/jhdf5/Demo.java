/*
 * Copyright 2023 Howard Hughes Medical Institute
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

package org.scijava.janelia.jhdf5;

import hdf.hdf5lib.H5;
import hdf.hdf5lib.HDF5Constants;

import org.bytedeco.hdf5.hdf5_java;
import org.bytedeco.hdf5.global.hdf5;

public class Demo {
	
	static {
		new hdf5_java();
	}

	public static void main(String[] args) {
	}
}
