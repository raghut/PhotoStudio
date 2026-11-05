package com.raghu.photostudio.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.raghu.photostudio.R
import com.raghu.photostudio.tools.ChitFund
import com.raghu.photostudio.tools.Money
import com.raghu.photostudio.ui.StudioTopBar
import com.raghu.photostudio.ui.theme.Teal

/**
 * Chit fund auction: what the winner takes, what everyone pays this month,
 * and what interest the winner is effectively paying for taking the money early.
 */
@Composable
fun ChitScreen(onBack: () -> Unit) {
    val context = LocalContext.current

    // Starts with the same example as the website, so it shows a full answer straight away.
    var amountText by rememberSaveable { mutableStateOf("3,00,000") }
    var membersText by rememberSaveable { mutableStateOf("20") }
    var monthsText by rememberSaveable { mutableStateOf("20") }
    var commissionText by rememberSaveable { mutableStateOf("4") }
    var auctionMonthText by rememberSaveable { mutableStateOf("14") }
    var auctionText by rememberSaveable { mutableStateOf("20,000") }

    val amount = Money.parse(amountText)
    val members = membersText.toIntOrNull()
    val months = monthsText.toIntOrNull()
    val commission = Money.parse(commissionText)
    val auctionMonth = auctionMonthText.toIntOrNull()
    val auction = Money.parse(auctionText)

    val (result, problem) =
        if (amount == null || members == null || months == null || commission == null ||
            auctionMonth == null || auction == null
        ) {
            null to ChitFund.Problem.MISSING
        } else {
            ChitFund.calculate(ChitFund.Input(amount, members, months, commission, auctionMonth, auction))
        }

    val money: (String) -> String = { it.filter { c -> c.isDigit() || c == '.' || c == ',' } }
    val whole: (String) -> String = { it.filter(Char::isDigit).take(4) }

    Scaffold(
        topBar = {
            StudioTopBar(stringResource(R.string.chit_title), onBack) {
                TextButton(onClick = {
                    amountText = ""; membersText = ""; monthsText = ""
                    commissionText = ""; auctionMonthText = ""; auctionText = ""
                }) { Text(stringResource(R.string.interest_clear)) }
            }
        },
    ) { pad ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(pad)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Field(stringResource(R.string.chit_amount), amountText, { amountText = money(it) }, rupee = true)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Field(
                            stringResource(R.string.chit_members), membersText, { membersText = whole(it) },
                            decimal = false, modifier = Modifier.weight(1f),
                        )
                        Field(
                            stringResource(R.string.chit_months), monthsText, { monthsText = whole(it) },
                            decimal = false, modifier = Modifier.weight(1f),
                        )
                    }
                    Field(
                        stringResource(R.string.chit_commission), commissionText,
                        { commissionText = it.filter { c -> c.isDigit() || c == '.' } }, suffix = "%",
                    )
                    Field(
                        stringResource(R.string.chit_auction_month), auctionMonthText, { auctionMonthText = whole(it) },
                        decimal = false,
                        suffix = months?.let { stringResource(R.string.chit_of_months, it) },
                    )
                    Field(stringResource(R.string.chit_auction_amount), auctionText, { auctionText = money(it) }, rupee = true)
                    Text(
                        stringResource(R.string.chit_auction_help),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (result == null) {
                Text(
                    stringResource(
                        when (problem) {
                            ChitFund.Problem.AUCTION_MONTH -> R.string.chit_err_month
                            ChitFund.Problem.AUCTION_TOO_BIG -> R.string.chit_err_auction
                            ChitFund.Problem.COMMISSION -> R.string.chit_err_commission
                            else -> R.string.chit_err_missing
                        },
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (problem == ChitFund.Problem.MISSING) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                )
            } else {
                WinnerCard(result, auctionMonth!!) {
                    share(context, amount!!, members!!, months!!, commission!!, auctionMonth, auction!!, result)
                }
                InfoCard(stringResource(R.string.chit_everyone_title, auctionMonth)) {
                    Line(stringResource(R.string.chit_monthly), Money.rupees(result.monthlyContribution))
                    Line(stringResource(R.string.chit_dividend), "− " + Money.rupees(result.dividendPerMember))
                    HorizontalDivider()
                    Line(stringResource(R.string.chit_net_payable), Money.rupees(result.netPayablePerMember), strong = true)
                }
                InfoCard(stringResource(R.string.chit_split_title)) {
                    Line(stringResource(R.string.chit_auction_amount), Money.rupees(auction!!))
                    Line(stringResource(R.string.chit_commission_amount), "− " + Money.rupees(result.commission))
                    HorizontalDivider()
                    Line(stringResource(R.string.chit_dividend_total), Money.rupees(result.dividendTotal), strong = true)
                    Text(
                        stringResource(R.string.chit_dividend_split, members!!, Money.rupees(result.dividendPerMember)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun Field(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
    rupee: Boolean = false,
    decimal: Boolean = true,
    suffix: String? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        prefix = { if (rupee) Text("₹ ") },
        suffix = { if (suffix != null) Text(suffix) },
        singleLine = true,
        textStyle = MaterialTheme.typography.titleLarge,
        keyboardOptions = KeyboardOptions(keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number),
        modifier = modifier,
    )
}

@Composable
private fun WinnerCard(r: ChitFund.Result, auctionMonth: Int, onShare: () -> Unit) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Teal, contentColor = Color.White),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.chit_winner_takes, auctionMonth), style = MaterialTheme.typography.bodyLarge)
            Text(Money.rupees(r.prizeAmount), style = MaterialTheme.typography.headlineMedium)
            HorizontalDivider(color = Color.White.copy(alpha = 0.3f))
            Text(stringResource(R.string.chit_interest_title), style = MaterialTheme.typography.titleMedium)
            if (r.annualRatePercent != null) {
                Text(
                    stringResource(R.string.chit_rate_rupees, Money.plain(r.ratePer100PerMonth!!)),
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    stringResource(R.string.chit_rate_percent, Money.plain(r.annualRatePercent)),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    stringResource(R.string.chit_remaining, r.remainingMonths),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.85f),
                )
            } else {
                Text(stringResource(R.string.chit_last_month), style = MaterialTheme.typography.bodyLarge)
            }
            Spacer(Modifier.height(4.dp))
            Button(
                onClick = onShare,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Teal),
            ) {
                Icon(Icons.Filled.Share, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text(stringResource(R.string.interest_share))
            }
        }
    }
}

