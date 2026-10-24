import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.swing.*;
import java.awt.*;
import java.io.File;
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

/**
 * Kleines Fenster für hybride Verschlüsselung (AES + RSA + SHA-256).
 * Start: java CryptoGUI.java   (oder javac CryptoGUI.java && java CryptoGUI)
 */
public class CryptoGUI extends JFrame {

    // Tab "Schlüssel"
    private final JTextField keyName = new JTextField("marco");
    private final JLabel keyStatus = new JLabel(" ");

    // Tab "Verschlüsseln"
    private final JTextArea encPlain = new JTextArea(4, 40);
    private final JTextField encKey = new JTextField();
    private final JTextField encPub = new JTextField();
    private final JTextArea outText = new JTextArea(4, 40);
    private final JTextArea outKey = new JTextArea(3, 40);
    private final JTextArea outHash = new JTextArea(2, 40);

    // Tab "Entschlüsseln"
    private final JTextArea decText = new JTextArea(4, 40);
    private final JTextArea decKey = new JTextArea(3, 40);
    private final JTextArea decHash = new JTextArea(2, 40);
    private final JTextField decPriv = new JTextField();
    private final JTextArea decResult = new JTextArea(4, 40);

    public CryptoGUI() {
        super("CryptoTool");
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Schlüssel", keyPanel());
        tabs.addTab("Verschlüsseln", encryptPanel());
        tabs.addTab("Entschlüsseln", decryptPanel());
        add(tabs);

        setSize(700, 620);
        setLocationRelativeTo(null);
    }

    // ================= Oberfläche =================

    private JPanel keyPanel() {
        JPanel p = newPanel();
        int r = 0;
        addRow(p, r++, "Dateiname-Präfix", keyName, 0);
        JButton b = new JButton("Schlüsselpaar erzeugen");
        b.addActionListener(e -> run(this::doGenerateKeys));
        addRow(p, r++, "", b, 0);
        addRow(p, r++, "Status", keyStatus, 0);
        addRow(p, r++, "", new JLabel(" "), 1);
        return p;
    }

    private JPanel encryptPanel() {
        JPanel p = newPanel();
        int r = 0;
        addRow(p, r++, "Klartext", scroll(encPlain, true), 1);

        JButton random = new JButton("Zufällig");
        random.addActionListener(e -> encKey.setText(b64(randomKey())));
        JPanel keyRow = new JPanel(new BorderLayout(5, 0));
        keyRow.add(encKey, BorderLayout.CENTER);
        keyRow.add(random, BorderLayout.EAST);
        addRow(p, r++, "AES-Schlüssel (Base64)", keyRow, 0);

        addRow(p, r++, "Public Key (Pfad)", pathRow(encPub), 0);

        JButton go = new JButton("Verschlüsseln");
        go.addActionListener(e -> run(this::doEncrypt));
        addRow(p, r++, "", go, 0);

        addRow(p, r++, "Verschlüsselter Text", scroll(outText, false), 1);
        addRow(p, r++, "Verschlüsselter Schlüssel", scroll(outKey, false), 1);
        addRow(p, r++, "Hash des Klartextes", scroll(outHash, false), 1);
        return p;
    }

    private JPanel decryptPanel() {
        JPanel p = newPanel();
        int r = 0;
        addRow(p, r++, "Verschlüsselter Text", scroll(decText, true), 1);
        addRow(p, r++, "Verschlüsselter Schlüssel", scroll(decKey, true), 1);
        addRow(p, r++, "Hash des Klartextes", scroll(decHash, true), 1);
        addRow(p, r++, "Private Key (Pfad)", pathRow(decPriv), 0);

        JButton go = new JButton("Entschlüsseln");
        go.addActionListener(e -> run(this::doDecrypt));
        addRow(p, r++, "", go, 0);

        addRow(p, r++, "Ergebnis", scroll(decResult, false), 1);
        return p;
    }

