import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;

public class Decryptor {
    public static void main(String[] args) {
        if (args.length != 4) {
            System.err.println("Usage: Decryptor <base64 encrypted text> <base64 encrypted symmetric key> <hash of plaintext> <private key file path>");
            System.exit(1);
        }

        String encryptedTextBase64 = args[0];
        String encryptedSymmetricKeyBase64 = args[1];
        String plaintextHash = args[2];
        String privateKeyFilePath = args[3];

        try {
            String pem = new String(Files.readAllBytes(Paths.get(privateKeyFilePath)));
            byte[] privateKeyBytes = Base64.getDecoder().decode(
                    pem.replace("-----BEGIN PRIVATE KEY-----", "")
                       .replace("-----END PRIVATE KEY-----", "")
                       .replaceAll("\\s", ""));
            PrivateKey privateKey = KeyFactory.getInstance("RSA")
                    .generatePrivate(new PKCS8EncodedKeySpec(privateKeyBytes));

            byte[] encryptedText = Base64.getDecoder().decode(encryptedTextBase64);
            byte[] encryptedSymmetricKey = Base64.getDecoder().decode(encryptedSymmetricKeyBase64);

            Cipher rsa = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
            rsa.init(Cipher.DECRYPT_MODE, privateKey);
            byte[] keyBytes = rsa.doFinal(encryptedSymmetricKey);
            SecretKeySpec symmetricKey = new SecretKeySpec(keyBytes, "AES");

            byte[] iv = Arrays.copyOfRange(encryptedText, 0, 16);
            byte[] cipherText = Arrays.copyOfRange(encryptedText, 16, encryptedText.length);
            Cipher aes = Cipher.getInstance("AES/CBC/PKCS5Padding");
            aes.init(Cipher.DECRYPT_MODE, symmetricKey, new IvParameterSpec(iv));
            byte[] plaintext = aes.doFinal(cipherText);

            byte[] computedHash = MessageDigest.getInstance("SHA-256").digest(plaintext);
            byte[] receivedHash = Base64.getDecoder().decode(plaintextHash);

            if (MessageDigest.isEqual(computedHash, receivedHash)) {
                System.out.println("Hash OK. Klartext: " + new String(plaintext, "UTF-8"));
            } else {
                System.err.println("FEHLER: Hash stimmt nicht überein, Daten wurden verändert!");
                System.exit(3);
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(2);
        }
    }
}
