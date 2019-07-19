/*
 * Copyright (c) 2018. Jahir Fiquitiva
 *
 * Licensed under the CreativeCommons Attribution-ShareAlike
 * 4.0 International License. You may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *    http://creativecommons.org/licenses/by-sa/4.0/legalcode
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.viper.vpapers.data.models.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.viper.vpapers.data.models.Wallpaper

// TODO: Be sure to increment update if one changes Wallpaper class structure
@Database(entities = [(Wallpaper::class)], version = 7, exportSchema = false)
abstract class FavoritesDatabase : RoomDatabase() {
    abstract fun favoritesDao(): FavoritesDao
}