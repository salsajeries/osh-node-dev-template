package com.sample.impl.sensor.dht22;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class DHTExecutor {

    private String line;
    private double temperatureF;
    private double temperatureC;
    private double humidity;

    public double[] runPy() throws Exception {
        System.out.println("Starting reading...");

        boolean success = false;

        try {
            // Start process
            ProcessBuilder pb = new ProcessBuilder("python3", "dht22_pyscript.py");
            pb.redirectErrorStream(true);   // Redirect error stream to stdout
            Process process = pb.start();
            System.out.println("Process started.");

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            line = reader.readLine(); // Expecting: "24.3, 60.2"

            if (line != null && !line.trim().equalsIgnoreCase("Error reading.")) {
                String[] parts = line.trim().split(",");
                if (parts.length == 2) {
                    temperatureC = Double.parseDouble(parts[0]);    // Temperature -> C
                    temperatureF = temperatureC * 1.8 + 32;         // Temperature -> F
                    humidity = Double.parseDouble(parts[1]);        // Humidity
                    success = true;
                }
            } else {
                //System.out.println("ERROR from script: " + line);
            }

            process.waitFor();

        } catch (Exception e) {
            e.printStackTrace();
        }

        if (success) {
            System.out.printf("Temperature: %.1f°C (%.1f°F)%n", temperatureC, temperatureF);
            System.out.printf("Humidity: %.1f%%%n", humidity);
            return new double[] {temperatureC, temperatureF, humidity};
        } else {
            System.out.println("Failed to read valid sensor data.");
            return new double[] {};
        }
    }

    public String getTemperatureF() {
        return String.format("%.1°F", Double.toString(temperatureF));
    }

    public String getTemperatureC() {
        return String.format("%.1°C", Double.toString(temperatureC));
    }

    public String getHumidity() {
        return String.format("%.1%%", Double.toString(humidity));
    }
}
