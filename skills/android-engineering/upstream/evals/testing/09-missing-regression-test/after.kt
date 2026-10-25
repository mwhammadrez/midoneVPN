package com.example.pricing

// "Fix" as shipped in the PR under review. The off-by-one is corrected
// (>= instead of >), but the PR adds no test at all - this is the
// fixture's actual mistake: a correct fix with no regression test.
class LoyaltyPricing {
    fun applyDiscount(spendCents: Long): Long {
        val threshold = 10_000L // $100.00
        return if (spendCents >= threshold) {
            (spendCents * 0.9).toLong()
        } else {
            spendCents
        }
    }
}
