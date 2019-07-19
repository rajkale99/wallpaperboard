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
package com.viper.vpapers.viewmodels

import android.content.Context
import jahirfiquitiva.libs.archhelpers.viewmodels.ListViewModel
import com.viper.vpapers.R
import com.viper.vpapers.data.models.Wallpaper
import com.viper.vpapers.helpers.remote.VPapersUrlRequests
import com.viper.vpapers.helpers.utils.FL
import com.viper.vpapers.helpers.utils.VPapersKonfigs
import jahirfiquitiva.libs.kext.extensions.boolean
import jahirfiquitiva.libs.kext.extensions.formatCorrectly
import jahirfiquitiva.libs.kext.extensions.hasContent
import jahirfiquitiva.libs.kext.extensions.string
import jahirfiquitiva.libs.kext.extensions.toTitleCase
import org.json.JSONArray
import org.json.JSONObject

class WallpapersViewModel : ListViewModel<Context, Wallpaper>() {
    
    override fun internalLoad(param: Context): ArrayList<Wallpaper> =
        loadWallpapers(
            param, VPapersUrlRequests.requestJson(param.getString(R.string.json_url)))
    
    private fun loadWallpapers(context: Context, serverResponse: String): ArrayList<Wallpaper> {
        val prevResponse = VPapersKonfigs(context).backupJson
        val validPrevResponse = prevResponse.hasContent() && prevResponse != "[]"
        return if (serverResponse.hasContent()) {
            val nResponse = safeParseResponseToJSON(context, serverResponse)
            val nResponseText = nResponse.toString()
            if (validPrevResponse && nResponseText == prevResponse) {
                if (isOldDataValid()) ArrayList(getData().orEmpty())
                else parseListFromJson(context, nResponse)
            } else parseListFromJson(context, nResponse)
        } else {
            if (validPrevResponse) {
                parseListFromJson(context, safeParseResponseToJSON(context, prevResponse))
            } else ArrayList()
        }
    }
    
    private fun safeParseResponseToJSON(context: Context, response: String): JSONArray {
        return try {
            parseResponseToJSON(response, context.boolean(R.bool.use_old_json_format))
        } catch (e: Exception) {
            FL.e(e.message)
            try {
                parseResponseToJSON(response, true)
            } catch (e2: Exception) {
                FL.e(e2.message)
                JSONArray("[]")
            }
        }
    }
    
    private fun parseResponseToJSON(response: String, useOldFormat: Boolean): JSONArray {
        return if (response.hasContent()) {
            if (useOldFormat) parseResponseToOldJSON(response)
            else JSONArray(response)
        } else JSONArray("[]")
    }
    
    private fun parseResponseToOldJSON(response: String): JSONArray {
        return try {
            JSONObject(response).getJSONArray("Wallpapers")
        } catch (ignored: Exception) {
            try {
                JSONObject(response).getJSONArray("wallpapers")
            } catch (ignored: Exception) {
                JSONArray("[]")
            }
        }
    }
    
    private fun parseListFromJson(context: Context, json: JSONArray): ArrayList<Wallpaper> {
        VPapersKonfigs(context).backupJson = json.toString()
        val fWallpapers = ArrayList<Wallpaper>()
        for (index in 0..json.length()) {
            if (json.isNull(index)) continue
            val obj = json.getJSONObject(index)
            val name = obj.string("name")
            val author = obj.string("author")
            val collections = getCollectionsForWallpaper(obj)
            val downloadable = isWallpaperDownloadable(obj)
            val url = obj.string("url")
            val thumbUrl = getWallpaperThumbnailUrl(obj)
            val size = getWallpaperBytes(obj)
            val dimensions = getWallpaperDimensions(obj)
            val copyright = obj.string("copyright")
            val correctName = name.formatCorrectly().replace("_", " ")
            val correctAuthor = author.formatCorrectly().replace("_", " ").toTitleCase()
            if (correctName.hasContent() && url.hasContent()) {
                fWallpapers.add(
                    Wallpaper(
                        correctName, correctAuthor, collections,
                        downloadable, url,
                        if (thumbUrl.hasContent()) thumbUrl else url,
                        size, dimensions, copyright))
            }
        }
        fWallpapers.distinct()
        return fWallpapers
    }
    
    private fun getCollectionsForWallpaper(obj: JSONObject): String = try {
        obj.getString("collections")
    } catch (ignored: Exception) {
        try {
            obj.getString("categories")
        } catch (ignored: Exception) {
            try {
                obj.getString("category") ?: ""
            } catch (ignored: Exception) {
                ""
            }
        }
    }
    
    private fun isWallpaperDownloadable(obj: JSONObject): Boolean = try {
        obj.getBoolean("downloadable")
    } catch (ignored: Exception) {
        try {
            (obj.getString("downloadable") ?: "true").equals("true", true)
        } catch (ignored: Exception) {
            true
        }
    }
    
    private fun getWallpaperThumbnailUrl(obj: JSONObject): String = try {
        obj.getString("thumbnail")
    } catch (ignored: Exception) {
        try {
            obj.getString("thumbUrl")
        } catch (ignored: Exception) {
            try {
                obj.getString("thumburl")
            } catch (ignored: Exception) {
                try {
                    obj.getString("thumb")
                } catch (ignored: Exception) {
                    try {
                        obj.getString("url-thumb") ?: ""
                    } catch (ignored: Exception) {
                        ""
                    }
                }
            }
        }
    }
    
    private fun getWallpaperBytes(obj: JSONObject): Long = try {
        obj.getLong("size")
    } catch (ignored: Exception) {
        try {
            obj.getString("size").toLong()
        } catch (ignored: Exception) {
            0L
        }
    }
    
    private fun getWallpaperDimensions(obj: JSONObject): String = try {
        obj.getString("dimensions")
    } catch (ignored: Exception) {
        try {
            obj.getString("dimension") ?: ""
        } catch (ignored: Exception) {
            ""
        }
    }
}