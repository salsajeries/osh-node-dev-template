/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2020-2021 Botts Innovative Research, Inc. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/
package com.sample.impl.sensor.hw507;

import com.pi4j.Pi4J;
import com.pi4j.context.Context;
//import com.pi4j.io.gpio.digital.DigitalInput;
//import com.pi4j.io.gpio.digital.DigitalInputConfig;
//import com.pi4j.io.gpio.digital.DigitalOutput;
//import com.pi4j.io.gpio.digital.DigitalState;
import com.pi4j.io.gpio.digital.*;
import com.pi4j.io.gpio.digital.DigitalStateChangeEvent;

import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.sensorhub.api.common.SensorHubException;

import java.util.concurrent.TimeUnit;

/**
 * Sensor driver providing sensor description, output registration, initialization and shutdown of driver and outputs.
 *
 * @author Salwa Jeries
 * @since date
 */
public class Sensor extends AbstractSensorModule<Config> {

    //private static final Logger logger = LoggerFactory.getLogger(Sensor.class);



    private Context pi4j;
    private DigitalInput gpioInput;

    Output sensorOutput;

    public Sensor() {}

    private void setGpioInput() {
        try {
            // Initialize Pi4J context
            this.pi4j = Pi4J.newAutoContext();

            // Configure GPIO pin
            DigitalInputConfig inputConfig = DigitalInput.newConfigBuilder(pi4j)
                    .id("gpio-input")
                    .name("GPIO Input")
                    //.address(Integer.valueOf(config.gpioInput)) // Use pin for reading input
                    .address(17) // Hardcode pin for testing
                    //.pull(PullResistance.PULL_DOWN)
                    .provider("pigpio-digital-input")
                    .build();

            // Used for reading HIGH (1) state by default
            //DigitalInputProvider digitalInputProvider = pi4j.provider("pigpio-digital-input");
            //gpioInput = digitalInputProvider.create(inputConfig);
            // gpioInput.setPull(PullResistance.PULL_DOWN);

            this.gpioInput = pi4j.create(inputConfig);

            DigitalStateChangeEvent

            // Add listener to read sensor dynamically
            this.gpioInput.addListener((DigitalStateChangeEvent e) -> {
                System.out.println(e.state() == DigitalState.LOW);
            });

        } catch (Exception e) {
            System.out.println("ERROR SETTING INPUT");
            System.out.println(e);
        }
    }

    private void setSensorOutput() {
        sensorOutput = new Output(this);
        addOutput(sensorOutput, false);
    }

    //public void readTemperatureAndHumidity() {

        //        try {
//            DigitalOutput dataPin = pi4j.create(DigitalOutput.newConfigBuilder(pi4j)
//                    .id("hw507-data")
//                    .name("HW-507 Data Pin")
//                    .address(Integer.valueOf(config.gpioInput))
//                    .shutdown(DigitalState.LOW)
//                    .initial(DigitalState.HIGH)
//                    .build());
//
//            dataPin.low();
//            TimeUnit.MILLISECONDS.sleep(18);
//            dataPin.high();
//            TimeUnit.MICROSECONDS.sleep(40);
//
//            dataPin.shutdown(pi4j);
//            setGpioInput();
//
//            while (gpioInput.isHigh()) {}
//            while (gpioInput.isLow()) {}
//            while (gpioInput.isHigh()) {}
//
//
//            int[] data = new int[40];
//            for (int i = 0; i < 40; i++) {
//                while (gpioInput.isLow()) {}
//                long startTime = System.nanoTime();
//                while (gpioInput.isHigh()) {}
//                long pulseWidth = System.nanoTime() - startTime;
//                data[i] = (pulseWidth > 50000) ? 1 : 0;
//            }
//
//            int humidity = 0, temperature = 0;
//            for (int i = 0; i < 8; i++) humidity = (humidity << 1) | data[i];
//            for (int i = 16; i < 24; i++) temperature = (temperature << 2) | data[i];
//
//            System.out.println("Temperature: " + temperature + "C");
//            System.out.println("Humidity: " + humidity + "&");
//
//            sensorOutput.setData(temperature, humidity);
//        } catch (InterruptedException e) {
//            Thread.currentThread().interrupt();
//            System.out.println("Error reading sensor: " + e.getMessage());
//        }
//    }

