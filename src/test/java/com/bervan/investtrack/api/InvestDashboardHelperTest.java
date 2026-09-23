package com.bervan.investtrack.api;

import com.bervan.investtrack.model.Wallet;
import com.bervan.investtrack.model.WalletSnapshot;
import com.bervan.investtrack.model.WalletType;
import com.bervan.investtrack.service.BudgetChartDataService;
import com.bervan.investtrack.service.CurrencyConverter;
import com.bervan.investtrack.service.ETFDataService;
import com.bervan.investtrack.service.InvestmentCalculationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InvestDashboardHelperTest {

    @Mock
    private ETFDataService etfDataService;

    @Spy
    private InvestmentCalculationService calculationService = new InvestmentCalculationService();

    @Mock
    private CurrencyConverter currencyConverter;

    @Mock
    private BudgetChartDataService budgetChartDataService;

    private InvestDashboardHelper helper;

    @BeforeEach
    void setUp() {
        helper = new InvestDashboardHelper(
                etfDataService,
                calculationService,
                currencyConverter,
                budgetChartDataService
        );

        // Default 1:1 currency conversion for PLN
        when(currencyConverter.convert(any(BigDecimal.class), any(), eq(CurrencyConverter.Currency.PLN)))
                .thenAnswer(inv -> inv.getArgument(0));

        // Default empty budget
        when(budgetChartDataService.getMonthlyIncomeExpense(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(new BudgetChartDataService.MonthlyBudgetData(new LinkedHashMap<>(), new LinkedHashMap<>()));

        // Default benchmark ticker calculation -> empty list
        when(etfDataService.calculateBenchmarkValuesForTicker(anyString(), anyString(), anyList(), anyList(), anyString()))
                .thenReturn(new ArrayList<>());
    }

    private Wallet createWallet(String name, String currency, WalletType walletType) {
        Wallet wallet = new Wallet();
        wallet.setId(UUID.randomUUID());
        wallet.setName(name);
        wallet.setCurrency(currency);
        wallet.setWalletType(walletType != null ? walletType.name() : null);
        return wallet;
    }

    private WalletSnapshot createSnapshot(Wallet wallet, LocalDate date, BigDecimal portfolioValue,
                                          BigDecimal monthlyDeposit, BigDecimal monthlyWithdrawal) {
        WalletSnapshot snapshot = new WalletSnapshot();
        snapshot.setId(UUID.randomUUID());
        snapshot.setWallet(wallet);
        snapshot.setSnapshotDate(date);
        snapshot.setPortfolioValue(portfolioValue);
        snapshot.setMonthlyDeposit(monthlyDeposit);
        snapshot.setMonthlyWithdrawal(monthlyWithdrawal);
        snapshot.setMonthlyEarnings(BigDecimal.ZERO);
        wallet.getSnapshots().add(snapshot);
        return snapshot;
    }

    @Nested
    @DisplayName("Empty & Edge Case Scenarios")
    class EmptyAndEdgeCaseTests {

        @Test
        @DisplayName("getDashboard with empty wallet list returns default zero KPIs and empty series")
        void emptyWalletsList() {
            Map<String, Object> result = helper.getDashboard(Collections.emptyList());

            assertThat(result).isNotNull();
            @SuppressWarnings("unchecked")
            Map<String, Object> kpi = (Map<String, Object>) result.get("kpi");
            assertThat(kpi).isNotNull();
            assertThat(kpi.get("investBalance")).isEqualTo(new BigDecimal("0.00"));
            assertThat(kpi.get("investNetDeposits")).isEqualTo(new BigDecimal("0.00"));
            assertThat(kpi.get("investReturn")).isEqualTo(new BigDecimal("0.00"));
            assertThat(kpi.get("investReturnPct")).isEqualTo(new BigDecimal("0.00"));
            assertThat(kpi.get("investTwr")).isEqualTo(new BigDecimal("0.00"));
            assertThat(kpi.get("investCagr")).isEqualTo(new BigDecimal("0.00"));
            assertThat(kpi.get("savingsBalance")).isEqualTo(new BigDecimal("0.00"));
            assertThat(kpi.get("savingsGrowth")).isEqualTo(new BigDecimal("0.00"));
            assertThat(kpi.get("netWorth")).isEqualTo(new BigDecimal("0.00"));
            assertThat(kpi.get("avgMonthlyDeposit")).isEqualTo(new BigDecimal("0.00"));
            assertThat(kpi.get("investMonthsSpan")).isEqualTo(1);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> investTimeSeries = (List<Map<String, Object>>) result.get("investTimeSeries");
            assertThat(investTimeSeries).isEmpty();

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> netWorthTimeSeries = (List<Map<String, Object>>) result.get("netWorthTimeSeries");
            assertThat(netWorthTimeSeries).isEmpty();

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> allocation = (List<Map<String, Object>>) result.get("allocation");
            assertThat(allocation).isEmpty();

            @SuppressWarnings("unchecked")
            Map<String, BigDecimal> heatmap = (Map<String, BigDecimal>) result.get("heatmap");
            assertThat(heatmap).isEmpty();

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> walletSeries = (List<Map<String, Object>>) result.get("walletSeries");
            assertThat(walletSeries).isEmpty();
        }

        @Test
        @DisplayName("getDashboard with wallets having no snapshots returns zero KPIs and empty series per wallet")
        void walletsWithNoSnapshots() {
            Wallet investWallet = createWallet("Invest 1", "PLN", WalletType.INVESTMENT);
            Wallet savingsWallet = createWallet("Savings 1", "PLN", WalletType.SAVINGS);

            Map<String, Object> result = helper.getDashboard(List.of(investWallet, savingsWallet));

            @SuppressWarnings("unchecked")
            Map<String, Object> kpi = (Map<String, Object>) result.get("kpi");
            assertThat(kpi.get("investBalance")).isEqualTo(new BigDecimal("0.00"));
            assertThat(kpi.get("savingsBalance")).isEqualTo(new BigDecimal("0.00"));
            assertThat(kpi.get("netWorth")).isEqualTo(new BigDecimal("0.00"));

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> allocation = (List<Map<String, Object>>) result.get("allocation");
            assertThat(allocation).isEmpty();

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> walletSeries = (List<Map<String, Object>>) result.get("walletSeries");
            assertThat(walletSeries).hasSize(2);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> series0 = (List<Map<String, Object>>) walletSeries.get(0).get("series");
            assertThat(series0).isEmpty();
        }

        @Test
        @DisplayName("Snapshots with null deposits, withdrawals, or values in walletSeries are defaulted to BigDecimal.ZERO")
        void snapshotsWithNullFieldsHandledGracefully() {
            Wallet wallet = createWallet("Invest Wallet", "PLN", WalletType.INVESTMENT);
            WalletSnapshot snapshot = new WalletSnapshot();
            snapshot.setId(UUID.randomUUID());
            snapshot.setWallet(wallet);
            snapshot.setSnapshotDate(LocalDate.of(2024, 1, 1));
            snapshot.setPortfolioValue(null);
            snapshot.setMonthlyDeposit(null);
            snapshot.setMonthlyWithdrawal(null);
            wallet.getSnapshots().add(snapshot);

            // Mock calculation service aggregated time series to avoid NPE in calculationService
            when(calculationService.buildAggregatedTimeSeries(anyList(), any())).thenReturn(Collections.emptyMap());

            Map<String, Object> result = helper.getDashboard(List.of(wallet));

            @SuppressWarnings("unchecked")
            Map<String, Object> kpi = (Map<String, Object>) result.get("kpi");
            assertThat(kpi.get("investBalance")).isEqualTo(new BigDecimal("0.00"));
            assertThat(kpi.get("investNetDeposits")).isEqualTo(new BigDecimal("0.00"));

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> walletSeries = (List<Map<String, Object>>) result.get("walletSeries");
            assertThat(walletSeries).hasSize(1);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> series = (List<Map<String, Object>>) walletSeries.get(0).get("series");
            assertThat(series).hasSize(1);
            assertThat(series.get(0).get("balance")).isEqualTo(new BigDecimal("0.00"));
            assertThat(series.get(0).get("cumDeposit")).isEqualTo(new BigDecimal("0.00"));
        }

        @Test
        @DisplayName("Snapshots with null snapshotDate in walletSeries are filtered out")
        void snapshotsWithNullDateFilteredOutInWalletSeries() {
            Wallet wallet = createWallet("Invest Wallet", "PLN", WalletType.INVESTMENT);

            createSnapshot(wallet, LocalDate.of(2024, 1, 1), new BigDecimal("1000"), new BigDecimal("1000"), BigDecimal.ZERO);
            createSnapshot(wallet, LocalDate.of(2024, 2, 1), new BigDecimal("1100"), BigDecimal.ZERO, BigDecimal.ZERO);

            Map<String, Object> result = helper.getDashboard(List.of(wallet));

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> walletSeries = (List<Map<String, Object>>) result.get("walletSeries");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> series = (List<Map<String, Object>>) walletSeries.get(0).get("series");
            assertThat(series).hasSize(2);
            assertThat(series.get(0).get("date")).isEqualTo("2024-01-01");
            assertThat(series.get(1).get("date")).isEqualTo("2024-02-01");
        }
    }

    @Nested
    @DisplayName("Investment KPIs & Returns Calculations")
    class InvestmentKpiTests {

        @Test
        @DisplayName("Single investment wallet calculates balance, net deposits, return, percentage, TWR and CAGR")
        void singleInvestmentWalletKpis() {
            Wallet wallet = createWallet("My Stock Portfolio", "PLN", WalletType.INVESTMENT);
            createSnapshot(wallet, LocalDate.of(2023, 1, 1), new BigDecimal("10000"), new BigDecimal("10000"), BigDecimal.ZERO);
            createSnapshot(wallet, LocalDate.of(2023, 7, 1), new BigDecimal("15000"), new BigDecimal("2000"), BigDecimal.ZERO);
            createSnapshot(wallet, LocalDate.of(2024, 1, 1), new BigDecimal("20000"), new BigDecimal("3000"), BigDecimal.ZERO);

            Map<String, Object> result = helper.getDashboard(List.of(wallet));

            @SuppressWarnings("unchecked")
            Map<String, Object> kpi = (Map<String, Object>) result.get("kpi");

            // Net deposits = 10000 + 2000 + 3000 = 15000
            // Current value = 20000
            // Return = 5000
            // Return % = (5000 / 15000) * 100 = 33.33%
            assertThat(kpi.get("investBalance")).isEqualTo(new BigDecimal("20000.00"));
            assertThat(kpi.get("investNetDeposits")).isEqualTo(new BigDecimal("15000.00"));
            assertThat(kpi.get("investReturn")).isEqualTo(new BigDecimal("5000.00"));
            assertThat(kpi.get("investReturnPct")).isEqualTo(new BigDecimal("33.33"));
            assertThat(kpi.get("investTwr")).isNotNull();
            assertThat(kpi.get("investCagr")).isNotNull();

            // Months span: 2023-01 to 2024-01 is 12 months + 1 = 13 months
            assertThat(kpi.get("investMonthsSpan")).isEqualTo(13);

            // avgMonthlyDeposit: 15000 / (13/12 * 12) = 15000 / 13 = 1153.85
            assertThat(kpi.get("avgMonthlyDeposit")).isEqualTo(new BigDecimal("1153.85"));
        }

        @Test
        @DisplayName("Investment portfolio with loss calculates negative return and negative return percentage")
        void negativeInvestmentReturn() {
            Wallet wallet = createWallet("Crypto Wallet", "PLN", WalletType.CRYPTO);
            createSnapshot(wallet, LocalDate.of(2024, 1, 1), new BigDecimal("10000"), new BigDecimal("10000"), BigDecimal.ZERO);
            createSnapshot(wallet, LocalDate.of(2024, 6, 1), new BigDecimal("7000"), BigDecimal.ZERO, BigDecimal.ZERO);

            Map<String, Object> result = helper.getDashboard(List.of(wallet));

            @SuppressWarnings("unchecked")
            Map<String, Object> kpi = (Map<String, Object>) result.get("kpi");

            // Net deposits = 10000, current = 7000, return = -3000, returnPct = -30.00%
            assertThat(kpi.get("investBalance")).isEqualTo(new BigDecimal("7000.00"));
            assertThat(kpi.get("investNetDeposits")).isEqualTo(new BigDecimal("10000.00"));
            assertThat(kpi.get("investReturn")).isEqualTo(new BigDecimal("-3000.00"));
            assertThat(kpi.get("investReturnPct")).isEqualTo(new BigDecimal("-30.00"));
        }

        @Test
        @DisplayName("Zero or negative net deposits prevent division by zero in return percentage and CAGR")
        void zeroOrNegativeNetDeposits() {
            Wallet wallet = createWallet("Withdrawn Wallet", "PLN", WalletType.INVESTMENT);
            // Deposit 1000, withdraw 1500 -> net deposits = -500
            createSnapshot(wallet, LocalDate.of(2023, 1, 1), new BigDecimal("1000"), new BigDecimal("1000"), BigDecimal.ZERO);
            createSnapshot(wallet, LocalDate.of(2024, 1, 1), new BigDecimal("500"), BigDecimal.ZERO, new BigDecimal("1500"));

            Map<String, Object> result = helper.getDashboard(List.of(wallet));

            @SuppressWarnings("unchecked")
            Map<String, Object> kpi = (Map<String, Object>) result.get("kpi");

            assertThat(kpi.get("investNetDeposits")).isEqualTo(new BigDecimal("-500.00"));
            assertThat(kpi.get("investReturnPct")).isEqualTo(new BigDecimal("0.00"));
            assertThat(kpi.get("investCagr")).isEqualTo(new BigDecimal("0.00"));
        }

        @Test
        @DisplayName("Short time span (< 0.1 years) sets investCagr to zero")
        void shortTimeSpanSetsCagrToZero() {
            Wallet wallet = createWallet("Quick Wallet", "PLN", WalletType.INVESTMENT);
            // Single snapshot -> monthsSpan = 1 -> investYears = 1/12 ≈ 0.0833 (< 0.1)
            createSnapshot(wallet, LocalDate.of(2024, 1, 1), new BigDecimal("5000"), new BigDecimal("5000"), BigDecimal.ZERO);

            Map<String, Object> result = helper.getDashboard(List.of(wallet));

            @SuppressWarnings("unchecked")
            Map<String, Object> kpi = (Map<String, Object>) result.get("kpi");
            assertThat(kpi.get("investCagr")).isEqualTo(new BigDecimal("0.00"));
        }

        @Test
        @DisplayName("Multiple investment wallets aggregate balances and net deposits correctly")
        void multipleInvestmentWalletsAggregate() {
            Wallet stockWallet = createWallet("Stocks", "PLN", WalletType.INVESTMENT);
            createSnapshot(stockWallet, LocalDate.of(2024, 1, 1), new BigDecimal("10000"), new BigDecimal("8000"), BigDecimal.ZERO);

            Wallet bondWallet = createWallet("Bonds", "PLN", WalletType.BONDS);
            createSnapshot(bondWallet, LocalDate.of(2024, 1, 1), new BigDecimal("5000"), new BigDecimal("4500"), BigDecimal.ZERO);

            Wallet ppkWallet = createWallet("PPK", "PLN", WalletType.PPK);
            createSnapshot(ppkWallet, LocalDate.of(2024, 1, 1), new BigDecimal("3000"), new BigDecimal("2500"), BigDecimal.ZERO);

            Map<String, Object> result = helper.getDashboard(List.of(stockWallet, bondWallet, ppkWallet));

            @SuppressWarnings("unchecked")
            Map<String, Object> kpi = (Map<String, Object>) result.get("kpi");

            // Total invest balance = 10000 + 5000 + 3000 = 18000
            // Total net deposits = 8000 + 4500 + 2500 = 15000
            // Return = 3000
            // Return % = (3000 / 15000) * 100 = 20.00%
            assertThat(kpi.get("investBalance")).isEqualTo(new BigDecimal("18000.00"));
            assertThat(kpi.get("investNetDeposits")).isEqualTo(new BigDecimal("15000.00"));
            assertThat(kpi.get("investReturn")).isEqualTo(new BigDecimal("3000.00"));
            assertThat(kpi.get("investReturnPct")).isEqualTo(new BigDecimal("20.00"));
            assertThat(kpi.get("savingsBalance")).isEqualTo(new BigDecimal("0.00"));
            assertThat(kpi.get("netWorth")).isEqualTo(new BigDecimal("18000.00"));
        }
    }

    @Nested
    @DisplayName("Savings & Mixed Portfolio KPIs")
    class SavingsAndMixedKpiTests {

        @Test
        @DisplayName("Savings wallets only populate savingsBalance, savingsGrowth and netWorth")
        void savingsWalletsOnly() {
            Wallet savingsWallet = createWallet("High Yield Savings", "PLN", WalletType.SAVINGS);
            createSnapshot(savingsWallet, LocalDate.of(2024, 1, 1), new BigDecimal("10000"), new BigDecimal("10000"), BigDecimal.ZERO);
            createSnapshot(savingsWallet, LocalDate.of(2024, 6, 1), new BigDecimal("10300"), BigDecimal.ZERO, BigDecimal.ZERO);

            Wallet cashWallet = createWallet("Physical Cash", "PLN", WalletType.CASH);
            createSnapshot(cashWallet, LocalDate.of(2024, 1, 1), new BigDecimal("2000"), new BigDecimal("2000"), BigDecimal.ZERO);

            Map<String, Object> result = helper.getDashboard(List.of(savingsWallet, cashWallet));

            @SuppressWarnings("unchecked")
            Map<String, Object> kpi = (Map<String, Object>) result.get("kpi");

            // Savings balance = 10300 + 2000 = 12300
            // Savings net deposits = 10000 + 2000 = 12000
            // Savings growth = 12300 - 12000 = 300
            // Invest balance = 0
            // Net worth = 12300
            assertThat(kpi.get("investBalance")).isEqualTo(new BigDecimal("0.00"));
            assertThat(kpi.get("investNetDeposits")).isEqualTo(new BigDecimal("0.00"));
            assertThat(kpi.get("savingsBalance")).isEqualTo(new BigDecimal("12300.00"));
            assertThat(kpi.get("savingsGrowth")).isEqualTo(new BigDecimal("300.00"));
            assertThat(kpi.get("netWorth")).isEqualTo(new BigDecimal("12300.00"));

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> investTimeSeries = (List<Map<String, Object>>) result.get("investTimeSeries");
            assertThat(investTimeSeries).isEmpty();
        }

        @Test
        @DisplayName("Mixed portfolio combines investment and savings into net worth")
        void mixedPortfolioNetWorth() {
            Wallet investWallet = createWallet("ETF Portfolio", "PLN", WalletType.INVESTMENT);
            createSnapshot(investWallet, LocalDate.of(2024, 1, 1), new BigDecimal("50000"), new BigDecimal("40000"), BigDecimal.ZERO);

            Wallet savingsWallet = createWallet("Emergency Fund", "PLN", WalletType.SAVINGS);
            createSnapshot(savingsWallet, LocalDate.of(2024, 1, 1), new BigDecimal("20000"), new BigDecimal("19000"), BigDecimal.ZERO);

            Map<String, Object> result = helper.getDashboard(List.of(investWallet, savingsWallet));

            @SuppressWarnings("unchecked")
            Map<String, Object> kpi = (Map<String, Object>) result.get("kpi");

            assertThat(kpi.get("investBalance")).isEqualTo(new BigDecimal("50000.00"));
            assertThat(kpi.get("savingsBalance")).isEqualTo(new BigDecimal("20000.00"));
            assertThat(kpi.get("netWorth")).isEqualTo(new BigDecimal("70000.00"));
            assertThat(kpi.get("savingsGrowth")).isEqualTo(new BigDecimal("1000.00"));
            assertThat(kpi.get("investReturn")).isEqualTo(new BigDecimal("10000.00"));
        }
    }

    @Nested
    @DisplayName("Multi-Currency Conversion Scenarios")
    class MultiCurrencyTests {

        @Test
        @DisplayName("Foreign currency wallets are converted to PLN for balances, deposits and series")
        void foreignCurrencyConversion() {
            Wallet usdWallet = createWallet("US Tech Stocks", "USD", WalletType.INVESTMENT);
            createSnapshot(usdWallet, LocalDate.of(2024, 1, 1), new BigDecimal("1000"), new BigDecimal("1000"), BigDecimal.ZERO);

            // Mock currency converter: 1 USD = 4.00 PLN
            when(currencyConverter.convert(eq(new BigDecimal("1000")), eq(CurrencyConverter.Currency.USD), eq(CurrencyConverter.Currency.PLN)))
                    .thenReturn(new BigDecimal("4000.00"));

            Map<String, Object> result = helper.getDashboard(List.of(usdWallet));

            @SuppressWarnings("unchecked")
            Map<String, Object> kpi = (Map<String, Object>) result.get("kpi");

            // 1000 USD converted to 4000 PLN
            assertThat(kpi.get("investBalance")).isEqualTo(new BigDecimal("4000.00"));
            assertThat(kpi.get("investNetDeposits")).isEqualTo(new BigDecimal("4000.00"));
            assertThat(kpi.get("netWorth")).isEqualTo(new BigDecimal("4000.00"));

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> allocation = (List<Map<String, Object>>) result.get("allocation");
            assertThat(allocation).hasSize(1);
            assertThat(allocation.get(0).get("valuePln")).isEqualTo(new BigDecimal("4000.00"));
        }

        @Test
        @DisplayName("toPln returns BigDecimal.ZERO when amount is null")
        void toPlnWithNullAmount() {
            Wallet wallet = createWallet("Null Value Wallet", "PLN", WalletType.INVESTMENT);
            // Empty wallet has null current value/deposits
            Map<String, Object> result = helper.getDashboard(List.of(wallet));

            @SuppressWarnings("unchecked")
            Map<String, Object> kpi = (Map<String, Object>) result.get("kpi");
            assertThat(kpi.get("investBalance")).isEqualTo(new BigDecimal("0.00"));
            verify(currencyConverter, never()).convert(isNull(), any(), any());
        }
    }

    @Nested
    @DisplayName("Time Series & Benchmark Integration")
    class BenchmarkTimeSeriesTests {

        @Test
        @DisplayName("buildTimeSeriesWithBenchmarks populates all benchmark values and cumulative deposit")
        void timeSeriesWithBenchmarks() {
            Wallet wallet = createWallet("SP500 Tracker", "PLN", WalletType.INVESTMENT);
            createSnapshot(wallet, LocalDate.of(2024, 1, 1), new BigDecimal("1000"), new BigDecimal("1000"), BigDecimal.ZERO);
            createSnapshot(wallet, LocalDate.of(2024, 2, 1), new BigDecimal("2200"), new BigDecimal("1000"), BigDecimal.ZERO);

            // Mock benchmark returns
            when(etfDataService.calculateBenchmarkValuesForTicker(eq(ETFDataService.SP500_TICKER), eq("USD"), anyList(), anyList(), eq("PLN")))
                    .thenReturn(List.of(new BigDecimal("1000.00"), new BigDecimal("2100.50")));
            when(etfDataService.calculateBenchmarkValuesForTicker(eq(ETFDataService.WIG20_TICKER), eq("PLN"), anyList(), anyList(), eq("PLN")))
                    .thenReturn(List.of(new BigDecimal("1000.00"), new BigDecimal("2050.25")));
            when(etfDataService.calculateBenchmarkValuesForTicker(eq(ETFDataService.NASDAQ_TICKER), eq("USD"), anyList(), anyList(), eq("PLN")))
                    .thenReturn(List.of(new BigDecimal("1000.00"), new BigDecimal("2150.75")));
            when(etfDataService.calculateBenchmarkValuesForTicker(eq(ETFDataService.DJI_TICKER), eq("USD"), anyList(), anyList(), eq("PLN")))
                    .thenReturn(List.of(new BigDecimal("1000.00"), new BigDecimal("2020.10")));
            when(etfDataService.calculateBenchmarkValuesForTicker(eq(ETFDataService.FIXED_DEPOSIT_TICKER_3_5), eq("PLN"), anyList(), anyList(), eq("PLN")))
                    .thenReturn(List.of(new BigDecimal("1000.00"), new BigDecimal("2035.00")));

            Map<String, Object> result = helper.getDashboard(List.of(wallet));

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> investTimeSeries = (List<Map<String, Object>>) result.get("investTimeSeries");
            assertThat(investTimeSeries).hasSize(2);

            Map<String, Object> pt1 = investTimeSeries.get(0);
            assertThat(pt1.get("date")).isEqualTo("2024-01-01");
            assertThat(pt1.get("balance")).isEqualTo(new BigDecimal("1000.00"));
            assertThat(pt1.get("cumDeposit")).isEqualTo(new BigDecimal("1000.00"));
            assertThat(pt1.get("sp500")).isEqualTo(new BigDecimal("1000.00"));
            assertThat(pt1.get("wig20")).isEqualTo(new BigDecimal("1000.00"));
            assertThat(pt1.get("nasdaq")).isEqualTo(new BigDecimal("1000.00"));
            assertThat(pt1.get("dji")).isEqualTo(new BigDecimal("1000.00"));
            assertThat(pt1.get("fixedDeposit3_5")).isEqualTo(new BigDecimal("1000.00"));

            Map<String, Object> pt2 = investTimeSeries.get(1);
            assertThat(pt2.get("date")).isEqualTo("2024-02-01");
            assertThat(pt2.get("balance")).isEqualTo(new BigDecimal("2200.00"));
            assertThat(pt2.get("cumDeposit")).isEqualTo(new BigDecimal("2000.00"));
            assertThat(pt2.get("sp500")).isEqualTo(new BigDecimal("2100.50"));
            assertThat(pt2.get("wig20")).isEqualTo(new BigDecimal("2050.25"));
            assertThat(pt2.get("nasdaq")).isEqualTo(new BigDecimal("2150.75"));
            assertThat(pt2.get("dji")).isEqualTo(new BigDecimal("2020.10"));
            assertThat(pt2.get("fixedDeposit3_5")).isEqualTo(new BigDecimal("2035.00"));
        }

        @Test
        @DisplayName("Benchmark list with fewer items than dates defaults safely to BigDecimal.ZERO")
        void benchmarkListShorterThanDates() {
            Wallet wallet = createWallet("Wallet", "PLN", WalletType.INVESTMENT);
            createSnapshot(wallet, LocalDate.of(2024, 1, 1), new BigDecimal("1000"), new BigDecimal("1000"), BigDecimal.ZERO);
            createSnapshot(wallet, LocalDate.of(2024, 2, 1), new BigDecimal("2000"), new BigDecimal("1000"), BigDecimal.ZERO);

            // Benchmark service returns only 1 item for 2 dates
            when(etfDataService.calculateBenchmarkValuesForTicker(anyString(), anyString(), anyList(), anyList(), anyString()))
                    .thenReturn(List.of(new BigDecimal("1000.00")));

            Map<String, Object> result = helper.getDashboard(List.of(wallet));

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> investTimeSeries = (List<Map<String, Object>>) result.get("investTimeSeries");
            assertThat(investTimeSeries).hasSize(2);

            Map<String, Object> pt2 = investTimeSeries.get(1);
            assertThat((BigDecimal) pt2.get("sp500")).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat((BigDecimal) pt2.get("wig20")).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat((BigDecimal) pt2.get("nasdaq")).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat((BigDecimal) pt2.get("dji")).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat((BigDecimal) pt2.get("fixedDeposit3_5")).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("Benchmark calculations receive correctly formatted dd-MM-yyyy dates and net deposits")
        void benchmarkTickerArgumentsFormattedCorrectly() {
            Wallet wallet = createWallet("Wallet", "PLN", WalletType.INVESTMENT);
            createSnapshot(wallet, LocalDate.of(2024, 1, 15), new BigDecimal("1000"), new BigDecimal("1000"), BigDecimal.ZERO);
            createSnapshot(wallet, LocalDate.of(2024, 2, 28), new BigDecimal("2500"), new BigDecimal("1200"), new BigDecimal("200")); // net +1000

            helper.getDashboard(List.of(wallet));

            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<String>> datesCaptor = ArgumentCaptor.forClass(List.class);
            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<BigDecimal>> depositsCaptor = ArgumentCaptor.forClass(List.class);

            verify(etfDataService, atLeastOnce()).calculateBenchmarkValuesForTicker(
                    eq(ETFDataService.SP500_TICKER),
                    eq("USD"),
                    datesCaptor.capture(),
                    depositsCaptor.capture(),
                    eq("PLN")
            );

            List<String> capturedDates = datesCaptor.getValue();
            assertThat(capturedDates).containsExactly("15-01-2024", "28-02-2024");

            List<BigDecimal> capturedDeposits = depositsCaptor.getValue();
            assertThat(capturedDeposits.get(0)).isEqualByComparingTo(new BigDecimal("1000"));
            assertThat(capturedDeposits.get(1)).isEqualByComparingTo(new BigDecimal("1000"));
        }
    }

    @Nested
    @DisplayName("Heatmap Calculations")
    class HeatmapTests {

        @Test
        @DisplayName("Heatmap calculates monthly return percentages correctly for consecutive months")
        void heatmapMonthlyCalculations() {
            Wallet wallet = createWallet("Invest Wallet", "PLN", WalletType.INVESTMENT);
            // Month 1: Jan deposit 1000, value 1000
            // Month 2: Feb deposit 0, value 1200 -> begin = 1000 + 0 = 1000 -> ret = (1200 - 1000)/1000 = +20.00%
            // Month 3: Mar deposit 0, value 1100 -> begin = 1200 + 0 = 1200 -> ret = (1100 - 1200)/1200 = -8.33%
            createSnapshot(wallet, LocalDate.of(2024, 1, 1), new BigDecimal("1000"), new BigDecimal("1000"), BigDecimal.ZERO);
            createSnapshot(wallet, LocalDate.of(2024, 2, 1), new BigDecimal("1200"), BigDecimal.ZERO, BigDecimal.ZERO);
            createSnapshot(wallet, LocalDate.of(2024, 3, 1), new BigDecimal("1100"), BigDecimal.ZERO, BigDecimal.ZERO);

            Map<String, Object> result = helper.getDashboard(List.of(wallet));

            @SuppressWarnings("unchecked")
            Map<String, BigDecimal> heatmap = (Map<String, BigDecimal>) result.get("heatmap");
            assertThat(heatmap).hasSize(2);
            assertThat(heatmap.get("2024-02")).isEqualTo(new BigDecimal("20.00"));
            assertThat(heatmap.get("2024-03")).isEqualTo(new BigDecimal("-8.33"));
        }

        @Test
        @DisplayName("Heatmap skips months when beginValue (prevBalance + currCashFlow) is <= 0")
        void heatmapSkipsNonPositiveBeginValue() {
            Wallet wallet = createWallet("Zero Wallet", "PLN", WalletType.INVESTMENT);
            createSnapshot(wallet, LocalDate.of(2024, 1, 1), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
            createSnapshot(wallet, LocalDate.of(2024, 2, 1), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);

            Map<String, Object> result = helper.getDashboard(List.of(wallet));

            @SuppressWarnings("unchecked")
            Map<String, BigDecimal> heatmap = (Map<String, BigDecimal>) result.get("heatmap");
            assertThat(heatmap).isEmpty();
        }

        @Test
        @DisplayName("Heatmap is empty when time series has only 1 point")
        void heatmapSinglePointIsEmpty() {
            Wallet wallet = createWallet("Invest Wallet", "PLN", WalletType.INVESTMENT);
            createSnapshot(wallet, LocalDate.of(2024, 1, 1), new BigDecimal("1000"), new BigDecimal("1000"), BigDecimal.ZERO);

            Map<String, Object> result = helper.getDashboard(List.of(wallet));

            @SuppressWarnings("unchecked")
            Map<String, BigDecimal> heatmap = (Map<String, BigDecimal>) result.get("heatmap");
            assertThat(heatmap).isEmpty();
        }
    }

    @Nested
    @DisplayName("Asset Allocation")
    class AssetAllocationTests {

        @Test
        @DisplayName("Allocation includes only wallets with positive value in PLN")
        void allocationPositiveWalletsOnly() {
            Wallet wPositive1 = createWallet("Wallet Positive 1", "PLN", WalletType.INVESTMENT);
            createSnapshot(wPositive1, LocalDate.of(2024, 1, 1), new BigDecimal("5000"), new BigDecimal("5000"), BigDecimal.ZERO);

            Wallet wPositive2 = createWallet("Wallet Positive 2", "PLN", WalletType.SAVINGS);
            createSnapshot(wPositive2, LocalDate.of(2024, 1, 1), new BigDecimal("3000"), new BigDecimal("3000"), BigDecimal.ZERO);

            Wallet wZero = createWallet("Zero Wallet", "PLN", WalletType.CASH);
            createSnapshot(wZero, LocalDate.of(2024, 1, 1), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);

            Wallet wNoSnapshots = createWallet("Empty Wallet", "PLN", WalletType.BONDS);

            Map<String, Object> result = helper.getDashboard(List.of(wPositive1, wPositive2, wZero, wNoSnapshots));

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> allocation = (List<Map<String, Object>>) result.get("allocation");
            assertThat(allocation).hasSize(2);

            assertThat(allocation.get(0).get("name")).isEqualTo("Wallet Positive 1");
            assertThat(allocation.get(0).get("type")).isEqualTo(WalletType.INVESTMENT.name());
            assertThat(allocation.get(0).get("valuePln")).isEqualTo(new BigDecimal("5000.00"));

            assertThat(allocation.get(1).get("name")).isEqualTo("Wallet Positive 2");
            assertThat(allocation.get(1).get("type")).isEqualTo(WalletType.SAVINGS.name());
            assertThat(allocation.get(1).get("valuePln")).isEqualTo(new BigDecimal("3000.00"));
        }

        @Test
        @DisplayName("Allocation formats diverse wallet types (INVESTMENT_FUND, PPK, BONDS, CRYPTO)")
        void allocationFormatsDiverseWalletTypes() {
            Wallet fund = createWallet("Index Fund", "PLN", WalletType.INVESTMENT_FUND);
            createSnapshot(fund, LocalDate.of(2024, 1, 1), new BigDecimal("12000.555"), new BigDecimal("10000"), BigDecimal.ZERO);

            Wallet ppk = createWallet("My PPK", "PLN", WalletType.PPK);
            createSnapshot(ppk, LocalDate.of(2024, 1, 1), new BigDecimal("8500.444"), new BigDecimal("7000"), BigDecimal.ZERO);

            Map<String, Object> result = helper.getDashboard(List.of(fund, ppk));

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> allocation = (List<Map<String, Object>>) result.get("allocation");
            assertThat(allocation).hasSize(2);
            assertThat(allocation.get(0).get("type")).isEqualTo("INVESTMENT_FUND");
            assertThat(allocation.get(0).get("valuePln")).isEqualTo(new BigDecimal("12000.56"));
            assertThat(allocation.get(1).get("type")).isEqualTo("PPK");
            assertThat(allocation.get(1).get("valuePln")).isEqualTo(new BigDecimal("8500.44"));
        }
    }

    @Nested
    @DisplayName("Budget Data Integration")
    class BudgetIntegrationTests {

        @Test
        @DisplayName("Budget entries from BudgetChartDataService are properly mapped in dashboard result")
        void budgetDataMapping() {
            Map<String, BigDecimal> income = new LinkedHashMap<>();
            income.put("2024-01", new BigDecimal("10000.00"));
            income.put("2024-02", new BigDecimal("12000.00"));

            Map<String, BigDecimal> expense = new LinkedHashMap<>();
            expense.put("2024-01", new BigDecimal("6000.00"));
            expense.put("2024-02", new BigDecimal("7500.00"));

            when(budgetChartDataService.getMonthlyIncomeExpense(any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(new BudgetChartDataService.MonthlyBudgetData(income, expense));

            Map<String, Object> result = helper.getDashboard(Collections.emptyList());

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> budget = (List<Map<String, Object>>) result.get("budget");
            assertThat(budget).hasSize(2);

            Map<String, Object> b1 = budget.get(0);
            assertThat(b1.get("month")).isEqualTo("2024-01");
            assertThat(b1.get("income")).isEqualTo(new BigDecimal("10000.00"));
            assertThat(b1.get("expense")).isEqualTo(new BigDecimal("6000.00"));

            Map<String, Object> b2 = budget.get(1);
            assertThat(b2.get("month")).isEqualTo("2024-02");
            assertThat(b2.get("income")).isEqualTo(new BigDecimal("12000.00"));
            assertThat(b2.get("expense")).isEqualTo(new BigDecimal("7500.00"));
        }

        @Test
        @DisplayName("Missing expense for an income month defaults to BigDecimal.ZERO")
        void budgetMissingExpenseDefaultsToZero() {
            Map<String, BigDecimal> income = new LinkedHashMap<>();
            income.put("2024-01", new BigDecimal("10000.00"));

            Map<String, BigDecimal> expense = new LinkedHashMap<>(); // no entry for 2024-01

            when(budgetChartDataService.getMonthlyIncomeExpense(any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(new BudgetChartDataService.MonthlyBudgetData(income, expense));

            Map<String, Object> result = helper.getDashboard(Collections.emptyList());

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> budget = (List<Map<String, Object>>) result.get("budget");
            assertThat(budget).hasSize(1);
            assertThat(budget.get(0).get("expense")).isEqualTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("Budget timeframe requests 12 months up to current date")
        void budgetTimeframeCalculation() {
            helper.getDashboard(Collections.emptyList());

            ArgumentCaptor<LocalDate> fromCaptor = ArgumentCaptor.forClass(LocalDate.class);
            ArgumentCaptor<LocalDate> toCaptor = ArgumentCaptor.forClass(LocalDate.class);

            verify(budgetChartDataService).getMonthlyIncomeExpense(fromCaptor.capture(), toCaptor.capture());

            LocalDate expectedFrom = LocalDate.now().minusMonths(12).withDayOfMonth(1);
            LocalDate expectedTo = LocalDate.now();

            assertThat(fromCaptor.getValue()).isEqualTo(expectedFrom);
            assertThat(toCaptor.getValue()).isEqualTo(expectedTo);
        }
    }

    @Nested
    @DisplayName("Per-Wallet Series (Balance / Earnings Tabs)")
    class WalletSeriesTests {

        @Test
        @DisplayName("Wallet snapshots are sorted chronologically and cumulative net deposits are calculated")
        void walletSeriesChronologicalOrderAndCumDeposits() {
            Wallet wallet = createWallet("My Wallet", "PLN", WalletType.INVESTMENT);
            // Snapshots inserted in random order
            createSnapshot(wallet, LocalDate.of(2024, 3, 1), new BigDecimal("3500"), new BigDecimal("1000"), new BigDecimal("200")); // net +800
            createSnapshot(wallet, LocalDate.of(2024, 1, 1), new BigDecimal("1000"), new BigDecimal("1000"), BigDecimal.ZERO);          // net +1000
            createSnapshot(wallet, LocalDate.of(2024, 2, 1), new BigDecimal("2200"), new BigDecimal("1000"), BigDecimal.ZERO);          // net +1000

            when(etfDataService.calculateBenchmarkValuesForTicker(anyString(), anyString(), anyList(), anyList(), anyString()))
                    .thenReturn(List.of(new BigDecimal("1000"), new BigDecimal("2000"), new BigDecimal("3000")));

            Map<String, Object> result = helper.getDashboard(List.of(wallet));

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> walletSeriesList = (List<Map<String, Object>>) result.get("walletSeries");
            assertThat(walletSeriesList).hasSize(1);

            Map<String, Object> walletEntry = walletSeriesList.get(0);
            assertThat(walletEntry.get("walletId")).isEqualTo(wallet.getId().toString());
            assertThat(walletEntry.get("walletName")).isEqualTo("My Wallet");
            assertThat(walletEntry.get("isInvestment")).isEqualTo(true);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> series = (List<Map<String, Object>>) walletEntry.get("series");
            assertThat(series).hasSize(3);

            // Month 1 (Jan): balance=1000, cumDeposit=1000
            assertThat(series.get(0).get("date")).isEqualTo("2024-01-01");
            assertThat(series.get(0).get("balance")).isEqualTo(new BigDecimal("1000.00"));
            assertThat(series.get(0).get("cumDeposit")).isEqualTo(new BigDecimal("1000.00"));

            // Month 2 (Feb): balance=2200, cumDeposit=1000+1000=2000
            assertThat(series.get(1).get("date")).isEqualTo("2024-02-01");
            assertThat(series.get(1).get("balance")).isEqualTo(new BigDecimal("2200.00"));
            assertThat(series.get(1).get("cumDeposit")).isEqualTo(new BigDecimal("2000.00"));

            // Month 3 (Mar): balance=3500, cumDeposit=2000+(1000-200)=2800
            assertThat(series.get(2).get("date")).isEqualTo("2024-03-01");
            assertThat(series.get(2).get("balance")).isEqualTo(new BigDecimal("3500.00"));
            assertThat(series.get(2).get("cumDeposit")).isEqualTo(new BigDecimal("2800.00"));
        }

        @Test
        @DisplayName("Wallet return rate formatting when return rate is zero or positive")
        void walletReturnRateFormatting() {
            Wallet wallet = createWallet("Stock Wallet", "PLN", WalletType.INVESTMENT);
            createSnapshot(wallet, LocalDate.of(2024, 1, 1), new BigDecimal("1000"), new BigDecimal("1000"), BigDecimal.ZERO);
            createSnapshot(wallet, LocalDate.of(2024, 2, 1), new BigDecimal("1200"), BigDecimal.ZERO, BigDecimal.ZERO);

            Map<String, Object> result = helper.getDashboard(List.of(wallet));

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> walletSeriesList = (List<Map<String, Object>>) result.get("walletSeries");
            Map<String, Object> walletEntry = walletSeriesList.get(0);

            // Return rate from wallet.getReturnRate(): deposits=1000, curr=1200 -> return=200/1000*100 = 20.00%
            assertThat(walletEntry.get("returnRate")).isEqualTo(new BigDecimal("20.00"));
        }

        @Test
        @DisplayName("Wallet with no return rate (e.g. 0 deposits) sets returnRate to BigDecimal.ZERO")
        void walletWithZeroReturnRate() {
            Wallet wallet = createWallet("Empty Wallet", "PLN", WalletType.SAVINGS);

            Map<String, Object> result = helper.getDashboard(List.of(wallet));

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> walletSeriesList = (List<Map<String, Object>>) result.get("walletSeries");
            Map<String, Object> walletEntry = walletSeriesList.get(0);
            assertThat(walletEntry.get("returnRate")).isEqualTo(new BigDecimal("0.00"));
        }

        @Test
        @DisplayName("Per-wallet series for foreign currency converts portfolio value and benchmarks to PLN")
        void walletSeriesForeignCurrencyConversion() {
            Wallet wallet = createWallet("USD Wallet", "USD", WalletType.INVESTMENT);
            createSnapshot(wallet, LocalDate.of(2024, 1, 1), new BigDecimal("1000"), new BigDecimal("1000"), BigDecimal.ZERO);

            // USD to PLN conversion: 1 USD = 4.00 PLN
            when(currencyConverter.convert(eq(new BigDecimal("1000")), eq(CurrencyConverter.Currency.USD), eq(CurrencyConverter.Currency.PLN)))
                    .thenReturn(new BigDecimal("4000.00"));
            when(currencyConverter.convert(eq(new BigDecimal("500")), eq(CurrencyConverter.Currency.USD), eq(CurrencyConverter.Currency.PLN)))
                    .thenReturn(new BigDecimal("2000.00"));

            // Benchmark returns in USD
            when(etfDataService.calculateBenchmarkValuesForTicker(eq(ETFDataService.SP500_TICKER), eq("USD"), anyList(), anyList(), eq("USD")))
                    .thenReturn(List.of(new BigDecimal("500")));

            Map<String, Object> result = helper.getDashboard(List.of(wallet));

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> walletSeriesList = (List<Map<String, Object>>) result.get("walletSeries");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> series = (List<Map<String, Object>>) walletSeriesList.get(0).get("series");
            assertThat(series).hasSize(1);

            Map<String, Object> pt = series.get(0);
            assertThat(pt.get("balance")).isEqualTo(new BigDecimal("4000.00"));
            assertThat(pt.get("cumDeposit")).isEqualTo(new BigDecimal("4000.00"));
            assertThat(pt.get("sp500")).isEqualTo(new BigDecimal("2000.00"));
        }
    }

    @Nested
    @DisplayName("Special Wallet Types & Months Span")
    class SpecialTypesAndSpans {

        @Test
        @DisplayName("Unknown or null walletType defaults to isInvestmentLike = true")
        void unknownWalletTypeDefaultsToInvestmentLike() {
            Wallet wallet = createWallet("Custom Wallet", "PLN", null);
            wallet.setWalletType("CUSTOM_TYPE");
            createSnapshot(wallet, LocalDate.of(2024, 1, 1), new BigDecimal("5000"), new BigDecimal("5000"), BigDecimal.ZERO);

            Map<String, Object> result = helper.getDashboard(List.of(wallet));

            @SuppressWarnings("unchecked")
            Map<String, Object> kpi = (Map<String, Object>) result.get("kpi");
            assertThat(kpi.get("investBalance")).isEqualTo(new BigDecimal("5000.00"));
            assertThat(kpi.get("savingsBalance")).isEqualTo(new BigDecimal("0.00"));
        }

        @Test
        @DisplayName("Null walletType string defaults to isInvestmentLike = true")
        void nullWalletTypeDefaultsToInvestmentLike() {
            Wallet wallet = createWallet("Null Type Wallet", "PLN", null);
            wallet.setWalletType(null);
            createSnapshot(wallet, LocalDate.of(2024, 1, 1), new BigDecimal("3000"), new BigDecimal("3000"), BigDecimal.ZERO);

            Map<String, Object> result = helper.getDashboard(List.of(wallet));

            @SuppressWarnings("unchecked")
            Map<String, Object> kpi = (Map<String, Object>) result.get("kpi");
            assertThat(kpi.get("investBalance")).isEqualTo(new BigDecimal("3000.00"));
        }

        @Test
        @DisplayName("Months span is calculated across earliest snapshot of any wallet to latest snapshot of any wallet")
        void monthsSpanAcrossMultipleWallets() {
            Wallet w1 = createWallet("Wallet 1", "PLN", WalletType.INVESTMENT);
            createSnapshot(w1, LocalDate.of(2023, 1, 1), new BigDecimal("1000"), new BigDecimal("1000"), BigDecimal.ZERO);
            createSnapshot(w1, LocalDate.of(2023, 6, 1), new BigDecimal("1200"), BigDecimal.ZERO, BigDecimal.ZERO);

            Wallet w2 = createWallet("Wallet 2", "PLN", WalletType.INVESTMENT);
            createSnapshot(w2, LocalDate.of(2023, 4, 1), new BigDecimal("2000"), new BigDecimal("2000"), BigDecimal.ZERO);
            createSnapshot(w2, LocalDate.of(2024, 1, 1), new BigDecimal("2500"), BigDecimal.ZERO, BigDecimal.ZERO);

            Map<String, Object> result = helper.getDashboard(List.of(w1, w2));

            @SuppressWarnings("unchecked")
            Map<String, Object> kpi = (Map<String, Object>) result.get("kpi");
            // Min date: 2023-01-01, Max date: 2024-01-01 -> 12 months + 1 = 13 months
            assertThat(kpi.get("investMonthsSpan")).isEqualTo(13);
        }

        @Test
        @DisplayName("Single snapshot in wallet results in monthsSpan = 1")
        void singleSnapshotMonthsSpanIsOne() {
            Wallet w = createWallet("Single Snap Wallet", "PLN", WalletType.INVESTMENT);
            createSnapshot(w, LocalDate.of(2024, 5, 15), new BigDecimal("5000"), new BigDecimal("5000"), BigDecimal.ZERO);

            Map<String, Object> result = helper.getDashboard(List.of(w));

            @SuppressWarnings("unchecked")
            Map<String, Object> kpi = (Map<String, Object>) result.get("kpi");
            assertThat(kpi.get("investMonthsSpan")).isEqualTo(1);
        }
    }
}
