package com.tirex.pokedexapp

import retrofit2.Call
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface APIService {
    @GET("pokemon/{id}")
    suspend fun getPokemonById(@Path("id") pokemonId: Int?): Response<PokemonUrl>

    @GET("pokemon?limit=100000&offset=0")
    suspend fun getPokemonList(@Query("limit") limit: Int, @Query("offset") offset: Int): Response<PokemonResponse>

    @GET("pokemon/{name}")
    suspend fun getPokemonByName(@Path("name") pokemonName: String?): Response<PokemonUrl>

    @GET("type/{type}")
    suspend fun getPokemonByType(@Path("type") pokemonType: String?): Response<PokemonResponseType>


}


