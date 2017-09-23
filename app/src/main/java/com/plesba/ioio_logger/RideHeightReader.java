package com.plesba.ioio_logger;

import ioio.lib.api.IOIO;
import ioio.lib.api.AnalogInput;
import ioio.lib.api.exception.ConnectionLostException;

public class RideHeightReader extends Thread {
    private static int leftInputPin = 44;
    private static int rightInputPin = 42;
    private AnalogInput leftInput;
    private AnalogInput rightInput;
    private String leftReading = "";
    private String rightReading = "";
    private String ultimateLeft = "";
    private String ultimateRight = "";
    private String penultimateLeft = "";
    private String penultimateRight = "";
    private String reportLeft = "";
    private String reportRight = "";
    private String tStr = "";


    public String getLeftReading() {
        return reportLeft;
    }
    public String getRightReading() {
        return reportRight;
    }

    public RideHeightReader(IOIO ioio_) {
        FileWriter.getInstance().syslog("RideHeightReader is being created");
        try {
            leftInput = ioio_.openAnalogInput(leftInputPin);
            rightInput = ioio_.openAnalogInput(rightInputPin);
        } catch (ConnectionLostException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        while (true) {
            try {
			/*
			 * Turn the ride height sensor readings into strings.
			 * The IOIO read() on an analog input returns a float in the 0.0-1.0 range
			 * The measurement values have lots of trailing digits - but the
			 * sensors we have will never show 1.0, so 0.99xxx is the max. We
			 * only need two digits of resolution, so trim off the leading '0.'
			 * of the string, and throw away digits past two.
			 *  example:
			 * 0.234529684f => 23 (substring(2,4)
			 * AND! the ride height sensor
			 * readings go DOWN as the accordion units are extended (and the
			 * readings go UP as the units are compressed. So we subtract the
			 * readings from 1.0 to reverse the relationship.
			 */
                // leftSide sensor. Make sure the string is long enough to index[4]
                tStr = Float.toString(1.0f - leftInput.read()) + "000";
                leftReading = tStr.substring(2, 4);
                Thread.sleep(33); // worried about voltage sagging? why does shortening one sensor change both readings?
                // now right side sensor
                tStr = Float.toString(1.0f - rightInput.read()) + "000";
                rightReading = tStr.substring(2, 4);

                //   check the last TWO sensor readings to ignore analog flutter
                if ((!ultimateLeft.equals(leftReading) && !penultimateLeft.equals(leftReading)) ||
                        (!ultimateRight.equals(rightReading) && !penultimateRight.equals(rightReading))) {
                    reportLeft = leftReading;
                    reportRight = rightReading;
                }

                //  shift the sensor readings down by one place for next comparison
                penultimateLeft = ultimateLeft;
                penultimateRight = ultimateRight;
                ultimateLeft = leftReading;
                ultimateRight = rightReading;

                // read 'em three times a second max
                Thread.sleep(300);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
