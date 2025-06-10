package com.example.countdowntimerapp

import android.app.*
import android.content.Context
import android.os.*
import android.widget.*
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import android.content.pm.PackageManager
import android.annotation.SuppressLint
import com.google.android.material.tabs.TabLayout
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import java.text.SimpleDateFormat
import java.util.*

/**
 * MainActivity: Main container for CountDownMaster app, implementing input for target date/time or duration,
 * visual updating timer, notification on completion, and easy control buttons.
 */
class MainActivity : AppCompatActivity() {
    private lateinit var tvCountdownTimer: TextView
    private lateinit var tabLayout: TabLayout
    private lateinit var inputDateTime: LinearLayout
    private lateinit var inputDuration: LinearLayout
    private lateinit var btnPickDate: Button
    private lateinit var btnPickTime: Button
    private lateinit var npHour: NumberPicker
    private lateinit var npMinute: NumberPicker
    private lateinit var npSecond: NumberPicker
    private lateinit var btnStart: Button
    private lateinit var btnReset: Button
    private lateinit var btnStop: Button

    private var countdownMode: CountdownMode = CountdownMode.TARGET // Target = by datetime, Duration = by duration

    private var pickedDate: Calendar? = null
    private var pickedTime: Calendar? = null
    private var targetTimeMillis: Long = 0L
    private var durationMillis: Long = 0L

    private var timer: CountDownTimer? = null
    private var isCountdownActive: Boolean = false

    private val notificationChannelId = "countdownmaster_channel"
    private val notificationId = 1701

    private val POST_NOTIFICATIONS_REQUEST_CODE = 1824

    private enum class CountdownMode { TARGET, DURATION }

