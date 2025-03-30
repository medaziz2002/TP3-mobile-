package com.example.tp1application2

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment

class DisplayFragment : Fragment() {
    private lateinit var textViewNomPrenom: TextView
    private lateinit var editTextDateNaissance: EditText
    private lateinit var editTextNumTelephone: EditText
    private lateinit var editTextAdresseEmail: EditText
    private lateinit var editTextLogin: EditText
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
        val view = inflater.inflate(R.layout.profil_fragement, container, false)
        initViews(view)
        return view
    }

    private fun initViews(view: View) {
        textViewNomPrenom = view.findViewById(R.id.NomPrenom)
        editTextDateNaissance = view.findViewById(R.id.dateNaissance)
        editTextNumTelephone = view.findViewById(R.id.numTelephone)
        editTextAdresseEmail = view.findViewById(R.id.adresseEmail)
        editTextLogin = view.findViewById(R.id.login)
        btnModifier = view.findViewById(R.id.btnModifier)
        btnRevenir = view.findViewById(R.id.btnCancel)
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

    }
    private fun setupButtons() {
        btnModifier.setOnClickListener {
            currentUser?.let { user ->
                Log.d(TAG, "Passage en mode édition pour l'utilisateur: $user")
                val UpdateUserInformations = UpdateUserInformations.newInstance(user)
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, UpdateUserInformations)
                    .addToBackStack(null)
                    .commit()
            }
        }
        btnRevenir.setOnClickListener {
            parentFragmentManager.popBackStack()
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