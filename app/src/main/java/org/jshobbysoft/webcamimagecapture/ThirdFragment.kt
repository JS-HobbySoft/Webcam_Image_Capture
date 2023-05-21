package org.jshobbysoft.webcamimagecapture

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.setFragmentResultListener
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import org.jshobbysoft.webcamimagecapture.databinding.FragmentThirdBinding

/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class ThirdFragment : Fragment() {

    private var _binding: FragmentThirdBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private var param1: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString("urlNickName")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentThirdBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sP = androidx.preference.PreferenceManager.getDefaultSharedPreferences(requireActivity())
        var nicknameForEdit: String?
        var urlForEdit: String?

        setFragmentResultListener("requestKey") {_, bundle ->
            nicknameForEdit = bundle.getString("bundleKey")
            urlForEdit = sP.getString(nicknameForEdit,"defaultImageNickname")
            binding.editttextThirdNickname.setText(nicknameForEdit)
            binding.editttextThirdUrl.setText(urlForEdit)
        }

        if (param1 != null) {
            binding.editttextThirdNickname.setText(param1)
            binding.editttextThirdUrl.setText(sP.getString(param1,"defaultImageNickname"))
        }

        binding.buttonThirdTest.setOnClickListener {
            val newURL = binding.editttextThirdUrl.text.toString()
            bindImageTest(binding.imageCaptureTest, newURL,it)
        }

        binding.buttonThirdSave.setOnClickListener {
            val keyNickName = binding.editttextThirdNickname.text.toString()
            val keyNickNameURL = binding.editttextThirdUrl.text.toString()
            if ((keyNickName == "") or (keyNickNameURL == "")) {
                Snackbar.make(it, "URL and nickname must not be blank", Snackbar.LENGTH_LONG).show()
            } else {
                sP.edit()?.putString(keyNickName, keyNickNameURL)?.apply()
                findNavController().navigate(R.id.action_ThirdFragment_to_FirstFragment)
                Snackbar.make(it, "Nickname saved", Snackbar.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}