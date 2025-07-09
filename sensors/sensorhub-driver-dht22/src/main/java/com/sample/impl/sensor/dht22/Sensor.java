/***************************** BEGIN LICENSE BLOCK ***************************
 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2020-2025 Botts Innovative Research, Inc. All Rights Reserved.
 ******************************* END LICENSE BLOCK ***************************/
package com.sample.impl.sensor.dht22;

import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.sensor.AbstractSensorModule;

/**
 * Driver implementation for the sensor.
 * <p>
 * This class is responsible for providing sensor information, managing output registration,
 * and performing initialization and shutdown for the driver and its outputs.
 */
public class Sensor extends AbstractSensorModule<Config> {
    static final String UID_PREFIX = "urn:osh:dht22_driver:";
    static final String XML_PREFIX = "DHT22_DRIVER_";

    DHTExecutor pyExec = new DHTExecutor();
    Output output;

    @Override
    public void doInit() throws SensorHubException {
        super.doInit();

        // Generate identifiers
        generateUniqueID(UID_PREFIX, config.serialNumber);
        generateXmlID(XML_PREFIX, config.serialNumber);

        // Create and initialize output
        output = new Output(this);
        addOutput(output, false);
        output.doInit();
    }

    @Override
    public void doStart() {
        //super.doStart();
        if (null != output) {
            output.doStart();   // Start output
        }

        try {
            // Execute python scripts
            pyExec.runPy();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void doStop() {
        //super.doStop();
        if (null != output) {
            output.doStop();
        }
    }

    /**
     * Check if sensor is connected
     * @return
     */
    @Override
    public boolean isConnected() {
        return output.isAlive();
    }
}
