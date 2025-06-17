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

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.DataRecord;
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.swe.SWEHelper;
import org.vast.swe.helper.GeoPosHelper;

/**
 * Output specification and provider for {@link Sensor}.
 *
 * @author your_name
 * @since date
 */
public class Output extends AbstractSensorOutput<Sensor> {

    private static final String SENSOR_OUTPUT_NAME = "Temperature/Humidity";
    private static final String SENSOR_OUTPUT_LABEL = "Temperature/Humidity";
    private static final String SENSOR_OUTPUT_DESCRIPTION = "Temperature and humidity measurements from HW-507 sensor";

    //private static final Logger logger = LoggerFactory.getLogger(Output.class);

    private DataRecord dataStruct;
    private DataEncoding dataEncoding;

    //private Boolean stopProcessing = false;
    //private final Object processingLock = new Object();

    //private static final int MAX_NUM_TIMING_SAMPLES = 10;
    //private int setCount = 0;
    //private final long[] timingHistogram = new long[MAX_NUM_TIMING_SAMPLES];
    //private final Object histogramLock = new Object();

    //private Thread worker;

    /**
     * Constructor
     *
     * @param parentSensor Sensor driver providing this output
     */
    Output(Sensor parentSensor) {

        super(SENSOR_OUTPUT_NAME, parentSensor);

        doInit();

        //logger.debug("Output created");
    }

    /**
     * Sets the data for the sensor output
     *
     * @param temperature
     * @param humidity
     */
    public void setData(double temperature, double humidity) {
        long timestamp = System.currentTimeMillis();
        DataBlock dataBlock = latestRecord == null ? dataStruct.createDataBlock() : latestRecord.renew();

        dataBlock.setDoubleValue(0, timestamp / 1000d); // Timestamp record
        dataBlock.setDoubleValue(1, temperature);       // Temperature value
        dataBlock.setDoubleValue(2, humidity);          // Humidity value

        latestRecord = dataBlock;
        eventHandler.publish(new DataEvent(timestamp, Output.this, dataBlock));
    }

    /**
     * Initializes the data structure for the output, defining the fields, their ordering,
     * and data types.
     */
    void doInit() {

        //logger.debug("Initializing Output");

        // Get an instance of SWE Factory suitable to build components
        //GeoPosHelper sweFactory = new GeoPosHelper();
        SWEHelper sweHelper = new SWEHelper();

        // TODO: Create data record description
        dataStruct = sweHelper.createRecord()
                .name(SENSOR_OUTPUT_NAME)
                .label(SENSOR_OUTPUT_LABEL)
                .description(SENSOR_OUTPUT_DESCRIPTION)
                .addField("sampleTime", sweHelper.createTime()
                        .asSamplingTimeIsoUTC()
                        .label("Sample Time")
                        .description("Time of data collected"))
                .addField("temperature", sweHelper.createQuantity()
                        .label("Temperature (C)")
                        .description("Measured temperature in Celsius"))
                .addField("humidity", sweHelper.createQuantity()
                        .label("Humidity {%)")
                        .description("Measured humidity in percentage"))
                .build();

        // Based on training modules
        dataEncoding = new SWEHelper().newTextEncoding(",", "\n");

        //logger.debug("Initializing Output Complete");
    }

//
//    /**
//     * Begins processing data for output
//     */
//    public void doStart() {
//
//        // Instantiate a new worker thread
//        worker = new Thread(this, this.name);
//
//        // TODO: Perform other startup
//
//        logger.info("Starting worker thread: {}", worker.getName());
//
//        // Start the worker thread
//        worker.start();
//    }
//
//    /**
//     * Terminates processing data for output
//     */
//    public void doStop() {
//
//        synchronized (processingLock) {
//
//            stopProcessing = true;
//        }
//
//        // TODO: Perform other shutdown procedures
//    }
//
//    /**
//     * Check to validate data processing is still running
//     *
//     * @return true if worker thread is active, false otherwise
//     */
//    public boolean isAlive() {
//
//        return worker.isAlive();
//    }
//

    @Override
    public DataComponent getRecordDescription() {

        return dataStruct;
    }

    @Override
    public DataEncoding getRecommendedEncoding() {

        return dataEncoding;
    }

    @Override
    public double getAverageSamplingPeriod() {

        return Double.NaN;
//        long accumulator = 0;
//
//        synchronized (histogramLock) {
//
//            for (int idx = 0; idx < MAX_NUM_TIMING_SAMPLES; ++idx) {
//
//                accumulator += timingHistogram[idx];
//            }
//        }
//
//        return accumulator / (double) MAX_NUM_TIMING_SAMPLES;
    }

//    @Override
//    public void run() {
//
//        boolean processSets = true;
//
//        long lastSetTimeMillis = System.currentTimeMillis();
//
//        try {
//
//            while (processSets) {
//
//                DataBlock dataBlock;
//                if (latestRecord == null) {
//
//                    dataBlock = dataStruct.createDataBlock();
//
//                } else {
//
//                    dataBlock = latestRecord.renew();
//                }
//
//                synchronized (histogramLock) {
//
//                    int setIndex = setCount % MAX_NUM_TIMING_SAMPLES;
//
//                    // Get a sampling time for latest set based on previous set sampling time
//                    timingHistogram[setIndex] = System.currentTimeMillis() - lastSetTimeMillis;
//
//                    // Set latest sampling time to now
//                    lastSetTimeMillis = timingHistogram[setIndex];
//                }
//
//                ++setCount;
//
//                double timestamp = System.currentTimeMillis() / 1000d;
//
//                // TODO: Populate data block
//                dataBlock.setDoubleValue(0, timestamp);
//                dataBlock.setStringValue(1, "Your data here");
//
//                latestRecord = dataBlock;
//
//                latestRecordTime = System.currentTimeMillis();
//
//                eventHandler.publish(new DataEvent(latestRecordTime, Output.this, dataBlock));
//
//                synchronized (processingLock) {
//
//                    processSets = !stopProcessing;
//                }
//            }
//
//        } catch (Exception e) {
//
//            logger.error("Error in worker thread: {}", Thread.currentThread().getName(), e);
//
//        } finally {
//
//            // Reset the flag so that when driver is restarted loop thread continues
//            // until doStop called on the output again
//            stopProcessing = false;
//
//            logger.debug("Terminating worker thread: {}", this.name);
//        }
//    }
}
