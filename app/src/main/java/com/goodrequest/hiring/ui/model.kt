package com.goodrequest.hiring.ui

import android.content.Context
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.goodrequest.hiring.PokemonApi
import kotlinx.coroutines.launch
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
        Log.d("PokemonViewModel", "load start")
        _state.value = _state.value.copy(isLoading = forceRefresh)
        if (pokemons.value == null || forceRefresh) {
            viewModelScope.launch {
                try {
                    val result = api.getPokemons(page = 1)
                    pokemons.postValue(result)
                    _state.value = _state.value.copy(isLoading = false)
                } catch (e: Exception) {
                    errorMessage.postValue(e.message)
                } catch (e: UnknownHostException) {
                   errorMessage.postValue("No internet connection")
                }
            }
        }
        Log.d("PokemonViewModel", "load end")
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