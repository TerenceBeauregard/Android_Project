package fr.android.projet;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "geomemory.db";
    private static final int DB_VERSION = 1;

    private static final String TABLE_SOUVENIRS = "souvenirs";
    private static final String COL_ID = "id";
    private static final String COL_TITRE = "titre";
    private static final String COL_DESCRIPTION = "description";
    private static final String COL_LATITUDE = "latitude";
    private static final String COL_LONGITUDE = "longitude";
    private static final String COL_ADRESSE = "adresse";
    private static final String COL_CHEMIN_PHOTO = "chemin_photo";
    private static final String COL_DATE = "date_creation";
    private static final String COL_SYNC = "sync";

    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Création de la table locale
        String createTable = "CREATE TABLE " + TABLE_SOUVENIRS + " ("
                + COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_TITRE + " TEXT NOT NULL, "
                + COL_DESCRIPTION + " TEXT, "
                + COL_LATITUDE + " REAL, "
                + COL_LONGITUDE + " REAL, "
                + COL_ADRESSE + " TEXT, "
                + COL_CHEMIN_PHOTO + " TEXT, "
                + COL_DATE + " TEXT, "
                + COL_SYNC + " INTEGER DEFAULT 0"  // 0 = pas synchronisé, 1 = synchronisé
                + ")";
        db.execSQL(createTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SOUVENIRS);
        onCreate(db);
    }

    // Insérer un souvenir localement
    public long insertSouvenir(String titre, String description,
                               double latitude, double longitude,
                               String adresse, String cheminPhoto,
                               String date) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_TITRE, titre);
        values.put(COL_DESCRIPTION, description);
        values.put(COL_LATITUDE, latitude);
        values.put(COL_LONGITUDE, longitude);
        values.put(COL_ADRESSE, adresse);
        values.put(COL_CHEMIN_PHOTO, cheminPhoto);
        values.put(COL_DATE, date);
        values.put(COL_SYNC, 0);

        long id = db.insert(TABLE_SOUVENIRS, null, values);
        db.close();
        return id;
    }

    // Récupérer tous les souvenirs locaux
    public List<Souvenir> getSouvenirs() {
        List<Souvenir> liste = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(TABLE_SOUVENIRS, null, null, null,
                null, null, COL_DATE + " DESC");

        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow(COL_ID));
                String titre = cursor.getString(cursor.getColumnIndexOrThrow(COL_TITRE));
                String date = cursor.getString(cursor.getColumnIndexOrThrow(COL_DATE));
                String cheminPhoto = cursor.getString(cursor.getColumnIndexOrThrow(COL_CHEMIN_PHOTO));
                String adresse = cursor.getString(cursor.getColumnIndexOrThrow(COL_ADRESSE));

                liste.add(new Souvenir(id, titre, date, cheminPhoto, adresse));
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return liste;
    }

    // Marquer un souvenir comme synchronisé
    public void marquerSynchronise(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_SYNC, 1);
        db.update(TABLE_SOUVENIRS, values, COL_ID + "=?",
                new String[]{String.valueOf(id)});
        db.close();
    }

    // Récupérer les souvenirs non synchronisés
    public List<Souvenir> getSouvenirstNonSync() {
        List<Souvenir> liste = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(TABLE_SOUVENIRS, null,
                COL_SYNC + "=0", null, null, null, null);

        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow(COL_ID));
                String titre = cursor.getString(cursor.getColumnIndexOrThrow(COL_TITRE));
                String date = cursor.getString(cursor.getColumnIndexOrThrow(COL_DATE));
                String cheminPhoto = cursor.getString(cursor.getColumnIndexOrThrow(COL_CHEMIN_PHOTO));
                String adresse = cursor.getString(cursor.getColumnIndexOrThrow(COL_ADRESSE));

                liste.add(new Souvenir(id, titre, date, cheminPhoto, adresse));
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return liste;
    }

    // Remplace tous les souvenirs locaux par ceux de Supabase
    public void syncDepuisSupabase(List<Souvenir> souvenirs) {
        SQLiteDatabase db = this.getWritableDatabase();

        // Vider la table locale
        db.delete(TABLE_SOUVENIRS, null, null);

        // Réinsérer tous les souvenirs depuis Supabase
        for (Souvenir s : souvenirs) {
            ContentValues values = new ContentValues();
            values.put(COL_TITRE, s.titre);
            values.put(COL_ADRESSE, s.adresse);
            values.put(COL_CHEMIN_PHOTO, s.cheminPhoto);
            values.put(COL_DATE, s.date);
            values.put(COL_SYNC, 1); // déjà synchronisé
            db.insert(TABLE_SOUVENIRS, null, values);
        }

        db.close();
    }
}