# Invest Track App - Project Notes

> **IMPORTANT**: Keep this file updated when making significant changes to the codebase. This file serves as persistent memory between Claude Code sessions.

## Overview
Investment portfolio tracking application with budget management, stock recommendations, and FIRE (Financial Independence) projections.

## Key Architecture

### Entities

#### Wallet
Main investment portfolio container:
- `name`, `description`, `currency` (PLN/EUR/USD), `riskLevel`
- `snapshots` (OneToMany) - monthly portfolio states
- Calculated: `getReturnRate()`, `getTotalEarnings()`, `calculateTotalReturn()`

#### WalletSnapshot
Monthly portfolio state:
- `snapshotDate`, `portfolioValue`
- `monthlyDeposit`, `monthlyWithdrawal`, `monthlyEarnings`
- `notes`

#### BudgetEntry
Income/expense tracking:
- `name`, `category`, `value`, `entryDate`
- `paymentMethod` (Cash/Card/Transfer), `entryType` (Expense/Income)
- `isRecurring` - for recurring entries

#### StockPriceAlert & Config
Price monitoring with email notifications:
- `symbol`, `exchange`, `emails`
- Config: `price`, `operator`, `checkIntervalMinutes`

### Services

#### CurrencyConverter
- Automatic daily rate updates at 3 AM
- Source: `cdn.jsdelivr.net/npm/@fawazahmed0/currency-api`
- Supports: PLN, EUR, USD
- 4 decimal precision

#### InvestmentCalculationService (NEW)
Advanced investment metrics:
- `calculateCAGR()` - Compound Annual Growth Rate
- `calculateRealCAGR()` - CAGR adjusted for inflation
- `calculateTWR()` - Time-Weighted Return (eliminates cash flow impact)
- `calculateXIRR()` - Extended IRR using Newton-Raphson method
- `calculateYearlyReturns()` - Returns per year
- `calculateMonthlyReturns()` - Monthly returns for heatmap

#### BudgetChartDataService (NEW)
Budget data aggregation for charts:
- `getMonthlyIncomeExpense()` - Income vs Expense per month
- `getCategoryTrends()` - Expense trends by category
- `getMonthlyNetBalance()` - Net savings per month
- `getTopExpenseCategories()` - Top spending categories
- `getSummary()` - Period statistics

#### BudgetGridService
- Hierarchical tree: Month → Category → Item
- `copyToMonth()` - copy entries between months
- `copyRecurringToAnotherDate()` - auto-populate recurring

#### RecurringBudgetScheduler (NEW)
- Scheduled job at 00:01 on 1st of each month
- Auto-copies recurring entries to new month
- `addRecurringForMonth()` - manual trigger

#### Investment Strategies (ShortTermRecommendationStrategy)
1. GrowthAtMorningStrategyShortTerm
2. FallAtMorningStrategyShortTerm
3. ExtremeMorningSpikeStrategyShortTerm
4. HighVolumeMomentumStrategyShortTerm
5. LowVolatilityGrowersStrategyShortTerm
6. YesterdayWinnerContinuationShortTermStrategy
7. RandomStrategyShortTerm (baseline)

### Views & Dashboards

#### MainDashboardView (NEW - Main Tab)
Unified glassmorphism dashboard:
- **KPI Cards**: Total Balance, Net Deposits, Return, CAGR, TWR
  - CAGR and TWR cards have **tooltips** (ⓘ icon) explaining the formula
  - Tooltips use `title` attribute + `.kpi-has-tooltip` CSS class
- **Quick Filters**: MTD, YTD, 1Y, 3Y, 5Y, ALL
- **Charts Grid**:
  - Balance vs Deposits line chart
  - Budget Income/Expense bar chart
  - Asset Allocation pie chart
  - Monthly Returns Heatmap

#### WalletsBalanceView
- Portfolio balance vs cumulative deposits chart
- Total Balance, Deposit, Profit cards
- Period aggregation (Monthly/Quarterly/Yearly)

#### WalletsEarningsView
- Investment earnings over time
- Same filtering as Balance view

#### FirePathView (FIRE Analysis)
- 12-stage progression to editable FIRE goal (default 1,500,000 PLN)
- **Editable Goal**: NumberField to change target dynamically
- CAGR calculation with 3.8% inflation adjustment
- Projection variants: Baseline, +20%, -20%, Conservative
- Years-to-goal estimation (binary search algorithm)

#### WalletComparisonView (NEW)
Multi-wallet comparison table:
- Columns: Wallet, Currency, Risk, Balance, Deposits, Return, CAGR, TWR
- Sorted by balance descending
- Color-coded positive/negative returns

#### StrategyDashboardView
- Historical strategy performance
- Best/Good/Risky probability charts
- Side-by-side strategy comparison

#### BudgetGridView (TreeGrid)
- Hierarchical categories with icons
- Copy/Delete/Edit bulk operations
- Color-coded income (green) / expense (red)

