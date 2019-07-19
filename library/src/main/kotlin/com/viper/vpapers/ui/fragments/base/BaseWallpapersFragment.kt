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
package com.viper.vpapers.ui.fragments.base

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.view.View
import android.widget.ImageView
import androidx.core.app.ActivityOptionsCompat
import androidx.core.util.Pair
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.Glide
import com.bumptech.glide.integration.recyclerview.RecyclerViewPreloader
import com.bumptech.glide.util.ViewPreloadSizeProvider
import com.pluscubed.recyclerfastscroll.RecyclerFastScroller
import com.viper.vpapers.R
import com.viper.vpapers.data.models.Collection
import com.viper.vpapers.data.models.Wallpaper
import com.viper.vpapers.helpers.extensions.configs
import com.viper.vpapers.helpers.extensions.jfilter
import com.viper.vpapers.helpers.extensions.maxPictureRes
import com.viper.vpapers.helpers.extensions.maxPreload
import com.viper.vpapers.helpers.utils.FL
import com.viper.vpapers.helpers.utils.MAX_WALLPAPERS_LOAD
import com.viper.vpapers.ui.activities.ViewerActivity
import com.viper.vpapers.ui.activities.base.BaseVPapersActivity
import com.viper.vpapers.ui.activities.base.FavsDbManager
import com.viper.vpapers.ui.adapters.WallpapersAdapter
import com.viper.vpapers.ui.adapters.viewholders.VPapersViewClickListener
import com.viper.vpapers.ui.adapters.viewholders.WallpaperHolder
import com.viper.vpapers.ui.widgets.EmptyViewRecyclerView
import com.viper.vpapers.ui.widgets.EndlessRecyclerViewScrollListener
import jahirfiquitiva.libs.kext.extensions.accentColor
import jahirfiquitiva.libs.kext.extensions.activity
import jahirfiquitiva.libs.kext.extensions.cardBackgroundColor
import jahirfiquitiva.libs.kext.extensions.context
import jahirfiquitiva.libs.kext.extensions.dimenPixelSize
import jahirfiquitiva.libs.kext.extensions.formatCorrectly
import jahirfiquitiva.libs.kext.extensions.hasContent
import jahirfiquitiva.libs.kext.extensions.isInHorizontalMode
import jahirfiquitiva.libs.kext.extensions.isLowRamDevice
import jahirfiquitiva.libs.kext.extensions.toBitmap
import jahirfiquitiva.libs.kext.ui.decorations.GridSpacingItemDecoration
import java.io.FileOutputStream

abstract class BaseWallpapersFragment : BaseVPapersFragment<Wallpaper, WallpaperHolder>() {
    
    var hasChecker = false
    var recyclerView: EmptyViewRecyclerView? = null
        private set
    var fastScroller: RecyclerFastScroller? = null
        private set
    private var swipeToRefresh: SwipeRefreshLayout? = null
    
    private val provider: ViewPreloadSizeProvider<Wallpaper> by lazy {
        ViewPreloadSizeProvider<Wallpaper>()
    }
    
    val wallsAdapter: WallpapersAdapter by lazy {
        WallpapersAdapter(
            context?.let { Glide.with(it) },
            provider, fromFavorites(), showFavoritesIcon(),
            object : VPapersViewClickListener<Wallpaper, WallpaperHolder>() {
                override fun onSingleClick(item: Wallpaper, holder: WallpaperHolder) {
                    onItemClicked(item, holder)
                }
                
                override fun onLongClick(item: Wallpaper) {
                    super.onLongClick(item)
                    (activity as? BaseVPapersActivity<*>)?.showWallpaperOptionsDialog(item)
                }
                
                override fun onHeartClick(
                    view: ImageView,
                    item: Wallpaper,
                    color: Int
                                         ) {
                    super.onHeartClick(view, item, color)
                    onHeartClicked(view, item, color)
                }
            })
    }
    
    private var spanCount = 0
    private var spacingDecoration = GridSpacingItemDecoration(0, 0)
    
