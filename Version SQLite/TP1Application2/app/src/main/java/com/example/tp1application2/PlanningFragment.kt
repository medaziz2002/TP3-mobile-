package com.example.tp1application2

import LoginFragment
import android.app.AlertDialog
import android.app.TimePickerDialog
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat.recreate
import androidx.core.content.ContextCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import java.text.SimpleDateFormat
import java.util.*

class PlanningFragment : Fragment(), NavigationView.OnNavigationItemSelectedListener {

    // Views
    private lateinit var calendarView: CalendarView
    private lateinit var recyclerViewEvents: RecyclerView
    private lateinit var btnAddEvent: FloatingActionButton
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var btnMenu: Button
    private lateinit var mainLayout: LinearLayout

    // Database
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var database: SQLiteDatabase

    // Data
    private var selectedDate: String = ""
    private var eventObjects = mutableListOf<Event>()
    private var eventStrings = mutableListOf<String>()
    private lateinit var eventAdapter: EventAdapter
    private var currentUserId: Long = -1

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_planning, container, false)
        initializeViews(view)
        setupDatabase()
        setupRecyclerView()
        setupEventListeners()
        setupNavigation()
        loadEvents()
        return view
    }

    private fun initializeViews(view: View) {
        calendarView = view.findViewById(R.id.calendarView)
        recyclerViewEvents = view.findViewById(R.id.recyclerViewEvents)
        btnAddEvent = view.findViewById(R.id.btnAddEvent)
        mainLayout = view.findViewById(R.id.mainLayout)
        drawerLayout = view.findViewById(R.id.drawer_layout)
        navigationView = view.findViewById(R.id.navigation_view)
        btnMenu = view.findViewById(R.id.btn_menu)

        currentUserId = arguments?.getLong("USER_ID", -1) ?: -1

        if (currentUserId == -1L) {
            Toast.makeText(requireContext(), "Authentification requise", Toast.LENGTH_SHORT).show()
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, LoginFragment())
                .commit()
        }
    }

    private fun setupNavigation() {
        navigationView.setNavigationItemSelectedListener(this)
        btnMenu.setOnClickListener { drawerLayout.openDrawer(navigationView) }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_logout -> logout()
            R.id.profil -> profil()
            R.id.menu_lang_fr -> changeLanguage("fr")
            R.id.menu_lang_en -> changeLanguage("en")
        }
        drawerLayout.closeDrawers()
        return true
    }

    private fun logout() {
        requireActivity().getSharedPreferences("UserSession", Context.MODE_PRIVATE).edit().clear()
            .apply()
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, LoginFragment())
            .commit()
    }

    private fun getUserIdFromSession(): Long {
        return requireContext().getSharedPreferences("UserSession", Context.MODE_PRIVATE)
            .getLong("USER_ID", -1).takeIf { it != -1L }
            ?: throw IllegalStateException("Session invalide")
    }

    private fun profil() {
        Log.d("test test","je suis dans profil s'il vous plait ")
        try {
            val userId = getUserIdFromSession()
            val displayFragment = DisplayFragment.newInstance(dbHelper.getUserById(userId))

            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, displayFragment)
                .addToBackStack(null)
                .commit()
        } catch (e: IllegalStateException) {
            Toast.makeText(requireContext(), "Session invalide", Toast.LENGTH_SHORT).show()
            // Rediriger vers l'écran de connexion
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, LoginFragment())
                .commit()
        }
    }




    private fun changeLanguage(lang: String) {
        val config = resources.configuration
        val locale = Locale(lang)
        Locale.setDefault(locale)
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)
        recreate(requireActivity())
    }

    private fun setupDatabase() {
        dbHelper = DatabaseHelper(requireContext())
        database = dbHelper.writableDatabase
        selectedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    private fun setupRecyclerView() {
        recyclerViewEvents.layoutManager = LinearLayoutManager(requireContext())
        eventAdapter = EventAdapter(
            eventStrings,
            { eventString, position -> onEditEvent(position) },
            { position -> onDeleteEvent(position) }
        )
        recyclerViewEvents.adapter = eventAdapter
    }

    private fun setupEventListeners() {
        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            selectedDate = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
            loadEvents()
        }

        btnAddEvent.setOnClickListener { showAddEventDialog() }
    }

    private fun showAddEventDialog() {
        val builder = AlertDialog.Builder(requireContext())
        val inflater = layoutInflater
        val dialogView = inflater.inflate(R.layout.dialog_add_event_pager, null)

        val dialogView1 = inflater.inflate(R.layout.item_time_slot, null)

        val editEnd = dialogView1.findViewById<EditText>(R.id.editEnd)
        val editStart = dialogView1.findViewById<EditText>(R.id.editStart)
        val description = dialogView1.findViewById<EditText>(R.id.editEvent)

        val viewPager = dialogView.findViewById<ViewPager2>(R.id.viewPager)
        val pageIndicators = dialogView.findViewById<LinearLayout>(R.id.pageIndicators)
        val btnAdd = dialogView.findViewById<Button>(R.id.btnAdd)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)

        // Récupérer les créneaux existants
        val existingEvents = dbHelper.getEventsByUserAndDate(currentUserId, selectedDate)
        val existingSlots = existingEvents.map {
            TimeSlotFragment.TimeSlotData(it.startTime, it.endTime, it.description)
        }

        // Configuration du ViewPager
        val adapter = TimeSlotPagerAdapter(this).apply {
            updateExistingSlots(existingSlots)
        }
        viewPager.adapter = adapter
        viewPager.offscreenPageLimit = 1

        // Configuration des indicateurs de page
        if (adapter.itemCount > 1) {
            setupPageIndicators(pageIndicators, adapter.itemCount)
            viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    updatePageIndicators(pageIndicators, position)
                }
            })
        } else {
            pageIndicators.visibility = View.GONE
        }

        builder.setView(dialogView)
        val dialog = builder.create()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            mainLayout.setRenderEffect(RenderEffect.createBlurEffect(20f, 20f, Shader.TileMode.CLAMP))
        }

        btnAdd.setOnClickListener {
            val currentFragment = childFragmentManager.findFragmentByTag("f${viewPager.currentItem}") as? TimeSlotFragment
            val slotData = currentFragment?.getTimeSlotData()

            when {
                slotData == null -> {
                    Toast.makeText(requireContext(), getString(R.string.slot_load_error), Toast.LENGTH_SHORT).show()
                }
                slotData.startTime.isEmpty() -> {
                    showError(currentFragment.requireView().findViewById(R.id.editStart), getString(R.string.start_time_required))
                }
                slotData.endTime.isEmpty() -> {
                    showError(currentFragment.requireView().findViewById(R.id.editEnd), getString(R.string.end_time_required))
                }
                slotData.startTime >= slotData.endTime -> {
                    showError(currentFragment.requireView().findViewById(R.id.editEnd), getString(R.string.end_time_after_start))
                }
                slotData.activity.isEmpty() -> {
                    showError(currentFragment.requireView().findViewById(R.id.editEvent), getString(R.string.description_required))
                }
                else -> {
                    saveEventToDatabase(
                        startTime = slotData.startTime,
                        endTime = slotData.endTime,
                        description = slotData.activity
                    )
                    dialog.dismiss()
                }
            }
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.setOnDismissListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                mainLayout.setRenderEffect(null)
            }
        }

        dialog.show()
    }


    private fun validateEventInputs(
        startTime: String,
        endTime: String,
        description: String,
        excludeEventId: Long? = null
    ): Boolean {
        return when {
            startTime.isEmpty() -> {
                Toast.makeText(requireContext(), "L'heure de début est requise", Toast.LENGTH_SHORT)
                    .show()
                false
            }

            endTime.isEmpty() -> {
                Toast.makeText(requireContext(), "L'heure de fin est requise", Toast.LENGTH_SHORT)
                    .show()
                false
            }

            startTime >= endTime -> {
                Toast.makeText(
                    requireContext(),
                    "L'heure de fin doit être après l'heure de début",
                    Toast.LENGTH_SHORT
                ).show()
                false
            }

            description.isEmpty() -> {
                Toast.makeText(requireContext(), "La description est requise", Toast.LENGTH_SHORT)
                    .show()
                false
            }

            dbHelper.hasTimeConflict(
                currentUserId,
                selectedDate,
                startTime,
                endTime,
                excludeEventId
            ) -> {
                Toast.makeText(
                    requireContext(),
                    "Conflit d'horaire avec un événement existant",
                    Toast.LENGTH_SHORT
                ).show()
                false
            }

            else -> true
        }
    }

    private fun saveEventToDatabase(startTime: String, endTime: String, description: String) {
        // Vérifier d'abord si le créneau existe déjà
        val existing = eventObjects.any {
            it.startTime == startTime && it.endTime == endTime && it.date == selectedDate
        }

        if (!existing) {
            val id = dbHelper.insertEvent(currentUserId, selectedDate, startTime, endTime, description)
            if (id != -1L) {
                val newEvent = Event(id, currentUserId, selectedDate, startTime, endTime, description)
                eventObjects.add(newEvent)
                eventStrings.add("${startTime}-${endTime}: $description")
                eventAdapter.notifyItemInserted(eventStrings.size - 1)
                if (eventObjects.size >= 4) {
                    btnAddEvent.hide()
                }
                Toast.makeText(requireContext(), getString(R.string.event_added), Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), getString(R.string.add_error), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun Int.dpToPx(): Int {
        return (this * resources.displayMetrics.density).toInt()
    }

    private fun setupPageIndicators(container: LinearLayout, count: Int) {
        container.removeAllViews()
        for (i in 0 until count) {
            val indicator = ImageView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    resources.getDimensionPixelSize(R.dimen.indicator_size),
                    resources.getDimensionPixelSize(R.dimen.indicator_size)
                ).apply {
                    setMargins(8.dpToPx(), 0, 8.dpToPx(), 0)
                }
                setImageResource(
                    if (i == 0) R.drawable.indicator_selected
                    else R.drawable.indicator_unselected
                )
            }
            container.addView(indicator)
        }
    }

    private fun updatePageIndicators(container: LinearLayout, position: Int) {
        for (i in 0 until container.childCount) {
            val indicator = container.getChildAt(i) as ImageView
            indicator.setImageResource(
                if (i == position) R.drawable.indicator_selected
                else R.drawable.indicator_unselected
            )
        }
    }

    private fun loadEvents() {
        eventObjects.clear()
        eventStrings.clear()

        val events = dbHelper.getEventsByUserAndDate(currentUserId, selectedDate)
        eventObjects.addAll(events)
        eventStrings.addAll(events.map { "${it.startTime}-${it.endTime}: ${it.description}" })

        if (eventObjects.size >= 4) {
            btnAddEvent.hide()
        } else {
            btnAddEvent.show()
        }

        eventAdapter.notifyDataSetChanged()

        if (eventStrings.isEmpty()) {
            Toast.makeText(requireContext(), getString(R.string.no_events_for_date), Toast.LENGTH_SHORT).show()
        }
    }

    private fun onEditEvent(position: Int) {
        val event = eventObjects[position]
        showEditEventDialog(event, position)
    }


    private fun showEditEventDialog(event: Event, position: Int) {
        val builder = AlertDialog.Builder(requireContext())
        val inflater = layoutInflater

        val dialogView = inflater.inflate(R.layout.item_time_slot, null)

        val textTitle = dialogView.findViewById<TextView>(R.id.textTitle)
        val editStart = dialogView.findViewById<EditText>(R.id.editStart)
        val editEnd = dialogView.findViewById<EditText>(R.id.editEnd)
        val editEvent = dialogView.findViewById<EditText>(R.id.editEvent)

        TimePickerHelper(requireContext(), editStart).setupTimePicker()
        TimePickerHelper(requireContext(), editEnd).setupTimePicker()

        textTitle.text = getString(R.string.edit_slot_title)
        editStart.setText(event.startTime)
        editEnd.setText(event.endTime)
        editEvent.setText(event.description)

        // Déclarer la variable dialog comme lateinit puisqu'elle sera initialisée plus tard
        lateinit var dialog: AlertDialog

        val buttonsLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 16.dpToPx()
            }

            val btnCancel = Button(requireContext()).apply {
                text = "Annuler"
                setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
                background = ContextCompat.getDrawable(requireContext(), R.drawable.button_cancel_bg)
                setPadding(20.dpToPx(), 10.dpToPx(), 20.dpToPx(), 10.dpToPx())
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    marginEnd = 16.dpToPx()
                }
                setOnClickListener {
                    dialog.dismiss()
                }
            }

            val btnSave = Button(requireContext()).apply {
                text = getString(R.string.save)
                setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
                background = ContextCompat.getDrawable(requireContext(), R.drawable.button_add_bg)
                setPadding(20.dpToPx(), 10.dpToPx(), 20.dpToPx(), 10.dpToPx())
                setOnClickListener {
                    val startTime = editStart.text.toString()
                    val endTime = editEnd.text.toString()
                    val description = editEvent.text.toString()

                    when {
                        startTime.isEmpty() -> {
                            showError(editStart, getString(R.string.start_time_required))
                            return@setOnClickListener
                        }
                        endTime.isEmpty() -> {
                            showError(editEnd, getString(R.string.end_time_required))
                            return@setOnClickListener
                        }
                        startTime >= endTime -> {
                            showError(editEnd, getString(R.string.end_time_after_start))
                            return@setOnClickListener
                        }
                        description.isEmpty() -> {
                            showError(editEvent, getString(R.string.description_required))
                            return@setOnClickListener
                        }
                        else -> {
                            val updated = dbHelper.updateEvent(
                                event.id,
                                selectedDate,
                                startTime,
                                endTime,
                                description
                            )

                            if (updated > 0) {
                                eventObjects[position] = event.copy(
                                    startTime = startTime,
                                    endTime = endTime,
                                    description = description
                                )
                                eventStrings[position] = "$startTime-$endTime: $description"
                                eventAdapter.notifyItemChanged(position)
                                dialog.dismiss()
                                Toast.makeText(requireContext(),
                                    getString(R.string.slot_updated),
                                    Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }

            addView(btnCancel)
            addView(btnSave)
        }

        (dialogView as ViewGroup).addView(buttonsLayout)

        builder.setView(dialogView)
        dialog = builder.create() // Initialisation de la variable dialog

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            mainLayout.setRenderEffect(RenderEffect.createBlurEffect(20f, 20f, Shader.TileMode.CLAMP))
        }

        dialog.setOnDismissListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                mainLayout.setRenderEffect(null)
            }
        }

        dialog.show()
    }



    private fun showError(editText: EditText, message: String) {
        editText.error = message
        editText.requestFocus()
    }




    private fun onDeleteEvent(position: Int) {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.confirm_delete_title))
            .setMessage(getString(R.string.confirm_delete_message))
            .setPositiveButton(getString(R.string.delete)) { _, _ ->
                val eventToDelete = eventObjects[position]
                val deleted = dbHelper.deleteEvent(eventToDelete.id)
                if (deleted > 0) {
                    eventObjects.removeAt(position)
                    eventStrings.removeAt(position)
                    eventAdapter.notifyItemRemoved(position)

                    if (eventObjects.size < 4) {
                        btnAddEvent.show()
                    }

                    Toast.makeText(requireContext(), getString(R.string.event_deleted), Toast.LENGTH_SHORT)
                        .show()
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        database.close()
        dbHelper.close()
    }

}
