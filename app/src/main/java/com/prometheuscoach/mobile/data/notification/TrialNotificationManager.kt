package com.prometheuscoach.mobile.data.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.*
import com.prometheuscoach.mobile.MainActivity
import com.prometheuscoach.mobile.R
import com.prometheuscoach.mobile.data.model.SubscriptionInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "TrialNotificationManager"

// Notification channel
const val TRIAL_NOTIFICATION_CHANNEL_ID = "trial_notifications"
const val TRIAL_NOTIFICATION_CHANNEL_NAME = "Trial Reminders"

// Worker tags
const val TRIAL_REMINDER_WORK_TAG = "trial_reminder"
const val TRIAL_REMINDER_3_DAYS_TAG = "trial_reminder_3_days"
const val TRIAL_REMINDER_1_DAY_TAG = "trial_reminder_1_day"
const val TRIAL_EXPIRED_TAG = "trial_expired"

// Notification IDs
const val NOTIFICATION_ID_3_DAYS = 1001
const val NOTIFICATION_ID_1_DAY = 1002
const val NOTIFICATION_ID_EXPIRED = 1003

/**
 * Manages trial-related notifications.
 * Schedules reminders before trial expires and notifies when trial has ended.
 */
@Singleton
class TrialNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val workManager = WorkManager.getInstance(context)

    init {
        createNotificationChannel()
    }

    /**
     * Create the notification channel (required for Android 8+)
     */
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            TRIAL_NOTIFICATION_CHANNEL_ID,
            TRIAL_NOTIFICATION_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Reminders about your trial subscription"
            enableLights(true)
            enableVibration(true)
        }

        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    /**
     * Schedule trial reminder notifications based on subscription info.
     * This should be called after fetching subscription data.
     */
    fun scheduleTrialReminders(subscriptionInfo: SubscriptionInfo) {
        // Cancel any existing reminders first
        cancelAllReminders()

        val subscription = subscriptionInfo.subscription ?: return

        // Only schedule if in trial and trial end is in the future
        if (!subscriptionInfo.isTrialing) {
            Log.d(TAG, "Not in trial, skipping notification scheduling")
            return
        }

        val trialEndStr = subscription.trialEnd ?: return

        try {
            val trialEnd = Instant.parse(trialEndStr)
            val now = Instant.now()

            if (trialEnd.isBefore(now)) {
                Log.d(TAG, "Trial already ended, not scheduling reminders")
                return
            }

            val daysUntilExpiry = ChronoUnit.DAYS.between(now, trialEnd)
            Log.d(TAG, "Trial ends in $daysUntilExpiry days")

            // Schedule 3-day reminder
            if (daysUntilExpiry >= 3) {
                val delayFor3DayReminder = ChronoUnit.MILLIS.between(now, trialEnd.minus(3, ChronoUnit.DAYS))
                scheduleReminder(
                    tag = TRIAL_REMINDER_3_DAYS_TAG,
                    delayMillis = delayFor3DayReminder,
                    notificationId = NOTIFICATION_ID_3_DAYS,
                    title = "Trial ending soon",
                    message = "Your free trial expires in 3 days. Subscribe now to keep your coaching features!"
                )
            }

            // Schedule 1-day reminder
            if (daysUntilExpiry >= 1) {
                val delayFor1DayReminder = ChronoUnit.MILLIS.between(now, trialEnd.minus(1, ChronoUnit.DAYS))
                scheduleReminder(
                    tag = TRIAL_REMINDER_1_DAY_TAG,
                    delayMillis = delayFor1DayReminder,
                    notificationId = NOTIFICATION_ID_1_DAY,
                    title = "Trial expires tomorrow!",
                    message = "Your free trial ends tomorrow. Don't lose access to your clients - subscribe now!"
                )
            }

            // Schedule trial expired notification
            val delayForExpired = ChronoUnit.MILLIS.between(now, trialEnd)
            scheduleReminder(
                tag = TRIAL_EXPIRED_TAG,
                delayMillis = delayForExpired,
                notificationId = NOTIFICATION_ID_EXPIRED,
                title = "Trial has ended",
                message = "Your free trial has expired. Subscribe to continue using Prometheus Coach."
            )

            Log.d(TAG, "Scheduled trial reminder notifications")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule trial reminders", e)
        }
    }

    /**
     * Schedule a single reminder notification
     */
    private fun scheduleReminder(
        tag: String,
        delayMillis: Long,
        notificationId: Int,
        title: String,
        message: String
    ) {
        if (delayMillis <= 0) {
            Log.d(TAG, "Delay is <= 0 for $tag, skipping")
            return
        }

        val inputData = workDataOf(
            TrialReminderWorker.KEY_NOTIFICATION_ID to notificationId,
            TrialReminderWorker.KEY_TITLE to title,
            TrialReminderWorker.KEY_MESSAGE to message
        )

        val workRequest = OneTimeWorkRequestBuilder<TrialReminderWorker>()
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .setInputData(inputData)
            .addTag(TRIAL_REMINDER_WORK_TAG)
            .addTag(tag)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                    .build()
            )
            .build()

        workManager.enqueueUniqueWork(
            tag,
            ExistingWorkPolicy.REPLACE,
            workRequest
        )

        Log.d(TAG, "Scheduled reminder '$tag' in ${delayMillis / 1000 / 60} minutes")
    }

    /**
     * Cancel all scheduled trial reminders
     */
    fun cancelAllReminders() {
        workManager.cancelAllWorkByTag(TRIAL_REMINDER_WORK_TAG)
        Log.d(TAG, "Cancelled all trial reminders")
    }

    /**
     * Check if notification permission is granted (Android 13+)
     */
    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Permission not required before Android 13
        }
    }

    /**
     * Show a notification immediately (for testing or immediate alerts)
     */
    fun showNotification(
        notificationId: Int,
        title: String,
        message: String
    ) {
        if (!hasNotificationPermission()) {
            Log.w(TAG, "Notification permission not granted")
            return
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, TRIAL_NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
            Log.d(TAG, "Showed notification: $title")
        } catch (e: SecurityException) {
            Log.e(TAG, "Failed to show notification - permission denied", e)
        }
    }
}
