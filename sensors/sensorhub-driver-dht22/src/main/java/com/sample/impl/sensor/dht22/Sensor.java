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

import net.opengis.sensorml.v20.PhysicalSystem;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.vast.sensorML.SMLHelper;

/**
 * Driver implementation for the sensor.
 * <p>
 * This class is responsible for providing sensor information, managing output registration,
 * and performing initialization and shutdown for the driver and its outputs.
 */
public class Sensor extends AbstractSensorModule<Config> {
    static final String UID_PREFIX = "urn:osh:dht22:";
    static final String XML_PREFIX = "DHT22_";

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


    @Override
    protected void updateSensorDescription() {
        synchronized (sensorDescLock) {
            super.updateSensorDescription();
            sensorDescription.setDescription("DHT22 Sensor providing temperature and humidity readings.");

            SMLHelper helper = new SMLHelper();
            helper.edit((PhysicalSystem)sensorDescription)
                    .addIdentifier(helper.identifiers.serialNumber(config.serialNumber))

                    // Type of sensor
                    .addClassifier(helper.classifiers.sensorType("Temperature/Humidity Sensor"))

                    // Operating specifications of the sensor
                    .addCharacteristicList("operating_specs", helper.characteristics.operatingCharacteristics()
                            .add("voltage", helper.characteristics.operatingVoltageRange(3.3, 6.0, "V"))
                            .add("temperature", helper.conditions.temperatureRange(-40.0, 80.0, "Cel"))
                            .add("humidity", helper.conditions.humidityRange(0.0, 100.0, "%")))

                    // Information about sensor readings
                    .addCapabilityList("system_caps", helper.capabilities.systemCapabilities()
                            .add("update_rate", helper.capabilities.reportingFrequency(0.5))    // 0.5Hz = 2sec
                            .add("humidity_accuracy", helper.capabilities.absoluteAccuracy(2.0, "%"))
                            .add("temperature_accuracy", helper.capabilities.absoluteAccuracy(0.5, "Cel"))
                            .add("humidity_sensitivity", helper.capabilities.sensitivity(0.1, "%"))
                            .add("temperature_sensitivity", helper.capabilities.sensitivity(0.1, "Cel"))
                            .add("Something", helper.createQuantity()
                                    .label("Power-Up Time")
                                    .description("Time to first reading")
                                    .uomCode("s")
                                    .value(1.0)
                            )
                    )

            ;
        }

    }
}
