# Projet de Minage Distribué

Ce projet est réalisé dans le cadre du cours Miage M1 - Réseaux. Il consiste à implémenter un système de minage distribué permettant de déléguer des tâches à plusieurs workers et de consolider les résultats.

## Description du Projet

L'objectif du projet est de développer une application composée d'un serveur centralisé et de plusieurs workers. Le serveur distribue des tâches de recherche de hash aux workers, lesquels tentent de trouver un nonce permettant de résoudre la tâche. Le serveur valide ensuite les résultats et informe les autres workers lorsque la solution est trouvée.

## Architecture

Le projet est composé des éléments suivants :

- **Serveur** : Récupère les tâches auprès d'une application web, les distribue aux workers, et gère la communication avec eux.
- **Workers** : Se connectent au serveur, reçoivent les tâches et effectuent le calcul pour trouver le nonce correct.

## Configuration et Exécution

### Prérequis

- Java 11 ou supérieur

### Instructions

1. **Cloner le repository** :
    ```bash
    git clone https://github.com/[votre_nom_utilisateur]/projet-distributed-mining.git
    cd projet-distributed-mining
    ```

2. **Compiler le projet** :
    ```bash
    javac -d out fr/idmc/raizo/*.java
    ```

3. **Exécuter le serveur** :
    ```bash
    java -cp out fr.idmc.raizo.ServerLauncher
    ```

4. **Exécuter un worker** :
    ```bash
    telnet localhost 1337
    ```

## Utilisation

### Commandes du Serveur

- `status` : Afficher l'état des workers.
- `solve <d>` : Démarrer une nouvelle tâche de minage avec une difficulté spécifiée.
- `progress` : Voir ce que chaque worker teste ou ne teste pas.
- `cancel` : Annuler la tâche en cours.
- `help` : Afficher les commandes disponibles.
- `quit` : Terminer les travaux en cours et quitter.

### Commandes des Workers

Les workers se connectent automatiquement au serveur et attendent des tâches.

## Auteurs

- **FILALI Hatim**
- **EDEKAKI Adam**
- **KABBAJ Jad**
- **MAGHAFRI Acharf**
