package com.example.namapopup;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

public class GlobalVariable {
    public static class HougenInformation {
        String hougen;
        String trigger;
        String chihou;
        String pref;
        String area;
        String def;
        String example;

        HougenInformation(String hougen, String trigger, String hougenchihou, String pref, String area, String def, String example) {
            this.hougen = hougen;
            this.trigger = trigger;
            this.chihou = hougenchihou;
            this.pref = pref;
            this.area = area;
            this.def = def;
            this.example = example;
        }
    }
}
