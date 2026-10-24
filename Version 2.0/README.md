# CryptoTool 2.0

Version 2 enthält nur die grafische Anwendung:

```text
CryptoGUI.java
```

Die alte Version bleibt unter `Version 1.0` erhalten.

## Starten

PowerShell im Ordner `Version 2.0` öffnen:

```powershell
cd C:\Dev\Encryptor-App\Version 2.0
javac CryptoGUI.java
java CryptoGUI
```

## Verwenden

### 1. Schlüssel erzeugen

1. Tab **Schlüssel** öffnen.
2. Dateiname-Präfix eingeben, zum Beispiel `b`.
3. **Schlüsselpaar erzeugen** klicken.

Es entstehen:

```text
b_public.pem
b_private.pem
```

Der Public Key wird zum Verschlüsseln verwendet. Der Private Key bleibt
geheim und wird zum Entschlüsseln benötigt.

### 2. Verschlüsseln

1. Tab **Verschlüsseln** öffnen.
2. Klartext eingeben.
3. Beim AES-Schlüssel auf **Zufällig** klicken oder einen eigenen Base64-Key
   eintragen.
4. Den Public Key des Empfängers auswählen.
5. Bei **Hash / RSA-OAEP** einen Algorithmus auswählen.
6. **Verschlüsseln** klicken.

Die drei ausgegebenen Werte vollständig kopieren:

- Verschlüsselter Text
- Verschlüsselter Schlüssel
- Hash des Klartextes

### 3. Entschlüsseln

1. Tab **Entschlüsseln** öffnen.
2. Die drei Werte aus dem Verschlüsselungs-Tab einfügen.
3. Den passenden Private Key auswählen.
4. Bei **Hash / RSA-OAEP** exakt denselben Algorithmus wie beim
   Verschlüsseln auswählen.
5. **Entschlüsseln** klicken.

Bei Erfolg erscheint der ursprüngliche Klartext.

## Wichtig

```text
Verschlüsseln: Public Key des Empfängers
Entschlüsseln: Private Key des Empfängers
```

Public und Private Key müssen zum selben Schlüsselpaar gehören.

## Hash- und Web-Crypto-Auswahl

Das Dropdown bietet `SHA-1`, `SHA-256`, `SHA-384` und `SHA-512`.

Die Auswahl wird für den Hash des Klartextes, RSA-OAEP und MGF1 verwendet.
Das ist für Web Crypto wichtig: Web Crypto verwendet bei `RSA-OAEP`
denselben Hash für OAEP und MGF1. Beim Verschlüsseln und Entschlüsseln muss
daher dieselbe Auswahl getroffen werden.

`SHA-256` ist die empfohlene Auswahl. `SHA-1` ist nur für ältere
Kompatibilität gedacht.

Die erzeugten `.pem`-Dateien und `.class`-Dateien sind lokale Testdateien
und sollen nicht veröffentlicht werden.
