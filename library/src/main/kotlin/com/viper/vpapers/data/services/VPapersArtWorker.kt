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
package com.viper.vpapers.data.services

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.room.Room
import com.google.android.apps.muzei.api.provider.Artwork
import com.google.android.apps.muzei.api.provider.ProviderContract
import jahirfiquitiva.libs.archhelpers.tasks.QAsync
import com.viper.vpapers.data.models.Wallpaper
import com.viper.vpapers.data.models.db.FavoritesDatabase
import com.viper.vpapers.helpers.utils.DATABASE_NAME
import com.viper.vpapers.helpers.utils.VPapersKonfigs
import com.viper.vpapers.viewmodels.FavoritesViewModel
import com.viper.vpapers.viewmodels.WallpapersViewModel
import jahirfiquitiva.libs.kext.extensions.formatCorrectly
import jahirfiquitiva.libs.kext.extensions.hasContent
import java.lang.ref.WeakReference

@SuppressLint("NewApi")
class VPapersArtWorker : LifecycleOwner {
    
    private var task: QAsync<*, *>? = null
    
    private val lcRegistry = LifecycleRegistry(this)
    override fun getLifecycle(): LifecycleRegistry = lcRegistry
    
    private var wallsVM: WallpapersViewModel? = null
    private var favsDB: FavoritesDatabase? = null
    private var favsVM: FavoritesViewModel? = null
    
    fun loadWallpapers(context: Context?) {
        context ?: return
        task?.cancel(true)
        task = null
        task = QAsync<Context, Unit>(
            WeakReference(context),
            object : QAsync.Callback<Context, Unit>() {
                override fun doLoad(param: Context): Unit? = doWork(param)
                override fun onSuccess(result: Unit) {}
            })
        task?.execute()
    }
    
    fun destroy() {
        destroyViewModel(true)
    }
    
    private fun doWork(context: Context) {
        try {
            destroyViewModel()
            if (wallsVM == null) {
                wallsVM = WallpapersViewModel()
                wallsVM?.extraObserve {
                    if (it.isNotEmpty()) {
                        val configs = VPapersKonfigs(context)
                        val realData =
                            getValidWallpapersList(configs.muzeiCollections, ArrayList(it))
                        if (configs.muzeiCollections.contains("favorites", true)) {
                            if (favsDB == null) {
                                favsDB =
                                    Room.databaseBuilder(
                                        context, FavoritesDatabase::class.java, DATABASE_NAME)
                                        .fallbackToDestructiveMigration().build()
                            }
                            if (favsVM == null) {
                                favsVM = FavoritesViewModel()
                                favsVM?.extraObserve {
                                    realData.addAll(
                                        getValidWallpapersList(
                                            configs.muzeiCollections, ArrayList(it)))
                                    realData.distinct()
                                    if (realData.isNotEmpty()) postWallpapers(context, realData)
                                }
                            }
                            val dao = favsDB?.favoritesDao()
                            if (dao != null) {
                                favsVM?.loadData(dao, true)
                            } else {
                                if (realData.isNotEmpty()) postWallpapers(context, realData)
                            }
                        } else {
                            if (realData.isNotEmpty()) postWallpapers(context, realData)
                        }
                    }
                }
            }
            wallsVM?.loadData(context, true)
        } catch (e: Exception) {
        }
    }
    
    private fun postWallpapers(context: Context, wallpapers: ArrayList<Wallpaper>) {
        val client: String by lazy { "${context.packageName}.muzei" }
        val providerClient = ProviderContract.getProviderClient(context, client)
        providerClient.addArtwork(wallpapers.map { wallpaper ->
            Artwork().apply {
                token = wallpaper.url
                title = wallpaper.name
                byline = wallpaper.author
                attribution =
                    if (wallpaper.copyright.hasContent()) wallpaper.copyright else wallpaper.author
                persistentUri = Uri.parse(wallpaper.url)
                webUri = Uri.parse(wallpaper.url)
                metadata = wallpaper.url
            }
        })
        destroyViewModel()
    }
    
    private fun getValidWallpapersList(
        muzeiCollections: String,
        original: ArrayList<Wallpaper>
                                      ): ArrayList<Wallpaper> {
        val newList = ArrayList<Wallpaper>()
        original.forEach { if (validWallpaper(muzeiCollections, it)) newList.add(it) }
        newList.distinct()
        return newList
    }
    
    private fun validWallpaper(muzeiCollections: String, item: Wallpaper): Boolean {
        val collections = item.collections.split("[,|]".toRegex())
        val selected = muzeiCollections.split("[,|]".toRegex())
        if (collections.isEmpty() || selected.isEmpty()) return true
        for (collection in collections) {
            val correct = collection.formatCorrectly().replace("_", " ")
            selected.forEach {
                if (!it.hasContent() || it.equals(collection, true) || it.equals(correct, true))
                    return true
            }
        }
        return false
    }
    
    private fun destroyViewModel(makeNull: Boolean = false) {
        wallsVM?.destroy(this)
        favsVM?.destroy(this)
        favsDB?.close()
        if (makeNull) {
            wallsVM = null
            favsVM = null
            favsDB = null
        }
    }
}