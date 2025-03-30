import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.tp1application2.R
import com.example.tp1application2.User
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.regex.Pattern

class SignupFragment : Fragment() {
    private val db = FirebaseFirestore.getInstance()
    private val usersCollection = db.collection("users")

    // Déclaration des vues
    private lateinit var firstnameEditText: EditText
    private lateinit var lastnameEditText: EditText
    private lateinit var loginEditText: EditText
    private lateinit var emailEditText: EditText
    private lateinit var phoneEditText: EditText
    private lateinit var birthdateEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var signupButton: Button
    private lateinit var backButton: Button

    private val calendar = Calendar.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_signup, container, false)

        // Initialisation des vues
        firstnameEditText = view.findViewById(R.id.firstname)
        lastnameEditText = view.findViewById(R.id.lastname)
        loginEditText = view.findViewById(R.id.login)
        emailEditText = view.findViewById(R.id.email)
        phoneEditText = view.findViewById(R.id.phone)
        birthdateEditText = view.findViewById(R.id.birthdate)
        passwordEditText = view.findViewById(R.id.password)
        signupButton = view.findViewById(R.id.signup)
        backButton = view.findViewById(R.id.back)

        setupDateField()
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        signupButton.setOnClickListener {
            registerUser()
        }

        backButton.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupDateField() {
        birthdateEditText.apply {
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
        birthdateEditText.setText(dateFormat.format(calendar.time))
    }

    private fun registerUser() {
        val firstName = firstnameEditText.text.toString().trim()
        val lastName = lastnameEditText.text.toString().trim()
        val login = loginEditText.text.toString().trim()
        val email = emailEditText.text.toString().trim()
        val phone = phoneEditText.text.toString().trim()
        val birthdate = birthdateEditText.text.toString().trim()
        val password = passwordEditText.text.toString().trim()

        when {
            firstName.isEmpty() -> {
                showError(firstnameEditText, getString(R.string.username_required))
                return
            }
            lastName.isEmpty() -> {
                showError(lastnameEditText, getString(R.string.lastname_required))
                return
            }
            login.isEmpty() -> {
                showError(loginEditText, getString(R.string.username_required))
                return
            }
            !isValidLogin(login) -> {
                showError(loginEditText, getString(R.string.invalid_login))
                return
            }
            password.isEmpty() -> {
                showError(passwordEditText, getString(R.string.password_required))
                return
            }
            password.length < 6 -> {
                showError(passwordEditText, getString(R.string.password_length_error))
                return
            }
            email.isEmpty() -> {
                showError(emailEditText, getString(R.string.email_required))
                return
            }
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                showError(emailEditText, getString(R.string.invalid_email))
                return
            }
            birthdate.isEmpty() -> {
                showError(birthdateEditText, getString(R.string.birthdate_required))
                return
            }
        }

        // Vérifications asynchrones et enregistrement
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Vérifier si le login existe déjà
                val loginExists = isLoginExists(login)
                val emailExists = isEmailExists(email)

                withContext(Dispatchers.Main) {
                    when {
                        loginExists -> {
                            showError(loginEditText, getString(R.string.login_taken))
                            return@withContext
                        }
                        emailExists -> {
                            showError(emailEditText, getString(R.string.email_taken))
                            return@withContext
                        }
                        else -> {
                            // Tout est valide, procéder à l'inscription
                            continueRegistration(firstName, lastName, login, email, phone, birthdate, password)
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, getString(R.string.signup_error), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private suspend fun continueRegistration(
        firstName: String,
        lastName: String,
        login: String,
        email: String,
        phone: String,
        birthdate: String,
        password: String
    ) {
        try {
            val user = User(
                nom = firstName,
                prenom = lastName,
                dateNaissance = birthdate,
                numTelephone = phone,
                adresseEmail = email,
                login = login,
                motDePasse = password
            )

            // Ajouter l'utilisateur à Firestore
            val documentRef = usersCollection.document()
            user.id = documentRef.id
            documentRef.set(user.toMap()).await()

            withContext(Dispatchers.Main) {
                Toast.makeText(context, getString(R.string.signup_success), Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, getString(R.string.signup_error), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showError(editText: EditText, message: String) {
        editText.error = message
        editText.requestFocus()
    }

    private fun isValidLogin(login: String): Boolean {
        val loginPattern = Pattern.compile("^[a-zA-Z][a-zA-Z0-9]{0,9}$")
        return loginPattern.matcher(login).matches()
    }

    private suspend fun isLoginExists(login: String): Boolean {
        val query = usersCollection.whereEqualTo("login", login).get().await()
        return !query.isEmpty
    }

    private suspend fun isEmailExists(email: String): Boolean {
        val query = usersCollection.whereEqualTo("adresseEmail", email).get().await()
        return !query.isEmpty
    }
}