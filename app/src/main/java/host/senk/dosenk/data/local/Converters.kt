package host.senk.dosenk.data.local

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {
    @TypeConverter
    fun fromGrid(grid: Array<IntArray>?): String? {
        if (grid == null) return null
        return Gson().toJson(grid)
    }

    @TypeConverter
    fun toGrid(json: String?): Array<IntArray>? {
        if (json == null) return null
        val type = object : TypeToken<Array<IntArray>>() {}.type
        return Gson().fromJson(json, type)
    }
}