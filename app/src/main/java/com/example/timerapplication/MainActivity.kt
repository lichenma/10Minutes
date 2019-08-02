package com.example.timerapplication

import android.os.Bundle
import android.os.CountDownTimer
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity;
import android.view.Menu
import android.view.MenuItem
import com.example.timerapplication.util.PrefUtil

import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {


    enum class TimerState{
        Stopped, Paused, Running
    }

    private lateinit var timer: CountDownTimer
    private var timerLengthSeconds: Long = 0L
    private var timerState: TimerState = TimerState.Stopped

    private var secondsRemaining: Long = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
        supportActionBar?.setIcon(R.drawable.ic_timer)
        supportActionBar?.title = "             Timer"

        fab_start.setOnClickListener{ v ->
            startTimer()
            timerState = TimerState.Running
            updateButtons()
        }

        fab_pause.setOnClickListener { v ->
            timer.cancel()
            timerState = TimerState.Paused
            updateButtons()
        }

        fab_start.setOnClickListener { v ->
            timer.cancel()
            onTimerFinished()
        }
    }

    // Overriding lifecycle functions
    override fun onResume() {
        super.onResume()

        initTimer()

        //TODO: Remove background timer, hide notification
    }
    // lifecycle function which is called right before the activity goes into background
    override fun onPause() {
        super.onPause()
        if (timerState == TimerState.Running){
            timer.cancel()
            // TODO: Start background timer and show notificiation
        } else if (timerState == TimerState.Paused){
            // TODO: Show Notification
        }
        // if we save variables to preferences, those variables are not wiped when app restarts - they are persistent and saved to the drive
        PrefUtil.setPreviousTimerLengthSeconds(timerLengthSeconds, this)
        PrefUtil.setSecondsRemaining(secondsRemaining, this)
        PrefUtil.setTimerState(timerState, this)
    }

    private fun initTimer(){
        timerState = PrefUtil.getTimerState(this)
        if (timerState == TimerState.Stopped){
            setNewTimerLength()
        } else {
            setPreviousTimerLength()
        }

        secondsRemaining = if (timerState == TimerState.Running || timerState == TimerState.Paused){
            PrefUtil.getSecondsRemaining(this)
        } else {
            // set the value to be the full timer length value which is timerlengthseconds
            timerLengthSeconds
        }
        //TODO: change seconds remaining to where the background timer stopped
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
