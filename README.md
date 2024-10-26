# Analyse de Couplage et Identification de Modules

Ce projet est un outil d'analyse de couplage entre classes Java et d'identification de modules basée sur ce couplage. Il utilise Spoon pour l'analyse statique du code et offre deux interfaces graphiques pour visualiser les résultats.

## Fonctionnalités

- Analyse du couplage entre les classes d'un projet Java
- Génération d'un graphe de couplage pondéré
- Clustering hiérarchique des classes
- Identification de modules basée sur le couplage
- Interfaces graphiques pour une utilisation facile

## Composants Principaux

### SpoonGUI

`SpoonGUI` est l'interface graphique principale utilisant Spoon pour l'analyse. Elle offre les fonctionnalités suivantes :

- Sélection d'un projet Java à analyser
- Analyse du couplage entre les classes
- Génération et visualisation du graphe de couplage
- Exécution du clustering hiérarchique
- Identification des modules basés sur le couplage

### CouplingAnalyzerGUI

`CouplingAnalyzerGUI` est une interface graphique alternative qui utilise une implémentation différente pour l'analyse de couplage. Ses fonctionnalités incluent :

- Analyse du couplage des classes d'un projet Java
- Génération d'un graphe de couplage pondéré
- Visualisation interactive du graphe avec options de zoom

## Structure du Projet

- `src/main/java/spoon/` : Classes liées à l'analyse avec Spoon
- `src/main/java/com/example/` : Classes d'analyse de couplage alternatives
- `SpoonGUI.java` : Interface graphique principale utilisant Spoon
- `CouplingAnalyzerGUI.java` : Interface graphique alternative

## Prérequis

- Java JDK 11 ou supérieur
- Maven

## Auteur

Ce projet a été réalisé par :

- DAIA Adam
- DAFAOUI Mohamed

Étudiants en M2 Génie Logiciel à l'Université de Montpellier
