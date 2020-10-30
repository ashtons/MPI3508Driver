package com.schimera.example.touch;

import java.io.Serializable;

import eu.vranckaert.driver.touch.driver.Driver;
import eu.vranckaert.driver.touch.profile.SPIDriverProfile;
import eu.vranckaert.driver.touch.profile.ScreenDimension;
import eu.vranckaert.driver.touch.profile.Vendor;

public class MPI3508Profile extends SPIDriverProfile implements Serializable {
    public static final ScreenDimension DIMENSION_480_320 = new ScreenDimension(480, 320, ScreenDimension.Ratio.R_4_3);

    public static MPI3508Profile INSTANCE;

    public MPI3508Profile(ScreenDimension screenDimension) {
        super(Vendor.UNKNOWN, screenDimension);
    }

    public static MPI3508Profile getInstance(ScreenDimension screenDimension) {
        if (INSTANCE == null) {
            INSTANCE = new MPI3508Profile(screenDimension);
        }
        return INSTANCE;
    }

    @Override
    public Driver getDriver() {
        return new MPI3508Driver(this);
    }
}
