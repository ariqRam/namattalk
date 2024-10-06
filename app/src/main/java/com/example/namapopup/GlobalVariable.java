package com.example.namapopup;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import java.util.List;

public class GlobalVariable {
    public static class HougenInformation {
        String hougen;
        String trigger;
        String chihou;
        String pref;
        String area;
        String def;
        String example;
        List<NamaPopup.CharacterPosition> characterPositions;

        HougenInformation(String hougen, String trigger, String chihou, String pref, String area, String def, String example, List<NamaPopup.CharacterPosition> characterPositions) {
            this.hougen = hougen;
            this.trigger = trigger;
            this.chihou = chihou;
            this.pref = pref;
            this.area = area;
            this.def = def;
            this.example = example;
            this.characterPositions = characterPositions;
        }
    }
}
