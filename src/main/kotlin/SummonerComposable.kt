import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.dp

@Composable
fun SummonerComposable(
    summoner: Summoner,
    kda: KDAData,
    backgroundColor: Color,
    summonerSelected: String,
    onSummonerSelect: (String) -> Unit
) {
    Row(
        modifier = Modifier.padding(vertical = 4.dp).clickable { onSummonerSelect(summoner.name) }.clip(RoundedCornerShape(8.dp))
            .background(backgroundColor).then(
                if (summoner.name == summonerSelected) Modifier.border(
                    width = 2.dp,
                    color = Color.Yellow,
                    RoundedCornerShape(8.dp)
                ) else Modifier
            ).fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically
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
        Column(verticalArrangement = Arrangement.SpaceEvenly) {
            Text(summoner.name)
            Text(summoner.championDisplayName)
        }
        Spacer(Modifier.width(8.dp))
        Text("${kda.kills} / ${kda.deaths} / ${kda.assists}")
    }
}