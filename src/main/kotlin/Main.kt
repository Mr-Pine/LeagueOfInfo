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
import info_elements.playerList
import info_elements.settings
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
                    playerList.content(appInfo, false, null)
                    settings.content(appInfo, false) { settings.content(appInfo, true, null) }
                }
                UIColumn(contrast) {
                    UICard(title = "Lane KDA", titleColor = mainColor.darken(contrast / 1.5f),
                        openPopup = { openWindow("hi") { Text("hi") } }) {
                        Column(Modifier.padding(vertical = 4.dp), verticalArrangement = Arrangement.SpaceBetween) {
                            val orderPlayers = game.getPlayers(Team.ORDER)
                            val chaosPlayers = game.getPlayers(Team.CHAOS)
                            for (i in 0..orderPlayers.lastIndex) {
                                val orderPlayer = orderPlayers[i]
                                if (i > chaosPlayers.lastIndex) continue
                                val chaosPlayer = chaosPlayers[i]
                                val orderKDA = orderPlayer.kda.against[chaosPlayer.name] ?: KDAData()
                                val chaosKDA = chaosPlayer.kda.against[orderPlayer.name] ?: KDAData()
                                Row(
                                    Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                ) {
                                    Column(Modifier.fillMaxWidth().background(orderColor).padding(8.dp).weight(1f)) {
                                        SummonerComposable(
                                            summoner = orderPlayer,
                                            kda = orderKDA,
                                            backgroundColor = orderColor.darken(contrast / 3),
                                            summonerSelected = game.summonerSelected,
                                            onSummonerSelect = { game.summonerSelected = it }
                                        )
                                    }
                                    Column(Modifier.fillMaxWidth().background(chaosColor).padding(8.dp).weight(1f)) {
                                        SummonerComposable(
                                            summoner = chaosPlayer,
                                            kda = chaosKDA,
                                            backgroundColor = chaosColor.darken(contrast / 3),
                                            summonerSelected = game.summonerSelected,
                                            onSummonerSelect = { game.summonerSelected = it }
                                        )
                                    }
                                }
                            }
                        }
                    }
                    UICard(title = "One vs all KDA", titleColor = mainColor.darken(contrast / 1.5f),
                        openPopup = { openWindow("hi") { Text("hi") } }) {
                        Column(
                            Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalArrangement = Arrangement.SpaceBetween,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            val allSummoners = game.getPlayers(Team.UNKNOWN)
                            if (allSummoners.any { it.name == game.summonerSelected }) {
                                var selectedOne by remember { mutableStateOf(allSummoners.last { it.name == game.summonerSelected }) }
                                var expanded by remember { mutableStateOf(false) }
                                Row(
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    SummonerComposable(
                                        summoner = selectedOne,
                                        kda = selectedOne.kda.total,
                                        backgroundColor = (if (selectedOne.team == Team.ORDER) orderColor else chaosColor).darken(
                                            contrast / 3
                                        ),
                                        summonerSelected = game.summonerSelected,
                                        modifier = Modifier.widthIn(max = 800.dp, min = 300.dp).fillMaxWidth(0.5f)
                                    ) {
                                        expanded = true
                                    }
                                    DropdownMenu(
                                        expanded = expanded,
                                        onDismissRequest = { expanded = false },
                                        modifier = Modifier.width(400.dp).background(mainColor.darken(contrast / 2))
                                            .clip(
                                                RoundedCornerShape(8.dp)
                                            )
                                    ) {
                                        allSummoners.forEach { summoner ->
                                            Row(Modifier.widthIn(min = 300.dp, max = 800.dp)) {
                                                DropdownSummoner(
                                                    summoner,
                                                    kda = summoner.kda.total,
                                                    backgroundColor = (if (summoner.team == Team.ORDER) orderColor else chaosColor).darken(
                                                        contrast / 3
                                                    ),
                                                    summonerSelected = game.summonerSelected,
                                                    modifier = Modifier.padding(horizontal = 8.dp)
                                                ) {
                                                    selectedOne = summoner
                                                    expanded = false
                                                }
                                            }
                                        }
                                    }
                                }

                                game.getPlayers(if (selectedOne.team == Team.ORDER) Team.CHAOS else Team.ORDER)
                                    .forEach { enemy ->
                                        val selectedKDA = selectedOne.kda.against[enemy.name] ?: KDAData()
                                        val enemyKDA = enemy.kda.against[selectedOne.name] ?: KDAData()
                                        Row(
                                            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)
                                                .clip(RoundedCornerShape(8.dp)),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(
                                                Modifier.fillMaxWidth().padding(8.dp).weight(1f)
                                            ) {
                                                SummonerComposable(
                                                    summoner = selectedOne,
                                                    kda = selectedKDA,
                                                    backgroundColor = orderColor.darken(contrast / 3),
                                                    summonerSelected = game.summonerSelected,
                                                    onSummonerSelect = { game.summonerSelected = it }
                                                )
                                            }
                                            Column(
                                                Modifier.fillMaxWidth().padding(8.dp).weight(1f)
                                            ) {
                                                SummonerComposable(
                                                    summoner = enemy,
                                                    kda = enemyKDA,
                                                    backgroundColor = chaosColor.darken(contrast / 3),
                                                    summonerSelected = game.summonerSelected,
                                                    onSummonerSelect = { game.summonerSelected = it }
                                                )
                                            }
                                        }
                                    }
                            }
                        }
                    }
                }
                UIColumn(contrast, 0.6f) {
                    UICard(title = "Events", titleColor = mainColor.darken(contrast / 1.5f),
                        openPopup = { openWindow("hi") { Text("hi") } }) {
                        Column {
                            game.events.forEach { event ->
                                EventComposable(event, chaosColor, orderColor, game.summonerSelected)
                            }
                        }
                    }
                    UICard(
                        modifier = Modifier.height(500.dp),
                        titleColor = mainColor.darken(contrast / 1.5f),
                        title = "Kill difference",
                        noPadding = true,
                        openPopup = { openWindow("hi") { Text("hi") } }
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
                            val killHeight = (canvasHeight - 50) / 2 / game.maxKillDifference

                            for (i in 0 until game.killDifference.size) {
                                Surface(
                                    shape = CircleShape,
                                    color = mainColor,
                                    border = BorderStroke(2.dp, mainColor.darken(contrast)),
                                    modifier = Modifier.size(12.dp).offset {
                                        (Offset(
                                            (killWidth * i).toFloat(),
                                            -(killHeight * game.killDifference[i]).toFloat()
                                        ) + centerOffset + with(localDensity) {
                                            Offset(
                                                -4.dp.toPx(),
                                                -4.dp.toPx()
                                            )
                                        }).round()
                                    }
                                ) {}
                            }
                        }
                    }
                }
            }
        }
    }
}

fun main() = application {
    val windowState = rememberWindowState(
        width = Dp.Unspecified,
        height = Dp.Unspecified,
        placement = WindowPlacement.Maximized,
        position = WindowPosition.PlatformDefault
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
                singleWindows[content] = title; singleWindowStates[title] = mutableStateOf(WindowState())
            }
        ) { gameNumber++ }
    }

    singleWindows.forEach { (single, title) ->
        Window(onCloseRequest = { singleWindows.remove(single); singleWindowStates.remove(title) }, title = title, state = singleWindowStates[title]!!.value) {
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
    val setSwitchTeamColors: (Boolean) -> Unit
)