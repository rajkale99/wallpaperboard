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
package com.viper.vpapers.ui.fragments

import com.viper.vpapers.data.models.Wallpaper
import com.viper.vpapers.ui.fragments.base.BaseWallpapersFragment

internal class FavoritesFragment : BaseWallpapersFragment() {
    override fun doOnFavoritesChange(data: ArrayList<Wallpaper>) {
        super.doOnFavoritesChange(data)
        wallsAdapter.setItems(data)
    }
    
    override fun autoStartLoad(): Boolean = true
    override fun fromCollectionActivity(): Boolean = false
    override fun fromFavorites(): Boolean = true
    override fun showFavoritesIcon(): Boolean = true
    
    companion object {
        fun create(hasChecker: Boolean): FavoritesFragment =
            FavoritesFragment().apply { this.hasChecker = hasChecker }
    }
}