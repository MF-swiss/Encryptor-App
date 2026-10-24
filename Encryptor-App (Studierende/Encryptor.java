import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class Encryptor {
    public static void main(String[] args) {
        if (args.length != 3) {
            System.err.println("Usage: Encryptor <text to encrypt> <symmetric key (base64)> <public key file path>");
            System.exit(1);
        }

        String textToEncrypt = args[0];
        String symmetricKeyString = args[1];
        String publicKeyFilePath = args[2];

        try {
            String pem = new String(Files.readAllBytes(Paths.get(publicKeyFilePath)));
            byte[] publicKeyBytes = Base64.getDecoder().decode(
                    pem.replace("-----BEGIN PUBLIC KEY-----", "")
                       .replace("-----END PUBLIC KEY-----", "")
                       .replaceAll("\\s", ""));
            PublicKey publicKey = KeyFactory.getInstance("RSA")
                    .generatePublic(new X509EncodedKeySpec(publicKeyBytes));

            byte[] decodedKey = Base64.getDecoder().decode(symmetricKeyString);
            SecretKeySpec symmetricKey = new SecretKeySpec(decodedKey, 0, decodedKey.length, "AES");

            byte[] plaintext = textToEncrypt.getBytes("UTF-8");
            byte[] iv = new byte[16];
            new SecureRandom().nextBytes(iv);
            Cipher aes = Cipher.getInstance("AES/CBC/PKCS5Padding");
            aes.init(Cipher.ENCRYPT_MODE, symmetricKey, new IvParameterSpec(iv));
            byte[] cipherText = aes.doFinal(plaintext);

            byte[] ivAndCipher = new byte[iv.length + cipherText.length];
            System.arraycopy(iv, 0, ivAndCipher, 0, iv.length);
            System.arraycopy(cipherText, 0, ivAndCipher, iv.length, cipherText.length);

            Cipher rsa = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
            rsa.init(Cipher.ENCRYPT_MODE, publicKey);
            byte[] encryptedKey = rsa.doFinal(decodedKey);

            byte[] hash = MessageDigest.getInstance("SHA-256").digest(plaintext);

            System.out.println("Encrypted text:          " + Base64.getEncoder().encodeToString(ivAndCipher));
            System.out.println("Encrypted symmetric key: " + Base64.getEncoder().encodeToString(encryptedKey));
            System.out.println("Hash of plaintext:       " + Base64.getEncoder().encodeToString(hash));

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(2);
        }
    }
}
