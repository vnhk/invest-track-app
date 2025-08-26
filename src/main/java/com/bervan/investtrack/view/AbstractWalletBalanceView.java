package com.bervan.investtrack.view;

import com.bervan.common.AbstractPageView;
import com.bervan.investtrack.InvestTrackPageLayout;
import com.vaadin.flow.component.dependency.CssImport;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@CssImport("./invest-track.css")
public abstract class AbstractWalletBalanceView extends AbstractPageView {
    public static final String ROUTE_NAME = "/invest-track-app/wallet-balance/";

    public AbstractWalletBalanceView() {
        add(new InvestTrackPageLayout(ROUTE_NAME));
        List<String> dates = List.of(
                "01/2025", "02/2025", "03/2025", "04/2025", "05/2025", "06/2025",
                "07/2025", "08/2025", "09/2025", "10/2025", "11/2025", "12/2025"
        );


        List<BigDecimal> sumDeposits = List.of(BigDecimal.valueOf(1000), BigDecimal.valueOf(2000), BigDecimal.valueOf(3000), BigDecimal.valueOf(4000), BigDecimal.valueOf(5000)
                , BigDecimal.valueOf(6000), BigDecimal.valueOf(7000), BigDecimal.valueOf(8000), BigDecimal.valueOf(9000), BigDecimal.valueOf(10000), BigDecimal.valueOf(11000), BigDecimal.valueOf(12000));
        ;
        List<BigDecimal> balances = List.of(BigDecimal.valueOf(1000), BigDecimal.valueOf(2100), BigDecimal.valueOf(1900), BigDecimal.valueOf(4050), BigDecimal.valueOf(5050)
                , BigDecimal.valueOf(6050), BigDecimal.valueOf(7050), BigDecimal.valueOf(10000), BigDecimal.valueOf(9330), BigDecimal.valueOf(10300), BigDecimal.valueOf(11500), BigDecimal.valueOf(14000));


        add(new WalletBalanceSumOfDepositsCharts(dates, balances, sumDeposits));
    }
}