package fr.android.projet;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import org.json.JSONObject;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class MapFragment extends Fragment implements LocationListener {

    private static final int PERM_REQUEST = 100;

    private MapView mapView;
    private TextView tvLatitude, tvLongitude, tvAdresse;
    private LocationManager locationManager;
    private Marker positionMarker;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Configure osmdroid
        Configuration.getInstance().setUserAgentValue(
                requireActivity().getPackageName()
        );

        View view = inflater.inflate(R.layout.fragment_map, container, false);

        // Récupérer les Views
        mapView = view.findViewById(R.id.map);
        tvLatitude = view.findViewById(R.id.tv_latitude);
        tvLongitude = view.findViewById(R.id.tv_longitude);
        tvAdresse = view.findViewById(R.id.tv_adresse);

        // Configurer la carte
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
        mapView.getController().setZoom(15.0);

        // Centrer sur Paris par défaut
        GeoPoint paris = new GeoPoint(48.8566, 2.3522);
        mapView.getController().setCenter(paris);

        // Créer le marqueur de position
        positionMarker = new Marker(mapView);
        positionMarker.setTitle("Ma position");
        mapView.getOverlays().add(positionMarker);

        // Démarrer le GPS
        locationManager = (LocationManager) requireActivity()
                .getSystemService(requireActivity().LOCATION_SERVICE);
        demanderPermissions();

        return view;
    }

    private void demanderPermissions() {
        if (ActivityCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // Permission déjà accordée → démarrer le GPS
            demarrerGPS();
        } else {
            // Demander la permission à l'utilisateur
            requestPermissions(
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    },
                    PERM_REQUEST
            );
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == PERM_REQUEST) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                demarrerGPS();
            }
        }
    }

    private void demarrerGPS() {
        if (ActivityCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        // Mise à jour toutes les 10 secondes ou si déplacement > 5 mètres
        locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                10000,
                5,
                this
        );
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        double lat = location.getLatitude();
        double lng = location.getLongitude();

        ((MainActivity) requireActivity()).setLastLocation(lat, lng);

        // Mettre à jour les TextViews
        tvLatitude.setText("Latitude : " + lat);
        tvLongitude.setText("Longitude : " + lng);

        // Déplacer le marqueur sur la carte
        GeoPoint point = new GeoPoint(lat, lng);
        positionMarker.setPosition(point);
        mapView.getController().animateTo(point);
        mapView.invalidate();

        // Lancer le géocodage inverse en arrière-plan
        lancerGeocodage(lat, lng);
    }

    private void lancerGeocodage(double lat, double lng) {
        // Handler attaché au thread UI pour pouvoir modifier les Views après
        Handler handler = new Handler(Looper.getMainLooper());

        new Thread(() -> {
            // Ce bloc s'exécute sur un thread séparé pour le pas bloquer le thread UI
            String adresse = appelNominatim(lat, lng);

            // Retour sur le thread UI pour mettre à jour le TextView
            handler.post(() -> {
                if (tvAdresse != null) {
                    tvAdresse.setText(adresse);
                }
                // Sauvegarder l'adresse dans MainActivity
                ((MainActivity) requireActivity()).setLastAdresse(adresse);
            });

        }).start();
    }

    private String appelNominatim(double lat, double lng) {
        try {
            String urlStr = "https://nominatim.openstreetmap.org/reverse"
                    + "?format=json"
                    + "&lat=" + lat
                    + "&lon=" + lng
                    + "&zoom=18"
                    + "&addressdetails=1";

            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", requireActivity().getPackageName());

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream())
            );
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();

            JSONObject json = new JSONObject(sb.toString());
            return json.getString("display_name");

        } catch (Exception e) {
            return "Adresse introuvable";
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
        // Arrêter le GPS quand le fragment n'est plus visible
        locationManager.removeUpdates(this);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mapView.onDetach();
    }
}