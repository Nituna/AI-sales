# AI-Powered Mobile Inventory, Sales & Reorder Analytics System
**College Data Science + Java Object-Oriented Programming (OOP) Project**

---

## 📱 Project Overview
The **AI-Powered Mobile Inventory, Sales & Reorder Analytics System** is an enterprise-grade supply chain intelligence platform developed for smartphone manufacturers and retail distributors. It enhances traditional inventory control and sales forecasting by fusing **AI Search Intelligence** (search query visibility, information accuracy against ground-truth specs, and customer purchase intent signals).

The system features:
1. **Java OOP Architecture**: Full object-oriented design using Inheritance, Interfaces, Polymorphism, Abstraction, Encapsulation, Custom Exceptions, Collections, File I/O, and JDBC.
2. **Data Science & AI Analytics Engine**:
   - **Visibility Analyzer**: Measures search and AI query appearance rates.
   - **Accuracy Analyzer**: Validates AI claims against verified hardware specifications (battery, chipset, camera, display, memory).
   - **Engagement Analyzer**: Synthesizes customer comparison, repeat queries, and recommendation clicks into a normalized engagement metric.
   - **Demand Forecasting Model**: Combines baseline sales run-rates with AI search pull and lead-time buffering.
   - **Four-Tier Product Classifier**: Classifies phones into *Fast Moving*, *Medium Moving*, *Slow Moving*, and early-warning **Potential Fast Moving** (*"Potential future demand increase detected"*).
   - **Smart Reorder Recommendation**: Automatically computes reorder levels, order quantities, and generates audit checklists explaining *"Why?"*.
3. **Dual User Interface Options**:
   - **Ultra-Modern Dark Glassmorphic Web App**: Running natively on Java's embedded HTTP server with responsive cards, glowing neon indicators, and interactive Chart.js visualizations.
   - **Native Java Swing Desktop GUI**: Desktop UI implementing all 12 requested screens with `CardLayout`, custom tables, metrics, and report export dialogs.

---

## 🚀 Quick Start Guide

### 1. Web Application (Currently Running)
Open your web browser and navigate to:
```
http://localhost:8081
```

#### Demo Credentials:
- **Administrator**: Username: `admin` | Password: `password123`
- **Data Analyst**: Username: `analyst` | Password: `analyst123`
- **Evaluator**: Username: `student` | Password: `student123`

---

### 2. Launching Native Java Swing Desktop GUI
To launch the desktop Java Swing GUI for demonstrations:
```bash
java -cp target/ai-sales-inventory-1.0.0.jar com.mobilesales.gui.MobileAnalyticsSwingApp
```
Or via Maven:
```bash
mvn exec:java -Dexec.mainClass="com.mobilesales.gui.MobileAnalyticsSwingApp"
```

---

### 3. Rebuilding the Standalone Executable Uber-JAR
```bash
mvn clean package -DskipTests
java -jar target/ai-sales-inventory-1.0.0.jar
```

---

## 🏛️ Java Object-Oriented Programming (OOP) Implementation

### 1. Classes & Inheritance
- **Abstract Base Class `Product`** (`com.mobilesales.model.Product`): Defines abstract methods `getProductType()`, `getFormattedSpecifications()`, and `calculateDepreciatedPrice(months)`.
- **Derived Class `MobileProduct`** (`com.mobilesales.model.MobileProduct`): Inherits from `Product`. Adds mobile hardware attributes: RAM, Storage, Processor, Battery, Display, Camera, OS, Network. Overrides abstract methods polymorphically.

### 2. Interfaces & Polymorphism
- **Generic Interface `Analytics<T, R>`** (`com.mobilesales.analytics.Analytics`):
  - `SalesAnalytics implements Analytics<Sales, Map<String, Object>>`
  - `InventoryAnalytics implements Analytics<Inventory, Map<String, Object>>`
  - `AIAnalytics implements Analytics<AISearchData, Map<String, Object>>`

### 3. Encapsulation & Domain Models
- All entity fields are `private` with validated getters and setters.
- Domain models: `User`, `Product`, `MobileProduct`, `Sales`, `Inventory`, `AISearchData`, `DemandPredictionResult`, `ReorderRecommendation`, `ProductClassification`, `InventoryStatus`.

### 4. Custom Exception Handling (`com.mobilesales.exception`)
- `InvalidProductException`: Thrown when product specifications or price values are invalid.
- `InvalidSalesDataException`: Thrown when revenue or sales units are invalid.
- `InsufficientStockException`: Thrown when inventory levels are depleted.
- `InvalidLoginException`: Thrown upon authentication mismatch, prompting user retry.
- `DataValidationException`: Thrown during CSV ingestion or form validation.

