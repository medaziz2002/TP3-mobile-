package com.example.tp1application2

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter



class TimeSlotPagerAdapter(
    fragment: Fragment,
    private var existingSlots: List<TimeSlotFragment.TimeSlotData> = emptyList()
) : FragmentStateAdapter(fragment) {

    // Calculer les créneaux restants à afficher (toujours 4 créneaux max au total)
    private val remainingSlots: List<TimeSlotFragment.TimeSlotData>
        get() {
            val totalPossibleSlots = 4
            val remainingCount = totalPossibleSlots - existingSlots.size
            return if (remainingCount > 0) {
                List(remainingCount) { TimeSlotFragment.TimeSlotData("", "", "") }
            } else {
                emptyList()
            }
        }

    override fun getItemCount(): Int = 1

    override fun createFragment(position: Int): Fragment {
        val slotNumber = existingSlots.size + position + 1
        return TimeSlotFragment.newInstance(slotNumber, remainingSlots[position])
    }

    fun updateExistingSlots(slots: List<TimeSlotFragment.TimeSlotData>) {
        this.existingSlots = slots
        notifyDataSetChanged()
    }
}

