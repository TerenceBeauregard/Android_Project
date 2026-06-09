# Critère 4 — Stockage et lecture depuis une base de données externe (Supabase)

## Vue d'ensemble

Le critère 4 repose sur trois mécanismes distincts qui travaillent ensemble :
- **Supabase** : base de données PostgreSQL distante exposant une API REST
- **HttpURLConnection** : API Android native pour effectuer les appels HTTP vers Supabase
- **Thread + Handler** : exécution des appels réseau en arrière-plan pour ne pas bloquer l'interface

---

## 1. Supabase et son API REST

### Ce qu'est Supabase

Supabase est un service de base de données PostgreSQL hébergé dans le cloud. Il expose
automatiquement une API REST pour chaque table créée, ce qui permet de lire et écrire
des données depuis n'importe quelle application via de simples requêtes HTTP. Dans le
cadre du projet, on n'utilise pas le SDK Supabase pour Android — tous les appels sont
effectués manuellement via HttpURLConnection, ce qui respecte le critère 0.

### La table souvenirs

La table stocke toutes les métadonnées d'un souvenir : un identifiant unique auto-incrémenté,
le titre, la description, les coordonnées GPS (latitude et longitude), l'adresse géocodée,
le chemin local vers la photo et la date de création générée automatiquement par le serveur.

### Row Level Security

Supabase protège par défaut ses tables avec un système de sécurité par ligne appelé Row
Level Security. Dans le cadre de ce projet qui ne comporte pas de système d'authentification
utilisateur, ce mécanisme est désactivé pour permettre un accès libre aux données depuis
l'application.

### Les clés API

Supabase fournit deux types de clés. La Publishable key est destinée aux applications
mobiles et peut être exposée publiquement dans le code. La Secret key donne un accès
total sans restriction et ne doit jamais être utilisée dans une application mobile.
Seule la Publishable key est utilisée dans GeoMemory.

---

## 2. Les appels HTTP

### Les headers obligatoires

Chaque requête envoyée à Supabase doit obligatoirement inclure deux headers
d'authentification : apikey qui contient la clé publique, et Authorization avec le
format Bearer suivi de la même clé. Sans ces headers, Supabase rejette la requête avec
une erreur d'authentification. Le header Content-Type indique que le corps de la requête
est au format JSON, et le header Prefer avec la valeur return=representation demande à
Supabase de retourner l'objet inséré dans sa réponse.

### Les méthodes HTTP

L'API REST de Supabase suit les conventions standard. Une requête GET récupère les
données, une requête POST insère un nouvel enregistrement et une requête DELETE supprime
un enregistrement. Les filtres et paramètres de tri sont passés directement dans l'URL
sous forme de query parameters — par exemple order=date_creation.desc pour trier par
date décroissante, ou id=eq.5 pour filtrer par identifiant.

### Thread et Handler

Comme pour le géocodage du critère 2, tous les appels HTTP vers Supabase sont exécutés
sur un Thread séparé pour ne pas bloquer le thread UI. Une fois la réponse reçue, le
Handler permet de repasser sur le thread UI pour mettre à jour l'interface. Un système
de callback via une interface SupabaseCallback notifie le Fragment appelant du succès
ou de l'échec de l'opération.

---

## 3. L'architecture des classes

### SupabaseConfig

Classe de configuration centralisée qui contient l'URL du projet Supabase et la clé
API publique. Centraliser ces valeurs dans une seule classe permet de les modifier
facilement sans avoir à chercher dans tout le code.

### SupabaseManager

Classe utilitaire qui gère tous les appels HTTP vers Supabase. Elle expose des méthodes
statiques pour insérer un souvenir, récupérer tous les souvenirs et supprimer un
souvenir. Chaque méthode crée un Thread, construit la requête HTTP, lit la réponse et
notifie le Fragment via le callback. La méthode centrale envoyerRequete factorise la
logique commune à tous les appels — construction de l'URL, ajout des headers,
écriture du body et lecture de la réponse.

