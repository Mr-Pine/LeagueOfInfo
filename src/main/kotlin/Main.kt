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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.useResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import color_picker.ColorPickerWidget
import design.darken
import design.getLegibleTextColor
import design.withLightness
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import org.jetbrains.skia.Image.Companion.makeFromEncoded
import settings.SettingsSaver
import java.awt.Desktop
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import javax.imageio.ImageIO
import kotlin.math.sqrt


@Composable
@Preview
fun App(game: Game, windowSize: DpSize, nextGame: () -> Unit) {
    var text by remember { mutableStateOf("Hello, World!") }
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
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp, end = 12.dp, start = 4.dp),
                        elevation = 4.dp, backgroundColor = mainColor
                    ) {
                        Row() {
                            Column(
                                modifier = Modifier.fillMaxWidth().weight(1f).background(orderColor)
                            ) {
                                Text(
                                    "Team ORDER",
                                    modifier = Modifier.fillMaxWidth().background(orderColor.darken(contrast / 2))
                                        .padding(4.dp),
                                    textAlign = TextAlign.Center,
                                    color = orderColor.darken(contrast / 2).getLegibleTextColor()
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
                                    textAlign = TextAlign.Center,
                                    color = chaosColor.darken(contrast / 2).getLegibleTextColor()
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
                        titleColor = mainColor.darken(contrast / 1.5f)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                            ColorPickerWidget(
                                mainColor,
                                onColorChange = { colorPicked ->
                                    mainColor = colorPicked
                                    settingsSaver.saveSettings { settings ->
                                        settings.colors.mainColorInt = colorPicked.value
                                        return@saveSettings settings
                                    }
                                },
                                setContrast = {
                                    CoroutineScope(Dispatchers.Default).launch {
                                        contrast = it
                                        settingsSaver.saveSettings { settings ->
                                            settings.colors.contrast = it
                                            return@saveSettings settings
                                        }
                                    }
                                },
                                currentContrast = contrast,
                                modifier = Modifier.widthIn(max = 300.dp)
                            )
                            Spacer(Modifier.height(16.dp))
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(4.dp).clip(RoundedCornerShape(8.dp))
                                        .background(mainColor.darken(contrast / 1.5f)).padding(12.dp)
                                        .padding(start = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Invert team colors if you are in Team Chaos",color = mainColor.darken(contrast / 1.5f).getLegibleTextColor())
                                    Switch(
                                        switchTeamColors,
                                        onCheckedChange = {
                                            switchTeamColors = it
                                            settingsSaver.saveSettings { settings ->
                                                settings.switchTeamColors = it; return@saveSettings settings
                                            }
                                        },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = mainColor,
                                            checkedTrackColor = mainColor.darken(contrast / 4),
                                            uncheckedThumbColor = mainColor.darken(contrast),
                                            uncheckedTrackColor = mainColor.darken(2 * contrast)
                                        )
                                    )
                                }
                            }
                            Spacer(Modifier.height(16.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                Button(
                                    onClick = { openWebpage(URI.create("https://github.com/Mr-Pine/LeagueOfInfo/issues/new?assignees=Mr-Pine&labels=bug&template=bug_report.md&title=")) },
                                    colors = ButtonDefaults.buttonColors(backgroundColor = mainColor.darken(contrast / 2), contentColor = mainColor.darken(contrast / 2).getLegibleTextColor())
                                ) {
                                    Text("Report Bug")
                                }
                                Button(
                                    onClick = { openWebpage(URI.create("https://github.com/Mr-Pine/LeagueOfInfo/issues/new?assignees=Mr-Pine&labels=enhancement&template=feature_request.md&title=")) },
                                    colors = ButtonDefaults.buttonColors(backgroundColor = mainColor.darken(contrast / 2), contentColor = mainColor.darken(contrast / 2).getLegibleTextColor())
                                ) {
                                    Text("Suggest Feature")
                                }
                            }
                        }
                    }
                }
                UIColumn(contrast) {
                    UICard(title = "Lane KDA", titleColor = mainColor.darken(contrast / 1.5f)) {
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
                    UICard(title = "One vs all KDA", titleColor = mainColor.darken(contrast / 1.5f)) {
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
                    UICard(title = "Events", titleColor = mainColor.darken(contrast / 1.5f)) {
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
    val windowState = rememberWindowState(width = Dp.Unspecified, height = Dp.Unspecified)
    Window(
        onCloseRequest = ::exitApplication,
        state = windowState,
        title = "LeagueOfInfo",
        icon = painterResource("logo.svg")
    ) {
        var gameNumber by remember { mutableStateOf(0) }
        val game by remember(gameNumber) { mutableStateOf(Game()) }
        App(game, windowState.size) { gameNumber++ }
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

