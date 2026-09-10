import java.awt.*;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.*;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.ArrayList;
import javax.swing.JPanel;
import java.util.List;
import java.util.stream.Collectors;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

// ==========================================
// 1. CONSOLE COLOR UTILITY
// ==========================================
class ConsoleColors {
    public static final String RESET = "\u001B[0m";
    public static final String GREEN = "\u001B[32m";
    public static final String CYAN = "\u001B[36m";
    public static final String RED = "\u001B[31m";
    public static final String YELLOW = "\u001B[33m";
}

// ==========================================
// 1A. ADMIN AUTHENTICATION / PERMISSION GATE
// ==========================================
class AuthManager {
    // Default credentials: username "admin", password "Admin@123"
    // Change ADMIN_PASSWORD_HASH by generating a new SHA-256 hash of your chosen password.
    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PASSWORD_HASH = sha256("Admin@123");

    private static final int MAX_ATTEMPTS = 3;

    /** Hashes a plaintext string with SHA-256 and returns it as a hex string. */
    private static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is always available on standard JVMs; this should never happen.
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Prompts for admin username and password (up to MAX_ATTEMPTS tries).
     * Returns true only if the credentials match — nothing that depends on
     * the dataset (loading students.csv, showing the menu, etc.) should run
     * unless this returns true.
     */
    public static boolean authenticate(Scanner sc) {
        System.out.println(ConsoleColors.CYAN + "==========================================");
        System.out.println("        ADMIN LOGIN REQUIRED");
        System.out.println("==========================================" + ConsoleColors.RESET);

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            System.out.print("Username: ");
            String username = sc.nextLine().trim();

            String password = readPasswordViaDialog();

            if (username.equals(ADMIN_USERNAME) && sha256(password).equals(ADMIN_PASSWORD_HASH)) {
                System.out.println(ConsoleColors.GREEN + "Login successful. Loading dataset...\n" + ConsoleColors.RESET);
                return true;
            }

            int remaining = MAX_ATTEMPTS - attempt;
            if (remaining > 0) {
                System.out.println(ConsoleColors.RED + "Invalid credentials. "
                        + remaining + " attempt(s) remaining." + ConsoleColors.RESET);
            }
        }

        System.out.println(ConsoleColors.RED + "Too many failed login attempts. Access denied." + ConsoleColors.RESET);
        return false;
    }

    /**
     * Opens a small Swing dialog with a masked JPasswordField so the
     * password shows as bullet characters instead of plain text.
     * This is used instead of java.io.Console because System.console()
     * returns null inside most IDE run consoles (IntelliJ, Eclipse, VS Code),
     * which would otherwise silently fall back to unmasked input and can
     * appear to "not accept typing" depending on the terminal.
     */
    private static String readPasswordViaDialog() {
        final char[][] result = {null};
        try {
            SwingUtilities.invokeAndWait(() -> {
                JPasswordField passwordField = new JPasswordField(20);
                JPanel panel = new JPanel(new BorderLayout(5, 5));
                panel.add(new JLabel("Enter Admin Password:"), BorderLayout.NORTH);
                panel.add(passwordField, BorderLayout.CENTER);

                // Ensure the dialog gets focus and is on top of other windows.
                JFrame owner = new JFrame();
                owner.setAlwaysOnTop(true);
                owner.setUndecorated(true);
                owner.setLocationRelativeTo(null);
                owner.setVisible(true);
                owner.setVisible(false);

                int option = JOptionPane.showConfirmDialog(
                        owner, panel, "Login", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

                if (option == JOptionPane.OK_OPTION) {
                    result[0] = passwordField.getPassword();
                } else {
                    result[0] = new char[0];
                }
                owner.dispose();
            });
        } catch (Exception e) {
            System.out.println(ConsoleColors.RED + "Could not open password dialog: " + e.getMessage() + ConsoleColors.RESET);
            return "";
        }
        return new String(result[0]).trim();
    }
}

// ==========================================
// 2. INTERFACE & CORE DOMAIN MODELS
// ==========================================
interface IDPrintable {
    void printIDCard();
}

abstract class Person {
    protected int id;
    protected String name;
    protected String department;
    protected String dob;
    protected String phone;
    protected String address;
    protected String photoPath;

    public Person(int id, String name, String department, String dob, String phone, String address, String photoPath) {
        this.id = id;
        this.name = name;
        this.department = department;
        this.dob = (dob == null || dob.isBlank()) ? "N/A" : dob;
        this.phone = (phone == null || phone.isBlank()) ? "N/A" : phone;
        this.address = (address == null || address.isBlank()) ? "N/A" : address;
        this.photoPath = (photoPath == null || photoPath.isBlank()) ? "None" : photoPath;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getDepartment() { return department; }
}

class Student extends Person implements IDPrintable {
    protected String year;
    protected String bloodGroup;
    protected String gender;
    protected String email;
    protected String city;
    protected String state;
    protected String pincode;
    protected String parentName;
    protected String parentPhone;
    protected int admissionYear;
    protected String section;
    protected int semester;
    protected String collegeName;
    protected String academicStatus;

    public Student(int id, String name, String department, String year, String bloodGroup,
                   String dob, String phone, String address, String photoPath) {
        super(id, name, department, dob, phone, address, photoPath);
        this.year = year;
        this.bloodGroup = bloodGroup;
        this.gender = "N/A";
        this.email = "N/A";
        this.city = "N/A";
        this.state = "N/A";
        this.pincode = "N/A";
        this.parentName = "N/A";
        this.parentPhone = "N/A";
        this.admissionYear = 2026;
        this.section = "A";
        this.semester = 1;
        this.collegeName = "Chennai Institute of Technology";
        this.academicStatus = "Active";
    }

    public Student(String[] data) {
        super(Integer.parseInt(data[0].trim()),
              data[1].replace("\"", "").trim(),
              data[2].trim(),
              data[5].trim(),
              data[7].trim(),
              data[9].replace("\"", "").trim(),
              "None");
        this.year = data[3].trim();
        this.gender = data[4].trim();
        this.bloodGroup = data[6].trim();
        this.email = data[8].trim();
        this.city = data[10].trim();
        this.state = data[11].trim();
        this.pincode = data[12].trim();
        this.parentName = data[13].replace("\"", "").trim();
        this.parentPhone = data[14].trim();
        this.admissionYear = Integer.parseInt(data[15].trim());
        this.section = data[16].trim();
        this.semester = Integer.parseInt(data[17].trim());
        this.collegeName = data[18].replace("\"", "").trim();
        this.academicStatus = data[19].trim();
    }

    @Override
    public void printIDCard() {
        CardGUIHelper.displayCardGUI(this);
    }

    public void displayFullProfile() {
        System.out.println("\n========== STUDENT PROFILE ==========");
        System.out.println("Student ID     : " + id);
        System.out.println("Name           : " + name);
        System.out.println("Department     : " + department);
        System.out.println("Year           : " + year);
        System.out.println("Section        : " + section);
        System.out.println("Semester       : " + semester);
        System.out.println("Gender         : " + gender);
        System.out.println("Date of Birth  : " + dob);
        System.out.println("Blood Group    : " + bloodGroup);
        System.out.println("Phone          : " + phone);
        System.out.println("Email          : " + email);
        System.out.println("Address        : " + address + ", " + city + ", " + state + " - " + pincode);
        System.out.println("Parent/Guardian: " + parentName + " (" + parentPhone + ")");
        System.out.println("Admission Year : " + admissionYear);
        System.out.println("College Name   : " + collegeName);
        System.out.println("Academic Status: " + academicStatus);
        System.out.println("=====================================");
    }