### Le callback SupabaseCallback

Interface Java qui définit deux méthodes : onSuccess appelée avec la réponse JSON
quand la requête réussit, et onError appelée avec le message d'erreur en cas d'échec.
Ce pattern permet aux Fragments de réagir au résultat de l'appel réseau sans avoir
à connaître les détails de l'implémentation HTTP.

---

## 4. Les données sauvegardées

### À l'insertion

Quand l'utilisateur valide le formulaire dans AddFragment, l'application récupère le
titre et la description saisis, les coordonnées GPS stockées dans MainActivity par
le MapFragment, l'adresse géocodée également stockée dans MainActivity, et le chemin
absolu du fichier photo sur le téléphone. Ces données sont assemblées dans un objet
JSON et envoyées à Supabase via une requête POST.

### À la lecture

Quand l'utilisateur ouvre le fragment Mes souvenirs, l'application envoie une requête
GET à Supabase qui retourne un tableau JSON contenant tous les souvenirs triés par date
décroissante. Chaque objet JSON est parsé pour en extraire les champs et créer un objet
Souvenir. La liste d'objets Souvenir est ensuite passée au SouvenirAdapter pour
l'affichage.

---

## 5. L'affichage de la liste

### Le modèle Souvenir

Classe Java simple qui représente un souvenir avec tous ses champs : identifiant, titre,
date, chemin photo et adresse. Elle sert de conteneur de données entre le parsing JSON
du ListFragment et l'affichage dans le SouvenirAdapter.

### Le SouvenirAdapter

Adapter personnalisé qui étend ArrayAdapter et définit comment chaque souvenir est
affiché dans la liste. Pour chaque ligne visible, il charge le layout item_souvenir.xml,
remplit le titre, la date et l'adresse dans les TextViews correspondants, puis tente
de charger l'image depuis le chemin local stocké en base. Si le fichier photo n'existe
pas sur l'appareil, une icône placeholder est affichée à la place.

### La réutilisation des vues

Le SouvenirAdapter réutilise les vues déjà créées quand l'utilisateur scrolle dans la
liste. Quand une ligne disparaît en haut de l'écran, Android la passe en paramètre
convertView pour qu'elle soit recyclée en bas. On remplace uniquement le contenu de
la ligne sans recréer le layout depuis zéro, ce qui rend le scroll fluide même avec
un grand nombre de souvenirs.

### Le chemin photo local

Le chemin stocké dans Supabase est le chemin absolu du fichier sur le téléphone qui a
pris la photo. Ce chemin n'est valide que sur cet appareil spécifique. Sur un autre
téléphone ou après une réinstallation de l'application, le fichier n'existera plus à
cet emplacement et le placeholder s'affichera à la place. C'est un comportement attendu
dans l'architecture actuelle où SQLite sert de cache local et Supabase de sauvegarde
distante.

---

## 6. Responsabilités de chaque composant

| Composant | Rôle |
|---|---|
| Supabase | Base de données PostgreSQL distante exposant une API REST |
| Table souvenirs | Stocke les métadonnées de chaque souvenir |
| SupabaseConfig | Centralise l'URL et la clé API |
| SupabaseManager | Gère tous les appels HTTP vers Supabase |
| SupabaseCallback | Interface de notification du résultat au Fragment appelant |
| Thread | Exécute les appels HTTP sur un fil d'exécution séparé |
| Handler | Repasse sur le thread UI pour mettre à jour l'interface |
| HttpURLConnection | API Android native pour les requêtes HTTP |
| JSONObject | Construit le corps JSON des requêtes POST |
| JSONArray | Parse le tableau JSON retourné par les requêtes GET |
| Souvenir | Modèle de données représentant un souvenir |
| SouvenirAdapter | Construit chaque ligne de la liste avec image et texte |
| item_souvenir.xml | Layout d'une ligne de la liste avec miniature et texte |