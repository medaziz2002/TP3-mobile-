package com.example.tp1application2

import android.app.TimePickerDialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import java.util.Calendar

class TimeSlotFragment : Fragment() {
    private lateinit var editStart: EditText
    private lateinit var editEnd: EditText
    private lateinit var editEvent: EditText
    private lateinit var textTitle: TextView

    companion object {
        private const val ARG_SLOT_NUMBER = "slotNumber"
        private const val ARG_START_TIME = "startTime"
        private const val ARG_END_TIME = "endTime"
        private const val ARG_ACTIVITY = "activity"

        fun newInstance(slotNumber: Int, data: TimeSlotData? = null): TimeSlotFragment {
            return TimeSlotFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_SLOT_NUMBER, slotNumber)
                    data?.let {
                        putString(ARG_START_TIME, it.startTime)
                        putString(ARG_END_TIME, it.endTime)
                        putString(ARG_ACTIVITY, it.activity)
                    }
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.item_time_slot, container, false)

        // Initialisation des vues
        editStart = view.findViewById(R.id.editStart)
        editEnd = view.findViewById(R.id.editEnd)
        editEvent = view.findViewById(R.id.editEvent)
        textTitle = view.findViewById(R.id.textTitle)

        // Récupération des arguments
        val slotNumber = arguments?.getInt(ARG_SLOT_NUMBER, 1) ?: 1
        textTitle.text = "Créneau $slotNumber"

        // Remplissage des champs si données existantes
        arguments?.let {
            editStart.setText(it.getString(ARG_START_TIME, ""))
            editEnd.setText(it.getString(ARG_END_TIME, ""))
            editEvent.setText(it.getString(ARG_ACTIVITY, ""))
        }

        // Configuration des time pickers
        TimePickerHelper(requireContext(), editStart).setupTimePicker()
        TimePickerHelper(requireContext(), editEnd).setupTimePicker()

        return view
    }

    fun getTimeSlotData(): TimeSlotData {
        return TimeSlotData(
            startTime = editStart.text.toString(),
            endTime = editEnd.text.toString(),
            activity = editEvent.text.toString()
        )
    }

    data class TimeSlotData(
        val startTime: String,
        val endTime: String,
        val activity: String
    )

    class TimePickerHelper(private val context: Context, private val editText: EditText) {
        fun setupTimePicker() {
            editText.setOnClickListener { showTimePicker() }
            editText.setOnFocusChangeListener { _, hasFocus -> if (hasFocus) showTimePicker() }
        }

        private fun showTimePicker() {
            val calendar = Calendar.getInstance()
            TimePickerDialog(
                context,
                { _, hour, minute ->
                    val formattedTime = String.format("%02d:%02d", hour, minute)
                    editText.setText(formattedTime)
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                true
            ).show()
        }
    }
}