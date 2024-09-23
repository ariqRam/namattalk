package com.example.namapopup;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

public class GlobalVariable {
    public static class HougenInformation {
        String hougen;
        String chihou;
        String pref;
        String area;
        String def;
        String example;

        HougenInformation(String hougen, String hougenchihou, String pref, String area, String def, String example) {
            this.hougen = hougen;
            this.chihou = hougenchihou;
            this.pref = pref;
            this.area = area;
            this.def = def;
            this.example = example;
        }
    }
}
