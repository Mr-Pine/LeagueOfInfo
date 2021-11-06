// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
import androidx.compose.desktop.DesktopMaterialTheme
import color_picker.ColorPickerWidget
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import design.darken
import design.withLightness
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.skia.Image.Companion.makeFromEncoded
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import javax.imageio.ImageIO
import kotlin.math.sqrt

@Composable
@Preview
fun App(game: Game, windowSize: DpSize) {
    var text by remember { mutableStateOf("Hello, World!") }
    var contrast by remember { mutableStateOf(0.5f) }
    val darkColor by remember(mainColor, contrast) { mutableStateOf(mainColor.darken(contrast)) }

    val orderColor by remember(mainColor) { mutableStateOf(Color(0xFF0A96AA).withLightness(sqrt(sqrt(mainColor.luminance())))) }
    val chaosColor by remember(mainColor) { mutableStateOf(Color(0xFFBE1E37).withLightness(sqrt(sqrt(mainColor.luminance())))) }

    val localDensity = LocalDensity.current

    MaterialTheme {
        Row(
            modifier = Modifier.background(darkColor).padding(top = 16.dp, bottom = 8.dp, start = 12.dp, end = 4.dp)
                .then(if (windowSize.isSpecified) Modifier.size(windowSize) else Modifier),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            Box(Modifier.fillMaxWidth().weight(1f)) {
                val verticalScrollState = rememberScrollState()
                Column(Modifier.fillMaxWidth().verticalScroll(verticalScrollState)) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp, end = 12.dp, start = 4.dp),
                        elevation = 4.dp
                    ) {
                        Row() {
                            Column(
                                modifier = Modifier.fillMaxWidth().weight(1f).background(orderColor)
                            ) {
                                Text(
                                    "Team ORDER",
                                    modifier = Modifier.fillMaxWidth().background(orderColor.darken(contrast / 2))
                                        .padding(4.dp),
                                    textAlign = TextAlign.Center
                                )
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    game.getPlayers(Team.ORDER).forEach { summoner ->
                                        SummonerComposable(
                                            summoner,
                                            summoner.kda.total,
                                            orderColor.darken(contrast / 3),
                                            game.summonerSelected
                                        ) {
                                            game.summonerSelected = it
                                        }
                                    }
                                }
                            }
                            Column(
                                modifier = Modifier.fillMaxWidth().weight(1f).background(chaosColor)
                            ) {
                                Text(
                                    "Team CHAOS",
                                    modifier = Modifier.fillMaxWidth().background(chaosColor.darken(contrast / 2))
                                        .padding(4.dp),
                                    textAlign = TextAlign.Center
                                )
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalArrangement = Arrangement.SpaceAround
                                ) {
                                    game.getPlayers(Team.CHAOS).forEach { summoner ->
                                        SummonerComposable(
                                            summoner,
                                            summoner.kda.total,
                                            chaosColor.darken(contrast / 3),
                                            game.summonerSelected
                                        ) {
                                            game.summonerSelected = it
                                        }
                                    }
                                }
                            }
                        }
                    }
                    UICard(
                        modifier = Modifier.fillMaxWidth(),
                        title = "Settings",
                        titleColor = mainColor.darken(contrast / 2)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                            ColorPickerWidget(
                                mainColor,
                                onColorChange = { colorPicked ->
                                    mainColor = colorPicked
                                },
                                setContrast = {
                                    CoroutineScope(Dispatchers.Default).launch {
                                        contrast = it
                                    }
                                },
                                currentContrast = contrast,
                                modifier = Modifier.widthIn(max = 300.dp)
                            )
                        }
                    }
                }
                VerticalScrollbar(
                    modifier = Modifier.align(Alignment.CenterEnd),
                    adapter = rememberScrollbarAdapter(verticalScrollState),
                    style = defaultScrollbarStyle().copy(
                        unhoverColor = mainColor.darken(contrast / 2).copy(alpha = 0.5f),
                        hoverColor = mainColor.darken(contrast / 2)
                    )
                )
            }
            Box(Modifier.fillMaxWidth().weight(1f)) {
                val verticalScrollState = rememberScrollState()
                Column(Modifier.fillMaxWidth().verticalScroll(verticalScrollState)) {
                    UICard(title = "Events", titleColor = mainColor.darken(contrast / 2)) {
                        Column() {
                            game.events.forEach { event ->
                                EventComposable(event, chaosColor, orderColor, game.summonerSelected)
                            }
                        }
                    }
                    UICard(
                        modifier = Modifier.height(500.dp),
                        titleColor = mainColor.darken(contrast / 2),
                        title = "Kill difference",
                        noPadding = true
                    ) {
                        Column(Modifier.background(orderColor)) {
                            Box(modifier = Modifier.fillMaxWidth().background(orderColor).weight(1f))
                            Box(modifier = Modifier.fillMaxWidth().background(chaosColor).weight(1f))
                        }
                        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                            val canvasHeight = constraints.maxHeight
                            val canvasWidth = constraints.maxWidth
                            val centerOffset = Offset(0f, canvasHeight / 2f)

                            val killWidth = canvasWidth / game.killDifference.size
                            val killHeight = (canvasHeight - 100) / 2 / game.maxKillDifference

                            for (i in 0 until game.killDifference.lastIndex) {
                                Surface(
                                    shape = CircleShape,
                                    color = mainColor,
                                    border = BorderStroke(2.dp, mainColor.darken(contrast)),
                                    modifier = Modifier.size(12.dp).offset {
                                        (Offset(
                                            (killWidth * i).toFloat(),
                                            (killHeight * game.killDifference[i]).toFloat()
                                        ) + centerOffset + with(localDensity) {
                                            Offset(
                                                -3.dp.toPx(),
                                                -3.dp.toPx()
                                            )
                                        }).round()
                                    }) {}
                            }
                        }
                    }
                }
                VerticalScrollbar(
                    modifier = Modifier.align(Alignment.CenterEnd),
                    adapter = rememberScrollbarAdapter(verticalScrollState),
                    style = defaultScrollbarStyle().copy(
                        unhoverColor = mainColor.darken(contrast / 2).copy(alpha = 0.5f),
                        hoverColor = mainColor.darken(contrast / 2)
                    )
                )
            }
        }
    }
}

fun main() = application {
    val windowState = rememberWindowState(width = Dp.Unspecified, height = Dp.Unspecified)
    Window(
        onCloseRequest = ::exitApplication,
        state = windowState,
        title = "LeagueOfInfo",
    ) {
        val game by remember { mutableStateOf(Game()) }
        App(game, windowState.size)
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

@Composable
fun UICard(
    modifier: Modifier = Modifier,
    titleColor: Color,
    title: String,
    noPadding: Boolean = false,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.padding(bottom = 8.dp, end = 12.dp, start = 4.dp).fillMaxWidth(),
        backgroundColor = mainColor,
        elevation = 4.dp
    ) {
        Column {
            Text(
                title,
                modifier = Modifier.fillMaxWidth().background(titleColor)
                    .padding(4.dp),
                textAlign = TextAlign.Center
            )
            Box(modifier = if (noPadding) Modifier else Modifier.padding(8.dp)) {
                content()
            }
        }
    }
}