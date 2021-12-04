open class ActionEvent(val killer: Entity, val assisters: Array<Summoner>, val victim: Entity)

enum class EventType {
CHAMPION_KILL, TURRET_KILLED, INHIB_KILLED, DRAGON_KILLED, UNKNOWN
}