### 5. Collections & Java Streams
- Uses `List<MobileProduct>`, `Map<String, Object>`, `Set<String>`, `ArrayList`, `HashMap`, and Java 8+ Stream API for aggregation, filtering, and data transformation.

### 6. Java File I/O (`com.mobilesales.io`)
- `CSVDataImporter`: Parses and validates CSV datasets. Automatically triggers multi-signal analytics without requiring manual user calculations.
- `CSVDataExporter`: Streams consolidated inventory and analytics data to CSV.
- `ReportGenerator`: Generates formal Markdown and HTML Supply Chain Audit reports.

### 7. JDBC Relational Database Layer (`com.mobilesales.dao`)
- `DatabaseConnection`: Singleton JDBC manager with dual database architecture:
  - Connects to **MySQL 8.0** (`jdbc:mysql://localhost:3306/mobile_sales_ai`).
  - Seamlessly falls back to embedded **SQLite** (`data/mobile_sales_ai.db`) for zero-configuration, robust college viva evaluation.
- Full DAO Pattern:
  - `UserDAO`: User registration and authentication.
  - `MobileProductDAO`: Product queries, brand filtering, product creation.
  - `SalesDAO`: Historical and monthly sales tracking.
  - `InventoryDAO`: Stock buffers and lead times.
  - `AISearchDAO`: AI search visibility and spec claim comparisons.
  - `AnalyticsDAO`: Aggregate analytics persistence.
  - `RecommendationDAO`: Reorder decision records.
- SQL DDL schema provided in `src/main/resources/sql/schema.sql`.

---

## 🔬 Data Science & AI Formulas

### 1. Visibility Score
$$\text{Visibility} = \left(\frac{\text{Product Appearances}}{\text{Total Relevant Queries}}\right) \times 100$$

### 2. Information Accuracy
$$\text{Accuracy} = \left(\frac{\text{Correct Claims}}{\text{Total Claims}}\right) \times 100$$
Compares AI search snippets with ground-truth hardware specifications for battery, chipset, camera, display, and memory.

### 3. Customer Engagement Score
Weighted purchase-intent funnel:
$$\text{Engagement} = w_1(\text{Repeat}) + w_2(\text{Recommendations}) + w_3(\text{Comparisons}) + w_4(\text{Interactions}) + w_5(\text{Reviews}) + w_6(\text{Searches})$$

### 4. Demand Analysis & Forecasting
$$\text{Predicted Demand} = (\text{Baseline Projected} \times 0.70) + (\text{Current Demand} \times (1 + \text{AI Modifier}) \times 0.30)$$
*Note: The model does not assume linear causality; AI signals act as an empirical market-pull modifier calibrated by spec accuracy.*

### 5. Smart Reorder Point & Quantity
$$\text{Reorder Level (ROP)} = (\text{Average Daily Demand} \times \text{Lead Time Days}) + \text{Safety Stock}$$
$$\text{Recommended Reorder Qty} = \max(0, (\text{Predicted Demand} + \text{Safety Stock}) - \text{Current Stock})$$

---

## 📊 Brands & Models Included

- **Samsung**: Galaxy S24 Ultra, Galaxy S25 (*Potential Fast Moving*), Galaxy A56, Galaxy A36, Galaxy M Series
- **Apple**: iPhone 16 Pro Max, iPhone 16, iPhone 15 Pro, iPhone 15, iPhone 14
- **OnePlus**: OnePlus 13, OnePlus 12, OnePlus 12R, OnePlus Nord 4
- **Xiaomi**: Xiaomi 15 Pro, Xiaomi 14 Ultra, Redmi Note 14 Pro+, POCO F6
- **Vivo**: Vivo X100 Pro, Vivo V40 Pro, Vivo T3 Ultra, Vivo Y200
- **Oppo**: Find X8 Pro, Reno 12 Pro, F27 Pro+, A3 Pro
- **Google Pixel**: Pixel 9 Pro XL, Pixel 9, Pixel 8a, Pixel 8 Pro

---

## 📂 Project Directory Structure

