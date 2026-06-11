# Vélib Métropole 🚲

Projet Android / Kotlin — TP EPF (rendu juin 2026).

Application de consultation en temps réel des stations Vélib de la métropole parisienne, avec carte interactive, favoris hors connexion et recherche de stations à proximité.

📖 **[Documentation utilisateur](DOCUMENTATION_UTILISATEUR.md)**

## Fonctionnalités

### Cahier des charges
- 🗺 **Carte des stations** (OpenStreetMap via osmdroid, style sombre) avec pastilles colorées selon la disponibilité
- 📋 **Détail d'une station** : vélos mécaniques / électriques, places libres, capacité, état, jauge de remplissage, dernière mise à jour
- ❤️ **Favoris hors connexion** : la liste et le détail des stations favorites restent accessibles sans réseau (instantané local des dernières données)
- 📍 **Stations à proximité** dans un périmètre réglable (100 m → 3 km) dans les paramètres

### Fonctionnalités supplémentaires
- 🔍 Recherche de station par nom avec résultats instantanés
- ⚡ Distinction vélos mécaniques / électriques (flux GBFS)
- 🧭 Bouton « Y aller » (itinéraire) et partage d'une station
- 📊 Statistiques globales (stations / vélos disponibles) sur la carte
- 🔄 Actualisation automatique des disponibilités (60 s, désactivable)
- 🌙 Interface sombre « glassmorphisme », carte assombrie, animations fluides
- 🚀 Affichage des markers limité à la zone visible → carte fluide malgré ~1 500 stations

## Architecture

Projet « cours » : **Activities + layouts XML + ViewBinding** (pas de Compose).

```
fr.epf.sin2.velib_metropol
├── MainActivity.kt            # Carte osmdroid, recherche, navigation
├── StationDetailActivity.kt   # Détail + favori + mode hors ligne
├── FavoritesActivity.kt       # Liste des favoris (offline)
├── NearbyActivity.kt          # Stations dans le périmètre
├── SettingsActivity.kt        # Périmètre + auto-refresh
├── StationAdapter.kt          # Adapter RecyclerView commun
├── SearchResultAdapter.kt     # Résultats de recherche
├── api/VelibApiService.kt     # Retrofit (flux GBFS)
├── data/StationRepository.kt  # Fusion info + status, cache mémoire
├── data/FavoritesStore.kt     # Favoris JSON (SharedPreferences)
├── data/SettingsStore.kt      # Préférences
├── model/                     # Station, modèles GBFS
└── util/                      # Haversine, fabrique d'icônes de markers
```

## Bibliothèques

| Lib | Usage |
|---|---|
| Retrofit 2 + Gson | API GBFS Vélib |
| osmdroid | Carte OpenStreetMap (aucune clé API requise) |
| Play Services Location | Géolocalisation |
| Material Components | UI (sliders, switches, jauges) |
| Coroutines | Appels réseau asynchrones |

## Lancer le projet

1. Ouvrir le dossier dans Android Studio
2. Synchroniser Gradle
3. Lancer sur un appareil / émulateur (API 36+)

Données : [open-data GBFS Vélib Métropole](https://www.velib-metropole.fr/donnees-open-data-gbfs-du-service-velib-metropole)
