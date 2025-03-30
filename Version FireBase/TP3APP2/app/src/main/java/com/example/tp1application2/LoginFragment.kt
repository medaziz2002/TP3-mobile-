import android.content.Context
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.tp1application2.PlanningFragment
import com.example.tp1application2.R
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class LoginFragment : Fragment() {
    private val db = FirebaseFirestore.getInstance()
    private val usersCollection = db.collection("users")

    private lateinit var usernameEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var loginButton: Button
    private lateinit var backButton: Button

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_login, container, false)

        usernameEditText = view.findViewById(R.id.username)
        passwordEditText = view.findViewById(R.id.password)
        loginButton = view.findViewById(R.id.login)
        backButton = view.findViewById(R.id.back)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loginButton.setOnClickListener {
            try {
                if (!isAdded || isDetached) return@setOnClickListener

                val username = usernameEditText.text.toString().trim()
                val password = passwordEditText.text.toString().trim()

                // Validation des champs
                when {
                    username.isEmpty() -> {
                        showError(usernameEditText, getString(R.string.username_required))
                        return@setOnClickListener
                    }
                    password.isEmpty() -> {
                        showError(passwordEditText, getString(R.string.password_required))
                        return@setOnClickListener
                    }
                }

                // Vérification des identifiants avec Firebase
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val userId = verifierUtilisateur(username, password)
                        withContext(Dispatchers.Main) {
                            if (userId != null) {
                                saveUserSession(userId)
                                showToast(getString(R.string.login_success))
                                allerVersPlanning(userId)
                            } else {
                                showError(passwordEditText, getString(R.string.login_error))
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("LoginError", "Erreur lors de la connexion", e)
                        withContext(Dispatchers.Main) {
                            showToast(getString(R.string.generic_error))
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("LoginError", "Erreur lors de la connexion", e)
                showToast(getString(R.string.generic_error))
            }
        }

        backButton.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private suspend fun verifierUtilisateur(email: String, password: String): String? {
        return try {
            val authResult = Firebase.auth.signInWithEmailAndPassword(email, password).await()
            authResult.user?.uid
        } catch (e: Exception) {
            Log.e("Auth", "Erreur de connexion", e)
            null
        }
    }

    private fun allerVersPlanning(userId: String) {
        try {
            val planningFragment = PlanningFragment().apply {
                arguments = Bundle().apply {
                    putString("USER_ID", userId)
                }
            }

            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, planningFragment)
                .addToBackStack(null)
                .commit()

        } catch (e: IllegalStateException) {
            showToast("Session expirée, veuillez vous reconnecter")
        } catch (e: Exception) {
            showToast("Erreur: ${e.message}")
            Log.e("Navigation", "Erreur navigation", e)
        }
    }

    private fun saveUserSession(userId: String) {
        requireContext().getSharedPreferences("UserSession", Context.MODE_PRIVATE).edit {
            putString("USER_ID", userId)
            apply()
        }
    }

    private fun getUserIdFromSession(): String {
        return requireContext().getSharedPreferences("UserSession", Context.MODE_PRIVATE)
            .getString("USER_ID", null)
            ?: throw IllegalStateException("Session invalide")
    }

    private fun showError(editText: EditText, message: String) {
        editText.error = message
        editText.requestFocus()
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
}