package com.bervan.investtrack.view.dashboards;

import com.bervan.investtrack.model.Wallet;
import com.bervan.investtrack.service.CurrencyConverter;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.data.renderer.ComponentRenderer;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.*;

public class FirePathView extends VerticalLayout {
    private static final BigDecimal GLOBAL_TARGET = BigDecimal.valueOf(1_500_000L); // PLN
    private static final BigDecimal INFLATION = BigDecimal.valueOf(0.03); // 3%
    private final CurrencyConverter currencyConverter;
    private final List<Wallet> wallets;
    private final Map<UUID, List<BigDecimal>> balances;
    private final Map<UUID, List<BigDecimal>> sumOfDeposits;

    public FirePathView(CurrencyConverter currencyConverter, List<Wallet> wallets, Map<UUID, List<BigDecimal>> balances, Map<UUID, List<BigDecimal>> sumOfDeposits, Map<UUID, List<String>> dates) {
        this.currencyConverter = currencyConverter;
        this.wallets = wallets;
        this.balances = balances;
        this.sumOfDeposits = sumOfDeposits;

        setSizeFull();
        setPadding(false);
        setSpacing(false);
        addClassName("fire-view");

        add(createMainContent());
    }

    private Component createMainContent() {
        VerticalLayout content = new VerticalLayout();
        content.addClassName("fire-content");
        content.setSizeFull();
        content.setPadding(true);
        content.setSpacing(false);

        Div stagesCard = createStagesCard();

        HorizontalLayout bottomRow = new HorizontalLayout();
        bottomRow.addClassName("fire-bottom-row");
        bottomRow.setWidthFull();
        bottomRow.setSpacing(true);

        Component progressCard = createProgressCard();

        bottomRow.add(progressCard);
        bottomRow.setFlexGrow(1, progressCard);

        content.add(stagesCard, bottomRow);
        content.setFlexGrow(1, stagesCard);
        return content;
    }

    private Div createStagesCard() {
        Div card = new Div();
        card.addClassName("fire-card");
        card.setWidthFull();

        H3 title = new H3("FIRE Stages");
        title.addClassName("card-title");

        Grid<FireStage> grid = createStagesGrid();

        Span note = new Span(
                "* Values are projections using historical deposits and an estimated monthly return after inflation (inflation = 3%).");
        note.addClassName("card-note");

        card.add(title, grid, note);
        return card;
    }

    private Grid<FireStage> createStagesGrid() {
        Grid<FireStage> grid = new Grid<>(FireStage.class, false);
        grid.addClassName("stages-grid");
        grid.setWidthFull();
        grid.setHeight("320px");

        grid.addColumn(FireStage::getStageName).setHeader("Stage").setAutoWidth(true).setFlexGrow(2);
        grid.addColumn(FireStage::getPercent).setHeader("% goal").setAutoWidth(true);
        grid.addColumn(FireStage::getAmount).setHeader("Amount").setAutoWidth(true);
        grid.addColumn(FireStage::getHowMuchLeft).setHeader("How much left").setAutoWidth(true);
        grid.addColumn(FireStage::getHowManyMonths).setHeader("How many months?").setAutoWidth(true);

        grid.addColumn(new ComponentRenderer<>(stage -> {
            ProgressBar pb = new ProgressBar(0, 1, stage.getProgress());
            pb.addClassName("fire-progress");
            return pb;
        })).setHeader("Progress").setFlexGrow(2);

        grid.setItems(computeStages());
        return grid;
    }