    /** Serializes this student back into the same 20-column CSV row format used by DatasetGenerator. */
    public String toCsvRow() {
        return String.format("%d,\"%s\",%s,%s,%s,%s,%s,%s,%s,\"%s\",%s,%s,%s,\"%s\",%s,%d,%s,%d,\"%s\",%s",
                id, name, department, year, gender, dob, bloodGroup, phone, email, address,
                city, state, pincode, parentName, parentPhone, admissionYear, section, semester,
                collegeName, academicStatus);
    }
}

class Faculty extends Person implements IDPrintable {
    protected String designation;

    public Faculty(int id, String name, String department, String designation,
                   String dob, String phone, String address, String photoPath) {
        super(id, name, department, dob, phone, address, photoPath);
        this.designation = designation;
    }

    @Override
    public void printIDCard() {
        CardGUIHelper.displayCardGUI(this);
    }
}

// ==========================================
// 2A. DISCIPLINARY CASE MANAGEMENT
// ==========================================
class DisciplinaryCase {
    private final String caseId;
    private final int personId;
    private final String personName;
    private final String personType;
    private final String caseType;
    private final String description;
    private final String date;
    private final String reportedBy;
    private String status;
    private String actionTaken;

    public DisciplinaryCase(String caseId, int personId, String personName, String personType,
                            String caseType, String description, String date,
                            String reportedBy, String status, String actionTaken) {
        this.caseId = caseId;
        this.personId = personId;
        this.personName = personName;
        this.personType = personType;
        this.caseType = caseType;
        this.description = description;
        this.date = date;
        this.reportedBy = reportedBy;
        this.status = status;
        this.actionTaken = actionTaken;
    }

    public String getCaseId() { return caseId; }
    public int getPersonId() { return personId; }
    public String getPersonName() { return personName; }
    public String getPersonType() { return personType; }
    public String getCaseType() { return caseType; }
    public String getDescription() { return description; }
    public String getDate() { return date; }
    public String getReportedBy() { return reportedBy; }
    public String getStatus() { return status; }
    public String getActionTaken() { return actionTaken; }

    public void setStatus(String status) { this.status = status; }
    public void setActionTaken(String actionTaken) { this.actionTaken = actionTaken; }

    public void display() {
        System.out.println("\n----------------------------------------");
        System.out.println("Case ID       : " + caseId);
        System.out.println("Person ID     : " + personId);
        System.out.println("Person Name   : " + personName);
        System.out.println("Person Type   : " + personType);
        System.out.println("Case Type     : " + caseType);
        System.out.println("Description   : " + description);
        System.out.println("Date          : " + date);
        System.out.println("Reported By   : " + reportedBy);
        System.out.println("Status        : " + status);
        System.out.println("Action Taken  : " + actionTaken);
        System.out.println("----------------------------------------");
    }
}

class CaseManager {
    private final ArrayList<DisciplinaryCase> cases = new ArrayList<>();

    public void addCase(DisciplinaryCase c) {
        cases.add(c);
    }

    public ArrayList<DisciplinaryCase> getCases() {
        return cases;
    }

    public boolean hasActiveCase(int personId) {
        for (DisciplinaryCase c : cases) {
            if (c.getPersonId() == personId &&
                !c.getStatus().equalsIgnoreCase("Closed") &&
                !c.getStatus().equalsIgnoreCase("Dismissed")) {
                return true;
            }
        }
        return false;
    }

    public void showCases(int personId) {
        boolean found = false;
        for (DisciplinaryCase c : cases) {
            if (c.getPersonId() == personId) {
                c.display();
                found = true;
            }
        }
        if (!found) {
            System.out.println(ConsoleColors.GREEN + "No case records found." + ConsoleColors.RESET);
        }
    }

    public void showAllCases() {
        if (cases.isEmpty()) {
            System.out.println(ConsoleColors.GREEN + "No disciplinary cases available." + ConsoleColors.RESET);
            return;
        }
        for (DisciplinaryCase c : cases) c.display();
    }

    public DisciplinaryCase findCase(String caseId) {
        for (DisciplinaryCase c : cases) {
            if (c.getCaseId().equalsIgnoreCase(caseId)) return c;
        }
        return null;
    }

    public void updateCase(Scanner sc) {
        System.out.print("Enter Case ID: ");
        String id = sc.nextLine().trim();
        DisciplinaryCase c = findCase(id);

        if (c == null) {
            System.out.println(ConsoleColors.RED + "Case not found." + ConsoleColors.RESET);
            return;
        }

        System.out.print("New Status (Under Review/Closed/Dismissed): ");
        c.setStatus(sc.nextLine().trim());

        System.out.print("Action Taken: ");
        c.setActionTaken(sc.nextLine().trim());

        System.out.println(ConsoleColors.GREEN + "Case updated successfully." + ConsoleColors.RESET);
    }

    /**
     * Search cases by numeric ID (exact) or by name (partial, case-insensitive).
     * Works even if the person's ID card record was later deleted, since the
     * name is stored directly on the case record.
     */
    public List<DisciplinaryCase> searchCases(String query) {
        String q = query.trim();
        List<DisciplinaryCase> results = new ArrayList<>();

        // Try as numeric ID first
        try {
            int id = Integer.parseInt(q);
            for (DisciplinaryCase c : cases) {
                if (c.getPersonId() == id) results.add(c);
            }
            if (!results.isEmpty()) return results;
        } catch (NumberFormatException ignored) {
            // not numeric, fall through to name search
        }

        // Name search (partial match, case-insensitive)
        String qLower = q.toLowerCase();
        for (DisciplinaryCase c : cases) {
            if (c.getPersonName() != null && c.getPersonName().toLowerCase().contains(qLower)) {
                results.add(c);
            }
        }
        return results;
    }

    /**
     * Prints a grouped summary: does this person (by name or ID) have any
     * case history, and how many of those cases are currently active.
     */
    public void searchAndReport(String query) {
        List<DisciplinaryCase> results = searchCases(query);

        if (results.isEmpty()) {
            System.out.println(ConsoleColors.GREEN + "No disciplinary case records found for \"" + query + "\"." + ConsoleColors.RESET);
            return;
        }

        Map<Integer, List<DisciplinaryCase>> grouped = new LinkedHashMap<>();
        for (DisciplinaryCase c : results) {
            grouped.computeIfAbsent(c.getPersonId(), k -> new ArrayList<>()).add(c);
        }

        for (Map.Entry<Integer, List<DisciplinaryCase>> entry : grouped.entrySet()) {
            List<DisciplinaryCase> personCases = entry.getValue();
            String name = personCases.get(0).getPersonName();
            long activeCount = personCases.stream()
                    .filter(c -> !c.getStatus().equalsIgnoreCase("Closed") && !c.getStatus().equalsIgnoreCase("Dismissed"))
                    .count();

            System.out.println("\n" + ConsoleColors.CYAN + "==== " + name + " (ID: " + entry.getKey() + ") ====" + ConsoleColors.RESET);
            System.out.println("Total Cases  : " + personCases.size());
            if (activeCount > 0) {
                System.out.println(ConsoleColors.RED + "Active Cases : " + activeCount + ConsoleColors.RESET);
            } else {
                System.out.println(ConsoleColors.GREEN + "Active Cases : 0 (all closed/dismissed)" + ConsoleColors.RESET);
            }
            for (DisciplinaryCase c : personCases) {
                c.display();
            }
        }
    }
}

// ==========================================
// 3. AES ENCRYPTION UTILITY
// ==========================================
class AESEncryptionUtil {
    private static final String SECRET_KEY = "MySecretKey12345";
    private static final String ALGO = "AES";

    public static String encrypt(String data) {
        try {
            SecretKeySpec key = new SecretKeySpec(SECRET_KEY.getBytes(StandardCharsets.UTF_8), ALGO);
            Cipher cipher = Cipher.getInstance(ALGO);
            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] encrypted = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            return data;
        }
    }