    override fun initUI(content: View) {
        swipeToRefresh = content.findViewById(R.id.swipe_to_refresh)
        recyclerView = content.findViewById(R.id.list_rv)
        fastScroller = content.findViewById(R.id.fast_scroller)
        
        swipeToRefresh?.let {
            it.setProgressBackgroundColorSchemeColor(it.context.cardBackgroundColor)
            it.setColorSchemeColors(it.context.accentColor)
            it.setOnRefreshListener { reloadData(if (fromFavorites()) 2 else 1) }
        }
        
        recyclerView?.let { recyclerView ->
            with(recyclerView) {
                textView = content.findViewById(R.id.empty_text)
                emptyView = content.findViewById(R.id.empty_view)
                setEmptyImage(
                    if (fromFavorites()) R.drawable.no_favorites else R.drawable.empty_section)
                setEmptyText(if (fromFavorites()) R.string.no_favorites else R.string.empty_section)
                loadingView = content.findViewById(R.id.loading_view)
                setLoadingText(R.string.loading_section)
                configureRVColumns()
                itemAnimator =
                    if (context.isLowRamDevice) null else DefaultItemAnimator()
                setHasFixedSize(true)
                
                activity {
                    addOnScrollListener(
                        RecyclerViewPreloader(it, wallsAdapter, provider, context.maxPreload))
                }
                
                layoutManager?.let {
                    addOnScrollListener(
                        EndlessRecyclerViewScrollListener(it) { _, view ->
                            if (userVisibleHint) {
                                view.post { wallsAdapter.allowMoreItemsLoad() }
                            }
                        })
                }
                
                setItemViewCacheSize((MAX_WALLPAPERS_LOAD * 1.5).toInt())
                adapter = wallsAdapter
            }
        }
        
        swipeToRefresh?.let { fastScroller?.attachSwipeRefreshLayout(it) }
        recyclerView?.let { fastScroller?.attachRecyclerView(it) }
    }
    
    override fun scrollToTop() {
        recyclerView?.post { recyclerView?.scrollToPosition(0) }
    }
    
    override fun onResume() {
        super.onResume()
        configureRVColumns()
        canClick = true
    }
    
    fun configureRVColumns() {
        context {
            if (configs.columns != spanCount) {
                recyclerView?.removeItemDecoration(spacingDecoration)
                val columns = configs.columns
                spanCount = if (it.isInHorizontalMode) (columns * 1.5).toInt() else columns
                recyclerView?.layoutManager =
                    GridLayoutManager(context, spanCount, RecyclerView.VERTICAL, false)
                spacingDecoration = GridSpacingItemDecoration(
                    spanCount, it.dimenPixelSize(R.dimen.wallpapers_grid_spacing))
                recyclerView?.addItemDecoration(spacingDecoration)
            }
        }
    }
    
    override fun getContentLayout(): Int = R.layout.section_with_swipe_refresh
    
    override fun onItemClicked(item: Wallpaper, holder: WallpaperHolder) =
        onWallpaperClicked(item, holder)
    
    override fun loadDataFromViewModel() {
        recyclerView?.state = EmptyViewRecyclerView.State.LOADING
        super.loadDataFromViewModel()
    }
    
    override fun enableRefresh(enable: Boolean) {
        swipeToRefresh?.isEnabled = enable
    }
    
    override fun reloadData(section: Int) {
        val isRefreshing = swipeToRefresh?.isRefreshing ?: false
        if (isRefreshing) swipeToRefresh?.isRefreshing = false
        recyclerView?.state = EmptyViewRecyclerView.State.LOADING
        super.reloadData(section)
        swipeToRefresh?.isRefreshing = true
    }
    
    override fun doOnCollectionsChange(data: ArrayList<Collection>) {
        super.doOnCollectionsChange(data)
        swipeToRefresh?.isRefreshing = false
    }
    
    override fun doOnFavoritesChange(data: ArrayList<Wallpaper>) {
        super.doOnFavoritesChange(data)
        wallsAdapter.updateFavorites(data)
        swipeToRefresh?.isRefreshing = false
    }
    
    override fun doOnWallpapersChange(data: ArrayList<Wallpaper>, fromCollectionActivity: Boolean) {
        super.doOnWallpapersChange(data, fromCollectionActivity)
        swipeToRefresh?.isRefreshing = false
    }
    
