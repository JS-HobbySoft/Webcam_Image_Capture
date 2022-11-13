package org.jshobbysoft.webcamimagecapture

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.*
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import org.jshobbysoft.webcamimagecapture.databinding.FragmentFirstBinding
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import kotlinx.coroutines.Runnable

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class FirstFragment : Fragment() {

    private var _binding: FragmentFirstBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var urlCustomAdapter: CustomAdapter

    private val mainHandler = Handler(Looper.getMainLooper())
    var refreshTimeIntMillis: Long = 2000
    private val runnable = object : Runnable {
        override fun run() {
            println("in runnable")
            urlCustomAdapter.notifyDataSetChanged()
            mainHandler.postDelayed(this, refreshTimeIntMillis)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentFirstBinding.inflate(inflater, container, false)
        binding.recyclerviewImages.layoutManager = LinearLayoutManager(activity)
        swipeRefreshLayout = binding.container

        // The usage of an interface lets you inject your own implementation
        val menuHost: MenuHost = requireActivity()

        // Add menu items without using the Fragment Menu APIs
        // Note how we can tie the MenuProvider to the viewLifecycleOwner
        // and an optional Lifecycle.State (here, RESUMED) to indicate when
        // the menu should be visible
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                // Add menu items here
                menuInflater.inflate(R.menu.menu_main, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                // Handle the menu selection
                return when (menuItem.itemId) {
                    R.id.action_settings -> {
                        val settingsIntent = Intent(requireContext(), SettingsActivity::class.java)
                        startActivity(settingsIntent)
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner)

        return binding.root
    }

    override fun onResume() {
        super.onResume()

        val sP = androidx.preference.PreferenceManager.getDefaultSharedPreferences(requireActivity())
        val allPrefsListed = sP.all
        val urlsAndNicknames = ArrayList<ImageViewModel>()

//        If a refresh time has not been set yet, set it to 0
        if (sP.getString("refreshTimeKey",null).isNullOrEmpty()) {
            sP.edit().putString("refreshTimeKey","0").apply()
        }

//        Put all nicknames and urls into an array list to pass to the adapter
        allPrefsListed.forEach { entry ->
            if (entry.key != "refreshTimeKey") {
                urlsAndNicknames.add(ImageViewModel(entry.key, entry.value.toString()))
            }
        }

//        Sort on the nicknames so the images are always in the same order
        urlsAndNicknames.sortBy { it.nickNameFromPrefs }

//        Load the data into the recyclerview
        urlCustomAdapter = CustomAdapter(urlsAndNicknames,sP,requireContext())
        binding.recyclerviewImages.adapter = urlCustomAdapter

//        handle swipe to refresh
        swipeRefreshLayout.setOnRefreshListener {
            urlCustomAdapter.notifyDataSetChanged()
            swipeRefreshLayout.isRefreshing = false
        }

//        Refresh at requested frequency
        val refreshTime = sP.getString("refreshTimeKey","-1")
        val refreshTimeInt = refreshTime!!.toLong()
        refreshTimeIntMillis = refreshTimeInt * 1000

        if (refreshTime != "0") {
            runnable.run()
        } else {
            mainHandler.removeCallbacks(runnable)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}