/*
* Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
* WSO2 Inc. licenses this file to you under the Apache License,
* Version 2.0 (the "License"); you may not use this file except
* in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied. See the License for the
* specific language governing permissions and limitations
* under the License.
*/
package org.apache.synapse.aspects.flow.statistics.log;

import org.apache.synapse.aspects.flow.statistics.publishing.PublishingFlow;

import java.util.Observable;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by rajith on 7/20/16.
 */
public class StatisticsObservable extends Observable {
//    private static StatisticsObservable observable;
//    private static Lock lock = new ReentrantLock();
//
//    private StatisticsObservable() {
//
//    }
//
//    public static StatisticsObservable getInstance() {
//        if (observable == null) {
//            synchronized (lock) {
//                if (observable == null) {
//                    observable = new StatisticsObservable();
//                }
//            }
//        }
//        return observable;
//    }

    public void submitToMediationLayer(PublishingFlow publishingFlow) {
        setChanged();
        notifyObservers(publishingFlow);
    }
}
