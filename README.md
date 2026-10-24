# Encryptor / Decryptor

Dieses Projekt zeigt eine hybride Verschlüsselung:

- Der Klartext wird mit **AES/CBC/PKCS5Padding** verschlüsselt.
- Der AES-Schlüssel wird mit **RSA-OAEP (SHA-256)** und dem öffentlichen Schlüssel verschlüsselt.
- Ein **SHA-256-Hash** des Klartexts dient zur Integritätsprüfung.

## Voraussetzungen

- JDK 8 oder neuer
- `java` und `javac` müssen im PATH verfügbar sein
- Ein RSA-Schlüsselpaar im PEM-Format:
  - öffentlicher Schlüssel: `-----BEGIN PUBLIC KEY-----`
  - privater Schlüssel: `-----BEGIN PRIVATE KEY-----`

Die Programme erwarten daher PKCS#8-Private-Keys und X.509-Public-Keys.

## 1. Schlüssel und AES-Schlüssel erzeugen

Die folgenden Befehle funktionieren in PowerShell. Im Projektordner werden ein
RSA-Schlüsselpaar und ein zufälliger 256-Bit-AES-Schlüssel erzeugt:

```powershell
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out private_key.pem
openssl pkey -in private_key.pem -pubout -out public_key.pem
$aesBytes = [byte[]]::new(32)
[Security.Cryptography.RandomNumberGenerator]::Fill($aesBytes)
$aesKey = [Convert]::ToBase64String($aesBytes)
```

Den ausgegebenen Base64-Wert von `$aesKey` für den nächsten Schritt kopieren.
Alternativ kann ein bereits vorhandener Base64-AES-Schlüssel mit 16, 24 oder
32 Byte verwendet werden.

> **Hinweis:** Private Keys und AES-Schlüssel niemals veröffentlichen oder in
> Git einchecken.

## 2. Programme kompilieren

```powershell
New-Item -ItemType Directory -Force build
javac -d build `
  "Decryptor-App (Studierende)\Decryptor.java" `
  "Encryptor-App (Studierende)\Encryptor.java"
```

## 3. Text verschlüsseln

`Encryptor` benötigt genau drei Argumente:

```text
Encryptor <Klartext> <Base64-AES-Schlüssel> <Pfad-zum-public-key.pem>
```

Beispiel:

```powershell
$encrypted = java -cp build Encryptor `
  "Geheime Nachricht" `
  $aesKey `
  .\public_key.pem
$encrypted
```

Die Ausgabe enthält drei Werte:

```text
Encrypted text:          <BASE64_TEXT>
Encrypted symmetric key: <BASE64_AES_KEY>
Hash of plaintext:       <BASE64_HASH>
```

Für die Entschlüsselung die drei Base64-Werte separat kopieren. Leerzeichen
zwischen Bezeichner und Wert gehören nicht zum jeweiligen Wert.

## 4. Text entschlüsseln

`Decryptor` benötigt genau vier Argumente:

```text
Decryptor <Base64-verschlüsselter-Text> `
          <Base64-verschlüsselter-AES-Schlüssel> `
          <Base64-Hash> `
          <Pfad-zum-private-key.pem>
```

Beispiel mit manuell eingesetzten Werten:

```powershell
java -cp build Decryptor `
  "<BASE64_TEXT>" `
  "<BASE64_AES_KEY>" `
  "<BASE64_HASH>" `
  .\private_key.pem
```

Bei erfolgreicher Prüfung erscheint:

```text
Hash OK. Klartext: Geheime Nachricht
```

Stimmt der Hash nicht überein, meldet das Programm:

```text
FEHLER: Hash stimmt nicht überein, Daten wurden verändert!
```

## Fehlercodes

| Code | Bedeutung |
| ---: | --- |
| 0 | Erfolgreich |
| 1 | Falsche Anzahl an Kommandozeilenargumenten |
| 2 | Fehler beim Lesen, Decodieren oder Entschlüsseln |
| 3 | Hashprüfung fehlgeschlagen |

## Ablauf testen

1. Beide Java-Dateien kompilieren.
2. Einen kurzen Text mit `Encryptor` verschlüsseln.
3. Die drei ausgegebenen Werte mit `Decryptor` entschlüsseln.
4. Prüfen, dass der ursprüngliche Text ausgegeben wird.
5. Einen einzelnen Wert absichtlich verändern und erneut entschlüsseln.
   Erwartet wird ein Fehler (typischerweise Fehlercode 2 oder eine
   fehlgeschlagene Hashprüfung).

## Sicherheitshinweis

AES-CBC mit einem separaten SHA-256-Hash ist nur eine Demonstration und kein
modernes authentifiziertes Verschlüsselungsformat. Für produktive Anwendungen
sollte ein AEAD-Verfahren wie **AES-GCM** verwendet werden. Außerdem sollten
Schlüssel nicht als Kommandozeilenargumente übergeben werden, da diese in
Prozesslisten oder Shell-Historien sichtbar sein können.