    public static String decrypt(String encryptedData) {
        try {
            SecretKeySpec key = new SecretKeySpec(SECRET_KEY.getBytes(StandardCharsets.UTF_8), ALGO);
            Cipher cipher = Cipher.getInstance(ALGO);
            cipher.init(Cipher.DECRYPT_MODE, key);
            byte[] decoded = Base64.getDecoder().decode(encryptedData);
            byte[] decrypted = cipher.doFinal(decoded);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "DECRYPTION_FAILED";
        }
    }
}

// ==========================================
// 4. PHOTO QUALITY CHECKER
// ==========================================
class PhotoQualityChecker {
    public static double calculateSharpness(BufferedImage img) {
        BufferedImage gray = toGrayscale(img);
        int width = gray.getWidth();
        int height = gray.getHeight();
        double sum = 0;
        double sumSq = 0;
        int count = 0;

        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
                int center = gray.getRGB(x, y) & 0xFF;
                int left   = gray.getRGB(x - 1, y) & 0xFF;
                int right  = gray.getRGB(x + 1, y) & 0xFF;
                int up     = gray.getRGB(x, y - 1) & 0xFF;
                int down   = gray.getRGB(x, y + 1) & 0xFF;

                double laplacian = (4 * center) - left - right - up - down;
                sum += laplacian;
                sumSq += laplacian * laplacian;
                count++;
            }
        }
        if (count == 0) return 0;
        double mean = sum / count;
        return (sumSq / count) - (mean * mean);
    }

    private static BufferedImage toGrayscale(BufferedImage img) {
        BufferedImage gray = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g = gray.createGraphics();
        g.drawImage(img, 0, 0, null);
        g.dispose();
        return gray;
    }
}

// ==========================================
// 5. DUPLICATE PHOTO DETECTOR
// ==========================================
class DuplicatePhotoChecker {
    public static long computeHash(BufferedImage img) {
        BufferedImage resized = new BufferedImage(8, 8, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g = resized.createGraphics();
        g.drawImage(img, 0, 0, 8, 8, null);
        g.dispose();

        int[] pixels = new int[64];
        int sum = 0;
        int idx = 0;
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                int val = resized.getRGB(x, y) & 0xFF;
                pixels[idx++] = val;
                sum += val;
            }
        }
        int avg = sum / 64;

        long hash = 0L;
        for (int i = 0; i < 64; i++) {
            hash <<= 1;
            if (pixels[i] >= avg) hash |= 1;
        }
        return hash;
    }

    public static int hammingDistance(long hash1, long hash2) {
        return Long.bitCount(hash1 ^ hash2);
    }

    public static boolean isDuplicate(BufferedImage newImg, ArrayList<Person> existingPeople, int threshold) {
        long newHash = computeHash(newImg);
        for (Person p : existingPeople) {
            if (p.photoPath != null && !p.photoPath.equalsIgnoreCase("None")) {
                File f = new File(p.photoPath);
                if (f.exists()) {
                    try {
                        BufferedImage existingImg = ImageIO.read(f);
                        if (existingImg != null) {
                            long existingHash = computeHash(existingImg);
                            if (hammingDistance(newHash, existingHash) <= threshold) {
                                return true;
                            }
                        }
                    } catch (IOException ignored) {}
                }
            }
        }
        return false;
    }
}

// ==========================================
// 6. SCANNABLE QR CODE GENERATOR
// ==========================================
class QRCodeGenerator {
    static Map<Integer, String> encryptedPayloads = new HashMap<>();

    public static BufferedImage generateQRCode(String data, int size) {
        try {
            String encodedData = URLEncoder.encode(data, StandardCharsets.UTF_8.name());
            String urlString = "https://api.qrserver.com/v1/create-qr-code/?size="
                    + size + "x" + size + "&data=" + encodedData;

            BufferedImage image = ImageIO.read(URI.create(urlString).toURL());
            if (image != null) return image;
        } catch (Exception e) {}

        BufferedImage fallback = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = fallback.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, size, size);
        g.setColor(Color.BLACK);
        g.drawRect(2, 2, size - 5, size - 5);
        g.setFont(new Font("SansSerif", Font.BOLD, 10));
        g.drawString("QR Offline", 12, size / 2);
        g.dispose();
        return fallback;
    }

    public static BufferedImage generateSecureQRCode(Person person, int size) {
        String rawData = "ID:" + person.id + "|NAME:" + person.name + "|DEPT:" + person.department
                + "|PHONE:" + person.phone + "|TOKEN:" + UUID.randomUUID();
        String encrypted = AESEncryptionUtil.encrypt(rawData);
        encryptedPayloads.put(person.id, encrypted);
        return generateQRCode(encrypted, size);
    }

    public static String verifyById(int id) {
        String encrypted = encryptedPayloads.get(id);
        if (encrypted == null) {
            return ConsoleColors.RED + "No QR record generated yet for ID " + id + ". Display or print ID card first." + ConsoleColors.RESET;
        }
        String decrypted = AESEncryptionUtil.decrypt(encrypted);
        if (decrypted.equals("DECRYPTION_FAILED")) {
            return ConsoleColors.RED + "QR verification FAILED - data corrupted or tampered." + ConsoleColors.RESET;
        }
        return ConsoleColors.GREEN + "VERIFIED (valid signed ID)\n" + ConsoleColors.RESET + decrypted.replace("|", "\n");
    }
}

// ==========================================
// 7. GUI & CARD GENERATOR HELPER
// ==========================================
class CardGUIHelper {
    public static JPanel createCardPanel(Person person) {
        int width = 500;
        int height = 300;

        JPanel card = new JPanel(new BorderLayout());
        card.setPreferredSize(new Dimension(width, height));
        card.setSize(width, height);
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200), 2));

        boolean isStudent = person instanceof Student;
        Color headerColor = isStudent ? new Color(15, 82, 186) : new Color(180, 100, 20);
        String headerTitle = isStudent ? "STUDENT IDENTIFICATION CARD" : "FACULTY IDENTIFICATION CARD";

        JPanel headerPanel = new JPanel();
        headerPanel.setBackground(headerColor);
        JLabel headerLabel = new JLabel(headerTitle);
        headerLabel.setForeground(Color.WHITE);
        headerLabel.setFont(new Font("SansSerif", Font.BOLD, 15));
        headerPanel.add(headerLabel);
        card.add(headerPanel, BorderLayout.NORTH);

        JPanel bodyPanel = new JPanel(new BorderLayout(10, 0));
        bodyPanel.setBackground(Color.WHITE);
        bodyPanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 5, 15));

        JLabel photoLabel = new JLabel();
        photoLabel.setPreferredSize(new Dimension(100, 130));
        boolean photoLoaded = false;

        if (person.photoPath != null && !person.photoPath.equalsIgnoreCase("None")) {
            File file = new File(person.photoPath);
            if (file.exists()) {
                ImageIcon icon = new ImageIcon(new ImageIcon(person.photoPath).getImage().getScaledInstance(100, 130, Image.SCALE_SMOOTH));
                photoLabel.setIcon(icon);
                photoLoaded = true;
            }
        }
        if (!photoLoaded) {
            photoLabel.setOpaque(true);
            photoLabel.setBackground(new Color(235, 235, 235));
            photoLabel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
            photoLabel.setText("<html><center>NO<br>PHOTO</center></html>");
            photoLabel.setHorizontalAlignment(SwingConstants.CENTER);
        }
        bodyPanel.add(photoLabel, BorderLayout.WEST);

        StringBuilder details = new StringBuilder("<html><div style='font-family:SansSerif; font-size:9px; line-height:1.2;'>");
        details.append("<b>ID:</b> ").append(person.id).append("<br>");
        details.append("<b>Name:</b> ").append(person.name.toUpperCase()).append("<br>");
        details.append("<b>Dept:</b> ").append(person.department).append("<br>");
        details.append("<b>DOB:</b> ").append(person.dob).append("<br>");
        details.append("<b>Phone:</b> ").append(person.phone).append("<br>");

        if (isStudent) {
            Student s = (Student) person;
            details.append("<b>Year:</b> ").append(s.year).append("<br>");
            details.append("<b>Blood Group:</b> ").append(s.bloodGroup).append("<br>");
        } else {
            Faculty f = (Faculty) person;
            details.append("<b>Designation:</b> ").append(f.designation).append("<br>");
        }
        details.append("<b>Address:</b> ").append(person.address);
        details.append("</div></html>");

        JLabel detailsLabel = new JLabel(details.toString());
        bodyPanel.add(detailsLabel, BorderLayout.CENTER);

        BufferedImage qrImage = QRCodeGenerator.generateSecureQRCode(person, 90);
        JLabel qrLabel = new JLabel(new ImageIcon(qrImage));
        bodyPanel.add(qrLabel, BorderLayout.EAST);

        card.add(bodyPanel, BorderLayout.CENTER);

        JPanel footerPanel = new JPanel(new BorderLayout());
        footerPanel.setBackground(Color.WHITE);
        footerPanel.setBorder(BorderFactory.createEmptyBorder(0, 15, 10, 15));

        JLabel instLabel = new JLabel("Chennai Institute of Technology");
        instLabel.setFont(new Font("SansSerif", Font.ITALIC, 11));
        instLabel.setForeground(Color.DARK_GRAY);
        footerPanel.add(instLabel, BorderLayout.WEST);

        JLabel sigLabel = new JLabel("<html><div style='text-align: right;'>"
                + "<span style='color: #2E7D32; font-size: 14px;'><b>&#10003;</b></span> "
                + "<b style='color: #222222; font-size: 10px;'>Digitally Signed</b><br>"
                + "<span style='color: #666666; font-size: 8px;'>Principal / Authorized Signatory</span>"
                + "</div></html>");
        footerPanel.add(sigLabel, BorderLayout.EAST);

        card.add(footerPanel, BorderLayout.SOUTH);
        return card;
    }

    public static void displayCardGUI(Person person) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("ID Card Display - " + person.name);
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            frame.getContentPane().add(createCardPanel(person));
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}

