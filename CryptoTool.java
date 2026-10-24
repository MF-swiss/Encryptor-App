import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;
import java.util.Scanner;

/**
 * Kleines Menü-Programm: Hybride Verschlüsselung (AES + RSA + SHA-256).
 * Start: java CryptoTool.java   (oder javac CryptoTool.java && java CryptoTool)
 */
public class CryptoTool {

    static final Scanner in = new Scanner(System.in);

    public static void main(String[] args) {
        while (true) {
            System.out.println("\n=== CryptoTool ===");
            System.out.println("1) Schlüsselpaar erzeugen");
            System.out.println("2) Verschlüsseln");
            System.out.println("3) Entschlüsseln");
            System.out.println("0) Beenden");
            String choice = ask("Auswahl");

            try {
                switch (choice) {
                    case "1": generateKeys(); break;
                    case "2": encrypt(); break;
                    case "3": decrypt(); break;
                    case "0": return;
                    default: System.out.println("Ungültige Auswahl.");
                }
            } catch (Exception e) {
                System.out.println("FEHLER: " + e);
            }
        }
    }

    // ---------- 1) Schlüsselpaar ----------
    static void generateKeys() throws Exception {
        String name = ask("Dateiname-Präfix (z.B. marco)");
        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);
        KeyPair pair = gen.generateKeyPair();

        writePem(name + "_public.pem", "PUBLIC KEY", pair.getPublic().getEncoded());
        writePem(name + "_private.pem", "PRIVATE KEY", pair.getPrivate().getEncoded());
        System.out.println("Erstellt: " + name + "_public.pem  und  " + name + "_private.pem");
        System.out.println("Den PUBLIC Key an den Partner schicken, den PRIVATE Key geheim halten!");
    }

    // ---------- 2) Verschlüsseln ----------
    static void encrypt() throws Exception {
        String text = ask("Klartext");
        String keyInput = ask("Symmetrischer Schlüssel in Base64 (leer = automatisch erzeugen)");
        String pubPath = ask("Pfad zum Public Key des Empfängers");

        byte[] aesKey;
        if (keyInput.isEmpty()) {
            aesKey = new byte[16];
            new SecureRandom().nextBytes(aesKey);
            System.out.println("Erzeugter AES-Schlüssel: " + Base64.getEncoder().encodeToString(aesKey));
        } else {
            aesKey = Base64.getDecoder().decode(keyInput);
        }

        byte[] plaintext = text.getBytes("UTF-8");

        // AES-CBC, zufälliger IV wird vorne an den Ciphertext gehängt
        byte[] iv = new byte[16];
        new SecureRandom().nextBytes(iv);
        Cipher aes = Cipher.getInstance("AES/CBC/PKCS5Padding");
        aes.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(aesKey, "AES"), new IvParameterSpec(iv));
        byte[] cipher = aes.doFinal(plaintext);
        byte[] ivAndCipher = concat(iv, cipher);

        // RSA: AES-Schlüssel verschlüsseln
        Cipher rsa = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
        rsa.init(Cipher.ENCRYPT_MODE, loadPublicKey(pubPath));
        byte[] encKey = rsa.doFinal(aesKey);

        // SHA-256 vom Klartext
        byte[] hash = MessageDigest.getInstance("SHA-256").digest(plaintext);

        System.out.println("\n--- Diese 3 Werte an den Empfänger senden ---");
        System.out.println("Verschlüsselter Text:    " + b64(ivAndCipher));
        System.out.println("Verschlüsselter Schlüssel: " + b64(encKey));
        System.out.println("Hash:                    " + b64(hash));
    }

    // ---------- 3) Entschlüsseln ----------
    static void decrypt() throws Exception {
        byte[] encText = Base64.getDecoder().decode(ask("Verschlüsselter Text (Base64)"));
        byte[] encKey = Base64.getDecoder().decode(ask("Verschlüsselter Schlüssel (Base64)"));
        byte[] receivedHash = Base64.getDecoder().decode(ask("Hash (Base64)"));
        String privPath = ask("Pfad zum Private Key");

        // RSA: AES-Schlüssel entschlüsseln
        Cipher rsa = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
        rsa.init(Cipher.DECRYPT_MODE, loadPrivateKey(privPath));
        byte[] aesKey = rsa.doFinal(encKey);

        // AES: Text entschlüsseln
        byte[] iv = Arrays.copyOfRange(encText, 0, 16);
        byte[] cipher = Arrays.copyOfRange(encText, 16, encText.length);
        Cipher aes = Cipher.getInstance("AES/CBC/PKCS5Padding");
        aes.init(Cipher.DECRYPT_MODE, new SecretKeySpec(aesKey, "AES"), new IvParameterSpec(iv));
        byte[] plaintext = aes.doFinal(cipher);

        // Integritätsprüfung
        byte[] hash = MessageDigest.getInstance("SHA-256").digest(plaintext);
        if (MessageDigest.isEqual(hash, receivedHash)) {
            System.out.println("\nHash stimmt. Klartext: " + new String(plaintext, "UTF-8"));
        } else {
            System.out.println("\nFEHLER: Hash stimmt NICHT überein - Daten wurden verändert!");
        }
    }

    // ---------- Hilfsmethoden ----------
    static String ask(String prompt) {
        System.out.print(prompt + ": ");
        return in.nextLine().trim();
    }

    static String b64(byte[] data) {
        return Base64.getEncoder().encodeToString(data);
    }

    static byte[] concat(byte[] a, byte[] b) {
        byte[] r = new byte[a.length + b.length];
        System.arraycopy(a, 0, r, 0, a.length);
        System.arraycopy(b, 0, r, a.length, b.length);
        return r;
    }

    static byte[] readPem(String path) throws Exception {
        String pem = new String(Files.readAllBytes(Paths.get(path)));
        if (pem.contains("OPENSSH") || pem.contains("RSA PRIVATE KEY") || pem.startsWith("ssh-rsa")) {
            throw new IllegalArgumentException(
                "Key hat das ssh-keygen-Format. Bitte zuerst konvertieren (siehe Befehle) oder Option 1 nutzen.");
        }
        return Base64.getDecoder().decode(pem.replaceAll("-----[A-Z ]+-----", "").replaceAll("\\s", ""));
    }

    static PublicKey loadPublicKey(String path) throws Exception {
        return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(readPem(path)));
    }

    static PrivateKey loadPrivateKey(String path) throws Exception {
        return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(readPem(path)));
    }

    static void writePem(String file, String type, byte[] der) throws Exception {
        String body = Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(der);
        String pem = "-----BEGIN " + type + "-----\n" + body + "\n-----END " + type + "-----\n";
        Files.write(Paths.get(file), pem.getBytes());
    }
}