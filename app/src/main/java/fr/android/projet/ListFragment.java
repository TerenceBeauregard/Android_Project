package fr.android.projet;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

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

                    tvChargement.setVisibility(View.GONE);

                    // Utiliser notre adapter personnalisé
                    SouvenirAdapter adapter = new SouvenirAdapter(
                            requireContext(), liste
                    );
                    lvSouvenirs.setAdapter(adapter);

                } catch (Exception e) {
                    tvChargement.setText("Erreur de chargement");
                }
            }

            @Override
            public void onError(String error) {
                tvChargement.setText("Erreur : " + error);
            }
        });
    }
}