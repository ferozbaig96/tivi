/*
 * Copyright 2023 Google LLC
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

package app.tivi.data.popularshows

import app.tivi.data.mappers.IndexedMapper
import app.tivi.data.mappers.TraktShowToTiviShow
import app.tivi.data.mappers.pairMapperOf
import app.tivi.data.models.PopularShowEntry
import app.tivi.data.models.TiviShow
import app.tivi.data.util.bodyOrThrow
import app.tivi.data.util.withRetry
import com.uwetrottmann.trakt5.entities.Show
import com.uwetrottmann.trakt5.enums.Extended
import com.uwetrottmann.trakt5.services.Shows
import javax.inject.Inject
import javax.inject.Provider
import retrofit2.awaitResponse

class TraktPopularShowsDataSource @Inject constructor(
    private val showService: Provider<Shows>,
    showMapper: TraktShowToTiviShow,
) : PopularShowsDataSource {
    private val entryMapper = IndexedMapper<Show, PopularShowEntry> { index, _ ->
        PopularShowEntry(showId = 0, pageOrder = index, page = 0)
    }

    private val resultsMapper = pairMapperOf(showMapper, entryMapper)

    override suspend operator fun invoke(
        page: Int,
        pageSize: Int,
    ): List<Pair<TiviShow, PopularShowEntry>> = withRetry {
        showService.get().popular(page + 1, pageSize, Extended.NOSEASONS)
            .awaitResponse()
            .let { resultsMapper.invoke(it.bodyOrThrow()) }
    }
}
