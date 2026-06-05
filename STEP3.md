# Critère 3 — Prise de photo et stockage local

## Vue d'ensemble

Le critère 3 repose sur trois mécanismes distincts qui travaillent ensemble :
- **Intent implicite** : délègue la prise de photo à l'app caméra native du téléphone
- **FileProvider** : partage sécurisé du fichier photo entre l'app et l'app caméra
- **Système de fichiers** : stockage local de la photo dans le dossier privé de l'app

---

## 1. Les permissions

Comme pour le GPS, Android applique un système de sécurité en deux niveaux pour accéder
à la caméra.

Le premier niveau est la déclaration dans le Manifest. Deux balises sont nécessaires :
uses-permission déclare que l'app a besoin d'accéder au matériel caméra, et uses-feature
avec required="false" informe le Play Store que l'app utilise la caméra sans l'exiger
obligatoirement. Sans required="false", le Play Store bloquerait l'installation sur tous
les appareils sans caméra comme certaines tablettes.

Le second niveau est la demande à l'exécution. Au moment où l'utilisateur appuie sur le
bouton photo, l'app vérifie si la permission est déjà accordée. Si ce n'est pas le cas,
elle affiche la boîte de dialogue système. Si l'utilisateur refuse, la caméra reste
inaccessible et un message d'erreur est affiché. Ce mécanisme est identique à celui
utilisé pour la permission GPS.

---

## 2. L'Intent implicite

### Ce que c'est

Pour prendre une photo, l'app utilise un Intent implicite avec l'action
ACTION_IMAGE_CAPTURE. On ne nomme pas directement l'app caméra à ouvrir — on décrit
uniquement l'action souhaitée et Android se charge de trouver quelle application installée
sur le téléphone peut y répondre. Si plusieurs apps caméra sont installées, Android
affiche un sélecteur pour que l'utilisateur choisisse.

### Pourquoi ce choix

Cette approche respecte le principe de séparation des responsabilités. L'app GeoMemory
ne gère pas elle-même le matériel caméra — c'est complexe et chaque constructeur
(Samsung, Google, Xiaomi) optimise sa propre app caméra pour son matériel. En déléguant
via un Intent implicite, on obtient automatiquement la meilleure qualité photo possible
sur chaque téléphone, sans aucun code supplémentaire.

### ActivityResultLauncher

Pour récupérer le résultat de l'app caméra quand l'utilisateur a terminé la prise de
vue, on utilise ActivityResultLauncher. C'est le remplacement moderne de onActivityResult
qui est officiellement déprécié. Le launcher est déclaré à l'initialisation du Fragment
et reçoit un callback automatiquement quand l'app caméra se ferme, avec un code indiquant
si la photo a bien été prise ou si l'utilisateur a annulé.

---

## 3. Le FileProvider

### Le problème à résoudre

Depuis Android 7.0, une application ne peut pas partager directement un chemin de fichier
avec une autre application pour des raisons de sécurité. Si on passait le chemin brut du
fichier à l'app caméra, Android lèverait une exception de sécurité et l'app planterait.

### Ce qu'est le FileProvider

Le FileProvider est un composant Android qui agit comme un intermédiaire sécurisé. Au
lieu de partager un chemin de fichier brut, il génère une URI sécurisée et temporaire
que l'app caméra est autorisée à utiliser. Cette URI n'est valable que le temps de
l'opération et ne donne accès qu'au fichier spécifique, pas à tout le stockage.

### La configuration

Le FileProvider doit être déclaré dans le Manifest à l'intérieur de la balise application.
Il référence un fichier file_paths.xml placé dans res/xml qui déclare les dossiers que
le FileProvider est autorisé à partager. Dans GeoMemory, seul le dossier Pictures du
stockage privé de l'app est déclaré.

---

## 4. Le stockage local des photos

### Où sont stockées les photos

Les photos sont sauvegardées dans le dossier privé de l'application sur le stockage
externe du téléphone, sous le chemin Pictures. Ce dossier est privé — les autres
applications ne peuvent pas y accéder, et il est automatiquement supprimé si
l'application est désinstallée.

### Le nom de fichier unique

Chaque photo reçoit un nom unique basé sur la date et l'heure exacte de la prise de vue,
au format PHOTO_yyyyMMdd_HHmmss.jpg. Cela garantit qu'aucun fichier n'écrase un autre,
même si deux photos sont prises rapidement l'une après l'autre.

### Le fichier vide préalable

Avant de lancer l'app caméra, l'app crée d'abord un fichier vide à l'emplacement prévu.
Ce fichier vide est nécessaire car on indique à l'app caméra via EXTRA_OUTPUT où elle
doit écrire la photo pleine résolution. Sans ce fichier préalable, l'app caméra ne sait
pas où sauvegarder l'image.

### La miniature

Une fois la photo prise, l'app lit le fichier jpg depuis le système de fichiers, le
décode en Bitmap et l'affiche dans l'ImageView du formulaire. Seul le chemin absolu de
la photo est conservé en mémoire — ce chemin sera sauvegardé dans SQLite lors de
l'implémentation du critère 5.

---

## 5. Flux complet d'une prise de photo

L'utilisateur appuie sur le bouton photo. L'app vérifie si la permission caméra est
accordée. Si ce n'est pas le cas, elle affiche la boîte de dialogue système. Une fois
la permission obtenue, l'app crée un fichier vide avec un nom unique basé sur la date,
génère une URI sécurisée via le FileProvider, puis lance un Intent implicite
ACTION_IMAGE_CAPTURE en lui passant cette URI.

L'app caméra native s'ouvre. L'utilisateur prend sa photo. L'app caméra sauvegarde
l'image dans le fichier préparé par GeoMemory et se ferme. Le ActivityResultLauncher
reçoit le callback avec le code RESULT_OK. L'app lit alors le fichier photo, le décode
en Bitmap et l'affiche en miniature dans l'ImageView. Le chemin absolu du fichier est
conservé pour être sauvegardé dans SQLite lors du critère 5.

---

## 6. Responsabilités de chaque composant

| Composant | Rôle |
|---|---|
| uses-permission CAMERA | Déclare le besoin d'accès à la caméra dans le Manifest |
| uses-feature required=false | Permet l'installation sur les appareils sans caméra |
| checkSelfPermission | Vérifie si la permission caméra est accordée à l'exécution |
| Intent ACTION_IMAGE_CAPTURE | Intent implicite qui délègue la prise de photo à l'app caméra native |
| FileProvider | Génère une URI sécurisée pour partager le fichier entre les apps |
| file_paths.xml | Déclare les dossiers que le FileProvider est autorisé à partager |
| EXTRA_OUTPUT | Indique à l'app caméra où sauvegarder la photo pleine résolution |
| ActivityResultLauncher | Reçoit le résultat de l'app caméra quand la prise de vue est terminée |
| creerFichierPhoto() | Crée un fichier vide avec un nom unique basé sur la date |
| afficherMiniature() | Décode le fichier jpg en Bitmap et l'affiche dans l'ImageView |