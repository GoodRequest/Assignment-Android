package com.goodrequest.hiring.ui

import android.content.Context
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.goodrequest.hiring.PokemonApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.UnknownHostException

class PokemonViewModel(
    state: SavedStateHandle,
    context: Context?,
    private val api: PokemonApi) : ViewModel() {
    private val _state = mutableStateOf(PokemonScreenState())
    val state: State<PokemonScreenState> = _state

    val pokemons = state.getLiveData<Result<List<Pokemon>>?>("pokemons", null)
    private val errorMessage = state.getLiveData<String?>("errorMessage", null)
    private var page by mutableIntStateOf(1)
    var canPaginate by mutableStateOf(false)
    var listState by mutableStateOf(ListState.IDLE)

    /**
     * Load pokemons from API.
     * If no pokemon is loaded yet, load first page.
     * If pull-refresh is performed with one-page loaded, reload first page.
     * If pull-refresh is performed with more than two pages loaded, drop pokemons and leave only last 20.
     */
    fun load(forceRefresh: Boolean = false) {
        _state.value = _state.value.copy(isLoading = forceRefresh)
        when {
            pokemons.value == null -> {
                loadFirstPage()
            }
            canPaginate && !forceRefresh -> {
                loadNextPage()
            }
            forceRefresh -> {
                dropAndReloadLastPage()
            }
            else -> {
                loadFirstPage()
            }
        }
    }

    /**
     * Load first page of pokemons from API.
     */
    private fun loadFirstPage() {
        page = 1
        fetchPokemons()
    }

    /**
     * Load next page of pokemons from API.
     */
    private fun loadNextPage() {
        page++
        fetchPokemons()
    }

    /**
     * Show actual set of pokemons and reload last page from API.
     * If reload is successful, drop all pokemons except last 20.
     */
    private fun dropAndReloadLastPage() {
        val oldPokemons = pokemons.value?.let { (it as? Result.Success)?.data } ?: emptyList()
        pokemons.value = Result.Success(oldPokemons.takeLast(20))

        page = oldPokemons.size / 20
        fetchPokemons()
        // remove old pokemons
        pokemons.value = Result.Success(emptyList())





    }

    /**
     * Fetch pokemons from API and update [pokemons] and [canPaginate] values.
     */
    private fun fetchPokemons() {
        viewModelScope.launch(Dispatchers.IO) { // Use Dispatchers.IO for network operations
            try {
                val result = api.getPokemons(page = page)

                    result.let {
                        val detailedPokemons = (result as? Result.Success<List<Pokemon>>)?.data?.map { pokemon ->
                            val detailResult = async { api.getPokemonDetail(pokemon) }
                            detailResult.await().let { detail ->
                                when (detail) {
                                    is Result.Success -> pokemon.copy(detail = detail.data)
                                    is Result.Failure -> pokemon
                                }
                            }
                        }
                        if (detailedPokemons != null) {
                            val oldPokemons = pokemons.value?.let { (it as? Result.Success)?.data } ?: emptyList()
                            pokemons.postValue(Result.Success(oldPokemons + detailedPokemons))
                            canPaginate = detailedPokemons.isNotEmpty()
                        } else {
                            pokemons.postValue(Result.Failure(Exception("Failed to load Pokemon details")))
                        }
                    _state.value = _state.value.copy(isLoading = false)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { // Switch back to main thread to update UI
                    errorMessage.postValue(e.message)
                }
            } catch (e: UnknownHostException) {
                withContext(Dispatchers.Main) { // Switch back to main thread to update UI
                    errorMessage.postValue("No internet connection")
                }
            }
        }
    }
}

data class Pokemon(
    val id     : String,
    val name   : String,
    val detail : PokemonDetail? = null)

data class PokemonDetail(
    val image  : String,
    val move   : String,
    val weight : Int)

data class PokemonScreenState(
    val pokemons: Result<List<Pokemon>>? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)