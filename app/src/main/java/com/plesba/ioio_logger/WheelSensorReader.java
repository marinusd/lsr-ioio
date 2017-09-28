package com.plesba.ioio_logger;

import ioio.lib.api.IOIO;
import ioio.lib.api.DigitalInput;
import ioio.lib.api.PulseInput;
import ioio.lib.api.exception.ConnectionLostException;

public class WheelSensorReader {
    public static int frontInput = 11;
    public static int rearInput  = 13;
    private double rpm = 0d;
    private PulseInput pulse;

    double getRPM() {
        rpm = 0d;
        try {
            rpm = pulse.getFrequency() * 60d;  // getFrequency() gives a float
        } catch (Exception e) {
            e.printStackTrace();
        }
        return rpm;
    }

    WheelSensorReader(IOIO ioio_, int pin) {
        DigitalInput.Spec pinPullUp = new DigitalInput.Spec(pin,DigitalInput.Spec.Mode.PULL_UP);
        PulseInput.ClockRate rate_2MHz = PulseInput.ClockRate.RATE_2MHz;
        PulseInput.PulseMode freq_scale_4 = PulseInput.PulseMode.FREQ_SCALE_4;
        boolean doublePrecision = true;
        FileWriter.getInstance().syslog("WheelSensorReader is being created for pin " + pin);
        try {
            pulse = ioio_.openPulseInput(pinPullUp, rate_2MHz, freq_scale_4, doublePrecision);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
