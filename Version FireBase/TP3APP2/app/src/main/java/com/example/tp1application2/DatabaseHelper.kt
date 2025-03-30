package com.example.tp1application2

import android.content.Context
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

class DatabaseHelper(context: Context) {
    private val db: FirebaseFirestore = Firebase.firestore

    companion object {
        // Collections names
        const val COLLECTION_USERS = "users"
        const val COLLECTION_EVENTS = "events"
    }

    // User CRUD operations

    suspend fun insertUser(user: User): User {
        val docRef = db.collection(COLLECTION_USERS).document()
        val newUser = user.copy(id = docRef.id)
        docRef.set(newUser.toMap()).await()
        return newUser
    }

    suspend fun getUserById(id: String): User {
        val document = db.collection(COLLECTION_USERS).document(id).get().await()
        if (document.exists()) {
            return document.toUser()
        } else {
            throw NoSuchElementException("User with ID $id not found")
        }
    }
    suspend fun updateUser(user: User): Boolean {
        return try {
            db.collection(COLLECTION_USERS).document(user.id!!)
                .set(user.toMap()).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun isLoginExists(login: String): Boolean {
        val query = db.collection(COLLECTION_USERS)
            .whereEqualTo("login", login)
            .get()
            .await()
        return !query.isEmpty
    }

    suspend fun isEmailExists(email: String): Boolean {
        val query = db.collection(COLLECTION_USERS)
            .whereEqualTo("email", email)
            .get()
            .await()
        return !query.isEmpty
    }

    suspend fun getUserByLogin(login: String): User? {
        val query = db.collection(COLLECTION_USERS)
            .whereEqualTo("login", login)
            .get()
            .await()
        return if (!query.isEmpty) {
            query.documents[0].toUser()
        } else {
            null
        }
    }

    // Event CRUD operations

    suspend fun insertEvent(userId: String, date: String, startTime: String, endTime: String, description: String): String {
        val docRef = db.collection(COLLECTION_EVENTS).document()
        val event = Event(
            id = docRef.id,
            userId = userId,
            date = date,
            startTime = startTime,
            endTime = endTime,
            description = description
        )
        docRef.set(event.toMap()).await()
        return docRef.id
    }

    suspend fun getEventsByUser(userId: String): List<Event> {
        val query = db.collection(COLLECTION_EVENTS)
            .whereEqualTo("userId", userId)
            .orderBy("date")
            .orderBy("startTime")
            .get()
            .await()
        return query.documents.map { it.toEvent() }
    }

    suspend fun getEventsByUserAndDate(userId: String, date: String): List<Event> {
        val query = db.collection(COLLECTION_EVENTS)
            .whereEqualTo("userId", userId)
            .whereEqualTo("date", date)
            .orderBy("startTime")
            .get()
            .await()
        return query.documents.map { it.toEvent() }
    }

    suspend fun getEventById(eventId: String): Event? {
        val document = db.collection(COLLECTION_EVENTS).document(eventId).get().await()
        return if (document.exists()) {
            document.toEvent()
        } else {
            null
        }
    }

    suspend fun updateEvent(eventId: String, date: String, startTime: String, endTime: String, description: String): Boolean {
        return try {
            val updates = mapOf(
                "date" to date,
                "startTime" to startTime,
                "endTime" to endTime,
                "description" to description
            )
            db.collection(COLLECTION_EVENTS).document(eventId)
                .update(updates).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun deleteEvent(eventId: String): Boolean {
        return try {
            db.collection(COLLECTION_EVENTS).document(eventId).delete().await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun hasTimeConflict(userId: String, date: String, newStart: String, newEnd: String, excludeEventId: String? = null): Boolean {
        val query = db.collection(COLLECTION_EVENTS)
            .whereEqualTo("userId", userId)
            .whereEqualTo("date", date)
            .get()
            .await()

        return query.documents.any { document ->
            val event = document.toEvent()
            (excludeEventId == null || event.id != excludeEventId) &&
                    ((event.startTime < newEnd && event.endTime > newStart) ||
                            (event.startTime < newStart && event.endTime > newEnd) ||
                            (event.startTime in newStart..newEnd) ||
                            (event.endTime in newStart..newEnd))
        }
    }

    // Extension functions to convert between Firestore documents and model objects

    private fun User.toMap(): Map<String, Any?> {
        return mapOf(
            "firstName" to nom,
            "lastName" to prenom,
            "login" to login,
            "email" to adresseEmail,
            "phone" to numTelephone,
            "birthdate" to dateNaissance,
            "password" to motDePasse
        )
    }

    private fun com.google.firebase.firestore.DocumentSnapshot.toUser(): User {
        return User(
            id = id,
            nom = getString("firstName") ?: "",
            prenom = getString("lastName") ?: "",
            login = getString("login") ?: "",
            adresseEmail = getString("email") ?: "",
            numTelephone = getString("phone") ?: "",
            dateNaissance = getString("birthdate") ?: "",
            motDePasse = getString("password") ?: ""
        )
    }

    private fun Event.toMap(): Map<String, Any?> {
        return mapOf(
            "userId" to userId,
            "date" to date,
            "startTime" to startTime,
            "endTime" to endTime,
            "description" to description
        )
    }

    private fun com.google.firebase.firestore.DocumentSnapshot.toEvent(): Event {
        return Event(
            id = id, // Firebase document ID (String)
            userId = getString("userId") ?: -1L, // Récupération comme Long avec valeur par défaut -1
            date = getString("date") ?: "",
            startTime = getString("startTime") ?: "",
            endTime = getString("endTime") ?: "",
            description = getString("description") ?: ""
        )
    }
}