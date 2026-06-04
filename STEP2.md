# Critère 2 — Géolocalisation, carte et géocodage inverse

## Vue d'ensemble

Le critère 2 repose sur trois mécanismes distincts qui travaillent ensemble :
- **LocationManager** : récupère les coordonnées GPS du téléphone en temps réel
- **osmdroid** : affiche une carte OpenStreetMap et place un marqueur sur la position
- **Nominatim** : transforme les coordonnées GPS en adresse lisible via internet

---

## 1. Les permissions

Android applique un système de sécurité en deux niveaux pour accéder au GPS.

Le premier niveau est la déclaration dans le fichier Manifest. C'est une déclaration
d'intention lue par Android au moment de l'installation de l'app. Elle indique que
l'application est susceptible d'utiliser le GPS et internet, sans pour autant y avoir
accès immédiatement.

Le second niveau est la demande à l'exécution. Depuis Android 6.0, déclarer la permission
dans le Manifest ne suffit plus. Au moment où l'app a réellement besoin du GPS, elle doit
afficher une boîte de dialogue système et demander à l'utilisateur d'accepter ou refuser.
Si l'utilisateur refuse, le GPS reste inactif et l'app doit fonctionner sans.

---

## 2. LocationManager

### Ce que c'est

LocationManager est un service système Android qui tourne en permanence sur le téléphone.
Il gère directement le matériel GPS et distribue les positions à toutes les applications
qui en font la demande. On n'instancie pas ce service, on y accède via le système Android.

### Comment il fonctionne

Après avoir obtenu la permission de l'utilisateur, l'application s'abonne aux mises à jour
GPS en précisant deux conditions : un intervalle de temps minimum (par exemple 10 secondes)
et une distance minimum de déplacement (par exemple 5 mètres). Android n'envoie une nouvelle
position que si au moins une des deux conditions est remplie, ce qui évite de vider la
batterie inutilement.

### La réception des positions

Le Fragment implémente l'interface LocationListener, ce qui l'oblige à définir une méthode
onLocationChanged. Android appelle cette méthode automatiquement sur le thread principal
dès qu'une nouvelle position GPS est disponible. Le développeur n'appelle jamais cette
méthode manuellement.

### La gestion de la batterie

Quand le Fragment n'est plus visible (l'utilisateur change d'écran ou met l'app en
arrière-plan), l'application se désabonne du LocationManager. Continuer à recevoir des
positions GPS en arrière-plan consommerait inutilement la batterie de l'utilisateur.

---

## 3. osmdroid et OpenStreetMap

### Ce que c'est

osmdroid est une librairie qui télécharge et affiche les tuiles OpenStreetMap. Une tuile
est une image carrée de 256x256 pixels représentant une zone géographique à un niveau de
zoom précis. osmdroid télécharge automatiquement les tuiles nécessaires depuis les serveurs
d'OpenStreetMap et les assemble pour former la carte visible à l'écran.

### L'identification obligatoire

OpenStreetMap exige que chaque application s'identifie par un User-Agent dans ses requêtes
HTTP. Sans cette identification, le serveur refuse les connexions. Cette configuration doit
être faite avant tout affichage de la carte.

### Le marqueur de position

Un marqueur est un élément visuel placé sur la carte à des coordonnées précises. Quand le
GPS fournit une nouvelle position, le marqueur est déplacé vers ces nouvelles coordonnées
et la carte est recentrée sur ce point. La carte doit ensuite être forcée à se redessiner
pour que le changement soit visible à l'écran.

### Le cycle de vie de la carte

osmdroid doit être informé des changements de cycle de vie du Fragment pour gérer
correctement son cache de tuiles. Quand le Fragment reprend, osmdroid reprend le
téléchargement des tuiles. Quand le Fragment est mis en pause, osmdroid sauvegarde son
cache. Quand le Fragment est détruit, osmdroid libère toutes ses ressources mémoire.

