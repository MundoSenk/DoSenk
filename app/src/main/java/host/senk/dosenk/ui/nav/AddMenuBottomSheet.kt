package host.senk.dosenk.ui.nav

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import host.senk.dosenk.R
import host.senk.dosenk.util.applyDoSenkGradient
import androidx.navigation.fragment.findNavController
import host.senk.dosenk.ui.mission.CreateMissionViewModel
import kotlin.getValue



class AddMenuBottomSheet : BottomSheetDialogFragment() {

    private val viewModel: CreateMissionViewModel by activityViewModels()



    override fun getTheme(): Int = com.google.android.material.R.style.Theme_Design_BottomSheetDialog

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.layout_bottom_sheet_add, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Pintamos las tarjetas con tu motor de gradientes
        view.findViewById<View>(R.id.bgMissionGradient)?.applyDoSenkGradient(cornerRadius = 20f)
        view.findViewById<View>(R.id.bgPurposeGradient)?.applyDoSenkGradient(cornerRadius = 20f)
        view.findViewById<View>(R.id.bgReminderGradient)?.applyDoSenkGradient(cornerRadius = 20f)

        // Botón CERRAR
        view.findViewById<View>(R.id.btnClose).setOnClickListener {
            dismiss()
        }

        // Click en Misión Diaria
        view.findViewById<View>(R.id.btnOptionMission).setOnClickListener {
            dismiss()


            findNavController().navigate(R.id.createMissionFragment)
            viewModel.resetForNewMission()
        }



    }
}