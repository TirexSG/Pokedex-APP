package com.tirex.pokedexapp

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.graphics.Typeface
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.TypedValue
import android.view.GestureDetector
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowInsets
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import com.google.gson.Gson
import com.squareup.picasso.Picasso
import com.tirex.pokedexapp.databinding.ActivityPokemonDetailsBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.HttpException
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import uk.co.samuelwall.materialtaptargetprompt.MaterialTapTargetPrompt
import uk.co.samuelwall.materialtaptargetprompt.extras.focals.RectanglePromptFocal
import java.io.IOException
import kotlin.math.roundToInt

class PokemonDetailsActivity : AppCompatActivity(), GestureDetector.OnGestureListener {

    companion object {
        const val EXTRA_ID = "extra_id"
        const val MIN_DISTANCE = 100

    }

    private lateinit var binding: ActivityPokemonDetailsBinding
    private var mediaPlayer: MediaPlayer? = null
    private lateinit var gestureDetector: GestureDetector
    private var isShiny: Boolean = false
    private var pokemonImage: String? = null
    private var pokemonImageShiny: String? = null
    private var downX = 0f
    private var downY = 0f


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPokemonDetailsBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)

        mediaPlayer = MediaPlayer.create(this, R.raw.shiny_sound_effect)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val id: String = intent.getStringExtra(EXTRA_ID) ?: ""

        if (id.isNotEmpty()) {
            getPokemonInformation(id)
        } else {
            Log.e("ID", "ID recibido es inválido")
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.hide(WindowInsets.Type.statusBars())
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    View.SYSTEM_UI_FLAG_IMMERSIVE or
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        }

        val progressBar = binding.progressBarDetails
        progressBar.indeterminateTintList = ContextCompat.getColorStateList(this, R.color.black)


        gestureDetector = GestureDetector(this, this)

        setupTouchListeners()

        startEvolutionAnimation(binding.ivPokemonEvolution1)
        startEvolutionAnimation(binding.ivPokemonEvolution2)
        startEvolutionAnimation(binding.ivPokemonEvolution3)
        startImageAnimation(binding.ivPokemonDetail)


    }


    override fun onDown(p0: MotionEvent): Boolean {
        //TODO("Not yet implemented")
        return false
    }

    override fun onShowPress(p0: MotionEvent) {
        // TODO("Not yet implemented")
    }

    override fun onSingleTapUp(p0: MotionEvent): Boolean {
        //TODO("Not yet implemented")
        return false
    }

    override fun onScroll(p0: MotionEvent?, p1: MotionEvent, p2: Float, p3: Float): Boolean {
        //TODO("Not yet implemented")
        return false
    }

    override fun onLongPress(p0: MotionEvent) {
        //TODO("Not yet implemented")
    }

    override fun onFling(
        e1: MotionEvent?,
        e2: MotionEvent,
        velocityX: Float,
        velocityY: Float
    ): Boolean {
        val xDiff = e2.x - e1!!.x
        val yDiff = e2.y - e1.y

        if (Math.abs(xDiff) > Math.abs(yDiff)) {
            if (Math.abs(xDiff) > MIN_DISTANCE && Math.abs(velocityX) > MIN_DISTANCE) {
                if (xDiff > 0) {
                    onCardSwipeRight()
                } else {
                    onCardSwipeLeft()
                }
                return true
            }
        }
        return false
    }

    private fun startImageAnimation(imageView: ImageView) {
        // Animaciones de escalado para X e Y
        val scaleDown = ObjectAnimator.ofFloat(imageView, "scaleX", 0.99f).also {
            it.duration = 1500
            it.repeatCount = ObjectAnimator.INFINITE
            it.repeatMode = ObjectAnimator.REVERSE
        }

        val scaleDownY = ObjectAnimator.ofFloat(imageView, "scaleY", 0.99f).also {
            it.duration = 1500
            it.repeatCount = ObjectAnimator.INFINITE
            it.repeatMode = ObjectAnimator.REVERSE
        }

        val animatorSet = AnimatorSet()
        animatorSet.play(scaleDown).with(scaleDownY)
        animatorSet.start()
    }

    private fun startEvolutionAnimation(imageView: ImageView) {
        // Animaciones de escalado para X e Y
        val scaleDown = ObjectAnimator.ofFloat(imageView, "scaleX", 0.90f).also {
            it.duration = 1500
            it.repeatCount = ObjectAnimator.INFINITE
            it.repeatMode = ObjectAnimator.REVERSE
        }

        val scaleDownY = ObjectAnimator.ofFloat(imageView, "scaleY", 0.90f).also {
            it.duration = 1500
            it.repeatCount = ObjectAnimator.INFINITE
            it.repeatMode = ObjectAnimator.REVERSE
        }

        val animatorSet = AnimatorSet()
        animatorSet.play(scaleDown).with(scaleDownY)
        animatorSet.start()
    }

    private fun imageAnimation(view: View) {
        val scaleUpX = ObjectAnimator.ofFloat(view, "scaleX", 1.1f).apply {
            duration = 500
        }

        val scaleUpY = ObjectAnimator.ofFloat(view, "scaleY", 1.1f).apply {
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



    private fun setupTouchListeners() {
        binding.cvPokemonDetail.setOnTouchListener { v, event ->
            handleSwipeGesture(event, ::onCardSwipeRight, ::onCardSwipeLeft)
        }

    }

    private fun handleSwipeGesture(
        event: MotionEvent,
        onSwipeRight: (() -> Unit)?,
        onSwipeLeft: (() -> Unit)?
    ): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x
                downY = event.y
                return true
            }

            MotionEvent.ACTION_UP -> {
                val upX = event.x
                val upY = event.y

                val deltaX = upX - downX
                val deltaY = upY - downY

                // Verificar si el deslizamiento es horizontal y suficientemente largo
                if (Math.abs(deltaX) > MIN_DISTANCE && Math.abs(deltaX) > Math.abs(deltaY)) {
                    if (deltaX > 0) {
                        onSwipeRight?.invoke()  // Deslizar a la derecha
                    } else {
                        onSwipeLeft?.invoke()  // Deslizar a la izquierda
                    }
                    return true
                }
                return false
            }

            else -> return false
        }
    }

    private fun showTutorialPrompts() {

        // Mostrar el primer MaterialTapTargetPrompt
        MaterialTapTargetPrompt.Builder(this)
            .setTarget(binding.cvPokemonDetail)
            .setPrimaryText("Sonido del Pokémon")
            .setSecondaryText("Pulsa sobre la imagen del Pokemon para volver a escuchar el sonido")
            .setBackgroundColour(getResources().getColor(R.color.white))
            .setPrimaryTextColour(getResources().getColor(R.color.black))
            .setSecondaryTextColour(getResources().getColor(R.color.black))
            .setTextGravity(Gravity.CENTER_HORIZONTAL)
            .setPrimaryTextTypeface(Typeface.DEFAULT_BOLD)
            .setBackButtonDismissEnabled(true)
            .setFocalRadius(100f)
            .setFocalPadding(20f)
            .setPromptFocal(RectanglePromptFocal())
            .setPromptStateChangeListener { prompt, state ->
                if (state == MaterialTapTargetPrompt.STATE_DISMISSED || state == MaterialTapTargetPrompt.STATE_FOCAL_PRESSED) {

                    showSecondPrompt()

                }
            }
            .show()
    }


    private fun showSecondPrompt() {

        MaterialTapTargetPrompt.Builder(this)
            .setTarget(binding.cvPokemonDetail)
            .setPrimaryText("Imagen del Pokémon")
            .setSecondaryText("Desliza sobre la imagen del Pokemon para ver su versión shiny")
            .setBackgroundColour(getResources().getColor(R.color.white))
            .setPrimaryTextColour(getResources().getColor(R.color.black))
            .setSecondaryTextColour(getResources().getColor(R.color.black))
            .setTextGravity(Gravity.CENTER_HORIZONTAL)
            .setBackButtonDismissEnabled(true)
            .setPrimaryTextTypeface(Typeface.DEFAULT_BOLD)
            .setFocalRadius(100f)
            .setFocalPadding(20f)
            .setPromptFocal(RectanglePromptFocal())
            .setPromptStateChangeListener { prompt, state ->
                if (state == MaterialTapTargetPrompt.STATE_DISMISSED || state == MaterialTapTargetPrompt.STATE_FOCAL_PRESSED) {
                    val sharedPreferences = getSharedPreferences("PokedexPrefs", MODE_PRIVATE)
                    val editor = sharedPreferences.edit()
                    editor.putBoolean("isFirstPokemon", false)
                    editor.apply()


                    Handler(Looper.getMainLooper()).postDelayed({
                        showSecondAdviceDialog()
                    }, 2000)

                }
            }
            .show()
    }


    private suspend fun getEvolutions(pokemonId: Int): EvolutionChain? {
        val apiService = getRetrofit().create(APIService::class.java)

        return withContext(Dispatchers.IO) {
            try {
                // Obtener la información del Pokémon
                val pokemonSpeciesResponse = apiService.getPokemonSpecies(pokemonId)
                Log.d("getEvolutions", "$pokemonSpeciesResponse")
                if (!pokemonSpeciesResponse.isSuccessful) {
                    Log.e(
                        "getEvolutions",
                        "Error en la solicitud de Pokémon Species: ${pokemonSpeciesResponse.message()}"
                    )
                    return@withContext null
                }

                // Obtener la URL de la cadena de evolución
                val evolutionChainUrl = pokemonSpeciesResponse.body()?.evolution_chain?.url
                Log.d("getEvolutions", "evolutionChainUrl: $evolutionChainUrl")

                if (evolutionChainUrl.isNullOrEmpty()) {
                    Log.e("getEvolutions", "URL de la cadena de evolución es nula o vacía")
                    return@withContext null
                }

                // Extraer el ID de la cadena de evolución de la URL
                val evolutionChainId =
                    evolutionChainUrl.split("/").filter { it.isNotEmpty() }.lastOrNull()
                        ?.toIntOrNull()
                if (evolutionChainId == null) {
                    Log.e(
                        "getEvolutions",
                        "ID de la cadena de evolución no válido: $evolutionChainUrl"
                    )
                    return@withContext null
                }
                Log.d("getEvolutions", "evolutionChainId: $evolutionChainId")

                // Obtener la información de la cadena de evolución
                val evolutionChainResponse = apiService.getEvolutionChain(evolutionChainId)
                Log.d("getEvolutions", "evolutionChainResponse: $evolutionChainResponse")
                if (!evolutionChainResponse.isSuccessful) {
                    Log.e(
                        "getEvolutions",
                        "Error en la solicitud de Evolution Chain: ${evolutionChainResponse.message()}"
                    )
                    return@withContext null
                }

                evolutionChainResponse.body()?.chain
            } catch (e: HttpException) {
                Log.e("getEvolutions", "Error en la solicitud HTTP: ${e.message()}")
                null
            } catch (e: IOException) {
                Log.e("getEvolutions", "Error en la conexión: ${e.message}")
                null
            } catch (e: Exception) {
                Log.e("getEvolutions", "Error al obtener la evolución: ${e.message}")
                null
            }
        }
    }


    private fun extractPokemonImageUrlsAndIds(chain: EvolutionChain): Pair<List<String>, List<Int>> {
        val imageUrls = mutableListOf<String>()
        val pokemonIds = mutableListOf<Int>()

        fun traverseChain(chain: EvolutionChain) {
            val speciesUrl = chain.species.url
            if (speciesUrl.isNotBlank()) {
                val speciesId =
                    speciesUrl.split("/").filter { it.isNotEmpty() }.lastOrNull()?.toIntOrNull()
                if (speciesId != null) {
                    // Construye la URL para obtener la imagen del Pokémon
                    val imageUrl =
                        "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/home/$speciesId.png"
                    imageUrls.add(imageUrl)
                    pokemonIds.add(speciesId) // Añadir el ID del Pokémon como entero
                } else {
                    Log.e("extractPokemonImageUrlsAndIds", "ID de especie no válido: $speciesUrl")
                }
            } else {
                Log.e("extractPokemonImageUrlsAndIds", "URL de especie nula")
            }

            // Recorre las evoluciones siguientes
            for (evolveTo in chain.evolves_to) {
                traverseChain(evolveTo)
            }
        }

        traverseChain(chain)
        return Pair(imageUrls, pokemonIds) // Retorna las URLs de imágenes y los IDs de los Pokémon
    }


    private fun loadEvolutionImages(imageUrls: List<String>, pokemonIds: List<Int>) {
        Log.d(
            "loadEvolutionImages",
            "Cargando imágenes de evolución: $imageUrls, Pokemon IDs: $pokemonIds"
        )

        // Ocultar el CardView padre por defecto
        binding.cvPokemonEvolutions.isVisible = false

        fun loadEvolutionCard(
            index: Int,
            imageUrl: String,
            pokemonId: Int,
            cardView: View,
            imageView: ImageView
        ) {
            Log.d("loadEvolutionCard", "Cargando imagen: $imageUrl para Pokémon ID: $pokemonId")
            if (imageUrl.isNotEmpty()) {
                cardView.isVisible = true
                Picasso.get()
                    .load(imageUrl)
                    .placeholder(R.drawable.placeholder)
                    .into(imageView)

                cardView.setOnClickListener {
                    Log.d("loadEvolutionCard", "Clic en el CardView para ID: $pokemonId")
                    navigateToDetail(pokemonId.toString())
                }
            } else {
                Log.d("loadEvolutionCard", "Imagen vacía para ID: $pokemonId")
            }
        }

        // Cargar las imágenes de evolución
        val validEvolutions = imageUrls.indices.filter { index ->
            index < pokemonIds.size && imageUrls[index].isNotEmpty()
        }

        // Limpiar la visibilidad de todos los CardView antes de cargar
        binding.cvPokemonEvolution1.isVisible = false
        binding.cvPokemonEvolution2.isVisible = false
        binding.cvPokemonEvolution3.isVisible = false

        // Cargar solo las evoluciones válidas si hay más de una
        if (validEvolutions.size > 1) {
            validEvolutions.forEach { index ->
                loadEvolutionCard(
                    index, imageUrls[index], pokemonIds[index], when (index) {
                        0 -> binding.cvPokemonEvolution1
                        1 -> binding.cvPokemonEvolution2
                        2 -> binding.cvPokemonEvolution3
                        else -> throw IndexOutOfBoundsException("Index out of bounds for evolutions")
                    }, when (index) {
                        0 -> binding.ivPokemonEvolution1
                        1 -> binding.ivPokemonEvolution2
                        2 -> binding.ivPokemonEvolution3
                        else -> throw IndexOutOfBoundsException("Index out of bounds for evolutions")
                    }
                )
            }

            // Determinar si se debe mostrar el CardView padre
            binding.cvPokemonEvolutions.isVisible = true
        }
    }


    private fun showSecondAdviceDialog() {
        val inflater = LayoutInflater.from(this)
        val dialogView = inflater.inflate(R.layout.advice_seconddialog, null)


        val builder = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)

        val dialog = builder.create()

        dialogView.setOnClickListener{
            dialog.dismiss()
        }

        dialog.show()

        val window = dialog.window
        val layoutParams = window?.attributes
        layoutParams?.gravity = Gravity.BOTTOM
        layoutParams?.x = 0
        layoutParams?.y = 150

        window?.attributes = layoutParams
    }


    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null

    }


    private fun onCardSwipeRight() {
        Log.d("Swipe", "Deslizado hacia la derecha")

        if (isShiny) {
            // Mostrar imagen normal y ocultar animación
            binding.shinyAnimation.visibility = View.GONE
            pokemonImage?.let {
                Picasso.get()
                    .load(it)
                    .placeholder(R.drawable.placeholder)
                    .into(binding.ivPokemonDetail)
            }
            isShiny = false
        } else {
            // Primero, ocultar la animación y cargar la imagen shiny
            binding.shinyAnimation.visibility = View.GONE
            pokemonImageShiny?.let {
                Picasso.get()
                    .load(it)
                    .placeholder(R.drawable.placeholder)
                    .into(binding.ivPokemonDetail, object : com.squareup.picasso.Callback {
                        override fun onSuccess() {
                            // Mostrar la animación después de que la imagen se ha cargado
                            binding.shinyAnimation.visibility = View.VISIBLE
                            binding.shinyAnimation.playAnimation()
                            imageAnimation(binding.ivPokemonDetail)
                            playShinySound()
                            isShiny = true
                        }

                        override fun onError(e: Exception?) {
                            // Detener animación si hay error al cargar la imagen
                            binding.shinyAnimation.visibility = View.GONE
                            binding.shinyAnimation.cancelAnimation()
                        }
                    })
            }
        }
    }

    private fun onCardSwipeLeft() {
        Log.d("Swipe", "Deslizado hacia la izquierda")

        if (isShiny) {
            // Mostrar imagen normal y ocultar animación
            binding.shinyAnimation.visibility = View.GONE
            pokemonImage?.let {
                Picasso.get()
                    .load(it)
                    .placeholder(R.drawable.placeholder)
                    .into(binding.ivPokemonDetail)
            }
            isShiny = false
        } else {
            // Primero, ocultar la animación y cargar la imagen shiny
            binding.shinyAnimation.visibility = View.GONE
            pokemonImageShiny?.let {
                Picasso.get()
                    .load(it)
                    .placeholder(R.drawable.placeholder)
                    .into(binding.ivPokemonDetail, object : com.squareup.picasso.Callback {
                        override fun onSuccess() {
                            // Mostrar la animación después de que la imagen se ha cargado
                            binding.shinyAnimation.visibility = View.VISIBLE
                            binding.shinyAnimation.playAnimation()
                            imageAnimation(binding.ivPokemonDetail)
                            playShinySound()
                            isShiny = true
                        }

                        override fun onError(e: Exception?) {
                            // Detener animación si hay error al cargar la imagen
                            binding.shinyAnimation.visibility = View.GONE
                            binding.shinyAnimation.cancelAnimation()
                        }
                    })
            }
        }
    }


    private fun playShinySound() {
        // Detener el sonido actual si está reproduciéndose
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.stop()
            }
            it.release()
        }
        // Crear una nueva instancia del MediaPlayer con el nuevo sonido
        mediaPlayer = MediaPlayer.create(this@PokemonDetailsActivity, R.raw.shiny_sound_effect)
        mediaPlayer?.start()

        // Configurar el listener para liberar el MediaPlayer cuando el sonido termine
        mediaPlayer?.setOnCompletionListener {
            it.release()
            mediaPlayer = null
        }
    }


    private suspend fun createUI(pokemonDetail: PokemonUrl) {

        CoroutineScope(Dispatchers.Main).launch {
            val evolutionChain = getEvolutions(pokemonDetail.id)
            if (evolutionChain != null) {
                val (imageUrls, pokemonIds) = extractPokemonImageUrlsAndIds(evolutionChain)
                loadEvolutionImages(imageUrls, pokemonIds)
                Log.d("getEvolutions", "Image URLs: $imageUrls")
            } else {
                Log.e("createUI", "No se pudo obtener la cadena de evolución")
            }
        }

        binding.apply {
            ivPokemonDetail.isVisible = false
            progressBarDetails.isVisible = true
            viewMain.isVisible = true
            pokemonInfo.isVisible = false
            cvPokemonDescription.isVisible = false
            cvPokemonDetail.isVisible = false
            cvPokemonNameDetail.isVisible = false
            llHeightWeight.isVisible = false
            cvPokemonFirstType.isVisible = false
            cvPokemonStats.isVisible = false
            svPokemonStats.isVisible = false
            cvPokemonStatsInterior.isVisible = false


            val pokemonName = pokemonDetail.name
            pokemonImage = pokemonDetail.sprites.others?.image?.frontImage
            pokemonImageShiny = pokemonDetail.sprites.others?.image?.frontShiny
            val pokemonId = pokemonDetail.id
            val pokemonHeight = pokemonDetail.height
            val pokemonWeight = pokemonDetail.weight
            val pokemonFixedHeight = String.format("%.1f", pokemonHeight * 0.1) + " m"
            val pokemonFixedWeight = String.format("%.1f", pokemonWeight * 0.1) + " kg"
            val pokemonSound = pokemonDetail.cries["latest"]
            val pokemonStats = pokemonDetail.stats
            val pokemonDescription = fetchSpriteUrl(pokemonDetail.species.url)
            val pokemonType = pokemonDetail.types[0].type?.typeName
            val pokemonSecondType = if (pokemonDetail.types.size > 1) {
                pokemonDetail.types[1].type?.typeName
            } else {
                null
            }


            if (pokemonDescription != null) {
                val entries = pokemonDescription.entries
                val desiredLanguage = "es"
                val filteredEntry = entries.firstOrNull { it.language.name == desiredLanguage }

                val flavorText = filteredEntry?.flavorText?.replace("\n", " ")
                val flavorTextFiltered = flavorText?.replace(".", ". \n")
                tvPokemonDescription.text =
                    flavorTextFiltered ?: "No se ha encontrado una descripción de este Pokemon"
            }


            tvPokemonStat1.text = pokemonStats[0].statNumber.toString()
            tvPokemonStat2.text = pokemonStats[1].statNumber.toString()
            tvPokemonStat3.text = pokemonStats[2].statNumber.toString()
            tvPokemonStat4.text = pokemonStats[3].statNumber.toString()
            tvPokemonStat5.text = pokemonStats[4].statNumber.toString()
            tvPokemonStat6.text = pokemonStats[5].statNumber.toString()


            val stats = pokemonDetail.stats
            prepareStats(stats)
            tvPokemonNameDetail.text = pokemonName
            tvPokemonIdDetail.text = pokemonId.toString()
            tvPokemonHeight.text = pokemonFixedHeight
            tvPokemonWeight.text = pokemonFixedWeight
            tvPokemonFirstType.text = translateType(pokemonType)
            if (pokemonSecondType != null) {
                tvPokemonSecondType.text = translateType(pokemonSecondType)
                cvPokemonSecondType.isVisible = true
            }
            if (pokemonImage != null) {
                Picasso.get()
                    .load(pokemonImage)
                    .placeholder(R.drawable.placeholder)
                    .into(ivPokemonDetail)
            } else {
                ivPokemonDetail.setImageResource(R.drawable.placeholder)
            }
            pokemonTypeColor(pokemonDetail)


            val sharedPreferences = getSharedPreferences("PokedexPrefs", MODE_PRIVATE)
            val isFirstPokemon = sharedPreferences.getBoolean("isFirstPokemon", true)


            if (pokemonSound != null) {
                delay(200)
                playPokemonSound(pokemonSound)
                cvPokemonDetail.setOnClickListener {
                    playPokemonSound(pokemonSound)
                    imageAnimation(binding.ivPokemonDetail)

                }
            } else {
                Log.e("PokemonSound", "No se encontró la URL del sonido")
            }

            binding.cvPokemonDetail.setOnTouchListener { v, event ->
                gestureDetector.onTouchEvent(event)
            }

            progressBarDetails.isVisible = false
            pokemonInfo.isVisible = true
            cvPokemonDescription.isVisible = true
            cvPokemonDetail.isVisible = true
            ivPokemonDetail.isVisible = true
            cvPokemonNameDetail.isVisible = true
            llHeightWeight.isVisible = true
            cvPokemonFirstType.isVisible = true
            cvPokemonStats.isVisible = true
            svPokemonStats.isVisible = true
            cvPokemonStatsInterior.isVisible = true

            if (isFirstPokemon) {
                showTutorialPrompts()
            } else {
                Log.d("Tutorial", "No se muestra tutorial")
            }


        }

    }


    private fun translateType(type: String?): String {
        val translation = when (type) {
            "steel" -> "Acero"
            "water" -> "Agua"
            "bug" -> "Insecto"
            "dragon" -> "Dragón"
            "electric" -> "Eléctrico"
            "ghost" -> "Fantasma"
            "fire" -> "Fuego"
            "fairy" -> "Hada"
            "ice" -> "Hielo"
            "fighting" -> "Luchador"
            "normal" -> "Normal"
            "grass" -> "Planta"
            "psychic" -> "Psíquico"
            "rock" -> "Roca"
            "dark" -> "Siniestro"
            "ground" -> "Tierra"
            "poison" -> "Veneno"
            "flying" -> "Volador"
            "default" -> "Default"
            else -> {
                "Null"
            }
        }
        return translation
    }

    suspend fun fetchSpriteUrl(url: String): DescriptionUrl? = withContext(Dispatchers.IO) {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url(url)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IOException("Unexpected code $response")

                val jsonResponse = response.body?.string()

                val gson = Gson()
                gson.fromJson(jsonResponse, DescriptionUrl::class.java)
            }
        } catch (e: IOException) {
            Log.e("fetchSpriteUrl", "Error fetching sprite URL", e)
            null
        }
    }


    private fun getPokemonInformation(id: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val response: Response<PokemonUrl> =
                getRetrofit().create(APIService::class.java).getPokemonByName(id)


            if (response.isSuccessful) {
                val pokemonDetail = response.body()
                withContext(Dispatchers.Main) {
                    if (pokemonDetail != null) {
                        createUI(pokemonDetail)
                    } else {
                        Log.i("tirex", "Failed to fetch details")
                    }
                }
            }


        }
    }

    private fun playPokemonSound(soundUrl: String) {
        try {
            // Libera el anterior MediaPlayer si existe
            mediaPlayer?.release()

            // Crea una nueva instancia de MediaPlayer
            mediaPlayer = MediaPlayer().apply {
                // Configura los atributos de audio
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                // Establece la fuente de datos del sonido
                setDataSource(soundUrl)
                // Configura el listener para cuando el MediaPlayer esté preparado
                setOnPreparedListener {
                    it.start() // Reproduce el sonido
                }
                // Configura el listener para manejar errores
                setOnErrorListener { _, what, extra ->
                    Log.e("MediaPlayerError", "Error de reproducción: $what, $extra")
                    true // Indica que el error ha sido manejado
                }
                // Configura el listener para cuando el sonido se haya completado
                setOnCompletionListener {
                    it.release() // Libera el MediaPlayer después de la reproducción
                    mediaPlayer = null
                }
                // Prepara el MediaPlayer de forma asíncrona
                prepareAsync()
            }
        } catch (e: Exception) {
            Log.e("MediaPlayerError", "Excepción al configurar MediaPlayer", e)
            e.printStackTrace()
        }
    }


    private fun getRetrofit(): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://pokeapi.co/api/v2/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    private fun pokemonTypeColor(pokemonDetail: PokemonUrl) {
        val firstType = pokemonDetail.types.firstOrNull()?.type?.typeName ?: ""
        val secondType = pokemonDetail.types.getOrNull(1)?.type?.typeName ?: ""

        fun getColorForType(type: String): Int {
            return when (type) {
                "steel" -> ContextCompat.getColor(
                    binding.cvPokemonDetail.context,
                    R.color.steel_type
                )

                "water" -> ContextCompat.getColor(
                    binding.cvPokemonDetail.context,
                    R.color.water_type
                )

                "bug" -> ContextCompat.getColor(
                    binding.cvPokemonDetail.context,
                    R.color.bug_type
                )

                "dragon" -> ContextCompat.getColor(
                    binding.cvPokemonDetail.context,
                    R.color.dragon_type
                )

                "electric" -> ContextCompat.getColor(
                    binding.cvPokemonDetail.context,
                    R.color.electric_type
                )

                "ghost" -> ContextCompat.getColor(
                    binding.cvPokemonDetail.context,
                    R.color.ghost_type
                )

                "fire" -> ContextCompat.getColor(
                    binding.cvPokemonDetail.context,
                    R.color.fire_type
                )

                "fairy" -> ContextCompat.getColor(
                    binding.cvPokemonDetail.context,
                    R.color.fairy_type
                )

                "ice" -> ContextCompat.getColor(
                    binding.cvPokemonDetail.context,
                    R.color.ice_type
                )

                "fighting" -> ContextCompat.getColor(
                    binding.cvPokemonDetail.context,
                    R.color.fighting_type
                )

                "normal" -> ContextCompat.getColor(
                    binding.cvPokemonDetail.context,
                    R.color.normal_type
                )

                "grass" -> ContextCompat.getColor(
                    binding.cvPokemonDetail.context,
                    R.color.grass_type
                )

                "psychic" -> ContextCompat.getColor(
                    binding.cvPokemonDetail.context,
                    R.color.psychic_type
                )

                "rock" -> ContextCompat.getColor(
                    binding.cvPokemonDetail.context,
                    R.color.rock_type
                )

                "dark" -> ContextCompat.getColor(
                    binding.cvPokemonDetail.context,
                    R.color.dark_type
                )

                "ground" -> ContextCompat.getColor(
                    binding.cvPokemonDetail.context,
                    R.color.ground_type
                )

                "poison" -> ContextCompat.getColor(
                    binding.cvPokemonDetail.context,
                    R.color.poison_type
                )

                "flying" -> ContextCompat.getColor(
                    binding.cvPokemonDetail.context,
                    R.color.flying_type
                )

                else -> ContextCompat.getColor(
                    binding.cvPokemonDetail.context,
                    R.color.default_type
                )
            }
        }

        val firstColor = getColorForType(firstType)
        val secondColor = getColorForType(secondType)


        binding.cvPokemonDetail.setCardBackgroundColor(firstColor)
        binding.cvPokemonNameDetail.setCardBackgroundColor(firstColor)
        binding.pokemonInfo.setBackgroundColor(firstColor)
        binding.cvPokemonDescription.setCardBackgroundColor(firstColor)
        binding.cvPokemonDescriptionInterior.setCardBackgroundColor(firstColor)
        binding.main.setBackgroundColor(firstColor)
        binding.cvPokemonHeight.setCardBackgroundColor(firstColor)
        binding.cvPokemonHeightInterior.setCardBackgroundColor(firstColor)
        binding.cvPokemonWeight.setCardBackgroundColor(firstColor)
        binding.cvPokemonWeightInterior.setCardBackgroundColor(firstColor)
        binding.cvPokemonFirstType.setBackgroundColor(firstColor)
        binding.cvPokemonSecondType.setBackgroundColor(secondColor)
        binding.cvPokemonStats.setBackgroundColor(firstColor)
        binding.cvPokemonStatsInterior.setBackgroundColor(firstColor)
        binding.cardView1.setCardBackgroundColor(firstColor)
        binding.cardView2.setCardBackgroundColor(firstColor)
        binding.cardView3.setCardBackgroundColor(firstColor)
        binding.cardView4.setCardBackgroundColor(firstColor)
        binding.cardView5.setCardBackgroundColor(firstColor)
        binding.cardView6.setCardBackgroundColor(firstColor)
        binding.cvPokemonEvolutions.setCardBackgroundColor(firstColor)


    }

    private fun prepareStats(stats: List<StatInfo>) {
        if (stats.size >= 6) { // Asegúrate de tener suficientes stats
            updateWidth(binding.view1, stats[0].statNumber.toString())
            updateWidth(binding.view2, stats[1].statNumber.toString())
            updateWidth(binding.view3, stats[2].statNumber.toString())
            updateWidth(binding.view4, stats[3].statNumber.toString())
            updateWidth(binding.view5, stats[4].statNumber.toString())
            updateWidth(binding.view6, stats[5].statNumber.toString())
        } else {
            Log.e("DetailSuperHeroActivity", "Número insuficiente de stats")
        }
    }

    private fun updateWidth(view: View, stat: String) {
        val widthInPx = try {
            if (stat.isEmpty() || stat.equals("null", ignoreCase = true)) {
                0f
            } else {
                stat.toFloat()
            }
        } catch (e: NumberFormatException) {
            Log.e("DetailSuperHeroActivity", "Error al convertir altura: ${e.message}")
            0f
        }

        val maxStat = 255
        val maxWidthPx = 150 // Ajustar este valor al ancho máximo deseado
        val scaleFactor = maxWidthPx / maxStat.toFloat()

        // Multiplicar el stat por el factor de escala para obtener el ancho correcto
        val adjustedWidthPx = widthInPx * scaleFactor

        // Convertir el valor a dp y aplicar al layout
        val params = view.layoutParams
        params.width = pxToDp(adjustedWidthPx)
        view.layoutParams = params
    }


    private fun pxToDp(px: Float): Int {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, px, resources.displayMetrics)
            .roundToInt()
    }

    private fun navigateToDetail(id: String) {
        val intent = Intent(this, PokemonDetailsActivity::class.java)
        val bundle = Bundle()
        bundle.putString(PokemonDetailsActivity.EXTRA_ID, id)
        intent.putExtras(bundle)
        startActivity(intent)
    }


}

