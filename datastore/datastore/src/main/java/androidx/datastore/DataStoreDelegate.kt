/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.datastore

import android.content.Context
import androidx.annotation.GuardedBy
import androidx.datastore.core.DataMigration
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.core.Serializer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.io.File
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * Creates a property delegate for a single process DataStore. This should only be called once
 * in a file (at the top level), and all usages of the DataStore should use a reference the same
 * Instance. The receiver type for the property delegate must be an instance of [Context].
 *
 * Example usage:
 * ```
 * val Context.myDataStore by dataStore("filename", serializer)
 *
 * class SomeClass(val context: Context) {
 *    suspend fun update() = context.myDataStore.updateData {...}
 * }
 * ```
 *
 * @param fileName the filename relative to Context.filesDir that DataStore acts on. The File is
 * obtained by calling File(context.filesDir, "datastore/$fileName")). No two instances of DataStore
 * should act on the same file at the same time.
 * @param corruptionHandler The corruptionHandler is invoked if DataStore encounters a
 * [androidx.datastore.core.CorruptionException] when attempting to read data. CorruptionExceptions
 * are thrown by serializers when data can not be de-serialized.
 * @param migrations are run before any access to data can occur. Each producer and migration
 * may be run more than once whether or not it already succeeded (potentially because another
 * migration failed or a write to disk failed.)
 * @param scope The scope in which IO operations and transform functions will execute.
 *
 * @return a property delegate that manages a datastore as a singleton.
 */
@JvmOverloads
public fun <T> dataStore(
    fileName: String,
    serializer: Serializer<T>,
    corruptionHandler: ReplaceFileCorruptionHandler<T>? = null,
    migrations: List<DataMigration<T>> = listOf(),
    scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
): ReadOnlyProperty<Context, DataStore<T>> {
    return DataStoreSingletonDelegate(fileName, serializer, corruptionHandler, migrations, scope)
}

/**
 * Delegate class to manage DataStore as a singleton.
 */
internal class DataStoreSingletonDelegate<T> internal constructor(
    private val fileName: String,
    private val serializer: Serializer<T>,
    private val corruptionHandler: ReplaceFileCorruptionHandler<T>?,
    private val migrations: List<DataMigration<T>>,
    private val scope: CoroutineScope
) : ReadOnlyProperty<Context, DataStore<T>> {

    private val lock = Any()

    @GuardedBy("lock")
    @Volatile
    private var INSTANCE: DataStore<T>? = null

    /**
     * Gets the instance of the DataStore.
     *
     * @param thisRef must be an instance of [Context]
     * @param property not used
     */
    override fun getValue(thisRef: Context, property: KProperty<*>): DataStore<T> {
        return INSTANCE ?: synchronized(lock) {
            if (INSTANCE == null) {
                INSTANCE = DataStoreFactory.create(
                    serializer = serializer,
                    produceFile = {
                        File(
                            thisRef.applicationContext.filesDir,
                            "datastore/$fileName"
                        )
                    },
                    corruptionHandler = corruptionHandler,
                    migrations = migrations,
                    scope = scope
                )
            }
            INSTANCE!!
        }
    }
}
