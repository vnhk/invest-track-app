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
    private static final BigDecimal GLOBAL_TARGET = BigDecimal.valueOf(1_500_000L); // PLN
    private static final BigDecimal INFLATION = BigDecimal.valueOf(0.038); // 3.8% last 10 years in PL
    private final CurrencyConverter currencyConverter;
    private final List<Wallet> wallets;
    private final Map<UUID, List<BigDecimal>> balances;
    private final Map<UUID, List<BigDecimal>> sumOfDeposits;
    private final Map<UUID, List<String>> dates;

    public FirePathView(CurrencyConverter currencyConverter, List<Wallet> wallets,
                        Map<UUID, List<BigDecimal>> balances, Map<UUID, List<BigDecimal>> sumOfDeposits,
                        Map<UUID, List<String>> dates) {
        this.currencyConverter = currencyConverter;
        this.wallets = wallets;
        this.balances = balances;
        this.sumOfDeposits = sumOfDeposits;
        this.dates = dates;

        setSizeFull();
        setPadding(false);
        setSpacing(false);
        addClassName("fire-view");

        add(createMainContent());
    }

    private static double getMonthlyReturn(long monthsBetween, BigDecimal combinedTotalDeposits, BigDecimal combinedCurrentBalance) {
        double years = monthsBetween / 12.0;

        // annualized return
        double annualReturn;
        if (combinedTotalDeposits.compareTo(BigDecimal.ZERO) <= 0 || combinedCurrentBalance.compareTo(BigDecimal.ZERO) <= 0) {
            annualReturn = 0.0;
        } else {
            double totalMultiplier = combinedCurrentBalance.divide(combinedTotalDeposits, 18, RoundingMode.HALF_UP).doubleValue();
            annualReturn = Math.pow(totalMultiplier, 1.0 / years) - 1.0;
        }

        return (annualReturn - INFLATION.doubleValue()) / 12.0;
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

            combinedCurrentBalance = combinedCurrentBalance.add(convert(walletBalances.get(walletBalances.size() - 1), CurrencyConverter.Currency.of(wallet.getCurrency()), CurrencyConverter.Currency.PLN));
            combinedTotalDeposits = combinedTotalDeposits.add(convert(sumOfWalletDeposits.get(sumOfWalletDeposits.size() - 1), CurrencyConverter.Currency.of(wallet.getCurrency()), CurrencyConverter.Currency.PLN));

            for (String dStr : walletDates) {
                LocalDate d = LocalDate.parse(dStr, formatter);
                if (d.isBefore(firstDate)) firstDate = d;
                if (d.isAfter(lastDate)) lastDate = d;
            }
        }

        long monthsBetween = ChronoUnit.MONTHS.between(firstDate.withDayOfMonth(1), lastDate.withDayOfMonth(1)) + 1;
        double avgMonthlyDeposit = monthsBetween > 0 ? combinedTotalDeposits.divide(BigDecimal.valueOf(monthsBetween), 18, RoundingMode.HALF_UP).doubleValue() : 0.0;
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
            BigDecimal stageAmount = GLOBAL_TARGET.multiply(pct).setScale(0, RoundingMode.HALF_UP);
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

        HorizontalLayout metrics = new HorizontalLayout();
        metrics.addClassName("metrics-row");
        metrics.setWidthFull();
        metrics.setSpacing(true);

        ChartData result = getChartData(combinedCurrentBalance, avgMonthlyDeposit, monthlyReturn, yearsSlider.getValue().intValue());
        FireProjectionChart chart = createFireChart(result.years(), result.baseline(), result.plus20(), result.minus20(), result.currentBalance());

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
        bottomInfo.setText("Target: " + nf.format(GLOBAL_TARGET) + ".");

        yearsSlider.addValueChangeListener(yearsChanged -> {
            synchronized (this) {
                ChartData charData = getChartData(combinedCurrentBalance, avgMonthlyDeposit, monthlyReturn, yearsChanged.getValue().intValue());
                card.removeAll();
                card.add(title, createFireChart(charData.years(), charData.baseline(), charData.plus20(), charData.minus20(), charData.currentBalance()), sliderLabel, yearsSlider, metrics, bottomInfo);
            }
        });

        card.add(title, chart, sliderLabel, yearsSlider, metrics, bottomInfo);
        return card;
    }

    private ChartData getChartData(BigDecimal combinedCurrentBalance, double avgMonthlyDeposit, double monthlyReturn, Integer yearsChanged) {
        double currentBalance = combinedCurrentBalance.doubleValue();
        List<Integer> years = new ArrayList<>();
        List<Double> baseline = new ArrayList<>();
        List<Double> plus20 = new ArrayList<>();
        List<Double> minus20 = new ArrayList<>();
        for (int y = 0; y <= yearsChanged; y++) {
            years.add(y);

            baseline.add(futureValue(currentBalance, avgMonthlyDeposit, monthlyReturn, y * 12));
            plus20.add(futureValue(currentBalance, avgMonthlyDeposit * 1.20, monthlyReturn, y * 12));
            minus20.add(futureValue(currentBalance, avgMonthlyDeposit * 0.80, monthlyReturn, y * 12));
        }
        ChartData result = new ChartData(currentBalance, years, baseline, plus20, minus20);
        return result;
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
            List<Integer> years, List<Double> baseline, List<Double> plus20, List<Double> minus20, double currentBalance) {

        FireProjectionChart chart = new FireProjectionChart(
                years,
                baseline,
                plus20,
                minus20,
                currentBalance
        );

        chart.setWidth("100%");
        chart.setHeight("400px");

        return chart;
    }

    private record ChartData(double currentBalance, List<Integer> years, List<Double> baseline, List<Double> plus20,
                             List<Double> minus20) {
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