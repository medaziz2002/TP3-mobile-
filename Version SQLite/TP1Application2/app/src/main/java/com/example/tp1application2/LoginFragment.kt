import android.content.Context
import android.database.sqlite.SQLiteDatabase
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
import com.example.tp1application2.DatabaseHelper
import com.example.tp1application2.PlanningFragment
import com.example.tp1application2.R

class LoginFragment : Fragment() {
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var database: SQLiteDatabase

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

        dbHelper = DatabaseHelper(requireContext())
        database = dbHelper.readableDatabase

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


                // Vérification des identifiants
                val userId = verifierUtilisateur(username, password)
                if (userId != -1L) {
                    saveUserSession(userId)
                    showToast(getString(R.string.login_success))
                    allerVersPlanning()
                } else {
                    showError(passwordEditText, getString(R.string.login_error))
                }
            } catch (e: Exception) {
                Log.e("LoginError", "Erreur lors de la connexion", e)
                showToast(getString(R.string.generic_error))
            }
        }

        backButton.setOnClickListener {

                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, LandingPageFragment())
                    .commit()

        }
    }

    private fun showError(editText: EditText, message: String) {
        editText.error = message
        editText.requestFocus()
    }


    private fun verifierUtilisateur(username: String, password: String): Long {
        val query = """
            SELECT ${DatabaseHelper.COLUMN_USER_ID} 
            FROM ${DatabaseHelper.TABLE_USERS} 
            WHERE ${DatabaseHelper.COLUMN_LOGIN} = ? 
            AND ${DatabaseHelper.COLUMN_PASSWORD} = ?
        """.trimIndent()

        val cursor = database.rawQuery(query, arrayOf(username, password))
        return if (cursor.moveToFirst()) {
            cursor.getLong(0)
        } else {
            -1L
        }.also { cursor.close() }
    }

    private fun allerVersPlanning() {
        try {
            val userId = getUserIdFromSession()
            val planningFragment = PlanningFragment().apply {
                arguments = Bundle().apply {
                    putLong("USER_ID", userId)
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

    private fun saveUserSession(userId: Long) {
        requireContext().getSharedPreferences("UserSession", Context.MODE_PRIVATE).edit {
            putLong("USER_ID", userId)
            apply()
        }
    }

    private fun getUserIdFromSession(): Long {
        return requireContext().getSharedPreferences("UserSession", Context.MODE_PRIVATE)
            .getLong("USER_ID", -1).takeIf { it != -1L }
            ?: throw IllegalStateException("Session invalide")
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        database.close()
    }
}