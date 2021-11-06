import androidx.compose.foundation.Image
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import kotlinx.coroutines.*
import kotlinx.coroutines.future.await
import kotlinx.serialization.json.*
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.*
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import kotlin.math.absoluteValue
import kotlin.math.max


class Game {
    private val scope = CoroutineScope(Dispatchers.Default)
    var gameStarted by mutableStateOf(false)

    var activeTeam by mutableStateOf(Team.UNKNOWN)

    private val players = object {
        val order = mutableStateListOf<Summoner>()
        val chaos = mutableStateListOf<Summoner>()
    }

    var summonerSelected by mutableStateOf("unknown")

    val events = mutableStateListOf<ActionEvent>()

    val killDifference = mutableStateListOf(0)
    var maxKillDifference by mutableStateOf(1)

    var isFinished by mutableStateOf(false)

    fun getPlayers(team: Team): Array<Summoner> {
        return when (team) {
            Team.ORDER -> {
                players.order.toTypedArray()
            }
            Team.CHAOS -> {
                players.chaos.toTypedArray()
            }
            else -> {
                players.chaos.toTypedArray().plus(players.order.toTypedArray())
            }
        }
    }

    init {
        //<editor-fold desc="No ssl">
        val trustAllCerts = arrayOf<TrustManager>(
            object : X509TrustManager {
                override fun getAcceptedIssuers(): Array<X509Certificate>? {
                    return null
                }

                override fun checkClientTrusted(
                    certs: Array<X509Certificate>, authType: String
                ) {
                }

                override fun checkServerTrusted(
                    certs: Array<X509Certificate>, authType: String
                ) {
                }
            }
        )
        val sc = SSLContext.getInstance("SSL")
        try {
            sc.init(null, trustAllCerts, SecureRandom())
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.socketFactory)
        } catch (e: Exception) {
        }
        //</editor-fold>

