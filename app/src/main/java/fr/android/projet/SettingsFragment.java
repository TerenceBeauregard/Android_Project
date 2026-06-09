package fr.android.projet;

import android.content.res.Configuration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import java.util.Locale;

public class SettingsFragment extends Fragment {

    private RadioGroup rgLangue;
    private RadioButton rbFrancais, rbAnglais;
    private PreferencesManager prefsManager;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        prefsManager = new PreferencesManager(requireContext());

        rgLangue = view.findViewById(R.id.rg_langue);
        rbFrancais = view.findViewById(R.id.rb_francais);
        rbAnglais = view.findViewById(R.id.rb_anglais);
        Button btnAppliquer = view.findViewById(R.id.btn_appliquer);

        // Cocher le bouton correspondant à la langue sauvegardée
        if (prefsManager.estFrancais()) {
            rbFrancais.setChecked(true);
        } else {
            rbAnglais.setChecked(true);
        }

        btnAppliquer.setOnClickListener(v -> appliquerLangue());

        return view;
    }

    private void appliquerLangue() {
        String langue = rbFrancais.isChecked() ? "fr" : "en";

        // Sauvegarder dans SharedPreferences
        prefsManager.sauvegarderLangue(langue);

        // Appliquer la langue immédiatement
        changerLangue(langue);

        Toast.makeText(requireContext(),
                langue.equals("fr") ? "Langue changée en français"
                        : "Language changed to English",
                Toast.LENGTH_SHORT).show();
    }

    private void changerLangue(String langue) {
        Locale locale = new Locale(langue);
        Locale.setDefault(locale);

        Configuration config = new Configuration();
        config.setLocale(locale);

        requireActivity().getResources()
                .updateConfiguration(config,
                        requireActivity().getResources().getDisplayMetrics());

        // Redémarrer l'Activity pour appliquer la langue partout
        requireActivity().recreate();
    }
}