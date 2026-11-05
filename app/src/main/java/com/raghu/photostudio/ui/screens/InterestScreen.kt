package com.raghu.photostudio.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.raghu.photostudio.R
import com.raghu.photostudio.tools.Money
import com.raghu.photostudio.tools.SimpleInterest
import com.raghu.photostudio.ui.StudioTopBar
import com.raghu.photostudio.ui.theme.Teal
import java.time.LocalDate

private const val DAY_MS = 86_400_000L

/** The rates people usually say: ₹1, ₹1.5, ₹2 … for every ₹100 a month. */
private val COMMON_RATES = listOf("1", "1.5", "2", "2.5", "3", "5")

/**
 * Simple interest the village way: amount, the date it was given, the date it comes back,
 * and "₹2 for every ₹100 a month". Shows the interest month by month and the final total.
 */
@Composable
fun InterestScreen(onBack: () -> Unit) {
    val context = LocalContext.current

    var amountText by rememberSaveable { mutableStateOf("") }
    var rateText by rememberSaveable { mutableStateOf("2") }
    var givenDay by rememberSaveable { mutableStateOf<Long?>(null) }
    var returnDay by rememberSaveable { mutableStateOf(LocalDate.now().toEpochDay()) }
    var picking by rememberSaveable { mutableStateOf<String?>(null) } // "given" / "return" / null

    val amount = Money.parse(amountText)
    val rate = Money.parse(rateText)
    val given = givenDay?.let(LocalDate::ofEpochDay)
    val returned = LocalDate.ofEpochDay(returnDay)
    val badDates = given != null && returned.isBefore(given)
    val result = remember(amount, rate, given, returned) {
        if (amount == null || rate == null || given == null) null
        else SimpleInterest.calculate(amount, rate, given, returned)
    }

    Scaffold(
        topBar = {
            StudioTopBar(stringResource(R.string.interest_title), onBack) {
                TextButton(onClick = {
                    amountText = ""; rateText = "2"; givenDay = null
                    returnDay = LocalDate.now().toEpochDay()
                }) { Text(stringResource(R.string.interest_clear)) }
            }
        },
    ) { pad ->
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(pad),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                InputCard(
                    amountText = amountText,
                    onAmount = { amountText = it.filter { c -> c.isDigit() || c == '.' || c == ',' } },
                    rateText = rateText,
                    onRate = { rateText = it.filter { c -> c.isDigit() || c == '.' } },
                    rate = rate,
                    given = given,
                    returned = returned,
                    onPickGiven = { picking = "given" },
                    onPickReturn = { picking = "return" },
                )
            }

            when {
                badDates -> item { Hint(stringResource(R.string.interest_bad_dates), isError = true) }
                result == null -> item { Hint(stringResource(R.string.interest_enter), isError = false) }
                else -> {
                    item { SummaryCard(result) { shareResult(context, result) } }
                    if (result.months.isNotEmpty()) {
                        item {
                            Text(
                                stringResource(R.string.interest_breakdown),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 6.dp, start = 4.dp),
                            )
                        }
                        items(result.months, key = { it.number }) { MonthRow(it) }
                        item { TotalRow(result) }
                    }
                }
            }
        }
    }

    picking?.let { which ->
        val start = if (which == "given") givenDay ?: returnDay else returnDay
        DatePick(
            initialEpochDay = start,
            onPicked = { day ->
                if (which == "given") givenDay = day else returnDay = day
                picking = null
            },
            onDismiss = { picking = null },
        )
    }
}

@Composable
private fun InputCard(
    amountText: String,
    onAmount: (String) -> Unit,
    rateText: String,
    onRate: (String) -> Unit,
    rate: Double?,
    given: LocalDate?,
    returned: LocalDate,
    onPickGiven: () -> Unit,
    onPickReturn: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = amountText,
                onValueChange = onAmount,
                label = { Text(stringResource(R.string.interest_amount)) },
                prefix = { Text("₹ ") },
                singleLine = true,
                textStyle = MaterialTheme.typography.titleLarge,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )

            Text(stringResource(R.string.interest_rate), style = MaterialTheme.typography.titleMedium)
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                COMMON_RATES.forEach { r ->
                    FilterChip(
                        selected = rate != null && Money.parse(r) == rate,
                        onClick = { onRate(r) },
                        label = { Text("₹$r", style = MaterialTheme.typography.titleMedium) },
                        modifier = Modifier.height(44.dp),
                    )
                }
            }
            OutlinedTextField(
                value = rateText,
                onValueChange = onRate,
                prefix = { Text("₹ ") },
                suffix = { Text(stringResource(R.string.interest_rate_suffix)) },
                singleLine = true,
                textStyle = MaterialTheme.typography.titleLarge,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                supportingText = {
                    if (rate != null) Text(stringResource(R.string.interest_rate_year, Money.plain(rate * 12)))
                },
                modifier = Modifier.fillMaxWidth(),
            )

            DateField(
                label = stringResource(R.string.interest_given_date),
                value = given?.let(Money::date) ?: stringResource(R.string.interest_pick_date),
                missing = given == null,
                onClick = onPickGiven,
            )
            DateField(
                label = stringResource(R.string.interest_return_date),
                value = Money.date(returned) +
                    if (returned == LocalDate.now()) "  (" + stringResource(R.string.interest_today) + ")" else "",
                missing = false,
                onClick = onPickReturn,
            )
        }
    }
}

