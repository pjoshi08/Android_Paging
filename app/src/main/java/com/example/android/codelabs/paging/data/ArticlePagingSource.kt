package com.example.android.codelabs.paging.data

import androidx.paging.PagingSource
import androidx.paging.PagingState
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import kotlin.math.max

private val firstArticleCreated = LocalDateTime.now()
private const val STARTING_KEY = 0
private const val LOAD_DELAY_MILLIS = 3_000L

/**
 * To build the PagingSource you will need to define the following:
 *
 * 1. The type of the paging key - The definition of the type of the page query we use to
 * request more data. In our case, we fetch articles after or before a certain article ID
 * since the IDs are guaranteed to be ordered and increasing.
 *
 * 2. The type of data loaded - Each page returns a List of articles, so the type is Article.
 *
 * 3. Where the data is retrieved from - Typically, this would be a database, network resource,
 * or any other source of paginated data. Here, we're using locally generated data.
 */
class ArticlePagingSource : PagingSource<Int, Article>() {

    /**
     * Next we need to implement getRefreshKey(). This method is called when the Paging library
     * needs to reload items for the UI because the data in its backing PagingSource has changed.
     * This situation where the underlying data for a PagingSource has changed and needs to be
     * updated in the UI is called invalidation. When invalidated, the Paging Library creates
     * a new PagingSource to reload the data, and informs the UI by emitting new PagingData.
     *
     * When loading from a new PagingSource, getRefreshKey() is called to provide the key the
     * new PagingSource should start loading with to make sure the user does not lose their
     * current place in the list after the refresh.
     *
     * Invalidation in the paging library occurs for one of two reasons:
     *
     * 1. You called refresh() on the PagingAdapter.
     * 2. You called invalidate() on the PagingSource.
     *
     * The key returned (in our case, an Int) will be passed to the next call of the
     * load() method in the new PagingSource via the LoadParams argument. To prevent items
     * from jumping around after invalidation, we need to make sure the key returned will load
     * enough items to fill the screen. This increases the possibility that the new set of items
     * includes items that were present in the invalidated data, which helps maintain the current
     * scroll position.
     *
     * we make use of PagingState.anchorPosition. If you've wondered how the paging library
     * knows to fetch more items, this is a clue! When the UI tries to read items from PagingData,
     * it tries to read at a certain index. If data was read, then that data is displayed in
     * the UI. If there is no data, however, then the paging library knows it needs to fetch
     * data to fulfill the failed read request. The last index that successfully fetched data
     * when read is the anchorPosition.
     *
     * When we're refreshing, we grab the key of the Article closest to the anchorPosition to
     * use as the load key. That way, when we start loading again from a new PagingSource, the
     * set of fetched items includes items that were already loaded, which ensures a smooth
     * and consistent user experience.
     *
     * The refresh key is used for the initial load of the next PagingSource, after invalidation
     *
     */
    override fun getRefreshKey(state: PagingState<Int, Article>): Int? {
        // In our case we grab the item closest to the anchor position
        // then return its id - (state.config.pageSize / 2) as a buffer
        val anchorPosition = state.anchorPosition ?: return null
        val article = state.closestItemToPosition(anchorPosition) ?: return null
        return ensureValidKey(key = article.id - (state.config.pageSize / 2))
    }

    /**
     * The load() function will be called by the Paging library to asynchronously fetch more
     * data to be displayed as the user scrolls around. The LoadParams object keeps information
     * related to the load operation, including the following:
     *
     * 1. Key of the page to be loaded - If this is the first time that load() is called,
     * LoadParams.key will be null. In this case, you will have to define the initial page
     * key. For our project, we use the article ID as the key. Let's also add a STARTING_KEY
     * constant of 0 to the top of the ArticlePagingSource file for the initial page key.
     *
     * 2. Load size - the requested number of items to load.
     *
     * The load() function returns a LoadResult. The LoadResult can be one of the following types:
     *
     * 1. LoadResult.Page, if the result was successful.
     * 2. LoadResult.Error, in case of error.
     * 3. LoadResult.Invalid, if the PagingSource should be invalidated because it can no
     * longer guarantee the integrity of its results.
     *
     * A LoadResult.Page has three required arguments:
     *
     * 1. data: A List of the items fetched.
     * 2. prevKey: The key used by the load() method if it needs to fetch items behind the current page.
     * 3. nextKey: The key used by the load() method if it needs to fetch items after the current page.
     *
     * ...and two optional ones:
     *
     * 4. itemsBefore: The number of placeholders to show before the loaded data.
     * 5. itemsAfter: The number of placeholders to show after the loaded data.
     */
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Article> {
        // Start paging with the STARTING_KEY if this is the first load
        val start = params.key ?: STARTING_KEY
        // Load as many items as hinted by params.loadSize
        val range = start.until(start + params.loadSize)

        if (start != STARTING_KEY) delay(LOAD_DELAY_MILLIS)

        /**
         * The nextKey or prevKey is null if there is no more data to be loaded in the
         * corresponding direction. In our case, for prevKey:
         *
         * 1. If the startKey is the same as STARTING_KEY we return null since we can't load more
         * items behind this key.
         * 2. Otherwise, we take the first item in the list and load LoadParams.loadSize behind it
         * making sure to never return a key less than STARTING_KEY. We do this by defining the ensureValidKey()
         * method
         *
         * For nextKey:
         *
         * Since we support loading infinite items, we pass in range.last + 1.
         * */
        return LoadResult.Page(
            data = range.map { number ->
                // Generate consecutive increasing numbers as the article id
                Article(
                    id = number,
                    title = "Article $number",
                    description = "This describes article $number",
                    created = firstArticleCreated.minusDays(number.toLong())
                )
            },
            // Make sure we don't try to load items behind the STARTING_KEY
            prevKey = when (start) {
                STARTING_KEY -> null
                else -> ensureValidKey(key = range.first - params.loadSize)
            },
            nextKey = range.last + 1
        )
    }

    /**
     * Makes sure the paging key is never less than [STARTING_KEY]
     */
    private fun ensureValidKey(key: Int) = max(STARTING_KEY, key)

}