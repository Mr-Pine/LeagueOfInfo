import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

@Composable
fun EventComposable(event: ActionEvent, colorCHAOS: Color, colorORDER: Color, summonerSelected: String) {
    val team = if (event.killer.team != Team.UNKNOWN) event.killer.team else if (event.victim.team != Team.UNKNOWN) {
        if (event.victim.team == Team.CHAOS) Team.ORDER else Team.CHAOS
    } else Team.UNKNOWN
    Column(
        modifier = Modifier.padding(4.dp).clip(RoundedCornerShape(8.dp)).background(
            when (team) {
                Team.CHAOS -> colorCHAOS
                Team.ORDER -> colorORDER
                Team.UNKNOWN -> Color.Gray
            }
        ).padding(8.dp)
    ) {
        Row {
            val killerIcon = event.killer.icon
            if (killerIcon != null && killerIcon is ImageBitmap) {
                Image(killerIcon, "icon", modifier = Modifier.height(50.dp).then(if(summonerSelected == (event.killer as SummonerEntity).name) Modifier.border(2.dp, Color.Yellow) else Modifier))
            } else if (killerIcon != null && killerIcon is Painter) {
                Image(killerIcon, "icon", modifier = Modifier.height(50.dp))
            } else {
                Image(painterResource("Icons/missing_square.png"), "no icon found", modifier = Modifier.height(50.dp))
                event.killer.getIcon()
            }

            Spacer(modifier = Modifier.width(24.dp))

            val victimIcon = event.victim.icon
            if (victimIcon != null && victimIcon is ImageBitmap) {
                Image(victimIcon, "icon", modifier = Modifier.height(50.dp).then(if(summonerSelected == (event.victim as SummonerEntity).name) Modifier.border(2.dp, Color.Yellow) else Modifier))
            } else if (victimIcon != null && victimIcon is Painter) {
                Image(victimIcon, "icon", modifier = Modifier.height(50.dp))
            } else {
                Image(painterResource("Icons/missing_square.png"), "no icon found", modifier = Modifier.height(50.dp))
                event.victim.getIcon()
            }

        }
        Spacer(modifier = Modifier.height(4.dp))
        Row {
            event.assisters.forEach {
                val assisterIcon = it.entity.icon
                if (assisterIcon != null && assisterIcon is ImageBitmap) {
                    Image(assisterIcon, "icon", modifier = Modifier.height(20.dp).then(if(summonerSelected == it.name) Modifier.border(1.dp, Color.Yellow) else Modifier))
                } else if (assisterIcon != null && assisterIcon is Painter) {
                    Image(assisterIcon, "icon", modifier = Modifier.height(20.dp))
                } else {
                    Image(
                        painterResource("Icons/missing_square.png"),
                        "no icon found",
                        modifier = Modifier.height(50.dp)
                    )
                    it.entity.getIcon()
                }
            }
        }
    }
}