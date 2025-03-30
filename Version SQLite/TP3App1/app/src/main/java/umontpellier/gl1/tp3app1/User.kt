package umontpellier.gl1.tp3app1

import java.io.Serializable



data class User(
    var id: Int? = null,
    val nom: String,
    val prenom: String,
    val dateNaissance: String,
    val numTelephone: String,
    val adresseEmail: String,
    val login: String,
    val motDePasse: String,
    val centresInteret: List<String>? = null
) : Serializable