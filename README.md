# Goulo Client

A small, **private and non-commercial** Minecraft: Java Edition launcher plus a Fabric client mod,
built for two friends who play together on their own small server ("Gulaschfritteuse").
It is not distributed publicly and not monetized.

> Not an official Minecraft product. Not approved by or associated with Mojang or Microsoft.

## What it does

| Part | Folder | Description |
|---|---|---|
| Launcher | [`launcher/`](launcher) | Windows desktop app (Java/Swing). Signs in with Microsoft, installs Minecraft 1.21.4 + Fabric, manages profiles and skins, starts the game. |
| Client mod | [`client/`](client) | Fabric mod for Minecraft 1.21.4. Quality-of-life / HUD framework only (module menu, HUD editor, config). No cheats. |

## Sign-in and account security

- **Microsoft only.** Sign-in uses the OAuth 2.0 *device code flow*: the launcher shows a code, the user enters it
  in their own browser at microsoft.com/link. The launcher never sees a password.
- Token chain: Microsoft → Xbox Live → XSTS → `api.minecraftservices.com/authentication/login_with_xbox`
  → `minecraft/profile`.
- **Game ownership is required.** If the account has no Minecraft: Java Edition profile, the launcher refuses
  to start the game. There is **no offline / cracked mode**.
- The Minecraft access token is kept in memory only. The Microsoft refresh token is stored locally on the
  user's own PC (`%APPDATA%\.goulo\launcher\account.properties`) so the user does not have to sign in
  on every start. Nothing is sent to any server other than Microsoft/Mojang.
- No telemetry, no own backend, no third-party services.

Azure application (client) ID: `77e8924c-fbf4-46bd-8315-b75b679434bb` (personal Microsoft accounts only).

## Downloads

The launcher only downloads from official sources:

- Mojang: version manifest (`piston-meta.mojang.com`), client, libraries, assets
  (`resources.download.minecraft.net`), and Mojang's Java runtime.
- FabricMC: Fabric Loader profile (`meta.fabricmc.net`) and Fabric API (`maven.fabricmc.net`).

## Skins

The *Skins* page uses the official Minecraft Services endpoints to upload a skin
(`POST /minecraft/profile/skins`) or reset it (`DELETE /minecraft/profile/skins/active`) for the signed-in user.

## Building

- **Launcher:** JDK 21+ on Windows. Run `launcher/install.bat`. It builds a Windows app with `jpackage`
  into `%USERPROFILE%\Apps\Goulo` and adds a Start menu entry.
- **Client mod:** open `client/` in IntelliJ IDEA (Gradle import), then run the Gradle task `build`.
  The jar ends up in `client/build/libs/`. Re-run `launcher/install.bat` afterwards to bundle it.

---

## Kurz auf Deutsch

Privater Launcher und Client-Mod für unseren eigenen kleinen Minecraft-Server. Anmeldung nur mit Microsoft
und gekauftem Minecraft, kein Offline-Modus, Downloads nur von Mojang und Fabric. Details siehe oben und in
den READMEs der Unterordner.

## License

[MIT](LICENSE)
