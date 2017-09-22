package com.plesba.ioio_logger;

import ioio.lib.api.IOIO;
import ioio.lib.api.DigitalInput;
import ioio.lib.api.exception.ConnectionLostException;

public class WheelSensorReader extends Thread {
    public static int frontInput = 11;
    public static int rearInput  = 13;
    private long count = 0;
    private DigitalInput input;

    public long getCount() {
        return count;
    }

    public WheelSensorReader(IOIO ioio_, int pin) {
        FileWriter.getInstance().syslog("WheelSensorReader is being created for pin " + pin);
        try {
            input = ioio_.openDigitalInput(pin, DigitalInput.Spec.Mode.PULL_UP);
        } catch (ConnectionLostException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        while (true) {
            try {
                input.waitForValue(false); // low = false
                count++;
                input.waitForValue(true);  // high = true
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
