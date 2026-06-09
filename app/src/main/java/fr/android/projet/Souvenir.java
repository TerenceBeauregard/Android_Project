package fr.android.projet;

public class Souvenir {
    public int id;
    public String titre;
    public String description;  // vérifie que c'est là
    public String date;
    public String cheminPhoto;
    public String adresse;
    public double latitude;     // vérifie que c'est là
    public double longitude;    // vérifie que c'est là

    public Souvenir(int id, String titre, String date,
                    String cheminPhoto, String adresse) {
        this.id = id;
        this.titre = titre;
        this.date = date;
        this.cheminPhoto = cheminPhoto;
        this.adresse = adresse;
    }
}