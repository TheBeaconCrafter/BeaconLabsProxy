# BeaconLabsProxy Plugin

**BeaconLabsProxy** is a versatile plugin for BungeeCord that enhances server management and player interaction with various features.

## Features

- **Punishment System:** Allows admins to ban, kick and mute players for a set amount of time.
- **Report System:** Allows players to report misbehaving members of your community.
- **Built-in chatfilter:** Gives supporters the option to see offensive messages proxy-wide
- **Chatlogger:** All chats and commands are saved and can be retrieved later on using /chatreport
- **Discord Webhooks:** All actions (bans, kicks, mutes, etc) by your supporters can be logged in a Discord Channel
- **JoinMe:** Allows players to invite others to their current server.
- **Maintenance Mode:** Enables or disables maintenance mode, kicking non-authorized players.
- **MOTD with dynamic text:** Configurable messages that can be displayed in server list descriptions.
- **Team Chat:** Team Chat for all your staff members to talk.
- **Permissions:** Provides fine-grained control over who can use each command and bypass maintenance mode.
- **Serverguard:** Protect backend servers from unwanted connections by players (kind of like a whitelist, but with an invite system).
- **Playtime:** Builtin playtime tracker (/pt)
- **Many more:** Including useful tools like /staff, /uid, /check, /punishments, /ping, /skin, /goto, /broadcast

## Commands

### /ban
- **Description:** Bans players from your server for a set amount of time
- **Usage:** `/ban <player> [time] <reason>` Example: `/ban ItsBeacon 10d Cheating`
- **Permissions:** `beaconlabs.ban`

### /unban
- **Description:** Unbans a players from your server
- **Usage:** `/unban <player>` Example: `/unban ItsBeacon`
- **Permissions:** `beaconlabs.unban`

### /kick
- **Description:** Bans players from your server for a set amount of time
- **Usage:** `/kick <player> <reason>` Example: `/kick ItsBeacon Spamming`
- **Permissions:** `beaconlabs.kick`

### /warn
- **Description:** Handles bans, kicks & mutes automatically based on the reason
- **Usage:** `/warn <player> <reason>` Example: `/warn ItsBeacon Chatabuse`
- **Permissions:** `beaconlabs.warn`

### /joinme
- **Description:** Invites players from all servers to join the sender's current server.
- **Usage:** `/joinme`
- **Permissions:** `beaconlabs.joinme`

### /maintenance
- **Description:** Enables or disables maintenance mode, kicking non-authorized players.
- **Usage:** `/maintenance <on|off>`
- **Permissions:** `beaconlabs.maintenance` & `beaconlabs.maintenancejoin`

### /serverguard (aliases: /sg)
- **Description:** Protect backend servers via an invite-based whitelist system.
- **Usage:** `/serverguard <subcommand> ...` or `/sg <subcommand> ...`
- **Permissions:** `beaconlabs.serverguard.admin`

#### /sg invite <player> <server>
- **Description:** Grants permanent access for the player to the specified server.
- **Usage:** `/sg invite <player> <server>`

#### /sg tempinvite <player> <server> <duration>
- **Description:** Grants temporary access for the player to the specified server.
- **Usage:** `/sg tempinvite <player> <server> <duration>` (e.g., 1d, 2h, 30min)

#### /sg remove <player> <server>
- **Description:** Revokes access for the player from the specified server.
- **Usage:** `/sg remove <player> <server>`

#### /sg plist <player>
- **Description:** Lists all servers the specified player is invited to.
- **Usage:** `/sg plist <player>`

#### /sg slist <server>
- **Description:** Lists all players invited to the specified server.
- **Usage:** `/sg slist <server>`

### /serverperm
- **Description:** Manage server-specific permission requirements.
- **Usage:** `/serverperm <subcommand> ...`
- **Permissions:** `beaconlabs.admin.serverperm`

#### /serverperm list
- **Description:** Lists all configured server permission requirements.
- **Usage:** `/serverperm list`

#### /serverperm set <server> <permission>
- **Description:** Sets a permission requirement for a server.
- **Usage:** `/serverperm set <server> <permission>`

#### /serverperm remove <server>
- **Description:** Removes a permission requirement for a server.
- **Usage:** `/serverperm remove <server>`

...many more! Documentation following soon.

## Configuration

- **config.yml:** Contains settings for dynamic messages, excluded servers, and other plugin configurations.

## Installation

1. **Download:** Download the BeaconLabsProxy plugin JAR file.
2. **Place:** Place the JAR file in the plugins folder of your BungeeCord server.
3. **Start & stop server:** Start your server and instantly stop it
4. **Configure:** Edit the `config.yml` to customize messages and settings.
5. **Restart:** Restart your BungeeCord server to load the plugin.

## License

This plugin is licensed under the MIT License. See LICENSE.MD for more details.

## Support

For any issues or feature requests, please open an issue on [GitHub](https://github.com/TheBeaconCrafter/BeaconLabsProxy/issues).
