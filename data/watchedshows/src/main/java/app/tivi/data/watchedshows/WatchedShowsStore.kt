/*
 * Copyright 2020 Google LLC
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

package app.tivi.data.watchedshows

import app.tivi.data.daos.TiviShowDao
import app.tivi.data.daos.WatchedShowDao
import app.tivi.data.daos.getIdOrSavePlaceholder
import app.tivi.data.models.WatchedShowEntry
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.map
import org.mobilenativefoundation.store.store5.Fetcher
import org.mobilenativefoundation.store.store5.SourceOfTruth
import org.mobilenativefoundation.store.store5.Store
import org.mobilenativefoundation.store.store5.StoreBuilder
import org.threeten.bp.Duration

@Singleton
class WatchedShowsStore @Inject constructor(
    dataSource: WatchedShowsDataSource,
    watchedShowsDao: WatchedShowDao,
    showDao: TiviShowDao,
    lastRequestStore: WatchedShowsLastRequestStore,
) : Store<Unit, List<WatchedShowEntry>> by StoreBuilder.from(
    fetcher = Fetcher.of {
        dataSource()
            .also { lastRequestStore.updateLastRequest() }
    },
    sourceOfTruth = SourceOfTruth.of(
        reader = {
            watchedShowsDao.entriesObservable().map { entries ->
                when {
                    // Store only treats null as 'no value', so convert to null
                    entries.isEmpty() -> null
                    // If the request is expired, our data is stale
                    lastRequestStore.isRequestExpired(Duration.ofHours(6)) -> null
                    // Otherwise, our data is fresh and valid
                    else -> entries
                }
            }
        },
        writer = { _: Unit, response ->
            watchedShowsDao.withTransaction {
                val entries = response.map { (show, entry) ->
                    entry.copy(showId = showDao.getIdOrSavePlaceholder(show))
                }
                watchedShowsDao.deleteAll()
                watchedShowsDao.insertAll(entries)
            }
        },
        delete = {
            // Delete of an entity here means the entire list
            watchedShowsDao.deleteAll()
        },
        deleteAll = watchedShowsDao::deleteAll,
    ),
).build()