    override fun applyFilter(filter: String, closed: Boolean) {
        val list = ArrayList(
            if (fromFavorites())
                (activity as? FavsDbManager)?.getFavs() ?: wallpapersModel?.getData().orEmpty()
            else wallpapersModel?.getData().orEmpty())
        
        if (filter.hasContent()) {
            recyclerView?.setEmptyImage(R.drawable.no_results)
            recyclerView?.setEmptyText(R.string.search_no_results)
            wallsAdapter.setItems(list.jfilter { filteredWallpaper(it, filter) })
        } else {
            recyclerView?.setEmptyImage(
                if (fromFavorites()) R.drawable.no_favorites else R.drawable.empty_section)
            recyclerView?.setEmptyText(
                if (fromFavorites()) R.string.no_favorites else R.string.empty_section)
            wallsAdapter.setItems(list)
        }
        if (!closed)
            scrollToTop()
    }
    
    private fun filteredWallpaper(wallpaper: Wallpaper, filter: String): Boolean {
        return if (configs.deepSearchEnabled) {
            wallpaper.name.contains(filter, true) ||
                wallpaper.author.contains(filter, true) ||
                (!fromCollectionActivity() &&
                    wallpaper.collections.formatCorrectly().replace("_", " ")
                        .contains(filter, true))
        } else {
            wallpaper.name.contains(filter, true)
        }
    }
    
    private var canClick = true
    
    private fun onWallpaperClicked(wallpaper: Wallpaper, holder: WallpaperHolder) {
        if (!canClick) return
        try {
            val intent = Intent(activity, ViewerActivity::class.java)
            
            var options: ActivityOptionsCompat? = null
            
            with(intent) {
                putExtra("wallpaper", wallpaper)
                putExtra(
                    "inFavorites", (activity as? FavsDbManager)?.isInFavs(wallpaper) ?: false)
                putExtra("showFavoritesButton", showFavoritesIcon())
                putExtra("checker", hasChecker)
                
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
                    val imgTransition = holder.img?.let { ViewCompat.getTransitionName(it) } ?: ""
                    val nameTransition = holder.name?.let { ViewCompat.getTransitionName(it) } ?: ""
                    val authorTransition =
                        holder.author?.let { ViewCompat.getTransitionName(it) } ?: ""
                    val heartTransition =
                        holder.heartIcon?.let { ViewCompat.getTransitionName(it) } ?: ""
                    
                    putExtra("imgTransition", imgTransition)
                    putExtra("nameTransition", nameTransition)
                    putExtra("authorTransition", authorTransition)
                    putExtra("favTransition", heartTransition)
                    
                    val imgPair = Pair<View, String>(holder.img, imgTransition)
                    val namePair = Pair<View, String>(holder.name, nameTransition)
                    val authorPair = Pair<View, String>(holder.author, authorTransition)
                    val heartPair = Pair<View, String>(holder.heartIcon, heartTransition)
                    
                    options =
                        activity?.let {
                            ActivityOptionsCompat.makeSceneTransitionAnimation(
                                it, imgPair, namePair, authorPair, heartPair)
                        }
                }
            }
            
            var fos: FileOutputStream? = null
            try {
                val filename = "thumb.png"
                fos = activity?.openFileOutput(filename, Context.MODE_PRIVATE)
                holder.img?.drawable?.toBitmap()
                    ?.compress(Bitmap.CompressFormat.JPEG, context?.maxPictureRes ?: 25, fos)
                intent.putExtra("image", filename)
            } catch (ignored: Exception) {
            } finally {
                fos?.flush()
                fos?.close()
            }
            
            try {
                startActivityForResult(intent, 10, options?.toBundle())
            } catch (e: Exception) {
                FL.e("Error", e)
                startActivityForResult(intent, 10)
            }
        } catch (e: Exception) {
            FL.e("Error", e)
            canClick = true
        }
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == 10) {
            data?.let {
                val item = it.getParcelableExtra<Wallpaper>("item")
                val hasModifiedFavs = it.getBooleanExtra("modified", false)
                val inFavs = it.getBooleanExtra("inFavorites", false)
                item?.let { wall ->
                    if (hasModifiedFavs) {
                        activity?.let {
                            (it as? FavsDbManager)?.updateToFavs(wall, inFavs, it, false)
                        } ?: showErrorSnackBar()
                    }
                }
            }
        }
    }
    
    abstract fun showFavoritesIcon(): Boolean
    
    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)
        if (isVisibleToUser && !allowReloadAfterVisibleToUser()) recyclerView?.updateEmptyState()
    }
    
    override fun autoStartLoad(): Boolean = true
}