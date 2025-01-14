/*
 * Copyright 2018 Google LLC
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

import app.tivi.data.models.TiviEntity

interface EntityDao<in E : TiviEntity> {
    suspend fun insert(entity: E): Long

    suspend fun insertAll(vararg entity: E)

    suspend fun insertAll(entities: List<E>)

    suspend fun update(entity: E)

    suspend fun deleteEntity(entity: E): Int

    suspend fun withTransaction(tx: suspend () -> Unit)
}

suspend fun <E : TiviEntity> EntityDao<E>.insertOrUpdate(entity: E): Long {
    return if (entity.id == 0L) {
        insert(entity)
    } else {
        update(entity)
        entity.id
    }
}

suspend fun <E : TiviEntity> EntityDao<E>.insertOrUpdate(entities: List<E>) = withTransaction {
    entities.forEach {
        insertOrUpdate(it)
    }
}