        val client = HttpClient.newBuilder().sslContext(sc).build()
        val request =
            HttpRequest.newBuilder().uri(URI.create("http://127.0.0.1:2999/liveclientdata/allgamedata")).GET().build()
        scope.launch(Dispatchers.Main) {
            var responseJson: JsonElement
            while (true) {
                println("hi")
                try {
                    delay(5000)
                    val responseFuture = client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    val response = responseFuture.await()
                    responseJson = Json.parseToJsonElement(response.body())
                    if (responseJson.jsonObject["events"] != null) {
                        gameStarted = true
                        break
                    }

                    println(response.body())
                } catch (e: Exception) {
                }
            }
            val responseObject = responseJson.jsonObject
            val activePlayerName = responseObject["activePlayer"]!!.jsonObject["summonerName"]
            if (activePlayerName != null) {
                summonerSelected = activePlayerName.jsonPrimitive.content
            }
            val activePlayer =
                responseObject["allPlayers"]!!.jsonArray.last { it.jsonObject["summonerName"] == activePlayerName }
            activeTeam =
                if (activePlayer.jsonObject["team"]!!.jsonPrimitive.content == "ORDER") Team.ORDER else Team.CHAOS
            responseObject["allPlayers"]!!.jsonArray.forEach {
                val player = it.jsonObject
                val summonerName = player["summonerName"]!!.jsonPrimitive.content
                val championDisplayName = player["championName"]!!.jsonPrimitive.content
                val championName = player["rawChampionName"]!!.jsonPrimitive.content.substring(27)
                val team = player["team"]!!.jsonPrimitive.content
                /*val team = "ORDER"
                val summonerName = "Mr. Pine"
                val championDisplayName = "Sivir"
                val championName = "Sivir"*/
                (if (team == "ORDER") players.order else players.chaos).add(
                    Summoner(
                        name = summonerName,
                        championName = championName,
                        championDisplayName = championDisplayName,
                        team = if (team == "ORDER") Team.ORDER else Team.CHAOS
                    )
                )
            }
            //players.order.sortBy {  } TODO: Sorting by position
            watchEvents(client)
        }
    }

    private var lastEventIndex = 0

    private fun watchEvents(client: HttpClient) {
        val request =
            HttpRequest.newBuilder().uri(URI.create("http://127.0.0.1:2999/liveclientdata/eventdata")).GET().build()
        scope.launch(Dispatchers.IO) {
            println("hello")
            while (true) {
                delay(3000)
                val response = try {
                    client.send(request, HttpResponse.BodyHandlers.ofString())
                } catch (e: Exception) {
                    if(e is java.net.ConnectException){
                        isFinished = true
                        break
                    } else {
                        throw e
                    }
                }
                val eventList = Json.parseToJsonElement(response.body()).jsonObject["Events"]!!.jsonArray
                eventList.forEach {
                    val event = it.jsonObject
                    val eventName = event["EventName"]!!.jsonPrimitive.content
                    val eventIndex = event["EventID"]!!.jsonPrimitive.content.toInt()

                    val eventType = when (eventName) {
                        "ChampionKill" -> EventType.CHAMPION_KILL
                        "TurretKilled" -> EventType.TURRET_KILLED
                        "InhibKilled" -> EventType.INHIB_KILLED
                        "DragonKill" -> EventType.DRAGON_KILLED
                        else -> EventType.UNKNOWN
                    }

                    if (eventIndex > lastEventIndex && eventType != EventType.UNKNOWN) {
                        val killerName = event["KillerName"]!!.jsonPrimitive.content
                        val killer =
                            if (killerName.startsWith("Turret")) TurretEntity(killerName)
                            else if (killerName.startsWith("Minion")) MinionEntity(killerName)
                            else if (killerName.startsWith("SRU")
                            ) MonsterEntity(killerName)
                            else if (killerName == "Unknown") Entity()
                            else SummonerEntity(getPlayers(Team.UNKNOWN).last { summoner -> summoner.name == killerName })

                        val victimName =
                            event[
                                    when (eventType) {
                                        EventType.CHAMPION_KILL -> "VictimName"
                                        EventType.TURRET_KILLED -> "TurretKilled"
                                        EventType.INHIB_KILLED -> "InhibKilled"
                                        EventType.DRAGON_KILLED -> "DragonType"
                                        else -> throw java.lang.Exception("no victimName found")
                                    }
                            ]!!.jsonPrimitive.content
                        val victim =
                            if (victimName.startsWith("Turret")) TurretEntity(victimName)
                            else if (victimName.startsWith("Minion")) MinionEntity(victimName)
                            else if (victimName.startsWith("Barracks")) InhibEntity(victimName)
                            else if (eventType == EventType.DRAGON_KILLED || victimName.contains(
                                    "baron",
                                    true
                                )
                            ) MonsterEntity(victimName)
                            else {
                                SummonerEntity(getPlayers(Team.UNKNOWN).last { summoner ->
                                    summoner.name == victimName
                                })
                            }

                        val assisterNames = event["Assisters"]!!.jsonArray
                        val assisters = mutableListOf<Summoner>()
                        assisterNames.forEach { assisterElement ->
                            val assisterName = assisterElement.jsonPrimitive.content
                            val assister =
                                getPlayers(Team.UNKNOWN).last { summoner -> summoner.name == assisterName }
                            assisters.add(assister)
                        }

                        val team =
                            if (killer is SummonerEntity) {
                                killer.summoner.team
                            } else if (killer is MinionEntity) {
                                killer.team
                            } else if (killer is TurretEntity) {
                                killer.team
                            } else if (victim is SummonerEntity) {
                                if (victim.summoner.team == Team.ORDER) Team.CHAOS else Team.ORDER
                            } else {
                                Team.UNKNOWN
                            }

                        if (victim is SummonerEntity) {
                            if (killer is SummonerEntity) {
                                killer.summoner.kda.addKill(victim.summoner)

                                val lastKD = killDifference[killDifference.lastIndex]
                                val newKD = lastKD + if(killer.team == Team.ORDER) 1 else -1
                                killDifference.add(newKD)

                                maxKillDifference = max(maxKillDifference, newKD.absoluteValue)

                                victim.summoner.kda.addDeath(killer.summoner)

                                assisters.forEach { assister ->
                                    assister.kda.addAssist(victim.summoner)
                                }
                            } else {
                                victim.summoner.kda.addDeath()
                            }
                        }

                        events.add(
                            0,
                            ActionEvent(
                                eventType = eventType,
                                killer = killer,
                                assisters = assisters.toTypedArray(),
                                team = team,
                                victim = victim
                            )
                        )

                        lastEventIndex = eventIndex
                    }
                }
            }
        }
    }
}

enum class Team {
    ORDER,
    CHAOS,
    UNKNOWN
}

class Summoner(val name: String, val championName: String, val championDisplayName: String, val team: Team) {
    val kda = KDA()

    val entity = SummonerEntity(this)
}

class KDAData() {
    var kills by mutableStateOf(0);
    var deaths by mutableStateOf(0)
    var assists by mutableStateOf(0)
}

class KDA {
    val total by mutableStateOf(KDAData())
    val against = mutableStateMapOf<String, KDAData>()

