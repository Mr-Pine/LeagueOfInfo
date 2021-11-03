import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SummonerComposable(summoner: Summoner, kda: KDAData) {
    Row() {
        summoner.entity.iconDraw(Modifier.height(40.dp))
        Column {
            Text(summoner.name)
            Text(summoner.championDisplayName)
        }
        Text("${kda.kills} / ${kda.deaths} / ${kda.assists}")
    }
}