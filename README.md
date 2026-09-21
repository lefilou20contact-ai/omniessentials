# MonServeur — plugin tout-en-un pour Paper

Grades, tags, tab/nametag, économie, boutique, menus GUI, niveaux, quêtes
quotidiennes, homes, spawn, mode lobby, **liens/annonces personnalisables**,
**PNJ** et **générateur de spawn**. Réglages dans `config.yml`, `links.yml` et `spawn-build.yml`.

## 1. Compiler le plugin (il faut obtenir le fichier .jar)

Avant de compiler, ouvre `pom.xml` et mets dans `<paper.version>` la version de
Paper de ton serveur (ex : `1.21.1-R0.1-SNAPSHOT`, `1.21.4-R0.1-SNAPSHOT`).
Le plugin doit être compilé pour la version que tu utilises sur Minestrator.

**Option A — sans rien installer (GitHub)**
1. Crée un dépôt GitHub (privé si tu veux) et envoie-y tout ce dossier.
2. Onglet **Actions** > dernier "Build" > en bas **Artifacts** > télécharge `MonServeur.jar`.

**Option B — sur ton PC**
1. Installe JDK 21 (https://adoptium.net) et Maven (https://maven.apache.org).
2. Dans ce dossier : `mvn clean package`
3. Le fichier est dans `target/MonServeur-1.0.0.jar`.

**Option C — IntelliJ IDEA** : ouvre le dossier, il détecte Maven, puis Maven > Lifecycle > package.

## 2. Installer sur Minestrator
1. Panel Minestrator : serveur en **Paper** (même version que dans le pom).
2. Gestionnaire de fichiers / FTP : dépose le `.jar` dans le dossier `plugins/`.
3. Redémarre le serveur. `config.yml` est créé dans `plugins/MonServeur/`.
4. Donne-toi le grade admin : depuis la console du panel, `op TonPseudo`
   puis en jeu `/rank set TonPseudo admin`.

## 3. Commandes

| Commande | Description | Permission |
|---|---|---|
| `/menu` (ou clic droit sur l'étoile) | Menu principal | tous |
| `/shop` | Boutique (clic gauche achète, droit vend, Maj = x64 / tout) | tous |
| `/balance [joueur]`, `/pay <joueur> <montant>`, `/baltop` | Économie | tous |
| `/eco <give\|take\|set> <joueur> <montant>` | Gérer l'argent | `monserveur.admin` |
| `/sethome [nom]`, `/home [nom]`, `/delhome <nom>`, `/homes` | Homes | tous |
| `/spawn` | Retour au spawn | tous |
| `/setspawn` | Définir le spawn | `monserveur.admin` |
| `/level` | Niveau et XP | tous |
| `/quests` | Quêtes du jour | tous |
| `/tag` | Choisir / acheter un tag | tous |
| `/rank list`, `/rank set <joueur> <grade>` | Grades | `monserveur.admin` |
| `/monserveur reload` | Recharge config.yml et links.yml | `monserveur.admin` |
| `/monserveur buildspawn [confirm\|undo]` | Construit le spawn autour de toi / annule | `monserveur.admin` |
| `/npc ...` | Gère les PNJ et hologrammes | `monserveur.admin` |
| `/discord` `/youtube` `/store` `/website` `/links` `/rules` `/aide` | Commandes de `links.yml` (modifiables) | tous |

Autres permissions : `monserveur.teleport.bypass` (pas de délai de téléportation),
`monserveur.lobby.bypass` (ignore les protections lobby),
`monserveur.tag.fondateur` (exemple de tag réservé).

## 3 bis. Liens, annonces et accueil personnalisables (`links.yml`)

Le fichier `plugins/MonServeur/links.yml` est créé au premier démarrage.
- **`variables`** : mets ici tes liens (Discord, YouTube, boutique, site) et le nom du serveur.
  Elles s'utilisent partout avec `{discord}`, `{youtube}`, `{server-name}`...
- **`commands`** : chaque entrée devient une commande (`/discord`, `/rules`...). Ajoute-en autant que tu veux.
- **`announcements`** : messages automatiques dans le chat, à intervalle régulier.
- **`welcome`** : titre et messages affichés à chaque connexion.

`{server-name}` fonctionne aussi dans l'en-tête du TAB (`config.yml`, section `tab`).
Après modification : `/monserveur reload` (pas besoin de redémarrer).

## 3 ter. PNJ

Un PNJ est un villageois immobile et invulnérable, avec un texte flottant au-dessus.
Clic droit (ou gauche) = il exécute son action.

```
/npc create <id> <métier> <MENU|COMMAND|LINK> <valeur>
   ex : /npc create marchand toolsmith MENU shop
        /npc create yt butcher LINK youtube
        /npc create hub cleric COMMAND spawn
/npc name <id> <texte>        (MiniMessage, ex : <gold>Marchand)
/npc subtitle <id> <texte>    (- pour effacer)
/npc action <id> <type> <valeur>
/npc profession <id> <métier>
/npc move <id>                (le déplace là où tu es)
/npc remove <id>   /npc list   /npc respawn
/npc hologram <id> <texte>    (texte flottant seul, sans PNJ)
```
Menus disponibles : `main`, `shop`, `homes`, `quests`, `tags`. Pour un LINK, la valeur est le
nom d'une variable de `links.yml`. Les PNJ sont sauvegardés dans `npcs.yml`.
Limite : ce sont des villageois (pas de skin de joueur, ça demande un plugin externe comme Citizens).

## 3 quater. Générer le spawn

1. Va où tu veux le centre du spawn (sur un terrain à peu près plat).
2. `/monserveur buildspawn` : affiche l'avertissement et la taille de la zone.
3. `/monserveur buildspawn confirm` : construit (quelques secondes).
4. Pas content ? `/monserveur buildspawn undo` (possible tant que le serveur n'a pas redémarré).

Le spawn contient une place ronde en damier, une fontaine, 4 chemins, 4 portails, 8 lampadaires,
4 pavillons, 4 cerisiers, un titre flottant, et 7 PNJ (guide, boutique, quêtes, tags, homes, Discord, YouTube).
Il devient le spawn du monde et du plugin (`/spawn`). Style et PNJ : `spawn-build.yml`.
**Attention : la zone est remplacée (terrain, arbres, constructions).**

## 4. Utilisation selon le type de serveur

- **Survie / économie** : laisse la config par défaut, ajuste les prix de la boutique.
- **RPG** : joue sur `levels` (XP, courbe, récompenses) et `quests`.
- **Lobby / hub** : `lobby.enabled: true`, `spawn.teleport-on-join: true`,
  fais `/setspawn` là où tu veux accueillir les joueurs.
- **Minijeux** : le plugin fournit grades, tab, économie, menus et lobby, mais pas
  de moteur de minijeu (arènes, équipes, manches). Ça se code à part, sur cette base.

## 5. Personnaliser
- Textes : format MiniMessage (https://docs.advntr.dev/minimessage/format.html),
  couleurs `<red>`, dégradés `<gradient:red:gold>texte</gradient>`, etc.
- Grades : section `ranks` (préfixe, couleur, nombre de homes, bonus de vente).
- Boutique : section `shop.categories` (nom de matériau Minecraft en MAJUSCULES).
- Les menus (icônes, emplacements) sont dans `src/main/java/dev/monserveur/gui/`.

## 6. Bon à savoir
- Données joueurs : `plugins/MonServeur/players/<uuid>.yml` (sauvegarde auto toutes les 5 min).
- Pas de lien avec Vault / LuckPerms : l'économie et les grades sont ceux du plugin.
- L'XP des blocs peut être farmée en posant/cassant des blocs ; les quêtes aussi.
- Boutique : 45 articles max par catégorie, 21 catégories max.
- Homes : les menus affichent 14 homes max (la commande `/home <nom>` marche pour tous).
- Mise à jour du plugin : remplace le `.jar` (même nom) serveur éteint. `links.yml` et `spawn-build.yml`
  sont créés s'ils n'existent pas, mais jamais écrasés ; les nouveaux messages de `config.yml` ont une valeur par défaut.
