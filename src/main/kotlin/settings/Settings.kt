package settings

import androidx.compose.material.SwitchColors
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.useResource
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import java.io.File

data class Settings(
    @SerializedName("colors")
    val colors: ColorSettings,

    @SerializedName("switchColors")
    var switchTeamColors: Boolean
)

data class ColorSettings(
    @SerializedName("mainColor")
    var mainColorInt: ULong,

    @SerializedName("contrast")
    var contrast: Float
)

class SettingsSaver(setColor: (Color) -> Unit, setContrast: (Float) -> Unit, setSwitchColors: (Boolean) -> Unit) {
    var counter = 3
    var counting = false

    val gson = Gson()

    var settings: Settings

    private val settingsFile = File("${System.getenv("APPDATA")}/LeagueOfInfo/settings.json")

    init {
        if (!settingsFile.exists()){
            File("${System.getenv("APPDATA")}/LeagueOfInfo").mkdirs()
            settingsFile.createNewFile()
            settings = Settings(ColorSettings(0u, 0f), true)
            useResource("defaultSettings.json") {
                val defaultSettings = it.bufferedReader().readText()
                settingsFile.writeText(defaultSettings)
                settings = gson.fromJson(defaultSettings, Settings::class.java)
                setColor(Color(settings.colors.mainColorInt))
                setContrast(settings.colors.contrast)
            }
        } else {
            settings = gson.fromJson(settingsFile.readText(), Settings::class.java)
            setColor(Color(settings.colors.mainColorInt))
            setContrast(settings.colors.contrast)
            setSwitchColors(settings.switchTeamColors)
        }

    }

    fun saveSettings(modifySettings: (Settings) -> Settings) {
        settings = modifySettings(settings)
        startCounter()
    }

    private fun save() {
        settingsFile.writeText(gson.toJson(settings))
    }

    fun resetCounter() {
        counter = 3
    }

    fun startCounter() {
        resetCounter()
        if(!counting) {
            counting = true
            count()
        }
    }

    private fun count(){
        CoroutineScope(Dispatchers.IO).launch {
            while (counting && counter > 0) {
                delay(1000)
                counter--
                println("counter now $counter")
            }
            println("saving")
            save()
            counting = false
        }
    }
}