package com.tirex.pokedexapp

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.util.Log
import android.view.View
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

import com.google.gson.Gson
import com.squareup.picasso.Picasso
import com.tirex.pokedexapp.databinding.ItemPokemonBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.Locale


class PokemonViewHolder(view: View) : RecyclerView.ViewHolder(view) {

    private val binding = ItemPokemonBinding.bind(view)


    fun bind(pokemonItemResponse: PokemonResponseInfo, onItemSelected: (String) -> Unit) {
        val pokemonName = pokemonItemResponse.name.replace(" ", "")
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
        binding.tvPokemonName.text = pokemonName
        binding.root.setOnClickListener {
            onItemSelected(pokemonItemResponse.name)
            imageAnimation(binding.root)
        }

        val url = pokemonItemResponse.url
        if (!url.isNullOrEmpty()) {
            CoroutineScope(Dispatchers.Main).launch {
                val (id, spriteUrl, _) = fetchSpriteUrl(url)
                spriteUrl?.let {
                    Picasso.get()
                        .load(it)
                        .placeholder(R.drawable.placeholder)
                        .into(binding.ivPokemonImage)
                }
                binding.tvPokemonNumber.text = id.toString()
                pokemonTypeColor(url)
            }
        } else {
            Log.e("PokemonViewHolder", "URL proporcionada es nula o vacía")
        }
    }

    private fun imageAnimation(view: View) {
        val scaleUpX = ObjectAnimator.ofFloat(view, "scaleX", 1.05f).apply {
            duration = 500
        }

        val scaleUpY = ObjectAnimator.ofFloat(view, "scaleY", 1.05f).apply {
            duration = 500
        }

        val scaleDownX = ObjectAnimator.ofFloat(view, "scaleX", 1f).apply {
            duration = 500
        }
        val scaleDownY = ObjectAnimator.ofFloat(view, "scaleY", 1f).apply {
            duration = 500
        }

        val upAnimatorSet = AnimatorSet()
        upAnimatorSet.play(scaleUpX).with(scaleUpY)

        val downAnimatorSet = AnimatorSet()
        downAnimatorSet.play(scaleDownX).with(scaleDownY)

        upAnimatorSet.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                downAnimatorSet.start()
            }
        })

        upAnimatorSet.start()
    }

    private suspend fun fetchSpriteUrl(url: String): Triple<Int?, String?, Pair<TypeName?, TypeName?>?> {
        return withContext(Dispatchers.IO) {
            val client = OkHttpClient()
            val request = Request.Builder()
                .url(url)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IOException("Unexpected code $response")

                val jsonResponse = response.body?.string()
                val gson = Gson()
                val urlData = gson.fromJson(jsonResponse, Url::class.java)

                val id = urlData.id
                val imageUrl = urlData.sprites?.image?.home?.frontImage
                val types = urlData.types

                val firstType = types?.getOrNull(0)?.type
                val secondType = types?.getOrNull(1)?.type

                return@withContext Triple(id, imageUrl, Pair(firstType, secondType))
            }
        }
    }

    fun updatePokemonDetail(pokemon: PokemonUrl) {

        binding.apply {

            val pokemonImageUrl = pokemon.sprites.others?.image?.frontImage
            if (pokemonImageUrl != null) {
                Picasso.get()
                    .load(pokemonImageUrl)
                    .placeholder(R.drawable.placeholder)
                    .into(ivPokemonImage)
                Log.d("Url imagen", "{$pokemonImageUrl")
            } else {
                ivPokemonImage.setImageResource(R.drawable.placeholder)
            }

            val pokemonType = pokemon.types.firstOrNull()?.type?.typeName
            if (pokemonType != null) {
                pokemonTypeColor(pokemonType)
            } else {
                Log.e("PokemonType", "Tipo de Pokémon no disponible")
                pokemonTypeColor("default")
            }
        }
    }


    private fun pokemonTypeColor(url: String) {
        CoroutineScope(Dispatchers.Main).launch {
            val (_, _, types) = fetchSpriteUrl(url)
            val firstType = types?.first?.typeName
            val color = when (firstType) {
                "steel" -> ContextCompat.getColor(binding.cvPokemon.context, R.color.steel_type)
                "water" -> ContextCompat.getColor(binding.cvPokemon.context, R.color.water_type)
                "bug" -> ContextCompat.getColor(binding.cvPokemon.context, R.color.bug_type)
                "dragon" -> ContextCompat.getColor(binding.cvPokemon.context, R.color.dragon_type)
                "electric" -> ContextCompat.getColor(binding.cvPokemon.context, R.color.electric_type)
                "ghost" -> ContextCompat.getColor(binding.cvPokemon.context, R.color.ghost_type)
                "fire" -> ContextCompat.getColor(binding.cvPokemon.context, R.color.fire_type)
                "fairy" -> ContextCompat.getColor(binding.cvPokemon.context, R.color.fairy_type)
                "ice" -> ContextCompat.getColor(binding.cvPokemon.context, R.color.ice_type)
                "fighting" -> ContextCompat.getColor(binding.cvPokemon.context, R.color.fighting_type)
                "normal" -> ContextCompat.getColor(binding.cvPokemon.context, R.color.normal_type)
                "grass" -> ContextCompat.getColor(binding.cvPokemon.context, R.color.grass_type)
                "psychic" -> ContextCompat.getColor(binding.cvPokemon.context, R.color.psychic_type)
                "rock" -> ContextCompat.getColor(binding.cvPokemon.context, R.color.rock_type)
                "dark" -> ContextCompat.getColor(binding.cvPokemon.context, R.color.dark_type)
                "ground" -> ContextCompat.getColor(binding.cvPokemon.context, R.color.ground_type)
                "poison" -> ContextCompat.getColor(binding.cvPokemon.context, R.color.poison_type)
                "flying" -> ContextCompat.getColor(binding.cvPokemon.context, R.color.flying_type)
                else -> ContextCompat.getColor(binding.cvPokemon.context, R.color.default_type)
            }

            binding.cvPokemon.setCardBackgroundColor(color)
            binding.cvPokemonNumber.setCardBackgroundColor(color)
        }
    }


}