// ==========================================
// 7A. INTERACTIVE PAGINATED GUI ID CARD BROWSER
// ==========================================
class IDCardGUIBrowser extends JFrame {
    private final List<Person> masterList;
    private List<Person> filteredList;
    private int currentIndex = 0;

    private final JPanel cardHolderPanel = new JPanel(new BorderLayout());
    private final JTextField txtSearchId = new JTextField(6);
    private final JTextField txtSearchName = new JTextField(10);
    private final JComboBox<String> cbDept = new JComboBox<>(new String[]{"ALL", "CSE", "IT", "ECE", "EEE", "MECH", "CIVIL", "AIDS", "AIML"});
    private final JComboBox<String> cbYear = new JComboBox<>(new String[]{"ALL", "1", "2", "3", "4"});
    private final JLabel lblCount = new JLabel();

    public IDCardGUIBrowser(List<Person> people) {
        this.masterList = people;
        this.filteredList = new ArrayList<>(masterList);

        setTitle("ID Card Browser (5,000+ Students)");
        setSize(580, 420);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(5, 5));

        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        filterPanel.add(new JLabel("ID:"));
        filterPanel.add(txtSearchId);
        filterPanel.add(new JLabel("Name:"));
        filterPanel.add(txtSearchName);
        filterPanel.add(new JLabel("Dept:"));
        filterPanel.add(cbDept);
        filterPanel.add(new JLabel("Year:"));
        filterPanel.add(cbYear);

        JButton btnFilter = new JButton("Filter");
        btnFilter.addActionListener(e -> applyFilters());
        filterPanel.add(btnFilter);

        add(filterPanel, BorderLayout.NORTH);
        add(cardHolderPanel, BorderLayout.CENTER);

        JPanel navPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 5));
        JButton btnPrev = new JButton("<< Previous");
        JButton btnNext = new JButton("Next >>");

        btnPrev.addActionListener(e -> {
            if (currentIndex > 0) {
                currentIndex--;
                renderCurrentCard();
            }
        });

        btnNext.addActionListener(e -> {
            if (currentIndex < filteredList.size() - 1) {
                currentIndex++;
                renderCurrentCard();
            }
        });

        navPanel.add(btnPrev);
        navPanel.add(lblCount);
        navPanel.add(btnNext);
        add(navPanel, BorderLayout.SOUTH);

        renderCurrentCard();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void applyFilters() {
        String searchId = txtSearchId.getText().trim();
        String searchName = txtSearchName.getText().trim().toLowerCase();
        String selectedDept = (String) cbDept.getSelectedItem();
        String selectedYear = (String) cbYear.getSelectedItem();

        filteredList = masterList.stream().filter(p -> {
            boolean matchesId = searchId.isEmpty() || String.valueOf(p.id).equals(searchId);
            boolean matchesName = searchName.isEmpty() || p.name.toLowerCase().contains(searchName);
            boolean matchesDept = "ALL".equals(selectedDept) || p.department.equalsIgnoreCase(selectedDept);

            boolean matchesYear = true;
            if (!"ALL".equals(selectedYear)) {
                if (p instanceof Student) {
                    matchesYear = ((Student) p).year.equals(selectedYear);
                } else {
                    matchesYear = false;
                }
            }

            return matchesId && matchesName && matchesDept && matchesYear;
        }).collect(Collectors.toList());

        currentIndex = 0;
        renderCurrentCard();
    }

    private void renderCurrentCard() {
        cardHolderPanel.removeAll();
        if (filteredList.isEmpty()) {
            lblCount.setText("Result: 0 of 0");
            JLabel emptyLabel = new JLabel("No matching records found.", SwingConstants.CENTER);
            cardHolderPanel.add(emptyLabel, BorderLayout.CENTER);
        } else {
            lblCount.setText("Showing " + (currentIndex + 1) + " of " + filteredList.size());
            Person p = filteredList.get(currentIndex);
            cardHolderPanel.add(CardGUIHelper.createCardPanel(p), BorderLayout.CENTER);
        }
        cardHolderPanel.revalidate();
        cardHolderPanel.repaint();
    }
}

// ==========================================
// 8. PNG IMAGE EXPORTER
// ==========================================
class PNGExporter {
    public static void exportAllToPNG(ArrayList<Person> people) {
        int count = 0;
        for (Person p : people) {
            JPanel cardPanel = CardGUIHelper.createCardPanel(p);
            int width = cardPanel.getPreferredSize().width;
            int height = cardPanel.getPreferredSize().height;

            cardPanel.setSize(width, height);
            cardPanel.addNotify();
            cardPanel.validate();

            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = image.createGraphics();

            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            cardPanel.paint(g2d);
            g2d.dispose();

            String filename = "ID_Card_" + p.id + "_" + p.name.replaceAll("\\s+", "_") + ".png";
            File outputFile = new File(filename);
            try {
                ImageIO.write(image, "png", outputFile);
                count++;
            } catch (IOException e) {
                System.out.println(ConsoleColors.RED + "Failed to save PNG for ID " + p.id + ": " + e.getMessage() + ConsoleColors.RESET);
            }
        }
        System.out.println(ConsoleColors.CYAN + "Successfully exported " + count + " ID card(s) to PNG format." + ConsoleColors.RESET);
    }
}

