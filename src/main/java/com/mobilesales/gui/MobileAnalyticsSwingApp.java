package com.mobilesales.gui;

import com.mobilesales.analytics.*;
import com.mobilesales.dao.*;
import com.mobilesales.exception.DataValidationException;
import com.mobilesales.exception.InvalidLoginException;
import com.mobilesales.io.CSVDataExporter;
import com.mobilesales.io.CSVDataImporter;
import com.mobilesales.io.ReportGenerator;
import com.mobilesales.model.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Native Java Swing GUI Implementation.
 * Demonstrates Java Desktop GUI, CardLayout, JTabbedPane, JTable, Event Listeners, and Dialogs.
 * Implements all 12 requested screens with a sleek modern dark theme.
 */
public class MobileAnalyticsSwingApp extends JFrame {

    private static final Logger LOGGER = Logger.getLogger(MobileAnalyticsSwingApp.class.getName());

    // DAOs
    private final UserDAO userDAO = new UserDAO();
    private final MobileProductDAO productDAO = new MobileProductDAO();
    private final SalesDAO salesDAO = new SalesDAO();
    private final InventoryDAO inventoryDAO = new InventoryDAO();
    private final AISearchDAO aiDAO = new AISearchDAO();
    private final AnalyticsDAO analyticsDAO = new AnalyticsDAO();
    private final RecommendationDAO recommendationDAO = new RecommendationDAO();

    // Analytics engines
    private final SalesAnalytics salesAnalytics = new SalesAnalytics();
    private final InventoryAnalytics inventoryAnalytics = new InventoryAnalytics();
    private final AIAnalytics aiAnalytics = new AIAnalytics();
    private final DemandPrediction demandPrediction = new DemandPrediction();
    private final ProductClassifier productClassifier = new ProductClassifier();
    private final RecommendationEngine recommendationEngine = new RecommendationEngine();

    // UI Navigation
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel mainContainer = new JPanel(cardLayout);

    // Current session
    private User currentUser = null;
    private String selectedBrand = null;
    private MobileProduct selectedProduct = null;

    // GUI Components
    private JComboBox<String> brandComboBox;
    private JComboBox<String> modelComboBox;
    private JLabel userBadgeLabel;

    // Colors
    private static final Color BG_DARK = new Color(15, 23, 42);       // Slate 900
    private static final Color CARD_BG = new Color(30, 41, 59);       // Slate 800
    private static final Color ACCENT_BLUE = new Color(59, 130, 246);  // Blue 500
    private static final Color ACCENT_CYAN = new Color(6, 182, 212);   // Cyan 500
    private static final Color ACCENT_GREEN = new Color(16, 185, 129); // Emerald 500
    private static final Color TEXT_LIGHT = new Color(248, 250, 252);  // Slate 50
    private static final Color TEXT_MUTED = new Color(148, 163, 184); // Slate 400

    public MobileAnalyticsSwingApp() {
        super("AI-Enhanced Mobile Inventory & Sales Analytics (Java Swing)");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(1180, 820);
        setMinimumSize(new Dimension(960, 680));
        setLocationRelativeTo(null);

        initTheme();
        initUI();
    }

