package com.example.tp1application2

import android.app.DatePickerDialog
import android.os.Bundle
import android.text.InputFilter
import android.util.Log
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern

class UpdateUserInformations : Fragment() {
    private lateinit var databaseHelper: DatabaseHelper
    private var isEditMode = false
    private var currentUserId: Long = -1
    private lateinit var editTextNom: EditText
    private lateinit var editTextPrenom: EditText
    private lateinit var editTextDateNaissance: EditText
    private lateinit var editTextNumTelephone: EditText
    private lateinit var editTextAdresseEmail: EditText
    private lateinit var editTextLogin: EditText
    private lateinit var editTextMotDePasse: EditText
    private lateinit var buttonSignUp: CardView
    private lateinit var textViewTitle: TextView

    private val calendar = Calendar.getInstance()

    companion object {
        private const val ARG_USER = "user"
        private const val TAG = "SignUpFragment"

        fun newInstance(user: User? = null): UpdateUserInformations {
            return UpdateUserInformations().apply {
                arguments = Bundle().apply {
                    user?.let { putSerializable(ARG_USER, it) }
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.modifieruserinformations, container, false)
        databaseHelper = DatabaseHelper(requireContext())

        initViews(view)
        checkEditMode()
        setupDateField()
        setInputFilters()

        return view
    }

    private fun initViews(view: View) {
        editTextNom = view.findViewById(R.id.nom)
        editTextPrenom = view.findViewById(R.id.prenom)
        editTextDateNaissance = view.findViewById(R.id.dateNaissance)
        editTextNumTelephone = view.findViewById(R.id.numTelephone)
        editTextAdresseEmail = view.findViewById(R.id.adresseEmail)
        editTextLogin = view.findViewById(R.id.login)
        editTextMotDePasse = view.findViewById(R.id.montDePasse)
        buttonSignUp = view.findViewById(R.id.buttonSignUp)
        textViewTitle = view.findViewById(R.id.textView2)
    }

    private fun checkEditMode() {
        arguments?.getSerializable(ARG_USER)?.let { user ->
            if (user is User) {
                isEditMode = true
                currentUserId = user.id ?: -1
                populateForm(user)
            }
        }
    }

    private fun populateForm(user: User) {
        editTextNom.setText(user.nom)
        editTextPrenom.setText(user.prenom)
        editTextDateNaissance.setText(user.dateNaissance)
        editTextNumTelephone.setText(user.numTelephone)
        editTextAdresseEmail.setText(user.adresseEmail)
        editTextLogin.setText(user.login)
        editTextMotDePasse.hint = getString(R.string.leave_empty_to_keep)
    }

    private fun setupDateField() {
        editTextDateNaissance.apply {
            isFocusable = false
            isClickable = true
            setOnClickListener { showDatePickerDialog() }
        }
    }

    private fun showDatePickerDialog() {
        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                calendar.set(year, month, day)
                updateDateDisplay()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.datePicker.maxDate = System.currentTimeMillis()
        datePickerDialog.show()
    }

    private fun updateDateDisplay() {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        editTextDateNaissance.setText(dateFormat.format(calendar.time))
    }

    private fun setInputFilters() {
        val letterFilter = InputFilter { source, _, _, _, _, _ ->
            if (source.toString().matches(Regex("^[a-zA-ZÀ-ÿ\\s-]*$"))) null else ""
        }

        editTextNom.filters = arrayOf(letterFilter)
        editTextPrenom.filters = arrayOf(letterFilter)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        buttonSignUp.setOnClickListener {
            Log.d(TAG, getString(R.string.button_clicked_log, if (isEditMode) getString(R.string.edit_mode) else getString(R.string.create_mode)))

            if (isEditMode) {
                updateUser()
            } else {
                registerUser()
            }
        }
    }

    private fun registerUser() {
        val user = validateAndGetUserData() ?: return

        try {
            if (databaseHelper.isLoginExists(user.login)) {
                editTextLogin.error = getString(R.string.login_taken)
                return
            }
            if (databaseHelper.isEmailExists(user.adresseEmail)) {
                editTextAdresseEmail.error = getString(R.string.email_taken)
                return
            }

            val userWithId = databaseHelper.insertUser(user)
            if (userWithId.id != null) {
                showSuccess(getString(R.string.signup_success))
                navigateToDisplayFragment(userWithId)
            } else {
                showError(getString(R.string.signup_error))
            }
        } catch (e: Exception) {
            showError(getString(R.string.error_prefix) + e.message)
        }
    }

    private fun updateUser() {
        Log.d(TAG, getString(R.string.update_attempt_log, currentUserId))

        if (currentUserId.toInt() == -1) {
            showError(getString(R.string.invalid_user_id))
            return
        }

        val user = validateAndGetUserData()?.copy(id = currentUserId) ?: run {
            showError(getString(R.string.invalid_data))
            return
        }

        try {
            val existingUser = databaseHelper.getUserById(currentUserId)

            if (existingUser?.login != user.login && databaseHelper.isLoginExists(user.login)) {
                editTextLogin.error = getString(R.string.login_taken)
                return
            }

            if (existingUser?.adresseEmail != user.adresseEmail && databaseHelper.isEmailExists(user.adresseEmail)) {
                editTextAdresseEmail.error = getString(R.string.email_taken)
                return
            }

            val updatedUser = databaseHelper.updateUser(user)

            if (updatedUser != null) {
                showSuccess(getString(R.string.profile_updated))
                val displayFragment = DisplayFragment.newInstance(updatedUser)
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, displayFragment)
                    .commit()
            } else {
                showError(getString(R.string.update_failed))
            }
        } catch (e: Exception) {
            showError(getString(R.string.system_error_prefix) + e.message)
        }
    }

    private fun validateAndGetUserData(): User? {
        val nom = editTextNom.text.toString().trim()
        val prenom = editTextPrenom.text.toString().trim()
        val dateNaissance = editTextDateNaissance.text.toString().trim()
        val numTelephone = editTextNumTelephone.text.toString().trim()
        val adresseEmail = editTextAdresseEmail.text.toString().trim()
        val login = editTextLogin.text.toString().trim()
        var motDePasse = editTextMotDePasse.text.toString().trim()

        if (isEditMode && motDePasse.isEmpty()) {
            motDePasse = databaseHelper.getUserPassword(currentUserId)
        }

        if (!validateFields(nom, prenom, dateNaissance, numTelephone, adresseEmail, login, motDePasse)) {
            return null
        }

        return User(
            id = if (isEditMode) currentUserId else null,
            nom = nom,
            prenom = prenom,
            dateNaissance = dateNaissance,
            numTelephone = numTelephone,
            adresseEmail = adresseEmail,
            login = login,
            motDePasse = motDePasse,
        )
    }

    private fun validateFields(
        nom: String,
        prenom: String,
        dateNaissance: String,
        numTelephone: String,
        adresseEmail: String,
        login: String,
        motDePasse: String
    ): Boolean {
        return when {
            nom.isEmpty() -> {
                editTextNom.error = getString(R.string.lastname_required)
                false
            }
            prenom.isEmpty() -> {
                editTextPrenom.error = getString(R.string.firstname_required)
                false
            }
            dateNaissance.isEmpty() -> {
                editTextDateNaissance.error = getString(R.string.birthdate_required)
                false
            }
            !isValidPhone(numTelephone) -> {
                editTextNumTelephone.error = getString(R.string.invalid_phone)
                false
            }
            !isValidEmail(adresseEmail) -> {
                editTextAdresseEmail.error = getString(R.string.invalid_email)
                false
            }
            login.isEmpty() -> {
                editTextLogin.error = getString(R.string.username_required)
                false
            }
            !isEditMode && motDePasse.isEmpty() -> {
                editTextMotDePasse.error = getString(R.string.password_required)
                false
            }
            !isEditMode && motDePasse.length < 6 -> {
                editTextMotDePasse.error = getString(R.string.password_min_length)
                false
            }
            else -> true
        }
    }

    private fun isValidEmail(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun isValidPhone(phone: String): Boolean {
        return phone.length >= 8 && phone.all { it.isDigit() }
    }

    private fun showSuccess(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    private fun showError(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    private fun navigateToDisplayFragment(user: User) {
        val displayFragment = DisplayFragment.newInstance(user)
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, displayFragment)
            .addToBackStack(null)
            .commit()
    }
}