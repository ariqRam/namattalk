package com.example.namapopup;


import static com.example.namapopup.Helper.getDialectState;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.util.Log;
import android.widget.Switch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VerbConjugator {

    private DBHelper dbHelper;
    private SharedPreferences sharedPreferences;
    private DialectState toyamaState;
    private DialectState hidaState;

    public VerbConjugator(Context context) {
        dbHelper = new DBHelper(context);
        sharedPreferences = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);
        // Load saved states for dialects
        toyamaState = getDialectState(context, "toyama");
        hidaState = getDialectState(context, "hida");
    }

    public HashMap<String, List<String>> getVerbs() {
        HashMap<String, List<String>> verbMap = new HashMap<>();
        Cursor[] cursors = dbHelper.getVerbs();

        for (Cursor cursor : cursors) {
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    int hougenIndex = cursor.getColumnIndex("hougen");
                    int triggerIndex = cursor.getColumnIndex("trigger");
                    int posIndex = cursor.getColumnIndex("pos");

                    if (hougenIndex != -1 && triggerIndex != -1 && posIndex != -1) {
                        String hougen = cursor.getString(hougenIndex);
                        String trigger = cursor.getString(triggerIndex);
                        String pos = cursor.getString(posIndex);

                        if ("動詞".equals(pos) && hougen != null && trigger != null && toyamaState.isEnabled || hidaState.isEnabled) {
                            if (trigger != null) {
                                String[] splitTriggers = trigger.split("、");
                                for (String singleTrigger : splitTriggers) {
                                    List<String> verbFormList = new ArrayList<>();
                                    for (VerbForm form : VerbForm.values()) {
                                        verbFormList.add(conjugateFromBase(singleTrigger.trim(), form, false));
                                    }
                                    verbMap.put(singleTrigger.trim(), verbFormList);
                                }
                            }

                            // Handle hougen
                            List<String> hougenFormList = new ArrayList<>();
                            for (VerbForm form : VerbForm.values()) {
                                hougenFormList.add(conjugateFromBase(hougen, form, true));
                            }
                            verbMap.put(hougen, hougenFormList);
                        }
                    }
                } while (cursor.moveToNext());

                cursor.close();
            }
        }

        logVerbMap(verbMap);
        return verbMap;
    }

    public void logVerbMap(HashMap<String, List<String>> verbMap) {
        for (Map.Entry<String, List<String>> entry : verbMap.entrySet()) {
            String key = entry.getKey();
            List<String> values = entry.getValue();
            Log.d("VerbMap", "Key: " + key + ", Values: " + values.toString());
        }
    }


    public enum VerbType {
        ICHIDAN, GODAN, IRREGULAR, NULL
    }

    public enum VerbForm {
        BASE(0),
        NEGATIVE(1),
        PAST(2),
        TE_FORM(3),
        POLITE(4);

        private final int value;

        VerbForm(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    public static VerbForm getVerbForm(String verb) {
        if (verb.endsWith("ない") || verb.endsWith("ん")) {
            return VerbForm.NEGATIVE;
        }

        if (verb.endsWith("た") || verb.endsWith("だ")) {
            return VerbForm.PAST;
        }

        if (verb.endsWith("て") || verb.endsWith("で")) {
            return VerbForm.TE_FORM;
        }

        if (verb.endsWith("ます")) {
            return VerbForm.POLITE;
        }

        return VerbForm.BASE;
    }

    // Detect verb type based on its ending and conjugate accordingly
    public static VerbType getVerbType(String verb, VerbForm form) {
        //in case of verb is base form
        String stem = "";
        switch (form) {
            case NEGATIVE:
                //Handle irregular verbs
                if (verb.equals("しない") || verb.equals("こない") || verb.equals("いない") || verb.equals("いかない") || verb.equals("おらん")) {
                    return VerbType.IRREGULAR;
                }

                //Handle godan verbs
                stem = verb.substring(0, verb.length() - 2); // remove "ない" for check verb type
                if (stem.endsWith("わ") || stem.endsWith("か") || stem.endsWith("が") || stem.endsWith("さ") ||
                        stem.endsWith("ざ") || stem.endsWith("た") || stem.endsWith("だ") || stem.endsWith("な") ||
                        stem.endsWith("は") || stem.endsWith("ば") || stem.endsWith("ぱ") || stem.endsWith("ま") || stem.endsWith("ら")) {
                    return VerbType.GODAN;
                }

                return VerbType.ICHIDAN;

            case PAST:
                // Handle irregular verbs
                if (verb.equals("した") || verb.equals("きた") || verb.equals("いった") || verb.equals("おった")) {
                    return VerbType.IRREGULAR;
                }

                // Handle godan verbs
                if (verb.endsWith("った") || verb.endsWith("んだ")) {
                    return VerbType.GODAN;
                }

                return VerbType.ICHIDAN;

            case TE_FORM:
                // Handle irregular verbs (する, 来る)
                if (verb.equals("して") || verb.equals("きて") || verb.equals("いって") || verb.equals("おって")) {
                    return VerbType.IRREGULAR;
                }

                //handle Godan verbs
                if (verb.endsWith("って") || verb.endsWith("んで")) {
                    return VerbType.GODAN;
                }

                return VerbType.ICHIDAN;

            case BASE:
                if (verb.equals("する") || verb.equals("くる") || verb.equals("いる") || verb.equals("いく") || verb.equals("おる")) {
                    return VerbType.IRREGULAR;
                } else if (verb.endsWith("る")) {
                    // Ichidan verbs generally end with る with preceding i/e sound
                    stem = verb.substring(0, verb.length() - 1); // remove "る" for check verb type
                    if (stem.endsWith("い") || stem.endsWith("き") || stem.endsWith("ぎ") || stem.endsWith("し") ||
                            stem.endsWith("じ") || stem.endsWith("ち") || stem.endsWith("ぢ") || stem.endsWith("に") ||
                            stem.endsWith("ひ") || stem.endsWith("び") || stem.endsWith("ぴ") || stem.endsWith("み") || stem.endsWith("り") ||
                            stem.endsWith("え") || stem.endsWith("け") || stem.endsWith("げ") || stem.endsWith("せ") ||
                            stem.endsWith("ぜ") || stem.endsWith("て") || stem.endsWith("で") || stem.endsWith("ね") ||
                            stem.endsWith("へ") || stem.endsWith("べ") || stem.endsWith("ぺ") || stem.endsWith("め") || stem.endsWith("れ")) {
                        return VerbType.ICHIDAN;
                    }
                }
                // Default to Godan
                return VerbType.GODAN;

            case POLITE:
                if (verb.equals("します") || verb.equals("きます") || verb.equals("います") || verb.equals("いきます") || verb.equals("おります")) {
                    return VerbType.IRREGULAR;
                }

                stem = verb.substring(0, verb.length() - 2); // remove "ます" for check verb type
                if (stem.endsWith("い") || stem.endsWith("き") || stem.endsWith("ぎ") || stem.endsWith("し") ||
                        stem.endsWith("じ") || stem.endsWith("ち") || stem.endsWith("ぢ") || stem.endsWith("に") ||
                        stem.endsWith("ひ") || stem.endsWith("び") || stem.endsWith("ぴ") || stem.endsWith("み") || stem.endsWith("り")) {
                    return VerbType.GODAN;
                }

                return VerbType.GODAN;
        }

        return VerbType.NULL;

    }

    // Method to conjugate base form to other forms
    public String conjugate(String verb, VerbForm form, HashMap<String, List<String>> verbMap) {
        List<String> conjugatedForms = verbMap.get(verb);
        if (conjugatedForms != null && !conjugatedForms.isEmpty()) {
            return conjugatedForms.get(form.getValue());
        }
        else {
            return verb;
        }
    }

    //Method to reconjugate other forms to base form いない =》 いる
    public String reconjugate(String verb, HashMap<String, List<String>> verbMap) {
        for (Map.Entry<String, List<String>> entry : verbMap.entrySet()) {
            String baseVerb = entry.getKey();
            List<String> conjugatedForms = entry.getValue();

            for (String conjugatedForm : conjugatedForms) {
                if (conjugatedForm.equals(verb)) {
                    return baseVerb;
                }
            }
        }

        return verb;
    }


    // Conjugate based on verb type
    public static String conjugateFromBase(String verb, VerbForm conjugationform, boolean isHougen) {
        VerbType type = getVerbType(verb, getVerbForm(verb));
        Log.d("conjugator", "verb: " + verb + " form: " + conjugationform + " type: " + type);


        switch (type) {
            case ICHIDAN:
                return conjugateIchidan(verb, conjugationform, isHougen);
            case GODAN:
                return conjugateGodan(verb, conjugationform, isHougen);
            case IRREGULAR:
                return conjugateIrregular(verb, conjugationform);
        }
        return verb; // Return the original verb if no match
    }

    private static String conjugateIchidan(String verb, VerbForm form, boolean isHougen) {
        String stem = verb.substring(0, verb.length() - 1);  // Remove "る"
        switch (form) {
            case NEGATIVE: return isHougen ? stem + "ん" : stem + "ない";
            case PAST: return stem + "た";
            case TE_FORM: return stem + "て";
            case POLITE: return stem + "ます";
            // Add more forms as needed
        }
        return verb;
    }

    private static String conjugateGodan(String verb, VerbForm form, boolean isHougen) {
        HashMap<String, List<String>> endingMap = new HashMap<>();
        String stem = verb.substring(0, verb.length() - 1);
        char lastChar = verb.charAt(verb.length() - 1);
        String newStem = changeGodanEnding(stem, lastChar, form, endingMap);
        Log.d("ending", "changeGodanEnding: " + newStem);// Changes ending based on form

        switch (form) {
            case NEGATIVE: return isHougen ? newStem + "ん" : newStem + "ない";
            case PAST:
                switch (lastChar) {
                    case 'ぐ': case 'ぶ': case 'む': return newStem + "だ";
                    default: return newStem + 'た';
                }
            case TE_FORM:
                switch (lastChar) {
                    case 'ぐ': case 'ぶ': case 'む': case 'に' : return newStem + "で";
                    default: return newStem + 'て';
                }
            case POLITE: return newStem + "ます";
            // Add more forms as needed
        }
        return verb;
    }

    private static String conjugateIrregular(String verb, VerbForm form) {
        // Handle する and 来る separately
        if (verb.equals("する")) {
            switch (form) {
                case NEGATIVE: return "しない";
                case PAST: return "した";
                case TE_FORM: return "して";
                case POLITE: return "します";
            }
        } else if (verb.equals("くる")) {
            switch (form) {
                case NEGATIVE: return "こない";
                case PAST: return "きた";
                case TE_FORM: return "きて";
                case POLITE: return "きます";
            }
        } else if (verb.equals("いる")) {
            switch (form) {
                case NEGATIVE: return "いない";
                case PAST: return "いった";
                case TE_FORM: return "いって";
                case POLITE: return "います";
            }
        } else if (verb.equals("いく")) {
            switch (form) {
                case NEGATIVE: return "いかない";
                case PAST: return "いった";
                case TE_FORM: return "いって";
                case POLITE: return "いきます";
            }
        } else if (verb.equals("おる")) {
            switch (form) {
                case NEGATIVE: return "おらん";
                case PAST: return "おった";
                case TE_FORM: return "おって";
                case POLITE: return "おります";
            }
        }

        return verb;
    }

    // Helper function to handle the stem change for Godan verbs
    private static String changeGodanEnding(String stem, char lastChar, VerbForm form, HashMap<String, List<String>> endingMap) {

        //Populate HashMap
        populateEndingMap("う", "わ", "っ", "っ", "い", endingMap);
        populateEndingMap("く", "か", "い", "い", "き", endingMap);
        populateEndingMap("ぐ", "が", "い", "い", "ぎ", endingMap);
        populateEndingMap("す", "さ", "し", "し", "し", endingMap);
        populateEndingMap("ず", "ざ", "", "", "じ", endingMap);
        populateEndingMap("つ", "た", "っ", "っ", "ち", endingMap);
        populateEndingMap("づ", "だ", "", "", "ぢ", endingMap);
        populateEndingMap("ぬ", "な", "ん", "ん", "に", endingMap);
        populateEndingMap("ふ", "は", "", "", "ひ", endingMap);
        populateEndingMap("ぶ", "ば", "ん", "ん", "び", endingMap);
        populateEndingMap("ぷ", "ぱ", "", "", "ぴ", endingMap);
        populateEndingMap("む", "ま", "ん", "ん", "み", endingMap);
        populateEndingMap("る", "ら", "っ", "っ", "り", endingMap);

        for (Map.Entry<String, List<String>> entry : endingMap.entrySet()) {
            String baseEnd = entry.getKey();
            List<String> conjugatedForms = entry.getValue();

            if (baseEnd.equals(String.valueOf(lastChar))) {
                return stem + conjugatedForms.get(form.getValue());
            }

        }
        return stem;
    }

    private static void populateEndingMap(String base, String negative, String past, String teForm, String polite, HashMap<String, List<String>> endingMap) {
        List<String> arr = new ArrayList<>();
        arr.add(base);
        arr.add(negative);
        arr.add(past);
        arr.add(teForm);
        arr.add(polite);
        endingMap.put(base, arr);
    }
}
