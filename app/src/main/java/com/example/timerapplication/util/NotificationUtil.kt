package com.example.timerapplication.util

import android.app.PendingIntent
import android.app.TaskStackBuilder
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import androidx.core.app.NotificationCompat
import com.example.timerapplication.MainActivity
import com.example.timerapplication.R
import com.example.timerapplication.TimerNotificationActionReceiver

class NotificationUtil {
    // creating the companion object allows us to call this function
    // from other classes without needing to create an instance of the
    // NotificationUtil class
    companion object{
        // required when working with OREO api 26
        private const val CHANNEL_ID_TIMER = "menu_timer"
        private const val CHANNEL_NAME_TIMER = "Timer App Timer"
        private const val TIMER_ID = 0

        fun showTimerExpired(context: Context){
            // we want to be control timer from notification
            // allow starting the timer from notification
            val startIntent = Intent(context, TimerNotificationActionReceiver::class.java)
            // string
            startIntent.action = AppConstants.ACTION_START
            val startPendingIntent = PendingIntent.getBroadcast(context, 0, startIntent, PendingIntent.FLAG_UPDATE_CURRENT)
            val nBuilder = getBasicNotificationBuilder(context, CHANNEL_ID_TIMER, true)
            nBuilder.setContentTitle("Timer Expired!")
                .setContentText("Start again?")
                .setContentIntent(getPendingIntentWithStack(context, MainActivity::class.java))
                // calls the pending intent when clicked
                .addAction(R.drawable.ic_play, "Start", startPendingIntent)

            val nManager = 
        }

        private fun getBasicNotificationBuilder(context: Context, channelId: String, playSound: Boolean): NotificationCompat.Builder {
            val notificationSound: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            // auto cancel ensures when user clicks on notification it will be dismissed
            val nBuilder = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_timer)
                .setAutoCancel(true)
                .setDefaults(0)
            if (playSound) nBuilder.setSound(notificationSound)
            return nBuilder
        }

        // activity two layers deep - when user clicks on notification, it
        // will take user to that activity - when back button is clicked
        // it takes user to activity one layer lower

        // just like if the activity that was opened before was not opened
        private fun <T> getPendingIntentWithStack(context: Context, javaClass: Class<T>):PendingIntent{
            val resultIntent = Intent(context, javaClass)
            // if activity we are calling is already open we are not creating it again
            resultIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP

            val stackBuilder = TaskStackBuilder.create(context)
            stackBuilder.addParentStack(javaClass)
            stackBuilder.addNextIntent(resultIntent)

            return stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT)
        }
    }
}