# Simple Voice Moderation

Paper plugin that hooks into Simple Voice Chat and exposes moderation permissions.

## Requirements

- Java 21
- Maven 3.9+
- Paper server 1.21.x
- Simple Voice Chat server plugin/mod
- LuckPerms, or another Bukkit permission manager

## Permissions

- `simplevoicemoderation.group.password.bypass`: lets a player join password-protected Simple Voice Chat groups without knowing the password.
- `simplevoicemoderation.command`: allows `/svm`.

Example LuckPerms grants:

```sh
lp group moderator permission set simplevoicemoderation.group.password.bypass true
lp group admin permission set simplevoicemoderation.group.password.bypass true
```

## Build

```sh
mvn package
```

The plugin jar will be created in `target/`.

## Install

1. Copy the generated jar into your Paper server's `plugins/` directory.
2. Restart the server.
3. Run `/svm` in game or from the server console.
