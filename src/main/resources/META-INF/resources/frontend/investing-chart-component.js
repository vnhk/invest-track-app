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

window.renderFireProjectionChart = (canvas, yearsLabels, baseline, plus20, minus20, onlyDeposits, avgInvestmentForAMonth, totalSavingForAMonth) => {

    let textColor = getComputedStyle(document.documentElement)
        .getPropertyValue('--chart-text-color')
        .trim();

    let line1 = getComputedStyle(document.documentElement)
        .getPropertyValue('--chart-line1-color')
        .trim();

    let line2 = getComputedStyle(document.documentElement)
        .getPropertyValue('--chart-line2-color')
        .trim();

    let line3 = getComputedStyle(document.documentElement)
        .getPropertyValue('--chart-line3-color')
        ?.trim() || '#888';

    if (canvas === null) return;

    const ctx = canvas.getContext('2d');

    avgInvestmentForAMonth = Math.round(avgInvestmentForAMonth);
    totalSavingForAMonth = Math.round(totalSavingForAMonth);
    let monthly80 = Math.round(avgInvestmentForAMonth * 0.8);
    let monthly120 = Math.round(avgInvestmentForAMonth * 1.2);
    let monthly120_otherSavings = 0;

    if (monthly120 > totalSavingForAMonth) {
        monthly120 = Math.round(totalSavingForAMonth);
    } else {
        monthly120_otherSavings = Math.round(totalSavingForAMonth - monthly120);
    }

    let currentlyNotInvested = Math.round(totalSavingForAMonth - avgInvestmentForAMonth);
    let monthly80_notInvested = Math.round(totalSavingForAMonth - monthly80);

    new Chart(ctx, {
        type: 'line',
        data: {
            labels: yearsLabels,
            datasets: [
                {
                    label: 'Currently (' + avgInvestmentForAMonth + ' invested, ' + currentlyNotInvested + ' not invested)',
                    data: baseline,
                    borderColor: line1,
                    backgroundColor: line1,
                    borderWidth: 2,
                    fill: false
                },
                {
                    label: '+20% deposits (' + monthly120 + ' invested, ' + monthly120_otherSavings + ' not invested)',
                    data: plus20,
                    borderColor: line2,
                    backgroundColor: line2,
                    borderWidth: 2,
                    fill: false
                },
                {
                    label: '-20% deposits (' + monthly80 + ' invested, ' + monthly80_notInvested + ' not invested)',
                    data: minus20,
                    borderColor: line3,
                    backgroundColor: line3,
                    borderWidth: 2,
                    borderDash: [5, 5],
                    fill: false
                },
                {
                    label: 'Only deposits (' + totalSavingForAMonth + ' savings)',
                    data: onlyDeposits,
                    borderColor: 'red',
                    backgroundColor: 'red',
                    borderDash: [5, 5],
                    borderWidth: 5,
                    fill: false
                }
            ]
        },
        options: {
            responsive: true,
            plugins: {
                legend: {
                    labels: {color: textColor}
                }
            },
            scales: {
                x: {
                    ticks: {color: textColor},
                    grid: {color: textColor}
                },
                y: {
                    ticks: {color: textColor},
                    grid: {color: textColor}
                }
            }
        }
    });
};

window.renderStrategyBGRHistoryChart = (canvas, dates, bestRecPercent, goodRecPercent, riskyRecPercent) => {

    let textColor = getComputedStyle(document.documentElement)
        .getPropertyValue('--chart-text-color')
        .trim();

    let line1 = getComputedStyle(document.documentElement)
        .getPropertyValue('--chart-line1-color')
        .trim();

    let line2 = getComputedStyle(document.documentElement)
        .getPropertyValue('--chart-line2-color')
        .trim();

    let line3 = getComputedStyle(document.documentElement)
        .getPropertyValue('--chart-line3-color')
        ?.trim() || '#888';

    if (canvas === null) return;

    const ctx = canvas.getContext('2d');

    new Chart(ctx, {
        type: 'line',
        data: {
            labels: dates,
            datasets: [
                {
                    label: 'Best recommendations',
                    data: bestRecPercent,
                    borderColor: line1,
                    backgroundColor: line1,
                    borderWidth: 2,
                    fill: false
                },
                {
                    label: 'Good recommendations',
                    data: goodRecPercent,
                    borderColor: line2,
                    backgroundColor: line2,
                    borderWidth: 2,
                    fill: false
                },
                {
                    label: 'Risky recommendations',
                    data: riskyRecPercent,
                    borderColor: line3,
                    backgroundColor: line3,
                    borderWidth: 2,
                    borderDash: [5, 5],
                    fill: false
                }
            ]
        },
        options: {
            responsive: true,
            plugins: {
                legend: {
                    labels: {color: textColor}
                }
            },
            scales: {
                x: {
                    ticks: {color: textColor},
                    grid: {color: textColor}
                },
                y: {
                    ticks: {color: textColor},
                    grid: {color: textColor}
                }
            }
        }
    });
};