    private static JPanel newPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        return p;
    }

    private static void addRow(JPanel p, int row, String label, JComponent comp, double weightY) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = row;
        c.insets = new Insets(4, 4, 4, 4);

        c.gridx = 0;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.weightx = 0;
        p.add(new JLabel(label), c);

        c.gridx = 1;
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1;
        c.weighty = weightY;
        p.add(comp, c);
    }

    private static JScrollPane scroll(JTextArea a, boolean editable) {
        a.setLineWrap(true);
        a.setEditable(editable);
        return new JScrollPane(a);
    }

    private JPanel pathRow(JTextField field) {
        JButton choose = new JButton("...");
        choose.addActionListener(e -> {
            JFileChooser fc = new JFileChooser(new File("."));
            if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                field.setText(fc.getSelectedFile().getAbsolutePath());
            }
        });
        JPanel row = new JPanel(new BorderLayout(5, 0));
        row.add(field, BorderLayout.CENTER);
        row.add(choose, BorderLayout.EAST);
        return row;
    }

    /** Führt eine Aktion aus und zeigt Fehler in einem Dialog. */
    private void run(ThrowingRunnable action) {
        try {
            action.run();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.toString(), "Fehler", JOptionPane.ERROR_MESSAGE);
        }
    }

    private interface ThrowingRunnable {
        void run() throws Exception;
    }

    // ================= Aktionen =================

    private void doGenerateKeys() throws Exception {
        String name = keyName.getText().trim();
        if (name.isEmpty()) {
            throw new IllegalArgumentException("Bitte einen Namen eingeben.");
        }
        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);
        KeyPair pair = gen.generateKeyPair();

        File pub = new File(name + "_public.pem");
        File priv = new File(name + "_private.pem");
        writePem(pub, "PUBLIC KEY", pair.getPublic().getEncoded());
        writePem(priv, "PRIVATE KEY", pair.getPrivate().getEncoded());

        keyStatus.setText("Erstellt in: " + pub.getAbsoluteFile().getParent());
        encPub.setText(pub.getAbsolutePath());
        decPriv.setText(priv.getAbsolutePath());
    }

    private void doEncrypt() throws Exception {
        byte[] plaintext = encPlain.getText().getBytes("UTF-8");

        byte[] aesKey;
        String k = encKey.getText().trim();
        if (k.isEmpty()) {
            aesKey = randomKey();
            encKey.setText(b64(aesKey));
        } else {
            aesKey = Base64.getDecoder().decode(k);
        }
        if (aesKey.length != 16 && aesKey.length != 24 && aesKey.length != 32) {
            throw new IllegalArgumentException("Der AES-Schlüssel muss 16, 24 oder 32 Bytes lang sein (Base64).");
        }

        // 1) AES-CBC, zufälliger IV wird vorangestellt
        byte[] iv = new byte[16];
        new SecureRandom().nextBytes(iv);
        Cipher aes = Cipher.getInstance("AES/CBC/PKCS5Padding");
        aes.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(aesKey, "AES"), new IvParameterSpec(iv));
        byte[] cipher = aes.doFinal(plaintext);
        byte[] ivAndCipher = new byte[iv.length + cipher.length];
        System.arraycopy(iv, 0, ivAndCipher, 0, iv.length);
        System.arraycopy(cipher, 0, ivAndCipher, iv.length, cipher.length);

        // 2) AES-Schlüssel mit RSA verschlüsseln
        Cipher rsa = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
        rsa.init(Cipher.ENCRYPT_MODE, loadPublicKey(encPub.getText().trim()));
        byte[] encKeyBytes = rsa.doFinal(aesKey);

        // 3) Hash vom Klartext
        byte[] hash = MessageDigest.getInstance("SHA-256").digest(plaintext);

        outText.setText(b64(ivAndCipher));
        outKey.setText(b64(encKeyBytes));
        outHash.setText(b64(hash));
    }

    private void doDecrypt() throws Exception {
        byte[] encText = Base64.getDecoder().decode(clean(decText.getText()));
        byte[] encKeyBytes = Base64.getDecoder().decode(clean(decKey.getText()));
        byte[] receivedHash = Base64.getDecoder().decode(clean(decHash.getText()));

        // 1) AES-Schlüssel mit RSA entschlüsseln
        Cipher rsa = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
        rsa.init(Cipher.DECRYPT_MODE, loadPrivateKey(decPriv.getText().trim()));
        byte[] aesKey = rsa.doFinal(encKeyBytes);

        // 2) Text mit AES entschlüsseln (erste 16 Bytes = IV)
        byte[] iv = Arrays.copyOfRange(encText, 0, 16);
        byte[] cipher = Arrays.copyOfRange(encText, 16, encText.length);
        Cipher aes = Cipher.getInstance("AES/CBC/PKCS5Padding");
        aes.init(Cipher.DECRYPT_MODE, new SecretKeySpec(aesKey, "AES"), new IvParameterSpec(iv));
        byte[] plaintext = aes.doFinal(cipher);

        // 3) Integritätsprüfung
        byte[] hash = MessageDigest.getInstance("SHA-256").digest(plaintext);
        if (MessageDigest.isEqual(hash, receivedHash)) {
            decResult.setText(new String(plaintext, "UTF-8"));
        } else {
            decResult.setText("FEHLER: Hash stimmt nicht überein, die Daten wurden verändert!");
        }
    }

    // ================= Hilfsmethoden =================

    private static String clean(String s) {
        return s.replaceAll("\\s", "");
    }

    private static String b64(byte[] data) {
        return Base64.getEncoder().encodeToString(data);
    }

    private static byte[] randomKey() {
        byte[] k = new byte[16];
        new SecureRandom().nextBytes(k);
        return k;
    }

    private static byte[] readPem(String path) throws Exception {
        String pem = new String(Files.readAllBytes(Paths.get(path)));
        if (pem.contains("OPENSSH") || pem.contains("RSA PRIVATE KEY") || pem.startsWith("ssh-rsa")) {
            throw new IllegalArgumentException(
                    "Der Key hat das ssh-keygen-Format. Bitte konvertieren (PKCS8) oder im Tab 'Schlüssel' neu erzeugen.");
        }
        return Base64.getDecoder().decode(pem.replaceAll("-----[A-Z ]+-----", "").replaceAll("\\s", ""));
    }

    private static PublicKey loadPublicKey(String path) throws Exception {
        return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(readPem(path)));
    }

    private static PrivateKey loadPrivateKey(String path) throws Exception {
        return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(readPem(path)));
    }

    private static void writePem(File file, String type, byte[] der) throws Exception {
        String body = Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(der);
        String pem = "-----BEGIN " + type + "-----\n" + body + "\n-----END " + type + "-----\n";
        Files.write(file.toPath(), pem.getBytes());
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new CryptoGUI().setVisible(true));
    }
}