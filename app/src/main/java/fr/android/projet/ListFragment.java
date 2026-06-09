package fr.android.projet;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ListFragment extends Fragment {

    private ListView lvSouvenirs;
    private TextView tvChargement;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_list, container, false);

        lvSouvenirs = view.findViewById(R.id.lv_souvenirs);
        tvChargement = view.findViewById(R.id.tv_chargement);

        chargerSouvenirs();

        return view;
    }

    private void chargerSouvenirs() {
        tvChargement.setText(R.string.chargement);

        if (isConnecte()) {
            // En ligne : d'abord envoyer ce qui est en attente, puis charger
            envoyerSouvenirstNonSync();
        } else {
            // Hors ligne : charger depuis SQLite directement
            chargerDepuisSQLite();
        }
    }

    private boolean isConnecte() {
        ConnectivityManager cm = (ConnectivityManager)
                requireContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }

    // Étape 1 : envoyer les souvenirs hors ligne vers Supabase
    private void envoyerSouvenirstNonSync() {
        DatabaseHelper db = new DatabaseHelper(requireContext());
        List<Souvenir> nonSync = db.getSouvenirstNonSync();

        if (nonSync.isEmpty()) {
            // Rien à envoyer → charger directement
            chargerDepuisSupabase();
            return;
        }

        // Compteur pour savoir quand tous les envois sont terminés
        final int[] restants = {nonSync.size()};

        for (Souvenir s : nonSync) {
            SupabaseManager.insertSouvenir(
                    s.titre,
                    s.description != null ? s.description : "",
                    s.latitude,
                    s.longitude,
                    s.adresse != null ? s.adresse : "",
                    s.cheminPhoto != null ? s.cheminPhoto : "",
                    new SupabaseManager.SupabaseCallback() {
                        @Override
                        public void onSuccess(String result) {
                            // Marquer comme synchronisé dans SQLite
                            db.marquerSynchronise(s.id);

                            restants[0]--;
                            if (restants[0] == 0) {
                                // Tous envoyés → charger la liste depuis Supabase
                                chargerDepuisSupabase();
                            }
                        }

                        @Override
                        public void onError(String error) {
                            restants[0]--;
                            if (restants[0] == 0) {
                                // Certains ont échoué mais on charge quand même
                                chargerDepuisSupabase();
                            }
                        }
                    }
            );
        }
    }

    // Étape 2 : charger depuis Supabase et mettre à jour SQLite
    private void chargerDepuisSupabase() {
        SupabaseManager.getSouvenirs(new SupabaseManager.SupabaseCallback() {
            @Override
            public void onSuccess(String result) {
                try {
                    JSONArray array = new JSONArray(result);
                    ArrayList<Souvenir> liste = new ArrayList<>();

                    for (int i = 0; i < array.length(); i++) {
                        JSONObject obj = array.getJSONObject(i);
                        int id = obj.getInt("id");
                        String titre = obj.getString("titre");
                        String date = obj.getString("date_creation").substring(0, 10);
                        String cheminPhoto = obj.optString("chemin_photo", "");
                        String adresse = obj.optString("adresse", "");

                        liste.add(new Souvenir(id, titre, date, cheminPhoto, adresse));
                    }

                    // Mettre à jour SQLite avec les données Supabase
                    syncVersSQLite(liste);
                    afficherListe(liste);

                } catch (Exception e) {
                    chargerDepuisSQLite();
                }
            }

            @Override
            public void onError(String error) {
                chargerDepuisSQLite();
            }
        });
    }

    private void syncVersSQLite(List<Souvenir> liste) {
        DatabaseHelper db = new DatabaseHelper(requireContext());
        db.syncDepuisSupabase(liste);
    }

    private void chargerDepuisSQLite() {
        DatabaseHelper db = new DatabaseHelper(requireContext());
        List<Souvenir> liste = db.getSouvenirs();
        afficherListe(liste);
    }

    private void afficherListe(List<Souvenir> liste) {
        tvChargement.setVisibility(View.GONE);
        SouvenirAdapter adapter = new SouvenirAdapter(requireContext(), liste);
        lvSouvenirs.setAdapter(adapter);
    }
}