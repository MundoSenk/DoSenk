package host.senk.dosenk.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import host.senk.dosenk.data.local.UserPreferences
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    userPreferences: UserPreferences
) : ViewModel() {



    val currentUserAlias = userPreferences.userAlias.asLiveData()
}