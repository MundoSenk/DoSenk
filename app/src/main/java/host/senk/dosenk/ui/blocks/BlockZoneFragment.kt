package host.senk.dosenk.ui.blocks

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.AndroidEntryPoint
import host.senk.dosenk.R
import host.senk.dosenk.ui.mission.CreateMissionViewModel
import host.senk.dosenk.ui.nav.AddMenuBottomSheet
import host.senk.dosenk.util.applyDoSenkGradient
import kotlinx.coroutines.launch


import kotlinx.coroutines.flow.first

@AndroidEntryPoint
class BlockZoneFragment : Fragment(R.layout.fragment_block_zone) {

    private val viewModel: CreateMissionViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupInsets(view)
        setupGradients(view)
        setupInteractiveElements(view)
        setupBackButton()

        val btnHumano = view.findViewById<TextView>(R.id.btnChooseHumano)
        val btnDios = view.findViewById<TextView>(R.id.btnChooseDios)

        btnHumano.applyDoSenkGradient(cornerRadius = 16f)
        btnDios.applyDoSenkGradient(cornerRadius = 16f)

        val isSelectionMode = arguments?.getBoolean("isSelectionMode") ?: false

        if (isSelectionMode) {
            btnHumano.text = "¡ESCÓGELO!"
            btnDios.text = "¡ESCÓGELO!"
            btnHumano.setOnClickListener { v -> saveAndNavigate("Humano", v) }
            btnDios.setOnClickListener { v -> saveAndNavigate("Dios", v) }
        } else {
            btnHumano.text = "MUÉSTRAMELO"
            btnDios.text = "MUÉSTRAMELO"
            btnHumano.setOnClickListener {
                Toast.makeText(requireContext(), "Demostración: Bloqueo Humano activado ", Toast.LENGTH_SHORT).show()
            }
            btnDios.setOnClickListener {
                Toast.makeText(requireContext(), "Demostración: Bloqueo Dios activado ", Toast.LENGTH_SHORT).show()
            }
        }

        view.findViewById<View>(R.id.btnEditCustomBlock).setOnClickListener {
            findNavController().navigate(R.id.action_BlockZone_to_editBlock)
        }




