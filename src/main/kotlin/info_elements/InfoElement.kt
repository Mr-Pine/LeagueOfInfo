package info_elements

import AppInfo
import DropdownSummoner
import EventComposable
import Game
import KDAData
import SummonerComposable
import UICard
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import color_picker.ColorPickerWidget
import design.darken
import design.getLegibleTextColor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import mainColor
import openWebpage
import java.net.URI

data class InfoElement(
    val name: String,
    val content: @Composable (appInfo: AppInfo, isSeparated: Boolean, singleContent: (@Composable () -> Unit)?) -> Unit,
)

val playerList = InfoElement("Player List") @Composable { appInfo, _, _ ->
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp, end = 12.dp, start = 4.dp),
        elevation = 4.dp, backgroundColor = mainColor
    ) {
        Row() {
            Column(
                modifier = Modifier.fillMaxWidth().weight(1f).background(appInfo.orderColor)
            ) {
                Text(
                    "Team ORDER",
                    modifier = Modifier.fillMaxWidth().background(appInfo.orderColor.darken(appInfo.contrast / 2))
                        .padding(4.dp),
                    textAlign = TextAlign.Center,
                    color = appInfo.orderColor.darken(appInfo.contrast / 2).getLegibleTextColor()
                )
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {
                    appInfo.game.getPlayers(Team.ORDER).forEach { summoner ->
                        SummonerComposable(
                            summoner,
                            summoner.kda.total,
                            appInfo.orderColor.darken(appInfo.contrast / 3),
                            appInfo.game.summonerSelected
                        ) {
                            appInfo.game.summonerSelected = it
                        }
                    }
                }
            }
            Column(
                modifier = Modifier.fillMaxWidth().weight(1f).background(appInfo.chaosColor)
            ) {
                Text(
                    "Team CHAOS",
                    modifier = Modifier.fillMaxWidth().background(appInfo.chaosColor.darken(appInfo.contrast / 2))
                        .padding(4.dp),
                    textAlign = TextAlign.Center,
                    color = appInfo.chaosColor.darken(appInfo.contrast / 2).getLegibleTextColor()
                )
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.SpaceAround
                ) {
                    appInfo.game.getPlayers(Team.CHAOS).forEach { summoner ->
                        SummonerComposable(
                            summoner,
                            summoner.kda.total,
                            appInfo.chaosColor.darken(appInfo.contrast / 3),
                            appInfo.game.summonerSelected
                        ) {
                            appInfo.game.summonerSelected = it
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
val settings = InfoElement("Settings") @Composable { appInfo, isSeparated, singleContent ->
    UICard(
        modifier = Modifier.fillMaxWidth(),
        title = "Settings",
        titleColor = mainColor.darken(appInfo.contrast / 1.5f),
        openPopup = {
            appInfo.openWindow("Settings") @Composable {
                if (singleContent != null) {
                    singleContent()
                }
            }
        },
        separable = (singleContent != null),
        noOutsidePadding = isSeparated
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            ColorPickerWidget(
                mainColor,
                onColorChange = { colorPicked ->
                    mainColor = colorPicked
                    appInfo.settingsSaver.saveSettings { settings ->
                        settings.colors.mainColorInt = colorPicked.value
                        return@saveSettings settings
                    }
                },
                setContrast = {
                    CoroutineScope(Dispatchers.Default).launch {
                        appInfo.setContrast(it)
                        appInfo.settingsSaver.saveSettings { settings ->
                            settings.colors.contrast = it
                            return@saveSettings settings
                        }
                    }
                },
                currentContrast = appInfo.contrast,
                modifier = Modifier.widthIn(max = 300.dp)
            )
            Spacer(Modifier.height(16.dp))
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(4.dp).clip(RoundedCornerShape(8.dp))
                        .background(mainColor.darken(appInfo.contrast / 1.5f)).padding(12.dp)
                        .padding(start = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Invert team colors if you are in Team Chaos",
                        color = mainColor.darken(appInfo.contrast / 1.5f).getLegibleTextColor()
                    )
                    Switch(
                        appInfo.switchTeamColors,
                        onCheckedChange = {
                            appInfo.setSwitchTeamColors(it)
                            appInfo.settingsSaver.saveSettings { settings ->
                                settings.switchTeamColors = it; return@saveSettings settings
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = mainColor,
                            checkedTrackColor = mainColor.darken(appInfo.contrast / 4),
                            uncheckedThumbColor = mainColor.darken(appInfo.contrast),
                            uncheckedTrackColor = mainColor.darken(2 * appInfo.contrast)
                        )
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Button(
                    onClick = { openWebpage(URI.create("https://github.com/Mr-Pine/LeagueOfInfo/issues/new?assignees=Mr-Pine&labels=bug&template=bug_report.md&title=")) },
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = mainColor.darken(appInfo.contrast / 2),
                        contentColor = mainColor.darken(appInfo.contrast / 2).getLegibleTextColor()
                    )
                ) {
                    Text("Report Bug")
                }
                Button(
                    onClick = { openWebpage(URI.create("https://github.com/Mr-Pine/LeagueOfInfo/issues/new?assignees=Mr-Pine&labels=enhancement&template=feature_request.md&title=")) },
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = mainColor.darken(appInfo.contrast / 2),
                        contentColor = mainColor.darken(appInfo.contrast / 2).getLegibleTextColor()
                    )
                ) {
                    Text("Suggest Feature")
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
val laneKDA = InfoElement("Lane KDA") @Composable { appInfo, isSeparated, singleContent ->
    UICard(
        title = "Lane KDA", titleColor = mainColor.darken(appInfo.contrast / 1.5f),
        openPopup = {
            appInfo.openWindow("Lane KDA") {
                if (singleContent != null) {
                    singleContent()
                }
            }
        },
        separable = (singleContent != null),
        noOutsidePadding = isSeparated) {
        Column(Modifier.padding(vertical = 4.dp), verticalArrangement = Arrangement.SpaceBetween) {
            val orderPlayers = appInfo.game.getPlayers(Team.ORDER)
            val chaosPlayers = appInfo.game.getPlayers(Team.CHAOS)
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
                    Column(Modifier.fillMaxWidth().background(appInfo.orderColor).padding(8.dp).weight(1f)) {
                        SummonerComposable(
                            summoner = orderPlayer,
                            kda = orderKDA,
                            backgroundColor = appInfo.orderColor.darken(appInfo.contrast / 3),
                            summonerSelected = appInfo.game.summonerSelected,
                            onSummonerSelect = { appInfo.game.summonerSelected = it }
                        )
                    }
                    Column(Modifier.fillMaxWidth().background(appInfo.chaosColor).padding(8.dp).weight(1f)) {
                        SummonerComposable(
                            summoner = chaosPlayer,
                            kda = chaosKDA,
                            backgroundColor = appInfo.chaosColor.darken(appInfo.contrast / 3),
                            summonerSelected = appInfo.game.summonerSelected,
                            onSummonerSelect = { appInfo.game.summonerSelected = it }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
val oneVsAllKDA = InfoElement("One vs all KDA") @Composable { appInfo, isSeparated, singleContent ->
    UICard(
        title = "One vs all KDA", titleColor = mainColor.darken(appInfo.contrast / 1.5f),
        openPopup = {
            appInfo.openWindow("One vs all KDA") @Composable {
                if (singleContent != null) {
                    singleContent()
                }
            }
        },
        separable = (singleContent != null),
        noOutsidePadding = isSeparated
    ) {
        Column(
            Modifier.fillMaxWidth().padding(vertical = 4.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val allSummoners = appInfo.game.getPlayers(Team.UNKNOWN)
            if (allSummoners.any { it.name == appInfo.game.summonerSelected }) {
                var selectedOne by remember { mutableStateOf(allSummoners.last { it.name == appInfo.game.summonerSelected }) }
                var expanded by remember { mutableStateOf(false) }
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SummonerComposable(
                        summoner = selectedOne,
                        kda = selectedOne.kda.total,
                        backgroundColor = (if (selectedOne.team == Team.ORDER) appInfo.orderColor else appInfo.chaosColor).darken(
                            appInfo.contrast / 3
                        ),
                        summonerSelected = appInfo.game.summonerSelected,
                        modifier = Modifier.widthIn(max = 800.dp, min = 300.dp).fillMaxWidth(0.5f)
                    ) {
                        expanded = true
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.width(400.dp).background(mainColor.darken(appInfo.contrast / 2))
                            .clip(
                                RoundedCornerShape(8.dp)
                            )
                    ) {
                        allSummoners.forEach { summoner ->
                            Row(Modifier.widthIn(min = 300.dp, max = 800.dp)) {
                                DropdownSummoner(
                                    summoner,
                                    kda = summoner.kda.total,
                                    backgroundColor = (if (summoner.team == Team.ORDER) appInfo.orderColor else appInfo.chaosColor).darken(
                                        appInfo.contrast / 3
                                    ),
                                    summonerSelected = appInfo.game.summonerSelected,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                ) {
                                    selectedOne = summoner
                                    expanded = false
                                }
                            }
                        }
                    }
                }

                appInfo.game.getPlayers(if (selectedOne.team == Team.ORDER) Team.CHAOS else Team.ORDER)
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
                                    backgroundColor = appInfo.orderColor.darken(appInfo.contrast / 3),
                                    summonerSelected = appInfo.game.summonerSelected,
                                    onSummonerSelect = { appInfo.game.summonerSelected = it }
                                )
                            }
                            Column(
                                Modifier.fillMaxWidth().padding(8.dp).weight(1f)
                            ) {
                                SummonerComposable(
                                    summoner = enemy,
                                    kda = enemyKDA,
                                    backgroundColor = appInfo.chaosColor.darken(appInfo.contrast / 3),
                                    summonerSelected = appInfo.game.summonerSelected,
                                    onSummonerSelect = { appInfo.game.summonerSelected = it }
                                )
                            }
                        }
                    }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
val events = InfoElement("Events") @Composable { appInfo, isSeparated, singleContent ->
    UICard(
        title = "Events",
        titleColor = mainColor.darken(appInfo.contrast / 1.5f),
        openPopup = {
            appInfo.openWindow("Events") @Composable {
                if (singleContent != null) {
                    singleContent()
                }
            }
        },
        separable = (singleContent != null),
        noOutsidePadding = isSeparated) {
        Column {
            appInfo.game.events.forEach { event ->
                EventComposable(event, appInfo.chaosColor, appInfo.orderColor, appInfo.game.summonerSelected)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
val killDifferenceGraph = InfoElement("Kill Difference") { appInfo, isSeparated, singleContent ->
    UICard(
        modifier = Modifier.height(500.dp),
        titleColor = mainColor.darken(appInfo.contrast / 1.5f),
        title = "Kill difference",
        noPadding = true,
        openPopup = {
            appInfo.openWindow("Kill Difference") @Composable {
                if (singleContent != null) {
                    singleContent()
                }
            }
        },
        separable = (singleContent != null),
        noOutsidePadding = isSeparated
    ) {
        Column(Modifier.background(appInfo.orderColor)) {
            Box(modifier = Modifier.fillMaxWidth().background(appInfo.orderColor).weight(1f))
            Box(modifier = Modifier.fillMaxWidth().background(appInfo.chaosColor).weight(1f))
        }
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val canvasHeight = constraints.maxHeight
            val canvasWidth = constraints.maxWidth
            val centerOffset = Offset(0f, canvasHeight / 2f)

            val killWidth = canvasWidth / appInfo.game.killDifference.size
            val killHeight = (canvasHeight - 50) / 2 / appInfo.game.maxKillDifference

            for (i in 0 until appInfo.game.killDifference.size) {
                Surface(
                    shape = CircleShape,
                    color = mainColor,
                    border = BorderStroke(2.dp, mainColor.darken(appInfo.contrast)),
                    modifier = Modifier.size(12.dp).offset {
                        (Offset(
                            (killWidth * i).toFloat(),
                            -(killHeight * appInfo.game.killDifference[i]).toFloat()
                        ) + centerOffset + with(appInfo.localDensity) {
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