```
AI Sales/
├── pom.xml                                  # Maven dependencies and Shade packaging
├── README.md                                # Comprehensive documentation
├── data/
│   └── mobile_sales_ai.db                   # Embedded SQLite DB (with MySQL schema parity)
├── src/
│   ├── main/
│   │   ├── java/com/mobilesales/
│   │   │   ├── MainApplication.java        # Main launcher & HTTP server bootstrap
│   │   │   ├── model/                      # Encapsulated models & OOP inheritance
│   │   │   │   ├── Product.java            # Abstract Base Class
│   │   │   │   ├── MobileProduct.java      # Derived Class (Inheritance)
│   │   │   │   ├── User.java               # Authentication User entity
│   │   │   │   ├── Sales.java              # Time-series sales data
│   │   │   │   ├── Inventory.java          # Stock, buffer, and lead time
│   │   │   │   ├── AISearchData.java       # Visibility, claims, engagement
│   │   │   │   ├── ProductClassification.java
│   │   │   │   ├── InventoryStatus.java
│   │   │   │   ├── DemandPredictionResult.java
│   │   │   │   └── ReorderRecommendation.java
│   │   │   ├── analytics/                  # Polymorphic analytics engine
│   │   │   │   ├── Analytics.java          # Core Generic Interface
│   │   │   │   ├── SalesAnalytics.java     # Sales realization
│   │   │   │   ├── InventoryAnalytics.java # Stock risk realization
│   │   │   │   ├── AIAnalytics.java        # AI intelligence realization
│   │   │   │   ├── VisibilityAnalyzer.java
│   │   │   │   ├── AccuracyAnalyzer.java
│   │   │   │   ├── EngagementAnalyzer.java
│   │   │   │   ├── DemandPrediction.java
│   │   │   │   ├── ProductClassifier.java  # 4-tier classification
│   │   │   │   ├── InventoryAnalyzer.java
│   │   │   │   └── RecommendationEngine.java
│   │   │   ├── exception/                  # Custom business exceptions
│   │   │   │   ├── InvalidProductException.java
│   │   │   │   ├── InvalidSalesDataException.java
│   │   │   │   ├── InsufficientStockException.java
│   │   │   │   ├── InvalidLoginException.java
│   │   │   │   └── DataValidationException.java
│   │   │   ├── dao/                        # JDBC Data Access Layer
│   │   │   │   ├── DatabaseConnection.java # Dual MySQL & SQLite manager
│   │   │   │   ├── UserDAO.java
│   │   │   │   ├── MobileProductDAO.java
│   │   │   │   ├── SalesDAO.java
│   │   │   │   ├── InventoryDAO.java
│   │   │   │   ├── AISearchDAO.java
│   │   │   │   ├── AnalyticsDAO.java
│   │   │   │   ├── RecommendationDAO.java
│   │   │   │   └── DataInitializer.java    # Seeds initial dataset
│   │   │   ├── io/                         # File I/O & reporting
│   │   │   │   ├── CSVDataImporter.java    # Automated CSV processing
│   │   │   │   ├── CSVDataExporter.java    # Dataset export
│   │   │   │   └── ReportGenerator.java    # Supply chain audit reports
│   │   │   ├── server/
│   │   │   │   └── AppServer.java          # Embedded multi-threaded HTTP REST server
│   │   │   └── gui/
│   │   │       └── MobileAnalyticsSwingApp.java # Full native Java Swing desktop app
│   │   └── resources/
│   │       ├── sql/
│   │       │   └── schema.sql              # MySQL DDL table script
│   │       └── web/                        # Dark Glassmorphism Web App
│   │           ├── index.html
│   │           ├── css/style.css
│   │           └── js/app.js
└── target/
    └── ai-sales-inventory-1.0.0.jar        # Standalone executable JAR
```

---

## 🎓 College Viva / Evaluation Checklist
- [x] **Starts with Login Page**: Mandatory authentication before accessing system.
- [x] **Registration Page**: Validates duplicate usernames, matching passwords, and emails.
- [x] **Home / Brand Selection**: Dynamic model selection across Samsung, Apple, OnePlus, Xiaomi, Vivo, Oppo, Google Pixel.
- [x] **Mobile Product Dashboard**: Specs, Sales Analytics, Inventory Analytics.
- [x] **AI Search Intelligence**: Visibility %, Spec Accuracy % with claim audit, Customer Engagement %.
- [x] **Demand Analysis & Prediction**: Combines historical run-rate with non-linear AI signals.
- [x] **Product Classification**: Fast, Medium, Slow, and **Potential Fast Moving** (*"Potential future demand increase detected"*).
- [x] **Smart Reorder Recommendation**: Formula-driven reorder level, recommended quantity, and *"Why?"* checklist.
- [x] **Executive Dashboard**: Top 7 cards, 6 charts, and supply chain alert badges.
- [x] **CSV Upload & Ingestion**: Automated validation and auto-calculation without manual math.
- [x] **Java OOP Concepts**: Inheritance, Interfaces, Polymorphism, Abstraction, Encapsulation, Custom Exceptions, Collections, File I/O, JDBC.
- [x] **MySQL + JDBC**: Complete `schema.sql` script with automated fallback for 100% demo reliability.
- [x] **Java GUI**: Native Java Swing implementation (`MobileAnalyticsSwingApp.java`) and Dark Glassmorphism Web App.
