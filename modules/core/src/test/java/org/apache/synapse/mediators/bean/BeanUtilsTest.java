/**
 * Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 * <p>
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.synapse.mediators.bean;

import org.junit.Assert;
import org.junit.Test;
import java.lang.reflect.Method;

/**
 * Unit tests for BeanUtils class
 */
public class BeanUtilsTest {

    /**
     * Asserting resolve method of beanUtils class
     */
    @Test
    public void testBeanUtilResolve() {
        Method method = BeanUtils.resolveMethod(SampleBean.class, "setTestProperty", 1);
        Assert.assertNotNull("method exist with given name and argument count", method);
    }

    /**
     * Asserting resolve method of beanUtils class with invalid data
     */
    @Test
    public void testBeanUtilResolveInvalid() {
        Method method = BeanUtils.resolveMethod(SampleBean.class, "setTestProperty", 2);
        Assert.assertNull("method not exists with given name and argument count", method);
    }
}
