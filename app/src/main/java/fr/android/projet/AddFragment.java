package fr.android.projet;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;


import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


import static android.app.Activity.RESULT_OK;

public class AddFragment extends Fragment {

    private ImageView ivPhoto;
    private EditText etTitre, etDescription;
    private File photoFile;
    private Uri photoUri;

    private static final int PERM_CAMERA = 200;

    // Remplace onActivityResult (déprécié) — lanceur d'Intent moderne
    private ActivityResultLauncher<Intent> cameraLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    new ActivityResultCallback<ActivityResult>() {
                        @Override
                        public void onActivityResult(ActivityResult result) {
                            if (result.getResultCode() == RESULT_OK) {
                                // La photo est sauvegardée dans photoFile
                                // On l'affiche en miniature dans l'ImageView
                                afficherMiniature();
                            }
                        }
                    }
            );

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_add, container, false);

        ivPhoto = view.findViewById(R.id.iv_photo);
        etTitre = view.findViewById(R.id.et_titre);
        etDescription = view.findViewById(R.id.et_description);

        Button btnPhoto = view.findViewById(R.id.btn_photo);
        Button btnSauvegarder = view.findViewById(R.id.btn_sauvegarder);

        btnPhoto.setOnClickListener(v -> demanderPermissionCamera());
        btnSauvegarder.setOnClickListener(v -> sauvegarder());

        return view;
    }

    private void demanderPermissionCamera() {
        if (ActivityCompat.checkSelfPermission(requireContext(),
                Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            // Permission déjà accordée
            lancerCamera();
        } else {
            // Demander la permission à l'utilisateur
            requestPermissions(
                    new String[]{ Manifest.permission.CAMERA },
                    PERM_CAMERA
            );
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == PERM_CAMERA) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // L'utilisateur a accepté
                lancerCamera();
            } else {
                Toast.makeText(requireContext(),
                        "Permission caméra refusée", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void lancerCamera() {
        // Créer le fichier vide qui recevra la photo
        photoFile = creerFichierPhoto();

        if (photoFile == null) {
            Toast.makeText(requireContext(),
                    R.string.erreur_camera, Toast.LENGTH_SHORT).show();
            return;
        }

        // Obtenir l'URI sécurisée via FileProvider
        photoUri = FileProvider.getUriForFile(
                requireContext(),
                requireActivity().getPackageName() + ".fileprovider",
                photoFile
        );

        // Intent implicite : on décrit l'action, Android trouve l'app caméra
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);

        // Lancer l'app caméra
        cameraLauncher.launch(intent);
    }

    private File creerFichierPhoto() {
        // Nom unique basé sur la date et l'heure
        String timestamp = new SimpleDateFormat(
                "yyyyMMdd_HHmmss", Locale.getDefault()
        ).format(new Date());
        String nomFichier = "PHOTO_" + timestamp;

        // Dossier Pictures dans le stockage privé de l'app
        File dossier = requireActivity()
                .getExternalFilesDir(Environment.DIRECTORY_PICTURES);

        try {
            return File.createTempFile(nomFichier, ".jpg", dossier);
        } catch (IOException e) {
            return null;
        }
    }

    private void afficherMiniature() {
        if (photoFile != null && photoFile.exists()) {
            Bitmap bitmap = BitmapFactory.decodeFile(photoFile.getAbsolutePath());
            ivPhoto.setImageBitmap(bitmap);
        }
    }

    private void sauvegarder() {
        String titre = etTitre.getText().toString().trim();
        String description = etDescription.getText().toString().trim();

        if (titre.isEmpty()) {
            etTitre.setError(getString(R.string.hint_titre));
            return;
        }

        if (photoFile == null || !photoFile.exists()) {
            Toast.makeText(requireContext(),
                    "Veuillez prendre une photo", Toast.LENGTH_SHORT).show();
            return;
        }

        // On stocke : titre, description, chemin de la photo, date
        String cheminPhoto = photoFile.getAbsolutePath();

        // Récupérer la dernière position GPS connue
        double latitude = ((MainActivity) requireActivity()).getLastLatitude();
        double longitude = ((MainActivity) requireActivity()).getLastLongitude();
        String adresse = ((MainActivity) requireActivity()).getLastAdresse();
        String date = new java.text.SimpleDateFormat(
                "yyyy-MM-dd", java.util.Locale.getDefault()
        ).format(new java.util.Date());

        // 1. Sauvegarder dans SQLite immédiatement
        DatabaseHelper db = new DatabaseHelper(requireContext());
        db.insertSouvenir(titre, description, latitude, longitude,
                adresse, cheminPhoto, date);

        // 2. Tenter la synchronisation avec Supabase
        SupabaseManager.insertSouvenir(titre, description, latitude,
                longitude, adresse, cheminPhoto,
                new SupabaseManager.SupabaseCallback() {
                    @Override
                    public void onSuccess(String result) {
                        Toast.makeText(requireContext(),
                                "Souvenir sauvegardé !", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(String error) {
                        // Sauvegardé en local même si Supabase échoue
                        Toast.makeText(requireContext(),
                                "Sauvegardé localement (pas de réseau)",
                                Toast.LENGTH_SHORT).show();
                    }
                }
        );

        Toast.makeText(requireContext(),
                "Souvenir sauvegardé !", Toast.LENGTH_SHORT).show();
    }
}