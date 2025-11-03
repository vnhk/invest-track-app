import 'https://cdn.jsdelivr.net/npm/chart.js';

window.renderWalletBalanceDepositAndWalletBalance = (canvas, dates, walletBalances, deposit) => {
    let textColor = getComputedStyle(document.documentElement)
        .getPropertyValue('--chart-text-color')
        .trim();

    let charLine1Color = getComputedStyle(document.documentElement)
        .getPropertyValue('--chart-line1-color')
        .trim();

    let charLine2Color = getComputedStyle(document.documentElement)
        .getPropertyValue('--chart-line2-color')
        .trim();

    if (canvas === null) return;
    const ctx = canvas.getContext('2d');
    new Chart(ctx, {
        type: 'line',
        data: {
            labels: dates,
            datasets: [
                {
                    label: 'Wallet balance',
                    data: walletBalances,
                    backgroundColor: charLine1Color,
                    borderColor: charLine1Color,
                    borderWidth: 2,
                    fill: false
                },
                {
                    label: 'Sum of deposits',
                    data: deposit,
                    backgroundColor: charLine2Color,
                    borderColor: charLine2Color,
                    borderWidth: 2,
                    fill: false
                }
            ]
        },
        options: {
            responsive: true,
            plugins: {
                legend: {
                    labels: {
                        color: textColor
                    }
                },
                title: {
                    display: false
                }
            },
            scales: {
                x: {
                    ticks: {
                        color: textColor
                    },
                    grid: {
                        color: textColor
                    }
                },
                y: {
                    beginAtZero: false,
                    ticks: {
                        color: textColor
                    },
                    grid: {
                        color: textColor
                    }
                }
            }
        }
    });
};

window.renderWalletEarningsBalance = (canvas, dates, walletEarnings) => {
    let textColor = getComputedStyle(document.documentElement)
        .getPropertyValue('--chart-text-color')
        .trim();

    let charLine1Color = getComputedStyle(document.documentElement)
        .getPropertyValue('--chart-line1-color')
        .trim();

    if (canvas === null) return;
    const ctx = canvas.getContext('2d');
    new Chart(ctx, {
        type: 'line',
        data: {
            labels: dates,
            datasets: [
                {
                    label: 'Wallet Earnings',
                    data: walletEarnings,
                    backgroundColor: charLine1Color,
                    borderColor: charLine1Color,
                    borderWidth: 2,
                    fill: false
                }
            ]
        },
        options: {
            responsive: true,
            plugins: {
                legend: {
                    labels: {
                        color: textColor
                    }
                },
                title: {
                    display: false
                }
            },
            scales: {
                x: {
                    ticks: {
                        color: textColor  // X
                    },
                    grid: {
                        color: textColor
                    }
                },
                y: {
                    beginAtZero: false,
                    ticks: {
                        color: textColor // Y
                    },
                    grid: {
                        color: textColor
                    }
                }
            }
        }
    });
};