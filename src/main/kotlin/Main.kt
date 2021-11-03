// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
import color_picker.ColorPickerWidget
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import org.jetbrains.skia.Image.Companion.makeFromEncoded
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import javax.imageio.ImageIO

@Composable
@Preview
fun App(game: Game) {
    var text by remember { mutableStateOf("Hello, World!") }
    var drkColor = mutableStateOf(darkColor)

    println(Color.White.toArgb())

    MaterialTheme {
        Row(modifier = Modifier.background(darkColor).padding(8.dp).fillMaxSize()) {
            Card(backgroundColor = mainColor, elevation = 6.dp ) {
                Row(modifier = Modifier.padding(8.dp)) {
                    Column {
                        Text("ORDER:")
                        game.getPlayers(Team.ORDER).forEach { summoner ->
                            SummonerComposable(summoner, summoner.kda.total)
                        }
                    }
                    Column {
                        Text("CHAOS:")
                        game.getPlayers(Team.CHAOS).forEach { summoner ->
                            SummonerComposable(summoner, summoner.kda.total)
                        }
                    }
                    ColorPickerWidget {colorPicked ->  mainColor = colorPicked}
                }
            }

            val scrollState = rememberScrollState()
            Column (modifier = Modifier.verticalScroll(scrollState)) {
                Text("Events:")
                game.events.forEach { event ->
                    EventComposable(event)
                }
            }
        }
    }
}

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "LeagueOfInfo"
    ) {
        val game = Game()
        App(game)
    }
}

fun loadNetworkImage(link: String): ImageBitmap {
    val url = URL(link.replace(" ", "").replace("'", ""))
    val connection = url.openConnection() as HttpURLConnection
    connection.connect()

    val inputStream = connection.inputStream
    val bufferedImage = ImageIO.read(inputStream)

    val stream = ByteArrayOutputStream()
    ImageIO.write(bufferedImage, "png", stream)
    val byteArray = stream.toByteArray()

    return makeFromEncoded(byteArray).toComposeImageBitmap()
}

var mainColor by mutableStateOf(Color(0xFF275682))
const val contrast = 0.3f
val defaultContrast: Float
get() {
    return 1 - mainColor.luminance()
}
val darkColor: Color
    get() {
        var r = 0
        var g = 0
        var b = 0

        r = ((r - mainColor.red * 255) * contrast + mainColor.red * 255).toInt()
        g = ((g - mainColor.green * 255) * contrast + mainColor.green * 255).toInt()
        b = ((b - mainColor.blue * 255) * contrast + mainColor.blue * 255).toInt()

        return Color(r, g, b)
    }
