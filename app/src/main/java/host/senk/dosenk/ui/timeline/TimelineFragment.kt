package host.senk.dosenk.ui.timeline

import android.os.Bundle
import android.view.View
import androidx.activity.addCallback
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import host.senk.dosenk.R
import host.senk.dosenk.data.local.entity.MissionEntity
import host.senk.dosenk.util.applyDoSenkGradient
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope


import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.widget.TextView
import host.senk.dosenk.ui.nav.AddMenuBottomSheet
import kotlinx.coroutines.launch

@AndroidEntryPoint
class TimelineFragment : Fragment(R.layout.fragment_timeline) {

    private val viewModel: TimelineViewModel by viewModels()

    private lateinit var rvTimeline: RecyclerView
    private lateinit var adapter: TimelineAdapter

    // VARIABLES DEL ACHICADO Y AGRANDADO DE LA LINEA DE TIEMPO
    private lateinit var scaleGestureDetector: ScaleGestureDetector
    private var currentPixelsPerMinute = 4f // Empieza en 4 pixeles por minuto
    private val MIN_PIXELS = 1.5f // Límite para que no se aplaste hasta desaparecer
    private val MAX_PIXELS = 12f

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)




        setupInsets(view)
        setupGradients(view)
        setupBackButton()
        setupInteractiveElements(view)

        // NAVEGACIÓN DESDE EL MENÚ INFERIOR
        view.findViewById<View>(R.id.bottomNav)?.findViewById<View>(R.id.nav_timeline)?.setOnClickListener {
            findNavController().popBackStack()
        }

        // INICIALIZAR LA LISTA (Con nuestros datos de mentira por ahora)
        rvTimeline = view.findViewById(R.id.rvTimeline)
        rvTimeline.layoutManager = LinearLayoutManager(requireContext())

        adapter = TimelineAdapter(emptyList(), currentPixelsPerMinute) {
            // Clic vacío mientras carga
        }
        rvTimeline.adapter = adapter

        // OBSERVAMOS A LA BASE DE DATOS EN TIEMPO REAL
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.timelineItems.collect { items ->

                // INYECTAMOS LA LÓGICA DEL CLIC AL ADAPTER
                adapter = TimelineAdapter(items, currentPixelsPerMinute) { clickedMission ->

                    val currentTime = System.currentTimeMillis()

                    // REGLA: Tiene que ser 'pending' Y su hora de inicio debe ser en el FUTURO
                    if (clickedMission.status == "pending" && clickedMission.executionDate > currentTime) {

                        // ES EDITABLE
                        android.widget.Toast.makeText(requireContext(), "Editando: ${clickedMission.name}", android.widget.Toast.LENGTH_SHORT).show()

                        // TODO: Aquí mandaremos al usuario a la pantalla de edición

                    } else {
                        android.widget.Toast.makeText(requireContext(), "El pasado pisado, gallo. Esta misión ya no se puede alterar.", android.widget.Toast.LENGTH_LONG).show()
                    }
                }

                rvTimeline.adapter = adapter

                // AUTO-SCROLL A LA HORA ACTUAL
                if (items.isNotEmpty()) {
                    // Sacamos la hora actual en texto (Ej: "14:45")
                    val currentTimeStr = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date())

                    // Buscamos cuál es la tarjeta que sigue
                    val targetIndex = items.indexOfFirst { item ->
                        val timeLabel = when (item) {
                            is TimelineItem.MissionCard -> item.timeLabel
                            is TimelineItem.EmptySlot -> item.timeLabel
                        }
                        // Comparamos los textos. Magia: "15:00" es alfabéticamente mayor que "14:45"
                        timeLabel >= currentTimeStr
                    }

                    if (targetIndex != -1) {
                        // Le damos un respiro de 50ms a la interfaz para que termine de dibujar las tarjetas
                        rvTimeline.postDelayed({
                            (rvTimeline.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(targetIndex, 100)
                        }, 50)
                    }
                }
            }
        }


        setupPinchToZoom()
    }

    //  pintado de gradiantes
    private fun setupGradients(view: View) {
        // Pintamos el logo de arriba
        view.findViewById<View>(R.id.header)?.findViewById<View>(R.id.layoutLogoGradient)?.applyDoSenkGradient(cornerRadius = 12f)
        // Pintamos tu nueva caja de filtros
        view.findViewById<View>(R.id.cardFilter)?.findViewById<View>(R.id.layoutFilterGradient)?.applyDoSenkGradient(cornerRadius = 16f)
        // Pintamos la barra de abajo
        view.findViewById<View>(R.id.bottomNav)?.findViewById<View>(R.id.layoutBottomGradient)?.applyDoSenkGradient()
    }

    //  RESPETAR LA BARRA DE ESTADO Y GESTOS
    private fun setupInsets(view: View) {
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(top = insets.top, bottom = insets.bottom)
            WindowInsetsCompat.CONSUMED
        }


    }

    // FORZAR EL COMPORTAMIENTO DEL BOTÓN ATRÁS
    private fun setupBackButton() {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            findNavController().navigate(R.id.action_timeline_to_Home)
        }
    }


    ////////// PINCH - TO - ZOOM
    private fun setupPinchToZoom() {
        // Configuramos el cerebro matemático que calcula qué tanto separas los dedos
        scaleGestureDetector = ScaleGestureDetector(requireContext(), object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                // Multiplicamos la escala actual por la fuerza del pellizco
                currentPixelsPerMinute *= detector.scaleFactor

                // Limitamos el valor para que no se rompa la UI (CoerceIn es magia de Kotlin)
                currentPixelsPerMinute = currentPixelsPerMinute.coerceIn(MIN_PIXELS, MAX_PIXELS)

                // Le avisamos a tu Adapter que redibuje todo
                adapter.updateScale(currentPixelsPerMinute)
                return true
            }
        })

        //  Conectamos el cerebro al RecyclerView
        rvTimeline.addOnItemTouchListener(object : RecyclerView.SimpleOnItemTouchListener() {
            override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                // Le pasamos la información del toque al sensor de pellizcos
                scaleGestureDetector.onTouchEvent(e)

                //  TRUCO MAESTRO: Si hay más de 1 dedo, retornamos TRUE.
                // Esto intercepta el evento y evita que la lista haga "Scroll" mientras haces Zoom.
                return e.pointerCount > 1
            }

            override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {
                // Si interceptamos el evento (2 dedos), seguimos mandándole la info al sensor
                scaleGestureDetector.onTouchEvent(e)
            }
        })
    }


    private fun setupInteractiveElements(view: View) {
        // 1. El botón de agregar (+)
        val fabAdd = view.findViewById<View>(R.id.fabAddContainer)

        fabAdd.setOnClickListener {
            val bottomSheet = AddMenuBottomSheet()
            bottomSheet.show(parentFragmentManager, "AddMenuBottomSheet")
        }
        val tvUser = view.findViewById<View>(R.id.header).findViewById<TextView>(R.id.tvUsername)
        viewModel.currentUserAlias.observe(viewLifecycleOwner) { alias ->
            tvUser.text = "¿A trabajar, @${alias}?"
        }
    }










}