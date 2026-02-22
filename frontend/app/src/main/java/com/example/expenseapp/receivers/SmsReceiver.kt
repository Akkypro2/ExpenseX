package com.example.expenseapp.receivers

import android.R
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.expenseapp.MainActivity

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val msgs = Telephony.Sms.Intents.getMessagesFromIntent(intent)

            for (msg in msgs) {
                val sender = msg.originatingAddress ?: ""
                val messageBody = msg.messageBody.lowercase()

                // 1. SECURITY: Only accept SMS from Banks/Wallets
                if (isBankSender(sender)) {

                    // 2. PARSE AMOUNT (The Fixed Regex that handles 'Rs: 100', 'Rs. 100', 'INR 100')
                    val amountRegex = Regex("(?i)(rs\\.?|inr)[\\s:.-]*(\\d+(?:,\\d+)*(?:\\.\\d{2})?)")
                    val match = amountRegex.find(messageBody)

                    if (match != null) {
                        // Clean up the amount (remove commas)
                        val amount = match.groupValues[2].replace(",", "")

                        // 3. DETERMINE TYPE (Credit or Debit)
                        val isCredit = messageBody.contains("credited") ||
                                messageBody.contains("received") ||
                                messageBody.contains("deposited") ||
                                messageBody.contains("refund")

                        val type = if (isCredit) "Credit" else "Debit"

                        // 4. SHOW NOTIFICATION
                        showNotification(context, amount, type)
                    }
                }
            }
        }
    }

    // Helper: Only allow real bank SMS headers (blocks spam/personal texts)
    private fun isBankSender(sender: String): Boolean {
        val s = sender.uppercase()
        return s.contains("HDFC") || s.contains("SBI") || s.contains("ICICI") ||
                s.contains("PAYTM") || s.contains("UNION") || s.contains("UPI") || s.contains("BOI") ||
                s.contains("AXIS") || s.contains("BANK") || s.contains("WALLET")
    }

    private fun showNotification(context: Context, amount: String, type: String) {
        val channelId = "expense_channel"

        // Create Channel (Android 8+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Expense Alerts",
                NotificationManager.IMPORTANCE_HIGH
            )
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        // Prepare Intent to open App
        val activityIntent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            putExtra("auto_add_amount", amount)
            putExtra("sms_type", type)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            activityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Notification Content
        val title = if (type == "Credit") "Money Received! 💰" else "New Expense Detected 💸"
        val message = if (type == "Credit") "Tap to add +₹$amount to income" else "Tap to add -₹$amount to expenses"

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_input_add) // Or R.drawable.your_logo
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        // Show It
        try {
            NotificationManagerCompat.from(context).notify(1001, builder.build())
        } catch (e: SecurityException) {
            e.printStackTrace()
            // Silent fail if permission revoked (User won't be annoyed by crashes)
        }
    }
}