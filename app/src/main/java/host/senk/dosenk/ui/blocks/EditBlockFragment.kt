package host.senk.dosenk.ui.blocks

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.AndroidEntryPoint
import host.senk.dosenk.R
import host.senk.dosenk.util.applyDoSenkGradient
import kotlinx.coroutines.launch

@AndroidEntryPoint
class EditBlockFragment : Fragment(R.layout.fragment_edit_block) {

    private val viewModel: EditBlockViewModel by viewModels()
    private lateinit var rvApps: RecyclerView
    private lateinit var adapter: AppBlockAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupInsets(view)
        setupGradients(view)

        // 1. INICIALIZACIÓN DE VISTAS
        val layoutLoading = view.findViewById<View>(R.id.layoutLoading)
        val etBlockName = view.findViewById<android.widget.EditText>(R.id.etBlockName)
        val btnSave = view.findViewById<TextView>(R.id.btnSaveBlock)
        val btnDeleteBlock = view.findViewById<TextView>(R.id.btnDeleteBlock)
        val tvPhaseTitle = view.findViewById<TextView>(R.id.tvPhaseTitle) // Para cambiar el título
        val tvUser = view.findViewById<View>(R.id.header)?.findViewById<TextView>(R.id.tvUsername)

        rvApps = view.findViewById(R.id.rvApps)
        rvApps.layoutManager = LinearLayoutManager(requireContext())
        rvApps.visibility = View.GONE

        // 2. MODO EDICIÓN VS CREACIÓN
        val originalProfileName = arguments?.getString("profileName")
        val savedAppsJson = arguments?.getString("profileAppsJson")

        if (originalProfileName != null) {
            // ESTAMOS EDITANDO
            etBlockName.setText(originalProfileName)
            tvPhaseTitle.text = "EDITAR BLOQUEO"
            btnDeleteBlock.visibility = View.VISIBLE
        } else {
            // ESTAMOS CREANDO
            tvPhaseTitle.text = "NUEVO BLOQUEO"
            btnDeleteBlock.visibility = View.GONE
        }

        // 3. HEADER SÁDICO
        viewModel.currentUserAlias.observe(viewLifecycleOwner) { alias ->
            tvUser?.text = "¿Asustado, @$alias?"
        }

        // 4. CARGA Y PRE-SELECCIÓN DE APPS
        viewModel.loadApps(requireContext())

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.installedApps.collect { apps ->
                if (apps.isNotEmpty()) {
                    layoutLoading.visibility = View.GONE
                    rvApps.visibility = View.VISIBLE

                    // Si es edición, cargamos las guardadas; si no, los 5 peores vicios.
                    val defaultVices: MutableSet<String> = if (savedAppsJson != null) {
                        try {
                            val type = object : TypeToken<Set<String>>() {}.type
                            Gson().fromJson(savedAppsJson, type) ?: mutableSetOf()
                        } catch (e: Exception) { mutableSetOf() }
                    } else {
                        apps.take(5).map { it.packageName }.toMutableSet()
                    }

                    adapter = AppBlockAdapter(apps, defaultVices)
                    rvApps.adapter = adapter
                } else {
                    layoutLoading.visibility = View.VISIBLE
                    rvApps.visibility = View.GONE
                }
            }
        }

        // 5. GUARDADO
        btnSave.setOnClickListener {
            if (::adapter.isInitialized) {
                val selectedPackages = adapter.selectedPackages
                val blockName = etBlockName.text.toString().trim()

                if (blockName.isEmpty()) {
                    Toast.makeText(requireContext(), "Bautiza tu bloqueo primero, gallo.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                if (selectedPackages.isEmpty()) {
                    Toast.makeText(requireContext(), "¡Cobarde! Selecciona al menos una app.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val jsonBlockList = Gson().toJson(selectedPackages)

                btnSave.isEnabled = false
                btnSave.text = "Guardando..."

                viewModel.saveCustomBlock(
                    originalName = originalProfileName,
                    blockName = blockName,
                    jsonBlockList = jsonBlockList,
                    onComplete = {
                        Toast.makeText(requireContext(), "Bloqueo '$blockName' guardado", Toast.LENGTH_SHORT).show()
                        findNavController().popBackStack()
                    },
                    onError = { errorMsg ->
                        btnSave.isEnabled = true
                        btnSave.text = "¡GUARDAR LISTA NEGRA!"
                        Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_LONG).show()
                    }
                )
            }
        }

        // 6. BORRADO
        btnDeleteBlock?.setOnClickListener {
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("¡ALERTA MÁXIMA!")
                .setMessage("Si borras este bloqueo, TODAS LAS MISIONES que lo tengan asignado se verán afectadas. >Do te recomienda que mejor edites sus restricciones.")
                .setPositiveButton("¡Tienes razón! Lo editaré") { dialog, _ ->
                    dialog.dismiss()
                }
                .setNegativeButton("QUIERO BORRARLO") { _, _ ->

                    // PASAMOS A LA FASE 2: REASIGNACIÓN
                    viewLifecycleOwner.lifecycleScope.launch {
                        val availableBlocks = viewModel.getFallbackBlocks(originalProfileName!!)
                        val blockNamesArray = availableBlocks.toTypedArray()
                        var selectedIndex = 0 // Por defecto apunta a "Dios"

                        androidx.appcompat.app.AlertDialog.Builder(requireContext())
                            .setTitle("Elige el nuevo castigo para las misiones huérfanas:")
                            .setSingleChoiceItems(blockNamesArray, selectedIndex) { _, which ->
                                selectedIndex = which
                            }
                            .setPositiveButton("¡Aniquilar y Reasignar!") { _, _ ->
                                val newTargetBlock = blockNamesArray[selectedIndex]

                                btnDeleteBlock.isEnabled = false
                                btnDeleteBlock.text = "Borrando..."

                                viewModel.deleteAndReassignBlock(
                                    oldBlockName = originalProfileName,
                                    newBlockName = newTargetBlock
                                ) {
                                    Toast.makeText(requireContext(), "Bloqueo aniquilado. Verifica la Timeline.", Toast.LENGTH_LONG).show()
                                    findNavController().popBackStack()
                                }
                            }
                            .setNegativeButton("Cancelar Proceso", null)
                            .show()
                    }
                }
                .show()
        }

        // boton de atras
        view.findViewById<View>(R.id.header)?.findViewById<View>(R.id.btnBack)?.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun setupGradients(view: View) {
        view.findViewById<View>(R.id.header)?.findViewById<View>(R.id.layoutLogoGradient)?.applyDoSenkGradient(cornerRadius = 12f)
        view.findViewById<View>(R.id.btnSaveBlock)?.applyDoSenkGradient(cornerRadius = 24f)
    }

    private fun setupInsets(view: View) {
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(top = insets.top, bottom = insets.bottom)
            WindowInsetsCompat.CONSUMED
        }
    }
}