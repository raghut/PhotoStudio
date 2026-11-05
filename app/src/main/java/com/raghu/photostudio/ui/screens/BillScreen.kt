package com.raghu.photostudio.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.raghu.photostudio.R
import com.raghu.photostudio.bill.BillSharing
import com.raghu.photostudio.data.Prefs
import com.raghu.photostudio.tools.Bill
import com.raghu.photostudio.tools.Money
import com.raghu.photostudio.ui.StudioTopBar
import com.raghu.photostudio.ui.theme.Teal
import java.time.LocalDate

/** One line on the bill as typed: text boxes stay text until the bill is made. */
private data class ItemRow(val id: Long, val name: String = "", val qty: String = "1", val rate: String = "")

private val rowsSaver = listSaver<SnapshotStateList<ItemRow>, String>(
    save = { rows -> rows.flatMap { listOf(it.id.toString(), it.name, it.qty, it.rate) } },
    restore = { flat -> flat.chunked(4).map { ItemRow(it[0].toLong(), it[1], it[2], it[3]) }.toMutableStateList() },
)

private fun newRow(name: String = "", rate: String = "") = ItemRow(System.nanoTime(), name, "1", rate)

private fun ItemRow.isEmpty() = name.isBlank() && rate.isBlank()

private fun ItemRow.toItem(): Bill.Item? {
    val q = Money.parse(qty) ?: 1.0
    val r = Money.parse(rate) ?: 0.0
    return if (name.isBlank() && r == 0.0) null else Bill.Item(name.trim(), q, r)
}

/**
 * Quick bill: items × qty × rate, discount, money received. Sends the receipt as a picture
 * or WhatsApp message, or prints it. The shop name is typed once and remembered.
 */
