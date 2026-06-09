package fr.android.projet;

public class Souvenir {
    public int id;
    public String titre;
    public String date;
    public String cheminPhoto;
    public String adresse;

    public Souvenir(int id, String titre, String date,
                    String cheminPhoto, String adresse) {
        this.id = id;
        this.titre = titre;
        this.date = date;
        this.cheminPhoto = cheminPhoto;
        this.adresse = adresse;
    }
}