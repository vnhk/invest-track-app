package com.bervan.investtrack.view.dashboards;

import com.bervan.investtrack.model.Wallet;
import com.bervan.investtrack.model.WalletSnapshot;
import com.bervan.investtrack.service.CurrencyConverter;
import com.bervan.investtrack.service.InvestmentCalculationService;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Multi-wallet comparison view with table and metrics
 */
@CssImport("./invest-track-dashboard.css")
public class WalletComparisonView extends VerticalLayout {

    private final List<Wallet> wallets;
    private final CurrencyConverter currencyConverter;
    private final InvestmentCalculationService calculationService;
    private final NumberFormat currencyFormat;

    public WalletComparisonView(List<Wallet> wallets,
                                 CurrencyConverter currencyConverter,
                                 InvestmentCalculationService calculationService) {
        this.wallets = wallets;
        this.currencyConverter = currencyConverter;
        this.calculationService = calculationService;

        this.currencyFormat = NumberFormat.getCurrencyInstance(new Locale("pl", "PL"));
        currencyFormat.setMaximumFractionDigits(0);

        addClassName("invest-dashboard");
        setSizeFull();
        setPadding(true);

        add(createComparisonTable());
    }

    private Div createComparisonTable() {
        Div card = new Div();
        card.addClassName("invest-chart-card");

        H3 title = new H3("Wallet Comparison");
        title.addClassName("chart-title");

        Grid<WalletMetrics> grid = new Grid<>(WalletMetrics.class, false);
        grid.addClassName("invest-comparison-table");
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        grid.setWidthFull();

        grid.addColumn(WalletMetrics::name).setHeader("Wallet").setFlexGrow(2);
        grid.addColumn(WalletMetrics::currency).setHeader("Currency").setAutoWidth(true);
        grid.addColumn(WalletMetrics::riskLevel).setHeader("Risk").setAutoWidth(true);
        grid.addColumn(m -> currencyFormat.format(m.balancePLN())).setHeader("Balance (PLN)").setAutoWidth(true);
        grid.addColumn(m -> currencyFormat.format(m.depositsPLN())).setHeader("Deposits (PLN)").setAutoWidth(true);
        grid.addColumn(m -> currencyFormat.format(m.returnPLN())).setHeader("Return (PLN)").setAutoWidth(true);
        grid.addColumn(new ComponentRenderer<>(m -> {
            Span span = new Span(m.returnRate().setScale(2, RoundingMode.HALF_UP) + "%");
            span.addClassName(m.returnRate().compareTo(BigDecimal.ZERO) >= 0 ?
                    "invest-text-success" : "invest-text-danger");
            return span;
        })).setHeader("Return %").setAutoWidth(true);
        grid.addColumn(new ComponentRenderer<>(m -> {
            Span span = new Span(m.cagr().setScale(2, RoundingMode.HALF_UP) + "%");
            span.addClassName(m.cagr().compareTo(BigDecimal.ZERO) >= 0 ?
                    "invest-text-success" : "invest-text-danger");
            return span;
        })).setHeader("CAGR").setAutoWidth(true);
        grid.addColumn(new ComponentRenderer<>(m -> {
            Span span = new Span(m.twr().setScale(2, RoundingMode.HALF_UP) + "%");
            span.addClassName(m.twr().compareTo(BigDecimal.ZERO) >= 0 ?
                    "invest-text-success" : "invest-text-danger");
            return span;
        })).setHeader("TWR").setAutoWidth(true);

        List<WalletMetrics> metrics = calculateMetrics();
        grid.setItems(metrics);

        card.add(title, grid);
        return card;
    }

    private List<WalletMetrics> calculateMetrics() {
        List<WalletMetrics> result = new ArrayList<>();

        for (Wallet wallet : wallets) {
            BigDecimal balancePLN = currencyConverter.convert(
                    wallet.getCurrentValue(),
                    CurrencyConverter.Currency.of(wallet.getCurrency()),
                    CurrencyConverter.Currency.PLN
            );

            BigDecimal depositsPLN = currencyConverter.convert(
                    wallet.getTotalDeposits().subtract(wallet.getTotalWithdrawals()),
                    CurrencyConverter.Currency.of(wallet.getCurrency()),
                    CurrencyConverter.Currency.PLN
            );

            BigDecimal returnPLN = balancePLN.subtract(depositsPLN);
            BigDecimal returnRate = wallet.getReturnRate();

            // Calculate CAGR
            double years = calculateYears(wallet);
            BigDecimal cagr = calculationService.calculateCAGR(depositsPLN, balancePLN, years)
                    .multiply(BigDecimal.valueOf(100));

            // Calculate TWR
            List<WalletSnapshot> snapshots = new ArrayList<>(wallet.getSnapshots());
            snapshots.sort(Comparator.comparing(WalletSnapshot::getSnapshotDate));
            BigDecimal twr = calculationService.calculateTWR(snapshots)
                    .multiply(BigDecimal.valueOf(100));

            result.add(new WalletMetrics(
                    wallet.getName(),
                    wallet.getCurrency(),
                    wallet.getRiskLevel(),
                    balancePLN,
                    depositsPLN,
                    returnPLN,
                    returnRate,
                    cagr,
                    twr
            ));
        }

        // Sort by balance descending
        result.sort((a, b) -> b.balancePLN().compareTo(a.balancePLN()));

        return result;
    }

    private double calculateYears(Wallet wallet) {
        List<WalletSnapshot> snapshots = wallet.getSnapshots();
        if (snapshots.isEmpty()) return 1;

        LocalDate firstDate = snapshots.stream()
                .map(WalletSnapshot::getSnapshotDate)
                .min(LocalDate::compareTo)
                .orElse(LocalDate.now());

        LocalDate lastDate = snapshots.stream()
                .map(WalletSnapshot::getSnapshotDate)
                .max(LocalDate::compareTo)
                .orElse(LocalDate.now());

        long months = ChronoUnit.MONTHS.between(firstDate, lastDate) + 1;
        return Math.max(months / 12.0, 0.1);
    }

    public record WalletMetrics(
            String name,
            String currency,
            String riskLevel,
            BigDecimal balancePLN,
            BigDecimal depositsPLN,
            BigDecimal returnPLN,
            BigDecimal returnRate,
            BigDecimal cagr,
            BigDecimal twr
    ) {}
}
