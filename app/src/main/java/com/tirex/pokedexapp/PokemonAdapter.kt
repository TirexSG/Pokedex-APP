package com.tirex.pokedexapp

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView


class PokemonAdapter(
    private var pokemonList: MutableList<PokemonResponseInfo> = mutableListOf() ,
    private val onItemSelected: (String) -> Unit

    ) : RecyclerView.Adapter<PokemonViewHolder>() {

    fun updateClearList(pokemonList: MutableList<PokemonResponseInfo>) {
        this.pokemonList.clear()
        this.pokemonList.addAll(pokemonList)
        notifyDataSetChanged()
    }

   fun updateList(pokemonList: MutableList<PokemonResponseInfo>) {
       this.pokemonList.addAll(pokemonList)
       notifyDataSetChanged()
   }

    fun addPokemons(newPokemons: List<PokemonResponseInfo>) {
        this.pokemonList.addAll(newPokemons)
        notifyDataSetChanged()
    }

    fun getPositionForPokemon(pokemonId: Int?): Int {
        return pokemonList.indexOfFirst {
            val urlId = it.url.split("/").filter { segment -> segment.isNotEmpty() }.lastOrNull()?.toIntOrNull()
            urlId == pokemonId
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PokemonViewHolder {
        return PokemonViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_pokemon, parent, false)
        )
    }

    override fun onBindViewHolder(viewholder: PokemonViewHolder, position: Int) {
        viewholder.bind(pokemonList[position], onItemSelected)
    }

    override fun getItemCount() = pokemonList.size
}
