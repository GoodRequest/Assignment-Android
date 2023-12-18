package com.goodrequest.hiring.ui

import android.content.Context
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
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
    val errorMessage = state.getLiveData<String?>("errorMessage", null)

    fun load(forceRefresh: Boolean = false) {
        _state.value = _state.value.copy(isLoading = forceRefresh)
        if (pokemons.value == null || forceRefresh) {
            viewModelScope.launch(Dispatchers.IO) { // Use Dispatchers.IO for network operations
                try {
                    val result = api.getPokemons(page = 1)
                    withContext(Dispatchers.Main) { // Switch back to main thread to update UI
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
                                pokemons.postValue(Result.Success(detailedPokemons))
                            } else {
                                pokemons.postValue(Result.Failure(Exception("Failed to load Pokemon details")))
                            }
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