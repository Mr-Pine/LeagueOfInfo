// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.*
import color_picker.ColorPickerWidget
import design.darken
import design.getLegibleTextColor
import design.withLightness
import info_elements.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.skia.Image.Companion.makeFromEncoded
import settings.SettingsSaver
import java.awt.Desktop
import java.awt.Point
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import javax.imageio.ImageIO
import kotlin.math.sqrt


@OptIn(ExperimentalFoundationApi::class)
@Composable
@Preview
fun App(
    game: Game,
    windowSize: DpSize,
    openWindow: (title: String, @Composable () -> Unit) -> Unit,
    nextGame: () -> Unit
) {
    var contrast by remember { mutableStateOf(0.5f) }
    val darkColor by remember(mainColor, contrast) { mutableStateOf(mainColor.darken(contrast)) }

    var switchTeamColors by remember { mutableStateOf(true) }

    val baseOrderColor = Color(0xFF0A96AA)
    val baseChaosColor = Color(0xFFBE1E37)

    val orderColor by remember(
        mainColor,
        switchTeamColors,
        game.activeTeam
    ) {
        mutableStateOf(
            (if (switchTeamColors && game.activeTeam == Team.CHAOS) baseChaosColor else baseOrderColor).withLightness(
                sqrt(sqrt(mainColor.luminance()))
            )
        )
    }
    val chaosColor by remember(
        mainColor,
        switchTeamColors,
        game.activeTeam
    ) {
        mutableStateOf(
            (if (switchTeamColors && game.activeTeam == Team.CHAOS) baseOrderColor else baseChaosColor).withLightness(
                sqrt(sqrt(mainColor.luminance()))
            )
        )
    }

    val settingsSaver = remember {
        SettingsSaver(setColor = { mainColor = it },
            setContrast = { contrast = it },
            setSwitchColors = { switchTeamColors = it })
    }

    val localDensity = LocalDensity.current

    val appInfo by remember(contrast, orderColor, chaosColor, game, openWindow) {
        mutableStateOf(
            AppInfo(
                contrast,
                { contrast = it },
                orderColor,
                chaosColor,
                game,
                openWindow,
                settingsSaver,
                switchTeamColors,
                localDensity,
                { switchTeamColors = it })
        )
    }

    MaterialTheme {
        Column(
            modifier = Modifier.background(darkColor).padding(top = 16.dp, bottom = 8.dp, start = 12.dp, end = 4.dp)
                .then(if (windowSize.isSpecified) Modifier.size(windowSize) else Modifier),
        ) {
            if (game.isFinished) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp, end = 12.dp, start = 4.dp),
                    backgroundColor = mainColor
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        Button(
                            onClick = nextGame,
                            modifier = Modifier.padding(16.dp),
                            colors = ButtonDefaults.buttonColors(backgroundColor = mainColor.darken(contrast / 2))
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Game finished")
                                Text("Detect new game")
                            }
                        }
                    }
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                UIColumn(contrast) {
                    playerList.content(appInfo, false) { playerList.content(appInfo, true, null)}
                    settings.content(appInfo, false) { settings.content(appInfo, true, null) }
                }
                UIColumn(contrast) {
                    laneKDA.content(appInfo, false) { laneKDA.content(appInfo, true, null) }
                    oneVsAllKDA.content(appInfo, false) { oneVsAllKDA.content(appInfo, true, null) }
                }
                UIColumn(contrast, 0.6f) {
                    events.content(appInfo, false) { events.content(appInfo, true, null)}
                    killDifferenceGraph.content(appInfo, false) { killDifferenceGraph.content(appInfo, true, null)}
                }
            }
        }
    }
}

