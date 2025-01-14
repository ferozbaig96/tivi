/*
 * Copyright 2017 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package app.tivi.data.daos

import app.tivi.data.compoundmodels.ShowDetailed
import app.tivi.data.models.TiviShow
import app.tivi.data.util.mergeShows
import kotlinx.coroutines.flow.Flow

interface TiviShowDao : EntityDao<TiviShow> {

    suspend fun getShowWithTraktId(id: Int): TiviShow?

    fun getShowsWithIds(ids: List<Long>): Flow<List<TiviShow>>

    suspend fun getShowWithTmdbId(id: Int): TiviShow?

    fun getShowWithIdFlow(id: Long): Flow<TiviShow>

    suspend fun getShowWithIdDetailed(id: Long): ShowDetailed?

    fun getShowDetailedWithIdFlow(id: Long): Flow<ShowDetailed>

    suspend fun getShowWithId(id: Long): TiviShow?

    suspend fun getTraktIdForShowId(id: Long): Int?

    suspend fun getTmdbIdForShowId(id: Long): Int?

    suspend fun getIdForTraktId(traktId: Int): Long?

    suspend fun getIdForTmdbId(tmdbId: Int): Long?

    suspend fun delete(id: Long)

    suspend fun deleteAll()
}

suspend fun TiviShowDao.getShowWithIdOrThrow(id: Long): TiviShow {
    return getShowWithId(id)
        ?: throw IllegalArgumentException("No show with id $id in database")
}

suspend fun TiviShowDao.getIdOrSavePlaceholder(show: TiviShow): Long {
    val idForTraktId: Long? = show.traktId?.let { getIdForTraktId(it) }
    val idForTmdbId: Long? = show.tmdbId?.let { getIdForTmdbId(it) }

    if (idForTraktId != null && idForTmdbId != null) {
        return if (idForTmdbId == idForTraktId) {
            // Great, the entities are matching
            idForTraktId
        } else {
            val showForTmdbId = getShowWithIdOrThrow(idForTmdbId)
            val showForTraktId = getShowWithIdOrThrow(idForTraktId)
            deleteEntity(showForTmdbId)
            return insertOrUpdate(mergeShows(showForTraktId, showForTraktId, showForTmdbId))
        }
    }

    if (idForTraktId != null) {
        // If we get here, we only have a entity with the trakt id
        return idForTraktId
    }
    if (idForTmdbId != null) {
        // If we get here, we only have a entity with the tmdb id
        return idForTmdbId
    }

    // TODO add fuzzy search on name or slug

    return insert(show)
}
