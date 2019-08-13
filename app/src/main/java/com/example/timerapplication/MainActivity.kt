package com.example.timerapplication

import android.annotation.TargetApi
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.os.PowerManager
import com.google.android.material.snackbar.Snackbar
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.example.timerapplication.util.NotificationUtil
import com.example.timerapplication.util.PrefUtil
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import java.util.*

class MainActivity : AppCompatActivity() {

    companion object {
        fun setAlarm(context: Context, nowSeconds: Long, secondsRemaining: Long): Long{
            val wakeUpTime = (nowSeconds + secondsRemaining)*1000
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, TimerExpiredReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0)
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, wakeUpTime, pendingIntent)
            PrefUtil.setAlarmSetTime(nowSeconds, context)
            return wakeUpTime
        }

        fun removeAlarm(context: Context){
            val intent = Intent(context, TimerExpiredReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0)
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.cancel(pendingIntent)
            PrefUtil.setAlarmSetTime(0, context)
        }

        // Immutable property
        val nowSeconds: Long
            get() = Calendar.getInstance().timeInMillis / 1000
    }
    enum class TimerState{
        Stopped, Paused, Running, Done
    }

    private lateinit var timer: CountDownTimer
    private var timerLengthSeconds: Long = 0
    private var timerState = TimerState.Running
    private var secondsRemaining: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
        supportActionBar?.setIcon(R.drawable.ic_timer)
        supportActionBar?.title = "      Timer"
        /*
        fab_start.setOnClickListener{v ->
            startTimer()
            timerState =  TimerState.Running
            updateButtons()
        }

        fab_pause.setOnClickListener { v ->
            timer.cancel()
            timerState = TimerState.Paused
            updateButtons()
        }

        fab_stop.setOnClickListener { v ->
            timer.cancel()
            onTimerFinished()
        } */
        PrefUtil.setTimerState(TimerState.Running, this)
        initTimer()
    }

    override fun onResume() {
        super.onResume()

        initTimer()

        removeAlarm(this)
        //NotificationUtil.hideTimerNotification(this)

        // TODO: Implementation for auto-countdown
        if (timerState != TimerState.Done){
            startTimer()
            timerState = TimerState.Running
        }
    }
    @TargetApi(20)
    override fun onPause() {
        super.onPause()
        timer.cancel()
        if (timerState == TimerState.Running){
            val wakeUpTime = setAlarm(this, nowSeconds, secondsRemaining)
            //NotificationUtil.showTimerRunning(this, wakeUpTime)
            var pm = this.applicationContext.getSystemService(Context.POWER_SERVICE) as PowerManager
            if (pm.isInteractive){
                // This means we left the app with time to spare - we want to remove the streak
                PrefUtil.setStreak(0, this)
                PrefUtil.setTimerState(TimerState.Stopped, this)
            } else{
                // This means the user probably put the app to sleep we want to allow this action
                PrefUtil.setPreviousTimerLengthSeconds(timerLengthSeconds, this)
                PrefUtil.setSecondsRemaining(secondsRemaining, this)
                PrefUtil.setTimerState(timerState, this)
            }

        } else if (timerState == TimerState.Paused){
            //NotificationUtil.showTimerPaused(this)
        } else if (timerState == TimerState.Stopped){
            //PrefUtil.setPreviousTimerLengthSeconds(timerLengthSeconds, this)
            //PrefUtil.setSecondsRemaining(secondsRemaining, this)
            //PrefUtil.setTimerState(timerState, this)
        } else if (timerState == TimerState.Done) {
            timerState = TimerState.Stopped
            PrefUtil.setTimerState(timerState, this)
        }
        // if we save variables to perferences, those variables are not wiped when
        // the app restarts - they are persistent and saved to the drive
    }

    private fun initTimer(){
        timerState = PrefUtil.getTimerState(this)

        //we don't want to change the length of the timer which is already running
        //if the length was changed in settings while it was backgrounded
        if (timerState == TimerState.Stopped || timerState == TimerState.Done)
            setNewTimerLength()
        else
            // set the value to be the full timer length value which is timerlengthseconds
            setPreviousTimerLength()

        secondsRemaining = if (timerState == TimerState.Running || timerState == TimerState.Paused)
            PrefUtil.getSecondsRemaining(this)
        else
            timerLengthSeconds

        val alarmSetTime = PrefUtil.getAlarmSetTime(this)
        // alarm was set if the value is greater than 0
        if (alarmSetTime > 0)
            // gives us the amount of time the app was running in the background
            secondsRemaining -= nowSeconds - alarmSetTime

        if (secondsRemaining <= 0 || timerState == TimerState.Done) {
            // finished in the background
            onTimerFinished()
        } else if (timerState == TimerState.Running)
            //startTimer()

        //updateButtons()
        updateCountdownUI()
    }
    @TargetApi(20)
    private fun onTimerFinished(){
        timerState = TimerState.Done
        // means user has stayed for 10 mins and we can increment the counter
        var streak = PrefUtil.getStreak(this)
        streak += 1
        PrefUtil.setStreak(streak, this)

        //set the length of the timer to be the one set in SettingsActivity
        //if the length was changed when the timer was running
        setNewTimerLength()

        progress_countdown.progress = 0

        PrefUtil.setSecondsRemaining(timerLengthSeconds, this)
        secondsRemaining = timerLengthSeconds

        //updateButtons()
        updateCountdownUI()

    }

    private fun startTimer(){
        timerState = TimerState.Running

        timer = object : CountDownTimer(secondsRemaining * 1000, 1000) {
            override fun onFinish() = onTimerFinished()

            override fun onTick(millisUntilFinished: Long) {
                secondsRemaining = millisUntilFinished / 1000
                updateCountdownUI()
            }
        }.start()
    }

    private fun setNewTimerLength(){
        val lengthInMinutes = PrefUtil.getTimerLength(this)
        timerLengthSeconds = (lengthInMinutes * 60L)
        progress_countdown.max = timerLengthSeconds.toInt()
    }

    private fun setPreviousTimerLength(){
        timerLengthSeconds = PrefUtil.getPreviousTimerLengthSeconds(this)
        progress_countdown.max = timerLengthSeconds.toInt()
    }

    private fun updateCountdownUI(){
        val minutesUntilFinished = secondsRemaining / 60
        val secondsInMinuteUntilFinished = secondsRemaining - minutesUntilFinished * 60
        val secondsStr = secondsInMinuteUntilFinished.toString()
        textView_countdown.text = "$minutesUntilFinished:${if (secondsStr.length == 2) secondsStr else "0" + secondsStr}"
        progress_countdown.progress = (timerLengthSeconds - secondsRemaining).toInt()
        var streak = PrefUtil.getStreak(this)
        streak_count.text = "Current Streak is: ${streak}"

    }

    private fun updateButtons(){
        when (timerState) {
            TimerState.Running ->{
                fab_start.isEnabled = false
                fab_pause.isEnabled = true
                fab_stop.isEnabled = true
            }
            TimerState.Stopped -> {
                fab_start.isEnabled = true
                fab_pause.isEnabled = false
                fab_stop.isEnabled = false
            }
            TimerState.Paused -> {
                fab_start.isEnabled = true
                fab_pause.isEnabled = false
                fab_stop.isEnabled = true
            }
        }
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }
}
