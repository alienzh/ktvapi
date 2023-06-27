package io.agora.ktv.demo

data class MusicModel constructor(
    val songCode: Long,
    val songName: String,
    val singer: String
) {
    override fun toString(): String {
        return "$songName-$singer"
    }
}