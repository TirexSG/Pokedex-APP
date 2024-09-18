package com.tirex.pokedexapp

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Typeface
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.GestureDetector
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import androidx.core.view.WindowInsetsControllerCompat
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
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
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.create
import uk.co.samuelwall.materialtaptargetprompt.MaterialTapTargetPrompt
import uk.co.samuelwall.materialtaptargetprompt.extras.focals.RectanglePromptFocal
import java.io.IOException
import java.util.Locale

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
            window.insetsController?.let { controller ->
                controller.hide(WindowInsets.Type.statusBars())
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


        gestureDetector = GestureDetector(this, this)


        val sharedPreferences = getSharedPreferences("PokedexPrefs", MODE_PRIVATE)
        val showTutorial = intent.getBooleanExtra("SHOW_TUTORIAL", false)

        if (showTutorial) {
            showTutorialPrompts()

            // Actualiza el estado de isFirstPokemon en SharedPreferences
            val editor = sharedPreferences.edit()
            editor.putBoolean("isFirstPokemon", false)
            editor.apply()
        }
        setupTouchListeners()
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

    private fun setupTouchListeners() {
        binding.cvPokemonDetail.setOnTouchListener { v, event ->
            handleSwipeGesture(event, ::onCardSwipeRight, ::onCardSwipeLeft)
        }

        binding.main.setOnTouchListener { v, event ->
            handleSwipeGesture(event, ::onMainSwipeRight, null)
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
            .setSecondaryText("Pulsa para escuchar el sonido de este Pokémon")
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
                    // Programar el segundo MaterialTapTargetPrompt para que aparezca después de un retraso
                    Handler(Looper.getMainLooper()).postDelayed({
                        showSecondPrompt()
                    }, 1000) // Retraso de 1 segundo antes de mostrar el segundo prompt
                }
            }
            .show()
    }


    private fun showSecondPrompt() {

        MaterialTapTargetPrompt.Builder(this)
            .setTarget(binding.cvPokemonDetail)
            .setPrimaryText("Imagen del Pokémon")
            .setSecondaryText("Desliza hacia un lateral para ver su versión shiny")
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
                }
            }
            .show()
    }


    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null

    }


    private fun onMainSwipeRight() {
        Log.d("Swipe", "Deslizado en Main hacia la derecha")

        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
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
        binding.apply {
            ivPokemonDetail.isVisible = false
            progressBarDetails.isVisible = true
            pokemonInfo.isVisible = false
            cvPokemonDescription.isVisible = false
            cvPokemonDetail.isVisible = false
            cvPokemonNameDetail.isVisible = false
            llHeightWeight.isVisible = false
            cvPokemonFirstType.isVisible = false

            val pokemonName = pokemonDetail.name
            pokemonImage = pokemonDetail.sprites.others?.image?.frontImage
            pokemonImageShiny = pokemonDetail.sprites.others?.image?.frontShiny
            val pokemonId = pokemonDetail.id
            val pokemonHeight = pokemonDetail.height
            val pokemonWeight = pokemonDetail.weight
            val pokemonFixedHeight = String.format("%.1f", pokemonHeight * 0.1) + " m"
            val pokemonFixedWeight = String.format("%.1f", pokemonWeight * 0.1) + " kg"
            val pokemonSound = pokemonDetail.cries["latest"]

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

            progressBarDetails.isVisible = false
            pokemonInfo.isVisible = true
            cvPokemonDescription.isVisible = true
            cvPokemonDetail.isVisible = true
            ivPokemonDetail.isVisible = true
            cvPokemonNameDetail.isVisible = true
            llHeightWeight.isVisible = true
            cvPokemonFirstType.isVisible = true

            if (pokemonSound != null) {
                delay(200)
                playPokemonSound(pokemonSound)
                cvPokemonDetail.setOnClickListener {
                    playPokemonSound(pokemonSound)

                }
            } else {
                Log.e("PokemonSound", "No se encontró la URL del sonido")
            }

            binding.cvPokemonDetail.setOnTouchListener { v, event ->
                gestureDetector.onTouchEvent(event)
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
        CoroutineScope(Dispatchers.Main).launch {

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


            val finalColor = firstColor


            binding.cvPokemonDetail.setCardBackgroundColor(finalColor)
            binding.cvPokemonNameDetail.setCardBackgroundColor(finalColor)
            binding.pokemonInfo.setBackgroundColor(finalColor)
            binding.cvPokemonDescription.setCardBackgroundColor(finalColor)
            binding.cvPokemonDescriptionInterior.setCardBackgroundColor(finalColor)
            binding.main.setBackgroundColor(finalColor)
            binding.cvPokemonHeight.setCardBackgroundColor(finalColor)
            binding.cvPokemonHeightInterior.setCardBackgroundColor(finalColor)
            binding.cvPokemonWeight.setCardBackgroundColor(finalColor)
            binding.cvPokemonWeightInterior.setCardBackgroundColor(finalColor)
            binding.cvPokemonFirstType.setBackgroundColor(firstColor)
            binding.cvPokemonSecondType.setBackgroundColor(secondColor)


        }
    }


}

