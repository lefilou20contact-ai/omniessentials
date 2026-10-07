# Omniessentials — all-in-one plugin for Paper

Ranks, tags, tab/nametag, economy, shop, GUI menus, levels, daily quests,
homes, spawn, lobby mode, customizable links/announcements,
NPCs, spawn generator, player-to-player teleportation (/tpa), quick sell
(/sell), and auction house (/ah). Settings in `config.yml`, `links.yml` and `spawn-build.yml`.

## 2. Install on [My Server]
1. Panel [My Server]: server on **Paper** (same version as in the pom).
2. File manager / FTP: place the `.jar` in the `plugins/` folder.
3. Restart the server. `config.yml` is created in `plugins/monServeur/`.
4. Grant yourself the admin rank: from the panel console, run `op YourUsername`
   then in-game `/rank set YourUsername admin`.

## 3. Commands

| Command | Description | Permission |
|---|---|---|
| `/menu` (or right-click the star) | Main menu | everyone |
| `/shop` | Shop (left click to buy, right click to sell, Shift = x64 / all) | everyone |
| `/balance [player]`, `/pay <player> <amount>`, `/baltop` | Economy | everyone |
| `/eco <give\|take\|set> <player> <amount>` | Manage money | `monserveur.admin` |
| `/sethome [name]`, `/home [name]`, `/delhome <name>`, `/homes` | Homes | everyone |
| `/spawn` | Return to spawn | everyone |
| `/setspawn` | Define the spawn | `monserveur.admin` |
| `/level` | Level and XP | everyone |
| `/quests` | Daily quests | everyone |
| `/tag` | Choose / buy a tag | everyone |
| `/rank list`, `/rank set <player> <rank>` | Ranks | `monserveur.admin` |
| `/tpa <player>`, `/tpahere <player>` | Teleport request | everyone |
| `/tpaccept`, `/tpdeny`, `/tpacancel` | Respond to a request | everyone |
| `/back` | Return to previous position (before tp or death) | everyone |
| `/sell hand`, `/sell all` | Quick sale without opening the menu | everyone |
| `/ah`, `/ah my`, `/ah collect`, `/ah sell <price>` | Player-to-player auction house | everyone |
| `/monserveur reload` | Reload `config.yml` and `links.yml` | `monserveur.admin` |
| `/monserveur buildspawn [confirm\|undo]` | Builds the spawn around you / cancels it | `monserveur.admin` |
| `/npc ...` | Manage NPCs and holograms | `monserveur.admin` |
| `/discord` `/youtube` `/store` `/website` `/links` `/rules` `/aide` | Commands from `links.yml` (modifiable) | everyone |

Additional permissions: `monserveur.teleport.bypass` (ignores teleport delay),
`monserveur.lobby.bypass` (ignores lobby protections),
`monserveur.tag.fondateur` (example of a reserved tag).

## 3 bis. Customizable links, announcements and welcome screen (`links.yml`)

The file `plugins/MonServeur/links.yml` is created on first startup.
- **`variables`**: add your links here (Discord, YouTube, shop, website, server name).
  They can be used everywhere with `{discord}`, `{youtube}`, `{server-name}`...
- **`commands`**: each entry becomes a command (`/discord`, `/rules`...). Add as many as you want.
- **`announcements`**: automatic chat messages at regular intervals.
- **`welcome`**: title and messages displayed on each connection.

`{server-name}` also works in the TAB header (`config.yml`, `tab` section).
After editing: `/monserveur reload` (no need to restart).

## 3 ter. NPCs

An NPC is an immobile and invulnerable villager, with floating text above it.
Right click (or left click) = it executes its action.

```
/npc create <id> <profession> <MENU|COMMAND|LINK> <value>
   ex : /npc create merchant toolsmith MENU shop
        /npc create yt butcher LINK youtube
        /npc create hub cleric COMMAND spawn
/npc name <id> <text>        (MiniMessage, ex : <gold>Merchant)
/npc subtitle <id> <text>    (- to clear)
/npc action <id> <type> <value>
/npc profession <id> <profession>
/npc move <id>               (moves it to where you are)
/npc remove <id>   /npc list   /npc respawn
/npc hologram <id> <text>    (floating text alone, without NPC)
```

