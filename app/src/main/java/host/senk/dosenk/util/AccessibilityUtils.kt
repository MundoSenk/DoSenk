package host.senk.dosenk.util

import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import android.text.TextUtils
import host.senk.dosenk.service.BlockerService

object AccessibilityUtils {

    fun isServiceEnabled(context: Context): Boolean {
        val expectedComponentName = ComponentName(context, BlockerService::class.java)

        val enabledServicesSetting = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        val colonSplitter = TextUtils.SimpleStringSplitter(':')
        colonSplitter.setString(enabledServicesSetting)

        while (colonSplitter.hasNext()) {
            val componentNameString = colonSplitter.next()
            val enabledComponent = ComponentName.unflattenFromString(componentNameString)

            if (enabledComponent != null && enabledComponent == expectedComponentName) {
                return true
            }
        }
        return false
    }
}