@Composable
private fun InfoCard(title: String, content: @Composable () -> Unit) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            content()
        }
    }
}

@Composable
private fun Line(label: String, value: String, strong: Boolean = false) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f).padding(end = 8.dp))
        Text(value, style = if (strong) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium)
    }
}

private fun share(
    context: Context,
    amount: Double,
    members: Int,
    months: Int,
    commission: Double,
    auctionMonth: Int,
    auction: Double,
    r: ChitFund.Result,
) {
    fun g(id: Int): String = context.getString(id)
    val text = buildString {
        appendLine(g(R.string.chit_title))
        appendLine(g(R.string.chit_amount) + ": " + Money.rupees(amount))
        appendLine(g(R.string.chit_members) + ": " + members + " · " + g(R.string.chit_months) + ": " + months)
        appendLine(g(R.string.chit_commission) + ": " + Money.plain(commission) + "%")
        appendLine(g(R.string.chit_auction_month) + ": " + auctionMonth)
        appendLine(g(R.string.chit_auction_amount) + ": " + Money.rupees(auction))
        appendLine()
        appendLine(context.getString(R.string.chit_winner_takes, auctionMonth) + ": " + Money.rupees(r.prizeAmount))
        appendLine(g(R.string.chit_net_payable) + ": " + Money.rupees(r.netPayablePerMember))
        appendLine(g(R.string.chit_dividend) + ": " + Money.rupees(r.dividendPerMember))
        if (r.annualRatePercent != null) {
            append(g(R.string.chit_interest_title) + ": ")
            append(context.getString(R.string.chit_rate_rupees, Money.plain(r.ratePer100PerMonth!!)))
            append(" (" + context.getString(R.string.chit_rate_percent, Money.plain(r.annualRatePercent)) + ")")
        } else {
            append(g(R.string.chit_last_month))
        }
    }
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(send, context.getString(R.string.interest_share)))
}