// ==========================================
// 9. NATIVE JAVA PDF EXPORTER
// ==========================================
class PDFExporter {
    /**
     * Exports every ID card directly to a valid, standalone PDF file
     * and automatically opens it in the default system viewer.
     */
    public static void exportAllToPDF(ArrayList<Person> people) {
        if (people == null || people.isEmpty()) {
            System.out.println(ConsoleColors.RED + "No records available to export." + ConsoleColors.RESET);
            return;
        }

        File outputFile = new File("ID_Cards_Export.pdf");
        try {
            ArrayList<BufferedImage> pageImages = new ArrayList<>();
            System.out.println(ConsoleColors.CYAN + "Generating PDF for " + people.size() + " ID card(s)..." + ConsoleColors.RESET);

            for (Person person : people) {
                JPanel panel = CardGUIHelper.createCardPanel(person);
                panel.setSize(500, 300);
                panel.doLayout();
                panel.addNotify();
                panel.validate();
                panel.doLayout();

                BufferedImage image = new BufferedImage(1000, 600, BufferedImage.TYPE_INT_RGB);
                Graphics2D g2 = image.createGraphics();
                g2.setColor(Color.WHITE);
                g2.fillRect(0, 0, image.getWidth(), image.getHeight());

                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);

                g2.scale(2.0, 2.0);
                panel.paint(g2);
                g2.dispose();

                pageImages.add(image);
            }

            PDFWriterHelper.writeImagesAsPdf(pageImages, outputFile);
            System.out.println(ConsoleColors.GREEN + "PDF created successfully! Saved to: " + outputFile.getAbsolutePath() + ConsoleColors.RESET);

            // Auto-open generated PDF file in default PDF viewer
            if (Desktop.isDesktopSupported() && outputFile.exists()) {
                try {
                    Desktop.getDesktop().open(outputFile);
                    System.out.println(ConsoleColors.CYAN + "Opened " + outputFile.getName() + " in your default PDF viewer." + ConsoleColors.RESET);
                } catch (Exception ex) {
                    System.out.println(ConsoleColors.YELLOW + "Note: PDF generated at " + outputFile.getAbsolutePath() + ConsoleColors.RESET);
                }
            }
        } catch (Exception e) {
            System.out.println(ConsoleColors.RED + "Failed to create PDF: " + e.getMessage() + ConsoleColors.RESET);
            e.printStackTrace();
        }
    }
}

// ==========================================
// 9A. DISCIPLINARY CASE PDF EXPORTER
// ==========================================
class DisciplinaryCasePDFExporter {
    public static void exportCasesToPDF(ArrayList<DisciplinaryCase> cases) {
        if (cases == null || cases.isEmpty()) {
            System.out.println(ConsoleColors.RED + "No disciplinary cases available to export." + ConsoleColors.RESET);
            return;
        }

        File outputFile = new File("IDForge_Disciplinary_Cases_Report.pdf");
        try {
            int itemsPerPage = 3;
            int totalPages = (int) Math.ceil((double) cases.size() / itemsPerPage);
            ArrayList<BufferedImage> pageImages = new ArrayList<>();

            final int pageW = 842;
            final int pageH = 595;

            System.out.println(ConsoleColors.CYAN + "Generating Disciplinary Cases PDF report..." + ConsoleColors.RESET);

            for (int pageIndex = 0; pageIndex < totalPages; pageIndex++) {
                BufferedImage image = new BufferedImage(pageW, pageH, BufferedImage.TYPE_INT_RGB);
                Graphics2D g2d = image.createGraphics();
                g2d.setColor(Color.WHITE);
                g2d.fillRect(0, 0, pageW, pageH);

                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                // Header banner
                g2d.setColor(new Color(180, 40, 40));
                g2d.fillRect(0, 0, pageW, 40);
                g2d.setColor(Color.WHITE);
                g2d.setFont(new Font("SansSerif", Font.BOLD, 16));
                g2d.drawString("DISCIPLINARY CASES REPORT", 15, 25);

                int yOffset = 60;
                int startIndex = pageIndex * itemsPerPage;
                int endIndex = Math.min(startIndex + itemsPerPage, cases.size());

                for (int i = startIndex; i < endIndex; i++) {
                    DisciplinaryCase c = cases.get(i);

                    g2d.setColor(new Color(245, 245, 245));
                    g2d.fillRect(10, yOffset, pageW - 20, 120);
                    g2d.setColor(Color.GRAY);
                    g2d.drawRect(10, yOffset, pageW - 20, 120);

                    g2d.setColor(Color.BLACK);
                    g2d.setFont(new Font("SansSerif", Font.BOLD, 11));
                    g2d.drawString("Case ID: " + c.getCaseId() + " | Person: " + c.getPersonName()
                            + " (ID " + c.getPersonId() + ", " + c.getPersonType() + ")", 20, yOffset + 20);

                    g2d.setFont(new Font("SansSerif", Font.PLAIN, 10));
                    g2d.drawString("Case Type: " + c.getCaseType(), 20, yOffset + 38);
                    g2d.drawString("Date: " + c.getDate() + " | Reported By: " + c.getReportedBy(), 20, yOffset + 54);
                    g2d.drawString("Description: " + c.getDescription(), 20, yOffset + 70);
                    g2d.drawString("Status: " + c.getStatus(), 20, yOffset + 86);
                    g2d.drawString("Action Taken: " + c.getActionTaken(), 20, yOffset + 102);

                    yOffset += 135;
                }

                // Footer
                g2d.setFont(new Font("SansSerif", Font.ITALIC, 9));
                g2d.setColor(Color.DARK_GRAY);
                g2d.drawString("Page " + (pageIndex + 1) + " of " + totalPages + " - Confidential Academic Disciplinary Record", 15, pageH - 15);

                g2d.dispose();
                pageImages.add(image);
            }

            PDFWriterHelper.writeImagesAsPdf(pageImages, outputFile);
            System.out.println(ConsoleColors.GREEN + "Disciplinary Cases PDF exported successfully! Saved to: " + outputFile.getAbsolutePath() + ConsoleColors.RESET);

            if (Desktop.isDesktopSupported() && outputFile.exists()) {
                try {
                    Desktop.getDesktop().open(outputFile);
                    System.out.println(ConsoleColors.CYAN + "Opened " + outputFile.getName() + " in your default PDF viewer." + ConsoleColors.RESET);
                } catch (Exception ex) {
                    System.out.println(ConsoleColors.YELLOW + "Note: PDF generated at " + outputFile.getAbsolutePath() + ConsoleColors.RESET);
                }
            }
        } catch (Exception e) {
            System.out.println(ConsoleColors.RED + "Failed to export Disciplinary Cases PDF: " + e.getMessage() + ConsoleColors.RESET);
            e.printStackTrace();
        }
    }
}

