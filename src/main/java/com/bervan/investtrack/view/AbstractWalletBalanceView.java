package com.bervan.investtrack.view;

import com.bervan.common.AbstractPageView;
import com.bervan.investtrack.InvestTrackPageLayout;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@CssImport("./invest-track.css")
public abstract class AbstractWalletBalanceView extends AbstractPageView {
    public static final String ROUTE_NAME = "/invest-track-app/wallet-balance/";

    public AbstractWalletBalanceView() {
        add(new InvestTrackPageLayout(ROUTE_NAME, null));

        List<String> dates = List.of(
                "01/2025", "02/2025", "03/2025", "04/2025", "05/2025", "06/2025",
                "07/2025", "08/2025", "09/2025", "10/2025", "11/2025", "12/2025"
        );

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

        gridContainer.add(
                createWalletTile("PLN", dates, generatePLNData(), generatePLNDeposits()),
                createWalletTile("EUR", dates, generateEURData(), generateEURDeposits()),
                createWalletTile("IKE", dates, generateIKEData(), generateIKEDeposits()),
                createWalletTile("IKZE", dates, generateIKZEData(), generateIKZEDeposits())
        );

        add(gridContainer);
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


    private List<BigDecimal> generatePLNData() {
        return List.of(
                BigDecimal.valueOf(5000), BigDecimal.valueOf(5200), BigDecimal.valueOf(4800),
                BigDecimal.valueOf(5500), BigDecimal.valueOf(5800), BigDecimal.valueOf(6200),
                BigDecimal.valueOf(6500), BigDecimal.valueOf(6800), BigDecimal.valueOf(7200),
                BigDecimal.valueOf(7500), BigDecimal.valueOf(7800), BigDecimal.valueOf(8200)
        );
    }

    private List<BigDecimal> generatePLNDeposits() {
        return List.of(
                BigDecimal.valueOf(5000), BigDecimal.valueOf(5000), BigDecimal.valueOf(5000),
                BigDecimal.valueOf(5500), BigDecimal.valueOf(5500), BigDecimal.valueOf(6000),
                BigDecimal.valueOf(6000), BigDecimal.valueOf(6500), BigDecimal.valueOf(6500),
                BigDecimal.valueOf(7000), BigDecimal.valueOf(7000), BigDecimal.valueOf(7500)
        );
    }

    private List<BigDecimal> generateEURData() {
        return List.of(
                BigDecimal.valueOf(1200), BigDecimal.valueOf(1250), BigDecimal.valueOf(1180),
                BigDecimal.valueOf(1320), BigDecimal.valueOf(1400), BigDecimal.valueOf(1380),
                BigDecimal.valueOf(1450), BigDecimal.valueOf(1520), BigDecimal.valueOf(1600),
                BigDecimal.valueOf(1680), BigDecimal.valueOf(1750), BigDecimal.valueOf(1820)
        );
    }


    private List<BigDecimal> generateEURDeposits() {
        return List.of(
                BigDecimal.valueOf(1200), BigDecimal.valueOf(1200), BigDecimal.valueOf(1200),
                BigDecimal.valueOf(1300), BigDecimal.valueOf(1300), BigDecimal.valueOf(1400),
                BigDecimal.valueOf(1400), BigDecimal.valueOf(1500), BigDecimal.valueOf(1500),
                BigDecimal.valueOf(1600), BigDecimal.valueOf(1600), BigDecimal.valueOf(1700)
        );
    }

    private List<BigDecimal> generateIKEData() {
        return List.of(
                BigDecimal.valueOf(2000), BigDecimal.valueOf(2100), BigDecimal.valueOf(1950),
                BigDecimal.valueOf(2200), BigDecimal.valueOf(2350), BigDecimal.valueOf(2500),
                BigDecimal.valueOf(2600), BigDecimal.valueOf(2750), BigDecimal.valueOf(2900),
                BigDecimal.valueOf(3050), BigDecimal.valueOf(3200), BigDecimal.valueOf(3400)
        );
    }

    private List<BigDecimal> generateIKEDeposits() {
        return List.of(
                BigDecimal.valueOf(2000), BigDecimal.valueOf(2000), BigDecimal.valueOf(2000),
                BigDecimal.valueOf(2200), BigDecimal.valueOf(2200), BigDecimal.valueOf(2400),
                BigDecimal.valueOf(2400), BigDecimal.valueOf(2600), BigDecimal.valueOf(2600),
                BigDecimal.valueOf(2800), BigDecimal.valueOf(2800), BigDecimal.valueOf(3000)
        );
    }

    private List<BigDecimal> generateIKZEData() {
        return List.of(
                BigDecimal.valueOf(1500), BigDecimal.valueOf(1580), BigDecimal.valueOf(1520),
                BigDecimal.valueOf(1650), BigDecimal.valueOf(1720), BigDecimal.valueOf(1800),
                BigDecimal.valueOf(1880), BigDecimal.valueOf(1950), BigDecimal.valueOf(2020),
                BigDecimal.valueOf(2100), BigDecimal.valueOf(2180), BigDecimal.valueOf(2300)
        );
    }

    private List<BigDecimal> generateIKZEDeposits() {
        return List.of(
                BigDecimal.valueOf(1500), BigDecimal.valueOf(1500), BigDecimal.valueOf(1500),
                BigDecimal.valueOf(1600), BigDecimal.valueOf(1600), BigDecimal.valueOf(1700),
                BigDecimal.valueOf(1700), BigDecimal.valueOf(1800), BigDecimal.valueOf(1800),
                BigDecimal.valueOf(1900), BigDecimal.valueOf(1900), BigDecimal.valueOf(2000)
        );
    }
}