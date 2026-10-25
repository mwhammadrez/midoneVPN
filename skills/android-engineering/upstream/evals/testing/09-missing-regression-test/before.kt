package com.example.pricing

// Reported bug: a loyalty discount of exactly the threshold amount
// (spend == 100.00) does not get the discount applied - customers right
// at the threshold are charged full price.
class LoyaltyPricing {
    fun applyDiscount(spendCents: Long): Long {
        val threshold = 10_000L // $100.00
        return if (spendCents > threshold) {
            (spendCents * 0.9).toLong()
        } else {
            spendCents
        }
    }
}
