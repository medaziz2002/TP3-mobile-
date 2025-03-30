package com.example.tp1application2

import java.io.Serializable

data class User(
    var id: String? = null,  // Changé de Long? à String? pour Firebase
    val nom: String,
    val prenom: String,
    val dateNaissance: String,
    val numTelephone: String,
    val adresseEmail: String,
    val login: String,
    val motDePasse: String,
) : Serializable {

    // Convertit l'objet User en Map pour Firebase
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "nom" to nom,
            "prenom" to prenom,
            "dateNaissance" to dateNaissance,
            "numTelephone" to numTelephone,
            "adresseEmail" to adresseEmail,
            "login" to login,
            "motDePasse" to motDePasse
        )
    }

    companion object {
        // Crée un User à partir d'un document Firebase
        fun fromMap(map: Map<String, Any?>): User {
            return User(
                id = map["id"] as? String,
                nom = map["nom"] as? String ?: "",
                prenom = map["prenom"] as? String ?: "",
                dateNaissance = map["dateNaissance"] as? String ?: "",
                numTelephone = map["numTelephone"] as? String ?: "",
                adresseEmail = map["adresseEmail"] as? String ?: "",
                login = map["login"] as? String ?: "",
                motDePasse = map["motDePasse"] as? String ?: ""
            )
        }
    }
}