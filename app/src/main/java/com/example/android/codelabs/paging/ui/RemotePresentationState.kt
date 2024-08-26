package com.example.android.codelabs.paging.ui

import androidx.paging.CombinedLoadStates
import androidx.paging.LoadState
import androidx.paging.Pager
import androidx.paging.PagingSource
import androidx.paging.RemoteMediator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.scan

/**
 * An enum representing the status of items in the as fetched by the
 * [Pager] when used with a [RemoteMediator]
 *
 * Until now, when reading from CombinedLoadStates, we've always read from
 * CombinedLoadStates.source. When using a RemoteMediator however, accurate
 * loading information can only be obtained by checking both CombinedLoadStates.source
 * and CombinedLoadStates.mediator. In particular, we currently trigger a scroll to
 * the top of the list on new queries when the source LoadState is NotLoading. We also
 * have to make sure that our newly-added RemoteMediator has a LoadState of NotLoading as well.
 */
enum class RemotePresentationState {
    INITIAL, REMOTE_LOADING, SOURCE_LOADING, PRESENTED
}

/**
 * Reduces [CombinedLoadStates] into [RemotePresentationState]. It operates on the assumption that
 * successful [RemoteMediator] fetches always cause invalidation of the [PagingSource] as in the
 * case of the [PagingSource] provided by Room.
 */
@OptIn(ExperimentalCoroutinesApi::class)
fun Flow<CombinedLoadStates>.asRemotePresentationState(): Flow<RemotePresentationState> =
    scan(RemotePresentationState.INITIAL) { state, loadState ->
        when (state) {
            RemotePresentationState.PRESENTED -> when (loadState.mediator?.refresh) {
                is LoadState.Loading -> RemotePresentationState.REMOTE_LOADING
                else -> state
            }
            RemotePresentationState.INITIAL -> when (loadState.mediator?.refresh) {
                is LoadState.Loading -> RemotePresentationState.REMOTE_LOADING
                else -> state
            }
            RemotePresentationState.REMOTE_LOADING -> when (loadState.source.refresh) {
                is LoadState.Loading -> RemotePresentationState.SOURCE_LOADING
                else -> state
            }
            RemotePresentationState.SOURCE_LOADING -> when (loadState.source.refresh) {
                is LoadState.NotLoading -> RemotePresentationState.PRESENTED
                else -> state
            }
        }
    }
        .distinctUntilChanged()