package host.senk.dosenk.ui.mission

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import host.senk.dosenk.R
import host.senk.dosenk.data.local.dao.UserDao
import host.senk.dosenk.util.DoRank
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlinx.coroutines.flow.first

@AndroidEntryPoint
class VictoryBottomSheet : BottomSheetDialogFragment() {

    @Inject lateinit var userDao: UserDao

    private var baseXP: Int = 0
    private var streakXP: Int = 0
    private var multiplier: Double = 1.0
    private var earnedXP: Int = 0

    override fun getTheme(): Int = com.google.android.material.R.style.Theme_Design_BottomSheetDialog

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.layout_bottom_sheet_victory, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (dialog as? BottomSheetDialog)?.behavior?.state = BottomSheetBehavior.STATE_EXPANDED

        // Extraer los XP del bundle
        baseXP = arguments?.getInt("baseXP") ?: 45
        streakXP = arguments?.getInt("streakXP") ?: 5
        multiplier = arguments?.getDouble("multiplier") ?: 5.0
        earnedXP = arguments?.getInt("totalXP") ?: 250

        val tvBaseXp = view.findViewById<TextView>(R.id.tvBaseXp)
        val tvStreakXp = view.findViewById<TextView>(R.id.tvStreakXp)
        val tvMultiplier = view.findViewById<TextView>(R.id.tvMultiplier)
        val tvTotalEarned = view.findViewById<TextView>(R.id.tvTotalEarned)
        val pbLevelUp = view.findViewById<ProgressBar>(R.id.pbLevelUp)
        val tvCurrentRank = view.findViewById<TextView>(R.id.tvCurrentRank)
        val tvProgressText = view.findViewById<TextView>(R.id.tvProgressText)
        val btnClaimXp = view.findViewById<TextView>(R.id.btnClaimXp)

        tvBaseXp.text = "+$baseXP XP"
        tvStreakXp.text = "+$streakXP XP"
        tvMultiplier.text = "x$multiplier"

        btnClaimXp.isEnabled = false
        btnClaimXp.alpha = 0.5f

        viewLifecycleOwner.lifecycleScope.launch {
            // LEEMOS AL USUARIO DE FORMA SEGURA 
            val currentUser = userDao.getActiveUser().first() ?: return@launch

            val oldTotalXp = currentUser.currentXp
            val newTotalXp = oldTotalXp + earnedXP

            // Animar el número total
            val numberAnimator = ValueAnimator.ofInt(0, earnedXP)
            numberAnimator.duration = 1500
            numberAnimator.addUpdateListener { anim ->
                tvTotalEarned.text = "${anim.animatedValue} XP"
            }
            numberAnimator.start()

            animateProgressBarChoreography(
                oldXp = oldTotalXp,
                targetXp = newTotalXp,
                pbLevelUp = pbLevelUp,
                tvCurrentRank = tvCurrentRank,
                tvProgressText = tvProgressText,
                onComplete = {
                    val finalRank = DoRank.getRankByXp(newTotalXp)
                    val updatedUser = currentUser.copy(
                        currentXp = newTotalXp,
                        rankName = finalRank.title
                    )
                    lifecycleScope.launch { userDao.updateUser(updatedUser) }

                    btnClaimXp.isEnabled = true
                    btnClaimXp.alpha = 1.0f
                }
            )
        }

        btnClaimXp.setOnClickListener {
            dismiss()
            // Vuelve al inicio
        }
    }

    private fun animateProgressBarChoreography(
        oldXp: Int, targetXp: Int, pbLevelUp: ProgressBar,
        tvCurrentRank: TextView, tvProgressText: TextView, onComplete: () -> Unit
    ) {
        val startRank = DoRank.getRankByXp(oldXp)
        val nextRank = getNextRank(startRank)

        updateRankTexts(oldXp, startRank, nextRank, tvCurrentRank, tvProgressText, pbLevelUp)

        if (targetXp >= nextRank.threshold && startRank != nextRank) {
            val fillToTopAnimator = ValueAnimator.ofInt(oldXp, nextRank.threshold)
            fillToTopAnimator.duration = 1000
            fillToTopAnimator.addUpdateListener { anim ->
                updateRankTexts(anim.animatedValue as Int, startRank, nextRank, tvCurrentRank, tvProgressText, pbLevelUp)
            }

            fillToTopAnimator.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    flashGold(pbLevelUp) {
                        val newlyAcquiredRank = DoRank.getRankByXp(nextRank.threshold)
                        val nextRankAfterLevelUp = getNextRank(newlyAcquiredRank)

                        updateRankTexts(nextRank.threshold, newlyAcquiredRank, nextRankAfterLevelUp, tvCurrentRank, tvProgressText, pbLevelUp)

                        val fillRemainderAnimator = ValueAnimator.ofInt(nextRank.threshold, targetXp)
                        fillRemainderAnimator.duration = 1000
                        fillRemainderAnimator.addUpdateListener { anim ->
                            updateRankTexts(anim.animatedValue as Int, newlyAcquiredRank, nextRankAfterLevelUp, tvCurrentRank, tvProgressText, pbLevelUp)
                        }
                        fillRemainderAnimator.addListener(object : AnimatorListenerAdapter() {
                            override fun onAnimationEnd(animation: Animator) { onComplete() }
                        })
                        fillRemainderAnimator.start()
                    }
                }
            })
            fillToTopAnimator.start()

        } else {
            val normalAnimator = ValueAnimator.ofInt(oldXp, targetXp)
            normalAnimator.duration = 1500
            normalAnimator.addUpdateListener { anim ->
                updateRankTexts(anim.animatedValue as Int, startRank, nextRank, tvCurrentRank, tvProgressText, pbLevelUp)
            }
            normalAnimator.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) { onComplete() }
            })
            normalAnimator.start()
        }
    }

    private fun updateRankTexts(
        currentXp: Int, currentRank: DoRank, nextRank: DoRank,
        tvCurrentRank: TextView, tvProgressText: TextView, pbLevelUp: ProgressBar
    ) {
        tvCurrentRank.text = "Rango: ${currentRank.title}"
        if (currentRank == nextRank) {
            tvProgressText.text = "$currentXp / MÁX"
            pbLevelUp.progress = 100
        } else {
            tvProgressText.text = "$currentXp / ${nextRank.threshold} XP"
            val progressRange = nextRank.threshold - currentRank.threshold
            val currentProgress = currentXp - currentRank.threshold
            val percent = ((currentProgress.toFloat() / progressRange.toFloat()) * 100).toInt()
            pbLevelUp.progress = percent
        }
    }


    private fun flashGold(pb: ProgressBar, onFlashComplete: () -> Unit) {
        val colorAnim = ValueAnimator.ofObject(ArgbEvaluator(), Color.parseColor("#00C853"), Color.parseColor("#FFD700"), Color.parseColor("#00C853"))
        colorAnim.duration = 400
        colorAnim.repeatCount = 1
        colorAnim.addUpdateListener { animator ->
            val color = animator.animatedValue as Int
            pb.progressTintList = ColorStateList.valueOf(color)
        }
        colorAnim.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) { onFlashComplete() }
        })
        colorAnim.start()
    }

    private fun getNextRank(currentRank: DoRank): DoRank {
        val nextIndex = currentRank.ordinal + 1
        return if (nextIndex < DoRank.entries.size) DoRank.entries[nextIndex] else currentRank
    }
}