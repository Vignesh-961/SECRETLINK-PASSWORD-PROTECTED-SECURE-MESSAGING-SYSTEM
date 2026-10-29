
import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.AEADBadTagException;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

public class SecretLink extends JFrame {

    private static final String PREFIX = "SL1:";
    private static final int SALT_LENGTH = 16;
    private static final int NONCE_LENGTH = 12;
    private static final int TAG_LENGTH_BITS = 128;
    private static final int KEY_LENGTH_BITS = 256;
    private static final int ITERATIONS = 600_000;

    private static final SecureRandom RANDOM = new SecureRandom();

    private final JTextArea messageArea = new JTextArea();
    private final JTextArea encryptedArea = new JTextArea();
    private final JTextArea encryptedInput = new JTextArea();
    private final JTextArea decryptedOutput = new JTextArea();

    private final JPasswordField encryptPassword =
            new JPasswordField(20);
    private final JPasswordField decryptPassword =
            new JPasswordField(20);

    private final JLabel status = new JLabel("Ready.");

    public SecretLink() {
        setTitle("SecretLink - Secure Messaging");
        setSize(850, 650);
        setMinimumSize(new Dimension(650, 500));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(new EmptyBorder(15, 15, 12, 15));

        JLabel heading = new JLabel("SecretLink");
        heading.setFont(new Font("SansSerif", Font.BOLD, 28));

        JLabel subtitle = new JLabel(
                "Password-Protected Secure Message System");
        subtitle.setForeground(new Color(80, 90, 110));

        JPanel header = new JPanel(new GridLayout(2, 1, 3, 3));
        header.add(heading);
        header.add(subtitle);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Encrypt Message", createEncryptPanel());
        tabs.addTab("Decrypt Message", createDecryptPanel());

        root.add(header, BorderLayout.NORTH);
        root.add(tabs, BorderLayout.CENTER);
        root.add(status, BorderLayout.SOUTH);

        setContentPane(root);
    }

    private JPanel createEncryptPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(new EmptyBorder(15, 5, 5, 5));

        JPanel passwordPanel =
                new JPanel(new FlowLayout(FlowLayout.LEFT));
        passwordPanel.add(new JLabel("Password:"));
        passwordPanel.add(encryptPassword);

        configureArea(messageArea, true);
        configureArea(encryptedArea, false);

        JPanel inputPanel = new JPanel(new BorderLayout(5, 5));
        inputPanel.add(
                new JLabel("Enter your secret message:"),
                BorderLayout.NORTH);
        inputPanel.add(
                new JScrollPane(messageArea), BorderLayout.CENTER);

