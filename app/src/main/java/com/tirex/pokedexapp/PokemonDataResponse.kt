package com.tirex.pokedexapp

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class PokemonResponse(
    @Expose @SerializedName("count") val count: Int,
    @Expose @SerializedName("next") val next: String,
    @Expose @SerializedName("previous") val previous: String,
    @Expose @SerializedName("results") val results: List<PokemonResponseInfo>
)

data class PokemonResponseInfo(
    @Expose @SerializedName("name") val name: String,
    @Expose @SerializedName("url") val url: String
)

data class PokemonResponseType(
    @SerializedName("pokemon") val pokemonList: List<PokemonTypeWrapper>
)

data class PokemonTypeWrapper(
    @SerializedName("pokemon") val pokemon: PokemonResponseInfo,
    @SerializedName("slot") val slot: Int
)



data class Url(
    @Expose @SerializedName("sprites") val sprites: Other?,
    @Expose @SerializedName("id") val id: Int?,
    @Expose @SerializedName("types") val types: List<Type>?,
    @Expose @SerializedName("forms") val forms: List<Form>?

)

data class Form(
    @Expose @SerializedName("name") val name: String?
)

data class Type(
    @Expose @SerializedName("type") val type: TypeName?
)

data class TypeName(
    @Expose @SerializedName("name") val typeName: String?
)

data class Other(
    @Expose @SerializedName("other") val image: Images?,
    @Expose @SerializedName("versions") val version: GenV?
)

data class GenV(
    @Expose @SerializedName("generation-v") val genV: BlackWhite?,
)

data class BlackWhite(
    @Expose @SerializedName("black-white") val blackWhite: Animated?,
)

data class Animated(
    @Expose @SerializedName("animated") val animated: FrontImage?,
)

data class Images(
    @Expose @SerializedName("home") val home: FrontImage?
)

data class FrontImage(
    @Expose @SerializedName("front_default") val frontImage: String?,
    @Expose @SerializedName("front_shiny") val frontShiny: String?

)

