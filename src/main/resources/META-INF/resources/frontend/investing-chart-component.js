import 'https://cdn.jsdelivr.net/npm/chart.js';

window.renderWalletBalanceDepositAndWalletBalance = (canvas, dates, walletBalances, deposit) => {
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
                    backgroundColor: 'rgba(54, 162, 235, 0.2)',
                    borderColor: 'rgba(54, 162, 235, 1)',
                    borderWidth: 2,
                    fill: false
                },
                {
                    label: 'Sum of deposits',
                    data: deposit,
                    backgroundColor: 'rgb(241,175,85)',
                    borderColor: 'rgb(58,49,49)',
                    borderWidth: 2,
                    fill: false
                }
            ]
        },
        options: {
            responsive: true,
            scales: {
                y: {
                    beginAtZero: false
                }
            }
        }
    });
};

window.renderWalletEarningsBalance = (canvas, dates, walletEarnings) => {
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
                    backgroundColor: 'rgba(54, 162, 235, 0.2)',
                    borderColor: 'rgba(54, 162, 235, 1)',
                    borderWidth: 2,
                    fill: false
                }
            ]
        },
        options: {
            responsive: true,
            scales: {
                y: {
                    beginAtZero: false
                }
            }
        }
    });
};