---

## 4. Géocodage inverse avec Nominatim

### Le problème à résoudre

Le GPS fournit des coordonnées brutes sous forme de latitude et longitude. Ces valeurs
numériques ne sont pas lisibles pour un utilisateur. Le géocodage inverse est l'opération
qui transforme ces coordonnées en adresse compréhensible comme un nom de rue, une ville
et un pays.

### Ce qu'est Nominatim

Nominatim est un service web gratuit maintenu par la communauté OpenStreetMap. L'application
lui envoie une requête HTTP avec les coordonnées GPS, et il répond avec un fichier JSON
contenant l'adresse complète correspondant à ces coordonnées.

### Pourquoi exécuter cela en arrière-plan

Android interdit strictement les appels réseau sur le thread principal, appelé thread UI.
La raison est simple : un appel réseau peut prendre plusieurs secondes selon la qualité
de la connexion. Si le thread UI était bloqué pendant ce temps, l'application serait
complètement figée et l'utilisateur ne pourrait plus interagir avec elle.

### Thread et Handler

Pour résoudre ce problème, on utilise un Thread combiné à un Handler. Le Thread crée un
fil d'exécution parallèle au thread UI dans lequel l'appel HTTP à Nominatim s'exécute
sans bloquer l'interface. Le Handler est un objet attaché au thread UI via son Looper.
Une fois l'appel réseau terminé, le Handler est utilisé pour repasser sur le thread UI
et mettre à jour le TextView affichant l'adresse. C'est le seul thread autorisé à
modifier les Views.

Cette approche est préférée à AsyncTask, qui est officiellement dépréciée depuis
Android 11, et correspond mieux au critère 0 car elle utilise l'API Android pure sans
abstraction cachée.

### L'appel HTTP

L'appel réseau est réalisé avec HttpURLConnection, l'API Android native pour les requêtes
HTTP. Aucune librairie externe n'est utilisée. La réponse est lue ligne par ligne et
assemblée en une chaîne de caractères, puis le JSON est parsé avec JSONObject pour en
extraire uniquement le champ contenant l'adresse complète.

---

## 5. Flux complet d'une mise à jour de position

Quand l'utilisateur ouvre le MapFragment, l'application vérifie d'abord si la permission
GPS a déjà été accordée. Si ce n'est pas le cas, elle affiche la boîte de dialogue système.
Une fois la permission obtenue, elle s'abonne aux mises à jour du LocationManager.

Quand le GPS détecte un déplacement suffisant ou qu'assez de temps s'est écoulé, Android
appelle automatiquement onLocationChanged avec les nouvelles coordonnées. L'application
met alors à jour les TextViews affichant la latitude et la longitude, déplace le marqueur
sur la carte osmdroid et force le redessin de la carte. Simultanément, un nouveau Thread
est lancé pour appeler Nominatim en arrière-plan. Quand Nominatim répond, le Handler
repassse sur le thread UI pour afficher l'adresse dans le TextView prévu à cet effet.

---

## 6. Responsabilités de chaque composant

| Composant | Rôle |
|---|---|
| Manifest | Déclare les permissions nécessaires à l'installation |
| checkSelfPermission | Vérifie si la permission est déjà accordée à l'exécution |
| LocationManager | Service système qui gère le matériel GPS |
| LocationListener | Interface qui reçoit automatiquement les nouvelles positions |
| osmdroid | Télécharge et affiche les tuiles de la carte OpenStreetMap |
| Marker | Élément visuel positionné sur la carte à la position GPS |
| Nominatim | Service web qui convertit des coordonnées en adresse lisible |
| HttpURLConnection | API Android native pour effectuer les requêtes HTTP |
| JSONObject | Parse la réponse JSON retournée par Nominatim |
| Thread | Exécute l'appel réseau sur un fil d'exécution séparé du thread UI |
| Handler | Permet de repasser sur le thread UI pour mettre à jour les Views |