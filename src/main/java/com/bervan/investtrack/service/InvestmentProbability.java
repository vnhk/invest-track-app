package com.bervan.investtrack.service;

public class InvestmentProbability {

    // p = probability of a good investment (e.g., 0.93)
    // n = number of investments
    public static double probabilityOfProfit(double p, int n) {
        double q = 1 - p; // probability of a bad investment
        int minGood = (n / 2) + 1; // minimum good investments needed to be in profit

        double total = 0.0;

        // Sum the binomial distribution probabilities
        for (int k = minGood; k <= n; k++) {
            total += binomial(n, k) * Math.pow(p, k) * Math.pow(q, n - k);
        }

        return total;
    }

    // Compute binomial coefficient C(n, k)
    private static long binomial(int n, int k) {
        if (k > n - k)
            k = n - k;

        long result = 1;
        for (int i = 1; i <= k; i++) {
            result = result * (n - i + 1) / i;
        }
        return result;
    }
}