@Composable
fun BillScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { Prefs(context) }

    var shopName by remember { mutableStateOf(prefs.shopName) }
    var shopDetails by remember { mutableStateOf(prefs.shopDetails) }
    var editingShop by rememberSaveable { mutableStateOf(prefs.shopName.isBlank()) }
    var recent by remember { mutableStateOf(prefs.recentItems) }

    var number by rememberSaveable { mutableStateOf(prefs.nextBillNumber) }
    /** True once this bill has been sent or printed — its number is then used up. */
    var issued by rememberSaveable { mutableStateOf(false) }
    var customer by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    val rows = rememberSaveable(saver = rowsSaver) { mutableListOf(newRow()).toMutableStateList() }
    var discountText by rememberSaveable { mutableStateOf("") }
    var receivedText by rememberSaveable { mutableStateOf("") }

    val today = LocalDate.now()
    val bill = Bill(
        number = number,
        date = today,
        shopName = shopName,
        shopDetails = shopDetails,
        customerName = customer,
        customerPhone = phone,
        items = rows.mapNotNull { it.toItem() },
        discount = Money.parse(discountText) ?: 0.0,
        received = Money.parse(receivedText),
    )
    val ready = bill.items.isNotEmpty()

    val labels = Bill.Labels(
        bill = stringResource(R.string.bill_word),
        date = stringResource(R.string.bill_date),
        customer = stringResource(R.string.bill_customer),
        item = stringResource(R.string.bill_item),
        qty = stringResource(R.string.bill_qty),
        rate = stringResource(R.string.bill_rate),
        amount = stringResource(R.string.bill_amount),
        subtotal = stringResource(R.string.bill_subtotal),
        discount = stringResource(R.string.bill_discount),
        total = stringResource(R.string.bill_total),
        received = stringResource(R.string.bill_received),
        balance = stringResource(R.string.bill_balance),
        thanks = stringResource(R.string.bill_thanks),
    )
    val jobName = stringResource(R.string.bill_print_job, number)

    /** First send/print uses up the bill number and remembers the items for next time. */
    fun issue() {
        if (!issued) {
            issued = true
            if (prefs.nextBillNumber <= number) prefs.nextBillNumber = number + 1
        }
        prefs.rememberItems(bill.items.filter { it.name.isNotBlank() }.map { it.name to it.rate })
        recent = prefs.recentItems
    }

    fun startNewBill() {
        if (issued) number = prefs.nextBillNumber
        issued = false
        customer = ""; phone = ""; discountText = ""; receivedText = ""
        rows.clear(); rows.add(newRow())
    }

    val money: (String) -> String = { it.filter { c -> c.isDigit() || c == '.' || c == ',' } }

    Scaffold(
        topBar = {
            StudioTopBar(stringResource(R.string.bill_title), onBack) {
                TextButton(onClick = { startNewBill() }) { Text(stringResource(R.string.bill_new)) }
            }
        },
    ) { pad ->
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(pad),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Shop + bill number
            item {
                Card(
                    onClick = { editingShop = true },
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                shopName.ifBlank { stringResource(R.string.bill_shop_add) },
                                style = MaterialTheme.typography.titleLarge,
                            )
                            if (shopDetails.isNotBlank()) Text(shopDetails, style = MaterialTheme.typography.bodyMedium)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                stringResource(R.string.bill_number_date, number, Money.date(today)),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                        Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.bill_shop_edit))
                    }
                }
            }

            // Customer
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = customer,
                        onValueChange = { customer = it },
                        label = { Text(stringResource(R.string.bill_customer_name)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it.filter { c -> c.isDigit() || c == '+' || c == ' ' }.take(16) },
                        label = { Text(stringResource(R.string.bill_customer_phone)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            // Items
            itemsIndexed(rows, key = { _, r -> r.id }) { index, row ->
                ItemCard(
                    number = index + 1,
                    row = row,
                    onChange = { rows[index] = it },
                    onDelete = if (rows.size > 1) ({ rows.removeAt(index) }) else null,
                    money = money,
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (recent.isNotEmpty()) {
                        Text(
                            stringResource(R.string.bill_recent),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Row(
                            Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            recent.forEach { (name, rate) ->
                                AssistChip(
                                    onClick = {
                                        val r = Money.plain(rate)
                                        val last = rows.lastOrNull()
                                        if (last != null && last.isEmpty()) {
                                            rows[rows.lastIndex] = last.copy(name = name, rate = r)
                                        } else {
                                            rows.add(newRow(name, r))
                                        }
                                    },
                                    label = { Text("$name · ${Money.rupees(rate)}") },
                                )
                            }
                        }
                    }
                    OutlinedButton(
                        onClick = { rows.add(newRow()) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Spacer(Modifier.size(8.dp))
                        Text(stringResource(R.string.bill_add_item))
                    }
                }
            }

            // Discount + received
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = discountText,
                        onValueChange = { discountText = money(it) },
                        label = { Text(stringResource(R.string.bill_discount)) },
                        prefix = { Text("₹ ") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = receivedText,
                        onValueChange = { receivedText = money(it) },
                        label = { Text(stringResource(R.string.bill_received)) },
                        prefix = { Text("₹ ") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            // Totals + send
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (bill.discount > 0) {
                            TotalLine(labels.subtotal, Money.rupees(bill.subtotal))
                            TotalLine(labels.discount, "− " + Money.rupees(bill.discount))
                            HorizontalDivider()
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(labels.total, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                            Text(Money.rupees(bill.total), style = MaterialTheme.typography.headlineMedium, color = Teal)
                        }
                        val bal = bill.balance
                        if (bill.received != null && bal != null) {
                            TotalLine(labels.received, Money.rupees(bill.received))
                            TotalLine(labels.balance, Money.rupees(bal), strong = true)
                        }
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (!ready) {
                        Text(
                            stringResource(R.string.bill_empty),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    Button(
                        onClick = { issue(); BillSharing.shareImage(context, bill, labels) },
                        enabled = ready,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                    ) {
                        Icon(Icons.Filled.Share, contentDescription = null)
                        Spacer(Modifier.size(8.dp))
                        Text(stringResource(R.string.bill_send_picture))
                    }
                    OutlinedButton(
                        onClick = { issue(); BillSharing.shareText(context, bill, labels) },
                        enabled = ready,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                    ) {
                        Text(
                            if (Bill.whatsAppNumber(phone) != null) stringResource(R.string.bill_send_text_to, phone.trim())
                            else stringResource(R.string.bill_send_text),
                        )
                    }
                    OutlinedButton(
                        onClick = { issue(); BillSharing.print(context, bill, labels, jobName) },
                        enabled = ready,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                    ) { Text(stringResource(R.string.bill_print)) }
                }
            }
        }
    }

    if (editingShop) {
        ShopDialog(
            name = shopName,
            details = shopDetails,
            onSave = { n, d ->
                shopName = n.trim(); shopDetails = d.trim()
                prefs.shopName = n; prefs.shopDetails = d
                editingShop = false
            },
            onDismiss = { editingShop = false },
        )
    }
}

@Composable
private fun ItemCard(
    number: Int,
    row: ItemRow,
    onChange: (ItemRow) -> Unit,
    onDelete: (() -> Unit)?,
    money: (String) -> String,
) {
    val amount = (Money.parse(row.qty) ?: 1.0) * (Money.parse(row.rate) ?: 0.0)
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "$number.",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.width(32.dp),
                )
                OutlinedTextField(
                    value = row.name,
                    onValueChange = { onChange(row.copy(name = it)) },
                    label = { Text(stringResource(R.string.bill_item)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.weight(1f),
                )
                if (onDelete != null) {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.bill_remove_item))
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.width(24.dp))
                OutlinedTextField(
                    value = row.qty,
                    onValueChange = { onChange(row.copy(qty = it.filter { c -> c.isDigit() || c == '.' })) },
                    label = { Text(stringResource(R.string.bill_qty)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(0.8f),
                )
                OutlinedTextField(
                    value = row.rate,
                    onValueChange = { onChange(row.copy(rate = money(it))) },
                    label = { Text(stringResource(R.string.bill_rate)) },
                    prefix = { Text("₹") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1.1f),
                )
                Text(
                    "= " + Money.rupees(amount),
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(1.1f),
                )
            }
        }
    }
}

@Composable
private fun TotalLine(label: String, value: String, strong: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(value, style = if (strong) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun ShopDialog(name: String, details: String, onSave: (String, String) -> Unit, onDismiss: () -> Unit) {
    var n by rememberSaveable { mutableStateOf(name) }
    var d by rememberSaveable { mutableStateOf(details) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.bill_shop_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(R.string.bill_shop_explain), style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(
                    value = n,
                    onValueChange = { n = it },
                    label = { Text(stringResource(R.string.bill_shop_name)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                )
                OutlinedTextField(
                    value = d,
                    onValueChange = { d = it },
                    label = { Text(stringResource(R.string.bill_shop_details)) },
                    singleLine = true,
                )
            }
        },
        confirmButton = { TextButton(onClick = { onSave(n, d) }) { Text(stringResource(R.string.name_save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