fun main() = application {
    val windowState = rememberWindowState(
        width = Dp.Unspecified,
        height = Dp.Unspecified,
        placement = WindowPlacement.Maximized
    )
    val singleWindows = remember { mutableStateMapOf<@Composable () -> Unit, String>() }
    val singleWindowStates = remember { mutableStateMapOf<String, MutableState<WindowState>>() }
    Window(
        onCloseRequest = ::exitApplication,
        state = windowState,
        title = "LeagueOfInfo",
        icon = painterResource("logo.svg")
    ) {
        var gameNumber by remember { mutableStateOf(0) }
        val game by remember(gameNumber) { mutableStateOf(Game()) }
        App(
            game,
            windowState.size,
            openWindow = { title, content ->
                if(singleWindowStates.contains(title)){
                    singleWindowStates[title]!!.value.isMinimized = false
                } else {
                    singleWindows[content] = title
                    singleWindowStates[title] = mutableStateOf(
                        WindowState(
                            placement = WindowPlacement.Floating, position = WindowPosition(
                                Alignment.Center
                            )
                        )
                    )
                }
            }
        ) { gameNumber++ }
    }

    singleWindows.forEach { (single, title) ->
        Window(
            onCloseRequest = { singleWindows.remove(single); singleWindowStates.remove(title) },
            title = title,
            state = singleWindowStates[title]!!.value,
            icon = painterResource("logo.svg")
        ) {
            Box(
                modifier = Modifier.background(mainColor.darken())
                    .then(if (singleWindowStates[title]!!.value.size.isSpecified) Modifier.size(singleWindowStates[title]!!.value.size) else Modifier)
            ) {
                single()
            }
        }
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

@ExperimentalFoundationApi
@Composable
fun UICard(
    modifier: Modifier = Modifier,
    titleColor: Color,
    title: String,
    noPadding: Boolean = false,
    separable: Boolean = true,
    noOutsidePadding: Boolean = false,
    openPopup: () -> Unit,
    content: @Composable () -> Unit
) {
    var showPopup by remember { mutableStateOf(false) }
    var cardLayout by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var latestMousePosition by remember { mutableStateOf(Point()) }
    val mouseOffset by remember(cardLayout, latestMousePosition) {
        mutableStateOf(
            cardLayout?.windowToLocal(
                Offset(
                    latestMousePosition.x + 20f, latestMousePosition.y + 10f
                )
            ) ?: Offset(0f, 0f)
        )
    }


    Card(
        modifier = modifier.then(if (separable) Modifier.onGloballyPositioned { layoutCoordinates ->
            cardLayout = layoutCoordinates
        } else Modifier)
            .then(if (noOutsidePadding) Modifier else Modifier.padding(bottom = 8.dp, end = 12.dp, start = 4.dp))
            .fillMaxWidth()
            .then(if (separable) Modifier.pointerInput(Unit) {
                while (true) {
                    val lastMouseEvent = awaitPointerEventScope { awaitPointerEvent() }.mouseEvent
                    if (lastMouseEvent != null && lastMouseEvent.isPopupTrigger) {
                        showPopup = true
                        latestMousePosition = lastMouseEvent.point
                    }
                }
            } else Modifier),
        backgroundColor = mainColor,
        elevation = 4.dp
    ) {
        DropdownMenu(
            expanded = showPopup,
            onDismissRequest = { showPopup = false },
            offset = with(LocalDensity.current) {
                DpOffset(
                    mouseOffset.x.toDp(),
                    -(cardLayout?.size?.height ?: 0).toDp() + mouseOffset.y.toDp()
                )
            }) {
            DropdownMenuItem(onClick = { println("show single"); openPopup(); showPopup = false }) {
                Text("show separated")
            }
        }
        Column {
            Text(
                title,
                modifier = Modifier.fillMaxWidth().background(titleColor)
                    .padding(4.dp),
                textAlign = TextAlign.Center,
                color = titleColor.getLegibleTextColor()
            )
            Box(modifier = if (noPadding) Modifier else Modifier.padding(8.dp)) {
                content()
            }
        }
    }
}

@Composable
fun RowScope.UIColumn(contrast: Float, weight: Float = 1f, content: @Composable ColumnScope.() -> Unit) {
    Box(Modifier.fillMaxWidth().weight(weight)) {
        val verticalScrollState = rememberScrollState()
        Column(Modifier.fillMaxWidth().verticalScroll(verticalScrollState)) {
            content()
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

fun openWebpage(uri: URI): Boolean {
    val desktop = if (Desktop.isDesktopSupported()) Desktop.getDesktop() else null
    if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE)) {
        try {
            desktop.browse(uri)
            return true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    return false
}

data class AppInfo(
    val contrast: Float,
    val setContrast: (Float) -> Unit,
    val orderColor: Color,
    val chaosColor: Color,
    val game: Game,
    val openWindow: (title: String, @Composable () -> Unit) -> Unit,
    val settingsSaver: SettingsSaver,
    val switchTeamColors: Boolean,
    val localDensity: Density,
    val setSwitchTeamColors: (Boolean) -> Unit
) {
}