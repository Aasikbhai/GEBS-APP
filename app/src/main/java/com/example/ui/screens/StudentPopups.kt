package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.audio.SmsIntentHelper
import com.example.data.local.FeeItemEntity
import com.example.data.local.FeeStatus
import com.example.ui.components.GlassPillChip
import com.example.ui.components.GlassPopupDialog
import com.example.ui.components.GlassPrimaryButton
import com.example.ui.components.GlassTextField
import com.example.ui.components.StatusBadge
import com.example.ui.theme.LocalGlassPalette

@Composable
fun FeeDetailGlassPopup(
    fee: FeeItemEntity,
    onPayFee: (FeeItemEntity) -> Unit,
    onPlayBillSms: (FeeItemEntity) -> Unit,
    onDismiss: () -> Unit
) {
    val glass = LocalGlassPalette.current
    val context = LocalContext.current
    val remaining = (fee.amount - fee.paidAmount).coerceAtLeast(0.0)
    val statusColor = when (fee.status) {
        FeeStatus.PAID.name -> glass.success
        FeeStatus.PARTIAL.name -> glass.warning
        else -> glass.danger
    }

    GlassPopupDialog(
        title = fee.feeTitle,
        subtitle = "${fee.studentName} • ${fee.className} (Roll ${fee.rollNumber})",
        onDismiss = onDismiss
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Status",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            StatusBadge(text = fee.status, color = statusColor)
        }
        Spacer(modifier = Modifier.height(12.dp))
        DetailRow(icon = Icons.Rounded.ReceiptLong, label = "Total Fee Amount", value = "Rs ${fee.amount.toInt()}")
        DetailRow(icon = Icons.Rounded.CheckCircle, label = "Paid So Far", value = "Rs ${fee.paidAmount.toInt()}")
        DetailRow(icon = Icons.Rounded.Sms, label = "Pending Dues", value = "Rs ${remaining.toInt()} (Due ${fee.dueDate})")

        Spacer(modifier = Modifier.height(8.dp))

        if (remaining > 0) {
            GlassPrimaryButton(
                text = "Pay Rs ${remaining.toInt()} & Get Bill SMS",
                onClick = {
                    onPayFee(fee)
                    onDismiss()
                },
                containerColor = glass.success,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("confirm_pay_fee_button")
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        GlassPrimaryButton(
            text = "Listen to Bill Sound SMS",
            onClick = { onPlayBillSms(fee) },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(10.dp))

        OutlinedButton(
            onClick = {
                val smsText = "CampusPulse Bill Info: ${fee.feeTitle} for ${fee.studentName} (Roll ${fee.rollNumber}). Total Rs ${fee.amount.toInt()}, Pending Rs ${remaining.toInt()}, Due ${fee.dueDate}."
                SmsIntentHelper.launchDeviceSmsComposer(context, "+91 98765 43210", smsText)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Rounded.Sms, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Share via Device SMS App", fontWeight = FontWeight.SemiBold)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NewParentComplaintPopup(
    studentName: String,
    onSubmit: (category: String, message: String, alsoOpenSmsApp: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val categories = listOf(
        "Student Activity",
        "Homework & Study",
        "Bus & Transport",
        "Fee & Bill Query",
        "Safety & Hygiene"
    )
    var selectedCategory by rememberSaveable { mutableStateOf(categories.first()) }
    var message by rememberSaveable { mutableStateOf("") }

    GlassPopupDialog(
        title = "Parent Complain Box",
        subtitle = "Send a concern about $studentName directly to Teacher",
        onDismiss = onDismiss
    ) {
        Text(
            text = "Select Category",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categories.forEach { cat ->
                GlassPillChip(
                    label = cat,
                    selected = selectedCategory == cat,
                    onClick = { selectedCategory = cat }
                )
            }
        }
        Spacer(modifier = Modifier.height(14.dp))
        GlassTextField(
            value = message,
            onValueChange = { message = it },
            label = "Describe student activity or school concern",
            singleLine = false,
            minLines = 4,
            modifier = Modifier.testTag("parent_complaint_input")
        )
        Spacer(modifier = Modifier.height(16.dp))
        GlassPrimaryButton(
            text = "Submit Complaint & Notify Teacher",
            enabled = message.trim().length >= 5,
            onClick = {
                onSubmit(selectedCategory, message, false)
                onDismiss()
            },
            leadingIcon = {
                Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = null)
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("submit_parent_complaint_button")
        )
        Spacer(modifier = Modifier.height(10.dp))
        OutlinedButton(
            enabled = message.trim().length >= 5,
            onClick = {
                onSubmit(selectedCategory, message, true)
                onDismiss()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Rounded.Sms, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Submit & Open Device SMS App")
        }
    }
}
