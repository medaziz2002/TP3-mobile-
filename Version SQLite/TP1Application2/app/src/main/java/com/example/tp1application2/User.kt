package com.example.tp1application2

import java.io.Serializable

data class User(
    var id: Long? = null,
    val nom: String,
    val prenom: String,
    val dateNaissance: String,
    val numTelephone: String,
    val adresseEmail: String,
    val login: String,
    val motDePasse: String,
) : Serializable