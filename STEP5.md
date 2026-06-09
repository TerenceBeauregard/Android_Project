# Critère 5 — Stockage local SQLite et préférences utilisateur

## Vue d'ensemble

Le critère 5 repose sur deux mécanismes distincts qui travaillent ensemble :
- **SharedPreferences** : stockage des préférences utilisateur, ici la langue de l'application
- **SQLite** : base de données locale servant de cache et permettant le fonctionnement hors ligne

---

## 1. SharedPreferences

### Ce que c'est

SharedPreferences est un mécanisme Android de stockage de données simples sous forme
de paires clé-valeur. Il est adapté aux préférences utilisateur légères comme un choix
de langue, un thème ou un niveau de zoom. Les données sont sauvegardées dans un fichier
XML privé sur le téléphone et persistent entre les sessions — elles survivent à la
fermeture de l'app et au redémarrage du téléphone.

### Ce qui est stocké

Dans GeoMemory, SharedPreferences stocke uniquement la langue choisie par l'utilisateur
sous forme d'un code ISO : "fr" pour le français et "en" pour l'anglais. La valeur par
défaut est "fr" si aucune préférence n'a encore été enregistrée.

### La classe PreferencesManager

Toutes les opérations de lecture et écriture des SharedPreferences sont centralisées
dans une classe dédiée PreferencesManager. Cette centralisation évite de dupliquer
le nom du fichier de préférences et des clés dans plusieurs endroits du code. La
méthode sauvegarderLangue écrit la valeur avec apply qui effectue l'écriture de manière
asynchrone sans bloquer le thread UI. La méthode getLangue lit la valeur stockée et
retourne "fr" par défaut si aucune valeur n'existe encore.

### Le changement de langue

Quand l'utilisateur sélectionne une langue dans le SettingsFragment et appuie sur
Appliquer, trois opérations s'enchaînent. La langue est d'abord sauvegardée dans
SharedPreferences. Ensuite un objet Locale est créé avec le code langue et appliqué
à la configuration des ressources Android via updateConfiguration. Enfin la méthode
recreate est appelée sur l'Activity, ce qui la redémarre entièrement et force Android
à recharger toutes les ressources dans la nouvelle langue.

### L'application au démarrage

Sans intervention au démarrage, Android ignorerait la langue sauvegardée et utiliserait
la langue système du téléphone. Pour éviter cela, la méthode appliquerLangueSauvegardee
est appelée dans onCreate de MainActivity avant setContentView. En appliquant la locale
avant que l'interface soit dessinée, tous les textes chargés depuis les fichiers strings
utilisent directement la bonne langue sans avoir besoin de redémarrer.

---

## 2. SQLite

### Ce que c'est

SQLite est une base de données relationnelle complète qui vit dans un seul fichier sur
le téléphone. Contrairement à Supabase qui nécessite une connexion internet, SQLite est
entièrement local et fonctionne sans réseau. Android fournit une API native pour
interagir avec SQLite sans aucune librairie externe.

### SQLiteOpenHelper

La classe DatabaseHelper étend SQLiteOpenHelper, la classe Android qui gère le cycle
de vie de la base de données. La méthode onCreate est appelée automatiquement par
Android lors de la première ouverture de la base — elle crée la table souvenirs avec
toutes ses colonnes. La méthode onUpgrade est appelée quand le numéro de version de
la base augmente — elle supprime l'ancienne table et la recrée, ce qui permet de faire
évoluer le schéma entre les versions de l'app.

### La table souvenirs

La table locale est identique à celle de Supabase avec une colonne supplémentaire
essentielle : sync. Cette colonne entière vaut 0 quand le souvenir a été créé hors
ligne et n'a pas encore été envoyé à Supabase, et 1 quand le souvenir est synchronisé.
C'est ce champ qui permet à l'application de savoir quels souvenirs doivent être
envoyés au prochain retour en ligne.

### Les opérations CRUD

La classe DatabaseHelper expose quatre opérations principales. insertSouvenir insère
un nouveau souvenir avec sync=0 par défaut. getSouvenirs récupère tous les souvenirs
triés par date décroissante via un Cursor. getSouvenirstNonSync récupère uniquement
les souvenirs dont sync vaut 0, utilisés lors de la synchronisation. marquerSynchronise
met à jour sync=1 pour un souvenir dont l'envoi vers Supabase a réussi.

### Le Cursor

En SQLite Android, les résultats d'une requête sont retournés dans un objet Cursor.
Le Cursor se comporte comme un pointeur qui se déplace ligne par ligne dans les
résultats. On appelle moveToFirst pour se positionner sur la première ligne, puis
moveToNext dans une boucle pour parcourir toutes les lignes. Pour chaque ligne, on
extrait les valeurs colonne par colonne avec getColumnIndexOrThrow qui retourne l'index
de la colonne à partir de son nom, puis getString, getInt ou getDouble selon le type.
Le Cursor doit être fermé après utilisation pour libérer les ressources.