// ==========================================
// 9B. PURE JAVA PDF WRITER HELPER
// ==========================================
class PDFWriterHelper {
    public static void writeImagesAsPdf(ArrayList<BufferedImage> images, File outputFile) throws IOException {
        if (images == null || images.isEmpty()) {
            throw new IllegalArgumentException("No images provided for PDF creation.");
        }

        ByteArrayOutputStream pdf = new ByteArrayOutputStream();

        int pageCount = images.size();
        int firstPageObject = 3;
        int objectsPerPage = 3;
        int totalObjects = 2 + pageCount * objectsPerPage;

        // Map offsets directly to object numbers (1-indexed)
        long[] offsets = new long[totalObjects + 1];

        // PDF Header with binary marker bytes to indicate binary stream content
        pdf.write("%PDF-1.4\n".getBytes(StandardCharsets.US_ASCII));
        pdf.write(new byte[] {(byte) 0xE2, (byte) 0xE3, (byte) 0xCF, (byte) 0xD3});
        pdf.write('\n');

        // Object 1: Catalog
        writeObject(pdf, offsets, 1, "<< /Type /Catalog /Pages 2 0 R >>");

        // Object 2: Pages tree
        StringBuilder kids = new StringBuilder("[");
        for (int i = 0; i < pageCount; i++) {
            int pageObj = firstPageObject + i * objectsPerPage;
            kids.append(pageObj).append(" 0 R ");
        }
        kids.append("]");

        writeObject(pdf, offsets, 2, "<< /Type /Pages /Kids " + kids + " /Count " + pageCount + " >>");

        for (int i = 0; i < pageCount; i++) {
            BufferedImage image = images.get(i);
            int pageW = image.getWidth();
            int pageH = image.getHeight();

            ByteArrayOutputStream jpegOut = new ByteArrayOutputStream();
            ImageIO.write(image, "jpg", jpegOut);
            byte[] jpeg = jpegOut.toByteArray();

            int pageObj = firstPageObject + i * objectsPerPage;
            int contentObj = pageObj + 1;
            int imageObj = pageObj + 2;

            // Page Object
            String pageDict = "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 " + pageW + " " + pageH
                    + "] /Resources << /ProcSet [/PDF /ImageC] /XObject << /Im" + i + " " + imageObj
                    + " 0 R >> >> /Contents " + contentObj + " 0 R >>";
            writeObject(pdf, offsets, pageObj, pageDict);

            // Content Stream Object
            String content = "q\n" + pageW + " 0 0 " + pageH + " 0 0 cm\n/Im" + i + " Do\nQ\n";
            byte[] contentBytes = content.getBytes(StandardCharsets.US_ASCII);
            writeStreamObject(pdf, offsets, contentObj, "<< /Length " + contentBytes.length + " >>", contentBytes);

            // Image XObject
            String imageDict = "<< /Type /XObject /Subtype /Image /Width " + pageW
                    + " /Height " + pageH + " /ColorSpace /DeviceRGB /BitsPerComponent 8 /Filter /DCTDecode /Length "
                    + jpeg.length + " >>";
            writeStreamObject(pdf, offsets, imageObj, imageDict, jpeg);
        }

        long xrefOffset = pdf.size();

        pdf.write(("xref\n0 " + (totalObjects + 1) + "\n").getBytes(StandardCharsets.US_ASCII));
        pdf.write("0000000000 65535 f \r\n".getBytes(StandardCharsets.US_ASCII));

        for (int i = 1; i <= totalObjects; i++) {
            String line = String.format(Locale.US, "%010d 00000 n \r\n", offsets[i]);
            pdf.write(line.getBytes(StandardCharsets.US_ASCII));
        }

        String trailer = "trailer\n<< /Size " + (totalObjects + 1) + " /Root 1 0 R >>\nstartxref\n" + xrefOffset + "\n%%EOF\n";
        pdf.write(trailer.getBytes(StandardCharsets.US_ASCII));

        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            fos.write(pdf.toByteArray());
        }
    }

    private static void writeObject(ByteArrayOutputStream pdf, long[] offsets, int objectNumber, String dictionary) throws IOException {
        offsets[objectNumber] = pdf.size();
        String object = objectNumber + " 0 obj\n" + dictionary + "\nendobj\n";
        pdf.write(object.getBytes(StandardCharsets.US_ASCII));
    }

    private static void writeStreamObject(ByteArrayOutputStream pdf, long[] offsets, int objectNumber, String dictionary, byte[] streamData) throws IOException {
        offsets[objectNumber] = pdf.size();
        String header = objectNumber + " 0 obj\n" + dictionary + "\nstream\n";
        pdf.write(header.getBytes(StandardCharsets.US_ASCII));
        pdf.write(streamData);
        pdf.write("\nendstream\nendobj\n".getBytes(StandardCharsets.US_ASCII));
    }
}

// ==========================================
// 10. AUTOMATIC 5,000 DATASET GENERATOR
// ==========================================
class DatasetGenerator {
    private static final String[] FIRST_NAMES = {"Aarav", "Ananya", "Rohan", "Priya", "Vikram", "Neha", "Aditya", "Sanya", "Rahul", "Kavya", "Arjun", "Diya", "Karan", "Pooja", "Varun", "Meera"};
    private static final String[] LAST_NAMES = {"Sharma", "Verma", "Gupta", "Patel", "Kumar", "Singh", "Reddy", "Nair", "Rao", "Joshi", "Mehta", "Chopra"};
    private static final String[] DEPARTMENTS = {"CSE", "IT", "ECE", "EEE", "MECH", "CIVIL", "AIDS", "AIML"};
    private static final String[] BLOOD_GROUPS = {"A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"};
    private static final String[] SECTIONS = {"A", "B", "C", "D"};
    private static final String[] CITIES = {"Chennai", "Mumbai", "Delhi", "Bengaluru", "Hyderabad", "Pune", "Kolkata"};
    private static final String[] STATES = {"Tamil Nadu", "Maharashtra", "Delhi", "Karnataka", "Telangana", "West Bengal"};

    public static final String CSV_HEADER =
        "StudentID,FullName,Department,Year,Gender,DOB,BloodGroup,Phone,Email,Address,City,State,Pincode,ParentName,ParentPhone,AdmissionYear,Section,Semester,CollegeName,AcademicStatus";

    public static void generateCsvIfMissing(String fileName, int count) {
        File file = new File(fileName);
        if (file.exists()) return;

        System.out.println(ConsoleColors.YELLOW + "Dataset missing. Generating " + count + " student records..." + ConsoleColors.RESET);
        Random rand = new Random(42);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write(CSV_HEADER + "\n");

            for (int id = 1; id <= count; id++) {
                String fname = FIRST_NAMES[rand.nextInt(FIRST_NAMES.length)];
                String lname = LAST_NAMES[rand.nextInt(LAST_NAMES.length)];
                String name = fname + " " + lname;
                String dept = DEPARTMENTS[rand.nextInt(DEPARTMENTS.length)];
                int year = 1 + rand.nextInt(4);
                int semester = (year * 2) - rand.nextInt(2);
                String gender = (rand.nextBoolean()) ? "Male" : "Female";
                String blood = BLOOD_GROUPS[rand.nextInt(BLOOD_GROUPS.length)];
                String phone = "9" + String.format("%09d", rand.nextInt(1000000000));
                String pPhone = "9" + String.format("%09d", rand.nextInt(1000000000));
                String email = fname.toLowerCase() + id + "@cit.edu.in";
                String address = (10 + rand.nextInt(90)) + " Gandhi Road";
                String city = CITIES[rand.nextInt(CITIES.length)];
                String state = STATES[rand.nextInt(STATES.length)];
                String pincode = String.format("%06d", 600000 + rand.nextInt(99999));
                String parent = FIRST_NAMES[rand.nextInt(FIRST_NAMES.length)] + " " + lname;
                String dob = (2002 + rand.nextInt(5)) + "-" + String.format("%02d", 1 + rand.nextInt(12)) + "-" + String.format("%02d", 1 + rand.nextInt(28));

                writer.write(String.format("%d,\"%s\",%s,%d,%s,%s,%s,%s,%s,\"%s\",%s,%s,%s,\"%s\",%s,%d,%s,%d,\"Chennai Institute of Technology\",Active\n",
                        id, name, dept, year, gender, dob, blood, phone, email, address, city, state, pincode, parent, pPhone, 2026 - year, SECTIONS[rand.nextInt(SECTIONS.length)], semester));
            }
            System.out.println(ConsoleColors.GREEN + "Generated " + count + " records in " + fileName + ConsoleColors.RESET);
        } catch (IOException e) {
            System.out.println(ConsoleColors.RED + "Error creating dataset: " + e.getMessage() + ConsoleColors.RESET);
        }
    }

    /**
     * Rewrites the CSV file from the current in-memory list of students.
     * Called after a delete so removed records don't reappear on next launch.
     */
    public static void rewriteCsv(String fileName, List<Person> people) {
        File file = new File(fileName);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, false))) {
            writer.write(CSV_HEADER + "\n");
            for (Person p : people) {
                if (p instanceof Student) {
                    writer.write(((Student) p).toCsvRow() + "\n");
                }
            }
        } catch (IOException e) {
            System.out.println(ConsoleColors.RED + "Failed to update " + fileName + ": " + e.getMessage() + ConsoleColors.RESET);
        }
    }
}

