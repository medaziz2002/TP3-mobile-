package umontpellier.gl1.tp3app1

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController

class DisplayFragment : Fragment() {
    private lateinit var textViewNomPrenom: TextView
    private lateinit var editTextDateNaissance: EditText
    private lateinit var editTextNumTelephone: EditText
    private lateinit var editTextAdresseEmail: EditText
    private lateinit var editTextLogin: EditText
    private lateinit var checkBoxSport: CheckBox
    private lateinit var checkBoxMusique: CheckBox
    private lateinit var checkBoxLecture: CheckBox
    private lateinit var btnModifier: androidx.cardview.widget.CardView
    private lateinit var btnRevenir: androidx.cardview.widget.CardView

    private var currentUser: User? = null

    companion object {
        private const val ARG_USER = "user"
        private const val TAG = "DisplayFragment"

        fun newInstance(user: User): DisplayFragment {
            return DisplayFragment().apply {
                arguments = Bundle().apply {
                    putSerializable(ARG_USER, user)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_display, container, false)
        initViews(view)
        return view
    }

    private fun initViews(view: View) {
        textViewNomPrenom = view.findViewById(R.id.NomPrenom)
        editTextDateNaissance = view.findViewById(R.id.dateNaissance)
        editTextNumTelephone = view.findViewById(R.id.numTelephone)
        editTextAdresseEmail = view.findViewById(R.id.adresseEmail)
        editTextLogin = view.findViewById(R.id.login)
        checkBoxSport = view.findViewById(R.id.checkBoxSport)
        checkBoxMusique = view.findViewById(R.id.checkBoxMusique)
        checkBoxLecture = view.findViewById(R.id.checkBoxLecture)
        btnModifier = view.findViewById(R.id.btnModifier)
        btnRevenir = view.findViewById(R.id.btnRevenir)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        currentUser = arguments?.getSerializable(ARG_USER) as? User
        currentUser?.let {
            displayUserInfo(it)
        }

        setupButtons()
        setupButtonAnimations()
    }

    private fun displayUserInfo(user: User) {
        val idDisplay = user.id?.toString() ?: "N/A"
        textViewNomPrenom.text = "${user.nom} ${user.prenom}"

        editTextDateNaissance.setText(user.dateNaissance)
        editTextNumTelephone.setText(user.numTelephone)
        editTextAdresseEmail.setText(user.adresseEmail)
        editTextLogin.setText(user.login)

        user.centresInteret?.let { interests ->
            checkBoxSport.isChecked = interests.contains("Sport")
            checkBoxMusique.isChecked = interests.contains("Musique")
            checkBoxLecture.isChecked = interests.contains("Lecture")
        }
    }
    private fun setupButtons() {
        btnModifier.setOnClickListener {
            currentUser?.let { user ->
                Log.d(TAG, "Passage en mode édition pour l'utilisateur: $user")
                val signUpFragment = SignUpFragment.newInstance(user)
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, signUpFragment)
                    .addToBackStack(null)
                    .commit()
            }
        }

        btnRevenir.setOnClickListener {
            val signUpFragment = SignUpFragment.newInstance()
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, signUpFragment)
                .commit()
        }
    }

    private fun setupButtonAnimations() {
        listOf(btnModifier, btnRevenir).forEach { button ->
            button.setOnTouchListener { v, event ->
                when (event.action) {
                    android.view.MotionEvent.ACTION_DOWN -> {
                        v.animate().scaleX(0.9f).scaleY(0.9f).setDuration(100).start()
                    }
                    android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> {
                        v.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
                    }
                }
                false
            }
        }
    }
}