### Chart Components (NEW)

#### BudgetIncomeExpenseChart
- Stacked bar chart: Income vs Expense per month
- Uses Chart.js via JsModule

#### AssetAllocationChart
- Doughnut/Pie chart for allocation
- Group by: Currency, Risk Level, or Wallet
- Auto-converts to PLN for comparison

#### MonthlyReturnsHeatmap
- CSS Grid showing monthly returns
- Color scale: green (positive) to red (negative)
- Levels from -5 to +5 for intensity

#### BudgetTrendChart
- Multi-line chart for category trends over time
- Shows expense patterns per category

### Key Calculations

```java
// Portfolio Returns
Return Rate = (Total Return / Net Investment) × 100
Total Return = Current Value - Net Investment
Net Investment = Total Deposits - Total Withdrawals

// CAGR - Compound Annual Growth Rate
CAGR = (EndValue/StartValue)^(1/years) - 1

// Real CAGR (inflation adjusted)
Real CAGR = (1 + nominalCAGR) / (1 + inflation) - 1

// TWR - Time-Weighted Return
TWR = Π(1 + Ri) - 1  // Product of all period returns

// XIRR - Extended Internal Rate of Return
// Newton-Raphson method finding rate where NPV = 0
NPV = Σ(CFi / (1+rate)^ti) = 0

// FIRE Projections
Real Annual Return = (1 + annualReturn) / (1 + inflation) - 1
FV = current × factor + deposits × ((factor-1)/return)
```

### Styling (Glassmorphism - Themeable)

CSS file: `invest-track-dashboard.css`

Dashboard uses `--glass-*` theme variables with fallbacks:
```css
/* Variables inherit from theme or use fallbacks */
--invest-glass-bg: var(--glass-bg, rgba(17, 25, 40, 0.75));
--invest-glass-blur: var(--glass-blur, 16px);
--invest-glass-border: var(--glass-border, rgba(255, 255, 255, 0.18));
--invest-primary: var(--glass-primary, #6366f1);
--invest-success: var(--vaadin-notification-card-bgColor-success, #10b981);
--invest-danger: var(--vaadin-notification-card-bgColor-error, #ef4444);
```

Dashboard container uses theme gradient:
```css
.invest-dashboard {
    background: var(--glass-container-gradient, linear-gradient(...));
}
```

Classes:
- `.invest-dashboard` - main dashboard container
- `.invest-kpi-card` - glassmorphism KPI cards
- `.invest-chart-card` - chart container cards
- `.invest-quick-filter-btn` - filter buttons (active state)
- `.invest-heatmap-cell.level-N` - heatmap colors (-5 to +5)

### Navigation (InvestTrackPageLayout)
Pill-tab navigation buttons:
- 📊 **Dashboard** - Main unified dashboard
- 🏠 Wallets - Portfolio list
- 🚩 Recommendations - Daily stock suggestions
- 🔔 Alerts - Price alerts
- 📜 Recommendation History - Past recommendations
- 💾 Data IE - Import/Export

### Navigation Routes
- `/invest-track-app/dashboard/` - Main dashboard (default)
- `/invest-track-app/wallets` - Portfolio list
- `/invest-track-app/wallet-details/{id}` - Individual wallet
- `/invest-track-app/recommendations` - Daily stock suggestions
- `/invest-track-app/alerts` - Price alerts
- `/invest-track-app/recommendations-history` - Past recommendations
- `/invest-track-app/import-export-data` - Data IE

### Constants
```java
WALLET_CURRENCIES = {"EUR", "USD", "PLN"}
RISK_LEVEL = {"Low Risk", "Medium Risk", "High Risk", "Very High Risk"}
TRANSACTION_TYPE = {"Deposit", "Withdrawal", "Earning", "Loss", "Dividend"}
```

### Category Icons (Budget)
- 🛒 Shop/Shopping
- 🍴 Food
- 🏠 House
- 🚗 Car
- 💼 Work
- 💍 Wedding
- 🎬 Entertainment
- 📀 Subscription
- 🏦 Loan

## Configuration
- Entity configs in `/src/main/resources/autoconfig/*.yml`
- Currency fallback rates: EUR=4.30, USD=3.70

## Important Notes
1. Wallets use EAGER fetch for snapshots (performance consideration)
2. Soft delete via `deleted` flag with `@Where` filtering
3. Charts use HTML5 Canvas via Vaadin JsModule
4. Long-running reports execute in async threads with SecurityContext
5. **Tab switching fix**: `AbstractBudgetDashboardView` tracks `currentTabLabel` to preserve tab selection when dropdowns change
6. **Aggregated time series**: `InvestmentCalculationService.buildAggregatedTimeSeries()` implements carry-forward for wallets with different snapshot dates
7. **BudgetGridView Glassmorphism**: Uses CSS classes `budget-tree-container`, `budget-tree-toolbar`, `budget-tree-grid` with glass-btn buttons
