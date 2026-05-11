# invest-track-app

Investment portfolio tracking with budget management, stock price alerts, and FIRE (Financial Independence, Retire Early) projections.

## Features

- **Portfolio tracking**: Multiple wallets in PLN/EUR/USD with monthly snapshots
- **Advanced metrics**: CAGR, Real CAGR, TWR (Time-Weighted Return), XIRR (Newton-Raphson)
- **Budget management**: Income/expense tracking by category with recurring entries; auto-copied monthly
- **FIRE projections**: 12-stage progression to a configurable goal (default 1.5M PLN), with ±20% and conservative variants
- **Stock alerts**: Price monitoring with email notifications
- **Investment strategies**: 7 short-term recommendation strategies with historical performance comparison
- **Currency conversion**: Automatic daily rate updates (PLN/EUR/USD)
- **Charts**: Balance vs deposits, budget income/expense, asset allocation, monthly returns heatmap

## Key Entities

| Entity | Description |
|--------|-------------|
| `Wallet` | Portfolio container with currency and risk level |
| `WalletSnapshot` | Monthly state: value, deposits, withdrawals, earnings |
| `BudgetEntry` | Income or expense with category, payment method, recurring flag |
| `StockPriceAlert` | Price alert with symbol, threshold, and email list |

## Routes (`/invest-track-app/`)

| Path | Purpose |
|------|---------|
| `dashboard/` | Main unified dashboard (KPIs + charts) |
| `wallets` | Portfolio list |
| `wallet-details/{id}` | Individual wallet detail |
| `recommendations` | Daily stock suggestions |
| `alerts` | Price alerts |
| `recommendations-history` | Past recommendations |
| `import-export-data` | Data import/export |

## Key Formulas

```
CAGR = (EndValue/StartValue)^(1/years) - 1
TWR  = Π(1 + Ri) - 1
XIRR: Newton-Raphson where NPV(rate) = 0
FIRE: FV = current × factor + deposits × ((factor-1)/return)
```

## Build

```bash
mvn clean install -DskipTests
```

Part of the `my-tools` multi-module Maven project. Requires `common` to be built first.