        JPanel outputPanel = new JPanel(new BorderLayout(5, 5));
        outputPanel.add(
                new JLabel("Encrypted message (copy to share):"),
                BorderLayout.NORTH);
        outputPanel.add(
                new JScrollPane(encryptedArea), BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(
                JSplitPane.VERTICAL_SPLIT, inputPanel, outputPanel);
        split.setResizeWeight(0.5);
        split.setDividerLocation(200);

        JButton encryptButton = new JButton("Encrypt Message");
        JButton copyButton = new JButton("Copy Ciphertext");
        JButton clearButton = new JButton("Clear");

        encryptButton.addActionListener(e -> encryptAction());
        copyButton.addActionListener(e ->
                copyText(encryptedArea.getText()));

        clearButton.addActionListener(e -> {
            messageArea.setText("");
            encryptedArea.setText("");
            encryptPassword.setText("");
            setStatus("Encryption fields cleared.");
        });

        JPanel buttons = new JPanel();
        buttons.add(encryptButton);
        buttons.add(copyButton);
        buttons.add(clearButton);

        panel.add(passwordPanel, BorderLayout.NORTH);
        panel.add(split, BorderLayout.CENTER);
        panel.add(buttons, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createDecryptPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(new EmptyBorder(15, 5, 5, 5));

        JPanel passwordPanel =
                new JPanel(new FlowLayout(FlowLayout.LEFT));
        passwordPanel.add(new JLabel("Password:"));
        passwordPanel.add(decryptPassword);

        configureArea(encryptedInput, true);
        configureArea(decryptedOutput, false);

        JPanel inputPanel = new JPanel(new BorderLayout(5, 5));
        inputPanel.add(
                new JLabel("Paste encrypted message:"),
                BorderLayout.NORTH);
        inputPanel.add(
                new JScrollPane(encryptedInput), BorderLayout.CENTER);

        JPanel outputPanel = new JPanel(new BorderLayout(5, 5));
        outputPanel.add(
                new JLabel("Recovered original message:"),
                BorderLayout.NORTH);
        outputPanel.add(
                new JScrollPane(decryptedOutput), BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(
                JSplitPane.VERTICAL_SPLIT, inputPanel, outputPanel);
        split.setResizeWeight(0.5);
        split.setDividerLocation(200);

        JButton decryptButton = new JButton("Decrypt Message");
        JButton tamperButton = new JButton("Simulate Tampering");
        JButton clearButton = new JButton("Clear");

        decryptButton.addActionListener(e -> decryptAction());
        tamperButton.addActionListener(e -> tamperAction());

        clearButton.addActionListener(e -> {
            encryptedInput.setText("");
            decryptedOutput.setText("");
            decryptPassword.setText("");
            setStatus("Decryption fields cleared.");
        });

        JPanel buttons = new JPanel();
        buttons.add(decryptButton);
        buttons.add(tamperButton);
        buttons.add(clearButton);

        panel.add(passwordPanel, BorderLayout.NORTH);
        panel.add(split, BorderLayout.CENTER);
        panel.add(buttons, BorderLayout.SOUTH);

        return panel;
    }

    private static void configureArea(
            JTextArea area, boolean editable) {
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setEditable(editable);
        area.setFont(new Font("Monospaced", Font.PLAIN, 13));
        area.setMargin(new Insets(8, 8, 8, 8));
    }

    private void encryptAction() {
        char[] password = encryptPassword.getPassword();

        try {
            String message = messageArea.getText();

            if (message.isEmpty()) {
                showError("Please enter a message.");
                return;
            }

            if (password.length == 0) {
                showError("Please enter a password.");
                return;
            }

            String result = encryptMessage(message, password);
            encryptedArea.setText(result);

            setStatus("Encryption successful.");
        } catch (Exception ex) {
            showError("Encryption failed: " + ex.getMessage());
        } finally {
            Arrays.fill(password, '\0');
        }
    }

    private void decryptAction() {
        char[] password = decryptPassword.getPassword();

        try {
            String encoded = encryptedInput.getText().trim();

            if (encoded.isEmpty()) {
                showError("Please paste the encrypted message.");
                return;
            }

            if (password.length == 0) {
                showError("Please enter the password.");
                return;
            }

            String result = decryptMessage(encoded, password);
            decryptedOutput.setText(result);
            setStatus("Message authenticated and decrypted.");
        } catch (Exception ex) {
            decryptedOutput.setText("");
            showError(
                    "Decryption failed. Check the password or ciphertext.");
        } finally {
            Arrays.fill(password, '\0');
        }
    }

    private static String encryptMessage(
            String message, char[] password) throws Exception {

        byte[] salt = new byte[SALT_LENGTH];
        byte[] nonce = new byte[NONCE_LENGTH];

        RANDOM.nextBytes(salt);
        RANDOM.nextBytes(nonce);

        byte[] keyBytes = deriveKey(password, salt);

        try {
            SecretKeySpec key = new SecretKeySpec(keyBytes, "AES");

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(
                    Cipher.ENCRYPT_MODE,
                    key,
                    new GCMParameterSpec(TAG_LENGTH_BITS, nonce));

            byte[] ciphertext = cipher.doFinal(
                    message.getBytes(StandardCharsets.UTF_8));

            // Format: salt || nonce || ciphertext || GCM tag
            byte[] payload = new byte[
                    salt.length + nonce.length + ciphertext.length];

            System.arraycopy(salt, 0, payload, 0, salt.length);
            System.arraycopy(
                    nonce, 0, payload, salt.length, nonce.length);
            System.arraycopy(
                    ciphertext, 0, payload,
                    salt.length + nonce.length, ciphertext.length);

            return PREFIX + Base64.getEncoder()
                    .encodeToString(payload);
        } finally {
            Arrays.fill(keyBytes, (byte) 0);
        }
    }

    private static String decryptMessage(
            String encoded, char[] password) throws Exception {

        if (!encoded.startsWith(PREFIX)) {
            throw new IllegalArgumentException("Invalid message format.");
        }

        byte[] payload = Base64.getDecoder().decode(
                encoded.substring(PREFIX.length()));

        int minimumLength = SALT_LENGTH + NONCE_LENGTH + 16;

        if (payload.length < minimumLength) {
            throw new IllegalArgumentException("Payload is too short.");
        }

        byte[] salt = Arrays.copyOfRange(payload, 0, SALT_LENGTH);
        byte[] nonce = Arrays.copyOfRange(
                payload, SALT_LENGTH, SALT_LENGTH + NONCE_LENGTH);
        byte[] ciphertext = Arrays.copyOfRange(
                payload, SALT_LENGTH + NONCE_LENGTH, payload.length);

        byte[] keyBytes = deriveKey(password, salt);

        try {
            SecretKeySpec key = new SecretKeySpec(keyBytes, "AES");

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(
                    Cipher.DECRYPT_MODE,
                    key,
                    new GCMParameterSpec(TAG_LENGTH_BITS, nonce));

            // Authentication must succeed before plaintext is returned.
            byte[] plaintext = cipher.doFinal(ciphertext);

            return new String(plaintext, StandardCharsets.UTF_8);
        } finally {
            Arrays.fill(keyBytes, (byte) 0);
        }
    }

    private static byte[] deriveKey(
            char[] password, byte[] salt) throws Exception {

        PBEKeySpec spec = new PBEKeySpec(
                password, salt, 600_000, 256);

        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance(
                    "PBKDF2WithHmacSHA256");

            return factory.generateSecret(spec).getEncoded();
        } finally {
            spec.clearPassword();
        }
    }

    private void tamperAction() {
        try {
            String encoded = encryptedInput.getText().trim();

            if (!encoded.startsWith(PREFIX)) {
                showError("Paste a valid SecretLink ciphertext first.");
                return;
            }

            byte[] payload = Base64.getDecoder().decode(
                    encoded.substring(PREFIX.length()));

            if (payload.length < SALT_LENGTH + NONCE_LENGTH + 16) {
                showError("Encrypted payload is too short.");
                return;
            }

            // Alter one bit in the authentication tag.
            payload[payload.length - 1] ^= 1;

            encryptedInput.setText(
                    PREFIX + Base64.getEncoder()
                            .encodeToString(payload));

            decryptedOutput.setText("");
            setStatus(
                    "Ciphertext modified. Click Decrypt to test integrity.");
        } catch (Exception ex) {
            showError("Could not modify the ciphertext.");
        }
    }

    private void copyText(String text) {
        if (text == null || text.isEmpty()) {
            showError("There is no ciphertext to copy.");
            return;
        }

        Toolkit.getDefaultToolkit().getSystemClipboard()
                .setContents(new StringSelection(text), null);

        setStatus("Ciphertext copied to clipboard.");
    }

    private void showError(String message) {
        status.setText("Error: " + message);

        JOptionPane.showMessageDialog(
                this,
                message,
                "SecretLink",
                JOptionPane.WARNING_MESSAGE);
    }

    private void setStatus(String message) {
        status.setText(message);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(
                        UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
                // Use the default Swing look and feel.
            }

            new SecretLink().setVisible(true);
        });
    }
}