@Composable
private fun DateField(label: String, value: String, missing: Boolean, onClick: () -> Unit) {
    Column {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        OutlinedButton(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp),
        ) {
            Icon(Icons.Filled.DateRange, contentDescription = null)
            Spacer(Modifier.size(10.dp))
            Text(
                value,
                style = MaterialTheme.typography.titleMedium,
                color = if (missing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun Hint(text: String, isError: Boolean) {
    Text(
        text,
        style = MaterialTheme.typography.bodyLarge,
        color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
    )
}

@Composable
private fun SummaryCard(r: SimpleInterest.Result, onShare: () -> Unit) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Teal, contentColor = Color.White),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SummaryLine(
                stringResource(R.string.interest_time),
                stringResource(R.string.interest_months_days, r.fullMonths, r.extraDays),
            )
            Text(
                stringResource(R.string.interest_total_days, r.totalDays.toInt()),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.align(Alignment.End),
            )
            SummaryLine(stringResource(R.string.interest_per_month), Money.rupees(r.interestPerMonth))
            HorizontalDivider(color = Color.White.copy(alpha = 0.3f))
            SummaryLine(stringResource(R.string.interest_principal), Money.rupees(r.principal))
            SummaryLine(stringResource(R.string.interest_total), Money.rupees(r.totalInterest))
            HorizontalDivider(color = Color.White.copy(alpha = 0.3f))
            Text(stringResource(R.string.interest_grand_total), style = MaterialTheme.typography.bodyLarge)
            Text(
                Money.rupees(r.totalAmount),
                style = MaterialTheme.typography.headlineMedium,
            )
            Spacer(Modifier.height(4.dp))
            Button(
                onClick = onShare,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Teal,
                ),
            ) {
                Icon(Icons.Filled.Share, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text(stringResource(R.string.interest_share))
            }
        }
    }
}

@Composable
private fun SummaryLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun MonthRow(m: SimpleInterest.Month) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.interest_month_n, m.number), style = MaterialTheme.typography.titleMedium)
                Text(
                    "${Money.date(m.from)} → ${Money.date(m.to)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                m.partDays?.let {
                    Text(
                        stringResource(R.string.interest_part_month, it),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(Money.rupees(m.interest), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    stringResource(R.string.interest_so_far, Money.rupees(m.interestSoFar)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun TotalRow(r: SimpleInterest.Result) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            SummaryLine(stringResource(R.string.interest_principal), Money.rupees(r.principal))
            SummaryLine(stringResource(R.string.interest_total), Money.rupees(r.totalInterest))
            SummaryLine(stringResource(R.string.interest_grand_total), Money.rupees(r.totalAmount))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePick(initialEpochDay: Long, onPicked: (Long) -> Unit, onDismiss: () -> Unit) {
    // The picker works in UTC milliseconds; whole days convert cleanly both ways.
    val state = rememberDatePickerState(initialSelectedDateMillis = initialEpochDay * DAY_MS)
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let { onPicked(Math.floorDiv(it, DAY_MS)) } ?: onDismiss()
            }) { Text(stringResource(R.string.interest_ok)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    ) {
        DatePicker(state = state)
    }
}

/** Sends a short summary as text — lands nicely in a WhatsApp chat. */
private fun shareResult(context: Context, r: SimpleInterest.Result) {
    val text = buildString {
        appendLine(context.getString(R.string.interest_share_title))
        appendLine(context.getString(R.string.interest_principal) + ": " + Money.rupees(r.principal))
        appendLine(context.getString(R.string.interest_share_rate, Money.plain(r.ratePer100), Money.plain(r.percentPerYear)))
        appendLine(context.getString(R.string.interest_share_dates, Money.date(r.given), Money.date(r.returned)))
        appendLine(
            context.getString(R.string.interest_time) + ": " +
                context.getString(R.string.interest_months_days, r.fullMonths, r.extraDays),
        )
        appendLine(context.getString(R.string.interest_per_month) + ": " + Money.rupees(r.interestPerMonth))
        appendLine(context.getString(R.string.interest_total) + ": " + Money.rupees(r.totalInterest))
        append(context.getString(R.string.interest_grand_total) + ": " + Money.rupees(r.totalAmount))
    }
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(send, context.getString(R.string.interest_share)))
}
