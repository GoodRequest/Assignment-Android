package com.goodrequest.hiring.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.SnackbarDuration
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelLazy
import coil.compose.AsyncImage
import com.goodrequest.hiring.PokemonApi
import com.goodrequest.hiring.R
import kotlinx.coroutines.launch

class PokemonActivity: ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val vm by viewModel { PokemonViewModel(it, null, PokemonApi) }
        vm.load()

        setContent {
            MaterialTheme {
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        val pokemons by vm.pokemons.observeAsState(initial = Result.Failure(Exception()))
                        when (val result = pokemons) {
                            is Result.Success<List<Pokemon>> -> PokemonList(pokemons = result.data)
                            is Result.Failure -> FailureView(onRetry = { vm.load(forceRefresh = true) })
                            else -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                        }
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalMaterialApi::class)
    @Composable
    fun PokemonList(pokemons: List<Pokemon>) {
        //inject viewModel here
        val vm by viewModel { PokemonViewModel(it, null, PokemonApi) }
        val errorMessage = vm.state.value.errorMessage
        val pullRefreshState = rememberPullRefreshState(
            refreshing = vm.state.value.isLoading,
            onRefresh = { vm.load(forceRefresh = true) }
        )
        val lazyColumnListState = rememberLazyListState()
        val coroutineScope = rememberCoroutineScope()
        val shouldStartPaginate = remember {
            derivedStateOf {
                vm.canPaginate && (lazyColumnListState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -9) >= (lazyColumnListState.layoutInfo.totalItemsCount - 1)
            }
        }
        val snackbarHostState = remember { SnackbarHostState() }
        Scaffold(
            // show pokemons count
            topBar = { TopAppBar(title = { Text("Gotta Catch 'Em All! Loaded: ${(pokemons.size)}") }) },
            snackbarHost = {  SnackbarHost(
                hostState = snackbarHostState,
            ) },
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .pullRefresh(pullRefreshState)
            ) {
                LazyColumn(state = lazyColumnListState) {
                    items(pokemons.size) { index ->
                        val pokemon = pokemons[index]
                        Card(
                            modifier = Modifier
                                .padding(8.dp)
                                .fillMaxWidth()
                        ) {
                            Row {
                                if (pokemon.detail == null) {
                                    // Display placeholder image for Pokemon with failed details
                                    Image(
                                        painter = painterResource(id = R.drawable.ic_launcher_foreground),
                                        contentDescription = "Placeholder image for ${pokemon.name}",
                                        modifier = Modifier.fillMaxHeight()
                                    )
                                } else {
                                    pokemon.detail.let { detail ->
                                        AsyncImage(
                                            model = detail.image,
                                            contentDescription = null,
                                            placeholder = painterResource(id = R.drawable.ic_launcher_foreground),
                                            modifier = Modifier.fillMaxHeight()
                                        )
                                    }
                                }
                                Column {
                                    Text(text = pokemon.name, modifier = Modifier.padding(16.dp))
                                    pokemon.detail?.let { detail ->
                                        Text(
                                            text = "Move: ${detail.move}",
                                            modifier = Modifier.padding(16.dp)
                                        )
                                        Text(
                                            text = "Weight: ${detail.weight}",
                                            modifier = Modifier.padding(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                        if (index == pokemons.size - 1) {
                            LaunchedEffect(remember {
                                derivedStateOf { lazyColumnListState.firstVisibleItemIndex }
                            }) {
                                vm.load()
                            }
                        }
                    }
                }
                PullRefreshIndicator(
                    refreshing = vm.state.value.isLoading,
                    state = pullRefreshState,
                    modifier = Modifier.align(Alignment.TopCenter),
                    backgroundColor = if (vm.state.value.isLoading) Color.Red else Color.Green,
                )
                LaunchedEffect(vm.state.value.errorMessage) {
                    if (errorMessage != null) {
                        snackbarHostState.showSnackbar(
                            message = errorMessage,
                            actionLabel = "Zavřít",
                            duration = SnackbarDuration.Short,
                        )
                    }
                }
            }
            }
        }
    }

    @Composable
    fun FailureView(onRetry: () -> Unit) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "Something went wrong!", fontSize = 20.sp, modifier = Modifier.padding(bottom = 16.dp))
            Button(onClick = onRetry) {
                Text(text = "Try again")
            }
        }

}

sealed class Result<out T> {
    data class Success<out T>(val data: T) : Result<T>()
    data class Failure(val exception: Exception) : Result<Nothing>()
}

enum class ListState {
    IDLE,
    LOADING,
    PAGINATING,
    ERROR,
    PAGINATION_EXHAUST,
}

/**
 * Helper function that enables us to directly call constructor of our ViewModel but also
 * provides access to SavedStateHandle.
 * Shit like this is usually generated by Hilt
 */
inline fun <reified VM: ViewModel> ComponentActivity.viewModel(crossinline create: (SavedStateHandle) -> VM) =
    ViewModelLazy(
        viewModelClass = VM::class,
        storeProducer = { viewModelStore },
        factoryProducer = {
            object: AbstractSavedStateViewModelFactory(this@viewModel, null) {
                override fun <T : ViewModel> create(key: String, type: Class<T>, handle: SavedStateHandle): T =
                    create(handle) as T
            }
    })