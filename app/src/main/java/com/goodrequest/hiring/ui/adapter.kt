package com.goodrequest.hiring.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role.Companion.Image
import androidx.compose.ui.unit.dp
import androidx.recyclerview.widget.RecyclerView
import coil.compose.rememberImagePainter
import coil.load
import com.goodrequest.hiring.R

@Composable
fun PokemonList(pokemons: List<Pokemon>) {
    LazyColumn {
        items(pokemons.size) { index ->
            val pokemon = pokemons[index]
            Card(modifier = Modifier.padding(8.dp)) {
                Column {
                    pokemon.detail?.let { detail ->
                        Image(
                            painter = rememberImagePainter(detail.image) {
                                crossfade(true)
                                placeholder(R.drawable.ic_launcher_foreground)
                            },
                            contentDescription = null,
                            modifier = Modifier.size(100.dp)
                        )
                        Text(text = "Move: ${detail.move}", modifier = Modifier.padding(16.dp))
                        Text(text = "Weight: ${detail.weight}", modifier = Modifier.padding(16.dp))
                    }
                    Text(text = pokemon.name, modifier = Modifier.padding(16.dp))
                }
            }
        }
    }
}