    private List<FireStage> computeStages() {
        // stage definitions (12 stages)
        List<StageDef> stageDefs = Arrays.asList(
                new StageDef("Initial Spark", BigDecimal.valueOf(1)),
                new StageDef("First Milestone", BigDecimal.valueOf(2)),
                new StageDef("Early Growth", BigDecimal.valueOf(5)),
                new StageDef("Momentum Phase", BigDecimal.valueOf(10)),
                new StageDef("Quarter Mark", BigDecimal.valueOf(25)),
                new StageDef("Steady Path", BigDecimal.valueOf(35)),
                new StageDef("Halfway There", BigDecimal.valueOf(50)),
                new StageDef("Comfort Zone", BigDecimal.valueOf(60)),
                new StageDef("Strong Position", BigDecimal.valueOf(70)),
                new StageDef("Three-Quarters Mark", BigDecimal.valueOf(75)),
                new StageDef("Lean FIRE", BigDecimal.valueOf(80)),
                new StageDef("Full FIRE", BigDecimal.valueOf(100))
        );

        // combined current balance and deposits (converted to PLN)
        BigDecimal combinedCurrentBalance = BigDecimal.ZERO;
        BigDecimal combinedTotalDeposits = BigDecimal.ZERO;
        int maxMonths = 0;
        int totalTimelinePointsWeighted = 0; // not used for now but available if needed

        for (Wallet wallet : wallets) {
            UUID wId = wallet.getId();
            List<BigDecimal> walletBalances = balances.getOrDefault(wId, Collections.emptyList());
            List<BigDecimal> walletDeposits = sumOfDeposits.getOrDefault(wId, Collections.emptyList());

            if (!walletBalances.isEmpty()) {
                BigDecimal lastBalance = walletBalances.get(walletBalances.size() - 1);
                BigDecimal convertedBalance = convert(lastBalance, CurrencyConverter.Currency.of(wallet.getCurrency()), CurrencyConverter.Currency.PLN);
                combinedCurrentBalance = combinedCurrentBalance.add(convertedBalance);
                maxMonths = Math.max(maxMonths, walletBalances.size());
            }

            if (!walletDeposits.isEmpty()) {
                BigDecimal lastDepositSum = walletDeposits.get(walletDeposits.size() - 1);
                BigDecimal convertedDeposits = convert(lastDepositSum, CurrencyConverter.Currency.of(wallet.getCurrency()), CurrencyConverter.Currency.PLN);
                combinedTotalDeposits = combinedTotalDeposits.add(convertedDeposits);
                maxMonths = Math.max(maxMonths, walletDeposits.size());
            }
        }

        // Estimate average monthly deposit across the observation window.
        // If there are N months (maxMonths), assume deposits are distributed across those months.
        BigDecimal avgMonthlyDeposit = BigDecimal.ZERO;
        if (maxMonths > 0) {
            avgMonthlyDeposit = combinedTotalDeposits.divide(BigDecimal.valueOf(maxMonths), 18, RoundingMode.HALF_UP);
        }

        // Calculate annualized return based on total deposits -> current balance.
        // if combinedTotalDeposits is zero, treat return as zero.
        double annualReturn;
        if (combinedTotalDeposits.compareTo(BigDecimal.ZERO) <= 0 || combinedCurrentBalance.compareTo(BigDecimal.ZERO) <= 0) {
            annualReturn = 0.0;
        } else {
            double totalMultiplier = combinedCurrentBalance.divide(combinedTotalDeposits, 18, RoundingMode.HALF_UP).doubleValue();
            double years = Math.max(1.0, ((double) maxMonths) / 12.0); // avoid divide by zero; if months < 12 assume 1 year
            // annualized geometric return
            annualReturn = Math.pow(totalMultiplier, 1.0 / years) - 1.0;
        }

        // monthly return after inflation
        double monthlyReturn = (annualReturn - INFLATION.doubleValue()) / 12.0;

        // prepare formatter for PLN without decimals
        NumberFormat nf = NumberFormat.getCurrencyInstance(new Locale("pl", "PL"));
        nf.setMaximumFractionDigits(0);
        nf.setRoundingMode(RoundingMode.HALF_UP);

        List<FireStage> result = new ArrayList<>();
        for (StageDef def : stageDefs) {
            BigDecimal pct = def.percent.divide(BigDecimal.valueOf(100), 18, RoundingMode.HALF_UP);
            BigDecimal stageAmount = GLOBAL_TARGET.multiply(pct).setScale(0, RoundingMode.HALF_UP);
            BigDecimal howMuchLeft = stageAmount.subtract(combinedCurrentBalance);
            boolean achieved = howMuchLeft.compareTo(BigDecimal.ZERO) <= 0;
            String howMuchLeftStr = achieved ? "Achieved" : nf.format(howMuchLeft);

            double progress;
            if (stageAmount.compareTo(BigDecimal.ZERO) == 0) {
                progress = 0.0;
            } else {
                progress = combinedCurrentBalance.divide(stageAmount, 6, RoundingMode.HALF_UP).doubleValue();
                if (progress > 1.0) progress = 1.0;
                if (progress < 0.0) progress = 0.0;
            }

            String monthsStr;
            if (achieved) {
                monthsStr = "—";
            } else {
                double monthsNeeded = estimateMonthsToTarget(
                        combinedCurrentBalance.doubleValue(),
                        avgMonthlyDeposit.doubleValue(),
                        monthlyReturn,
                        stageAmount.doubleValue()
                );
                if (Double.isInfinite(monthsNeeded) || Double.isNaN(monthsNeeded) || monthsNeeded > 1200) {
                    monthsStr = "Long term";
                } else {
                    monthsStr = formatMonths((int) Math.ceil(monthsNeeded));
                }
            }

            String percentLabel = def.percent.stripTrailingZeros().toPlainString() + "%";
            String amountLabel = nf.format(stageAmount);

            result.add(new FireStage(def.name, percentLabel, amountLabel, howMuchLeftStr, monthsStr, progress));
        }

        return result;
    }

