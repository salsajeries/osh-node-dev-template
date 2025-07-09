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

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.DataRecord;
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.vast.swe.helper.GeoPosHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.util.ArrayList;

/**
 * Output specification and provider for {@link Sensor}.
 */
public class Output extends AbstractSensorOutput<Sensor> implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(Output.class);
    static final String SENSOR_OUTPUT_NAME = "DHT22Output";
    static final String SENSOR_OUTPUT_LABEL = "DHT22 Output";
    static final String SENSOR_OUTPUT_DESCRIPTION = "Temperature and humidity data from the DHT22 sensor.";
    private DataRecord dataRecord;
    private DataEncoding dataEncoding;
    private Boolean stopProcessing = false;
    private final Object processingLock = new Object();
    private static final int MAX_NUM_TIMING_SAMPLES = 10;
    private int setCount = 0;
    private final long[] timingHistogram = new long[MAX_NUM_TIMING_SAMPLES];
    private final Object histogramLock = new Object();

    private Thread worker;
    private DHTExecutor exec;


    /**
     * Creates a new output for the sensor driver.
     *
     * @param parentSensor Sensor driver providing this output.
     */
    Output(Sensor parentSensor) {
        super(SENSOR_OUTPUT_NAME, parentSensor);

        exec = new DHTExecutor();

        logger.debug("Output created.");
    }

    /**
     * Initializes the data structure for the output, defining the fields, their ordering, and data types.
     */
    void doInit() {

        logger.debug("Initializing output...");

        // Get an instance of SWE Factory suitable to build components
        GeoPosHelper sweFactory = new GeoPosHelper();

        // Create the data record description
        dataRecord = sweFactory.createRecord()
                .name(SENSOR_OUTPUT_NAME)
                .label(SENSOR_OUTPUT_LABEL)
                .description(SENSOR_OUTPUT_DESCRIPTION)
                .addField("timestamp", sweFactory.createTime()
                        .asSamplingTimeIsoUTC()
                        .label("Timestamp")
                        .description("Time of data collection"))
                .addField("temperature", sweFactory.createText()
                        .label("Temperature")
                        .description("Recorded temperature in Celsius"))
                .addField("humidity", sweFactory.createText()
                        .label("Humidity")
                        .description("Humidity percentage"))
                .build();

        dataEncoding = sweFactory.newTextEncoding(",", "\n");

        logger.debug("Completed output initialization.");
    }

    /**
     * Begin processing data from sensor
     */
    public void doStart() {
        // Instantiate a new worker thread
        worker = new Thread(this, this.name);

        // TODO: Perform other startup

        logger.info("Starting worker thread: {}", worker.getName());

        // Start the worker thread
        worker.start();
    }

    /**
     * Terminate processing data from sensor
     */
    public void doStop() {
        synchronized (processingLock) {
            stopProcessing = true;
        }
    }

    /**
     * Check to validate data processing is still running
     *
     * @return true if worker thread is active, false otherwise
     */
    public boolean isAlive() {
        return worker.isAlive();
    }

    @Override
    public DataComponent getRecordDescription() {
        return dataRecord;
    }

    @Override
    public DataEncoding getRecommendedEncoding() {
        return dataEncoding;
    }

    @Override
    public double getAverageSamplingPeriod() {
        long accumulator = 0;

        synchronized (histogramLock) {

            for (int idx = 0; idx < MAX_NUM_TIMING_SAMPLES; ++idx) {

                accumulator += timingHistogram[idx];
            }
        }

        return accumulator / (double) MAX_NUM_TIMING_SAMPLES;
    }

    @Override
    public void run() {

        boolean processSets = true;
        long lastSetTimeMillis = System.currentTimeMillis();

        try {

            while (processSets) {

                DataBlock dataBlock;
                if (latestRecord == null) {
                    dataBlock = dataRecord.createDataBlock();
                } else {
                    dataBlock = latestRecord.renew();
                }

                synchronized (histogramLock) {

                    int setIndex = setCount % MAX_NUM_TIMING_SAMPLES;

                    timingHistogram[setIndex] = System.currentTimeMillis() - lastSetTimeMillis;
                    lastSetTimeMillis = System.currentTimeMillis();
                }

                ++setCount;

                double timestamp = System.currentTimeMillis() / 1000d;
                String temperatureC, temperatureF, humidity;

                try {
                    exec.runPy();   // RUN PYTHON SCRIPT
                    temperatureC = exec.getTemperatureC();
                    temperatureF = exec.getTemperatureF();
                    humidity = exec.getHumidity();
                } catch (Exception e) {
                    logger.error("Error reading data from sensor");
                    temperatureC = "Error reading temperatureC";
                    temperatureF = "Error reading temperatureF";
                    humidity = "Error reading humidity";
                }

                // TODO: Populate data block
                dataBlock.setDoubleValue(0, timestamp);
                dataBlock.setStringValue(1, temperatureF);
                dataBlock.setStringValue(2, humidity);

                latestRecord = dataBlock;

                latestRecordTime = System.currentTimeMillis();

                eventHandler.publish(new DataEvent(latestRecordTime, Output.this, dataBlock));

                synchronized (processingLock) {
                    processSets = !stopProcessing;
                }
            }

        } catch (Exception e) {
            logger.error("Error in worker thread: {}", Thread.currentThread().getName(), e);
        } finally {
            // Reset the flag so that when driver is restarted loop thread continues
            // until doStop called on the output again
            stopProcessing = false;
            logger.debug("Terminating worker thread: {}", this.name);
        }
    }
}
