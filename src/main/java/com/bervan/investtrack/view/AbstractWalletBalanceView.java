package com.bervan.investtrack.view;

import com.bervan.common.AbstractPageView;
import com.bervan.investtrack.InvestTrackPageLayout;
import com.bervan.investtrack.model.Wallet;
import com.bervan.investtrack.model.WalletSnapshot;
import com.bervan.investtrack.service.WalletService;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Slf4j
@CssImport("./invest-track.css")
public abstract class AbstractWalletBalanceView extends AbstractPageView {
    public static final String ROUTE_NAME = "/invest-track-app/wallet-balance/";
    private final WalletService service;

    public AbstractWalletBalanceView(WalletService service) {
        this.service = service;
        try {
            add(new InvestTrackPageLayout(ROUTE_NAME, null));

            Set<Wallet> wallets = service.load(Pageable.ofSize(16));
            if (wallets.size() > 16) {
                showWarningNotification("Too many wallets loaded. Only the first 16 will be displayed.");
            }

            Div gridContainer = getGridContainer();
            wallets.forEach(wallet -> {
                List<String> dates = new ArrayList<>();
                List<BigDecimal> balances = new ArrayList<>();
                List<BigDecimal> deposits = new ArrayList<>();
                for (WalletSnapshot snapshot : wallet.getSnapshots()) {
                    dates.add(snapshot.getSnapshotDate().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")));
                    balances.add(snapshot.getPortfolioValue());
                    deposits.add(snapshot.getMonthlyDeposit());
                }

                List<BigDecimal> sumOfDeposits = new ArrayList<>();
                for (BigDecimal deposit : deposits) {
                    sumOfDeposits.add(deposit.add(sumOfDeposits.isEmpty() ? BigDecimal.ZERO : sumOfDeposits.get(sumOfDeposits.size() - 1)));
                }

                gridContainer.add(createWalletTile(wallet.getName(), dates, balances, deposits));
            });

            add(gridContainer);
        } catch (Exception e) {
            log.error("Failed to load wallets: {}", e.getMessage(), e);
            showErrorNotification("Failed to load wallets!");
        }
    }

    private Div getGridContainer() {
        Div gridContainer = new Div();
        gridContainer.addClassName("wallet-grid-container");
        gridContainer.getStyle()
                .set("display", "grid")
                .set("grid-template-columns", "1fr 1fr")
                .set("grid-template-rows", "1fr 1fr")
                .set("gap", "20px")
                .set("height", "80vh")
                .set("width", "90vw")
                .set("padding", "20px");
        return gridContainer;
    }

    /**
     * Creates a single wallet tile with title, separator and chart
     *
     * @param walletName Name of the wallet
     * @param dates List of dates for the chart
     * @param balances List of balance values
     * @param deposits List of deposit values
     * @return VerticalLayout containing the wallet tile
     */
    private VerticalLayout createWalletTile(String walletName, List<String> dates,
                                            List<BigDecimal> balances, List<BigDecimal> deposits) {
        VerticalLayout tile = new VerticalLayout();
        tile.addClassName("wallet-tile");
        tile.getStyle()
                .set("border", "1px solid #ddd")
                .set("border-radius", "8px")
                .set("padding", "16px")
                .set("background", "white")
                .set("box-shadow", "0 2px 4px rgba(0,0,0,0.1)");

        H3 title = new H3(walletName);
        title.getStyle()
                .set("margin", "0 0 10px 0")
                .set("color", "#333")
                .set("text-align", "center");

        Hr separator = new Hr();
        separator.getStyle()
                .set("margin", "10px 0")
                .set("border", "1px solid #eee");

        WalletBalanceSumOfDepositsCharts chart = new WalletBalanceSumOfDepositsCharts(dates, balances, deposits);
        chart.setWidthFull();
        chart.getStyle().set("flex-grow", "1");

        tile.add(title, separator, chart);
        tile.setSpacing(false);
        tile.setPadding(false);
        tile.setWidthFull();
        tile.setHeightFull();

        return tile;
    }
}