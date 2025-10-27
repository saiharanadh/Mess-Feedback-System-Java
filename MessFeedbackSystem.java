import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;

/**
 * MessFeedbackSystem.java
 * A self-contained Java Swing frontend and simple backend logic for a mess feedback system.
 */

public class MessFeedbackSystem extends JFrame {
    private JTextField tfName, tfRoll, tfMess;
    private JComboBox<Integer> cbRating;
    private JTextArea taComments;
    private DefaultTableModel tableModel;
    private JTable table;
    private FeedbackManager manager;

    public MessFeedbackSystem() {
        super("Mess Feedback System");
        manager = new FeedbackManager();
        manager.loadFromFile();
        initUI();
        refreshTable();
        setSize(900, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                manager.saveToFile();
                dispose();
                System.exit(0);
            }
        });
    }

    private void initUI() {
        JPanel inputPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6,6,6,6);
        gbc.anchor = GridBagConstraints.WEST;

        tfName = new JTextField(15);
        tfRoll = new JTextField(10);
        tfMess = new JTextField(10);
        cbRating = new JComboBox<>(new Integer[]{1,2,3,4,5});
        taComments = new JTextArea(4, 30);
        taComments.setLineWrap(true);
        taComments.setWrapStyleWord(true);

        gbc.gridx = 0; gbc.gridy = 0; inputPanel.add(new JLabel("Name:"), gbc);
        gbc.gridx = 1; inputPanel.add(tfName, gbc);
        gbc.gridx = 2; inputPanel.add(new JLabel("Roll No:"), gbc);
        gbc.gridx = 3; inputPanel.add(tfRoll, gbc);

        gbc.gridx = 0; gbc.gridy = 1; inputPanel.add(new JLabel("Mess:"), gbc);
        gbc.gridx = 1; inputPanel.add(tfMess, gbc);
        gbc.gridx = 2; inputPanel.add(new JLabel("Rating (1-5):"), gbc);
        gbc.gridx = 3; inputPanel.add(cbRating, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 4;
        inputPanel.add(new JLabel("Comments:"), gbc);
        gbc.gridy = 3;
        inputPanel.add(new JScrollPane(taComments), gbc);

        JButton btnAdd = new JButton("Add Feedback");
        JButton btnDelete = new JButton("Delete Selected");
        JButton btnExport = new JButton("Export CSV");
        JButton btnStats = new JButton("Show Stats");

        JPanel btnPanel = new JPanel();
        btnPanel.add(btnAdd);
        btnPanel.add(btnDelete);
        btnPanel.add(btnExport);
        btnPanel.add(btnStats);

        String[] colNames = {"ID","Timestamp","Name","Roll No","Mess","Rating","Comments"};
        tableModel = new DefaultTableModel(colNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = new JTable(tableModel);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoCreateRowSorter(true);

        btnAdd.addActionListener(e -> onAdd());
        btnDelete.addActionListener(e -> onDelete());
        btnExport.addActionListener(e -> onExport());
        btnStats.addActionListener(e -> onStats());

        setLayout(new BorderLayout());
        add(inputPanel, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(btnPanel, BorderLayout.SOUTH);
    }

    private void onAdd() {
        String name = tfName.getText().trim();
        String roll = tfRoll.getText().trim();
        String mess = tfMess.getText().trim();
        int rating = (int) cbRating.getSelectedItem();
        String comments = taComments.getText().trim();

        if (name.isEmpty() || roll.isEmpty() || mess.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please fill Name, Roll No and Mess fields.", "Input required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Feedback f = new Feedback(UUID.randomUUID().toString(), new Date(), name, roll, mess, rating, comments);
        manager.addFeedback(f);
        manager.saveToFile();
        refreshTable();
        clearInputs();
    }

    private void onDelete() {
        int sel = table.getSelectedRow();
        if (sel == -1) {
            JOptionPane.showMessageDialog(this, "Select a row to delete.", "No selection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int modelIndex = table.convertRowIndexToModel(sel);
        String id = (String) tableModel.getValueAt(modelIndex, 0);
        int confirm = JOptionPane.showConfirmDialog(this, "Delete selected feedback?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            manager.removeFeedbackById(id);
            manager.saveToFile();
            refreshTable();
        }
    }

    private void onExport() {
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File("feedback_export.csv"));
        int ret = fc.showSaveDialog(this);
        if (ret == JFileChooser.APPROVE_OPTION) {
            File f = fc.getSelectedFile();
            try {
                manager.exportCSV(f);
                JOptionPane.showMessageDialog(this, "Exported to " + f.getAbsolutePath());
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Export failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void onStats() {
        java.util.List<Feedback> all = manager.getAllFeedbacks();
        if (all.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No feedbacks yet.", "Stats", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        double avg = all.stream().mapToInt(Feedback::getRating).average().orElse(0.0);
        long count = all.size();
        String msg = String.format("Total feedbacks: %d\nAverage rating: %.2f", count, avg);
        JOptionPane.showMessageDialog(this, msg, "Statistics", JOptionPane.INFORMATION_MESSAGE);
    }

    private void clearInputs() {
        tfName.setText("");
        tfRoll.setText("");
        tfMess.setText("");
        cbRating.setSelectedIndex(0);
        taComments.setText("");
    }

    private void refreshTable() {
        tableModel.setRowCount(0);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        for (Feedback f : manager.getAllFeedbacks()) {
            tableModel.addRow(new Object[]{f.getId(), sdf.format(f.getTimestamp()), f.getName(), f.getRollNo(), f.getMess(), f.getRating(), f.getComments()});
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}
            new MessFeedbackSystem().setVisible(true);
        });
    }
}

class Feedback implements Serializable {
    private static final long serialVersionUID = 1L;
    private String id;
    private Date timestamp;
    private String name;
    private String rollNo;
    private String mess;
    private int rating;
    private String comments;

    public Feedback(String id, Date timestamp, String name, String rollNo, String mess, int rating, String comments) {
        this.id = id;
        this.timestamp = timestamp;
        this.name = name;
        this.rollNo = rollNo;
        this.mess = mess;
        this.rating = rating;
        this.comments = comments;
    }

    public String getId() { return id; }
    public Date getTimestamp() { return timestamp; }
    public String getName() { return name; }
    public String getRollNo() { return rollNo; }
    public String getMess() { return mess; }
    public int getRating() { return rating; }
    public String getComments() { return comments; }
}

class FeedbackManager {
    private java.util.List<Feedback> list = new java.util.ArrayList<>();
    private final File storage = new File("feedbacks.ser");

    public void addFeedback(Feedback f) {
        list.add(0, f);
    }

    public void removeFeedbackById(String id) {
        list.removeIf(f -> f.getId().equals(id));
    }

    public java.util.List<Feedback> getAllFeedbacks() {
        return new java.util.ArrayList<>(list);
    }

    public void saveToFile() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(storage))) {
            oos.writeObject(list);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    public void loadFromFile() {
        if (!storage.exists()) return;
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(storage))) {
            Object o = ois.readObject();
            if (o instanceof java.util.List) {
                list = (java.util.List<Feedback>) o;
            }
        } catch (Exception e) {
            System.err.println("Failed to load stored feedbacks: " + e.getMessage());
        }
    }

    public void exportCSV(File file) throws IOException {
        try (PrintWriter pw = new PrintWriter(new FileWriter(file))) {
            pw.println("ID,Timestamp,Name,RollNo,Mess,Rating,Comments");
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            for (Feedback f : list) {
                String line = String.format("%s,%s,%s,%s,%s,%d,%s",
                        escapeCsv(f.getId()), escapeCsv(sdf.format(f.getTimestamp())), escapeCsv(f.getName()),
                        escapeCsv(f.getRollNo()), escapeCsv(f.getMess()), f.getRating(), escapeCsv(f.getComments()));
                pw.println(line);
            }
        }
    }

    private String escapeCsv(String s) {
        if (s == null) return "";
        String out = s.replace("\"", "\"\"");
        if (out.contains(",") || out.contains("\n") || out.contains("\"")) {
            out = "\"" + out + "\"";
        }
        return out;
    }
}
