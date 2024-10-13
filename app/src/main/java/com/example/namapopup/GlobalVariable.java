package com.example.namapopup;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import java.util.HashMap;
import java.util.List;

public class GlobalVariable {
    public static class HougenInformation {
        String hougen;
        String trigger;
        String yomikata;
        String candidate;
        String chihou;
        String pref;
        String area;
        String def;
        String example;
        String pos;
        List<NamaPopup.CharacterPosition> characterPositions;

        HougenInformation(String hougen, String trigger, String candidate, String yomikata, String chihou, String pref, String area, String def, String example, List<NamaPopup.CharacterPosition> characterPositions, String pos) {
            this.hougen = hougen;
            this.trigger = trigger;
            this.yomikata = yomikata;
            this.candidate = candidate;
            this.chihou = chihou;
            this.pref = pref;
            this.area = area;
            this.def = def;
            this.example = example;
            this.pos = pos;
            this.characterPositions = characterPositions;
        }
    }

    public static HashMap<String, List<String>> verbMap;
}