    /**
     * Estimate months to reach target using compound growth with monthly deposits:
     *
     * future(t) = current*(1+r)^t + monthly * [((1+r)^t - 1) / r]
     *
     * Solve numerically for t (months). r is monthly return (e.g., 0.0025).
     *
     * If r == 0, fallback to linear: (target - current) / monthly
     */
    private double estimateMonthsToTarget(double current, double monthly, double monthlyReturn, double target) {
        if (current >= target) return 0.0;
        double left = target - current;

        // if monthlyReturn is approximately zero, do linear estimate
        if (Math.abs(monthlyReturn) < 1e-12) {
            if (monthly <= 0.0) return Double.POSITIVE_INFINITY;
            return Math.max(0.0, left / monthly);
        }

        // numerical search: binary search on t in [0, maxT]
        double low = 0.0;
        double high = 1200.0; // 100 years in months - safety cap
        for (int iter = 0; iter < 80; iter++) {
            double mid = (low + high) / 2.0;
            double val = futureValue(current, monthly, monthlyReturn, mid);
            if (val >= target) {
                high = mid;
            } else {
                low = mid;
            }
        }
        double result = Math.ceil(high);
        // check feasibility: if even at high we don't reach, return infinity
        if (futureValue(current, monthly, monthlyReturn, high) < target - 0.5) {
            return Double.POSITIVE_INFINITY;
        }
        return result;
    }

    private double futureValue(double current, double monthly, double monthlyReturn, double months) {
        // current * (1+r)^t + monthly * [((1+r)^t - 1) / r]
        double factor = Math.pow(1.0 + monthlyReturn, months);
        return current * factor + (Math.abs(monthlyReturn) < 1e-12
                ? monthly * months
                : monthly * ((factor - 1.0) / monthlyReturn));
    }

    private String formatMonths(int months) {
        if (months <= 0) return "0 mos";
        int years = months / 12;
        int remMonths = months % 12;
        if (years > 0 && remMonths > 0) {
            return String.format("%d yr %d mos", years, remMonths);
        } else if (years > 0) {
            return String.format("%d yr", years);
        } else {
            return String.format("%d mos", remMonths);
        }
    }