Available menus: `main`, `shop`, `homes`, `quests`, `tags`. For a LINK, the value is the
name of a variable from `links.yml`. NPCs are saved in `npcs.yml`.
Limit: these are villagers (no player skin, this requires an external plugin such as Citizens).

## 3 quater. Generate the spawn

1. Go where you want the center of the spawn (on a roughly flat area).
2. `/monserveur buildspawn`: displays the warning and the size of the area.
3. `/monserveur buildspawn confirm`: builds it (takes a few seconds).
4. Not happy? `/monserveur buildspawn undo` (possible as long as the server has not restarted).

The spawn contains a round checkered plaza, a fountain, 4 paths, 4 portals, 8 lamps,
4 pavilions, 4 cherry trees, a floating title, and 7 NPCs (guide, shop, quests, tags, homes, Discord, YouTube).
It becomes the spawn of the world and the plugin (`/spawn`). Style and NPCs: `spawn-build.yml`.
**Warning: the area is replaced (terrain, trees, constructions).**

## 3 quinquies. Teleportation between players and /back

`/tpa <player>` sends a request (`/tpaccept` or `/tpdeny` on the recipient side, expires after
one minute by default). `/tpahere <player>` does the opposite: the other player comes to you. `/back`
returns to your previous position: before `/home`, `/spawn`, `/tpa`,
or after death. Adjustable in `config.yml`, `teleport` section.

## 3 sexies. Quick sell and auction house

- **`/sell hand`** sells the item in hand, **`/sell all`** sells everything sellable in
the inventory (same prices as `/shop`, rank bonus included). Renamed items are not sold
(protection against special items).
- **`/ah`** opens the auction house: players sell their items to other players.
  "Sell" button (hold the item in hand, the price is entered in chat) or `/ah sell <price>`.
  `/ah my`: your listings, removable if needed. `/ah collect` or `/ah mailbox`: your mailbox,
  for items received offline, expired listings, or removed items, or when your inventory was full at the time of purchase.
- Settings in `config.yml`, `auction` section: time before automatic return, number
  of active listings per player, commission taken on sale, min/max price.

## 4. Usage depending on server type

- **Survival / economy**: leave the default config, adjust shop prices.
- **RPG**: use `levels` (XP, curve, rewards) and `quests`.
- **Lobby / hub**: `lobby.enabled: true`, `spawn.teleport-on-join: true`,
  do `/setspawn` where you want to welcome players.
- **Minigames**: the plugin provides ranks, tab, economy, menus and lobby, but not
the minigame engine itself (arenas, teams, rounds). That is coded separately, on this base.

## 5. Customize
- Texts: MiniMessage format (https://docs.advntr.dev/minimessage/format.html),
  colors like `<red>`, gradients like `<gradient:red:gold>text</gradient>`, etc.
- Ranks: `ranks` section (prefix, color, number of homes, sale bonus).
- Shop: `shop.categories` section (Minecraft material name in uppercase).
- Menus (icons, slots) are in `src/main/java/dev/monserveur/gui/`.

## 6. Good to know
- Player data: `plugins/MonServeur/players/<uuid>.yml` (auto-saved every 5 minutes).
- Auction house: listings are in `plugins/MonServeur/auctions.yml`, the mailbox in `mailbox.yml`. Do not edit them manually.
- Block XP can be farmed by placing/breaking blocks; quests as well.
- Shop: max 45 items per category, max 21 categories.
- Homes: menus display max 14 homes (the `/home <name>` command works for all).
- Updating the plugin: replace the `.jar` (same name) with the server stopped. `links.yml` and `spawn-build.yml`
  are created if they do not exist, but are never overwritten; new default values in `config.yml` are added automatically.
