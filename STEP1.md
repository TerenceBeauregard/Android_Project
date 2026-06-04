# Critère 1 — Interface ergonomique, orientation et internationalisation

## Vue d'ensemble

Le critère 1 repose sur quatre mécanismes distincts qui travaillent ensemble :
- **Toolbar** : barre d'actions en haut de l'écran remplaçant l'ActionBar par défaut
- **Navigation Drawer** : menu latéral coulissant pour naviguer entre les sections
- **Layouts alternatifs** : adaptation automatique de l'interface selon l'orientation
- **Fichiers strings** : adaptation automatique de la langue selon les paramètres du téléphone

---

## 1. Le thème et la Toolbar

### Pourquoi remplacer l'ActionBar par défaut

Android fournit une ActionBar par défaut dans chaque application. Elle est simple à
utiliser mais très limitée en termes de personnalisation. Il est impossible de changer
facilement sa couleur selon l'écran, d'y intégrer des éléments complexes ou de l'animer.

La Toolbar est une View comme les autres, déclarée directement dans le fichier XML de
layout. Cela signifie qu'on peut la positionner n'importe où, changer son apparence
fragment par fragment, et y placer n'importe quel autre élément visuel. En appelant
setSupportActionBar dans le code Java, on indique à Android de traiter cette Toolbar
comme s'il s'agissait de l'ActionBar officielle, récupérant ainsi tout son comportement
habituel tout en gardant le contrôle total sur l'apparence.

### Le thème NoActionBar

Pour éviter d'avoir deux barres superposées en haut de l'écran, le thème de l'application
doit hériter de NoActionBar. Cela désactive l'ActionBar automatique d'Android et laisse
la place à la Toolbar déclarée manuellement dans le layout.

### Le menu des trois points

La Toolbar peut afficher un menu d'options accessible via trois points verticaux. Ce menu
est défini dans un fichier XML dédié dans le dossier res/menu. Android l'affiche
automatiquement dans la Toolbar quand la méthode onCreateOptionsMenu est définie dans
l'Activity. Les clics sur les items sont gérés dans onOptionsItemSelected.

---

## 2. Le Navigation Drawer

### Ce que c'est

Le Navigation Drawer est un panneau latéral qui coulisse depuis le bord gauche de l'écran.
Il est déclenché soit par un glissement du doigt depuis le bord gauche, soit par un appui
sur l'icône hamburger dans la Toolbar. C'est le composant de navigation recommandé par
Google pour les applications avec plusieurs sections principales.

### La structure du layout

Le DrawerLayout est le conteneur racine du layout de l'Activity. Il contient obligatoirement
deux enfants : le contenu principal de l'application d'un côté, et le NavigationView de
l'autre. Le NavigationView doit toujours être déclaré en dernier dans le DrawerLayout pour
s'afficher par-dessus le contenu quand il est ouvert.

### Le bouton hamburger

L'ActionBarDrawerToggle est l'objet qui fait le lien entre la Toolbar et le DrawerLayout.
Il crée automatiquement l'icône hamburger dans la Toolbar, gère son animation en flèche
quand le Drawer s'ouvre, et synchronise l'état visuel du bouton avec l'état réel du Drawer.

### Le menu du Drawer

Les items affichés dans le Drawer sont définis dans un fichier XML dans le dossier res/menu.
Chaque item possède un identifiant, une icône et un titre. L'Activity implémente l'interface
OnNavigationItemSelectedListener pour recevoir les clics sur ces items et charger le Fragment
correspondant dans le conteneur principal.

### L'en-tête du Drawer

Le haut du Navigation Drawer peut afficher un en-tête personnalisé défini dans un fichier
de layout séparé. Il contient généralement le nom de l'application, un sous-titre et
éventuellement une icône ou photo de profil.

---

## 3. Le Single Activity Pattern

### Le principe

L'application ne possède qu'une seule Activity, la MainActivity, qui joue le rôle de
coquille permanente. Elle contient la Toolbar, le Navigation Drawer et un conteneur de
Fragment. Ce conteneur est une zone vide de l'écran dans laquelle des Fragments sont
chargés et remplacés selon la navigation de l'utilisateur.

### Pourquoi ce choix

