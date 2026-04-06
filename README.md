# ⚔ Challenger

A lightweight **server-side only** NeoForge mod that takes full ownership of PvP on your Minecraft server. Players challenge each other to 1v1 battles using the `/battle` command — Challenger blocks ALL player damage by default and only allows it between players who have agreed to fight.

> Built for **NeoForge 21.1.221 / Minecraft 1.21.1**

---

## ✨ Features

- **Challenger owns all PvP** — ALL player vs player damage is blocked unless both players are in an active battle together
- **Consensual dueling** — Both players must agree before any damage is dealt
- **Multiple simultaneous battles** — Any number of battles can run at the same time independently
- **No item loss** — The loser keeps their inventory, gets fully healed, and is teleported to world spawn
- **Auto-expiring requests** — Battle requests cancel automatically after 30 seconds
- **Spam protection** — "PvP disabled" message has a 3 second cooldown per player
- **Server-wide announcements** — Battle start, results and forfeits are broadcast to everyone
- **Server-side only** — Players do not need to install anything on their client
- **UUID based** — Player name changes won't break anything

---

## 📋 Commands

| Command | Description |
|---|---|
| `/battle <player>` | Send a duel request |
| `/battle accept <player>` | Accept an incoming request |
| `/battle deny <player>` | Decline an incoming request |
| `/battle forfeit` | Surrender your current battle |
| `/battle status` | Check if you are in a battle |

---

## 🎮 How It Works

1. Player A types `/battle PlayerB`
2. PlayerB gets a notification and has 30 seconds to accept or deny
3. Once accepted the whole server sees the battle announcement
4. **Only A and B can damage each other** — any other player hitting anyone gets blocked
5. When a player dies, death is cancelled — inventory kept, fully healed, teleported to spawn
6. The winner and result is announced to the whole server

**Example with multiple battles running simultaneously:**
- A vs B active → only A and B can damage each other
- C vs D active → only C and D can damage each other
- A trying to hit C → blocked ✅
- E trying to hit anyone → blocked ✅

---

## 🔧 Installation

**1 — Set `pvp=true` in `server.properties`**

Challenger requires PvP to be enabled at the server level so it can intercept and manage all damage itself. Don't worry — Challenger blocks ALL player damage by default. No battle = no damage, period.

```properties
pvp=true
```

**2 — Drop the JAR into your `mods/` folder and restart**

```
mods/
  └── challenger-1.0.0.jar
```

**3 — Recommended: run this in your server console**

```
/gamerule keepInventory true
```

Look for `Challenger loaded! PvP is managed exclusively by Challenger.` in the console to confirm it's working.

---

## 🏗 Building from Source

### Requirements
- Java 21
- NeoForge MDK for 1.21.1 → https://github.com/NeoForgeMDKs/MDK-1.21.1-NeoGradle

### Steps

**1 — Download and extract the MDK**

**2 — Update `gradle.properties`** with these values:
```properties
mod_id=challenger
mod_name=Challenger
mod_version=1.0.0
mod_group_id=com.challenger
neo_version=21.1.221
```

**3 — Copy the `src/` folder** from this repo into the MDK root, replacing the existing one.

**4 — Build:**
```bash
# Windows
.\gradlew.bat build

# Linux / Mac
./gradlew build
```

**5 — Grab your JAR** from `build/libs/challenger-1.0.0.jar`

---

## ⚙️ Compatibility

| | |
|---|---|
| Minecraft | 1.21.1 |
| NeoForge | 21.1.221+ |
| Server-side only | ✅ |
| `pvp=true` required | ✅ |
| ATM10 v6.2.1 | ✅ Tested |
| Client install required | ❌ |
| Multiple simultaneous battles | ✅ |

---

## 🗺 Roadmap

- [ ] Auto-end battle when a player disconnects mid-fight
- [ ] Configurable request timeout duration
- [ ] Optional arena teleport instead of world spawn
- [ ] Win/loss leaderboard via `/battle stats`
- [ ] Optional item betting system

---

## 📝 Known Issues

- If a player disconnects mid-battle the battle state is not automatically cleared — the remaining player will need to use `/battle forfeit` to exit

---

## 📜 License

MIT — free to use, modify and distribute. If you use this on your server or build on top of it, a credit is appreciated but not required!
