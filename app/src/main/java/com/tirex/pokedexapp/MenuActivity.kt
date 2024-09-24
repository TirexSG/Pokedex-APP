package com.tirex.pokedexapp

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.LayoutInflaterCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.tirex.pokedexapp.databinding.ActivityMenuBinding

class MenuActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMenuBinding
    private var mediaPlayer: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMenuBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        mediaPlayer = MediaPlayer.create(this, R.raw.buttom_sound)
        binding.main.setOnClickListener {
            playSound()
            binding.pokeballAnimation.pauseAnimation()
            Handler(Looper.getMainLooper()).postDelayed({
                navigateToMain()
            }, 300)
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


        startCardAnimation()

    }

    private fun playSound() {
        mediaPlayer?.start()
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        val sharedPreferences = getSharedPreferences("PokedexPrefs", MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        val isFirstPokemon = sharedPreferences.getBoolean("isFirstPokemon", true)
        editor.putBoolean("isFirstPokemon", true).apply()

        Log.d("Tutorial", "isFirstPokemon enviado desde Menu: $isFirstPokemon")

        startActivity(intent)
    }

    private fun startCardAnimation() {
        // Animaciones de escalado para Y
        val scaleDown = ObjectAnimator.ofFloat(binding.cvPokemonButton, "scaleX", 0.95f).also {
            it.duration = 1000
            it.repeatCount = ObjectAnimator.INFINITE
            it.repeatMode = ObjectAnimator.REVERSE
        }

        val scaleDownY = ObjectAnimator.ofFloat(binding.cvPokemonButton, "scaleY", 0.95f).also {
            it.duration = 1000
            it.repeatCount = ObjectAnimator.INFINITE
            it.repeatMode = ObjectAnimator.REVERSE
        }

        val animatorSet = AnimatorSet()
        animatorSet.play(scaleDown).with(scaleDownY)
        animatorSet.start()
    }


}






