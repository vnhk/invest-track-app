package com.bervan.investtrack.view.dashboards;

import com.bervan.investtrack.model.Wallet;
import com.bervan.investtrack.model.WalletType;
import com.bervan.investtrack.service.CurrencyConverter;
import com.bervan.investtrack.view.charts.FireProjectionChart;
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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class FirePathView extends VerticalLayout {
    private static final BigDecimal DEFAULT_TARGET = BigDecimal.valueOf(1_500_000L); // PLN
    private static final BigDecimal INFLATION = BigDecimal.valueOf(0.038); // 3.8% last 10 years in PL
    private final CurrencyConverter currencyConverter;
    private final List<Wallet> wallets;
    private final Map<UUID, List<BigDecimal>> balances;
    private final Map<UUID, List<BigDecimal>> sumOfDeposits;
    private final Map<UUID, List<String>> dates;

    private BigDecimal fireTarget;
    private VerticalLayout mainContentLayout;

    public FirePathView(CurrencyConverter currencyConverter, List<Wallet> wallets,
                        Map<UUID, List<BigDecimal>> balances, Map<UUID, List<BigDecimal>> sumOfDeposits,
                        Map<UUID, List<String>> dates) {
        this(currencyConverter, wallets, balances, sumOfDeposits, dates, DEFAULT_TARGET);
    }

    public FirePathView(CurrencyConverter currencyConverter, List<Wallet> wallets,
                        Map<UUID, List<BigDecimal>> balances, Map<UUID, List<BigDecimal>> sumOfDeposits,
                        Map<UUID, List<String>> dates, BigDecimal customTarget) {
        this.currencyConverter = currencyConverter;
        this.wallets = wallets;
        this.balances = balances;
        this.sumOfDeposits = sumOfDeposits;
        this.dates = dates;
        this.fireTarget = customTarget != null ? customTarget : DEFAULT_TARGET;

        setSizeFull();
        setPadding(false);
        setSpacing(false);
        addClassName("fire-view");

        add(createGoalEditor());
        mainContentLayout = new VerticalLayout();
        mainContentLayout.setSizeFull();
        mainContentLayout.setPadding(false);
        mainContentLayout.setSpacing(false);
        mainContentLayout.add(createMainContent());
        add(mainContentLayout);
    }

    private HorizontalLayout createGoalEditor() {
        HorizontalLayout goalLayout = new HorizontalLayout();
        goalLayout.setWidthFull();
        goalLayout.setAlignItems(Alignment.CENTER);
        goalLayout.addClassName("fire-goal-editor");

        Span label = new Span("FIRE Goal (PLN):");

        NumberField goalField = new NumberField();
        goalField.setValue(fireTarget.doubleValue());
        goalField.setMin(10000);
        goalField.setMax(100000000);
        goalField.setStep(50000);
        goalField.setStepButtonsVisible(true);
        goalField.setWidth("200px");

        Span hint = new Span("Change the goal and the projections will update automatically.");
        hint.getStyle().set("font-size", "0.875rem")
                .set("color", "var(--invest-text-secondary, rgba(255,255,255,0.7))");

        goalField.addValueChangeListener(event -> {
            if (event.getValue() != null && event.getValue() > 0) {
                fireTarget = BigDecimal.valueOf(event.getValue());
                refreshContent();
            }
        });

        goalLayout.add(label, goalField, hint);
        return goalLayout;
    }

    private void refreshContent() {
        mainContentLayout.removeAll();
        mainContentLayout.add(createMainContent());
    }

    private static double getMonthlyReturn(long monthsBetween, BigDecimal totalDeposits, BigDecimal currentBalance) {
        if (totalDeposits.compareTo(BigDecimal.ZERO) <= 0 || currentBalance.compareTo(BigDecimal.ZERO) <= 0 || monthsBetween <= 0) {
            return 0.0;
        }

        double years = monthsBetween / 12.0;
        double totalMultiplier = currentBalance.divide(totalDeposits, 18, RoundingMode.HALF_UP).doubleValue();
        double annualReturn = Math.pow(totalMultiplier, 1.0 / years) - 1.0;
        double realAnnualReturn = (1 + annualReturn) / (1 + INFLATION.doubleValue()) - 1;
        return Math.pow(1 + realAnnualReturn, 1.0 / 12.0) - 1;
    }

    private record PortfolioTotals(
            BigDecimal investBalance,
            BigDecimal investDeposits,
            BigDecimal savingsBalance,
            BigDecimal savingsDeposits,
            long monthsBetween
    ) {
        BigDecimal totalBalance() { return investBalance.add(savingsBalance); }
    }

    private PortfolioTotals computeTotals() {
        BigDecimal investBalance = BigDecimal.ZERO;
        BigDecimal investDeposits = BigDecimal.ZERO;
        BigDecimal savingsBalance = BigDecimal.ZERO;
        BigDecimal savingsDeposits = BigDecimal.ZERO;

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        LocalDate firstDate = LocalDate.MAX;
        LocalDate lastDate = LocalDate.MIN;

        for (Wallet wallet : wallets) {
            UUID wId = wallet.getId();
            List<BigDecimal> walletBalances = balances.getOrDefault(wId, Collections.emptyList());
            List<BigDecimal> sumOfWalletDeposits = sumOfDeposits.getOrDefault(wId, Collections.emptyList());
            List<String> walletDates = dates.getOrDefault(wId, Collections.emptyList());

            if (walletBalances.isEmpty() || sumOfWalletDeposits.isEmpty() || walletDates.isEmpty()) {
                continue;
            }

            BigDecimal lastBalance = convert(walletBalances.get(walletBalances.size() - 1),
                    CurrencyConverter.Currency.of(wallet.getCurrency()), CurrencyConverter.Currency.PLN);
            BigDecimal lastDeposits = convert(sumOfWalletDeposits.get(sumOfWalletDeposits.size() - 1),
                    CurrencyConverter.Currency.of(wallet.getCurrency()), CurrencyConverter.Currency.PLN);

            if (wallet.isInvestmentLike()) {
                investBalance = investBalance.add(lastBalance);
                investDeposits = investDeposits.add(lastDeposits);
            } else {
                savingsBalance = savingsBalance.add(lastBalance);
                savingsDeposits = savingsDeposits.add(lastDeposits);
            }

            for (String dStr : walletDates) {
                LocalDate d = LocalDate.parse(dStr, formatter);
                if (d.isBefore(firstDate)) firstDate = d;
                if (d.isAfter(lastDate)) lastDate = d;
            }
        }

        if (firstDate.equals(LocalDate.MAX) || lastDate.equals(LocalDate.MIN)) {
            firstDate = LocalDate.now().minusYears(1);
            lastDate = LocalDate.now();
        }

        long monthsBetween = ChronoUnit.MONTHS.between(firstDate.withDayOfMonth(1), lastDate.withDayOfMonth(1)) + 1;
        if (monthsBetween <= 0) monthsBetween = 1;

        return new PortfolioTotals(investBalance, investDeposits, savingsBalance, savingsDeposits, monthsBetween);
    }

    private Component createMainContent() {
        PortfolioTotals totals = computeTotals();

        double avgMonthlyInvestDeposit = totals.investDeposits().compareTo(BigDecimal.ZERO) > 0
                ? totals.investDeposits().divide(BigDecimal.valueOf(totals.monthsBetween()), 18, RoundingMode.HALF_UP).doubleValue()
                : 0.0;
        double avgMonthlySavingsDeposit = totals.savingsDeposits().compareTo(BigDecimal.ZERO) > 0
                ? totals.savingsDeposits().divide(BigDecimal.valueOf(totals.monthsBetween()), 18, RoundingMode.HALF_UP).doubleValue()
                : 0.0;

        double monthlyReturn = getMonthlyReturn(totals.monthsBetween(), totals.investDeposits(), totals.investBalance());

        VerticalLayout content = new VerticalLayout();
        content.addClassName("fire-content");
        content.setSizeFull();
        content.setPadding(true);
        content.setSpacing(false);

        Div stagesCard = createStagesCard(totals, avgMonthlyInvestDeposit, avgMonthlySavingsDeposit, monthlyReturn);

        HorizontalLayout bottomRow = new HorizontalLayout();
        bottomRow.addClassName("fire-bottom-row");
        bottomRow.setWidthFull();

        Component progressCard = createProgressCard(totals, avgMonthlyInvestDeposit, avgMonthlySavingsDeposit, monthlyReturn);

        bottomRow.add(progressCard);
        bottomRow.setFlexGrow(1, progressCard);

        content.add(stagesCard, bottomRow);
        content.setFlexGrow(1, stagesCard);
        return content;
    }

    private Div createStagesCard(PortfolioTotals totals, double avgMonthlyInvestDeposit,
                                  double avgMonthlySavingsDeposit, double monthlyReturn) {
        Div card = new Div();
        card.addClassName("fire-card");
        card.setWidth("95%");
        card.setHeightFull();

        H3 title = new H3("FIRE Stages");
        title.addClassName("card-title");

        Grid<FireStage> grid = createStagesGrid(totals, avgMonthlyInvestDeposit, avgMonthlySavingsDeposit, monthlyReturn);

        Span note = new Span(
                "* Values are projections using historical deposits and an estimated monthly return after inflation (inflation = "
                        + INFLATION.multiply(BigDecimal.valueOf(100L)).stripTrailingZeros().toPlainString()
                        + "%). Savings accounts are counted towards the FIRE goal but without investment returns.");
        note.addClassName("card-note");

        card.add(title, grid, note);
        return card;
    }

    private Grid<FireStage> createStagesGrid(PortfolioTotals totals, double avgMonthlyInvestDeposit,
                                              double avgMonthlySavingsDeposit, double monthlyReturn) {
        Grid<FireStage> grid = new Grid<>(FireStage.class, false);
        grid.addClassName("stages-grid");
        grid.setWidthFull();
        grid.setHeight("500px");

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

        grid.setItems(computeStages(totals, avgMonthlyInvestDeposit, avgMonthlySavingsDeposit, monthlyReturn));
        return grid;
    }

    private List<FireStage> computeStages(PortfolioTotals totals, double avgMonthlyInvestDeposit,
                                           double avgMonthlySavingsDeposit, double monthlyReturn) {
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

        NumberFormat nf = NumberFormat.getCurrencyInstance(new Locale("pl", "PL"));
        nf.setMaximumFractionDigits(0);
        nf.setRoundingMode(RoundingMode.HALF_UP);

        double combinedCurrentBalance = totals.totalBalance().doubleValue();

        List<FireStage> result = new ArrayList<>();
        for (StageDef def : stageDefs) {
            BigDecimal pct = def.percent.divide(BigDecimal.valueOf(100), 18, RoundingMode.HALF_UP);
            BigDecimal stageAmount = fireTarget.multiply(pct).setScale(0, RoundingMode.HALF_UP);
            BigDecimal howMuchLeft = stageAmount.subtract(totals.totalBalance());
            boolean achieved = howMuchLeft.compareTo(BigDecimal.ZERO) <= 0;
            String howMuchLeftStr = achieved ? "Achieved" : nf.format(howMuchLeft);

            double progress = stageAmount.compareTo(BigDecimal.ZERO) == 0 ? 0.0
                    : totals.totalBalance().divide(stageAmount, 6, RoundingMode.HALF_UP).doubleValue();
            if (progress > 1.0) progress = 1.0;

            String monthsStr;
            if (achieved) {
                monthsStr = "—";
            } else {
                double monthsNeeded = estimateMonthsToTarget(
                        totals.investBalance().doubleValue(),
                        totals.savingsBalance().doubleValue(),
                        avgMonthlyInvestDeposit,
                        avgMonthlySavingsDeposit,
                        monthlyReturn,
                        stageAmount.doubleValue());
                if (Double.isInfinite(monthsNeeded) || Double.isNaN(monthsNeeded) || monthsNeeded > 1200)
                    monthsStr = "Long term";
                else monthsStr = formatMonths((int) Math.ceil(monthsNeeded));
            }

            String percentLabel = def.percent.stripTrailingZeros().toPlainString() + "%";
            String amountLabel = nf.format(stageAmount);

            result.add(new FireStage(def.name, percentLabel, amountLabel, howMuchLeftStr, monthsStr, progress));
        }
        return result;
    }

    /**
     * Estimates months until investment portfolio + savings portfolio combined reaches the target.
     * Investments grow at monthlyReturn; savings grow linearly (no investment return).
     */
    private double estimateMonthsToTarget(double investCurrent, double savingsCurrent,
                                           double monthlyInvest, double monthlySavings,
                                           double monthlyReturn, double target) {
        double combined = investCurrent + savingsCurrent;
        if (combined >= target) return 0.0;

        double low = 0.0;
        double high = 1200.0;

        for (int iter = 0; iter < 80; iter++) {
            double mid = (low + high) / 2.0;
            double investFV = futureValue(investCurrent, monthlyInvest, monthlyReturn, mid);
            double savingsFV = savingsCurrent + monthlySavings * mid; // linear growth
            if (investFV + savingsFV >= target) high = mid;
            else low = mid;
        }

        double investFV = futureValue(investCurrent, monthlyInvest, monthlyReturn, high);
        double savingsFV = savingsCurrent + monthlySavings * high;
        if (investFV + savingsFV < target - 0.5) return Double.POSITIVE_INFINITY;
        return high;
    }

    private double futureValue(double current, double monthly, double monthlyReturn, double months) {
        double factor = Math.pow(1.0 + monthlyReturn, months);
        return current * factor + (Math.abs(monthlyReturn) < 1e-12 ? monthly * months : monthly * ((factor - 1.0) / monthlyReturn));
    }

    private String formatMonths(int months) {
        if (months <= 0) return "0 mos";
        int years = months / 12;
        int remMonths = months % 12;
        if (years > 0 && remMonths > 0) return String.format("%d yr %d mos", years, remMonths);
        else if (years > 0) return String.format("%d yr", years);
        else return String.format("%d mos", remMonths);
    }

    private Component createProgressCard(PortfolioTotals totals, double avgMonthlyInvestDeposit,
                                          double avgMonthlySavingsDeposit, double monthlyReturn) {
        Div card = new Div();
        card.addClassName("fire-card");

        H3 title = new H3("Goal progress and variance");
        title.addClassName("card-title");

        Span sliderLabel = new Span("How many years do you want to invest?");
        sliderLabel.addClassName("section-label");

        NumberField yearsSlider = new NumberField();
        yearsSlider.setMin(0.0);
        yearsSlider.setMax(30.0);
        yearsSlider.setValue(5.0);
        yearsSlider.setStep(1);
        yearsSlider.setWidthFull();
        yearsSlider.setStepButtonsVisible(true);

        // "How much to save in total" defaults to investment deposits + savings deposits
        double defaultTotalSavings = Math.ceil(avgMonthlyInvestDeposit + avgMonthlySavingsDeposit);

        Span howMuchYouCanSaveInTotalLabel = new Span("How much to save in total (investments + savings)?");
        howMuchYouCanSaveInTotalLabel.addClassName("section-label");
        NumberField howMuchYouCanSaveInTotal = new NumberField();
        howMuchYouCanSaveInTotal.setMin(avgMonthlyInvestDeposit);
        howMuchYouCanSaveInTotal.setMax(1000000.0);
        howMuchYouCanSaveInTotal.setValue(defaultTotalSavings);
        howMuchYouCanSaveInTotal.setStep(1000);
        howMuchYouCanSaveInTotal.setWidthFull();
        howMuchYouCanSaveInTotal.setStepButtonsVisible(true);

        HorizontalLayout metrics = new HorizontalLayout();
        metrics.addClassName("metrics-row");
        metrics.setWidthFull();
        metrics.setSpacing(true);

        NumberFormat nf = NumberFormat.getCurrencyInstance(new Locale("pl", "PL"));
        nf.setMaximumFractionDigits(0);

        BigDecimal monthlyReturnBd = BigDecimal.valueOf(monthlyReturn * 100).setScale(2, RoundingMode.HALF_UP);

        double nextYearInvestFV = futureValue(totals.investBalance().doubleValue(), avgMonthlyInvestDeposit, monthlyReturn, 12);
        double nextYearSavingsFV = totals.savingsBalance().doubleValue() + avgMonthlySavingsDeposit * 12;
        double nextYearCombined = nextYearInvestFV + nextYearSavingsFV;

        double nextYearCombinedPlus20 = futureValue(totals.investBalance().doubleValue(), avgMonthlyInvestDeposit * 1.2, monthlyReturn, 12)
                + totals.savingsBalance().doubleValue() + avgMonthlySavingsDeposit * 1.2 * 12;

        metrics.add(
                createMetric("Investment Balance", nf.format(totals.investBalance())),
                createMetric("Savings Balance", nf.format(totals.savingsBalance())),
                createMetric("Net Worth", nf.format(totals.totalBalance())),
                createMetric("Prognosed next year net worth", nf.format(nextYearCombined)),
                createMetric("Prognosed next year (20% higher deposit)", nf.format(nextYearCombinedPlus20)),
                createMetric("Avg monthly investment deposit", nf.format(avgMonthlyInvestDeposit)),
                createMetric("Avg monthly savings deposit", nf.format(avgMonthlySavingsDeposit)),
                createMetric("Monthly investment return", monthlyReturnBd + "%")
        );

        ChartData result = getChartData(totals, avgMonthlyInvestDeposit, avgMonthlySavingsDeposit, monthlyReturn,
                yearsSlider.getValue().intValue(), howMuchYouCanSaveInTotal.getValue());
        FireProjectionChart chart = createFireChart(result, avgMonthlyInvestDeposit, howMuchYouCanSaveInTotal.getValue());

        Div bottomInfo = new Div();
        bottomInfo.addClassName("bottom-info");
        bottomInfo.setText("Target: " + nf.format(fireTarget) + ".");

        howMuchYouCanSaveInTotal.addValueChangeListener(ev -> {
            chartInputDataChanged(totals, avgMonthlyInvestDeposit, avgMonthlySavingsDeposit, monthlyReturn,
                    yearsSlider.getValue(), ev.getValue(),
                    card, title, sliderLabel, yearsSlider, metrics, bottomInfo,
                    howMuchYouCanSaveInTotalLabel, howMuchYouCanSaveInTotal);
        });

        yearsSlider.addValueChangeListener(ev -> {
            chartInputDataChanged(totals, avgMonthlyInvestDeposit, avgMonthlySavingsDeposit, monthlyReturn,
                    ev.getValue(), howMuchYouCanSaveInTotal.getValue(),
                    card, title, sliderLabel, yearsSlider, metrics, bottomInfo,
                    howMuchYouCanSaveInTotalLabel, howMuchYouCanSaveInTotal);
        });

        card.add(title, chart, new HorizontalLayout(
                new VerticalLayout(sliderLabel, yearsSlider),
                new VerticalLayout(howMuchYouCanSaveInTotalLabel, howMuchYouCanSaveInTotal)
        ), metrics, bottomInfo);
        return card;
    }

    private void chartInputDataChanged(PortfolioTotals totals, double avgMonthlyInvestDeposit,
                                        double avgMonthlySavingsDeposit, double monthlyReturn,
                                        Double years, Double totalSavings,
                                        Div card, H3 title, Span sliderLabel, NumberField yearsSlider,
                                        HorizontalLayout metrics, Div bottomInfo,
                                        Span howMuchYouCanSaveInTotalLabel, NumberField howMuchYouCanSaveInTotal) {
        synchronized (this) {
            ChartData charData = getChartData(totals, avgMonthlyInvestDeposit, avgMonthlySavingsDeposit,
                    monthlyReturn, years.intValue(), totalSavings);
            card.removeAll();
            card.add(title,
                    createFireChart(charData, avgMonthlyInvestDeposit, totalSavings),
                    new HorizontalLayout(
                            new VerticalLayout(sliderLabel, yearsSlider),
                            new VerticalLayout(howMuchYouCanSaveInTotalLabel, howMuchYouCanSaveInTotal)
                    ), metrics, bottomInfo);
        }
    }

    /**
     * Generates projection data with separate investment and savings tracks.
     * Total savings per month is split: investments get avgMonthlyInvestDeposit (or scaled variant),
     * savings get the remainder up to totalMonthlyBudget.
     */
    private ChartData getChartData(PortfolioTotals totals, double avgMonthlyInvestDeposit,
                                    double avgMonthlySavingsDeposit, double monthlyReturn,
                                    int yearsToProject, double totalMonthlyBudget) {

        double investCurrent = totals.investBalance().doubleValue();
        double savingsCurrent = totals.savingsBalance().doubleValue();

        List<Integer> years = new ArrayList<>();
        List<Double> baseline = new ArrayList<>();
        List<Double> plus20 = new ArrayList<>();
        List<Double> minus20 = new ArrayList<>();
        List<Double> onlyDeposits = new ArrayList<>();
        List<Double> savingsLine = new ArrayList<>();

        double monthlyInvest80 = Math.min(avgMonthlyInvestDeposit * 0.80, totalMonthlyBudget);
        double monthlyInvest100 = Math.min(avgMonthlyInvestDeposit, totalMonthlyBudget);
        double monthlyInvest120 = Math.min(avgMonthlyInvestDeposit * 1.20, totalMonthlyBudget);

        for (int y = 0; y <= yearsToProject; y++) {
            int months = y * 12;
            years.add(y);

            // Baseline: invest avgMonthlyInvestDeposit, save the rest
            double baseSavingsMonthly = Math.max(0, totalMonthlyBudget - monthlyInvest100);
            double baseInvestFV = futureValue(investCurrent, monthlyInvest100, monthlyReturn, months);
            double baseSavingsFV = savingsCurrent + baseSavingsMonthly * months;
            baseline.add(baseInvestFV + baseSavingsFV);

            // +20% investment deposit
            double plusSavingsMonthly = Math.max(0, totalMonthlyBudget - monthlyInvest120);
            double plusInvestFV = futureValue(investCurrent, monthlyInvest120, monthlyReturn, months);
            double plusSavingsFV = savingsCurrent + plusSavingsMonthly * months;
            plus20.add(plusInvestFV + plusSavingsFV);

            // -20% investment deposit
            double minusSavingsMonthly = Math.max(0, totalMonthlyBudget - monthlyInvest80);
            double minusInvestFV = futureValue(investCurrent, monthlyInvest80, monthlyReturn, months);
            double minusSavingsFV = savingsCurrent + minusSavingsMonthly * months;
            minus20.add(minusInvestFV + minusSavingsFV);

            // Only deposits (invest nothing, all goes to savings)
            double onlyDepFV = savingsCurrent + investCurrent + totalMonthlyBudget * months;
            onlyDeposits.add(onlyDepFV);

            // Pure savings track (just savings accounts, no investments)
            savingsLine.add(savingsCurrent + avgMonthlySavingsDeposit * months);
        }

        return new ChartData(investCurrent + savingsCurrent, years, baseline, plus20, minus20, onlyDeposits, savingsLine);
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

    private FireProjectionChart createFireChart(ChartData data, double avgInvestmentForAMonth, double totalSavingForAMonth) {
        FireProjectionChart chart = new FireProjectionChart(
                data.years(),
                data.baseline(),
                data.plus20(),
                data.minus20(),
                data.onlyDeposits(),
                avgInvestmentForAMonth,
                totalSavingForAMonth
        );
        chart.setWidth("100%");
        chart.setHeight("400px");
        return chart;
    }

    private record ChartData(
            double currentBalance,
            List<Integer> years,
            List<Double> baseline,
            List<Double> plus20,
            List<Double> minus20,
            List<Double> onlyDeposits,
            List<Double> savingsLine
    ) {}

    private static class StageDef {
        final String name;
        final BigDecimal percent;

        StageDef(String name, BigDecimal percent) {
            this.name = name;
            this.percent = percent;
        }
    }
}
