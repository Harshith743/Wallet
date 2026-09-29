package com.ivy.creditcards.pay

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Immutable
import java.util.Locale

@Immutable
data class UpiPaymentRequest(
    val payeeVpa: String?,
    val payeeName: String,
    val amount: Double,
    val note: String,
)

/**
 * Opens the UPI app chooser. With a payee id the payee, name, amount and note are
 * prefilled (`upi://pay?pa=…`); without one the bare `upi://pay` intent is used and the
 * user fills in the details. Does nothing when no UPI app is installed.
 */
fun Context.launchUpiPayment(request: UpiPaymentRequest) {
    val uri = Uri.Builder()
        .scheme("upi")
        .authority("pay")
        .apply {
            val vpa = request.payeeVpa?.trim().orEmpty()
            if (vpa.isNotEmpty()) {
                appendQueryParameter("pa", vpa)
                appendQueryParameter("pn", request.payeeName)
                appendQueryParameter("am", String.format(Locale.US, "%.2f", request.amount))
                appendQueryParameter("cu", "INR")
                if (request.note.isNotBlank()) appendQueryParameter("tn", request.note)
            }
        }
        .build()
    val intent = Intent(Intent.ACTION_VIEW, uri)
    try {
        startActivity(Intent.createChooser(intent, null))
    } catch (_: ActivityNotFoundException) {
        // No UPI app installed; the repayment is already recorded.
    }
}
