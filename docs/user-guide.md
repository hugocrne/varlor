# Guide Utilisateur Varlor MVP

*Bienvenue dans Varlor, votre plateforme de Data Intelligence universelle et souveraine*

## Table des Matières

1. [Premiers Pas](#premiers-pas)
2. [Interface Utilisateur](#interface-utilisateur)
3. [Importer vos Données](#importer-vos-données)
4. [Nettoyage Automatique](#nettoyage-automatique)
5. [Analyse et Visualisations](#analyse-et-visualisations)
6. [Rapports et Export](#rapports-et-export)
7. [Comprendre les Insights IA](#comprendre-les-insights-ia)
8. [FAQ](#faq)
9. [Support](#support)

---

## Premiers Pas

### Connexion

1. Ouvrez votre navigateur et accédez à : `http://localhost:3000`
2. Utilisez les identifiants fournis par votre administrateur :
   - Email : `admin@varlor.com` (ou vos identifiants personnalisés)
   - Mot de passe : Celui configuré lors de l'installation

### Première Connexion

Après vous connecter, vous arriverez sur le tableau de bord. La navigation principale se trouve dans la barre latérale gauche :
- **Datasets** : Gestion de vos fichiers de données
- **Analyses** : Visualisation des analyses en cours
- **Rapports** : Accès aux rapports générés

---

## Interface Utilisateur

### Tableau de Bord

Le tableau de bord vous donne une vue d'ensemble de :
- Vos datasets récents
- Les analyses en cours
- Les rapports disponibles
- L'espace de stockage utilisé

### Barre de Navigation

- **Datasets** : Liste de tous vos datasets importés
- **Nouveau Dataset** : Importer un nouveau fichier
- **Paramètres** : Configuration du compte

---

## Importer vos Données

### Formats Supportés

Varlor accepte les formats suivants :
- **CSV** (Comma-Separated Values)
- **Excel** (.xlsx, .xls)

### Étape 1 : Préparer votre Fichier

Avant d'importer, assurez-vous que :
- Votre fichier a une ligne d'en-tête avec les noms de colonnes
- Les données sont organisées de manière cohérente
- Le fichier ne dépasse pas 100 MB (limite MVP)

### Étape 2 : Importer

1. Cliquez sur **"Nouveau Dataset"** dans la barre latérale
2. Glissez-déposez votre fichier ou cliquez pour parcourir
3. Donnez un nom à votre dataset
4. Cliquez sur **"Importer"**

### Étape 3 : Preview

L'aperçu s'affiche avec :
- Les 10 premières lignes de données
- Les types de colonnes détectés automatiquement
- Le nombre total de lignes et colonnes

### Étape 4 : Confirmation

Vérifiez que :
- Les colonnes sont correctement identifiées
- Les types de données sont appropriés
- Aucune erreur majeure n'est détectée

Cliquez sur **"Confirmer l'import"** pour continuer.

---

## Nettoyage Automatique

Varlor nettoie automatiquement vos données après l'import. Ce processus peut prendre quelques minutes selon la taille du fichier.

### Ce qui est nettoyé

1. **Valeurs Manquantes**
   - Détection des cellules vides
   - Remplacement par des valeurs par défaut quand approprié

2. **Formatage**
   - Standardisation des dates
   - Uniformisation des nombres (décimales, séparateurs)
   - Normalisation du texte (majuscules/minuscules)

3. **Doublons**
   - Détection des lignes identiques
   - Gestion automatique ou signalement

4. **Valeurs Aberrantes**
   - Identification des valeurs extrêmes
   - Validation de la cohérence des données

### Suivre le Nettoyage

1. Allez dans l'onglet **"Cleaning"** de votre dataset
2. Une barre de progression montre l'état d'avancement
3. Les corrections sont listées avec leur impact

### Valider les Corrections

Après le nettoyage :
- Revoyez les corrections proposées
- Acceptez ou refusez chaque correction
- Cliquez sur **"Appliquer les corrections"**

---

## Analyse et Visualisations

Une fois le nettoyage terminé, Varlor lance automatiquement l'analyse.

### Types d'Analyse

#### Statistiques Descriptives
- Moyenne, médiane, écart-type
- Quartiles et percentiles
- Distribution des valeurs

#### Visualisations Automatiques
- **Histogrammes** : Pour les données numériques continues
- **Diagrammes en barres** : Pour les données catégorielles
- **Graphiques temporels** : Pour les données avec dates
- **Matrice de corrélation** : Relations entre variables

### Accéder aux Analyses

1. Sélectionnez votre dataset dans la liste
2. Cliquez sur l'onglet **"Analyse"**
3. Les visualisations apparaissent automatiquement

### Interagir avec les Visualisations

- **Zoom** : Cliquez et glissez sur une zone
- **Filtres** : Utilisez les contrôles pour filtrer les données
- **Export** : Téléchargez les graphiques en PNG

### Détails des Colonnes

Chaque colonne a sa propre fiche d'analyse :
- Type de données détecté
- Statistiques détaillées
- Qualité des données
- Insights spécifiques

---

## Comprendre les Insights IA

Varlor génère des intelligibles à partir de vos données.

### Types d'Insights

1. **Patterns Détectés**
   - "Les ventes augmentent de 20% le week-end"
   - "Le produit X représente 35% du chiffre d'affaires"

2. **Anomalies**
   - "Pic anormal le 15 mars (3x la moyenne)"
   - "Valeur manquante dans 45% des enregistrements"

3. **Recommandations**
   - "Considérez supprimer la colonne 'Ancienne_ID'"
   - "Le format de date nécessite une standardisation"

4. **Corrélations**
   - "Forte corrélation entre température et ventes (r=0.87)"
   - "Les clients de la région A achètent 30% plus"

### Interpréter les Scores

- **Score de Qualité** : 0-100%
  - 90-100% : Excellent
  - 70-89% : Bon
  - 50-69% : Moyen
  - <50% : Nécessite des corrections

- **Score de Complétude** : Pourcentage de données valides
- **Score de Cohérence** : Uniformité des formats

---

## Rapports et Export

### Génération de Rapport

1. Après analyse, cliquez sur l'onglet **"Rapport"**
2. Le rapport est généré automatiquement avec :
   - Synthèse générale
   - Évaluation de la qualité
   - Visualisations sélectionnées
   - Insights IA

### Contenu du Rapport PDF

Le rapport inclut 4 sections :

1. **Informations Générales**
   - Nom du dataset
   - Date d'import
   - Nombre de lignes/colonnes
   - Taille du fichier

2. **Synthèse Qualité**
   - Score global de qualité
   - Corrections appliquées
   - Problèmes restants

3. **Visualisations**
   - Jusqu'à 6 graphiques pertinents
   - Légendes et interprétations
   - Statistiques clés

4. **Analyse IA**
   - Insights générés
   - Anomalies détectées
   - Recommandations

### Télécharger le Rapport

1. Dans l'onglet **"Rapport"**
2. Cliquez sur **"Générer le PDF"**
3. Attendez la génération (quelques secondes)
4. Cliquez sur **"Télécharger"**

Le PDF inclut :
- Entête Varlor avec date
- Table des matières
- Mise en page professionnelle
- Graphiques haute résolution

---

## FAQ

### Q : Mon fichier est trop volumineux (>100MB)
**R :** La limite MVP est de 100MB. Pour les fichiers plus volumineux :
- Divisez votre fichier en plusieurs parties
- Contactez votre administrateur pour une augmentation de limite

### Q : Certaines colonnes ne sont pas correctement identifiées
**R :** Varlor détecte automatiquement les types mais vous pouvez :
- Vérifier l'aperçu avant import
- Utiliser des formats standards (UTF-8, dates ISO)
- Nettoyer les en-têtes de colonnes

### Q : L'analyse prend beaucoup de temps
**R :** Le temps dépend de :
- Taille du fichier (plus de lignes = plus de temps)
- Complexité des données
- Charge du serveur

### Q : Je ne vois pas l'onglet "Rapport"
**R :** L'onglet apparaît uniquement après :
- Import réussi du fichier
- Nettoyage terminé
- Analyse complétée

### Q : Comment supprimer un dataset ?
**R :** Actuellement dans la MVP :
- Contactez votre administrateur
- Les futures versions permettront la suppression directe

---

## Support

### Obtenir de l'Aide

- **Documentation technique** : Voir `/docs/index.md`
- **Email support** : `team@varlor.com`
- **Issues** : Via votre système de suivi interne

### Signaler un Problème

Pour signaler un bug ou problème :
1. Notez le message d'erreur exact
2. Capturez d'écran si possible
3. Décrivez les étapes pour reproduire
4. Contactez votre administrateur

### Astuces

- **Sauvegardez** vos fichiers originaux avant import
- **Utilisez des noms de colonnes clairs** (pas d'espaces, caractères spéciaux)
- **Vérifiez les formats de dates** avant import
- **Commencez avec de petits fichiers** pour tester

---

## Limitations de la MVP

La version MVP actuelle a certaines limites :

- **Taille maximale** : 100MB par fichier
- **Formats** : CSV et Excel uniquement
- **Utilisateurs** : Un seul utilisateur par instance
- **Export** : PDF uniquement
- **Stockage** : Local uniquement (pas S3)

Ces limitations seront levées dans les futures versions.

---

*Ce guide couvre la version MVP de Varlor. Pour les mises à jour et nouvelles fonctionnalités, consultez la roadmap du projet.*