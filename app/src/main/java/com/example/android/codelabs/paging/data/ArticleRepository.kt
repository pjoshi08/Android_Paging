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

package com.example.android.codelabs.paging.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.time.LocalDateTime

private val firstArticleCreatedTime = LocalDateTime.now()

/**
 * Other reasons we should explore paging through the articles include the following:
 *
 * 1. The ViewModel keeps all items loaded in memory in the items StateFlow. This is a major concern
 * when the dataset gets really large as it can impact performance.
 * 2. Updating one or more articles in the list when they've changed becomes more expensive the
 * bigger the list of articles gets.
 *
 * When implementing pagination, we want to be confident the following conditions are met:
 *
 * 1. Properly handling requests for the data from the UI, ensuring that multiple requests
 * aren't triggered at the same time for the same query.
 * 2. Keeping a manageable amount of retrieved data in memory.
 * 3. Triggering requests to fetch more data to supplement the data we've already fetched.
 *
 * We can achieve all this with a PagingSource. A [PagingSource] defines the source of data by
 * specifying how to retrieve data in incremental chunks. The PagingData object then pulls data
 * from the PagingSource in response to loading hints that are generated as the user scrolls in a RecyclerView.
 */

/**
 * Repository class that mimics fetching [Article] instances from an asynchronous source.
 */
class ArticleRepository {

    fun articlePagingSource() = ArticlePagingSource()
}
