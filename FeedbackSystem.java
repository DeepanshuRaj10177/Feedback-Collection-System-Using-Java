import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FeedbackSystem {

    // --- SOLID COLOR THEME CONSTANTS ---
    // Backgrounds & Text
    private static final Color BG_COLOR = new Color(245, 245, 245); // Light Gray Background
    
    // Button Colors (Solid & Bright)
    private static final Color BTN_PURPLE = new Color(155, 89, 182);
    private static final Color BTN_GREEN  = new Color(46, 204, 113);
    private static final Color BTN_RED    = new Color(231, 76, 60);
    private static final Color BTN_YELLOW = new Color(241, 196, 15);
    private static final Color BTN_BLUE   = new Color(52, 152, 219);
    private static final Color BTN_GRAY   = new Color(149, 165, 166);

    public static void main(String[] args) {
        // NOTE: We keep the System Look and Feel for the window borders and file choosers,
        // but we will manually override the Buttons to look "Solid" using styleButton().
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}
        
        // Global Font Settings
        UIManager.put("Label.font", new Font("Segoe UI", Font.PLAIN, 14));
        UIManager.put("Button.font", new Font("Segoe UI", Font.BOLD, 12));
        UIManager.put("Table.font", new Font("Segoe UI", Font.PLAIN, 12));
        UIManager.put("TableHeader.font", new Font("Segoe UI", Font.BOLD, 13));

        SwingUtilities.invokeLater(FeedbackSystem::showRoleSelectionScreen);
    }

    // --- HELPER METHOD TO STYLE BUTTONS (FIXED FOR SOLID COLORS) ---
    private static void styleButton(JButton btn, Color bgColor) {
        // 1. Force the UI to be "Basic". This removes the Windows/Mac native "skin"
        //    that prevents the background color from showing fully.
        btn.setUI(new BasicButtonUI());
        
        // 2. Set the solid colors
        btn.setBackground(bgColor);
        btn.setForeground(Color.BLACK); // Text is explicitly BLACK
        
        // 3. Font and Cursor
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // 4. Border and Opacity
        btn.setOpaque(true); // Mandatory for background color to paint
        btn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.DARK_GRAY, 1), // Thin dark outline
            BorderFactory.createEmptyBorder(8, 20, 8, 20)       // Padding inside button
        ));
        
        // 5. Interaction effects (optional: creates a simple hover effect)
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                btn.setBackground(bgColor.brighter());
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                btn.setBackground(bgColor);
            }
        });
    }

    private static void showRoleSelectionScreen() {
        RoleSelectionDialog roleDialog = new RoleSelectionDialog(null);
        roleDialog.setVisible(true);
        String selectedRole = roleDialog.getSelectedRole();

        if (selectedRole == null) {
            System.exit(0);
        } else if ("ADMIN".equals(selectedRole)) {
            showLoginScreen("ADMIN", null);
        } else {
            showFormSelectionScreen();
        }
    }
    
    private static void showFormSelectionScreen() {
        FormSelectionDialog formDialog = new FormSelectionDialog(null);
        formDialog.setVisible(true);
        FormDefinition selectedForm = formDialog.getSelectedForm();
        
        if (selectedForm != null) {
            showLoginScreen("USER", selectedForm);
        } else {
            showRoleSelectionScreen();
        }
    }

    private static void showLoginScreen(String role, FormDefinition form) {
        LoginDialog loginDlg = new LoginDialog(null, role);
        loginDlg.setVisible(true);
        User user = loginDlg.getAuthenticatedUser();
        
        if (user != null) {
            if ("ADMIN".equals(role)) {
                new AdminDashboard(user).setVisible(true);
            } else {
                boolean submitted = DataService.getInstance().hasUserSubmittedForm(user, form);
                if (submitted) {
                    JOptionPane.showMessageDialog(null, "You have already submitted feedback for this form.", "Already Submitted", JOptionPane.WARNING_MESSAGE);
                    showFormSelectionScreen();
                } else {
                    new FeedbackForm(user, form).setVisible(true);
                }
            }
        } else {
            if ("ADMIN".equals(role)) {
                showRoleSelectionScreen();
            } else {
                showFormSelectionScreen();
            }
        }
    }

    // --- DIALOG CLASSES ---

    private static class RoleSelectionDialog extends JDialog {
        private String selectedRole = null;

        public RoleSelectionDialog(Frame parent) {
            super(parent, "Feedback System - Login", true);
            getContentPane().setBackground(BG_COLOR);
            
            JPanel panel = new JPanel(new GridLayout(3, 1, 15, 15));
            panel.setBackground(BG_COLOR);
            panel.setBorder(BorderFactory.createEmptyBorder(30, 50, 30, 50));

            JLabel label = new JLabel("Please select a login type:");
            label.setHorizontalAlignment(SwingConstants.CENTER);
            label.setFont(new Font("Segoe UI", Font.BOLD, 16));
            panel.add(label);

            JButton adminButton = new JButton("Admin Login");
            styleButton(adminButton, BTN_PURPLE); // Solid Purple
            adminButton.addActionListener(e -> {
                selectedRole = "ADMIN";
                dispose();
            });
            panel.add(adminButton);

            JButton userButton = new JButton("User Login");
            styleButton(userButton, BTN_BLUE); // Solid Blue
            userButton.addActionListener(e -> {
                selectedRole = "USER";
                dispose();
            });
            panel.add(userButton);

            getContentPane().add(panel, BorderLayout.CENTER);
            setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
            setSize(400, 300);
            setResizable(false);
            setLocationRelativeTo(parent);
        }
        public String getSelectedRole() { return selectedRole; }
    }
    
    private static class FormSelectionDialog extends JDialog {
        private FormDefinition selectedForm = null;

        public FormSelectionDialog(Frame parent) {
            super(parent, "Select Feedback Form", true);
            getContentPane().setBackground(BG_COLOR);
            
            DefaultListModel<FormDefinition> listModel = new DefaultListModel<>();
            DataService.getInstance().getForms().forEach(listModel::addElement);
            
            JList<FormDefinition> formList = new JList<>(listModel);
            formList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            formList.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            formList.setFixedCellHeight(30);
            
            JButton selectButton = new JButton("Select & Login");
            styleButton(selectButton, BTN_GREEN); // Solid Green
            selectButton.setEnabled(false);
            
            formList.addListSelectionListener(e -> {
                if (!e.getValueIsAdjusting()) {
                    selectButton.setEnabled(formList.getSelectedIndex() != -1);
                }
            });
            
            selectButton.addActionListener(e -> {
                selectedForm = formList.getSelectedValue();
                dispose();
            });
            
            JPanel mainPanel = new JPanel(new BorderLayout(15, 15));
            mainPanel.setBackground(BG_COLOR);
            mainPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

            JLabel titleLbl = new JLabel("Available Feedback Forms:");
            titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 14));
            mainPanel.add(titleLbl, BorderLayout.NORTH);
            
            mainPanel.add(new JScrollPane(formList), BorderLayout.CENTER);
            
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            buttonPanel.setBackground(BG_COLOR);
            
            JButton cancelButton = new JButton("Back");
            styleButton(cancelButton, BTN_RED); // Solid Red
            cancelButton.addActionListener(e -> {
                selectedForm = null;
                dispose();
            });
            
            buttonPanel.add(cancelButton);
            buttonPanel.add(selectButton);
            mainPanel.add(buttonPanel, BorderLayout.SOUTH);
            
            setContentPane(mainPanel);
            setSize(500, 400);
            setLocationRelativeTo(parent);
        }
        
        public FormDefinition getSelectedForm() { return selectedForm; }
    }

    private static class LoginDialog extends JDialog {
        private final JTextField tfUsername;
        private final JPasswordField pfPassword;
        private User authenticatedUser;
        private final String expectedRole;

        public LoginDialog(Frame parent, String role) {
            super(parent, role + " Login", true);
            this.expectedRole = role;
            getContentPane().setBackground(BG_COLOR);

            JPanel panel = new JPanel(new GridBagLayout());
            panel.setBackground(BG_COLOR);
            GridBagConstraints cs = new GridBagConstraints();
            cs.fill = GridBagConstraints.HORIZONTAL;
            cs.insets = new Insets(10, 10, 10, 10);
            
            cs.gridx = 0; cs.gridy = 0;
            panel.add(new JLabel("Username:"), cs);

            tfUsername = new JTextField(20);
            cs.gridx = 1; panel.add(tfUsername, cs);

            cs.gridx = 0; cs.gridy = 1;
            panel.add(new JLabel("Password:"), cs);

            pfPassword = new JPasswordField(20);
            cs.gridx = 1; panel.add(pfPassword, cs);

            JButton btnLogin = new JButton("Login");
            styleButton(btnLogin, BTN_GREEN); // Solid Green
            btnLogin.addActionListener(e -> onLogin());

            JButton btnCancel = new JButton("Cancel");
            styleButton(btnCancel, BTN_RED); // Solid Red
            btnCancel.addActionListener(e -> onCancel());

            JPanel bp = new JPanel();
            bp.setBackground(BG_COLOR);
            bp.add(btnCancel);
            bp.add(btnLogin);

            getContentPane().add(panel, BorderLayout.CENTER);
            getContentPane().add(bp, BorderLayout.PAGE_END);
            pack();
            setResizable(false);
            setLocationRelativeTo(parent);
        }

        private void onLogin() {
            String username = tfUsername.getText();
            String password = new String(pfPassword.getPassword());
            authenticatedUser = DataService.getInstance().authenticateUser(username, password);
            if (authenticatedUser != null && authenticatedUser.getRole().equals(this.expectedRole)) {
                dispose();
            } else {
                JOptionPane.showMessageDialog(this, "Invalid credentials.", "Login Failed", JOptionPane.ERROR_MESSAGE);
            }
        }
        
        private void onCancel() {
            authenticatedUser = null;
            dispose();
        }
        
        public User getAuthenticatedUser() { return authenticatedUser; }
    }

    private static class AdminDashboard extends JFrame {
        private final User currentUser;
        private final DataService dataService = DataService.getInstance();
        private JList<FormDefinition> formList;
        private DefaultListModel<FormDefinition> listModel;
        private JButton viewFeedbackBtn;

        public AdminDashboard(User user) {
            this.currentUser = user;
            setTitle("Admin Dashboard - (" + currentUser.getUsername() + ")");
            setSize(900, 650);
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            setLocationRelativeTo(null);
            
            JPanel mainPanel = new JPanel(new BorderLayout(15, 15));
            mainPanel.setBackground(BG_COLOR);
            mainPanel.setBorder(new EmptyBorder(15, 15, 15, 15));
            
            JLabel header = new JLabel("Welcome, Admin " + currentUser.getUsername(), SwingConstants.CENTER);
            header.setFont(new Font("Segoe UI", Font.BOLD, 22));
            mainPanel.add(header, BorderLayout.NORTH);

            mainPanel.add(createFormManagementPanel(), BorderLayout.CENTER);
            mainPanel.add(createQuickActionsPanel(), BorderLayout.WEST);

            setContentPane(mainPanel);
        }

        private JPanel createQuickActionsPanel() {
            JPanel panel = new JPanel(new GridLayout(6, 1, 10, 10));
            panel.setBackground(Color.WHITE);
            panel.setBorder(BorderFactory.createTitledBorder("Admin Tools"));
            
            JButton manageUsersBtn = new JButton("Manage Users");
            styleButton(manageUsersBtn, BTN_YELLOW); // Solid Yellow
            manageUsersBtn.addActionListener(e -> new UserManagerDialog(this, currentUser).setVisible(true));
            panel.add(manageUsersBtn);

            JButton saveToFileBtn = new JButton("Export Feedback");
            styleButton(saveToFileBtn, BTN_BLUE); // Solid Blue
            saveToFileBtn.addActionListener(e -> saveFeedbackToFile());
            panel.add(saveToFileBtn);
            
            JButton clearDataBtn = new JButton("Clear All Data");
            styleButton(clearDataBtn, BTN_RED); // Solid Red
            clearDataBtn.addActionListener(e -> clearAllFeedback());
            panel.add(clearDataBtn);
            
            JButton logoutBtn = new JButton("Logout");
            styleButton(logoutBtn, BTN_GRAY); // Solid Gray
            logoutBtn.addActionListener(e -> logout());
            panel.add(logoutBtn);
            
            return panel;
        }

        private JPanel createFormManagementPanel() {
            JPanel panel = new JPanel(new BorderLayout(10, 10));
            panel.setBackground(Color.WHITE);
            panel.setBorder(BorderFactory.createTitledBorder("Form Management"));
            
            listModel = new DefaultListModel<>();
            dataService.getForms().forEach(listModel::addElement);
            formList = new JList<>(listModel);
            
            panel.add(new JScrollPane(formList), BorderLayout.CENTER);
            
            JPanel buttonPanel = new JPanel(new FlowLayout());
            buttonPanel.setBackground(Color.WHITE);
            
            JButton addBtn = new JButton("Create Form");
            styleButton(addBtn, BTN_GREEN); // Solid Green
            
            viewFeedbackBtn = new JButton("View Feedback");
            styleButton(viewFeedbackBtn, BTN_PURPLE); // Solid Purple
            
            JButton delBtn = new JButton("Delete Form");
            styleButton(delBtn, BTN_RED); // Solid Red
            
            viewFeedbackBtn.setEnabled(false);
            
            buttonPanel.add(addBtn);
            buttonPanel.add(viewFeedbackBtn);
            buttonPanel.add(delBtn);
            
            formList.addListSelectionListener(e -> {
                if (!e.getValueIsAdjusting()) {
                    viewFeedbackBtn.setEnabled(formList.getSelectedIndex() != -1);
                }
            });

            addBtn.addActionListener(e -> addNewForm());
            viewFeedbackBtn.addActionListener(e -> viewSelectedFormFeedback());
            delBtn.addActionListener(e -> deleteSelectedForm());
            
            panel.add(buttonPanel, BorderLayout.SOUTH);
            return panel;
        }

        private void addNewForm() {
            JTextField titleField = new JTextField();
            JTextArea descArea = new JTextArea(5, 20);
            descArea.setLineWrap(true);
            JTextField[] ratingFields = new JTextField[5];
            
            JPanel fieldsPanel = new JPanel(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(5, 5, 5, 5); gbc.fill = GridBagConstraints.HORIZONTAL;
            
            gbc.gridx=0; gbc.gridy=0; fieldsPanel.add(new JLabel("Title:"), gbc);
            gbc.gridx=1; fieldsPanel.add(titleField, gbc);
            gbc.gridx=0; gbc.gridy=1; fieldsPanel.add(new JLabel("Desc:"), gbc);
            gbc.gridx=1; fieldsPanel.add(new JScrollPane(descArea), gbc);

            for(int i=0; i<5; i++) {
                 gbc.gridy = 2 + i; gbc.gridx = 0;
                 fieldsPanel.add(new JLabel("Rating Cat " + (i+1) + ":"), gbc);
                 ratingFields[i] = new JTextField();
                 gbc.gridx = 1; fieldsPanel.add(ratingFields[i], gbc);
            }

            fieldsPanel.setPreferredSize(new Dimension(400, 350));
            int result = JOptionPane.showConfirmDialog(this, fieldsPanel, "Create New Form", JOptionPane.OK_CANCEL_OPTION);
            
            if (result == JOptionPane.OK_OPTION) {
                String title = titleField.getText();
                String desc = descArea.getText();
                List<String> categories = new ArrayList<>();
                for (JTextField field : ratingFields) {
                    if (field.getText() != null && !field.getText().trim().isEmpty()) {
                        categories.add(field.getText().trim());
                    }
                }
                if (title != null && !title.isEmpty() && !categories.isEmpty()) {
                    dataService.addForm(title, desc, categories);
                    refreshFormList();
                }
            }
        }
        
        private void viewSelectedFormFeedback() {
            FormDefinition selected = formList.getSelectedValue();
            if (selected != null) new FeedbackManagerDialog(this, selected).setVisible(true);
        }
        
        private void deleteSelectedForm() {
            FormDefinition selected = formList.getSelectedValue();
            if (selected != null) {
                int confirm = JOptionPane.showConfirmDialog(this, "Delete " + selected.getTitle() + "?", "Confirm", JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    dataService.deleteForm(selected);
                    refreshFormList();
                }
            }
        }
        
        private void refreshFormList() {
            listModel.clear();
            dataService.getForms().forEach(listModel::addElement);
        }

        private void clearAllFeedback() {
            int response = JOptionPane.showConfirmDialog(this, "Are you sure? This deletes ALL feedback.", "Confirm", JOptionPane.YES_NO_OPTION);
            if (response == JOptionPane.YES_OPTION) dataService.clearAllFeedback();
        }

        private void saveFeedbackToFile() {
             JFileChooser fileChooser = new JFileChooser();
             fileChooser.setSelectedFile(new File("feedback_export.txt"));
             if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                 try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileChooser.getSelectedFile()))) {
                     for (Feedback fb : dataService.getFeedback()) writer.write(fb.toString());
                     JOptionPane.showMessageDialog(this, "Saved successfully.");
                 } catch (IOException ex) { ex.printStackTrace(); }
             }
        }
        
        private void logout() {
            this.dispose();
            SwingUtilities.invokeLater(FeedbackSystem::showRoleSelectionScreen);
        }
    }

    private static class FeedbackForm extends JFrame {
        private final DataService dataService = DataService.getInstance();
        private final User currentUser;
        private final FormDefinition form;
        private final JTextField nameField, emailField;
        private final JTextArea commentsArea;
        private final Map<String, JComboBox<Integer>> ratingComboBoxes;

        public FeedbackForm(User user, FormDefinition form) {
            this.currentUser = user;
            this.form = form;
            this.ratingComboBoxes = new HashMap<>();

            setTitle("Feedback: " + form.getTitle());
            setSize(600, 700);
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            setLocationRelativeTo(null);
            
            JPanel mainPanel = new JPanel(new BorderLayout(15, 15));
            mainPanel.setBackground(BG_COLOR);
            mainPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

            JLabel titleLabel = new JLabel(form.getTitle(), SwingConstants.CENTER);
            titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
            mainPanel.add(titleLabel, BorderLayout.NORTH);

            JPanel formPanel = new JPanel(new GridBagLayout());
            formPanel.setBackground(Color.WHITE);
            formPanel.setBorder(new LineBorder(Color.GRAY, 1));
            
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(8, 8, 8, 8); gbc.fill = GridBagConstraints.HORIZONTAL;
            
            gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
            JLabel desc = new JLabel("<html><body style='width: 350px'>" + form.getDescription() + "</body></html>");
            desc.setFont(new Font("Segoe UI", Font.ITALIC, 14));
            formPanel.add(desc, gbc);

            gbc.gridwidth = 1;
            gbc.gridx = 0; gbc.gridy = 1; formPanel.add(new JLabel("Name:"), gbc);
            nameField = new JTextField(currentUser.getUsername(), 20);
            gbc.gridx = 1; formPanel.add(nameField, gbc);

            gbc.gridx = 0; gbc.gridy = 2; formPanel.add(new JLabel("Email:"), gbc);
            emailField = new JTextField(20);
            gbc.gridx = 1; formPanel.add(emailField, gbc);
            
            int y = 3;
            Integer[] ratings = {1, 2, 3, 4, 5};
            for (String category : form.getRatingCategories()) {
                gbc.gridx = 0; gbc.gridy = y;
                formPanel.add(new JLabel(category + " (1-5):"), gbc);
                JComboBox<Integer> box = new JComboBox<>(ratings);
                box.setSelectedIndex(4);
                gbc.gridx = 1; formPanel.add(box, gbc);
                ratingComboBoxes.put(category, box);
                y++;
            }

            gbc.gridx = 0; gbc.gridy = y; gbc.anchor = GridBagConstraints.NORTH;
            formPanel.add(new JLabel("Comments:"), gbc);
            commentsArea = new JTextArea(5, 20);
            commentsArea.setLineWrap(true);
            gbc.gridx = 1; formPanel.add(new JScrollPane(commentsArea), gbc);

            mainPanel.add(new JScrollPane(formPanel), BorderLayout.CENTER);

            JPanel btnPanel = new JPanel();
            btnPanel.setBackground(BG_COLOR);
            
            JButton submitBtn = new JButton("Submit");
            styleButton(submitBtn, BTN_GREEN); // Solid Green
            submitBtn.addActionListener(e -> submitFeedback());
            
            JButton logoutBtn = new JButton("Logout");
            styleButton(logoutBtn, BTN_RED); // Solid Red
            logoutBtn.addActionListener(e -> logout());

            btnPanel.add(logoutBtn);
            btnPanel.add(submitBtn);
            mainPanel.add(btnPanel, BorderLayout.SOUTH);

            setContentPane(mainPanel);
        }

        private void logout() {
            this.dispose();
            SwingUtilities.invokeLater(FeedbackSystem::showRoleSelectionScreen);
        }

        private void submitFeedback() {
             String email = emailField.getText();
             if (email.isEmpty() || !email.contains("@")) {
                 JOptionPane.showMessageDialog(this, "Invalid Email.");
                 return;
             }
             Map<String, Integer> ratings = new HashMap<>();
             ratingComboBoxes.forEach((k, v) -> ratings.put(k, (Integer) v.getSelectedItem()));
             
             Feedback fb = new Feedback(nameField.getText(), email, ratings, commentsArea.getText(), form.getId(), form.getTitle());
             dataService.addFeedback(fb);
             JOptionPane.showMessageDialog(this, "Thank you!");
             this.dispose();
             SwingUtilities.invokeLater(FeedbackSystem::showFormSelectionScreen);
        }
    }

    private static class UserManagerDialog extends JDialog {
        public UserManagerDialog(Frame parent, User currentUser) {
            super(parent, "Manage Users", true);
            setSize(500, 400);
            setLocationRelativeTo(parent);
            JTable table = new JTable(new DefaultTableModel(new Object[]{"User", "Role"}, 0));
            add(new JScrollPane(table));
        }
    }

    private static class FeedbackManagerDialog extends JDialog {
        private final DataService dataService = DataService.getInstance();
        private DefaultTableModel tableModel;
        private TableRowSorter<DefaultTableModel> sorter;
        private JTextField searchField;
        private final FormDefinition form;
        private JTable feedbackTable;

        public FeedbackManagerDialog(Frame parent, FormDefinition form) {
            super(parent, "Feedback for: " + form.getTitle(), true);
            this.form = form;
            setSize(900, 600);
            setLocationRelativeTo(parent);
            getContentPane().setBackground(BG_COLOR);

            JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            filterPanel.setBackground(Color.WHITE);
            filterPanel.add(new JLabel("Search by Name: "));
            searchField = new JTextField(20);
            filterPanel.add(searchField);
            
            JButton applyBtn = new JButton("Filter");
            styleButton(applyBtn, BTN_PURPLE); // Solid Purple
            applyBtn.addActionListener(e -> applyFilters());
            filterPanel.add(applyBtn);
            add(filterPanel, BorderLayout.NORTH);

            tableModel = new DefaultTableModel(new String[]{"Name", "Email", "Ratings", "Comments", "OBJ"}, 0) {
                public boolean isCellEditable(int row, int column) { return false; }
            };
            
            feedbackTable = new JTable(tableModel);
            feedbackTable.setRowHeight(30);
            
            sorter = new TableRowSorter<>(tableModel);
            feedbackTable.setRowSorter(sorter);
            feedbackTable.removeColumn(feedbackTable.getColumnModel().getColumn(4));
            
            feedbackTable.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2) {
                        int row = feedbackTable.getSelectedRow();
                        if (row != -1) {
                            Feedback fb = (Feedback) tableModel.getValueAt(feedbackTable.convertRowIndexToModel(row), 4);
                            JOptionPane.showMessageDialog(FeedbackManagerDialog.this, new JScrollPane(new JTextArea(fb.toString())));
                        }
                    }
                }
            });

            add(new JScrollPane(feedbackTable), BorderLayout.CENTER);
            refreshTable();
        }
        
        private void refreshTable() {
            tableModel.setRowCount(0);
            for (Feedback fb : dataService.getFeedback()) {
                if (fb.getFormId().equals(form.getId())) {
                    StringBuilder ratings = new StringBuilder();
                    fb.getRatings().forEach((k, v) -> ratings.append(k).append(":").append(v).append(" "));
                    tableModel.addRow(new Object[]{fb.getUserName(), fb.getUserEmail(), ratings.toString(), fb.getComments(), fb});
                }
            }
        }
        
        private void applyFilters() {
             String text = searchField.getText();
             if (text.isEmpty()) sorter.setRowFilter(null);
             else sorter.setRowFilter(RowFilter.regexFilter("(?i)" + text, 0));
        }
    }
}