package com.bervan.investtrack.view;

import com.bervan.common.AbstractPageView;
import com.bervan.investtrack.InvestTrackPageLayout;
import com.bervan.investtrack.model.Wallet;
import com.bervan.investtrack.service.WalletService;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

@Slf4j
@CssImport("./invest-track.css")
public abstract class AbstractWalletView extends AbstractPageView implements HasUrlParameter<String> {
    public static final String ROUTE_NAME = "/invest-track-app/wallets/";
    private final WalletService service;
    private Wallet wallet;

    public AbstractWalletView(WalletService service) {
        super();
        this.service = service;
    }

    @Override
    public void setParameter(BeforeEvent event, String s) {
        String walletName = event.getRouteParameters().get("___url_parameter").orElse(UUID.randomUUID().toString());
        init(walletName);
    }

    private void init(String walletName) {
        try {
            add(new InvestTrackPageLayout(ROUTE_NAME, walletName));
            wallet = service.getWalletByName(walletName).iterator().next();
        } catch (Exception e) {
            log.error("Unable to load wallet: {}", walletName, e);
            showErrorNotification("Unable to load wallet: " + walletName + ".");
        }
    }
}