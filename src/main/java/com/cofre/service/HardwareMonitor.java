package com.cofre.service;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

import java.io.File;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class HardwareMonitor {

    private static final String DISPOSITIVO = "/dev/ttyACM0";
    private static final BooleanProperty conectado = new SimpleBooleanProperty(false);
    private static ScheduledExecutorService scheduler;

    public static BooleanProperty conectadoProperty() {
        return conectado;
    }

    public static boolean isConectado() {
        return conectado.get();
    }

    public static synchronized void iniciarMonitoramento() {
        if (scheduler != null && !scheduler.isShutdown()) {
            return;
        }

        conectado.set(new File(DISPOSITIVO).exists());

        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "usb-monitor-thread");
            t.setDaemon(true);
            return t;
        });

        scheduler.scheduleAtFixedRate(() -> {
            boolean existe = new File(DISPOSITIVO).exists();
            if (existe != conectado.get()) {
                Platform.runLater(() -> conectado.set(existe));
            }
        }, 500, 500, TimeUnit.MILLISECONDS);
    }

    public static synchronized void pararMonitoramento() {
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }
    }
}
