package org.jshobbysoft.webcamimagecapture

import android.content.Context
import android.content.SharedPreferences
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import coil.imageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.google.android.material.snackbar.Snackbar
import okhttp3.Credentials

data class ImageViewModel(val nickNameFromPrefs: String, val urlFromPrefs: String)

class CustomAdapter(
    private val dataSet: ArrayList<ImageViewModel>,
    private val prefs: SharedPreferences,
    private val icContext: Context
) :
    RecyclerView.Adapter<CustomAdapter.ViewHolder>() {

    /**
     * Provide a reference to the type of views that you are using
     * (custom ViewHolder).
     */
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textView: TextView
        val imageCapture: ImageView

        init {
            // Define click listener for the ViewHolder's View.
            textView = view.findViewById(R.id.textView)
            imageCapture = view.findViewById(R.id.image_capture)
        }
    }

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        // Create a new view, which defines the UI of the list item
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.recyclerview_item, viewGroup, false)
        return ViewHolder(view)
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {

        // Get element from your dataset at this position and replace the
        // contents of the view with that element
        val imageViewModel = dataSet[position]
        viewHolder.textView.text = imageViewModel.nickNameFromPrefs
        bindImage(viewHolder.imageCapture, imageViewModel.urlFromPrefs)

        viewHolder.imageCapture.setOnClickListener { v ->
            val bundle = bundleOf("url" to imageViewModel.urlFromPrefs)
            v.findNavController().navigate(R.id.action_FirstFragment_to_FullScreenFragment, bundle)
        }

        viewHolder.itemView.setOnClickListener { v ->
            val bundle = bundleOf("urlNickName" to imageViewModel.nickNameFromPrefs)
            v.findNavController().navigate(R.id.action_FirstFragment_to_ThirdFragment, bundle)
        }

        viewHolder.itemView.setOnLongClickListener {
            val builder = AlertDialog.Builder(icContext)
            builder.setMessage("Are you sure you want to Delete?")
                .setCancelable(false)
                .setPositiveButton("Yes") { _, _ ->
                    // Delete selected note from database
                    removeItem(position)
                    prefs.edit().remove(imageViewModel.nickNameFromPrefs).commit()
                }
                .setNegativeButton("No") { dialog, _ ->
                    // Dismiss the dialog
                    dialog.dismiss()
                }
            val alert = builder.create()
            alert.show()
            true
        }
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = dataSet.size

    // delete an item
    private fun removeItem(position: Int) {
        dataSet.removeAt(position)
        notifyDataSetChanged()
    }

//    fun updateList (dataSet: ArrayList<ImageViewModel>) {
//        bindImage(viewHolder.imageCapture, imageViewModel.urlFromPrefs)
//        notifyDataSetChanged()
//    }
}

fun bindImage(imgView: ImageView, imgUrl: CharSequence) {
    if (Regex("""https?://(.*?):(.*?)@.*""").containsMatchIn(imgUrl)) {
        val (camUsername, camPassword) = Regex("""https?://(.*?):(.*?)@.*""")
            .find(imgUrl)!!
            .destructured
        val credentials: String = Credentials.basic(camUsername, camPassword)
        imgUrl.let {
            val imageLoader = imgView.context.imageLoader
            val request = ImageRequest.Builder(imgView.context)
                .data(imgUrl)
                .setHeader("Authorization", credentials)
                .target(imgView)
                .placeholder(R.drawable.loading_animation)
                .error(R.drawable.ic_broken_image)
//                .networkCachePolicy(CachePolicy.DISABLED)
//                .diskCachePolicy(CachePolicy.DISABLED)
                .memoryCachePolicy(CachePolicy.DISABLED)
                .build()
            imageLoader.enqueue(request)
        }
    } else {
        imgUrl.let {
            val imageLoader = imgView.context.imageLoader
            val request = ImageRequest.Builder(imgView.context)
                .data(imgUrl)
                .target(imgView)
                .placeholder(R.drawable.loading_animation)
                .error(R.drawable.ic_broken_image)
//                .networkCachePolicy(CachePolicy.DISABLED)
//                .diskCachePolicy(CachePolicy.DISABLED)
                .memoryCachePolicy(CachePolicy.DISABLED)
                .build()
            imageLoader.enqueue(request)
        }
    }
}

fun bindImagetest(imgView: ImageView, imgUrl: CharSequence, view: View) {
    if (Regex("""https?://(.*?):(.*?)@.*""").containsMatchIn(imgUrl)) {
        val (camUsername, camPassword) = Regex("""https?://(.*?):(.*?)@.*""")
            .find(imgUrl)!!
            .destructured
        val credentials: String = Credentials.basic(camUsername, camPassword)
        imgUrl.let {
            val imageLoader = imgView.context.imageLoader
            val request = ImageRequest.Builder(imgView.context)
                .data(imgUrl)
                .setHeader("Authorization", credentials)
                .target(imgView)
                .placeholder(R.drawable.loading_animation)
                .error(R.drawable.ic_broken_image)
//                .networkCachePolicy(CachePolicy.DISABLED)
//                .diskCachePolicy(CachePolicy.DISABLED)
                .memoryCachePolicy(CachePolicy.DISABLED)
                .listener(onSuccess = {_, _ -> Snackbar.make(view,"Test Successful",Snackbar.LENGTH_LONG).show() },
                    onError = {_, throwable: Throwable -> Snackbar.make(view,"Error: ${throwable.message}",Snackbar.LENGTH_LONG).show() }
                )
                .build()
            imageLoader.enqueue(request)
        }
    } else {
        imgUrl.let {
            val imageLoader = imgView.context.imageLoader
            val request = ImageRequest.Builder(imgView.context)
                .data(imgUrl)
                .target(imgView)
                .placeholder(R.drawable.loading_animation)
                .error(R.drawable.ic_broken_image)
//                .networkCachePolicy(CachePolicy.DISABLED)
//                .diskCachePolicy(CachePolicy.DISABLED)
                .memoryCachePolicy(CachePolicy.DISABLED)
                .listener(onSuccess = {_, _ -> Snackbar.make(view,"Test Successful",Snackbar.LENGTH_LONG).show() },
                    onError = {_, throwable: Throwable -> Snackbar.make(view,"Error: ${throwable.message}",Snackbar.LENGTH_LONG).show() }
                )
                .build()
            imageLoader.enqueue(request)
        }
    }
}