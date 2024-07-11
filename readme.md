# BeaconLabsProxy Plugin

**BeaconLabsProxy** is a versatile plugin for BungeeCord that enhances server management and player interaction with various features.

## Features

- **Punishment System:** Allows admins to ban, kick and mute players for a set amount of time.
- **Report System:** Allows players to report misbehaving members of your community.
- **Built-in chatfilter:** Gives supporters the option to see offensive messages proxy-wide
- **Discord Webhooks:** All actions (bans, kicks, mutes, etc) by your supporters can be logged in a Discord Channel
- **JoinMe:** Allows players to invite others to their current server.
- **Maintenance Mode:** Enables or disables maintenance mode, kicking non-authorized players.
- **MOTD with dynamic text:** Configurable messages that can be displayed in server list descriptions.
- **Team Chat:** Team Chat for all your staff members to talk.
- **Permissions:** Provides fine-grained control over who can use each command and bypass maintenance mode.
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

### /joinme
- **Description:** Invites players from all servers to join the sender's current server.
- **Usage:** `/joinme`
- **Permissions:** `beaconlabs.joinme`

### /maintenance
- **Description:** Enables or disables maintenance mode, kicking non-authorized players.
- **Usage:** `/maintenance <on|off>`
- **Permissions:** `beaconlabs.maintenance` & `beaconlabs.maintenancejoin`

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