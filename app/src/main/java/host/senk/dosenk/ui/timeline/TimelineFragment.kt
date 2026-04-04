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
import host.senk.dosenk.util.applyDoSenkGradient
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope

import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.widget.TextView
import android.widget.Toast
import android.util.TypedValue
import android.graphics.Color
import android.content.res.ColorStateList
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

    // VARIABLE DE ESTADO PARA EL DASHBOARD
    private var isHeaderExpanded = false
    // Control de navegación semanal (0 = esta semana, -1 = pasada, 1 = próxima)
    private var currentWeekOffset = 0
    private lateinit var rvWeeklyGrid: RecyclerView
    private lateinit var weeklyAdapter: WeeklyAdapter



    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupInsets(view)
        setupGradients(view)
        setupBackButton()
        setupInteractiveElements(view)

        setupDashboardToggle(view)
        setupWeekNavigation(view)



        // INICIALIZAR LA LISTA (Con nuestros datos)
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

                        // ¡ES EDITABLE!
                        val bundle = Bundle().apply {
                            putString("missionId", clickedMission.uuid)
                        }
                        findNavController().navigate(R.id.createMissionFragment, bundle)
                    } else {
                        Toast.makeText(requireContext(), "El pasado pisado, gallo. Esta misión ya no se puede alterar.", Toast.LENGTH_LONG).show()
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





        /////////sEMANALLLL


        // INICIALIZAR LA CUADRÍCULA SEMANAL
        rvWeeklyGrid = view.findViewById(R.id.rvWeeklyGrid)
        // GridLayoutManager de 2 columnas para que se vea como en tu diseño
        rvWeeklyGrid.layoutManager = LinearLayoutManager(requireContext())


        weeklyAdapter = WeeklyAdapter(emptyList()) { selectedDayTimestamp ->

            // Le decimos al ViewModel que cargue las misiones de ese día exacto
            viewModel.loadMissionsForToday(selectedDayTimestamp)

            //  Simulamos un clic en la pestaña "Día" para que la UI regrese solita
            view.findViewById<TextView>(R.id.tabDay)?.performClick()

            // Opcional: Actualizar el texto del Header para que diga la fecha seleccionada
            val dateStr = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(java.util.Date(selectedDayTimestamp))
            view.findViewById<TextView>(R.id.tvCurrentDate)?.text = dateStr
        }
        rvWeeklyGrid.adapter = weeklyAdapter


        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.weeklyItems.collect { weeklyCards ->
                if (::weeklyAdapter.isInitialized) {
                    weeklyAdapter.updateData(weeklyCards)
                }
            }
        }

    }

    private fun setupDashboardToggle(view: View) {
        val layoutHeaderClickable = view.findViewById<View>(R.id.layoutHeaderClickable)
        val layoutTabs = view.findViewById<View>(R.id.layoutTabs)
        val layoutSearchBar = view.findViewById<View>(R.id.layoutSearchBar)

        val layoutWeeklyView = view.findViewById<View>(R.id.layoutWeeklyView)
        val rvTimeline = view.findViewById<View>(R.id.rvTimeline)

        val tabDay = view.findViewById<TextView>(R.id.tabDay)
        val tabWeek = view.findViewById<TextView>(R.id.tabWeek)
        val tabMonth = view.findViewById<TextView>(R.id.tabMonth)

        // Extraemos el color activo del Tema
        val activeColor = TypedValue()
        requireContext().theme.resolveAttribute(R.attr.doSkinButton, activeColor, true)
        val inactiveColor = Color.parseColor("#888888")

        // 1. EL CLIC MÁGICO EN EL TÍTULO
        layoutHeaderClickable?.setOnClickListener {
            isHeaderExpanded = !isHeaderExpanded

            if (isHeaderExpanded) {
                // EXPANDIR: Mostramos los botones, escondemos el buscador
                layoutTabs?.visibility = View.VISIBLE
                layoutSearchBar?.visibility = View.GONE

                // Pasamos automáticamente a la vista semanal como demostración
                tabWeek?.performClick()
            } else {
                // COLAPSAR: Volvems al modo Día normal
                layoutTabs?.visibility = View.GONE
                layoutSearchBar?.visibility = View.VISIBLE

                rvTimeline?.visibility = View.VISIBLE
                layoutWeeklyView?.visibility = View.GONE
            }
        }

        // 2. LÓGICA DE LOS TABS (Para cambiar los colores y las vistas)
        tabDay?.setOnClickListener {
            tabDay.backgroundTintList = ColorStateList.valueOf(activeColor.data)
            tabWeek?.backgroundTintList = ColorStateList.valueOf(inactiveColor)
            tabMonth?.backgroundTintList = ColorStateList.valueOf(inactiveColor)

            // Mostramos lista diaria, ocultamos semanal
            rvTimeline?.visibility = View.VISIBLE
            layoutWeeklyView?.visibility = View.GONE

            // Como elegimos Día, mejor colapsamos el menú para que se vea el buscador de nuevo
            isHeaderExpanded = false
            layoutTabs?.visibility = View.GONE
            layoutSearchBar?.visibility = View.VISIBLE
        }

        tabWeek?.setOnClickListener {
            tabWeek.backgroundTintList = ColorStateList.valueOf(activeColor.data)
            tabDay?.backgroundTintList = ColorStateList.valueOf(inactiveColor)
            tabMonth?.backgroundTintList = ColorStateList.valueOf(inactiveColor)

            // Mostramos cuadrícula semanal, ocultamos diaria
            layoutWeeklyView?.visibility = View.VISIBLE
            rvTimeline?.visibility = View.GONE
            layoutSearchBar?.visibility = View.GONE
        }

        tabMonth?.setOnClickListener {
            tabMonth.backgroundTintList = ColorStateList.valueOf(activeColor.data)
            tabDay?.backgroundTintList = ColorStateList.valueOf(inactiveColor)
            tabWeek?.backgroundTintList = ColorStateList.valueOf(inactiveColor)

            Toast.makeText(requireContext(), "Vista Mensual Próximamente", Toast.LENGTH_SHORT).show()
        }
    }

    // PINTADO DE GRADIENTES
    private fun setupGradients(view: View) {
        view.findViewById<View>(R.id.header)?.findViewById<View>(R.id.layoutLogoGradient)?.applyDoSenkGradient(cornerRadius = 12f)
        view.findViewById<View>(R.id.layoutFilterGradient)?.applyDoSenkGradient(cornerRadius = 16f)
        view.findViewById<View>(R.id.bottomNav)?.findViewById<View>(R.id.layoutBottomGradient)?.applyDoSenkGradient()
    }

    // RESPETAR LA BARRA DE ESTADO Y GESTOS
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
        scaleGestureDetector = ScaleGestureDetector(requireContext(), object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                currentPixelsPerMinute *= detector.scaleFactor
                currentPixelsPerMinute = currentPixelsPerMinute.coerceIn(MIN_PIXELS, MAX_PIXELS)
                adapter.updateScale(currentPixelsPerMinute)
                return true
            }
        })

        rvTimeline.addOnItemTouchListener(object : RecyclerView.SimpleOnItemTouchListener() {
            override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                scaleGestureDetector.onTouchEvent(e)
                return e.pointerCount > 1
            }

            override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {
                scaleGestureDetector.onTouchEvent(e)
            }
        })
    }

    private fun setupInteractiveElements(view: View) {
        val fabAdd = view.findViewById<View>(R.id.fabAddContainer)

        fabAdd.setOnClickListener {
            val bottomSheet = AddMenuBottomSheet()
            bottomSheet.show(parentFragmentManager, "AddMenuBottomSheet")
        }
        val tvUser = view.findViewById<View>(R.id.header).findViewById<TextView>(R.id.tvUsername)
        viewModel.currentUserAlias.observe(viewLifecycleOwner) { alias ->
            tvUser.text = "¿A trabajar, @${alias}?"
        }

        val navBlocks = view.findViewById<View>(R.id.bottomNav)?.findViewById<View>(R.id.nav_blocks)

        view.findViewById<View>(R.id.bottomNav)?.findViewById<View>(R.id.nav_home)?.setOnClickListener {
            findNavController().navigate(R.id.homeFragment)
        }

        navBlocks?.setOnClickListener {
            findNavController().navigate(R.id.action_TimeLime_to_BlockZone)
        }

        view.findViewById<View>(R.id.bottomNav)?.findViewById<View>(R.id.nav_timeline)?.setOnClickListener {
            // Regresamos o hacemos popBackStack
            findNavController().popBackStack()
        }
    }








    ///////////////CALCULO DE SEMANAS!!!!!!!!!!!






    private fun setupWeekNavigation(view: View) {
        val btnPrevWeek = view.findViewById<TextView>(R.id.btnPrevWeek)
        val btnNextWeek = view.findViewById<TextView>(R.id.btnNextWeek)
        val tvWeekRange = view.findViewById<TextView>(R.id.tvWeekRange)

        updateWeekText(tvWeekRange)

        btnPrevWeek?.setOnClickListener {
            currentWeekOffset--
            updateWeekText(tvWeekRange)
            viewModel.setWeekOffset(currentWeekOffset)
        }

        btnNextWeek?.setOnClickListener {
            currentWeekOffset++
            updateWeekText(tvWeekRange)
            viewModel.setWeekOffset(currentWeekOffset)
        }
    }

    private fun updateWeekText(tvWeekRange: TextView?) {
        val calendar = java.util.Calendar.getInstance()

        // Nos movemos a la semana que el usuario eligió
        calendar.add(java.util.Calendar.WEEK_OF_YEAR, currentWeekOffset)

        // Calculamos el LUNES de esa semana
        calendar.set(java.util.Calendar.DAY_OF_WEEK, java.util.Calendar.MONDAY)
        val startDay = calendar.get(java.util.Calendar.DAY_OF_MONTH)
        val startMonth = calendar.getDisplayName(java.util.Calendar.MONTH, java.util.Calendar.SHORT, java.util.Locale("es", "ES"))?.replaceFirstChar { it.uppercase() }

        // Calculamos el DOMINGO de esa semana
        calendar.add(java.util.Calendar.DAY_OF_WEEK, 6)
        val endDay = calendar.get(java.util.Calendar.DAY_OF_MONTH)
        val endMonth = calendar.getDisplayName(java.util.Calendar.MONTH, java.util.Calendar.SHORT, java.util.Locale("es", "ES"))?.replaceFirstChar { it.uppercase() }
        val year = calendar.get(java.util.Calendar.YEAR)

        // Mostramos el texto dinámico. Ej: "5 al 11 de Abr, 2026" o "28 Mar al 3 Abr, 2026"
        if (startMonth == endMonth) {
            tvWeekRange?.text = "$startDay al $endDay de $startMonth, $year"
        } else {
            tvWeekRange?.text = "$startDay $startMonth al $endDay $endMonth, $year"
        }
    }










}