    private Component createProgressCard() {
        Div card = new Div();
        card.addClassName("fire-card");

        H3 title = new H3("Goal progress and variance");
        title.addClassName("card-title");

        HorizontalLayout legend = new HorizontalLayout();
        legend.addClassName("card-legend");
        legend.setSpacing(true);

        legend.add(
                createLegendItem("Different returns (±2 pp)"),
                createLegendItem("Different deposits (±10%)")
        );

        Div chartPlaceholder = new Div();
        chartPlaceholder.addClassName("chart-placeholder");
        chartPlaceholder.setText("Placeholder for chart (Vaadin Charts / other library)");

        Span sliderLabel = new Span("How many years do you invest?");
        sliderLabel.addClassName("section-label");

        NumberField yearsSlider = new NumberField();
        yearsSlider.setMin(0.0);
        yearsSlider.setMax(30.0);
        yearsSlider.setValue(14.0);
        yearsSlider.setStep(1);
        yearsSlider.setWidthFull();
        yearsSlider.setStepButtonsVisible(true);

        HorizontalLayout metrics = new HorizontalLayout();
        metrics.addClassName("metrics-row");
        metrics.setWidthFull();
        metrics.setSpacing(true);

        // compute summary metrics for display
        BigDecimal combinedCurrentBalance = BigDecimal.ZERO;
        BigDecimal combinedTotalDeposits = BigDecimal.ZERO;
        int maxMonths = 0;

        for (Wallet wallet : wallets) {
            UUID wId = wallet.getId();
            List<BigDecimal> walletBalances = balances.getOrDefault(wId, Collections.emptyList());
            List<BigDecimal> walletDeposits = sumOfDeposits.getOrDefault(wId, Collections.emptyList());

            if (!walletBalances.isEmpty()) {
                BigDecimal lastBalance = walletBalances.get(walletBalances.size() - 1);
                BigDecimal convertedBalance = convert(lastBalance, CurrencyConverter.Currency.of(wallet.getCurrency()), CurrencyConverter.Currency.PLN);
                combinedCurrentBalance = combinedCurrentBalance.add(convertedBalance);
                maxMonths = Math.max(maxMonths, walletBalances.size());
            }

            if (!walletDeposits.isEmpty()) {
                BigDecimal lastDepositSum = walletDeposits.get(walletDeposits.size() - 1);
                BigDecimal convertedDeposits = convert(lastDepositSum, CurrencyConverter.Currency.of(wallet.getCurrency()), CurrencyConverter.Currency.PLN);
                combinedTotalDeposits = combinedTotalDeposits.add(convertedDeposits);
                maxMonths = Math.max(maxMonths, walletDeposits.size());
            }
        }

        BigDecimal avgMonthlyDeposit = BigDecimal.ZERO;
        if (maxMonths > 0) {
            avgMonthlyDeposit = combinedTotalDeposits.divide(BigDecimal.valueOf(maxMonths), 18, RoundingMode.HALF_UP);
        }

        double annualReturn;
        if (combinedTotalDeposits.compareTo(BigDecimal.ZERO) <= 0 || combinedCurrentBalance.compareTo(BigDecimal.ZERO) <= 0) {
            annualReturn = 0.0;
        } else {
            double totalMultiplier = combinedCurrentBalance.divide(combinedTotalDeposits, 18, RoundingMode.HALF_UP).doubleValue();
            double years = Math.max(1.0, ((double) maxMonths) / 12.0);
            annualReturn = Math.pow(totalMultiplier, 1.0 / years) - 1.0;
        }

        // estimate three variants for display: lower return (-2pp), baseline, higher return (+2pp)
        BigDecimal lowerVariant = GLOBAL_TARGET.multiply(BigDecimal.valueOf((1.0 + (annualReturn - 0.02))))
                .setScale(0, RoundingMode.HALF_UP);
        BigDecimal baselineVariant = GLOBAL_TARGET.multiply(BigDecimal.valueOf((1.0 + annualReturn)))
                .setScale(0, RoundingMode.HALF_UP);
        BigDecimal higherVariant = GLOBAL_TARGET.multiply(BigDecimal.valueOf((1.0 + (annualReturn + 0.02))))
                .setScale(0, RoundingMode.HALF_UP);

        NumberFormat nf = NumberFormat.getCurrencyInstance(new Locale("pl", "PL"));
        nf.setMaximumFractionDigits(0);

        metrics.add(
                createMetric("Lower-return variant", nf.format(lowerVariant)),
                createMetric("Plan (baseline)", nf.format(baselineVariant)),
                createMetric("Higher-return variant", nf.format(higherVariant)),
                createMetric("Current capital", nf.format(combinedCurrentBalance))
        );

        Div bottomInfo = new Div();
        // compute short gap example for baseline over a 19-year horizon just as an illustrative number
        // (not a promise — this is for UI summarization)
        bottomInfo.addClassName("bottom-info");
        bottomInfo.setText("Target: " + nf.format(GLOBAL_TARGET) + ".");

        card.add(title, legend, chartPlaceholder, sliderLabel, yearsSlider, metrics, bottomInfo);
        return card;
    }

    private Component createLegendItem(String text) {
        HorizontalLayout item = new HorizontalLayout();
        item.setSpacing(true);
        item.addClassName("legend-item");

        Div dot = new Div();
        dot.addClassName("legend-dot");

        Span label = new Span(text);
        label.addClassName("legend-label");

        item.add(dot, label);
        return item;
    }

    private Component createMetric(String label, String value) {
        Div box = new Div();
        box.addClassName("metric-box");

        Span v = new Span(value);
        v.addClassName("metric-value");

        Span l = new Span(label);
        l.addClassName("metric-label");

        box.add(v, l);
        return box;
    }

    private BigDecimal convert(BigDecimal amount, CurrencyConverter.Currency fromCurrency, CurrencyConverter.Currency toCurrency) {
        return currencyConverter.convert(amount, fromCurrency, toCurrency);
    }

    private static class StageDef {
        final String name;
        final BigDecimal percent;

        StageDef(String name, BigDecimal percent) {
            this.name = name;
            this.percent = percent;
        }
    }
}