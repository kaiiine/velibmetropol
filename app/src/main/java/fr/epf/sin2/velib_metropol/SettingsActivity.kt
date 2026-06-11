package fr.epf.sin2.velib_metropol

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import fr.epf.sin2.velib_metropol.data.SettingsStore
import fr.epf.sin2.velib_metropol.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.settingsRoot) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(top = bars.top, bottom = bars.bottom)
            insets
        }

        binding.buttonBack.setOnClickListener { finish() }

        // Périmètre de recherche
        val radius = SettingsStore.getRadiusMeters(this)
        binding.radiusSlider.value = radius.toFloat()
        binding.radiusValue.text = getString(R.string.radius_value, radius)

        binding.radiusSlider.addOnChangeListener { _, value, _ ->
            val meters = value.toInt()
            binding.radiusValue.text = getString(R.string.radius_value, meters)
            SettingsStore.setRadiusMeters(this, meters)
        }

        // Actualisation automatique
        binding.autoRefreshSwitch.isChecked = SettingsStore.isAutoRefreshEnabled(this)
        binding.autoRefreshSwitch.setOnCheckedChangeListener { _, isChecked ->
            SettingsStore.setAutoRefreshEnabled(this, isChecked)
        }
    }
}
