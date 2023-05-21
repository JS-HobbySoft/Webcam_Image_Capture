package org.jshobbysoft.webcamimagecapture

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import org.jshobbysoft.webcamimagecapture.databinding.FragmentFullScreenBinding

class FullScreenFragment : Fragment() {

    private var param1: String? = null

    private var _binding: FragmentFullScreenBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString("url")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFullScreenBinding.inflate(inflater, container, false)
        // Inflate the layout for this fragment
//        return inflater.inflate(R.layout.fragment_full_screen, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (param1 != null) {
//          pass bogus value for imgPos when starting full screen mode, downloading is not
//          available from this screen
            bindImage(binding.fullscreenImagecapture, param1.toString())
        }

        binding.fullscreenImagecapture.setOnClickListener {
            findNavController().navigate(R.id.action_FullScreenFragment_to_FirstFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}