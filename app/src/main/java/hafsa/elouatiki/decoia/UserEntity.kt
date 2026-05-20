package hafsa.elouatiki.decoia

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val firstName: String,
    val lastName: String,
    val email: String,
    val password: String,
    val city: String,
    val role: String, // "Client" or "Decorateur"
    val profileImage: String? = null
)
