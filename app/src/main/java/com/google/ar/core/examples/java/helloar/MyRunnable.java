package com.google.ar.core.examples.java.helloar;

import java.util.concurrent.atomic.AtomicBoolean;

public class MyRunnable implements Runnable{
    public AtomicBoolean running = new AtomicBoolean(false);
    public void stopMe() {
        running.set(false);
    }
    public void runMe() {
        running.set(true);
    }
    @Override
    public void run() {

    }

}
