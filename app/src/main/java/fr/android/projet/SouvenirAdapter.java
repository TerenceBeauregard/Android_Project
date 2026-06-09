package fr.android.projet;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.util.List;

public class SouvenirAdapter extends ArrayAdapter<Souvenir> {

    private Context context;
    private List<Souvenir> souvenirs;

    public SouvenirAdapter(Context context, List<Souvenir> souvenirs) {
        super(context, 0, souvenirs);
        this.context = context;
        this.souvenirs = souvenirs;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Réutiliser la vue si possible
        if (convertView == null) {
            convertView = LayoutInflater.from(context)
                    .inflate(R.layout.item_souvenir, parent, false);
        }

        Souvenir souvenir = souvenirs.get(position);

        TextView tvTitre = convertView.findViewById(R.id.tv_titre);
        TextView tvDate = convertView.findViewById(R.id.tv_date);
        TextView tvLocation = convertView.findViewById(R.id.tv_location);
        ImageView ivMiniature = convertView.findViewById(R.id.iv_miniature);

        tvTitre.setText(souvenir.titre);
        tvDate.setText(souvenir.date);

        // Afficher l'adresse ou un message par défaut
        if (souvenir.adresse != null && !souvenir.adresse.isEmpty()) {
            tvLocation.setText(souvenir.adresse);
        } else {
            tvLocation.setText("Position non disponible");
        }

        // Charger la photo depuis le chemin local
        if (souvenir.cheminPhoto != null && !souvenir.cheminPhoto.isEmpty()) {
            File photoFile = new File(souvenir.cheminPhoto);
            if (photoFile.exists()) {
                Bitmap bitmap = BitmapFactory.decodeFile(souvenir.cheminPhoto);
                ivMiniature.setImageBitmap(bitmap);
            } else {
                ivMiniature.setImageResource(android.R.drawable.ic_menu_gallery);
            }
        } else {
            ivMiniature.setImageResource(android.R.drawable.ic_menu_gallery);
        }

        return convertView;
    }
}