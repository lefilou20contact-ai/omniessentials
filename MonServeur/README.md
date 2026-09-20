# MonServeur — plugin tout-en-un pour Paper

Grades, tags, tab/nametag, économie, boutique, menus GUI, niveaux, quêtes
quotidiennes, homes, spawn, mode lobby. Tout se règle dans `config.yml`.

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
| `/monserveur reload` | Recharge la config | `monserveur.admin` |

Autres permissions : `monserveur.teleport.bypass` (pas de délai de téléportation),
`monserveur.lobby.bypass` (ignore les protections lobby),
`monserveur.tag.fondateur` (exemple de tag réservé).

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
