# CryptoTool – Hybride Verschlüsselung in Java

Kleine Java-Anwendung, die drei kryptographische Verfahren kombiniert:

| Verfahren | Algorithmus | Aufgabe |
|---|---|---|
| Symmetrische Verschlüsselung | AES-128 im CBC-Modus (PKCS5-Padding, zufälliger IV) | Verschlüsselt den Klartext |
| Asymmetrische Verschlüsselung | RSA 2048 mit OAEP (SHA-256) | Verschlüsselt den AES-Schlüssel für den Empfänger |
| Hashing | SHA-256 | Integritätsprüfung des Klartextes |

Das Projekt entstand im Rahmen einer Schulaufgabe (Zweierteam: Person A verschlüsselt, Person B entschlüsselt).

**Autoren:** _Name A_, _Name B_

## Inhalt

| Datei | Beschreibung |
|---|---|
| `CryptoGUI.java` | Fenster mit Eingabefeldern (empfohlen) |
| `CryptoTool.java` | Konsolenversion mit Textmenü |
| `Encryptor.java` / `Decryptor.java` | Optional: getrennte Kommandozeilen-Programme gemäss Aufgabenstellung |

## Voraussetzungen

- JDK 11 oder neuer (`javac -version` prüfen)
- Keine externen Bibliotheken nötig

## Starten

```bash
# Fenster-Version
java CryptoGUI.java

# Konsolen-Version
java CryptoTool.java
```

Alternativ klassisch kompilieren:

```bash
javac CryptoGUI.java
java CryptoGUI
```

## Ablauf

### 1. Schlüsselpaar erzeugen (Person B, der Empfänger)

Im Tab **Schlüssel** einen Namen eingeben (z. B. `marco`) und auf *Schlüsselpaar erzeugen* klicken. Es entstehen:

- `marco_public.pem` – wird an Person A weitergegeben
- `marco_private.pem` – bleibt **geheim** bei Person B

### 2. Verschlüsseln (Person A)

Tab **Verschlüsseln**:

1. Klartext eingeben
2. AES-Schlüssel eingeben oder mit *Zufällig* erzeugen (Base64, 16, 24 oder 32 Bytes)
3. Pfad zum Public Key von Person B wählen
4. *Verschlüsseln* klicken

Ausgabe (alles Base64):

- **Verschlüsselter Text** (die ersten 16 Bytes sind der IV)
- **Verschlüsselter Schlüssel** (AES-Schlüssel, mit dem Public Key von B verschlüsselt)
- **Hash** (SHA-256 des Klartextes)

Diese drei Werte werden an Person B geschickt.

### 3. Entschlüsseln (Person B)

Tab **Entschlüsseln**:

1. Die drei erhaltenen Werte in die Felder einfügen
2. Pfad zum eigenen Private Key wählen
3. *Entschlüsseln* klicken

Der Ablauf im Programm:

1. AES-Schlüssel mit dem Private Key entschlüsseln (RSA)
2. Text mit dem AES-Schlüssel entschlüsseln
3. SHA-256 vom Ergebnis berechnen und mit dem erhaltenen Hash vergleichen

Stimmen die Hashes überein, wird der Klartext angezeigt. Andernfalls erscheint eine Fehlermeldung.

## Eigene Keys mit ssh-keygen verwenden

`ssh-keygen` erzeugt standardmässig das OpenSSH-Format, das Java nicht direkt lesen kann. Vor der Verwendung muss konvertiert werden:

```bash
# Keypair erzeugen (RSA, ohne Passphrase)
ssh-keygen -t rsa -b 2048 -m PEM -f mykey -N ""

# Public Key ins X.509-Format (PEM)
ssh-keygen -e -m PKCS8 -f mykey.pub > mykey_public.pem

# Private Key in PKCS8 umwandeln (überschreibt die Datei mykey)
ssh-keygen -p -m PKCS8 -N "" -f mykey
```

Erkennungszeichen: Der Public Key beginnt mit `-----BEGIN PUBLIC KEY-----`, der Private Key mit `-----BEGIN PRIVATE KEY-----`. Steht dort `OPENSSH` oder `RSA PRIVATE KEY`, ist das Format noch falsch.

Ein passender AES-Schlüssel (Base64, 16 Bytes) lässt sich so erzeugen:

```bash
openssl rand -base64 16
```

## Fehlerbehebung

| Meldung / Problem | Ursache und Lösung |
|---|---|
| Key hat das ssh-keygen-Format | Key wie oben konvertieren oder im Tab *Schlüssel* neu erzeugen |
| `Illegal base64 character` | Beim Einfügen wurde etwas abgeschnitten oder ein Zeichen verändert |
| AES-Schlüssel muss 16, 24 oder 32 Bytes lang sein | Schlüssel mit *Zufällig* oder `openssl rand -base64 16` erzeugen |
| `BadPaddingException` / `IllegalBlockSizeException` | Falscher Private Key oder veränderte Daten |
| «Hash stimmt nicht überein» | Der Klartext wurde verändert, oder es wurde ein falscher Hash eingefügt |

## Sicherheitshinweise

- Der Private Key wird **nie** weitergegeben.
- Ein zufälliger IV pro Verschlüsselung sorgt dafür, dass gleicher Klartext jedes Mal anders aussieht.
- Der Hash allein schützt nur vor versehentlicher Veränderung. Ein Angreifer könnte Text und Hash gleichzeitig austauschen. Für echten Schutz braucht es eine digitale Signatur oder einen authentifizierten Modus wie AES-GCM.
- Das Projekt ist für Lernzwecke gedacht und nicht für den produktiven Einsatz geprüft.
