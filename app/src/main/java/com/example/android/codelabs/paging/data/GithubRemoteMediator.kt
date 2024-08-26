package com.example.android.codelabs.paging.data

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import com.example.android.codelabs.paging.api.GithubService
import com.example.android.codelabs.paging.api.IN_QUALIFIER
import com.example.android.codelabs.paging.db.RemoteKeys
import com.example.android.codelabs.paging.db.RepoDatabase
import com.example.android.codelabs.paging.model.Repo
import okio.IOException
import retrofit2.HttpException

// GitHub page API is 1 based: https://developer.github.com/v3/#pagination
private const val GITHUB_STARTING_PAGE_INDEX = 1

@OptIn(ExperimentalPagingApi::class)
// This class will be recreated for every new query
class GithubRemoteMediator(
    private val query: String,
    private val service: GithubService,
    private val repoDatabase: RepoDatabase
) : RemoteMediator<Int, Repo>() {

    /**
     * o be able to build the network request, the load method has 2 parameters that should give
     * us all the information we need:
     *
     * 1. PagingState - this gives us information about the pages that were loaded before, the
     * most recently accessed index in the list, and the PagingConfig we defined when
     * initializing the paging stream.
     * 2. LoadType - this tells us whether we need to load data at the end (LoadType.APPEND) or at
     * the beginning of the data (LoadType.PREPEND) that we previously loaded, or if this the
     * first time we're loading data (LoadType.REFRESH).
     *
     * For example, if the load type is LoadType.APPEND then we retrieve the last item that was
     * loaded from the PagingState. Based on that we should be able to find out how to load the
     * next batch of Repo objects, by computing the next page to be loaded.
     */
    override suspend fun load(loadType: LoadType, state: PagingState<Int, Repo>): MediatorResult {
        val page = when (loadType) {
            LoadType.REFRESH -> {
                val remoteKeys = getRemoteKeyClosestToCurrentPosition(state)
                remoteKeys?.nextKey?.minus(1) ?: GITHUB_STARTING_PAGE_INDEX
            }

            LoadType.PREPEND -> {
                val remoteKeys = getRemoteKeyForFirstItem(state)
                // If remoteKeys is null, that means the refresh result is not in the database yet.
                val prevKey = remoteKeys?.prevKey
                    ?: return MediatorResult.Success(endOfPaginationReached = remoteKeys != null)
                prevKey
            }

            LoadType.APPEND -> {
                val remoteKeys = getRemoteKeyForLastItem(state)
                // If remoteKeys is null, that means the refresh result is not in the database yet.
                // We can return Success with endOfPaginationReached = false because Paging
                // will call this method again if RemoteKeys becomes non-null.
                // If remoteKeys is NOT NULL but its nextKey is null, that means we've reached
                // the end of pagination for append.
                val nextKey = remoteKeys?.nextKey
                    ?: return MediatorResult.Success(endOfPaginationReached = remoteKeys != null)
                nextKey
            }
        }
        val apiQuery = query + IN_QUALIFIER

        try {
            val apiResponse = service.searchRepos(apiQuery, page, state.config.pageSize)

            val repos = apiResponse.items
            val endOfPaginationReached = repos.isEmpty()
            repoDatabase.withTransaction {
                // clear all tables in the database
                if (loadType == LoadType.REFRESH) {
                    repoDatabase.remoteKeysDao().clearRemoteKeys()
                    repoDatabase.reposDao().clearRepos()
                }
                val prevKey = if (page == GITHUB_STARTING_PAGE_INDEX) null else page - 1
                val nextKey = if (endOfPaginationReached) null else page + 1
                val keys = repos.map {
                    RemoteKeys(repoId = it.id, prevKey = prevKey, nextKey = nextKey)
                }
                repoDatabase.remoteKeysDao().insertAll(keys)
                repoDatabase.reposDao().insertAll(repos)
            }
            return MediatorResult.Success(endOfPaginationReached = endOfPaginationReached)
        } catch (exception: IOException) {
            return MediatorResult.Error(exception)
        } catch (exception: HttpException) {
            return MediatorResult.Error(exception)
        }
    }

    /**
     * LoadType.REFRESH gets called when it's the first time we're loading data, or when
     * PagingDataAdapter.refresh() is called; so now the point of reference for loading
     * our data is the state.anchorPosition. If this is the first load, then the
     * anchorPosition is null. When PagingDataAdapter.refresh() is called, the anchorPosition
     * is the first visible position in the displayed list, so we will need to load the
     * page that contains that specific item.
     *
     * 1. Based on the anchorPosition from the state, we can get the closest Repo item to that
     * position by calling state.closestItemToPosition().
     * 2. Based on the Repo item, we can get the RemoteKeys from the database.
     */
    private suspend fun getRemoteKeyClosestToCurrentPosition(
        state: PagingState<Int, Repo>
    ): RemoteKeys? {
        // The paging library is trying to load data after the anchor position
        // Get the item closest to the anchor position
        return state.anchorPosition?.let { position ->
            state.closestItemToPosition(position)?.id?.let { repoId ->
                repoDatabase.remoteKeysDao().remoteKeysRepoId(repoId)
            }
        }
    }

    /**
     * When we need to load data at the end of the currently loaded data set, the load parameter
     * is LoadType.APPEND. So now, based on the last item in the database we need to compute
     * the network page key.
     *
     * We need to get the remote key of the last Repo item loaded from the database.
     */
    private suspend fun getRemoteKeyForLastItem(state: PagingState<Int, Repo>): RemoteKeys? {
        // Get the last page that was retrieved, that contained items.
        // From that last page, get the last item
        return state.pages.lastOrNull { it.data.isNotEmpty() }?.data?.lastOrNull()
            ?.let { repo ->
                // Get the remote keys of the last item retrieved
                repoDatabase.remoteKeysDao().remoteKeysRepoId(repo.id)
            }
    }

    /**
     * When we need to load data at the beginning of the currently loaded data set, the load
     * parameter is LoadType.PREPEND. Based on the first item in the database we need to compute
     * the network page key.
     *
     * We need to get the remote key of the first Repo item loaded from the database.
     */
    private suspend fun getRemoteKeyForFirstItem(state: PagingState<Int, Repo>): RemoteKeys? {
        // Get the first page that was retrieved, that contained items.
        // From that first page, get the first item
        return state.pages.firstOrNull { it.data.isNotEmpty() }?.data?.firstOrNull()
            ?.let { repo ->
                // Get the remote keys of the first items retrieved
                repoDatabase.remoteKeysDao().remoteKeysRepoId(repo.id)
            }
    }
}