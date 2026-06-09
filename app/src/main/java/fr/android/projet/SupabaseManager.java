package fr.android.projet;

import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class SupabaseManager {

    // Interface callback pour retourner les résultats sur le thread UI
    public interface SupabaseCallback {
        void onSuccess(String result);
        void onError(String error);
    }

    // Insérer un souvenir
    public static void insertSouvenir(String titre, String description,
                                      double latitude, double longitude,
                                      String adresse,
                                      String cheminPhoto,
                                      SupabaseCallback callback) {
        Handler handler = new Handler(Looper.getMainLooper());

        new Thread(() -> {
            try {
                // Construire le JSON à envoyer
                JSONObject json = new JSONObject();
                json.put("titre", titre);
                json.put("description", description);
                json.put("latitude", latitude);
                json.put("longitude", longitude);
                json.put("adresse", adresse);
                json.put("chemin_photo", cheminPhoto);

                String response = envoyerRequete("POST",
                        SupabaseConfig.TABLE_SOUVENIRS,
                        json.toString(),
                        null);

                handler.post(() -> callback.onSuccess(response));

            } catch (Exception e) {
                handler.post(() -> callback.onError(e.getMessage()));
            }
        }).start();
    }

    // Récupérer tous les souvenirs
    public static void getSouvenirs(SupabaseCallback callback) {
        Handler handler = new Handler(Looper.getMainLooper());

        new Thread(() -> {
            try {
                String response = envoyerRequete("GET",
                        SupabaseConfig.TABLE_SOUVENIRS,
                        null,
                        "select=*&order=date_creation.desc");

                handler.post(() -> callback.onSuccess(response));

            } catch (Exception e) {
                handler.post(() -> callback.onError(e.getMessage()));
            }
        }).start();
    }

    // Supprimer un souvenir par son id
    public static void deleteSouvenir(int id, SupabaseCallback callback) {
        Handler handler = new Handler(Looper.getMainLooper());

        new Thread(() -> {
            try {
                String response = envoyerRequete("DELETE",
                        SupabaseConfig.TABLE_SOUVENIRS,
                        null,
                        "id=eq." + id);

                handler.post(() -> callback.onSuccess(response));

            } catch (Exception e) {
                handler.post(() -> callback.onError(e.getMessage()));
            }
        }).start();
    }

    // Méthode centrale qui construit et envoie la requête HTTP
    private static String envoyerRequete(String methode, String table,
                                         String body, String queryParams)
            throws Exception {

        // Construire l'URL
        String urlStr = SupabaseConfig.URL + "/rest/v1/" + table;
        if (queryParams != null && !queryParams.isEmpty()) {
            urlStr += "?" + queryParams;
        }

        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(methode);

        // Headers obligatoires pour Supabase
        conn.setRequestProperty("apikey", SupabaseConfig.API_KEY);
        conn.setRequestProperty("Authorization", "Bearer " + SupabaseConfig.API_KEY);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Prefer", "return=representation");

        // Envoyer le body si nécessaire (POST)
        if (body != null) {
            conn.setDoOutput(true);
            OutputStream os = conn.getOutputStream();
            os.write(body.getBytes(StandardCharsets.UTF_8));
            os.close();
        }

        // Lire la réponse
        int responseCode = conn.getResponseCode();
        BufferedReader reader;

        if (responseCode >= 200 && responseCode < 300) {
            reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream())
            );
        } else {
            reader = new BufferedReader(
                    new InputStreamReader(conn.getErrorStream())
            );
        }

        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        reader.close();

        return sb.toString();
    }
}