        loadCustomBlocks(view, isSelectionMode)
    }

    private fun loadCustomBlocks(rootView: View, isSelectionMode: Boolean) {
        // Buscamos el contenedor vacío que pusiste en el XML
        val layoutCustomBlocks = rootView.findViewById<LinearLayout>(R.id.layoutCustomBlocks)
        layoutCustomBlocks.removeAllViews()

        //  Le preguntamos a la base de datos
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.allCustomBlocks.collect { profiles ->
                // profiles es la lista de bloqueos que el usuario ha creado

                for (profile in profiles) {
                    //  Por cada bloqueo,
                    val cardView = LayoutInflater.from(requireContext())
                        .inflate(R.layout.item_custom_block_card, layoutCustomBlocks, false)

                    // Llenamos los datos del molde
                    val tvName = cardView.findViewById<TextView>(R.id.tvCustomNameLeft)
                    val tvAppsCount = cardView.findViewById<TextView>(R.id.tvCustomAppsCount)
                    val btnChoose = cardView.findViewById<TextView>(R.id.btnChooseCustom)

                    tvName.text = profile.name //.

                    // Extraemos cuántas apps bloqueó desencriptando el JSON
                    try {
                        val type = object : TypeToken<Set<String>>() {}.type
                        val appsSet: Set<String> = Gson().fromJson(profile.blockedAppsJson, type)
                        tvAppsCount.text = "BLOQUEA ${appsSet.size} APLICACIONES"
                    } catch (e: Exception) {
                        tvAppsCount.text = "BLOQUEA APLICACIONES"
                    }

                    //  Le damos vida al botón
                    if (isSelectionMode) {
                        btnChoose.text = "¡ESCÓGELO!"
                        btnChoose.setOnClickListener { v ->
                            saveAndNavigate(profile.name, v)
                        }
                    } else {
                        btnChoose.text = "EDITAR"
                        btnChoose.setOnClickListener {
                            viewLifecycleOwner.lifecycleScope.launch {
                                // Escudo anti-trampas
                                val activeMission = viewModel.missionDao.getActiveMission().first()
                                if (activeMission != null && activeMission.blockType == profile.name) {
                                    Toast.makeText(requireContext(), "¡Tramposo! No puedes editar un bloqueo mientras cumples castigo con él.", Toast.LENGTH_LONG).show()
                                } else {
                                    // 🚨 ¡AQUÍ ESTABA EL ERROR! Asegúrate de que el Bundle empaca los datos exactos:
                                    val bundle = Bundle().apply {
                                        putString("profileName", profile.name)
                                        putString("profileAppsJson", profile.blockedAppsJson)
                                    }
                                    findNavController().navigate(R.id.action_BlockZone_to_editBlock, bundle)
                                }
                            }
                        }
                    }

                    //  Pintamos el gradiente de la nueva tarjeta
                    val bgGradient = cardView.findViewById<View>(R.id.bgCustomGradient)
                    bgGradient.applyDoSenkGradient() // Por ahora usa el mismo degradado de la app

                    // Finalmente, metemos la tarjeta clonada y llena al contenedor
                    layoutCustomBlocks.addView(cardView)
                }
            }
        }
    }


    // MAGIA DE COLORES
    private fun setupGradients(view: View) {
        view.findViewById<View>(R.id.layoutZoneHeader).applyDoSenkGradient()
        view.findViewById<View>(R.id.btnEditCustomBlock).applyDoSenkGradient(cornerRadius = 16f)
        view.findViewById<View>(R.id.bgHumanoGradient).applyDoSenkGradient()
        view.findViewById<View>(R.id.bgDiosGradient).applyDoSenkGradient()

        // Pintamos el Header y el Nav
        view.findViewById<View>(R.id.header)?.findViewById<View>(R.id.layoutLogoGradient)?.applyDoSenkGradient(cornerRadius = 12f)
        view.findViewById<View>(R.id.bottomNav)?.findViewById<View>(R.id.layoutBottomGradient)?.applyDoSenkGradient()
    }

    //  RESPETAR LA BARRA DE ESTADO
    private fun setupInsets(view: View) {
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(top = insets.top, bottom = insets.bottom)
            WindowInsetsCompat.CONSUMED
        }
    }

    //  CLICS DE NAVEGACIÓN
    private fun setupInteractiveElements(view: View) {
        // Botón FAB Central
        val fabAdd = view.findViewById<View>(R.id.fabAddContainer)

        fabAdd.setOnClickListener {
            val bottomSheet = AddMenuBottomSheet()
            bottomSheet.show(parentFragmentManager, "AddMenuBottomSheet")
        }

        // Navegar de regreso a Inicio o Timeline
        view.findViewById<View>(R.id.bottomNav)?.findViewById<View>(R.id.nav_home)?.setOnClickListener {
            findNavController().navigate(R.id.homeFragment)
        }
        view.findViewById<View>(R.id.bottomNav)?.findViewById<View>(R.id.nav_timeline)?.setOnClickListener {
            findNavController().navigate(R.id.action_BlockZone_to_TimeLime)
        }

        // El Header text estático por ahora
        val tvUser = view.findViewById<View>(R.id.header)?.findViewById<TextView>(R.id.tvUsername)
        tvUser?.text = "Bloqueos, @User"
    }

    // COMPORTAMIENTO DEL BOTÓN "ATRÁS"
    private fun setupBackButton() {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            findNavController().popBackStack()
        }
    }

    private fun saveAndNavigate(blockType: String, clickedButton: View) {
        clickedButton.isEnabled = false

        viewModel.saveMissionToDatabase(blockType) {
            Toast.makeText(requireContext(), "¡Misión $blockType programada!", Toast.LENGTH_SHORT).show()
            clickedButton.isEnabled = true
            // Viajamos al Timeline para ver nuestra obra de arte
            findNavController().navigate(R.id.action_BlockZone_to_TimeLime)
        }
    }
}