package com.plesba.ioio_logger;

public class WheelRPMreporter {
    private WheelSensorReader reader;
    private long count, lastCount, revolutions, nowTime, lastTime, elapsedTime;
    private double seconds, rpm;

    public WheelRPMreporter(WheelSensorReader sensorReader){
        reader = sensorReader;
        lastCount = reader.getCount();
        lastTime = System.currentTimeMillis();
    }

    public double getRPM() {
        nowTime = System.currentTimeMillis();
        count = reader.getCount();

        elapsedTime = nowTime - lastTime;
        lastTime = nowTime;

        revolutions = count - lastCount;
        lastCount = count;

        seconds = elapsedTime/1000.0d;
        rpm = revolutions / seconds * 60;

        return rpm;

    }
}
