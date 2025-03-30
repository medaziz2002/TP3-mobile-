package com.example.tp1application2

import android.app.DownloadManager.COLUMN_ID
import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.provider.BaseColumns

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        // Database info
        const val DATABASE_NAME = "UserDatabase.db"
        const val DATABASE_VERSION = 3 // Version incrémentée

        // Table Users
        const val TABLE_USERS = "users"
        const val COLUMN_USER_ID = BaseColumns._ID
        const val COLUMN_FIRST_NAME = "first_name"
        const val COLUMN_LAST_NAME = "last_name"
        const val COLUMN_LOGIN = "login"
        const val COLUMN_EMAIL = "email"
        const val COLUMN_PHONE = "phone"
        const val COLUMN_BIRTHDATE = "birthdate"
        const val COLUMN_PASSWORD = "password"

        // Table Events
        const val TABLE_EVENTS = "events"
        const val COLUMN_EVENT_ID = "event_id"
        const val COLUMN_EVENT_USER_ID = "user_id"
        const val COLUMN_EVENT_DATE = "date"
        const val COLUMN_EVENT_START_TIME = "start_time"
        const val COLUMN_EVENT_END_TIME = "end_time"
        const val COLUMN_EVENT_DESCRIPTION = "description"
    }

    override fun onCreate(db: SQLiteDatabase) {
        // Create Users table
        val CREATE_USERS_TABLE = """
            CREATE TABLE $TABLE_USERS (
                $COLUMN_USER_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_FIRST_NAME TEXT NOT NULL,
                $COLUMN_LAST_NAME TEXT NOT NULL,
                $COLUMN_LOGIN TEXT NOT NULL UNIQUE,
                $COLUMN_EMAIL TEXT NOT NULL UNIQUE,
                $COLUMN_PHONE TEXT NOT NULL,
                $COLUMN_BIRTHDATE TEXT NOT NULL,
                $COLUMN_PASSWORD TEXT NOT NULL
            )
        """.trimIndent()

        // Create Events table with time columns
        val CREATE_EVENTS_TABLE = """
            CREATE TABLE $TABLE_EVENTS (
                $COLUMN_EVENT_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_EVENT_USER_ID INTEGER NOT NULL,
                $COLUMN_EVENT_DATE TEXT NOT NULL,
                $COLUMN_EVENT_START_TIME TEXT NOT NULL,
                $COLUMN_EVENT_END_TIME TEXT NOT NULL,
                $COLUMN_EVENT_DESCRIPTION TEXT NOT NULL,
                FOREIGN KEY ($COLUMN_EVENT_USER_ID) 
                REFERENCES $TABLE_USERS($COLUMN_USER_ID) ON DELETE CASCADE,
                CHECK ($COLUMN_EVENT_START_TIME < $COLUMN_EVENT_END_TIME)
            )
        """.trimIndent()

        db.execSQL(CREATE_USERS_TABLE)
        db.execSQL(CREATE_EVENTS_TABLE)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        when (oldVersion) {
            1 -> {
                // Upgrade from version 1 to 3
                db.execSQL("DROP TABLE IF EXISTS $TABLE_EVENTS")
                onCreate(db)
            }
            2 -> {
                // Upgrade from version 2 to 3 (add time columns)
                db.execSQL("ALTER TABLE $TABLE_EVENTS ADD COLUMN $COLUMN_EVENT_START_TIME TEXT NOT NULL DEFAULT '00:00'")
                db.execSQL("ALTER TABLE $TABLE_EVENTS ADD COLUMN $COLUMN_EVENT_END_TIME TEXT NOT NULL DEFAULT '00:00'")
            }
        }
    }

    // Event CRUD operations

    fun insertEvent(userId: Long, date: String, startTime: String, endTime: String, description: String): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_EVENT_USER_ID, userId)
            put(COLUMN_EVENT_DATE, date)
            put(COLUMN_EVENT_START_TIME, startTime)
            put(COLUMN_EVENT_END_TIME, endTime)
            put(COLUMN_EVENT_DESCRIPTION, description)
        }
        return db.insert(TABLE_EVENTS, null, values)
    }

    fun getEventsByUser(userId: Long): List<Event> {
        val events = mutableListOf<Event>()
        val db = readableDatabase

        val query = """
            SELECT * FROM $TABLE_EVENTS 
            WHERE $COLUMN_EVENT_USER_ID = ?
            ORDER BY $COLUMN_EVENT_DATE, $COLUMN_EVENT_START_TIME
        """.trimIndent()

        val cursor = db.rawQuery(query, arrayOf(userId.toString()))

        with(cursor) {
            while (moveToNext()) {
                events.add(extractEventFromCursor())
            }
        }
        cursor.close()
        return events
    }

    fun getEventsByUserAndDate(userId: Long, date: String): List<Event> {
        val events = mutableListOf<Event>()
        val db = readableDatabase

        val query = """
            SELECT * FROM $TABLE_EVENTS 
            WHERE $COLUMN_EVENT_USER_ID = ? AND $COLUMN_EVENT_DATE = ?
            ORDER BY $COLUMN_EVENT_START_TIME
        """.trimIndent()

        val cursor = db.rawQuery(query, arrayOf(userId.toString(), date))

        with(cursor) {
            while (moveToNext()) {
                events.add(extractEventFromCursor())
            }
        }
        cursor.close()
        return events
    }

    fun getEventById(eventId: Long): Event? {
        val db = readableDatabase
        val query = """
            SELECT * FROM $TABLE_EVENTS 
            WHERE $COLUMN_EVENT_ID = ?
        """.trimIndent()

        val cursor = db.rawQuery(query, arrayOf(eventId.toString()))
        return if (cursor.moveToFirst()) {
            cursor.extractEventFromCursor().also { cursor.close() }
        } else {
            cursor.close()
            null
        }
    }

    fun updateEvent(eventId: Long, date: String, startTime: String, endTime: String, description: String): Int {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_EVENT_DATE, date)
            put(COLUMN_EVENT_START_TIME, startTime)
            put(COLUMN_EVENT_END_TIME, endTime)
            put(COLUMN_EVENT_DESCRIPTION, description)
        }
        return db.update(
            TABLE_EVENTS,
            values,
            "$COLUMN_EVENT_ID = ?",
            arrayOf(eventId.toString())
        )
    }

    fun deleteEvent(eventId: Long): Int {
        val db = writableDatabase
        return db.delete(
            TABLE_EVENTS,
            "$COLUMN_EVENT_ID = ?",
            arrayOf(eventId.toString())
        )
    }

    // Time conflict check
    fun hasTimeConflict(userId: Long, date: String, newStart: String, newEnd: String, excludeEventId: Long? = null): Boolean {
        val db = readableDatabase
        val query = """
            SELECT COUNT(*) FROM $TABLE_EVENTS 
            WHERE $COLUMN_EVENT_USER_ID = ? 
            AND $COLUMN_EVENT_DATE = ?
            AND $COLUMN_EVENT_ID != ?
            AND (
                ($COLUMN_EVENT_START_TIME < ? AND $COLUMN_EVENT_END_TIME > ?)
                OR ($COLUMN_EVENT_START_TIME < ? AND $COLUMN_EVENT_END_TIME > ?)
                OR ($COLUMN_EVENT_START_TIME BETWEEN ? AND ?)
                OR ($COLUMN_EVENT_END_TIME BETWEEN ? AND ?)
            )
        """.trimIndent()

        val args = arrayOf(
            userId.toString(),
            date,
            excludeEventId?.toString() ?: "-1",
            newEnd, newStart, // First condition
            newStart, newEnd, // Second condition
            newStart, newEnd, // Third condition
            newStart, newEnd  // Fourth condition
        )

        val cursor = db.rawQuery(query, args)
        val count = if (cursor.moveToFirst()) cursor.getInt(0) else 0
        cursor.close()
        return count > 0
    }

    // Helper extension function
    private fun android.database.Cursor.extractEventFromCursor(): Event {
        return Event(
            id = getLong(getColumnIndexOrThrow(COLUMN_EVENT_ID)),
            userId = getLong(getColumnIndexOrThrow(COLUMN_EVENT_USER_ID)),
            date = getString(getColumnIndexOrThrow(COLUMN_EVENT_DATE)),
            startTime = getString(getColumnIndexOrThrow(COLUMN_EVENT_START_TIME)),
            endTime = getString(getColumnIndexOrThrow(COLUMN_EVENT_END_TIME)),
            description = getString(getColumnIndexOrThrow(COLUMN_EVENT_DESCRIPTION))
        )
    }




    fun isLoginExists(login: String): Boolean {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_USERS,
            arrayOf(COLUMN_ID),
            "$COLUMN_LOGIN = ?",
            arrayOf(login),
            null, null, null
        )
        val exists = cursor.count > 0
        cursor.close()
        return exists
    }

    fun isEmailExists(email: String): Boolean {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_USERS,
            arrayOf(COLUMN_ID),
            "$COLUMN_EMAIL = ?",
            arrayOf(email),
            null, null, null
        )
        val exists = cursor.count > 0
        cursor.close()
        return exists
    }


    fun getUserPassword(userId: Long): String {
        val db = this.readableDatabase
        val cursor = db.query(
            TABLE_USERS,
            arrayOf(COLUMN_PASSWORD),
            "$COLUMN_ID = ?",
            arrayOf(userId.toString()),
            null, null, null
        )

        return if (cursor.moveToFirst()) {
            cursor.getString(0)
        } else {
            ""
        }.also { cursor.close() }
    }



    fun getUserById(id: Long): User {
        val db = readableDatabase
        return db.rawQuery(
            "SELECT * FROM $TABLE_USERS WHERE $COLUMN_ID = ?",
            arrayOf(id.toString())
        ).use { cursor ->
            if (cursor.moveToFirst()) {
                User(
                    id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID)),
                    nom = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_FIRST_NAME)),
                    prenom = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LAST_NAME)),
                    dateNaissance = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_BIRTHDATE)),
                    numTelephone = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PHONE)),
                    adresseEmail = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EMAIL)),
                    login = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LOGIN)),
                    motDePasse = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PASSWORD)),
                )
            } else {
                throw NoSuchElementException("User with ID $id not found")
            }
        }
    }



    fun insertUser(user: User): User {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_FIRST_NAME, user.nom)
            put(COLUMN_LAST_NAME, user.prenom)
            put(COLUMN_BIRTHDATE, user.dateNaissance)
            put(COLUMN_PHONE, user.numTelephone)
            put(COLUMN_EMAIL, user.adresseEmail)
            put(COLUMN_LOGIN, user.login)
            put(COLUMN_PASSWORD, user.motDePasse)
        }

        val id = db.insert(TABLE_USERS, null, values)
        db.close()

        return if (id != -1L) {
            user.copy(id = id)
        } else {
            user
        }
    }



    fun refreshUser(id: Long): User? {
        val db = this.readableDatabase
        return db.rawQuery(
            "SELECT * FROM $TABLE_USERS WHERE $COLUMN_ID = ?",
            arrayOf(id.toString())
        ).use { cursor ->
            if (cursor.moveToFirst()) {
                User(
                    id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID)),
                    nom = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_FIRST_NAME)),
                    prenom = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LAST_NAME)),
                    dateNaissance = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_BIRTHDATE)),
                    numTelephone = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PHONE)),
                    adresseEmail = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EMAIL)),
                    login = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LOGIN)),
                    motDePasse = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PASSWORD)),
                )
            } else {
                null
            }
        }
    }


    fun updateUser(user: User): User? {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val values = ContentValues().apply {
                put(COLUMN_FIRST_NAME, user.nom)
                put(COLUMN_LAST_NAME, user.prenom)
                put(COLUMN_BIRTHDATE, user.dateNaissance)
                put(COLUMN_PHONE, user.numTelephone)
                put(COLUMN_EMAIL, user.adresseEmail)
                put(COLUMN_LOGIN, user.login)
                put(COLUMN_PASSWORD, user.motDePasse)
            }

            val rowsAffected = db.update(
                TABLE_USERS,
                values,
                "$COLUMN_ID = ?",
                arrayOf(user.id.toString())
            )

            if (rowsAffected > 0) {
                db.setTransactionSuccessful()
                return refreshUser(user.id ?: -1)
            }
            return null
        } catch (e: Exception) {
            return null
        } finally {
            db.endTransaction()
            db.close()
        }
    }




}