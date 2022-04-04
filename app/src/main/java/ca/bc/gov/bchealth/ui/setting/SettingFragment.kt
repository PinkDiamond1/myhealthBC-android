package ca.bc.gov.bchealth.ui.setting

import android.app.Activity
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import ca.bc.gov.bchealth.R
import ca.bc.gov.bchealth.databinding.FragmentSettingBinding
import ca.bc.gov.bchealth.ui.login.BcscAuthViewModel
import ca.bc.gov.bchealth.utils.AlertDialogHelper
import ca.bc.gov.bchealth.utils.viewBindings
import ca.bc.gov.bchealth.viewmodel.AnalyticsFeatureViewModel
import ca.bc.gov.common.model.settings.AnalyticsFeature
import com.snowplowanalytics.snowplow.Snowplow
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * [SettingFragment]
 *
 * @author amit metri
 */
@AndroidEntryPoint
class SettingFragment : Fragment(R.layout.fragment_setting) {

    private val binding by viewBindings(FragmentSettingBinding::bind)
    private val analyticsFeatureViewModel: AnalyticsFeatureViewModel by viewModels()
    private val viewModel: SettingsViewModel by viewModels()
    private val bcscAuthViewModel: BcscAuthViewModel by viewModels()

    private var logoutResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { activityResult ->
        if (activityResult.resultCode == Activity.RESULT_OK) {
            bcscAuthViewModel.processLogoutResponse()
            deleteAllRecordsAndSavedData()
        } else {
            deleteAllRecordsAndSavedData()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initUi()
        observeAuthStatus()
    }

    private fun initUi() {

        setUpToolbar()

        showLoader(true)

        analyticsSwitch()

        binding.tvDeleteAllRecords.setOnClickListener {
            showDeleteRecordsAlertDialog()
        }
    }

    private fun setUpToolbar() {
        binding.toolbar.apply {
            ivLeftOption.visibility = View.VISIBLE
            ivLeftOption.setImageResource(R.drawable.ic_action_back)
            tvTitle.visibility = View.VISIBLE
            tvTitle.text = getString(R.string.settings)
            ivLeftOption.setOnClickListener {
                findNavController().popBackStack()
            }
        }
    }

    private fun observeAuthStatus() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                bcscAuthViewModel.authStatus.collect {

                    showLoader(it.showLoading)

                    if (it.isError) {
                        bcscAuthViewModel.resetAuthStatus()
                        deleteAllRecordsAndSavedData()
                    }
                    if (it.endSessionIntent != null) {
                        logoutResultLauncher.launch(it.endSessionIntent)
                        bcscAuthViewModel.resetAuthStatus()
                    }
                }
            }
        }
    }

    private fun showLoader(value: Boolean) {
        binding.progressBar.isVisible = value
    }

    private fun analyticsSwitch() {
        binding.switchAnalytics.isChecked = Snowplow.getDefaultTracker()?.isTracking == false

        binding.switchAnalytics.setOnCheckedChangeListener { _, isChecked ->
            when (isChecked) {
                true -> {
                    Snowplow.getDefaultTracker()?.pause()
                    analyticsFeatureViewModel.toggleAnalyticsFeature(AnalyticsFeature.DISABLED)
                }
                false -> {
                    Snowplow.getDefaultTracker()?.resume()
                    analyticsFeatureViewModel.toggleAnalyticsFeature(AnalyticsFeature.ENABLED)
                }
            }
        }
    }

    private fun showDeleteRecordsAlertDialog() {
        AlertDialogHelper.showAlertDialog(
            context = requireContext(),
            title = getString(R.string.delete_data),
            msg = getString(R.string.delete_data_message),
            positiveBtnMsg = getString(R.string.delete),
            negativeBtnMsg = getString(R.string.cancel),
            positiveBtnCallback = {
                bcscAuthViewModel.getEndSessionIntent()
            }
        )
    }

    private fun deleteAllRecordsAndSavedData() {
        viewModel.deleteAllRecordsAndSavedData().invokeOnCompletion {
            navigatePostDeletion()
        }
    }

    private fun navigatePostDeletion() {
        findNavController().popBackStack(R.id.homeFragment, false)
    }
}
