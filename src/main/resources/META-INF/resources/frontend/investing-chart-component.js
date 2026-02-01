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

// Budget Income vs Expense stacked bar chart
window.renderBudgetIncomeExpenseChart = (canvas, months, incomeData, expenseData) => {
    let textColor = getComputedStyle(document.documentElement)
        .getPropertyValue('--chart-text-color')
        .trim() || 'rgba(255,255,255,0.9)';

    if (canvas === null) return;
    const ctx = canvas.getContext('2d');

    new Chart(ctx, {
        type: 'bar',
        data: {
            labels: months,
            datasets: [
                {
                    label: 'Income',
                    data: incomeData,
                    backgroundColor: 'rgba(16, 185, 129, 0.8)',
                    borderColor: 'rgba(16, 185, 129, 1)',
                    borderWidth: 1
                },
                {
                    label: 'Expense',
                    data: expenseData,
                    backgroundColor: 'rgba(239, 68, 68, 0.8)',
                    borderColor: 'rgba(239, 68, 68, 1)',
                    borderWidth: 1
                }
            ]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: {
                    labels: {color: textColor}
                }
            },
            scales: {
                x: {
                    stacked: false,
                    ticks: {color: textColor},
                    grid: {color: 'rgba(255,255,255,0.1)'}
                },
                y: {
                    stacked: false,
                    beginAtZero: true,
                    ticks: {color: textColor},
                    grid: {color: 'rgba(255,255,255,0.1)'}
                }
            }
        }
    });
};

// Asset Allocation Pie Chart
window.renderAssetAllocationChart = (canvas, labels, values, colors) => {
    let textColor = getComputedStyle(document.documentElement)
        .getPropertyValue('--chart-text-color')
        .trim() || 'rgba(255,255,255,0.9)';

    if (canvas === null) return;
    const ctx = canvas.getContext('2d');

    // Default colors if not provided
    const defaultColors = [
        'rgba(99, 102, 241, 0.8)',
        'rgba(34, 211, 238, 0.8)',
        'rgba(16, 185, 129, 0.8)',
        'rgba(245, 158, 11, 0.8)',
        'rgba(239, 68, 68, 0.8)',
        'rgba(139, 92, 246, 0.8)',
        'rgba(236, 72, 153, 0.8)',
        'rgba(59, 130, 246, 0.8)'
    ];

    const bgColors = colors && colors.length > 0 ? colors : defaultColors;

    new Chart(ctx, {
        type: 'doughnut',
        data: {
            labels: labels,
            datasets: [{
                data: values,
                backgroundColor: bgColors,
                borderColor: 'rgba(255,255,255,0.2)',
                borderWidth: 2
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: {
                    position: 'right',
                    labels: {
                        color: textColor,
                        padding: 15,
                        font: {size: 12}
                    }
                },
                tooltip: {
                    callbacks: {
                        label: function(context) {
                            const total = context.dataset.data.reduce((a, b) => a + b, 0);
                            const percentage = ((context.raw / total) * 100).toFixed(1);
                            return `${context.label}: ${context.raw.toLocaleString()} (${percentage}%)`;
                        }
                    }
                }
            },
            cutout: '60%'
        }
    });
};

// Category Trends Line Chart
window.renderCategoryTrendsChart = (canvas, months, categoriesData) => {
    let textColor = getComputedStyle(document.documentElement)
        .getPropertyValue('--chart-text-color')
        .trim() || 'rgba(255,255,255,0.9)';

    if (canvas === null) return;
    const ctx = canvas.getContext('2d');

    const colors = [
        'rgba(99, 102, 241, 1)',
        'rgba(34, 211, 238, 1)',
        'rgba(16, 185, 129, 1)',
        'rgba(245, 158, 11, 1)',
        'rgba(239, 68, 68, 1)',
        'rgba(139, 92, 246, 1)',
        'rgba(236, 72, 153, 1)',
        'rgba(59, 130, 246, 1)',
        'rgba(168, 162, 158, 1)',
        'rgba(251, 191, 36, 1)'
    ];

    // categoriesData is an object { categoryName: [values...] }
    const datasets = Object.keys(categoriesData).map((category, index) => ({
        label: category,
        data: categoriesData[category],
        borderColor: colors[index % colors.length],
        backgroundColor: colors[index % colors.length],
        borderWidth: 2,
        fill: false,
        tension: 0.3
    }));

    new Chart(ctx, {
        type: 'line',
        data: {
            labels: months,
            datasets: datasets
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: {
                    labels: {color: textColor}
                }
            },
            scales: {
                x: {
                    ticks: {color: textColor},
                    grid: {color: 'rgba(255,255,255,0.1)'}
                },
                y: {
                    beginAtZero: true,
                    ticks: {color: textColor},
                    grid: {color: 'rgba(255,255,255,0.1)'}
                }
            }
        }
    });
};