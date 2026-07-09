package com.reco1l.legacy.ui.beatmapdownloader

import org.json.JSONArray
import ru.nsu.ccfit.zuev.osu.RankedStatus


/**
 * Defines an action to be performed on a mirror API.
 */
data class MirrorAction<R, M>(

    /**
     * The action API endpoint.
     */
    val endpoint: String,

    /**
     * A function to map the response into a model.
     */
    val mapResponse: (R) -> M

)

/**
 * Sort options for beatmap search.
 */
enum class SortOption(val apiValue: String, val displayName: String) {
    RELEVANCE("relevance", "Relevance"),
    TITLE_ASC("title:asc", "Title (A-Z)"),
    TITLE_DESC("title:desc", "Title (Z-A)"),
    ARTIST_ASC("artist:asc", "Artist (A-Z)"),
    ARTIST_DESC("artist:desc", "Artist (Z-A)"),
    DIFFICULTY_ASC("beatmaps.difficulty_rating:asc", "Difficulty (Low)"),
    DIFFICULTY_DESC("beatmaps.difficulty_rating:desc", "Difficulty (High)"),
    FAVOURITES_ASC("favourite_count:asc", "Favourites (Low)"),
    FAVOURITES_DESC("favourite_count:desc", "Favourites (High)"),
    PLAY_COUNT_ASC("play_count:asc", "Plays (Low)"),
    PLAY_COUNT_DESC("play_count:desc", "Plays (High)"),
    BPM_ASC("bpm:asc", "BPM (Low)"),
    BPM_DESC("bpm:desc", "BPM (High)"),
    LAST_UPDATE_ASC("last_update:asc", "Oldest"),
    LAST_UPDATE_DESC("last_update:desc", "Newest"),
    RANKED_DATE_ASC("ranked_date:asc", "Ranked (Oldest)"),
    RANKED_DATE_DESC("ranked_date:desc", "Ranked (Newest)"),
}

/**
 * Ranked status filter options.
 */
enum class StatusFilter(val apiValue: String, val displayName: String) {
    ALL("", "All"),
    GRAVEYARD("-2", "Graveyard"),
    WIP("-1", "WIP"),
    PENDING("0", "Pending"),
    RANKED("1", "Ranked"),
    APPROVED("2", "Approved"),
    QUALIFIED("3", "Qualified"),
    LOVED("4", "Loved"),
}

/**
 * Game mode filter options.
 */
enum class ModeFilter(val apiValue: String, val displayName: String) {
    ALL("", "All"),
    OSU("0", "osu!"),
    TAIKO("1", "osu!taiko"),
    CTB("2", "osu!catch"),
    MANIA("3", "osu!mania"),
}

/**
 * Defines a beatmap mirror API and its actions.
 */
enum class BeatmapMirror(

    /**
     * The search query action.
     */
    val search: MirrorAction<JSONArray, MutableList<BeatmapSetModel>>,

    val downloadEndpoint: (Long) -> String,

    val previewEndpoint: (Long) -> String,

) {

    /**
     * osu.direct beatmap mirror.
     *
     * [See documentation](https://osu.direct/api/docs).
     */
    OSU_DIRECT(
        search = MirrorAction(
            endpoint = "https://osu.direct/api/v2/search",
            mapResponse = { array ->

                MutableList(array.length()) { index ->

                    val json = array.getJSONObject(index)

                    BeatmapSetModel(
                        id = json.getLong("id"),
                        title = json.getString("title"),
                        titleUnicode = json.getString("title_unicode"),
                        artist = json.getString("artist"),
                        artistUnicode = json.getString("artist_unicode"),
                        status = RankedStatus.valueOf(json.getInt("ranked")),
                        creator = json.getString("creator"),
                        thumbnail = json.optJSONObject("covers")?.optString("card"),
                        beatmaps = json.getJSONArray("beatmaps").let {

                            MutableList(it.length()) { i ->

                                val obj = it.getJSONObject(i)

                                BeatmapModel(
                                    id = obj.getLong("id"),
                                    version = obj.getString("version"),
                                    starRating = obj.getDouble("difficulty_rating"),
                                    ar = obj.getDouble("ar"),
                                    cs = obj.getDouble("cs"),
                                    hp = obj.getDouble("drain"),
                                    od = obj.getDouble("accuracy"),
                                    bpm = obj.getDouble("bpm"),
                                    lengthSec = obj.getLong("hit_length"),
                                    circleCount = obj.getInt("count_circles"),
                                    sliderCount = obj.getInt("count_sliders"),
                                    spinnerCount = obj.getInt("count_spinners")
                                )

                            }.sortedBy(BeatmapModel::starRating)
                        }
                    )
                }

            }
        ),
        downloadEndpoint = { "https://osu.direct/api/d/$it" },
        previewEndpoint = { "https://osu.direct/api/media/preview/$it" },
    );

}

