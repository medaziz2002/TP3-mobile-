package umontpellier.gl1.tp3app1

import android.app.DatePickerDialog
import android.os.Bundle
import android.text.InputFilter
import android.util.Log
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import java.text.SimpleDateFormat
import java.util.*

class SignUpFragment : Fragment() {
    private lateinit var databaseHelper: DatabaseHelper
    private var isEditMode = false
    private var currentUserId: Int = -1

    private lateinit var editTextNom: EditText
    private lateinit var editTextPrenom: EditText
    private lateinit var editTextDateNaissance: EditText
    private lateinit var editTextNumTelephone: EditText
    private lateinit var editTextAdresseEmail: EditText
    private lateinit var editTextLogin: EditText
    private lateinit var editTextMotDePasse: EditText
    private lateinit var checkBoxSport: CheckBox
    private lateinit var checkBoxMusique: CheckBox
    private lateinit var checkBoxLecture: CheckBox
    private lateinit var buttonSignUp: Button
    private lateinit var textViewTitle: TextView

    private val calendar = Calendar.getInstance()

    companion object {
        private const val ARG_USER = "user"
        private const val TAG = "SignUpFragment"

        fun newInstance(user: User? = null): SignUpFragment {
            return SignUpFragment().apply {
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
        val view = inflater.inflate(R.layout.fragment_sign_up, container, false)
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
        checkBoxSport = view.findViewById(R.id.checkBoxSport)
        checkBoxMusique = view.findViewById(R.id.checkBoxMusique)
        checkBoxLecture = view.findViewById(R.id.checkBoxLecture)
        buttonSignUp = view.findViewById(R.id.buttonSignUp)
        textViewTitle = view.findViewById(R.id.textView2)
    }

    private fun checkEditMode() {
        arguments?.getSerializable(ARG_USER)?.let { user ->
            if (user is User) {

                isEditMode = true
                currentUserId = user.id ?: -1
                populateForm(user)
                updateUIForEditMode()
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
        editTextMotDePasse.hint = "Laissez vide pour ne pas changer"

        user.centresInteret?.let { interests ->
            checkBoxSport.isChecked = interests.contains("Sport")
            checkBoxMusique.isChecked = interests.contains("Musique")
            checkBoxLecture.isChecked = interests.contains("Lecture")
        }
    }

    private fun updateUIForEditMode() {
        textViewTitle.text = "Modifier Profil"
        buttonSignUp.text = "Mettre à jour"
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
            Log.d(TAG, "Bouton cliqué - Mode: ${if (isEditMode) "Édition" else "Création"}")

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
                editTextLogin.error = "Ce login est déjà utilisé"
                return
            }
            if (databaseHelper.isEmailExists(user.adresseEmail)) {
                editTextAdresseEmail.error = "Cet email est déjà utilisé"
                return
            }

            val userWithId = databaseHelper.insertUser(user)
            if (userWithId.id != null) {
                showSuccess("Inscription réussie!")
                navigateToDisplayFragment(userWithId)
            } else {
                showError("Erreur lors de l'inscription")
            }
        } catch (e: Exception) {
            showError("Erreur: ${e.message}")
        }
    }

    private fun updateUser() {
        Log.d(TAG, "Tentative de mise à jour avec currentUserId: $currentUserId")

        if (currentUserId == -1) {
            showError("Erreur: ID utilisateur invalide")
            return
        }

        val user = validateAndGetUserData()?.copy(id = currentUserId) ?: run {
            showError("Données invalides")
            return
        }

        try {

            val existingUser = databaseHelper.getUserById(currentUserId)


            if (existingUser?.login != user.login && databaseHelper.isLoginExists(user.login)) {
                editTextLogin.error = "Ce login est déjà utilisé"
                return
            }


            if (existingUser?.adresseEmail != user.adresseEmail && databaseHelper.isEmailExists(user.adresseEmail)) {
                editTextAdresseEmail.error = "Cet email est déjà utilisé"
                return
            }


            if (user.centresInteret.isNullOrEmpty()) {
                showError("Veuillez sélectionner au moins un centre d'intérêt")
                return
            }


            val updatedUser = databaseHelper.updateUser(user)

            if (updatedUser != null) {
                showSuccess("Profil mis à jour")


                val displayFragment = DisplayFragment.newInstance(updatedUser)
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, displayFragment)
                    .commit()
            } else {
                showError("Échec de la mise à jour")
            }
        } catch (e: Exception) {
            showError("Erreur système: ${e.message}")

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


        val interests = mutableListOf<String>().apply {
            if (checkBoxSport.isChecked) add("Sport")
            if (checkBoxMusique.isChecked) add("Musique")
            if (checkBoxLecture.isChecked) add("Lecture")
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
            centresInteret = interests
        )
    }
    private fun validateInterests(): Boolean {
        return if (!checkBoxSport.isChecked && !checkBoxMusique.isChecked && !checkBoxLecture.isChecked) {
            showError("Veuillez sélectionner au moins un centre d'intérêt")
            false
        } else {
            true
        }
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
                editTextNom.error = "Nom requis"
                false
            }
            prenom.isEmpty() -> {
                editTextPrenom.error = "Prénom requis"
                false
            }
            dateNaissance.isEmpty() -> {
                editTextDateNaissance.error = "Date de naissance requise"
                false
            }
            !isValidPhone(numTelephone) -> {
                editTextNumTelephone.error = "Téléphone invalide (8 chiffres min)"
                false
            }
            !isValidEmail(adresseEmail) -> {
                editTextAdresseEmail.error = "Email invalide"
                false
            }
            login.isEmpty() -> {
                editTextLogin.error = "Login requis"
                false
            }
            !isEditMode && motDePasse.isEmpty() -> {
                editTextMotDePasse.error = "Mot de passe requis"
                false
            }
            !isEditMode && motDePasse.length < 6 -> {
                editTextMotDePasse.error = "6 caractères minimum"
                false
            }
            !validateInterests() -> false
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