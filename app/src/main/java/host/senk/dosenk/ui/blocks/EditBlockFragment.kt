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
import dagger.hilt.android.AndroidEntryPoint
import host.senk.dosenk.R
import host.senk.dosenk.util.applyDoSenkGradient
import kotlinx.coroutines.launch

import host.senk.dosenk.data.local.entity.BlockProfileEntity

@AndroidEntryPoint
class EditBlockFragment : Fragment(R.layout.fragment_edit_block) {

    private val viewModel: EditBlockViewModel by viewModels()
    private lateinit var rvApps: RecyclerView
    private lateinit var adapter: AppBlockAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupInsets(view)
        setupGradients(view)


        val layoutLoading = view.findViewById<View>(R.id.layoutLoading)
        rvApps = view.findViewById(R.id.rvApps)
        rvApps.layoutManager = LinearLayoutManager(requireContext())

        // Aseguramos que la lista empiece oculta
        rvApps.visibility = View.GONE

        rvApps = view.findViewById(R.id.rvApps)
        rvApps.layoutManager = LinearLayoutManager(requireContext())

        //  EL HEADER SÁDICO
        val tvUser = view.findViewById<View>(R.id.header).findViewById<TextView>(R.id.tvUsername)
        viewModel.currentUserAlias.observe(viewLifecycleOwner) { alias ->
            tvUser.text = "¿Asustado, @$alias?"
        }

        // CARGAMOS LAS APPS
        viewModel.loadApps(requireContext())

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.installedApps.collect { apps ->
                if (apps.isNotEmpty()) {
                    // OCULTAMOS EL LOADER Y MOSTRAMOS LA LISTA
                    layoutLoading.visibility = View.GONE
                    rvApps.visibility = View.VISIBLE

                    //  Pre-seleccionamos automáticamente los 5 peores vicios
                    val defaultVices = apps.take(5).map { it.packageName }.toMutableSet()

                    adapter = AppBlockAdapter(apps, defaultVices)
                    rvApps.adapter = adapter
                } else {
                    //  MIENTRAS ESTÉ VACÍO, MOSTRAMOS EL LOADER
                    layoutLoading.visibility = View.VISIBLE
                    rvApps.visibility = View.GONE
                }
            }
        }

        // EL BOTÓN DE GUARDAR
        val btnSave = view.findViewById<TextView>(R.id.btnSaveBlock)
        val etBlockName = view.findViewById<android.widget.EditText>(R.id.etBlockName)
        btnSave.setOnClickListener {
            if (::adapter.isInitialized) {
                val selectedPackages = adapter.selectedPackages
                val blockName = etBlockName.text.toString().trim()

                // CADENEROS DE VALIDACIÓN
                if (blockName.isEmpty()) {
                    Toast.makeText(requireContext(), "Bautiza tu bloqueo primero, gallo.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                if (selectedPackages.isEmpty()) {
                    Toast.makeText(requireContext(), "¡Cobarde! Selecciona al menos una app.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                // Convertimos el Set a JSON
                /*  val jsonBlockList = Gson().toJson(selectedPackages)
                val userUuidString = viewModel.currentUserUuid.value ?: "usuario_desconocido"

                val newProfile = BlockProfileEntity(
                    userUuid = userUuidString,
                    name = blockName,
                    blockedAppsJson = jsonBlockList
                )


                 */

                // Lanzamos la corrutina para guardar
                viewLifecycleOwner.lifecycleScope.launch {
                    // TODO: Necesitamos inyectar el DAO en el EditBlockViewModel para llamar a insertProfile(newProfile)


                    Toast.makeText(requireContext(), "Bloqueo '$blockName' guardado", Toast.LENGTH_SHORT).show()
                    findNavController().popBackStack()
                }
            }
        }

        // Botón ATRÁS del Header
        view.findViewById<View>(R.id.btnBack)?.setOnClickListener {
            findNavController().popBackStack()
        }
    }


    private fun setupGradients(view: View) {
        // El logo de arriba
        view.findViewById<View>(R.id.header)?.findViewById<View>(R.id.layoutLogoGradient)?.applyDoSenkGradient(cornerRadius = 12f)

        // El botón gigante de guardar
        view.findViewById<View>(R.id.btnSaveBlock)?.applyDoSenkGradient(cornerRadius = 24f)
    }

    // AJUSTANDO LOS BORDES
    private fun setupInsets(view: View) {
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(top = insets.top, bottom = insets.bottom)
            WindowInsetsCompat.CONSUMED
        }
    }
}