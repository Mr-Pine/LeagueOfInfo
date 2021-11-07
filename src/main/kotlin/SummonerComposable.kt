import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

@Composable
fun SummonerComposable(
    summoner: Summoner,
    kda: KDAData,
    backgroundColor: Color,
    summonerSelected: String,
    modifier: Modifier = Modifier,
    onSummonerSelect: (String) -> Unit
) {
    BoxWithConstraints(Modifier.padding(vertical = 4.dp).then(modifier).fillMaxWidth()
        .clip(RoundedCornerShape(8.dp))
        .clickable { onSummonerSelect(summoner.name) }
        .background(backgroundColor).then(
            if (summoner.name == summonerSelected) Modifier.border(
                width = 2.dp,
                color = Color.Yellow,
                RoundedCornerShape(8.dp)
            ) else Modifier
        ).padding(8.dp), contentAlignment = Alignment.Center) {

        val boxConstraints = constraints

        var kdaSize by remember { mutableStateOf(IntSize(0, 0)) }
        Row(
            modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically
        ) {
            val championIcon = summoner.entity.championIcon
            if (championIcon != null) {
                Image(
                    championIcon, "image",
                    modifier = Modifier.height(50.dp)
                )
            } else {
                summoner.entity.getIcon()
            }
            Spacer(Modifier.width(8.dp))
            Column(
                verticalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.padding(end = with(LocalDensity.current) { kdaSize.width.toDp() })
            ) {
                val textStyleBody1 = MaterialTheme.typography.body1
                var textStyle by remember (boxConstraints.maxWidth) { mutableStateOf(textStyleBody1) }
                var readyToDraw by remember (boxConstraints.maxWidth) { mutableStateOf(false) }
                println(boxConstraints)
                Text(summoner.name, modifier = Modifier.drawWithContent {
                    if (readyToDraw) drawContent()
                }, onTextLayout = { textLayoutResult ->
                        if (textLayoutResult.didOverflowWidth) {
                            textStyle = textStyle.copy(fontSize = textStyle.fontSize * 0.9)
                        } else {
                            readyToDraw = true
                        }
                    }, style = textStyle, softWrap = false, maxLines = 1)
                Text(summoner.championDisplayName)
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Text(
                "${kda.kills} / ${kda.deaths} / ${kda.assists}",
                modifier = Modifier.onGloballyPositioned { kdaSize = it.size },
                textAlign = TextAlign.End
            )
        }
    }
}

@Composable fun DropdownSummoner(summoner: Summoner,
    kda: KDAData,
    backgroundColor: Color,
    summonerSelected: String,
    modifier: Modifier = Modifier,
    onSummonerSelect: (String) -> Unit
) {
    Box(Modifier.padding(vertical = 4.dp).then(modifier).fillMaxWidth()
        .clip(RoundedCornerShape(8.dp))
        .clickable { onSummonerSelect(summoner.name) }
        .background(backgroundColor).then(
            if (summoner.name == summonerSelected) Modifier.border(
                width = 2.dp,
                color = Color.Yellow,
                RoundedCornerShape(8.dp)
            ) else Modifier
        ).padding(8.dp), contentAlignment = Alignment.Center) {

        var kdaSize by remember { mutableStateOf(IntSize(0, 0)) }
        Row(
            modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically
        ) {
            val championIcon = summoner.entity.championIcon
            if (championIcon != null) {
                Image(
                    championIcon, "image",
                    modifier = Modifier.height(50.dp)
                )
            } else {
                summoner.entity.getIcon()
            }
            Spacer(Modifier.width(8.dp))
            Column(
                verticalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.padding(end = with(LocalDensity.current) { kdaSize.width.toDp() })
            ) {
                Text(summoner.name)
                Text(summoner.championDisplayName)
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Text(
                "${kda.kills} / ${kda.deaths} / ${kda.assists}",
                modifier = Modifier.onGloballyPositioned { kdaSize = it.size },
                textAlign = TextAlign.End
            )
        }
    }
}