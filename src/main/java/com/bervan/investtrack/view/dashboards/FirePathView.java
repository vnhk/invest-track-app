package com.bervan.investtrack.view.dashboards;

import com.bervan.investtrack.model.Wallet;
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

    private static double getMonthlyReturn(long monthsBetween, BigDecimal combinedTotalDeposits, BigDecimal combinedCurrentBalance) {
        if (combinedTotalDeposits.compareTo(BigDecimal.ZERO) <= 0 || combinedCurrentBalance.compareTo(BigDecimal.ZERO) <= 0 || monthsBetween <= 0) {
            return 0.0;
        }

        double years = monthsBetween / 12.0;

        // Compound Annual Growth Rate (CAGR)
        double totalMultiplier = combinedCurrentBalance.divide(combinedTotalDeposits, 18, RoundingMode.HALF_UP).doubleValue();
        double annualReturn = Math.pow(totalMultiplier, 1.0 / years) - 1.0;

        // Adjust for inflation to get real annual return
        double realAnnualReturn = (1 + annualReturn) / (1 + INFLATION.doubleValue()) - 1;

        // Convert real annual return to monthly return
        return Math.pow(1 + realAnnualReturn, 1.0 / 12.0) - 1;
    }

    private Component createMainContent() {
        // combined balance and total deposits
        BigDecimal combinedCurrentBalance = BigDecimal.ZERO;
        BigDecimal combinedTotalDeposits = BigDecimal.ZERO;

        // detect first and last date for timeline
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        LocalDate firstDate = LocalDate.MAX;
        LocalDate lastDate = LocalDate.MIN;

        for (Wallet wallet : wallets) {
            UUID wId = wallet.getId();
            List<BigDecimal> walletBalances = balances.getOrDefault(wId, Collections.emptyList());
            List<BigDecimal> sumOfWalletDeposits = sumOfDeposits.getOrDefault(wId, Collections.emptyList());
            List<String> walletDates = dates.getOrDefault(wId, Collections.emptyList());

            // Skip wallets with no data
            if (walletBalances.isEmpty() || sumOfWalletDeposits.isEmpty() || walletDates.isEmpty()) {
                continue;
            }

            combinedCurrentBalance = combinedCurrentBalance.add(convert(walletBalances.get(walletBalances.size() - 1), CurrencyConverter.Currency.of(wallet.getCurrency()), CurrencyConverter.Currency.PLN));
            combinedTotalDeposits = combinedTotalDeposits.add(convert(sumOfWalletDeposits.get(sumOfWalletDeposits.size() - 1), CurrencyConverter.Currency.of(wallet.getCurrency()), CurrencyConverter.Currency.PLN));

            for (String dStr : walletDates) {
                LocalDate d = LocalDate.parse(dStr, formatter);
                if (d.isBefore(firstDate)) firstDate = d;
                if (d.isAfter(lastDate)) lastDate = d;
            }
        }

        // Handle case where no valid data was found
        if (firstDate.equals(LocalDate.MAX) || lastDate.equals(LocalDate.MIN)) {
            firstDate = LocalDate.now().minusYears(1);
            lastDate = LocalDate.now();
        }

        long monthsBetween = ChronoUnit.MONTHS.between(firstDate.withDayOfMonth(1), lastDate.withDayOfMonth(1)) + 1;
        if (monthsBetween <= 0) monthsBetween = 1;
        double avgMonthlyDeposit = combinedTotalDeposits.compareTo(BigDecimal.ZERO) > 0 ?
                combinedTotalDeposits.divide(BigDecimal.valueOf(monthsBetween), 18, RoundingMode.HALF_UP).doubleValue() : 0.0;
        double monthlyReturn = getMonthlyReturn(monthsBetween, combinedTotalDeposits, combinedCurrentBalance);

        VerticalLayout content = new VerticalLayout();
        content.addClassName("fire-content");
        content.setSizeFull();
        content.setPadding(true);
        content.setSpacing(false);

        Div stagesCard = createStagesCard(combinedCurrentBalance, avgMonthlyDeposit, monthlyReturn);

        HorizontalLayout bottomRow = new HorizontalLayout();
        bottomRow.addClassName("fire-bottom-row");
        bottomRow.setWidthFull();

        Component progressCard = createProgressCard(combinedCurrentBalance, avgMonthlyDeposit, monthlyReturn);

        bottomRow.add(progressCard);
        bottomRow.setFlexGrow(1, progressCard);

        content.add(stagesCard, bottomRow);
        content.setFlexGrow(1, stagesCard);
        return content;
    }

    private Div createStagesCard(BigDecimal combinedCurrentBalance, double avgMonthlyDeposit, double monthlyReturn) {
        Div card = new Div();
        card.addClassName("fire-card");
        card.setWidth("95%");
        card.setHeightFull();

        H3 title = new H3("FIRE Stages");
        title.addClassName("card-title");

        Grid<FireStage> grid = createStagesGrid(combinedCurrentBalance, avgMonthlyDeposit, monthlyReturn);

        Span note = new Span(
                "* Values are projections using historical deposits and an estimated monthly return after inflation (inflation = " + INFLATION.multiply(BigDecimal.valueOf(100L)).stripTrailingZeros().toPlainString() + "%).");
        note.addClassName("card-note");

        card.add(title, grid, note);
        return card;
    }

    private Grid<FireStage> createStagesGrid(BigDecimal combinedCurrentBalance, double avgMonthlyDeposit, double monthlyReturn) {
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

        grid.setItems(computeStages(combinedCurrentBalance, avgMonthlyDeposit, monthlyReturn));
        return grid;
    }

    private List<FireStage> computeStages(BigDecimal combinedCurrentBalance, double avgMonthlyDeposit, double monthlyReturn) {
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

        List<FireStage> result = new ArrayList<>();
        for (StageDef def : stageDefs) {
            BigDecimal pct = def.percent.divide(BigDecimal.valueOf(100), 18, RoundingMode.HALF_UP);
            BigDecimal stageAmount = fireTarget.multiply(pct).setScale(0, RoundingMode.HALF_UP);
            BigDecimal howMuchLeft = stageAmount.subtract(combinedCurrentBalance);
            boolean achieved = howMuchLeft.compareTo(BigDecimal.ZERO) <= 0;
            String howMuchLeftStr = achieved ? "Achieved" : nf.format(howMuchLeft);

            double progress = stageAmount.compareTo(BigDecimal.ZERO) == 0 ? 0.0 : combinedCurrentBalance.divide(stageAmount, 6, RoundingMode.HALF_UP).doubleValue();
            if (progress > 1.0) progress = 1.0;

            String monthsStr;
            if (achieved) {
                monthsStr = "—";
            } else {
                double monthsNeeded = estimateMonthsToTarget(combinedCurrentBalance.doubleValue(), avgMonthlyDeposit, monthlyReturn, stageAmount.doubleValue());
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

    private double estimateMonthsToTarget(double current, double monthly, double monthlyReturn, double target) {
        if (current >= target) return 0.0;
        double left = target - current;
        if (Math.abs(monthlyReturn) < 1e-12) {
            if (monthly <= 0.0) return Double.POSITIVE_INFINITY;
            return Math.max(0.0, left / monthly);
        }
        double low = 0.0;
        double high = 1200.0;
        for (int iter = 0; iter < 80; iter++) {
            double mid = (low + high) / 2.0;
            double val = futureValue(current, monthly, monthlyReturn, mid);
            if (val >= target) high = mid;
            else low = mid;
        }
        if (futureValue(current, monthly, monthlyReturn, high) < target - 0.5) return Double.POSITIVE_INFINITY;
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

    private Component createProgressCard(BigDecimal combinedCurrentBalance, double avgMonthlyDeposit, double monthlyReturn) {
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

        Span howMuchYouCanSaveInTotalLabel = new Span("How much to save in total (not only in investments)?");
        howMuchYouCanSaveInTotalLabel.addClassName("section-label");
        NumberField howMuchYouCanSaveInTotal = new NumberField();
        howMuchYouCanSaveInTotal.setMin(avgMonthlyDeposit);
        howMuchYouCanSaveInTotal.setMax(1000000.0);
        howMuchYouCanSaveInTotal.setValue(Math.ceil(avgMonthlyDeposit));
        howMuchYouCanSaveInTotal.setStep(1000);
        howMuchYouCanSaveInTotal.setWidthFull();
        howMuchYouCanSaveInTotal.setStepButtonsVisible(true);

        HorizontalLayout metrics = new HorizontalLayout();
        metrics.addClassName("metrics-row");
        metrics.setWidthFull();
        metrics.setSpacing(true);

        ChartData result = getChartData(combinedCurrentBalance, avgMonthlyDeposit, monthlyReturn, yearsSlider.getValue().intValue(), howMuchYouCanSaveInTotal.getValue());
        FireProjectionChart chart = createFireChart(result.years(), result.baseline(), result.plus20(), result.minus20(), result.onlyDeposits, avgMonthlyDeposit, howMuchYouCanSaveInTotal.getValue());

        NumberFormat nf = NumberFormat.getCurrencyInstance(new Locale("pl", "PL"));
        nf.setMaximumFractionDigits(0);

        double nextYearPrognosedCapital = futureValue(combinedCurrentBalance.doubleValue(), avgMonthlyDeposit, monthlyReturn, 12);
        double nextYearPrognosedCapitalIf20PercentMore = futureValue(combinedCurrentBalance.doubleValue(), avgMonthlyDeposit * 1.2, monthlyReturn, 12);

        BigDecimal monthlyReturnBd = BigDecimal.valueOf(monthlyReturn * 100)
                .setScale(2, RoundingMode.HALF_UP);

        metrics.add(
                createMetric("Current capital", nf.format(combinedCurrentBalance)),
                createMetric("Prognosed next year capital", nf.format(nextYearPrognosedCapital)),
                createMetric("Prognosed next year capital with 20% higher deposit", nf.format(nextYearPrognosedCapitalIf20PercentMore)),
                createMetric("Average Monthly deposit", nf.format(avgMonthlyDeposit)),
                createMetric("Monthly return", monthlyReturnBd + "%")
        );

        Div bottomInfo = new Div();
        bottomInfo.addClassName("bottom-info");
        bottomInfo.setText("Target: " + nf.format(fireTarget) + ".");

        howMuchYouCanSaveInTotal.addValueChangeListener(howMuchYouCanSaveInTotalChanged -> {
            chartInputDataChanged(combinedCurrentBalance, avgMonthlyDeposit, monthlyReturn, yearsSlider.getValue(), howMuchYouCanSaveInTotalChanged.getValue(),
                    card, title, sliderLabel, yearsSlider, metrics, bottomInfo, howMuchYouCanSaveInTotalLabel, howMuchYouCanSaveInTotal);
        });

        yearsSlider.addValueChangeListener(yearsChanged -> {
            chartInputDataChanged(combinedCurrentBalance, avgMonthlyDeposit, monthlyReturn, yearsChanged.getValue(), howMuchYouCanSaveInTotal.getValue(), card, title, sliderLabel, yearsSlider, metrics, bottomInfo, howMuchYouCanSaveInTotalLabel, howMuchYouCanSaveInTotal);
        });

        card.add(title, chart, new HorizontalLayout(
                new VerticalLayout(sliderLabel, yearsSlider),
                new VerticalLayout(howMuchYouCanSaveInTotalLabel, howMuchYouCanSaveInTotal)
        ), metrics, bottomInfo);
        return card;
    }

    private void chartInputDataChanged(BigDecimal combinedCurrentBalance, double avgMonthlyDeposit, double monthlyReturn, Double yearsChanged, Double howMuchYouSaveAMonth, Div card, H3 title, Span sliderLabel, NumberField yearsSlider, HorizontalLayout metrics, Div bottomInfo, Span howMuchYouCanSaveInTotalLabel, NumberField howMuchYouCanSaveInTotal) {
        synchronized (this) {
            ChartData charData = getChartData(combinedCurrentBalance, avgMonthlyDeposit, monthlyReturn, yearsChanged.intValue(), howMuchYouSaveAMonth);
            card.removeAll();
            card.add(title, createFireChart(charData.years(), charData.baseline(), charData.plus20(), charData.minus20(), charData.onlyDeposits, avgMonthlyDeposit, howMuchYouSaveAMonth),
                    new HorizontalLayout(
                            new VerticalLayout(sliderLabel, yearsSlider),
                            new VerticalLayout(howMuchYouCanSaveInTotalLabel, howMuchYouCanSaveInTotal)
                    ), metrics, bottomInfo);
        }
    }

    private ChartData getChartData(
            BigDecimal combinedCurrentBalance,
            double avgMonthlyDeposit,
            double monthlyReturn,
            Integer yearsChanged,
            Double howMuchYouSaveAMonth
    ) {
        double currentBalance = combinedCurrentBalance.doubleValue();

        List<Integer> years = new ArrayList<>();
        List<Double> baseline = new ArrayList<>();
        List<Double> plus20 = new ArrayList<>();
        List<Double> minus20 = new ArrayList<>();
        List<Double> onlyDeposits = new ArrayList<>();

        // Calculate monthly strategies
        double monthly80 = avgMonthlyDeposit * 0.80;
        double monthly120 = avgMonthlyDeposit * 1.20;

        if (monthly120 > howMuchYouSaveAMonth) {
            monthly120 = howMuchYouSaveAMonth;
        }

        // Ensure strategies never violate total savings per month
        monthly80 = Math.min(monthly80, howMuchYouSaveAMonth);
        avgMonthlyDeposit = Math.min(avgMonthlyDeposit, howMuchYouSaveAMonth);

        // Each scenario: invest X, save (howMuchYouSaveAMonth - X)
        for (int y = 0; y <= yearsChanged; y++) {
            int months = y * 12;
            years.add(y);

            // Baseline (100% avgMonthlyDeposit)
            double baselineInvest = futureValue(currentBalance, avgMonthlyDeposit, monthlyReturn, months);
            double baselineSaved = futureValue(0, howMuchYouSaveAMonth - avgMonthlyDeposit, 0, months);
            baseline.add(baselineInvest + baselineSaved);

            // +20%
            double plusInvest = futureValue(currentBalance, monthly120, monthlyReturn, months);
            double plusSaved = futureValue(0, howMuchYouSaveAMonth - monthly120, 0, months);
            plus20.add(plusInvest + plusSaved);

            // -20%
            double minusInvest = futureValue(currentBalance, monthly80, monthlyReturn, months);
            double minusSaved = futureValue(0, howMuchYouSaveAMonth - monthly80, 0, months);
            minus20.add(minusInvest + minusSaved);

            // Only deposits (invest 0, save 100% of the monthly amount)
            double onlyDep = futureValue(currentBalance, 0, monthlyReturn, 0) // current balance stays unchanged
                    + futureValue(0, howMuchYouSaveAMonth, 0, months);
            onlyDeposits.add(onlyDep);
        }

        return new ChartData(currentBalance, years, baseline, plus20, minus20, onlyDeposits);
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

    private FireProjectionChart createFireChart(
            List<Integer> years, List<Double> baseline, List<Double> plus20, List<Double> minus20,
            List<Double> onlyDeposits, double avgInvestmentForAMonth, double totalSavingForAMonth) {

        FireProjectionChart chart = new FireProjectionChart(
                years,
                baseline,
                plus20,
                minus20,
                onlyDeposits,
                avgInvestmentForAMonth,
                totalSavingForAMonth
        );

        chart.setWidth("100%");
        chart.setHeight("400px");

        return chart;
    }

    private record ChartData(double currentBalance, List<Integer> years, List<Double> baseline, List<Double> plus20,
                             List<Double> minus20, List<Double> onlyDeposits) {
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