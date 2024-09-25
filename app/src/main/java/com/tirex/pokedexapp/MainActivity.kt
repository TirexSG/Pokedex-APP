package com.tirex.pokedexapp

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.GradientDrawable
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tirex.pokedexapp.databinding.ActivityMainBinding
import com.tirex.pokedexapp.databinding.ItemTypePokemonBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var retrofit: Retrofit
    private lateinit var adapter: PokemonAdapter
    private lateinit var recyclerView: RecyclerView
    private var pokemonList: MutableList<PokemonResponseInfo> = mutableListOf()

    private val typeColors = mapOf(
        "steel" to R.color.steel_type,
        "water" to R.color.water_type,
        "bug" to R.color.bug_type,
        "dragon" to R.color.dragon_type,
        "electric" to R.color.electric_type,
        "ghost" to R.color.ghost_type,
        "fire" to R.color.fire_type,
        "fairy" to R.color.fairy_type,
        "ice" to R.color.ice_type,
        "fighting" to R.color.fighting_type,
        "normal" to R.color.normal_type,
        "grass" to R.color.grass_type,
        "psychic" to R.color.psychic_type,
        "rock" to R.color.rock_type,
        "dark" to R.color.dark_type,
        "ground" to R.color.ground_type,
        "poison" to R.color.poison_type,
        "flying" to R.color.flying_type
    )


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.btnOrder.setOnClickListener { showTypeSelectionDialog() }
        recyclerView = binding.rvPokemonList
        retrofit = getRetrofit()
        initUI()
        lifecycleScope.launch {
            fetchPokemons()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.let { controller ->
                controller.hide(WindowInsets.Type.navigationBars())
                controller.systemBarsBehavior =
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    View.SYSTEM_UI_FLAG_IMMERSIVE or
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        }


        val searchEditText = binding.svPokemonSearch.findViewById<EditText>(
            androidx.appcompat.R.id.search_src_text
        )
        val searchIcon = binding.svPokemonSearch.findViewById<ImageView>(
            androidx.appcompat.R.id.search_mag_icon
        )
        val closeIcon = binding.svPokemonSearch.findViewById<ImageView>(
            androidx.appcompat.R.id.search_close_btn
        )

        val progressBar = binding.progressBarList
        progressBar.indeterminateTintList = ContextCompat.getColorStateList(this, R.color.black)

        closeIcon.setColorFilter(
            ContextCompat.getColor(this, R.color.black),
            PorterDuff.Mode.SRC_IN
        )
        searchIcon.setColorFilter(
            ContextCompat.getColor(this, R.color.black),
            PorterDuff.Mode.SRC_IN
        )


        searchEditText.setHintTextColor(ContextCompat.getColor(this, R.color.black))
        searchEditText.setTextColor(ContextCompat.getColor(this, R.color.black))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            searchEditText.textCursorDrawable = ContextCompat.getDrawable(this, R.drawable.cursor_black)
        } else {
            searchEditText.setTextAppearance(R.style.CustomEditText)
        }

        if (isInternetAvailable(this)) {
            Log.d("Internet", "El usuario tiene internet")
        } else {
            showCustomToast()
        }


    }


        private fun initUI() {
        binding.svPokemonSearch.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(binding.root.windowToken, 0)
                Log.d("Busqueda:", query)
                if (query.isEmpty()) {
                    lifecycleScope.launch {

                    }
                } else {
                    handleSearch(query)
                }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText.isNullOrEmpty()) {
                    lifecycleScope.launch {
                        fetchPokemons()
                    }
                } else {
                    handleSearch(newText)
                }
                return true
            }
        })

        adapter = PokemonAdapter { id -> navigateToDetail(id) }
        binding.rvPokemonList.setHasFixedSize(true)
        binding.rvPokemonList.layoutManager = GridLayoutManager(this, 2)
        binding.rvPokemonList.adapter = adapter
    }

    private fun handleSearch(query: String) {
        val isNumber = query.toIntOrNull() != null

        if (isNumber) {
            val id = query.toInt()
            searchById(id)
        } else {
            searchByName(query.lowercase(Locale.ROOT))
        }
    }

    private fun getRetrofit(): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://pokeapi.co/api/v2/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }


    private suspend fun fetchPokemons(limit: Int = 200, offset: Int = 0): List<PokemonResponseInfo> {
        binding.progressBarList.isVisible = true
        binding.rvPokemonList.isVisible = false
        binding.svPokemonSearch.isVisible = false
        binding.btnOrder.isVisible = false

        return withContext(Dispatchers.IO) {
            try {
                val response = retrofit.create(APIService::class.java).getPokemonList(limit, offset)
                if (response.isSuccessful) {
                    val fetchedList: List<PokemonResponseInfo> = response.body()?.results ?: emptyList()
                    withContext(Dispatchers.Main) {
                        if (offset == 0) {
                            pokemonList.clear() // Limpia la lista si es la primera carga
                        }
                        pokemonList.addAll(fetchedList)
                        adapter.updateClearList(pokemonList)
                        binding.rvPokemonList.scrollToPosition(0)

                        delay(1000)
                        binding.progressBarList.isVisible = false
                        binding.rvPokemonList.isVisible = true
                        binding.svPokemonSearch.isVisible = true
                        binding.btnOrder.isVisible = true
                    }
                    fetchedList
                } else {
                    Log.e("fetchPokemons", "Error en la respuesta: ${response.code()}")
                    emptyList()
                }
            } catch (e: Exception) {
                Log.e("fetchPokemons", "Error al obtener los Pokémon", e)
                withContext(Dispatchers.Main) {
                    binding.progressBarList.isVisible = false
                    binding.rvPokemonList.isVisible = true
                    binding.svPokemonSearch.isVisible = true
                    binding.btnOrder.isVisible = true
                }
                emptyList()
            }
        }

    }




    private fun searchById(pokemonId: Int) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response: Response<PokemonUrl> = retrofit.create(APIService::class.java).getPokemonById(pokemonId)
                if (response.isSuccessful) {
                    val pokemon: PokemonUrl? = response.body()
                    Log.d("URL POKEMON", "$pokemon")
                    withContext(Dispatchers.Main) {
                        if (pokemon != null) {
                            val pokemonUrl = "https://pokeapi.co/api/v2/pokemon/${pokemon.id}/"
                            val pokemonInfo = PokemonResponseInfo(
                                name = pokemon.name ?: "Nombre no disponible",
                                url = pokemonUrl
                            )
                            val newPokemonList: MutableList<PokemonResponseInfo> = mutableListOf(pokemonInfo)
                            adapter.updateClearList(newPokemonList)

                            // Opcional: Manejo del ViewHolder, si es necesario.
                            val position = adapter.getPositionForPokemon(pokemon.id)
                            val viewHolder = recyclerView.findViewHolderForAdapterPosition(position) as? PokemonViewHolder
                            viewHolder?.updatePokemonDetail(pokemon)
                        } else {
                            // Manejo de caso donde no se encuentra el Pokémon
                            adapter.updateList(mutableListOf())
                        }
                    }
                } else {
                    Log.d("BuscarId Error:", "Error")
                    withContext(Dispatchers.Main) {
                        adapter.updateList(mutableListOf()) // Vacía la lista en caso de error
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    adapter.updateList(mutableListOf()) // Vacía la lista en caso de excepción
                }
                Log.e("searchById", "Error al buscar Pokémon por ID", e)
            }
        }
    }





    private fun searchByName(pokemonName: String?) {
        if (pokemonName.isNullOrBlank()) {
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response: Response<PokemonUrl> = retrofit.create(APIService::class.java).getPokemonByName(pokemonName)
                if (response.isSuccessful) {
                    val pokemon: PokemonUrl? = response.body()
                    withContext(Dispatchers.Main) {
                        if (pokemon != null) {
                            val pokemonUrl = "https://pokeapi.co/api/v2/pokemon/${pokemon.name}/"
                            val pokemonInfo = PokemonResponseInfo(
                                name = pokemon.name ?: "Nombre no disponible",
                                url = pokemonUrl
                            )
                            // Crear una lista mutable con el Pokémon encontrado
                            val newPokemonList: MutableList<PokemonResponseInfo> = mutableListOf(pokemonInfo)
                            // Actualizar el adaptador con la lista vacía y luego agregar el nuevo Pokémon
                            adapter.updateClearList(newPokemonList)
                        } else {
                            // Maneja el caso en que el Pokémon no existe aquí (si es necesario)
                            // Por ejemplo, puedes mostrar un mensaje indicando que no se encontró el Pokémon
                            Toast.makeText(this@MainActivity, "Pokémon no encontrado", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    // Maneja el caso en que la respuesta no es exitosa aquí (si es necesario)
                    Log.e("searchByName", "Error en la respuesta: ${response.code()}")
                }
            } catch (e: Exception) {
                // Maneja excepciones aquí (si es necesario)
                Log.e("searchByName", "Error durante la búsqueda de Pokémon por nombre", e)
            }
        }
    }





    private fun navigateToDetail(id: String) {
        val intent = Intent(this, PokemonDetailsActivity::class.java)
        val bundle = Bundle()

        bundle.putString(PokemonDetailsActivity.EXTRA_ID, id)
        intent.putExtras(bundle)

        startActivity(intent)
    }





    private fun showTypeSelectionDialog() {
        val binding = ItemTypePokemonBinding.inflate(LayoutInflater.from(this))
        val typeLayouts = listOf(
            binding.llSteelType to "steel",
            binding.llWaterType to "water",
            binding.llBugType to "bug",
            binding.llDragonType to "dragon",
            binding.llElectricType to "electric",
            binding.llGhostType to "ghost",
            binding.llFireType to "fire",
            binding.llFairyType to "fairy",
            binding.llIceType to "ice",
            binding.llFightingType to "fighting",
            binding.llNormalType to "normal",
            binding.llGrassType to "grass",
            binding.llPsychicType to "psychic",
            binding.llRockType to "rock",
            binding.llDarkType to "dark",
            binding.llGroundType to "ground",
            binding.llPoisonType to "poison",
            binding.llFlyingType to "flying"
        )

        val selectedTypes = mutableListOf<String>()

        val dialog = AlertDialog.Builder(this)
            .setView(binding.root)
            .create()
        dialog.show()

        binding.btnOrderDialog.setOnClickListener {
            when (selectedTypes.size) {
                0 -> {
                    Toast.makeText(this, "Selecciona al menos un tipo", Toast.LENGTH_SHORT).show()
                }
                1 -> {
                    orderPokemons(selectedTypes[0], null)
                    dialog.dismiss()
                }
                2 -> {
                    orderPokemons(selectedTypes[0], selectedTypes[1])
                    dialog.dismiss()
                }
                else -> {
                    Toast.makeText(this, "Selecciona un máximo de dos tipos", Toast.LENGTH_SHORT).show()
                }
            }
        }

        for ((layout, type) in typeLayouts) {
            // Establece el background con el drawable que tiene los bordes
            layout.setBackgroundResource(R.drawable.linear_layout_border)

            layout.setOnClickListener {
                if (selectedTypes.contains(type)) {
                    // Desmarcar el tipo si ya está seleccionado
                    selectedTypes.remove(type)
                    layout.setBackgroundResource(R.drawable.linear_layout_border) // Vuelve al borde
                } else {
                    // Si ya hay 2 tipos seleccionados, no permitir seleccionar un nuevo tipo
                    if (selectedTypes.size < 2) {
                        selectedTypes.add(type)

                        // Crear un nuevo GradientDrawable con el color del tipo
                        val drawable = GradientDrawable().apply {
                            shape = GradientDrawable.RECTANGLE
                            setColor(getColor(typeColors[type] ?: R.color.default_type)) // Color del tipo
                            setStroke(5, Color.BLACK) // Mantener el borde negro
                            cornerRadius = 5f // Radio de las esquinas
                        }
                        layout.background = drawable
                    } else {
                        Toast.makeText(this, "Solo puedes seleccionar 2 tipos", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }




    private fun orderPokemons(desiredType1: String, desiredType2: String?) {
        // Muestra el progress bar y oculta el RecyclerView antes de iniciar la tarea
        binding.progressBarList.isVisible = true
        binding.rvPokemonList.isVisible = false

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Llamadas a la API para obtener los Pokémon por ambos tipos
                val apiService = retrofit.create(APIService::class.java)
                val response1: Response<PokemonResponseType> = apiService.getPokemonByType(desiredType1)
                val response2: Response<PokemonResponseType>? = if (desiredType2 != null) {
                    apiService.getPokemonByType(desiredType2)
                } else {
                    null
                }

                if (response1.isSuccessful && (response2?.isSuccessful == true || desiredType2 == null)) {
                    val pokemonTypeResponse1 = response1.body()
                    val pokemonTypeResponse2 = response2?.body()

                    val fetchedList1 = pokemonTypeResponse1?.pokemonList?.map { it.pokemon } ?: emptyList()
                    val fetchedList2 = pokemonTypeResponse2?.pokemonList?.map { it.pokemon } ?: emptyList()

                    // Si se especifica un segundo tipo, filtrar para obtener Pokémon que tienen ambos tipos
                    val combinedList = if (desiredType2 != null) {
                        fetchedList1.filter { pokemon1 ->
                            fetchedList2.any { pokemon2 -> pokemon2.name == pokemon1.name }
                        }
                    } else {
                        fetchedList1
                    }

                    withContext(Dispatchers.Main) {
                        // Ordenar por ID y limitar la cantidad de resultados
                        val sortedPokemonList = combinedList.sortedBy { extractIdFromUrl(it.url) }
                        val limitedPokemonResponseInfos = sortedPokemonList.take(200).toMutableList()

                        // Limpia el adaptador antes de actualizar la lista
                        adapter.updateClearList(limitedPokemonResponseInfos)

                        binding.rvPokemonList.scrollToPosition(0)

                        // Oculta el progress bar y muestra el RecyclerView después de actualizar la lista
                        delay(1000)  // Mantener el retraso para mostrar el progress bar por un tiempo
                        binding.progressBarList.isVisible = false
                        binding.rvPokemonList.isVisible = true
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        // Oculta el progress bar y muestra el RecyclerView en caso de error
                        binding.progressBarList.isVisible = false
                        binding.rvPokemonList.isVisible = true
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    // Oculta el progress bar y muestra el RecyclerView en caso de excepción
                    binding.progressBarList.isVisible = false
                    binding.rvPokemonList.isVisible = true
                }
            }
        }
    }

    private fun isInternetAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val network = connectivityManager.activeNetwork ?: return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
        return when {
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
            else -> false
        }
    }

    private fun showCustomToast() {
        val inflater = layoutInflater
        // Inflar el layout del custom toast
        val layout = inflater.inflate(R.layout.custom_toast, null)

        // Crear y configurar el Toast
        val toast = Toast(applicationContext)
        toast.duration = Toast.LENGTH_LONG
        toast.view = layout // Asignar la vista inflada al Toast
        toast.show()
    }



    // Función auxiliar para extraer el ID de la URL
    private fun extractIdFromUrl(url: String): Int {
        return url.trimEnd('/').substringAfterLast('/').toIntOrNull() ?: 0
    }




}