    public void readTemperatureAndHumidity() {
        try {
            DigitalOutput dataPin = pi4j.create(DigitalOutput.newConfigBuilder(pi4j)
                    .id("hw507-data")
                    .name("HW-507 Data Pin")
                    .address(Integer.valueOf(config.gpioInput))
                    .shutdown(DigitalState.LOW)
                    .initial(DigitalState.HIGH)
                    .build());

            // Start signal to sensor
            dataPin.low();
            TimeUnit.MILLISECONDS.sleep(18);
            dataPin.high();
            TimeUnit.MICROSECONDS.sleep(40);

            dataPin.shutdown(pi4j);
            setGpioInput();

            // Wait for sensor response signals with timeouts
            if (!waitForState(gpioInput, DigitalState.LOW, 1000) ||
                    !waitForState(gpioInput, DigitalState.HIGH, 1000) ||
                    !waitForState(gpioInput, DigitalState.LOW, 1000)) {
                System.out.println("Sensor response timeout");
                return;
            }

            int[] data = new int[40];
            for (int i = 0; i < 40; i++) {
                if (!waitForState(gpioInput, DigitalState.HIGH, 1000)) {
                    System.out.println("Timeout waiting for HIGH pulse " + i);
                    return;
                }
                long startTime = System.nanoTime();
                if (!waitForState(gpioInput, DigitalState.LOW, 1000)) {
                    System.out.println("Timeout waiting for LOW pulse " + i);
                    return;
                }
                long pulseWidth = System.nanoTime() - startTime;
                data[i] = (pulseWidth > 50000) ? 1 : 0;
            }

            int humidity_int = 0, humidity_dec = 0, temp_int = 0, temp_dec = 0, checksum = 0;
            for (int i = 0; i < 8; i++) humidity_int = (humidity_int << 1) | data[i];
            for (int i = 8; i < 16; i++) humidity_dec = (humidity_dec << 1) | data[i];
            for (int i = 16; i < 24; i++) temp_int = (temp_int << 1) | data[i];
            for (int i = 24; i < 32; i++) temp_dec = (temp_dec << 1) | data[i];
            for (int i = 32; i < 40; i++) checksum = (checksum << 1) | data[i];

            int sum = (humidity_int + humidity_dec + temp_int + temp_dec) & 0xFF;

            // Validate checksum
//            if (sum != checksum) {
//                System.out.println("Checksum error: data invalid");
//                return;
//            }

            double humidity = humidity_int + humidity_dec / 100.0;
            double temperature = temp_int + temp_dec / 100.0;

            System.out.println(String.format("Temperature: %.2f C", temperature));
            System.out.println(String.format("Humidity: %.2f %%", humidity));

            sensorOutput.setData(temperature, humidity);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("Error reading sensor: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("Unexpected error during sensor read: " + e);
        }
    }

    // Helper method to wait for a pin state with timeout (in microseconds)
    private boolean waitForState(DigitalInput input, DigitalState state, long timeoutMicros) throws InterruptedException {
        long start = System.nanoTime();
        while (input.state() != state) {
            if ((System.nanoTime() - start) > timeoutMicros * 1000) {
                return false; // timeout
            }
            Thread.sleep(0, 5000); // Sleep 5 microseconds to reduce CPU usage
        }
        return true;
    }


    @Override
    public void doInit() throws SensorHubException {

        System.out.println("Initializing sensor...");

        super.doInit();

        System.out.println("doInit() worked. Continuing...");

        // Generate identifiers
        generateUniqueID("urn:osh:sensor:hw507", config.serialNumber);
        generateXmlID("HW507", config.serialNumber);

        // Not needed due to no Control.java
//        controlInterface = new Control(this);
//        addControlInput(controlInterface);
//        controlInterface.init();

        pi4j = Pi4J.newAutoContext();
        setGpioInput();
        setSensorOutput();
    }

    @Override
    protected void updateSensorDescription() {
        synchronized (sensorDescLock) {
            super.updateSensorDescription();

            if (!sensorDescription.isSetDescription()) {
                sensorDescription.setDescription("Driver for HW-507 Temperature/humidity sensor on Raspberry Pi");
            }
        }
    }

    @Override
    public void doStart() throws SensorHubException {

        System.out.println("Starting sensor...");
        super.doStart();
        readTemperatureAndHumidity();
    }

    @Override
    public void doStop() throws SensorHubException {
        pi4j.shutdown();
    }

    @Override
    public void cleanup() throws SensorHubException {}

    @Override
    public boolean isConnected() {
        return true;
    }
}
