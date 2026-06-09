package fr.android.projet;

import android.content.Context;
import android.content.SharedPreferences;

public class PreferencesManager {

    private static final String PREFS_NAME = "geomemory_prefs";
    private static final String KEY_LANGUE = "langue";
    private static final String LANGUE_FR = "fr";
    private static final String LANGUE_EN = "en";

    private SharedPreferences prefs;

    public PreferencesManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    // Sauvegarder la langue choisie
    public void sauvegarderLangue(String langue) {
        prefs.edit().putString(KEY_LANGUE, langue).apply();
    }

    // Récupérer la langue sauvegardée (français par défaut)
    public String getLangue() {
        return prefs.getString(KEY_LANGUE, LANGUE_FR);
    }

    // Vérifier si la langue est le français
    public boolean estFrancais() {
        return LANGUE_FR.equals(getLangue());
    }
}