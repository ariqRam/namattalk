package com.example.namapopup;

import android.util.Log;
import android.widget.Switch;

public class VerbConjugator {

    public enum VerbType {
        ICHIDAN, GODAN, IRREGULAR, NULL
    }

    public enum VerbForm {
        NEGATIVE, PAST, TE_FORM, BASE
    }

    public static VerbForm getVerbForm(String verb) {
        if (verb.endsWith("ない")) {
            return VerbForm.NEGATIVE;
        }

        if (verb.endsWith("た") || verb.endsWith("だ")) {
            return VerbForm.PAST;
        }

        if (verb.endsWith("て") || verb.endsWith("で")) {
            return VerbForm.TE_FORM;
        }

        return VerbForm.BASE;
    }

    // Detect verb type based on its ending and conjugate accordingly
    public static VerbType getVerbType(String verb, VerbForm form) {
        //in case of verb is base form
        String stem = "";
        switch (form) {
            case NEGATIVE:
                //Handle godan verbs
                stem = verb.substring(0, verb.length() - 2); // remove "ない" for check verb type
                if (stem.endsWith("わ") || stem.endsWith("か") || stem.endsWith("が") || stem.endsWith("さ") ||
                        stem.endsWith("ざ") || stem.endsWith("た") || stem.endsWith("だ") || stem.endsWith("な") ||
                        stem.endsWith("は") || stem.endsWith("ば") || stem.endsWith("ぱ") || stem.endsWith("ま") || stem.endsWith("ら")) {
                    return VerbType.GODAN;
                }

                //Handle irregular verbs
                if (verb.equals("しない") || verb.equals("こない")) {
                    return VerbType.IRREGULAR;
                }

                return VerbType.ICHIDAN;

            case PAST:
                // Handle godan verbs
                if (verb.endsWith("った") || verb.endsWith("んだ")) {
                    return VerbType.GODAN;
                }

                // Handle irregular verbs
                if (verb.equals("した") || verb.equals("きた")) {
                    return VerbType.ICHIDAN;
                }

                return VerbType.ICHIDAN;

            case TE_FORM:
                //handle Godan verbs
                if (verb.endsWith("って") || verb.endsWith("んで")) {
                    return VerbType.GODAN;
                }

                // Handle irregular verbs (する, 来る)
                if (verb.equals("して") || verb.equals("きて")) {
                    return VerbType.IRREGULAR;
                }

                return VerbType.ICHIDAN;

            case BASE:
                if (verb.endsWith("る")) {
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
                // Handle irregular verbs
                if (verb.equals("する") || verb.equals("くる")) {
                    return VerbType.IRREGULAR;
                }
                // Default to Godan
                return VerbType.GODAN;
        }

        return VerbType.NULL;

    }

    public static String reconjugateToBase(String verb) {
        VerbForm form = getVerbForm(verb);
        VerbType type = getVerbType(verb, form);
        Log.d("conjugator", "verb: " + verb + " form: " + form + " type: " + type);

        switch (type) {
            case ICHIDAN:
                return reconjugateIchidan(verb, form);
            case GODAN:
                return reconjugateGodan(verb, form);
            case IRREGULAR:
                return reconjugateIrregular(verb, form);
        }

        return verb;
    }

    private static String reconjugateIchidan(String verb, VerbForm form) {
        String stem = "";  // Remove "る"
        switch (form) {
            case NEGATIVE:
                stem = verb.substring(0, verb.length() - 2);
                return stem + "る";

            case TE_FORM: case PAST:
                stem = verb.substring(0, verb.length() - 1);
                return stem + "る";

        }

        return verb;
    }

    private static String reconjugateGodan(String verb, VerbForm form) {
        String stem = "";
        switch (form) {
            case NEGATIVE:
                stem = verb.substring(0, verb.length() - 3);
                return changeGodanEndingToBase(stem, verb.charAt(verb.length() - 3), form);

            case TE_FORM:

        }

        return verb;
    }

    private static String reconjugateIrregular(String verb, VerbForm form) {
        if (verb.startsWith("し")) {
            switch (form) {
                case NEGATIVE:
                    return "する";  // しない -> する
                case PAST:
                    return "する";  // した -> する
                case TE_FORM:
                    return "する";  // して -> する
            }
        } else if (verb.startsWith("き")) {
            switch (form) {
                case NEGATIVE:
                    return "くる";  // こない -> くる
                case PAST:
                    return "くる";  // きた -> くる
                case TE_FORM:
                    return "くる";  // きて -> くる
            }
        }
        return verb;
    }

    // Reverse Godan stem change to base form
    private static String changeGodanEndingToBase(String stem, char lastChar, VerbForm form) {
        switch (form) {
            case NEGATIVE:
                switch (lastChar) {
                    case 'わ': return stem + 'う';
                    case 'か': case 'き': case 'く': case 'け': case 'こ':
                        return stem + "く";
                    case 'が': case 'ぎ': case 'ぐ': case 'げ': case 'ご':
                        return stem + "ぐ";
                    case 'さ': case 'し': case 'す': case 'せ': case 'そ':
                        return stem + "す";
                    case 'ざ': case 'じ': case 'ず': case 'ぜ': case 'ぞ':
                        return stem + "ず";
                    case 'た': case 'ち': case 'つ': case 'て': case 'と':
                        return stem + 'つ';
                    case 'だ': case 'ぢ': case 'づ': case 'で': case 'ど':
                        return stem + 'づ';
                    case 'な': case 'に': case 'ぬ': case 'ね': case 'の':
                        return stem + 'ぬ';
                    case 'は': case 'ひ': case 'ふ': case 'へ': case 'ほ':
                        return stem + 'ふ';
                    case 'ば': case 'び': case 'ぶ': case 'べ': case 'ぼ':
                        return stem + 'ぶ';
                    case 'ぱ': case 'ぴ': case 'ぷ': case 'ぺ': case 'ぽ':
                        return stem + 'ぷ';
                    case 'ま': case 'み': case 'む': case 'め': case 'も':
                        return stem + 'む';
                    case 'ら': case 'り': case 'る': case 'れ': case 'ろ':
                        return stem + 'る';
                }
            case PAST:
                switch (lastChar) {
                    case 'っ': return stem + "る";
                }

            case TE_FORM: return stem + "て";

        }

        return stem;
    }



    // Conjugate based on verb type
    public static String conjugateFromBase(String verb, VerbForm conjugationform) {
        VerbType type = getVerbType(verb, getVerbForm(verb));
        Log.d("conjugator", "verb: " + verb + " form: " + conjugationform + " type: " + type);


        switch (type) {
            case ICHIDAN:
                return conjugateIchidan(verb, conjugationform);
            case GODAN:
                return conjugateGodan(verb, conjugationform);
            case IRREGULAR:
                return conjugateIrregular(verb, conjugationform);
        }
        return verb; // Return the original verb if no match
    }

    private static String conjugateIchidan(String verb, VerbForm form) {
        String stem = verb.substring(0, verb.length() - 1);  // Remove "る"
        switch (form) {
            case NEGATIVE: return stem + "ない";  // 食べる -> 食べない
            case PAST: return stem + "た";        // 食べる -> 食べた
            case TE_FORM: return stem + "て";     // 食べる -> 食べて// 食べる -> 食べられる
            // Add more forms as needed
        }
        return verb;
    }

    private static String conjugateGodan(String verb, VerbForm form) {
        String stem = verb.substring(0, verb.length() - 1);
        char lastChar = verb.charAt(verb.length() - 1);
        String newStem = changeGodanEnding(stem, lastChar, form);  // Changes ending based on form

        switch (form) {
            case NEGATIVE: return newStem + "ん";   // 書く -> 書かない
            case PAST:
                switch (lastChar) {
                    case 'ぐ': case 'ぶ': case 'む': return newStem + "だ";
                    default: return newStem + 'た';
                }
            case TE_FORM:
                switch (lastChar) {
                    case 'ぐ': case 'ぶ': case 'む': return newStem + "で";
                    default: return newStem + 'て';
                }

            // Add more forms as needed
        }
        return verb;
    }

    private static String conjugateIrregular(String verb, VerbForm form) {
        // Handle する and 来る separately
        if (verb.equals("する")) {
            switch (form) {
                case NEGATIVE: return "しない";  // する -> しない
                case PAST: return "した";        // する -> した
                case TE_FORM: return "して";     // する -> して
            }
        } else if (verb.equals("くる")) {
            switch (form) {
                case NEGATIVE: return "こない";  // 来る -> 来ない
                case PAST: return "きた";        // 来る -> 来た
                case TE_FORM: return "きて";     // 来る -> 来て
            }
        }
        return verb;
    }

    // Helper function to handle the stem change for Godan verbs
    private static String changeGodanEnding(String stem, char lastChar, VerbForm form) {
        switch (lastChar) {
            case 'う': return form == VerbForm.NEGATIVE ? stem + "わ" : stem + "っ"; // う -> わ for negative, い for others
            case 'く': return form == VerbForm.NEGATIVE ? stem + "か" : stem + "い";
            case 'ぐ': return form == VerbForm.NEGATIVE ? stem + "が" : stem + "い";
            case 'す': return form == VerbForm.NEGATIVE ? stem + "さ" : stem + "し";
            case 'ず': return form == VerbForm.NEGATIVE ? stem + "ざ" : stem + "";
            case 'つ': return form == VerbForm.NEGATIVE ? stem + "た" : stem + "っ";
            case 'づ': return form == VerbForm.NEGATIVE ? stem + "だ" : stem + "";
            case 'ぬ': return form == VerbForm.NEGATIVE ? stem + "な" : stem + "";
            case 'ふ': return form == VerbForm.NEGATIVE ? stem + "は" : stem + "";
            case 'ぶ': return form == VerbForm.NEGATIVE ? stem + "ば" : stem + "ん";
            case 'ぷ': return form == VerbForm.NEGATIVE ? stem + "ぱ" : stem + "";
            case 'む': return form == VerbForm.NEGATIVE ? stem + "ま" : stem + "ん";
            case 'る': return form == VerbForm.NEGATIVE ? stem + "ら" : stem + "っ";
        }
        return stem;
    }
}
