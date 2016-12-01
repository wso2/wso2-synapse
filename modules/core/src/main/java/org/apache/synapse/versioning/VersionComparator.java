package org.apache.synapse.versioning;

/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *   * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

import java.util.Comparator;

public class VersionComparator implements Comparator<String> {

	//returns 1 if first one is higher than the second one.
	@Override public int compare(String o1, String o2) {
		String v1 = o1.substring(o1.lastIndexOf('/') + 1);
		String v2 = o2.substring(o2.lastIndexOf('/') + 1);

		String[] vArr1 = v1.split("\\.");
		String[] vArr2 = v2.split("\\.");

		int i = 0;
		while (i < vArr1.length || i < vArr2.length) {
			if (Integer.parseInt(vArr1[i]) < Integer.parseInt(vArr2[i])) {
				return -1;
			} else if (Integer.parseInt(vArr1[i]) > Integer.parseInt(vArr2[i])) {
				return 1;
			}

			i++;
		}

		return 0;
	}
}