// ==========================================
// 11. MAIN SYSTEM ENTRY POINT
// ==========================================
public class StudentID_Generator_AI_new {

    private static final Map<Integer, Person> personMap = new HashMap<>();
    private static final String STUDENTS_CSV = "students.csv";

    private static String selectPhotoGUI() {
        final String[] selectedPath = {"None"};
        try {
            SwingUtilities.invokeAndWait(() -> {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (Exception ignored) {}

                JFrame topFrame = new JFrame();
                topFrame.setAlwaysOnTop(true);
                topFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                topFrame.setLocationRelativeTo(null);

                JFileChooser chooser = new JFileChooser();
                chooser.setDialogTitle("Select Photo");
                chooser.setFileFilter(new FileNameExtensionFilter("Image Files", "jpg", "jpeg", "png", "bmp"));

                int res = chooser.showOpenDialog(topFrame);
                if (res == JFileChooser.APPROVE_OPTION) {
                    selectedPath[0] = chooser.getSelectedFile().getAbsolutePath();
                }
                topFrame.dispose();
            });
        } catch (Exception e) {
            System.out.println(ConsoleColors.RED + "Could not open File Chooser: " + e.getMessage() + ConsoleColors.RESET);
        }
        return selectedPath[0];
    }

    private static int readValidInt(Scanner sc, String prompt) {
        while (true) {
            System.out.print(prompt);
            try {
                return Integer.parseInt(sc.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println(ConsoleColors.RED + "Invalid input! Enter a valid integer." + ConsoleColors.RESET);
            }
        }
    }

    private static void checkPhoto(String photoPath, ArrayList<Person> existingPeople) {
        if (photoPath == null || photoPath.equalsIgnoreCase("None")) return;
        File file = new File(photoPath);
        if (!file.exists()) return;

        try {
            BufferedImage img = ImageIO.read(file);
            if (img == null) return;

            double sharpness = PhotoQualityChecker.calculateSharpness(img);
            if (sharpness < 80) {
                System.out.println(ConsoleColors.YELLOW
                        + "Warning: photo looks blurry (sharpness score "
                        + String.format("%.1f", sharpness)
                        + "). Consider re-uploading a clearer image."
                        + ConsoleColors.RESET);
            }

            if (DuplicatePhotoChecker.isDuplicate(img, existingPeople, 8)) {
                System.out.println(ConsoleColors.YELLOW
                        + "Warning: this photo appears similar to an existing photo."
                        + ConsoleColors.RESET);
            }
        } catch (IOException e) {
            System.out.println(ConsoleColors.RED + "Could not analyze photo: " + e.getMessage() + ConsoleColors.RESET);
        }
    }

    private static ArrayList<String> parseCSVLine(String line) {
        ArrayList<String> tokens = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder sb = new StringBuilder();

        for (char c : line.toCharArray()) {
            if (c == '\"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                tokens.add(sb.toString());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        tokens.add(sb.toString());
        return tokens;
    }

    private static int loadStudentDataset(ArrayList<Person> people, String fileName) {
        DatasetGenerator.generateCsvIfMissing(fileName, 5000);

        File file = new File(fileName);
        int loaded = 0;

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line = br.readLine(); // Header
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                ArrayList<String> fields = parseCSVLine(line);
                if (fields.size() >= 20) {
                    Student s = new Student(fields.toArray(new String[0]));
                    if (!personMap.containsKey(s.id)) {
                        people.add(s);
                        personMap.put(s.id, s);
                        loaded++;
                    }
                } else if (fields.size() >= 8) {
                    String idText = fields.get(0).trim().toUpperCase();
                    int id = idText.startsWith("CIT") ? Integer.parseInt(idText.substring(3)) : Integer.parseInt(idText);
                    if (!personMap.containsKey(id)) {
                        Student s = new Student(id, fields.get(1).trim(), fields.get(2).trim(), fields.get(3).trim(),
                                                fields.get(4).trim(), "N/A", fields.get(7).trim(), fields.get(5).trim(), "None");
                        people.add(s);
                        personMap.put(s.id, s);
                        loaded++;
                    }
                }
            }
            System.out.println(ConsoleColors.GREEN + "Loaded " + loaded + " student records from " + fileName + ConsoleColors.RESET);
        } catch (Exception e) {
            System.out.println(ConsoleColors.RED + "Failed loading dataset: " + e.getMessage() + ConsoleColors.RESET);
        }
        return loaded;
    }

    private static Person findPersonById(int id) {
        return personMap.get(id);
    }

    /**
     * Deletes a Student/Faculty record after confirmation. Removes it from
     * the in-memory list, the lookup map, and any cached QR payload.
     * Disciplinary case history is preserved (the case stores the name
     * directly, so it survives the person record being deleted).
     * If the person came from students.csv, the CSV is rewritten so the
     * record does not reappear on the next launch.
     */
    private static void deletePerson(Scanner sc, ArrayList<Person> people, CaseManager caseManager) {
        int id = readValidInt(sc, "Enter Student/Faculty ID to delete: ");
        Person p = findPersonById(id);

        if (p == null) {
            System.out.println(ConsoleColors.RED + "ID not found." + ConsoleColors.RESET);
            return;
        }

        List<DisciplinaryCase> history = caseManager.searchCases(String.valueOf(id));
        if (!history.isEmpty()) {
            System.out.println(ConsoleColors.YELLOW + "Warning: this person has " + history.size()
                    + " disciplinary case record(s) on file. Deleting the person record will NOT delete "
                    + "the case history (it is preserved by name), but they will no longer be linkable "
                    + "as an active person record." + ConsoleColors.RESET);
        }

        System.out.println(ConsoleColors.CYAN + "Record to delete: " + p.getName() + " (" + p.getDepartment() + ", ID " + id + ")" + ConsoleColors.RESET);
        System.out.print("Type YES to confirm permanent deletion: ");
        String confirm = sc.nextLine().trim();

        if (!confirm.equalsIgnoreCase("YES")) {
            System.out.println(ConsoleColors.GREEN + "Deletion cancelled." + ConsoleColors.RESET);
            return;
        }

        boolean wasStudent = p instanceof Student;

        people.remove(p);
        personMap.remove(id);
        QRCodeGenerator.encryptedPayloads.remove(id);

        if (wasStudent) {
            DatasetGenerator.rewriteCsv(STUDENTS_CSV, people);
        }

        System.out.println(ConsoleColors.GREEN + "Record for ID " + id + " deleted successfully." + ConsoleColors.RESET);
        if (wasStudent) {
            System.out.println(ConsoleColors.GREEN + STUDENTS_CSV + " updated - this student will not reappear on next launch." + ConsoleColors.RESET);
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        // Nothing below this line runs — the dataset is not loaded, no menu
        // is shown — unless the admin login succeeds.
        if (!AuthManager.authenticate(sc)) {
            System.exit(0);
        }

        ArrayList<Person> people = new ArrayList<>();
        CaseManager caseManager = new CaseManager();

        loadStudentDataset(people, STUDENTS_CSV);

        // Pre-seed sample faculty
        Faculty f1 = new Faculty(9001, "Dr. A. Ramanathan", "CSE", "Professor & HOD", "1978-05-12", "9876543210", "Anna Nagar, Chennai", "None");
        people.add(f1);
        personMap.put(f1.id, f1);

        while (true) {
            System.out.println("\n" + ConsoleColors.CYAN + "==========================================");
            System.out.println("  STUDENT & FACULTY ID CARD MANAGEMENT SYSTEM");
            System.out.println("==========================================" + ConsoleColors.RESET);
            System.out.println("1. Add Student Record");
            System.out.println("2. Add Faculty Record");
            System.out.println("3. Display All ID Cards (GUI Browser)");
            System.out.println("4. Faculty/Student ID Verification");
            System.out.println("5. Export All ID Cards to PNG Image");
            System.out.println("6. QR Verification / Decrypt Scan Payload");
            System.out.println("7. Add Disciplinary Case");
            System.out.println("8. View Case History for ID");
            System.out.println("9. View All Disciplinary Cases");
            System.out.println("10. Update Disciplinary Case Status");
            System.out.println("11. Student Services / Profile Lookup");
            System.out.println("12. Print/Export All ID Cards to PDF");
            System.out.println("13. Print/Export Disciplinary Cases to PDF");
            System.out.println("14. Search Disciplinary Cases (Name or ID)");
            System.out.println("15. Delete Student/Faculty Record");
            System.out.println("16. Exit");

            int choice = readValidInt(sc, "Select an option (1-16): ");

            switch (choice) {
                case 1: {
                    int id = readValidInt(sc, "Enter Student ID (Numeric): ");
                    if (personMap.containsKey(id)) {
                        System.out.println(ConsoleColors.RED + "ID " + id + " already exists." + ConsoleColors.RESET);
                        break;
                    }
                    System.out.print("Enter Name: ");
                    String name = sc.nextLine().trim();
                    System.out.print("Enter Department: ");
                    String dept = sc.nextLine().trim();
                    System.out.print("Enter Year (e.g. 1, 2, 3, 4): ");
                    String year = sc.nextLine().trim();
                    System.out.print("Enter Blood Group: ");
                    String blood = sc.nextLine().trim();
                    System.out.print("Enter Date of Birth (YYYY-MM-DD): ");
                    String dob = sc.nextLine().trim();
                    System.out.print("Enter Phone: ");
                    String phone = sc.nextLine().trim();
                    System.out.print("Enter Address: ");
                    String addr = sc.nextLine().trim();

                    System.out.println("Select Photo File...");
                    String photo = selectPhotoGUI();
                    checkPhoto(photo, people);

                    Student st = new Student(id, name, dept, year, blood, dob, phone, addr, photo);
                    people.add(st);
                    personMap.put(id, st);
                    System.out.println(ConsoleColors.GREEN + "Student added successfully." + ConsoleColors.RESET);
                    break;
                }

                case 2: {
                    int id = readValidInt(sc, "Enter Faculty ID (Numeric): ");
                    if (personMap.containsKey(id)) {
                        System.out.println(ConsoleColors.RED + "ID " + id + " already exists." + ConsoleColors.RESET);
                        break;
                    }
                    System.out.print("Enter Name: ");
                    String name = sc.nextLine().trim();
                    System.out.print("Enter Department: ");
                    String dept = sc.nextLine().trim();
                    System.out.print("Enter Designation: ");
                    String desig = sc.nextLine().trim();
                    System.out.print("Enter Date of Birth (YYYY-MM-DD): ");
                    String dob = sc.nextLine().trim();
                    System.out.print("Enter Phone: ");
                    String phone = sc.nextLine().trim();
                    System.out.print("Enter Address: ");
                    String addr = sc.nextLine().trim();

                    System.out.println("Select Photo File...");
                    String photo = selectPhotoGUI();
                    checkPhoto(photo, people);

                    Faculty fc = new Faculty(id, name, dept, desig, dob, phone, addr, photo);
                    people.add(fc);
                    personMap.put(id, fc);
                    System.out.println(ConsoleColors.GREEN + "Faculty added successfully." + ConsoleColors.RESET);
                    break;
                }

                case 3:
                    if (people.isEmpty()) {
                        System.out.println(ConsoleColors.RED + "No records available to display." + ConsoleColors.RESET);
                    } else {
                        SwingUtilities.invokeLater(() -> new IDCardGUIBrowser(people));
                    }
                    break;

                case 4: {
                    int id = readValidInt(sc, "Enter Student/Faculty ID: ");
                    Person p = findPersonById(id);
                    if (p != null) {
                        System.out.println(ConsoleColors.GREEN + "Record Found: " + p.getName() + " (" + p.getDepartment() + ")" + ConsoleColors.RESET);
                        if (caseManager.hasActiveCase(id)) {
                            System.out.println(ConsoleColors.RED + "WARNING: Active Disciplinary Case Recorded for ID " + id + ConsoleColors.RESET);
                        }
                        if (p instanceof IDPrintable) {
                            ((IDPrintable) p).printIDCard();
                        }
                    } else {
                        System.out.println(ConsoleColors.RED + "ID not found." + ConsoleColors.RESET);
                    }
                    break;
                }

                case 5:
                    if (people.isEmpty()) {
                        System.out.println(ConsoleColors.RED + "No records available." + ConsoleColors.RESET);
                    } else {
                        PNGExporter.exportAllToPNG(people);
                    }
                    break;

                case 6: {
                    int id = readValidInt(sc, "Enter ID to simulate QR verification: ");
                    System.out.println(QRCodeGenerator.verifyById(id));
                    break;
                }

                case 7: {
                    int id = readValidInt(sc, "Enter Student/Faculty ID for Case: ");
                    Person p = findPersonById(id);
                    if (p == null) {
                        System.out.println(ConsoleColors.RED + "ID not found. Cannot link case." + ConsoleColors.RESET);
                        break;
                    }
                    System.out.print("Enter Case ID: ");
                    String caseId = sc.nextLine().trim();
                    String personType = (p instanceof Student) ? "Student" : "Faculty";
                    System.out.print("Enter Case Type (e.g. Lab Safety Violation): ");
                    String cType = sc.nextLine().trim();
                    System.out.print("Enter Description: ");
                    String desc = sc.nextLine().trim();
                    System.out.print("Enter Date (YYYY-MM-DD): ");
                    String dt = sc.nextLine().trim();
                    System.out.print("Reported By: ");
                    String rep = sc.nextLine().trim();

                    DisciplinaryCase c = new DisciplinaryCase(caseId, id, p.getName(), personType, cType, desc, dt, rep, "Active", "Pending Hearing");
                    caseManager.addCase(c);
                    System.out.println(ConsoleColors.GREEN + "Case linked to ID " + id + " successfully." + ConsoleColors.RESET);
                    break;
                }

                case 8: {
                    int id = readValidInt(sc, "Enter Student/Faculty ID: ");
                    caseManager.showCases(id);
                    break;
                }

                case 9:
                    caseManager.showAllCases();
                    break;

                case 10:
                    caseManager.updateCase(sc);
                    break;

                case 11: {
                    int id = readValidInt(sc, "Enter Student ID: ");
                    Person p = findPersonById(id);
                    if (p instanceof Student) {
                        ((Student) p).displayFullProfile();
                    } else if (p != null) {
                        System.out.println(ConsoleColors.YELLOW + "ID " + id + " belongs to Faculty, not Student." + ConsoleColors.RESET);
                    } else {
                        System.out.println(ConsoleColors.RED + "ID not found." + ConsoleColors.RESET);
                    }
                    break;
                }

                case 12:
                    if (people.isEmpty()) {
                        System.out.println(ConsoleColors.RED + "No records available to export." + ConsoleColors.RESET);
                    } else {
                        PDFExporter.exportAllToPDF(people);
                    }
                    break;

                case 13:
                    DisciplinaryCasePDFExporter.exportCasesToPDF(caseManager.getCases());
                    break;

                case 14: {
                    System.out.print("Enter Student/Faculty Name or ID to search: ");
                    String query = sc.nextLine().trim();
                    caseManager.searchAndReport(query);
                    break;
                }

                case 15:
                    deletePerson(sc, people, caseManager);
                    break;

                case 16:
                    System.out.println("Exiting System. Goodbye!");
                    System.exit(0);

                default:
                    System.out.println(ConsoleColors.RED + "Invalid option." + ConsoleColors.RESET);
            }
        }
    }
}
