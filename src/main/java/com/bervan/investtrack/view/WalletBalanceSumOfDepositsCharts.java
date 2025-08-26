package com.bervan.investtrack.view;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasSize;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.JsModule;
import elemental.json.impl.JreJsonArray;
import elemental.json.impl.JreJsonFactory;
import elemental.json.impl.JreJsonObject;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@JsModule("./investing-chart-component.js")
@Tag("canvas")
public class WalletBalanceSumOfDepositsCharts extends Component implements HasSize {
    public WalletBalanceSumOfDepositsCharts(List<String> dates, List<BigDecimal> walletBalance, List<BigDecimal> sumOfDeposits) {
        renderWalletBalance(dates, walletBalance, sumOfDeposits);
    }

    // Converts a List into a JreJsonObject with a single "data" key pointing to a JreJsonArray
    private static JreJsonObject getJreJsonObject(List<?> values) {
        JreJsonObject jreJsonObject = new JreJsonObject(new JreJsonFactory());
        JreJsonArray jreJsonArray = new JreJsonArray(new JreJsonFactory());

        // Fill the array with stringified values from the input list
        for (int i = 0; i < values.size(); i++) {
            jreJsonArray.set(i, values.get(i).toString());
        }
        jreJsonObject.set("data", jreJsonArray);
        return jreJsonObject;
    }

    private void renderWalletBalance(List<String> dates, List<BigDecimal> walletBalance, List<BigDecimal> sumOfDeposits) {
        setId("walletBalanceChart_" + UUID.randomUUID());

        if (sumOfDeposits.size() != walletBalance.size()) {
            throw new IllegalArgumentException("Wallet Balances and Sum of Deposits must have the same size");
        }

        JreJsonObject datesJson = getJreJsonObject(dates);
        JreJsonObject walletBalanceJson = getJreJsonObject(walletBalance);
        JreJsonObject sumOfDepositsJson = getJreJsonObject(sumOfDeposits);

        UI.getCurrent().getPage().executeJs(
                "window.renderWalletBalanceDepositAndWallet($0, $1, $2, $3)",
                getElement(),
                datesJson.get("data"),
                walletBalanceJson,
                sumOfDepositsJson
        );
    }
}