---

## 3. La synchronisation bidirectionnelle

### Le problème à résoudre

L'application doit fonctionner dans trois situations : en ligne avec du réseau, hors
ligne sans réseau, et au retour en ligne après avoir créé des souvenirs hors ligne.
Sans synchronisation bidirectionnelle, les deux bases vivent en parallèle sans jamais
se parler — les souvenirs créés hors ligne ne remontent jamais vers Supabase et les
souvenirs créés en ligne ne descendent jamais vers SQLite.

### La stratégie adoptée

Supabase est la source de vérité principale. SQLite est le cache local qui permet de
fonctionner sans réseau. La synchronisation s'effectue dans les deux sens à chaque
ouverture du fragment Mes souvenirs quand le réseau est disponible.

### Le flux complet en ligne

Quand l'utilisateur ouvre Mes souvenirs et que le réseau est disponible, l'application
suit quatre étapes dans l'ordre. Premièrement, elle interroge SQLite pour récupérer
tous les souvenirs dont sync vaut 0. Deuxièmement, pour chacun d'eux, elle envoie une
requête POST à Supabase et marque le souvenir sync=1 dans SQLite dès que l'envoi
réussit. Un compteur surveille quand tous les envois sont terminés. Troisièmement,
une fois tous les souvenirs non synchronisés traités, elle envoie une requête GET à
Supabase pour récupérer la liste complète à jour. Quatrièmement, elle écrase le
contenu de SQLite avec les données reçues de Supabase et affiche la liste.

### Le flux complet hors ligne

Quand le réseau n'est pas disponible, l'application vérifie la connectivité via
ConnectivityManager et charge directement les souvenirs depuis SQLite sans tenter
aucun appel réseau. L'utilisateur voit les dernières données synchronisées lors de
sa dernière connexion.

### La création hors ligne

Quand l'utilisateur crée un souvenir sans réseau, celui-ci est inséré dans SQLite
avec sync=0. L'appel à Supabase échoue et le toast "Sauvegardé localement" est
affiché. Au prochain retour en ligne, lors de l'ouverture de Mes souvenirs, le
souvenir avec sync=0 est détecté et envoyé automatiquement vers Supabase avant le
chargement de la liste.

### Le comportement d'Android face aux appels réseau

Android interdit strictement les appels réseau sur le thread principal. Tous les
appels vers Supabase sont donc exécutés sur des Threads séparés via SupabaseManager.
Si l'application tentait un appel réseau sur le thread UI, Android lèverait une
NetworkOnMainThreadException et planterait immédiatement. De plus, ConnectivityManager
permet de vérifier l'état du réseau de manière synchrone sur le thread UI avant de
décider quelle stratégie adopter, ce qui évite de lancer un Thread pour rien quand
on sait déjà qu'il n'y a pas de réseau.

---

## 4. Schéma de synchronisation

Ouverture de Mes souvenirs  
│  
isConnecte() ?  
│  
┌─────┴─────┐  
Oui         Non  
│            │  
▼            ▼  
SQLite        SQLite  
sync=0 ?    getSouvenirs()  
│            │  
┌──┴──┐         ▼  
Oui   Non     Afficher  
│     │  
▼     ▼  
POST  GET Supabase  
Supabase   │  
│         ▼  
▼      Sync SQLite  
sync=1     │  
│         ▼  
▼      Afficher  
GET Supabase  
│  
▼  
Sync SQLite  
│  
▼  
Afficher  

---

## 5. Responsabilités de chaque composant

| Composant | Rôle |
|---|---|
| SharedPreferences | Stockage persistant des préférences utilisateur clé-valeur |
| PreferencesManager | Centralise la lecture et l'écriture des SharedPreferences |
| Locale + updateConfiguration | Applique la langue choisie aux ressources Android |
| recreate() | Redémarre l'Activity pour recharger l'interface dans la nouvelle langue |
| appliquerLangueSauvegardee() | Applique la langue avant setContentView au démarrage |
| SQLiteOpenHelper | Gère la création et la mise à jour de la base de données locale |
| DatabaseHelper | Expose les opérations CRUD sur la base SQLite locale |
| Cursor | Parcourt les résultats d'une requête SQLite ligne par ligne |
| Colonne sync | Indique si un souvenir a été synchronisé avec Supabase |
| ConnectivityManager | Vérifie la disponibilité du réseau avant tout appel HTTP |
| envoyerSouvenirstNonSync() | Envoie vers Supabase les souvenirs créés hors ligne |
| syncDepuisSupabase() | Écrase SQLite avec les données Supabase pour maintenir la cohérence |
