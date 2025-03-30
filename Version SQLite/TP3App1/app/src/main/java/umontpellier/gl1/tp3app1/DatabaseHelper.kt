package umontpellier.gl1.tp3app1

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import org.json.JSONArray
import org.json.JSONException



class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "UserDatabase.db"
        private const val DATABASE_VERSION = 1
        private const val TABLE_NAME = "users"
        private const val COLUMN_ID = "id"
        private const val COLUMN_NOM = "nom"
        private const val COLUMN_PRENOM = "prenom"
        private const val COLUMN_DATE_NAISSANCE = "date_naissance"
        private const val COLUMN_TELEPHONE = "telephone"
        private const val COLUMN_EMAIL = "email"
        private const val COLUMN_LOGIN = "login"
        private const val COLUMN_PASSWORD = "password"
        private const val COLUMN_INTERETS = "interets"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTable = """
            CREATE TABLE $TABLE_NAME (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_NOM TEXT,
                $COLUMN_PRENOM TEXT,
                $COLUMN_DATE_NAISSANCE TEXT,
                $COLUMN_TELEPHONE TEXT,
                $COLUMN_EMAIL TEXT,
                $COLUMN_LOGIN TEXT,
                $COLUMN_PASSWORD TEXT,
                $COLUMN_INTERETS TEXT
            )
        """.trimIndent()
        db.execSQL(createTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }

    fun insertUser(user: User): User {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_NOM, user.nom)
            put(COLUMN_PRENOM, user.prenom)
            put(COLUMN_DATE_NAISSANCE, user.dateNaissance)
            put(COLUMN_TELEPHONE, user.numTelephone)
            put(COLUMN_EMAIL, user.adresseEmail)
            put(COLUMN_LOGIN, user.login)
            put(COLUMN_PASSWORD, user.motDePasse)
            put(COLUMN_INTERETS, user.centresInteret?.joinToString(","))
        }

        val id = db.insert(TABLE_NAME, null, values)
        db.close()

        return if (id != -1L) {
            user.copy(id = id.toInt())
        } else {
            user
        }
    }


    fun updateUser(user: User): User? {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val values = ContentValues().apply {
                put(COLUMN_NOM, user.nom)
                put(COLUMN_PRENOM, user.prenom)
                put(COLUMN_DATE_NAISSANCE, user.dateNaissance)
                put(COLUMN_TELEPHONE, user.numTelephone)
                put(COLUMN_EMAIL, user.adresseEmail)
                put(COLUMN_LOGIN, user.login)
                put(COLUMN_PASSWORD, user.motDePasse)
                put(COLUMN_INTERETS, user.centresInteret?.joinToString(",") ?: "")
            }

            val rowsAffected = db.update(
                TABLE_NAME,
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

    fun getUserPassword(userId: Int): String {
        val db = this.readableDatabase
        val cursor = db.query(
            TABLE_NAME,
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

    fun refreshUser(id: Int): User? {
        val db = this.readableDatabase
        return db.rawQuery(
            "SELECT * FROM $TABLE_NAME WHERE $COLUMN_ID = ?",
            arrayOf(id.toString())
        ).use { cursor ->
            if (cursor.moveToFirst()) {
                User(
                    id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)),
                    nom = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NOM)),
                    prenom = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PRENOM)),
                    dateNaissance = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DATE_NAISSANCE)),
                    numTelephone = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TELEPHONE)),
                    adresseEmail = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EMAIL)),
                    login = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LOGIN)),
                    motDePasse = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PASSWORD)),
                    centresInteret = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_INTERETS))?.split(",")
                )
            } else {
                null
            }
        }
    }

    fun getUserById(id: Int): User? {
        val db = readableDatabase
        return db.rawQuery(
            "SELECT * FROM $TABLE_NAME WHERE $COLUMN_ID = ?",
            arrayOf(id.toString())
        ).use { cursor ->
            if (cursor.moveToFirst()) {
                User(
                    id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)),
                    nom = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NOM)),
                    prenom = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PRENOM)),
                    dateNaissance = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DATE_NAISSANCE)),
                    numTelephone = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TELEPHONE)),
                    adresseEmail = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EMAIL)),
                    login = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LOGIN)),
                    motDePasse = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PASSWORD)),
                    centresInteret = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_INTERETS))?.split(",")
                )
            } else {
                null
            }
        }
    }



    fun isLoginExists(login: String): Boolean {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_NAME,
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
            TABLE_NAME,
            arrayOf(COLUMN_ID),
            "$COLUMN_EMAIL = ?",
            arrayOf(email),
            null, null, null
        )
        val exists = cursor.count > 0
        cursor.close()
        return exists
    }



}