    fun addKill(againstSummoner: Summoner) {
        total.kills++
        if (against[againstSummoner.name] == null) {
            against[againstSummoner.name] = KDAData()
        }
        against[againstSummoner.name]!!.kills++
    }

    fun addDeath(againstSummoner: Summoner? = null) {
        total.deaths++
        if (againstSummoner != null) {
            if (against[againstSummoner.name] == null) {
                against[againstSummoner.name] = KDAData()
            }
            against[againstSummoner.name]!!.deaths++
        }
    }

    fun addAssist(againstSummoner: Summoner) {
        total.assists++
        if (against[againstSummoner.name] == null) {
            against[againstSummoner.name] = KDAData()
        }
        against[againstSummoner.name]!!.assists++
    }
}

open class Entity {
    open val type = EntityType.UNKNOWN

    open val team = Team.UNKNOWN

    var icon: Any? by mutableStateOf(null)

    @Composable
    open fun getIcon() {
        icon = painterResource("Icons/missing_square.png")
    }
}

class SummonerEntity(val summoner: Summoner) : Entity() {
    override val type = EntityType.SUMMONER

    override val team = summoner.team
    val name = summoner.name

    var championIcon: ImageBitmap? by mutableStateOf(null);

    @Composable
    override fun getIcon() {
        CoroutineScope(Dispatchers.Main).launch {
            championIcon =
                loadNetworkImage("http://ddragon.leagueoflegends.com/cdn/11.21.1/img/champion/${summoner.championName}.png")
            icon = championIcon
        }
    }
}

class MinionEntity(minionName: String) : Entity() {
    override val type = EntityType.MINIONS

    override val team: Team
    private val minionType: MinionType

    init {
        team = if (minionName.startsWith("Minion_T2")) Team.CHAOS else Team.ORDER
        minionType = if (minionName.contains("L0")) MinionType.SUPER else MinionType.NORMAL
    }

    enum class MinionType {
        SUPER, NORMAL
    }

    var unitIcon: Painter? = null

    @Composable
    override fun getIcon() {
        unitIcon =
            painterResource("Icons/${if (team == Team.ORDER) "blue" else "red"}${""/*if(minionType == MinionType.SUPER) "mech" else ""*/}melee_square.png")
        icon = unitIcon
    }
}

class TurretEntity(turretName: String) : Entity() {
    override val type = EntityType.TURRET

    override val team: Team

    init {
        team = if (turretName.contains("T1")) Team.ORDER else Team.CHAOS
    }

    var unitIcon: Painter? = null

    @Composable
    override fun getIcon() {
        unitIcon = painterResource("Icons/turret_${if (team == Team.ORDER) "blue" else "red"}_square.png")
        icon = unitIcon
    }
}

class InhibEntity(inhibName: String) : Entity() {
    override val type = EntityType.MINIONS

    override val team: Team

    init {
        team = if (inhibName.contains("T1")) Team.ORDER else Team.CHAOS
    }

    var unitIcon: Painter? = null

    @Composable
    override fun getIcon() {
        unitIcon = painterResource("Icons/inhibitor_${if (team == Team.ORDER) "blue" else "red"}_square.png")
        icon = unitIcon
    }
}

class MonsterEntity(monsterName: String) : Entity() {
    override val type = EntityType.TURRET

    val monsterType: MonsterType

    init {
        monsterType = if (monsterName.contains("Air")) MonsterType.DRAGON_AIR
        else if (monsterName.contains("Earth")) MonsterType.DRAGON_EARTH
        else if (monsterName.contains("Fire")) MonsterType.DRAGON_FIRE
        else if (monsterName.contains("Water")) MonsterType.DRAGON_WATER
        else if (monsterName.contains("Baron")) MonsterType.BARON
        else {
            println(monsterName)
            MonsterType.UNKNOWN
        }
    }

    var unitIcon: Painter? = null

    @Composable
    override fun getIcon() {
        unitIcon = painterResource("Icons/${monsterType}_square.png")
        icon = unitIcon
    }

    enum class MonsterType {
        DRAGON_AIR {
            override fun toString(): String {
                return "dragon_air"
            }
        },
        DRAGON_EARTH {
            override fun toString(): String {
                return "dragon_earth"
            }
        },
        DRAGON_FIRE {
            override fun toString(): String {
                return "dragon_fire"
            }
        },
        DRAGON_WATER {
            override fun toString(): String {
                return "dragon_water"
            }
        },
        BARON {
            override fun toString(): String {
                return "baron"
            }
        },
        UNKNOWN {
            override fun toString(): String {
                return "missing"
            }
        }
    }
}

enum class EntityType {
    SUMMONER, TURRET, MINIONS, MONSTER, UNKNOWN
}