package com.redn.farm.ui.screens.manage.employees.payment

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.redn.farm.data.model.EmployeePayment
import com.redn.farm.data.model.netPayAmount
import com.redn.farm.utils.CurrencyFormatter
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun PaymentCard(
    payment: EmployeePayment,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val dateFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Payment #${payment.payment_id}",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Gross wage: ${CurrencyFormatter.format(payment.amount)}",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )

                    val cashAdvance = payment.cash_advance_amount ?: 0.0
                    val liquidated = payment.liquidated_amount ?: 0.0
                    Text(
                        text = "Cash advance: ${CurrencyFormatter.format(cashAdvance)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Liquidated: ${CurrencyFormatter.format(liquidated)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    val netPay = payment.netPayAmount()
                    Text(
                        text = "Net pay: ${CurrencyFormatter.format(netPay)}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )

                    Text(
                        text = "Date paid: ${dateFormatter.format(Date(payment.date_paid))}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Signature: ${
                            if (payment.signature.isNotBlank() &&
                                (payment.signature.contains("/") || payment.signature.contains("+") || payment.signature.contains("="))
                            ) {
                                "Captured (image)"
                            } else {
                                payment.signature
                            }
                        }",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    payment.received_date?.let {
                        Text(
                            text = "Received: ${dateFormatter.format(Date(it))}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                Row {
                    IconButton(onClick = onEditClick) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit payment",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onDeleteClick) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete payment",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
} 