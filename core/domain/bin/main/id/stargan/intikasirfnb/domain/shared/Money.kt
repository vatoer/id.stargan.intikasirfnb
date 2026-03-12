package id.stargan.intikasirfnb.domain.shared

import java.math.BigDecimal

/**
 * Value object for monetary amount. Used across Transaction, Catalog, Accounting, Pricing.
 */
data class Money(
    val amount: BigDecimal,
    val currencyCode: String = "IDR"
) {
    operator fun plus(other: Money): Money {
        require(currencyCode == other.currencyCode) { "Cannot add different currencies" }
        return Money(amount.add(other.amount), currencyCode)
    }

    operator fun minus(other: Money): Money {
        require(currencyCode == other.currencyCode) { "Cannot subtract different currencies" }
        return Money(amount.subtract(other.amount), currencyCode)
    }

    operator fun times(factor: Int): Money = Money(amount.multiply(BigDecimal(factor)), currencyCode)

    fun isZero(): Boolean = amount.compareTo(BigDecimal.ZERO) == 0
    fun isPositive(): Boolean = amount.compareTo(BigDecimal.ZERO) > 0

    companion object {
        fun zero(currencyCode: String = "IDR") = Money(BigDecimal.ZERO, currencyCode)
    }
}