    // PUBLIC_INTERFACE
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setupViews()
        setupTabSwitcher()
        setupPickers()
        setupControlButtons()
        setupNotificationChannel()
        showTabInput(CountdownMode.TARGET)
    }

    // PUBLIC_INTERFACE
    private fun setupViews() {
        tvCountdownTimer = findViewById(R.id.tvCountdownTimer)
        tabLayout = findViewById(R.id.inputTabLayout)
        inputDateTime = findViewById(R.id.inputDateTime)
        inputDuration = findViewById(R.id.inputDuration)
        btnPickDate = findViewById(R.id.btnPickDate)
        btnPickTime = findViewById(R.id.btnPickTime)
        npHour = findViewById(R.id.npHour)
        npMinute = findViewById(R.id.npMinute)
        npSecond = findViewById(R.id.npSecond)
        btnStart = findViewById(R.id.btnStart)
        btnReset = findViewById(R.id.btnReset)
        btnStop = findViewById(R.id.btnStop)
    }

    // PUBLIC_INTERFACE
    private fun setupTabSwitcher() {
        // Set the tabs for picking input method
        tabLayout.addTab(tabLayout.newTab().setText("Date/Time"))
        tabLayout.addTab(tabLayout.newTab().setText("Duration"))
        tabLayout.addOnTabSelectedListener(object: TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                if (tab?.position == 1) {
                    countdownMode = CountdownMode.DURATION
                    showTabInput(CountdownMode.DURATION)
                } else {
                    countdownMode = CountdownMode.TARGET
                    showTabInput(CountdownMode.TARGET)
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    // PUBLIC_INTERFACE
    private fun showTabInput(mode: CountdownMode) {
        if (mode == CountdownMode.TARGET) {
            inputDateTime.visibility = View.VISIBLE
            inputDuration.visibility = View.GONE
        } else {
            inputDateTime.visibility = View.GONE
            inputDuration.visibility = View.VISIBLE
        }
    }

    // PUBLIC_INTERFACE
    private fun setupPickers() {
        // Date picker
        btnPickDate.setOnClickListener {
            val now = Calendar.getInstance()
            val dlg = DatePickerDialog(this,
                { _, year, month, dayOfMonth ->
                    if (pickedDate == null) pickedDate = Calendar.getInstance()
                    pickedDate?.set(Calendar.YEAR, year)
                    pickedDate?.set(Calendar.MONTH, month)
                    pickedDate?.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                    btnPickDate.text = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(pickedDate?.time)
                },
                now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH))
            dlg.show()
        }

        // Time picker
        btnPickTime.setOnClickListener {
            val now = Calendar.getInstance()
            val dlg = TimePickerDialog(this,
                { _, hourOfDay, minute ->
                    if (pickedDate == null) pickedDate = Calendar.getInstance()
                    pickedDate?.set(Calendar.HOUR_OF_DAY, hourOfDay)
                    pickedDate?.set(Calendar.MINUTE, minute)
                    pickedDate?.set(Calendar.SECOND, 0)
                    pickedDate?.set(Calendar.MILLISECOND, 0)
                    btnPickTime.text = String.format("%02d:%02d", hourOfDay, minute)
                },
                now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), true)
            dlg.show()
        }

        // NumberPickers for duration
        npHour.minValue = 0; npHour.maxValue = 23; npHour.value = 0
        npMinute.minValue = 0; npMinute.maxValue = 59; npMinute.value = 0
        npSecond.minValue = 0; npSecond.maxValue = 59; npSecond.value = 0
    }

    // PUBLIC_INTERFACE
    private fun setupControlButtons() {
        btnStart.setOnClickListener { startCountdown() }
        btnReset.setOnClickListener { resetCountdown() }
        btnStop.setOnClickListener { stopCountdown() }
    }

    // PUBLIC_INTERFACE
    private fun startCountdown() {
        stopCountdown()
        if (countdownMode == CountdownMode.TARGET) {
            val cal = pickedDate ?: Calendar.getInstance()
            // If date not picked, treat as now.
            if (btnPickDate.text == "Pick Date" || btnPickTime.text == "Pick Time") {
                Toast.makeText(this, "Please select both date and time.", Toast.LENGTH_SHORT).show()
                return
            }
            targetTimeMillis = cal.timeInMillis
            durationMillis = cal.timeInMillis - Calendar.getInstance().timeInMillis
            if (durationMillis <= 0) {
                Toast.makeText(this, "Selected time is in the past!", Toast.LENGTH_SHORT).show()
                return
            }
        } else {
            val hours = npHour.value
            val minutes = npMinute.value
            val seconds = npSecond.value
            durationMillis = (hours * 3600 + minutes * 60 + seconds) * 1000L
            targetTimeMillis = System.currentTimeMillis() + durationMillis
            if (durationMillis == 0L) {
                Toast.makeText(this, "Please set a non-zero duration.", Toast.LENGTH_SHORT).show()
                return
            }
        }
        timer = object : CountDownTimer(durationMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                updateTimerDisplay(millisUntilFinished)
            }
            override fun onFinish() {
                updateTimerDisplay(0L)
                onCountdownFinished()
            }
        }
        timer?.start()
        isCountdownActive = true
        setControlsEnabled(false)
        Toast.makeText(this, "Countdown started", Toast.LENGTH_SHORT).show()
    }

    // PUBLIC_INTERFACE
    private fun resetCountdown() {
        stopCountdown()
        tvCountdownTimer.text = formatTime(0L)
        pickedDate = null
        btnPickDate.text = "Pick Date"
        btnPickTime.text = "Pick Time"
        npHour.value = 0; npMinute.value = 0; npSecond.value = 0
    }

    // PUBLIC_INTERFACE
    private fun stopCountdown() {
        timer?.cancel()
        timer = null
        isCountdownActive = false
        setControlsEnabled(true)
    }

    // PUBLIC_INTERFACE
    private fun setControlsEnabled(enable: Boolean) {
        btnPickDate.isEnabled = enable && countdownMode == CountdownMode.TARGET
        btnPickTime.isEnabled = enable && countdownMode == CountdownMode.TARGET
        npHour.isEnabled = enable && countdownMode == CountdownMode.DURATION
        npMinute.isEnabled = enable && countdownMode == CountdownMode.DURATION
        npSecond.isEnabled = enable && countdownMode == CountdownMode.DURATION
        btnStart.isEnabled = enable
        btnReset.isEnabled = enable
        btnStop.isEnabled = !enable
    }

    // PUBLIC_INTERFACE
    private fun updateTimerDisplay(millis: Long) {
        val text = formatTime(millis)
        tvCountdownTimer.text = text
    }

    // PUBLIC_INTERFACE
    private fun formatTime(millis: Long): String {
        val total = millis / 1000
        val hours = total / 3600
        val minutes = (total % 3600) / 60
        val seconds = total % 60
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

    // PUBLIC_INTERFACE
    private fun onCountdownFinished() {
        isCountdownActive = false
        setControlsEnabled(true)
        showCompletionNotification()
        Toast.makeText(this, "Countdown Complete!", Toast.LENGTH_LONG).show()
    }

    // PUBLIC_INTERFACE
    private fun setupNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "CountdownMaster Notification"
            val descriptionText = "Notifies when countdown completes"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(notificationChannelId, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    // PUBLIC_INTERFACE
    @SuppressLint("MissingPermission")
    private fun showCompletionNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), POST_NOTIFICATIONS_REQUEST_CODE)
                // Don’t show notification until permission is granted, will handle in onRequestPermissionsResult
                return
            }
        }
        val builder = NotificationCompat.Builder(this, notificationChannelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Countdown Complete!")
            .setContentText("Your countdown has finished.")
            .setColor(getColorCompat(R.color.accent))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
        with(NotificationManagerCompat.from(this)) {
            notify(notificationId, builder.build())
        }
    }

    // PUBLIC_INTERFACE
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == POST_NOTIFICATIONS_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showCompletionNotification()
            }
        }
    }

    // Helper to resolve getColor deprecation
    private fun getColorCompat(resId: Int): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getColor(resId)
        } else {
            @Suppress("DEPRECATION")
            resources.getColor(resId)
        }
    }
}
