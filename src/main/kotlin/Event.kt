open class Event(val eventType: EventType) {
}

open class ActionEvent(eventType: EventType, val team: Team, val killer: Entity, val assisters: Array<Summoner>, val victim: Entity) :
    Event(eventType) {

}
enum class EventType {
CHAMPION_KILL, TURRET_KILLED, INHIB_KILLED, DRAGON_KILLED, UNKNOWN
}