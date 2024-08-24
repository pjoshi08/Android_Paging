/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.codelabs.paging.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.android.codelabs.paging.data.Article
import com.example.android.codelabs.paging.data.ArticleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

private const val ITEMS_PER_PAGE = 50

/**
 * ViewModel for the [ArticleActivity] screen.
 * The ViewModel works with the [ArticleRepository] to get the data.
 */
class ArticleViewModel(
    repository: ArticleRepository,
) : ViewModel() {

    /**
     * To construct the PagingData, we'll use one of several different builder methods from the
     * Pager class depending on which API we want to use to pass the PagingData to other layers
     * of our app:
     *
     * Kotlin Flow - use Pager.flow.
     * LiveData - use Pager.liveData.
     * RxJava Flowable - use Pager.flowable.
     * RxJava Observable - use Pager.observable.
     * As we're already using Flow in our app, we'll continue with this approach; but instead
     * of using Flow<List<Article>>, we'll use Flow<PagingData<Article>>.
     *
     * No matter which PagingData builder you use, you'll have to pass the following parameters:
     *
     * PagingConfig. This class sets options regarding how to load content from a PagingSource
     * such as how far ahead to load, the size request for the initial load, and others. The
     * only required parameter you have to define is the page size—how many items should be
     * loaded in each page. By default, Paging will keep all of the pages you load in memory.
     * To ensure that you're not wasting memory as the user scrolls, set the maxSize parameter
     * in PagingConfig. By default, Paging will return null items as a placeholder for content
     * that is not yet loaded if Paging can count the unloaded items and if the
     * enablePlaceholders config flag is true. That way, you will be able to display a
     * placeholder view in your adapter. To simplify the work in this codelab, let's disable
     * the placeholders by passing enablePlaceholders = false.
     * A function that defines how to create the PagingSource. In our case, we'll be creating
     * an ArticlePagingSource, so we need a function that tells the Paging library how to do that.
     * Note: The PagingConfig.pageSize should be enough for several screens' worth of items.
     * If the page is too small, your list might flicker as pages' content doesn't cover the
     * full screen. Larger page sizes are good for loading efficiency, but can increase latency
     * when the list is updated. If you would like to load a different amount of items for
     * your first page, use PagingConfig.initialLoadSize.
     *
     * The default PagingConfig.maxSize is unbounded, so pages are never dropped. If you do
     * want to drop pages, make sure that you keep maxSize to a high enough number that it
     * doesn't result in too many network requests when the user changes the scroll direction.
     * The minimum value is pageSize + prefetchDistance * 2.
     */
    val items: Flow<PagingData<Article>> = Pager(
        config = PagingConfig(pageSize = ITEMS_PER_PAGE, enablePlaceholders = false),
        pagingSourceFactory = {
            // The pagingSourceFactory lambda should always return a brand new PagingSource
            // when invoked as PagingSource instances are not reusable.
            repository.articlePagingSource()
        }
    )
        .flow
        // To maintain paging state through configuration or navigation changes, we use the
        // cachedIn() method passing it the viewModelScope
        // Do not use the stateIn() or sharedIn() operators with PagingData Flows as
        // PagingData Flows are not cold.
        .cachedIn(viewModelScope)
}