    public static void launchApp() {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}
            MobileAnalyticsSwingApp app = new MobileAnalyticsSwingApp();
            app.setVisible(true);
        });
    }

    private void initTheme() {
        mainContainer.setBackground(BG_DARK);
    }

    private void initUI() {
        // Build the cards
        mainContainer.add(createLoginPanel(), "LOGIN");
        mainContainer.add(createRegisterPanel(), "REGISTER");
        mainContainer.add(createHomePanel(), "HOME");
        mainContainer.add(createProductDashboardPanel(), "PRODUCT_DASHBOARD");
        mainContainer.add(createExecutiveDashboardPanel(), "EXECUTIVE_DASHBOARD");

        add(mainContainer);
        cardLayout.show(mainContainer, "LOGIN");
    }

    // ==========================================
    // 1. LOGIN SCREEN
    // ==========================================
    private JPanel createLoginPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(BG_DARK);

        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(CARD_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(51, 65, 85), 1, true),
                new EmptyBorder(35, 45, 35, 45)
        ));
        card.setPreferredSize(new Dimension(460, 520));

        JLabel title = new JLabel("AI Mobile Analytics");
        title.setFont(new Font("Segoe UI", Font.BOLD, 26));
        title.setForeground(TEXT_LIGHT);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitle = new JLabel("Inventory, Sales & Reorder Intelligence");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        subtitle.setForeground(TEXT_MUTED);
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        card.add(title);
        card.add(Box.createVerticalStrut(6));
        card.add(subtitle);
        card.add(Box.createVerticalStrut(30));

        JTextField usernameField = new JTextField("admin");
        styleTextField(usernameField);
        JPasswordField passwordField = new JPasswordField("password123");
        styleTextField(passwordField);

        card.add(createFieldLabel("Username"));
        card.add(Box.createVerticalStrut(4));
        card.add(usernameField);
        card.add(Box.createVerticalStrut(15));

        card.add(createFieldLabel("Password"));
        card.add(Box.createVerticalStrut(4));
        card.add(passwordField);
        card.add(Box.createVerticalStrut(25));

        JButton loginBtn = createPrimaryButton("Login to System");
        loginBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(loginBtn);
        card.add(Box.createVerticalStrut(12));

        JPanel linkPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
        linkPanel.setBackground(CARD_BG);

        JButton registerLink = createLinkButton("Create New Account");
        JButton forgotBtn = createLinkButton("Forgot Password?");

        linkPanel.add(registerLink);
        linkPanel.add(new JLabel("•") {{ setForeground(TEXT_MUTED); }});
        linkPanel.add(forgotBtn);
        card.add(linkPanel);

        // Actions
        loginBtn.addActionListener(e -> {
            String u = usernameField.getText();
            String p = new String(passwordField.getPassword());
            try {
                currentUser = userDAO.authenticate(u, p);
                JOptionPane.showMessageDialog(this, "Welcome back, " + currentUser.getName() + "!",
                        "Authentication Successful", JOptionPane.INFORMATION_MESSAGE);
                loadHomeData();
                cardLayout.show(mainContainer, "HOME");
            } catch (InvalidLoginException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Authentication Error", JOptionPane.ERROR_MESSAGE);
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Database Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        registerLink.addActionListener(e -> cardLayout.show(mainContainer, "REGISTER"));
        forgotBtn.addActionListener(e -> JOptionPane.showMessageDialog(this,
                "Demo accounts initialized:\n- Username: admin / Password: password123\n- Username: analyst / Password: analyst123",
                "Password Recovery", JOptionPane.INFORMATION_MESSAGE));

        panel.add(card);
        return panel;
    }

    // ==========================================
    // 2. REGISTRATION SCREEN
    // ==========================================
    private JPanel createRegisterPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(BG_DARK);

        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(CARD_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(51, 65, 85), 1, true),
                new EmptyBorder(30, 45, 30, 45)
        ));
        card.setPreferredSize(new Dimension(480, 600));

        JLabel title = new JLabel("Register New User");
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setForeground(TEXT_LIGHT);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(title);
        card.add(Box.createVerticalStrut(20));

        JTextField nameField = new JTextField(); styleTextField(nameField);
        JTextField usernameField = new JTextField(); styleTextField(usernameField);
        JTextField emailField = new JTextField(); styleTextField(emailField);
        JPasswordField passField = new JPasswordField(); styleTextField(passField);
        JPasswordField confirmField = new JPasswordField(); styleTextField(confirmField);

        card.add(createFieldLabel("Full Name")); card.add(nameField); card.add(Box.createVerticalStrut(10));
        card.add(createFieldLabel("Username")); card.add(usernameField); card.add(Box.createVerticalStrut(10));
        card.add(createFieldLabel("Email")); card.add(emailField); card.add(Box.createVerticalStrut(10));
        card.add(createFieldLabel("Password")); card.add(passField); card.add(Box.createVerticalStrut(10));
        card.add(createFieldLabel("Confirm Password")); card.add(confirmField); card.add(Box.createVerticalStrut(20));

        JButton registerBtn = createPrimaryButton("Register Account");
        JButton backBtn = createSecondaryButton("Back to Login");

        card.add(registerBtn);
        card.add(Box.createVerticalStrut(10));
        card.add(backBtn);

        registerBtn.addActionListener(e -> {
            try {
                String pass = new String(passField.getPassword());
                String conf = new String(confirmField.getPassword());
                if (!pass.equals(conf)) {
                    throw new DataValidationException("Passwords do not match!");
                }
                User user = new User(nameField.getText(), usernameField.getText(), emailField.getText(), pass);
                userDAO.registerUser(user);
                JOptionPane.showMessageDialog(this, "Registration successful! You can now log in.", "Success", JOptionPane.INFORMATION_MESSAGE);
                cardLayout.show(mainContainer, "LOGIN");
            } catch (DataValidationException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Validation Error", JOptionPane.WARNING_MESSAGE);
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        backBtn.addActionListener(e -> cardLayout.show(mainContainer, "LOGIN"));

        panel.add(card);
        return panel;
    }

    // ==========================================
    // 3. HOME & BRAND SELECTION SCREEN
    // ==========================================
    private JPanel createHomePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BG_DARK);

        // Header
        JPanel header = createHeaderPanel("Mobile Inventory & Sales Analytics", "Select Brand & Explore Mobile Models");
        panel.add(header, BorderLayout.NORTH);

        // Body
        JPanel body = new JPanel(new GridBagLayout());
        body.setBackground(BG_DARK);

        JPanel selectionCard = new JPanel();
        selectionCard.setLayout(new BoxLayout(selectionCard, BoxLayout.Y_AXIS));
        selectionCard.setBackground(CARD_BG);
        selectionCard.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(51, 65, 85), 1, true),
                new EmptyBorder(30, 40, 30, 40)
        ));
        selectionCard.setPreferredSize(new Dimension(560, 380));

        JLabel step1 = new JLabel("STEP 1: Select Mobile Brand");
        step1.setFont(new Font("Segoe UI", Font.BOLD, 16));
        step1.setForeground(ACCENT_CYAN);
        selectionCard.add(step1);
        selectionCard.add(Box.createVerticalStrut(8));

        brandComboBox = new JComboBox<>();
        brandComboBox.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        selectionCard.add(brandComboBox);
        selectionCard.add(Box.createVerticalStrut(25));

        JLabel step2 = new JLabel("STEP 2: Select Mobile Model");
        step2.setFont(new Font("Segoe UI", Font.BOLD, 16));
        step2.setForeground(ACCENT_CYAN);
        selectionCard.add(step2);
        selectionCard.add(Box.createVerticalStrut(8));

        modelComboBox = new JComboBox<>();
        modelComboBox.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        selectionCard.add(modelComboBox);
        selectionCard.add(Box.createVerticalStrut(30));

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
        btnPanel.setBackground(CARD_BG);

        JButton viewDashboardBtn = createPrimaryButton("Open Product Dashboard");
        JButton viewAllBtn = createSecondaryButton("Executive Dashboard");

        btnPanel.add(viewDashboardBtn);
        btnPanel.add(viewAllBtn);
        selectionCard.add(btnPanel);

        // Brand change listener updates models dropdown
        brandComboBox.addActionListener(e -> {
            String b = (String) brandComboBox.getSelectedItem();
            if (b != null) {
                selectedBrand = b;
                loadModelsForBrand(b);
            }
        });

        viewDashboardBtn.addActionListener(e -> {
            String model = (String) modelComboBox.getSelectedItem();
            if (model != null && selectedBrand != null) {
                loadProductDashboard(selectedBrand, model);
                cardLayout.show(mainContainer, "PRODUCT_DASHBOARD");
            }
        });

        viewAllBtn.addActionListener(e -> {
            loadExecutiveDashboardData();
            cardLayout.show(mainContainer, "EXECUTIVE_DASHBOARD");
        });

        body.add(selectionCard);
        panel.add(body, BorderLayout.CENTER);
        return panel;
    }

    // ==========================================
    // 4. PRODUCT DASHBOARD (All tabs: Specs, Sales, AI, Inventory, Demand, Reorder)
    // ==========================================
    private JPanel productDashboardContainer;

    private JPanel createProductDashboardPanel() {
        productDashboardContainer = new JPanel(new BorderLayout());
        productDashboardContainer.setBackground(BG_DARK);
        return productDashboardContainer;
    }

    private void loadProductDashboard(String brand, String model) {
        productDashboardContainer.removeAll();

        try {
            List<MobileProduct> products = productDAO.getProductsByBrand(brand);
            for (MobileProduct p : products) {
                if (p.getModelName().equalsIgnoreCase(model)) {
                    selectedProduct = p;
                    break;
                }
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error fetching product: " + ex.getMessage());
            return;
        }

        if (selectedProduct == null) {
            JOptionPane.showMessageDialog(this, "Product not found.");
            return;
        }

        int pid = selectedProduct.getProductId();
        Sales sales = null;
        Inventory inventory = null;
        AISearchData aiData = null;
        try {
            sales = salesDAO.getSalesByProductId(pid);
            inventory = inventoryDAO.getInventoryByProductId(pid);
            aiData = aiDAO.getAISearchDataByProductId(pid);
        } catch (SQLException ignored) {}

        DemandPredictionResult demandResult = demandPrediction.predictDemand(sales, inventory, aiData);
        ProductClassifier.ClassificationOutput classification = productClassifier.classifyProduct(sales, aiData);
        ReorderRecommendation rec = recommendationEngine.generateRecommendation(sales, inventory, aiData, demandResult, classification);

        // Header with Back Button
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(new Color(24, 33, 47));
        topBar.setBorder(new EmptyBorder(15, 25, 15, 25));

        JPanel titleGrp = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        titleGrp.setBackground(new Color(24, 33, 47));

        JButton backBtn = createSecondaryButton("← Back to Models");
        backBtn.addActionListener(e -> cardLayout.show(mainContainer, "HOME"));

        JLabel pTitle = new JLabel(selectedProduct.getBrand() + " " + selectedProduct.getModelName());
        pTitle.setFont(new Font("Segoe UI", Font.BOLD, 22));
        pTitle.setForeground(TEXT_LIGHT);

        JLabel classBadge = new JLabel("  " + classification.getClassification().getLabel() + "  ");
        classBadge.setFont(new Font("Segoe UI", Font.BOLD, 12));
        classBadge.setOpaque(true);
        classBadge.setBackground(Color.decode(classification.getClassification().getColor()));
        classBadge.setForeground(Color.BLACK);

        titleGrp.add(backBtn);
        titleGrp.add(pTitle);
        titleGrp.add(classBadge);
        topBar.add(titleGrp, BorderLayout.WEST);

        JLabel priceTag = new JLabel("$" + String.format("%.2f", selectedProduct.getPrice()));
        priceTag.setFont(new Font("Segoe UI", Font.BOLD, 22));
        priceTag.setForeground(ACCENT_GREEN);
        topBar.add(priceTag, BorderLayout.EAST);

        productDashboardContainer.add(topBar, BorderLayout.NORTH);

        // Tabbed Pane for full depth
        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("Segoe UI", Font.BOLD, 13));
        tabs.setBackground(BG_DARK);
        tabs.setForeground(TEXT_LIGHT);

        tabs.addTab("Product Specifications", createSpecsTab(selectedProduct));
        tabs.addTab("Sales Analytics", createSalesTab(sales));
        tabs.addTab("Inventory Analytics", createInventoryTab(inventory));
        tabs.addTab("AI Search Intelligence", createAISearchTab(aiData));
        tabs.addTab("Demand Analysis", createDemandTab(demandResult, sales, aiData));
        tabs.addTab("Smart Reorder Recommendation", createReorderTab(rec, classification));

        productDashboardContainer.add(tabs, BorderLayout.CENTER);
        productDashboardContainer.revalidate();
        productDashboardContainer.repaint();
    }

    private JPanel createSpecsTab(MobileProduct p) {
        JPanel panel = new JPanel(new GridLayout(4, 2, 20, 20));
        panel.setBackground(BG_DARK);
        panel.setBorder(new EmptyBorder(30, 40, 30, 40));

        panel.add(createSpecCard("Processor & Chipset", p.getProcessor(), "fas fa-microchip"));
        panel.add(createSpecCard("RAM & Internal Storage", p.getRam() + " RAM / " + p.getStorage(), "fas fa-memory"));
        panel.add(createSpecCard("Display Panel", p.getDisplay(), "fas fa-mobile-alt"));
        panel.add(createSpecCard("Camera System", p.getCamera(), "fas fa-camera"));
        panel.add(createSpecCard("Battery & Charging", p.getBattery(), "fas fa-battery-full"));
        panel.add(createSpecCard("Operating System", p.getOs(), "fas fa-code-branch"));
        panel.add(createSpecCard("Connectivity & Network", p.getNetwork(), "fas fa-wifi"));
        panel.add(createSpecCard("Launch Date", p.getLaunchDate() != null ? p.getLaunchDate().toString() : "Recent", "fas fa-calendar"));

        return panel;
    }

    private JPanel createSpecCard(String label, String value, String icon) {
        JPanel card = new JPanel(new BorderLayout(10, 10));
        card.setBackground(CARD_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(51, 65, 85), 1, true),
                new EmptyBorder(15, 20, 15, 20)
        ));

        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lbl.setForeground(TEXT_MUTED);

        JLabel val = new JLabel("<html>" + (value != null ? value : "Standard Spec") + "</html>");
        val.setFont(new Font("Segoe UI", Font.BOLD, 15));
        val.setForeground(TEXT_LIGHT);

        card.add(lbl, BorderLayout.NORTH);
        card.add(val, BorderLayout.CENTER);
        return card;
    }

    private JPanel createSalesTab(Sales s) {
        JPanel panel = new JPanel(new GridLayout(2, 3, 20, 20));
        panel.setBackground(BG_DARK);
        panel.setBorder(new EmptyBorder(30, 40, 30, 40));

        panel.add(createMetricCard("Total Units Sold", s != null ? String.format("%,d units", s.getTotalUnitsSold()) : "0", ACCENT_CYAN));
        panel.add(createMetricCard("Current Weekly Sales", s != null ? s.getCurrentWeeklySales() + " units" : "0", ACCENT_BLUE));
        panel.add(createMetricCard("Current Monthly Sales", s != null ? s.getCurrentMonthlySales() + " units" : "0", ACCENT_GREEN));
        panel.add(createMetricCard("Previous Month Sales", s != null ? s.getPreviousMonthSales() + " units" : "0", TEXT_LIGHT));
        panel.add(createMetricCard("Sales Growth %", s != null ? String.format("%+.1f%%", s.getSalesGrowthPct()) : "0%",
                (s != null && s.getSalesGrowthPct() >= 0) ? ACCENT_GREEN : Color.RED));
        panel.add(createMetricCard("Sales Trend Trajectory", s != null ? s.getSalesTrend() : "Stable", ACCENT_CYAN));

        return panel;
    }

    private JPanel createInventoryTab(Inventory inv) {
        JPanel panel = new JPanel(new GridLayout(2, 3, 20, 20));
        panel.setBackground(BG_DARK);
        panel.setBorder(new EmptyBorder(30, 40, 30, 40));

        panel.add(createMetricCard("Current Stock in Warehouse", inv != null ? inv.getCurrentStock() + " units" : "0", ACCENT_CYAN));
        panel.add(createMetricCard("Safety Stock Threshold", inv != null ? inv.getSafetyStock() + " units" : "0", Color.ORANGE));
        panel.add(createMetricCard("Calculated Reorder Level", inv != null ? inv.getReorderLevel() + " units" : "0", Color.YELLOW));
        panel.add(createMetricCard("Supplier Lead Time", inv != null ? inv.getSupplierLeadTimeDays() + " days" : "0 days", TEXT_LIGHT));
        panel.add(createMetricCard("Available Stock", inv != null ? inv.getAvailableStock() + " units" : "0", ACCENT_GREEN));

        String statusStr = inv != null && inv.getInventoryStatus() != null ? inv.getInventoryStatus().getDisplayName() : "In Stock";
        Color statusCol = (inv != null && inv.getInventoryStatus() != null) ? Color.decode(inv.getInventoryStatus().getColorHex()) : ACCENT_GREEN;
        panel.add(createMetricCard("Inventory Health Status", statusStr, statusCol));

        return panel;
    }

    private JPanel createAISearchTab(AISearchData ai) {
        JPanel panel = new JPanel(new GridLayout(2, 3, 20, 20));
        panel.setBackground(BG_DARK);
        panel.setBorder(new EmptyBorder(30, 40, 30, 40));

        panel.add(createMetricCard("AI Visibility Score", ai != null ? String.format("%.1f%%", ai.getVisibilityScore()) : "0%", ACCENT_CYAN));
        panel.add(createMetricCard("Search Appearances", ai != null ? String.format("%d / %d queries", ai.getProductAppearances(), ai.getTotalQueries()) : "0", ACCENT_BLUE));
        panel.add(createMetricCard("Brand Mentions Ratio", ai != null ? ai.getBrandMentions() + " mentions" : "0", ACCENT_GREEN));
        panel.add(createMetricCard("Information Accuracy Score", ai != null ? String.format("%.1f%%", ai.getAccuracyScore()) : "100%", ACCENT_GREEN));
        panel.add(createMetricCard("Customer Engagement Score", ai != null ? String.format("%.1f%%", ai.getEngagementScore()) : "0%", ACCENT_CYAN));
        panel.add(createMetricCard("High-Intent Repeat Searches", ai != null ? ai.getRepeatSearches() + " queries" : "0", ACCENT_BLUE));

        return panel;
    }

    private JPanel createDemandTab(DemandPredictionResult dem, Sales s, AISearchData ai) {
        JPanel panel = new JPanel(new BorderLayout(20, 20));
        panel.setBackground(BG_DARK);
        panel.setBorder(new EmptyBorder(30, 40, 30, 40));

        JPanel metricsGrid = new JPanel(new GridLayout(2, 2, 20, 20));
        metricsGrid.setBackground(BG_DARK);

        metricsGrid.add(createMetricCard("Current Realized Demand", dem != null ? dem.getCurrentDemand() + " units/mo" : "0", ACCENT_CYAN));
        metricsGrid.add(createMetricCard("AI-Forecasted Demand", dem != null ? dem.getPredictedDemand() + " units/mo" : "0", ACCENT_GREEN));
        metricsGrid.add(createMetricCard("Demand Growth Velocity", dem != null ? String.format("%+.1f%%", dem.getDemandGrowthPct()) : "0%", ACCENT_BLUE));
        metricsGrid.add(createMetricCard("Demand Momentum Trend", dem != null ? dem.getDemandTrend() : "Stable", Color.ORANGE));

        panel.add(metricsGrid, BorderLayout.NORTH);

        JPanel insightCard = new JPanel(new BorderLayout(10, 10));
        insightCard.setBackground(CARD_BG);
        insightCard.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(51, 65, 85), 1, true),
                new EmptyBorder(20, 25, 20, 25)
        ));

        JLabel insTitle = new JLabel("Data Science & AI Signal Synthesis:");
        insTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        insTitle.setForeground(ACCENT_CYAN);

        JTextArea insText = new JTextArea(dem != null ? dem.getAnalyticalInsights() : "Signals balanced.");
        insText.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        insText.setForeground(TEXT_LIGHT);
        insText.setBackground(CARD_BG);
        insText.setLineWrap(true);
        insText.setWrapStyleWord(true);
        insText.setEditable(false);

        insightCard.add(insTitle, BorderLayout.NORTH);
        insightCard.add(insText, BorderLayout.CENTER);

        panel.add(insightCard, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createReorderTab(ReorderRecommendation rec, ProductClassifier.ClassificationOutput classification) {
        JPanel panel = new JPanel(new BorderLayout(20, 20));
        panel.setBackground(BG_DARK);
        panel.setBorder(new EmptyBorder(30, 40, 30, 40));

        JPanel topGrid = new JPanel(new GridLayout(1, 4, 15, 15));
        topGrid.setBackground(BG_DARK);

        topGrid.add(createMetricCard("Current Stock", rec.getCurrentStock() + " units", ACCENT_CYAN));
        topGrid.add(createMetricCard("Predicted Demand", rec.getPredictedDemand() + " units", ACCENT_BLUE));
        topGrid.add(createMetricCard("Reorder Level", rec.getReorderLevel() + " units", Color.YELLOW));
        topGrid.add(createMetricCard("Recommended Order", rec.getRecommendedReorderQty() + " units",
                rec.getRecommendedReorderQty() > 0 ? Color.RED : ACCENT_GREEN));

        panel.add(topGrid, BorderLayout.NORTH);

        // Checklist Box
        JPanel whyCard = new JPanel(new BorderLayout(10, 10));
        whyCard.setBackground(CARD_BG);
        whyCard.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(51, 65, 85), 1, true),
                new EmptyBorder(25, 30, 25, 30)
        ));

        JLabel statusHeader = new JLabel("STATUS: " + rec.getStatus());
        statusHeader.setFont(new Font("Segoe UI", Font.BOLD, 20));
        statusHeader.setForeground("REORDER RECOMMENDED".equalsIgnoreCase(rec.getStatus()) ? Color.RED : ACCENT_GREEN);

        JLabel whyTitle = new JLabel("Audit Rationale Checklist (Why?):");
        whyTitle.setFont(new Font("Segoe UI", Font.BOLD, 15));
        whyTitle.setForeground(TEXT_LIGHT);

        JPanel reasonsPanel = new JPanel();
        reasonsPanel.setLayout(new BoxLayout(reasonsPanel, BoxLayout.Y_AXIS));
        reasonsPanel.setBackground(CARD_BG);

        for (String r : rec.getReasonsList()) {
            JLabel item = new JLabel("✓  " + r);
            item.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            item.setForeground(TEXT_LIGHT);
            item.setBorder(new EmptyBorder(4, 0, 4, 0));
            reasonsPanel.add(item);
        }

        JPanel centerGrp = new JPanel(new BorderLayout(0, 10));
        centerGrp.setBackground(CARD_BG);
        centerGrp.add(whyTitle, BorderLayout.NORTH);
        centerGrp.add(reasonsPanel, BorderLayout.CENTER);

        whyCard.add(statusHeader, BorderLayout.NORTH);
        whyCard.add(centerGrp, BorderLayout.CENTER);

        panel.add(whyCard, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createMetricCard(String title, String val, Color valColor) {
        JPanel card = new JPanel(new BorderLayout(8, 8));
        card.setBackground(CARD_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(51, 65, 85), 1, true),
                new EmptyBorder(16, 20, 16, 20)
        ));

        JLabel t = new JLabel(title);
        t.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        t.setForeground(TEXT_MUTED);

        JLabel v = new JLabel(val);
        v.setFont(new Font("Segoe UI", Font.BOLD, 22));
        v.setForeground(valColor);

        card.add(t, BorderLayout.NORTH);
        card.add(v, BorderLayout.CENTER);
        return card;
    }

    // ==========================================
    // 5. EXECUTIVE DASHBOARD & REPORTS SCREEN
    // ==========================================
    private JPanel execDashboardContainer;

    private JPanel createExecutiveDashboardPanel() {
        execDashboardContainer = new JPanel(new BorderLayout());
        execDashboardContainer.setBackground(BG_DARK);
        return execDashboardContainer;
    }

    private void loadExecutiveDashboardData() {
        execDashboardContainer.removeAll();

        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(new Color(24, 33, 47));
        topBar.setBorder(new EmptyBorder(15, 25, 15, 25));

        JButton backBtn = createSecondaryButton("← Back to Brands");
        backBtn.addActionListener(e -> cardLayout.show(mainContainer, "HOME"));

        JLabel title = new JLabel("Executive Inventory & Reorder Intelligence Dashboard");
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        title.setForeground(TEXT_LIGHT);

        JPanel btnBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnBar.setBackground(new Color(24, 33, 47));

        JButton exportCsvBtn = createPrimaryButton("Export CSV");
        JButton exportReportBtn = createSecondaryButton("Export Audit Report");
        JButton importCsvBtn = createSecondaryButton("Import CSV Data");

        exportCsvBtn.addActionListener(e -> exportCsvAction());
        exportReportBtn.addActionListener(e -> exportReportAction());
        importCsvBtn.addActionListener(e -> importCsvAction());

        btnBar.add(exportCsvBtn);
        btnBar.add(exportReportBtn);
        btnBar.add(importCsvBtn);

        topBar.add(backBtn, BorderLayout.WEST);
        topBar.add(title, BorderLayout.CENTER);
        topBar.add(btnBar, BorderLayout.EAST);

        execDashboardContainer.add(topBar, BorderLayout.NORTH);

        // Table of all products
        String[] cols = {"Brand", "Model", "Price", "Stock", "Monthly Sales", "Predicted Demand", "Visibility", "Accuracy", "Engagement", "Classification", "Reorder Status"};
        DefaultTableModel tableModel = new DefaultTableModel(cols, 0);

        try {
            List<MobileProduct> list = productDAO.getAllProducts();
            for (MobileProduct p : list) {
                int pid = p.getProductId();
                Sales s = salesDAO.getSalesByProductId(pid);
                Inventory inv = inventoryDAO.getInventoryByProductId(pid);
                AISearchData ai = aiDAO.getAISearchDataByProductId(pid);
                DemandPredictionResult dem = demandPrediction.predictDemand(s, inv, ai);
                ProductClassifier.ClassificationOutput cls = productClassifier.classifyProduct(s, ai);
                ReorderRecommendation rec = recommendationEngine.generateRecommendation(s, inv, ai, dem, cls);

                tableModel.addRow(new Object[]{
                        p.getBrand(),
                        p.getModelName(),
                        "$" + p.getPrice(),
                        inv != null ? inv.getCurrentStock() : 0,
                        s != null ? s.getCurrentMonthlySales() : 0,
                        dem != null ? dem.getPredictedDemand() : 0,
                        ai != null ? String.format("%.0f%%", ai.getVisibilityScore()) : "0%",
                        ai != null ? String.format("%.0f%%", ai.getAccuracyScore()) : "100%",
                        ai != null ? String.format("%.0f%%", ai.getEngagementScore()) : "0%",
                        cls.getClassification().getLabel(),
                        rec.getStatus()
                });
            }
        } catch (SQLException ex) {
            LOGGER.severe("Error loading exec table: " + ex.getMessage());
        }

        JTable table = new JTable(tableModel);
        table.setBackground(CARD_BG);
        table.setForeground(TEXT_LIGHT);
        table.setGridColor(new Color(51, 65, 85));
        table.setRowHeight(32);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.getTableHeader().setBackground(new Color(15, 23, 42));
        table.getTableHeader().setForeground(TEXT_LIGHT);
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));

        JScrollPane scroll = new JScrollPane(table);
        scroll.getViewport().setBackground(BG_DARK);
        scroll.setBorder(new EmptyBorder(20, 25, 20, 25));

        execDashboardContainer.add(scroll, BorderLayout.CENTER);
        execDashboardContainer.revalidate();
        execDashboardContainer.repaint();
    }

    private void exportCsvAction() {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("mobile_inventory_analytics.csv"));
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                CSVDataExporter exporter = new CSVDataExporter();
                exporter.exportToFile(chooser.getSelectedFile());
                JOptionPane.showMessageDialog(this, "CSV exported successfully to:\n" + chooser.getSelectedFile().getAbsolutePath(),
                        "Export Complete", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Export Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void exportReportAction() {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("AI_SupplyChain_Audit_Report.md"));
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                ReportGenerator generator = new ReportGenerator();
                generator.saveReportToFile(chooser.getSelectedFile());
                JOptionPane.showMessageDialog(this, "Audit Report saved to:\n" + chooser.getSelectedFile().getAbsolutePath(),
                        "Report Generated", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Report Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void importCsvAction() {
        JFileChooser chooser = new JFileChooser();
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                CSVDataImporter importer = new CSVDataImporter();
                CSVDataImporter.ImportResult res = importer.importCSV(chooser.getSelectedFile());
                JOptionPane.showMessageDialog(this, "Import Complete!\nProducts Added/Analyzed: " + res.getProductsImported() +
                        "\nErrors: " + res.getErrorsEncountered(), "Import Result", JOptionPane.INFORMATION_MESSAGE);
                loadExecutiveDashboardData();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Import Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // ==========================================
    // HELPERS & DATA LOADERS
    // ==========================================
    private void loadHomeData() {
        try {
            List<String> brands = productDAO.getAllBrands();
            brandComboBox.removeAllItems();
            for (String b : brands) {
                brandComboBox.addItem(b);
            }
            if (!brands.isEmpty()) {
                selectedBrand = brands.get(0);
                loadModelsForBrand(selectedBrand);
            }
        } catch (SQLException ex) {
            LOGGER.severe("Error loading brands: " + ex.getMessage());
        }
    }

    private void loadModelsForBrand(String brand) {
        try {
            List<MobileProduct> products = productDAO.getProductsByBrand(brand);
            modelComboBox.removeAllItems();
            for (MobileProduct p : products) {
                modelComboBox.addItem(p.getModelName());
            }
        } catch (SQLException ex) {
            LOGGER.severe("Error loading models: " + ex.getMessage());
        }
    }

    private JPanel createHeaderPanel(String title, String subtitle) {
        JPanel h = new JPanel(new BorderLayout());
        h.setBackground(new Color(24, 33, 47));
        h.setBorder(new EmptyBorder(20, 30, 20, 30));

        JPanel tGroup = new JPanel(new GridLayout(2, 1, 0, 4));
        tGroup.setBackground(new Color(24, 33, 47));

        JLabel t = new JLabel(title);
        t.setFont(new Font("Segoe UI", Font.BOLD, 22));
        t.setForeground(TEXT_LIGHT);

        JLabel st = new JLabel(subtitle);
        st.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        st.setForeground(TEXT_MUTED);

        tGroup.add(t);
        tGroup.add(st);
        h.add(tGroup, BorderLayout.WEST);

        JButton logoutBtn = createSecondaryButton("Log Out");
        logoutBtn.addActionListener(e -> {
            currentUser = null;
            cardLayout.show(mainContainer, "LOGIN");
        });
        h.add(logoutBtn, BorderLayout.EAST);
        return h;
    }

    private void styleTextField(JTextField f) {
        f.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        f.setBackground(new Color(15, 23, 42));
        f.setForeground(TEXT_LIGHT);
        f.setCaretColor(TEXT_LIGHT);
        f.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(71, 85, 105), 1, true),
                new EmptyBorder(8, 12, 8, 12)
        ));
        f.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
    }

    private JLabel createFieldLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.BOLD, 13));
        l.setForeground(TEXT_MUTED);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private JButton createPrimaryButton(String text) {
        JButton b = new JButton(text);
        b.setFont(new Font("Segoe UI", Font.BOLD, 14));
        b.setBackground(ACCENT_BLUE);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(ACCENT_BLUE, 1, true),
                new EmptyBorder(10, 22, 10, 22)
        ));
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return b;
    }

    private JButton createSecondaryButton(String text) {
        JButton b = new JButton(text);
        b.setFont(new Font("Segoe UI", Font.BOLD, 13));
        b.setBackground(new Color(51, 65, 85));
        b.setForeground(TEXT_LIGHT);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(71, 85, 105), 1, true),
                new EmptyBorder(8, 16, 8, 16)
        ));
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return b;
    }

    private JButton createLinkButton(String text) {
        JButton b = new JButton(text);
        b.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        b.setForeground(ACCENT_CYAN);
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return b;
    }
}
