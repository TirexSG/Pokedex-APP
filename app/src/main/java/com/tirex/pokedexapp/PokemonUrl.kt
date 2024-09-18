package com.tirex.pokedexapp

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class PokemonUrl(
    val abilities: List<Any>, // Ajusta según el tipo real de `abilities`
    val baseExperience: Int,
    @SerializedName("cries") val cries: Map<String, String>, // Ajusta según el tipo real de `cries`
    val forms: List<Any>, // Ajusta según el tipo real de `forms`
    val gameIndices: List<Any>, // Ajusta según el tipo real de `game_indices`
    val height: Int,
    val heldItems: List<Any>, // Ajusta según el tipo real de `held_items`
    val id: Int,
    val isDefault: Boolean,
    val locationAreaEncounters: String,
    val moves: List<Any>, // Ajusta según el tipo real de `moves`
    val name: String?,
    val order: Int,
    val pastAbilities: List<Any>, // Ajusta según el tipo real de `past_abilities`
    val pastTypes: List<Any>, // Ajusta según el tipo real de `past_types`
    val species: PokemonSpecies, // Ajusta según el tipo real de `species`
    val sprites: Others,
    val stats: List<Any>,
    val types: List<Types>,
    val weight: Int)

data class PokemonSpecies(
    val name: String,
    val url: String
)


data class DescriptionUrl(
    @Expose @SerializedName("flavor_text_entries") val entries: List<FlavorTextEntry>
)

data class FlavorTextEntry(
    @Expose @SerializedName("flavor_text") val flavorText: String,
    val language: Language,
    val version: Version
)

data class Language(
    val name: String,
    val url: String
)

data class Version(
    val name: String,
    val url: String
)

data class Others(
    @Expose @SerializedName("other") val others: Image?,
    @Expose @SerializedName("versions") val version: GenVImage?
)

data class GenVImage(
    @Expose @SerializedName("generation-v") val genV: BlackWhiteImage?,
)

data class BlackWhiteImage(
    @Expose @SerializedName("black-white") val animated: AnimatedImage?,
)

data class AnimatedImage(
    @Expose @SerializedName("front_default") val front: String?,
)

data class Image(
    @Expose @SerializedName("home") val image: FrontImages?
)

data class FrontImages(
    @Expose @SerializedName("front_default") val frontImage: String?,
    @Expose @SerializedName("front_shiny") val frontShiny: String?
)

data class Types(
    @Expose @SerializedName("type") val type: TypesName?
)

data class TypesName(
    @Expose @SerializedName("name") val typeName: String?
)