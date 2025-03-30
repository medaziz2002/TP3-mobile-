package com.example.tp1application2

import android.app.TimePickerDialog
import android.content.Context
import android.widget.EditText
import java.util.Calendar

class TimePickerHelper(private val context: Context, private val editText: EditText) {

    fun setupTimePicker() {
        editText.setOnClickListener {
            showTimePicker()
        }
    }

    private fun showTimePicker() {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        TimePickerDialog(
            context,
            { _, selectedHour, selectedMinute ->
                val formattedTime = String.format("%02d:%02d", selectedHour, selectedMinute)
                editText.setText(formattedTime)
            },
            hour,
            minute,
            true
        ).show()
    }
}