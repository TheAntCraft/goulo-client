# Baut den Goulo Launcher als echte Windows-App (Goulo.exe, mit eigenem Java)
# nach %USERPROFILE%\Apps\Goulo und legt Startmenue-Eintraege an.
# Erneut ausfuehren = aktualisieren. Vorher den Launcher schliessen.
# (Bewusst nicht unter AppData: manche Programme sehen dort nur eine eigene, versteckte Kopie.)

$ErrorActionPreference = 'Stop'
$src = $PSScriptRoot
$root = Join-Path $env:USERPROFILE 'Apps'
$imageDir = Join-Path $root 'Goulo'
$work = Join-Path $env:TEMP 'goulo-build'

# --- JDK finden (echter Ordner, nicht der javapath-Umweg)
$props = cmd /c "java -XshowSettings:properties -version 2>&1"
$homeLine = $props | Where-Object { $_ -match '^\s*java\.home\s*=' } | Select-Object -First 1
if (-not $homeLine) { throw 'Java wurde nicht gefunden. Bitte JDK 21 oder neuer installieren.' }
$javaHome = ($homeLine -split '=', 2)[1].Trim()
$bin = Join-Path $javaHome 'bin'
$javac = Join-Path $bin 'javac.exe'
$jar = Join-Path $bin 'jar.exe'
$jpackage = Join-Path $bin 'jpackage.exe'
$java = Join-Path $bin 'java.exe'
foreach ($tool in $javac, $jar, $jpackage) {
    if (-not (Test-Path $tool)) { throw "$tool fehlt - bitte ein vollstaendiges JDK (21+) installieren." }
}
Write-Host "JDK: $javaHome"

# --- Laeuft der Launcher noch?
$running = Get-Process -ErrorAction SilentlyContinue | Where-Object { $_.Path -and $_.Path.StartsWith($imageDir, [StringComparison]::OrdinalIgnoreCase) }
if ($running) { throw 'Der Launcher laeuft noch. Bitte zuerst schliessen und dann erneut ausfuehren.' }

# --- Arbeitsordner
if (Test-Path $work) { Remove-Item $work -Recurse -Force }
$classes = Join-Path $work 'classes'
$inputDir = Join-Path $work 'input'
New-Item -ItemType Directory -Force $classes, $inputDir | Out-Null

# --- Kompilieren und Jar bauen
$sources = Get-ChildItem (Join-Path $src 'src') -Recurse -Filter *.java | ForEach-Object FullName
& $javac -encoding UTF-8 -d $classes $sources
if ($LASTEXITCODE -ne 0) { throw 'Kompilieren fehlgeschlagen.' }
& $jar --create --file (Join-Path $inputDir 'goulo-launcher.jar') --main-class dev.nova.launcher.Main -C $classes .
if ($LASTEXITCODE -ne 0) { throw 'Jar bauen fehlgeschlagen.' }
Write-Host 'Launcher gebaut.'

# --- Icon
$png = Join-Path $src 'icon.png'
$ico = Join-Path $work 'icon.ico'
$iconArgs = @()
if (Test-Path $png) {
    Copy-Item $png (Join-Path $inputDir 'icon.png')
    & $java -cp $classes dev.nova.launcher.util.IcoWriter $png $ico
    if ($LASTEXITCODE -ne 0) { throw 'Icon erzeugen fehlgeschlagen.' }
    $iconArgs = @('--icon', $ico)
    Write-Host 'Icon erzeugt.'
} else {
    Write-Host 'Hinweis: keine icon.png gefunden - die App bekommt das Standard-Icon.'
}

# --- Client-Mod mitnehmen, falls schon gebaut
$modJar = Join-Path $src '..\client\build\libs\goulo-client-0.1.0.jar'
if (Test-Path $modJar) {
    Copy-Item $modJar (Join-Path $inputDir 'goulo-client.jar')
    Write-Host 'Client-Mod uebernommen.'
} else {
    Write-Host 'Hinweis: Client-Mod ist noch nicht gebaut (client -> gradlew build). Danach dieses Skript erneut ausfuehren.'
}

# --- Alte Installationen entfernen (nur unsere eigenen Ordner)
if (Test-Path $imageDir) { Remove-Item $imageDir -Recurse -Force }
New-Item -ItemType Directory -Force $root | Out-Null

# --- Windows-App bauen (enthaelt ein eigenes, verkleinertes Java)
& $jpackage --type app-image `
    --name Goulo `
    --app-version 0.1.0 `
    --vendor Goulo `
    --description 'Goulo Launcher' `
    --input $inputDir `
    --main-jar goulo-launcher.jar `
    --main-class dev.nova.launcher.Main `
    --java-options '-Dfile.encoding=UTF-8' `
    --add-modules 'java.desktop,java.net.http' `
    --dest $root `
    @iconArgs
if ($LASTEXITCODE -ne 0) { throw 'jpackage fehlgeschlagen.' }
Write-Host "App gebaut: $imageDir"

# --- Startmenue-Verknuepfungen
$programs = [Environment]::GetFolderPath('Programs')
$shell = New-Object -ComObject WScript.Shell

function New-Shortcut($name, $exe, $description) {
    $lnk = $shell.CreateShortcut((Join-Path $programs "$name.lnk"))
    $lnk.TargetPath = Join-Path $imageDir $exe
    $lnk.Arguments = ''
    $lnk.WorkingDirectory = $imageDir
    $lnk.Description = $description
    $lnk.IconLocation = (Join-Path $imageDir $exe) + ',0'
    $lnk.Save()
    Write-Host "Startmenue-Eintrag: $name"
}

New-Shortcut 'Goulo Client' 'Goulo.exe' 'Goulo Launcher'

Remove-Item $work -Recurse -Force -ErrorAction SilentlyContinue
Write-Host ''
Write-Host 'Fertig. In der Windows-Suche nach "Goulo" suchen.'
