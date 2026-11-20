# Fanaoka — état du développement

## Version de travail
Alpha 0.1.1 — prototype Android initial.

## Inclus dans cette branche
- Projet Android Kotlin avec interface Jetpack Compose.
- Autorisation VPN Android demandée à l'utilisateur.
- Service VPN local et notification persistante.
- Routage DNS IPv4 vers un moteur DNS local.
- Première liste de domaines publicitaires de démonstration.
- Tentative de résolution DNS via 1.1.1.1.
- Réponse NXDOMAIN pour les domaines de la liste.
- Workflow GitHub Actions pour compiler un APK debug.

## Limites connues — ne pas considérer comme une protection complète
- Le moteur actuel traite uniquement les paquets DNS IPv4/UDP.
- Le DNS chiffré privé (DoH/DoT), les DNS intégrés aux applications et certaines applications peuvent contourner ce filtrage.
- La liste de blocage est provisoire et codée dans le moteur.
- Les compteurs d'économie de données ne sont pas encore implémentés.
- Les mises à jour de listes Wi-Fi/données mobiles et les exceptions par application ne sont pas encore implémentées.
- Le trafic non DNS continue hors du tunnel : Fanaoka ne fait pas de VPN général et ne relaie pas tout le trafic.
- Aucun gain de données ni blocage global n'est garanti avant les essais réels.

## Prochaine séquence
1. Compiler avec GitHub Actions et corriger les erreurs.
2. Installer l'APK debug sur Itel A665L / Android 13.
3. Tester activation, désactivation, navigation, applications et stabilité Telma.
4. Ajouter IPv6, gestion de listes actualisables, exceptions et statistiques après validation du socle.

## Confidentialité
Le prototype ne collecte pas d'historique de navigation et n'intercepte pas le HTTPS. Les requêtes DNS non bloquées sont transmises au résolveur public 1.1.1.1. Cette destination devra être expliquée à l'utilisateur et rendue configurable avant une diffusion publique.