Avec plusieurs Activities, chaque transition recrée entièrement la Toolbar et le Drawer,
ce qui est coûteux en termes de performances et entraîne de la duplication de code. Avec
un seul Fragment swappé dans un conteneur, Android ne redessine que la zone qui change
réellement. La navigation est plus fluide, le code est centralisé et plus facile à
maintenir.

### Le FragmentManager

Le FragmentManager est le composant Android qui gère les transactions de Fragments. Une
transaction consiste à remplacer le Fragment actuellement affiché dans le conteneur par
un nouveau Fragment. Cette opération est déclenchée depuis MainActivity à chaque sélection
dans le Navigation Drawer. Les Fragments n'ont pas besoin d'être déclarés dans le Manifest,
contrairement aux Activities, car ils ne sont pas des points d'entrée de l'application.

---

## 4. L'adaptation portrait et paysage

### Le mécanisme Android

Android sélectionne automatiquement le fichier de layout à charger selon l'orientation
actuelle du téléphone. Les layouts pour le mode portrait sont placés dans le dossier
res/layout. Les layouts pour le mode paysage sont placés dans le dossier res/layout-land.
Les deux fichiers portent exactement le même nom. Aucun code Java n'est nécessaire pour
gérer cette sélection, Android s'en charge entièrement.

### Ce qui change entre les deux orientations

En mode portrait, l'écran est étroit et un seul Fragment est affiché à la fois. La
navigation entre les sections se fait via le Navigation Drawer. En mode paysage, l'écran
est suffisamment large pour afficher deux panneaux côte à côte. Le panneau gauche affiche
la carte et le panneau droit affiche la liste des souvenirs, offrant une meilleure
utilisation de l'espace disponible.

### La détection de l'orientation en Java

Les deux layouts ne contiennent pas les mêmes identifiants. Le layout paysage possède un
second conteneur de Fragment absent du layout portrait. En cherchant cet identifiant avec
findViewById, si le résultat est non nul, l'application sait qu'elle s'exécute en mode
paysage et charge deux Fragments simultanément au lieu d'un seul.

### Les contraintes sur les identifiants

Les identifiants communs aux deux layouts, comme le DrawerLayout, la Toolbar et le
NavigationView, doivent porter exactement le même nom dans les deux fichiers. Le code Java
de MainActivity utilise ces identifiants pour brancher la Toolbar et le Drawer. Si un
identifiant diffère entre les deux fichiers, le code plante car findViewById ne trouve
pas ce qu'il cherche.

---

## 5. L'internationalisation français et anglais

### Le mécanisme Android

Android sélectionne automatiquement le fichier de chaînes de caractères à utiliser selon
la langue configurée sur le téléphone. Les chaînes en français, langue par défaut, sont
placées dans res/values/strings.xml. Les chaînes en anglais sont placées dans
res/values-en/strings.xml. Aucun code Java n'est nécessaire, Android gère entièrement
cette sélection au démarrage de l'application.

### La règle du texte en dur

Aucun texte visible par l'utilisateur ne doit être écrit directement dans le code Java
ou dans les fichiers XML de layout. Chaque texte doit être déclaré dans strings.xml avec
un identifiant, puis référencé par cet identifiant. Cette règle est indispensable pour
que la traduction fonctionne correctement.

### La langue par défaut

Le fichier res/values/strings.xml est la langue de secours utilisée quand la langue du
téléphone ne correspond à aucun fichier de traduction disponible. Si l'utilisateur configure
son téléphone en espagnol et qu'il n'existe pas de fichier res/values-es/strings.xml,
Android utilisera automatiquement le fichier par défaut.

---

## 6. Responsabilités de chaque composant

| Composant | Rôle |
|---|---|
| Thème NoActionBar | Désactive l'ActionBar automatique pour laisser place à la Toolbar |
| Toolbar | Barre d'actions personnalisable remplaçant l'ActionBar |
| DrawerLayout | Conteneur racine gérant l'ouverture et fermeture du panneau latéral |
| NavigationView | Panneau latéral contenant le menu de navigation |
| ActionBarDrawerToggle | Synchronise le bouton hamburger avec l'état du Drawer |
| FragmentManager | Gère le chargement et le remplacement des Fragments dans le conteneur |
| res/layout | Contient le layout utilisé en mode portrait |
| res/layout-land | Contient le layout utilisé en mode paysage |
| res/values/strings.xml | Contient les chaînes en français, langue par défaut |
| res/values-en/strings.xml | Contient les chaînes en anglais |