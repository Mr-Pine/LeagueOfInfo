import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Colors
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

@Composable
fun EventComposable(event: ActionEvent){
    val team = if(event.killer.team != Team.UNKNOWN) event.killer.team else if (event.victim.team != Team.UNKNOWN) {if(event.victim.team == Team.CHAOS) Team.ORDER else Team.CHAOS} else Team.UNKNOWN
    Column (modifier = Modifier.padding(4.dp).background(
        when (team) {
            Team.CHAOS -> Color.Red
            Team.ORDER -> Color.Blue
            Team.UNKNOWN -> Color.Gray
        }
    ).padding(4.dp)) {
        Row(
        ) {
            event.killer.iconDraw(Modifier.height(50.dp))
            Spacer(modifier = Modifier.width(24.dp))
            event.victim.iconDraw(Modifier.height(50.dp))

        }
        Spacer(modifier = Modifier.height(4.dp))
        Row {
            event.assisters.forEach {
                it.entity.iconDraw(Modifier.height(20.dp).padding(end = 2.dp))
            }
        }
    }
}