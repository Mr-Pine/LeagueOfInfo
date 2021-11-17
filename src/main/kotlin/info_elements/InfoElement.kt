package info_elements

import AppInfo
import Game
import SummonerComposable
import UICard
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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