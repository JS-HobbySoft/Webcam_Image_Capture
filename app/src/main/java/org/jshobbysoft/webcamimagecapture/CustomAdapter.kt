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
import coil.ImageLoader
import coil.imageLoader
import coil.request.CachePolicy
import coil.request.ErrorResult
import coil.request.ImageRequest
import coil.result
import com.burgstaller.okhttp.AuthenticationCacheInterceptor
import com.burgstaller.okhttp.CachingAuthenticatorDecorator
import com.burgstaller.okhttp.DispatchingAuthenticator
import com.burgstaller.okhttp.basic.BasicAuthenticator
import com.burgstaller.okhttp.digest.CachingAuthenticator
import com.burgstaller.okhttp.digest.DigestAuthenticator
import com.burgstaller.okhttp.digest.Credentials as digestCredentials
import com.google.android.material.snackbar.Snackbar
import okhttp3.Credentials
import okhttp3.OkHttpClient
import java.util.concurrent.ConcurrentHashMap

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
        bindImage(viewHolder.imageCapture, imageViewModel.urlFromPrefs, position)

        viewHolder.imageCapture.setOnClickListener { v ->
            val bundle = bundleOf("url" to imageViewModel.urlFromPrefs)
            v.findNavController().navigate(R.id.action_FirstFragment_to_FullScreenFragment, bundle)
        }

        viewHolder.imageCapture.setOnLongClickListener {
            val builder = AlertDialog.Builder(icContext)
            builder.setMessage("Download image?")
                .setCancelable(false)
                .setPositiveButton("Yes") { _, _ ->
                    Snackbar.make(it,"To be implemented later",Snackbar.LENGTH_LONG).show()                }
                .setNegativeButton("No") { dialog, _ ->
                    // Dismiss the dialog
                    dialog.dismiss()
                }
            val alert = builder.create()
            alert.show()
            true
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

fun bindImage(imgView: ImageView, imgUrl: CharSequence, imgPos: Int) {
    if (Regex("""https?://(.*?):(.*?)@.*""").containsMatchIn(imgUrl)) {
        val (camUsername, camPassword) = Regex("""https?://(.*?):(.*?)@.*""")
            .find(imgUrl)!!
            .destructured
        val credentials: String = Credentials.basic(camUsername, camPassword)
        imgUrl.let {
            val imageLoader = ImageLoader.Builder(imgView.context)
                .okHttpClient {
                    buildHttpClient(camUsername,camPassword)
                }
                .build()
            val request = ImageRequest.Builder(imgView.context)
                .data(imgUrl)
                .setHeader("Authorization", credentials)
                .target(imgView)
                .placeholder(R.drawable.loading_animation)
                .error(R.drawable.ic_broken_image)
                .memoryCachePolicy(CachePolicy.DISABLED)
                .diskCacheKey(imgPos.toString())
                .build()
            imageLoader.enqueue(request)
//            imageLoader.diskCache?.get(imgUrl.toString())
            println("1 " + request.diskCacheKey)
        }
    } else {
        imgUrl.let {
            val imageLoader = imgView.context.imageLoader
            val request = ImageRequest.Builder(imgView.context)
                .data(imgUrl)
                .target(imgView)
                .placeholder(R.drawable.loading_animation)
                .error(R.drawable.ic_broken_image)
                .memoryCachePolicy(CachePolicy.DISABLED)
                .build()
            imageLoader.enqueue(request)
            println("2 " + imgView.result?.request?.diskCacheKey)
        }
    }
}

private fun buildHttpClient(username: String, password: String): OkHttpClient {
    // Library used for digest authentication: https://github.com/rburgst/okhttp-digest
    val digestAuthenticator = DigestAuthenticator(digestCredentials(username, password))
    val basicAuthenticator = BasicAuthenticator(digestCredentials(username, password))
    val authenticator = DispatchingAuthenticator.Builder()
        .with("digest", digestAuthenticator)
        .with("basic", basicAuthenticator)
        .build()
    val authCache: ConcurrentHashMap<String, CachingAuthenticator> =
        ConcurrentHashMap<String, CachingAuthenticator>()
    val decorator = CachingAuthenticatorDecorator(authenticator, authCache)
    return OkHttpClient.Builder()
        .authenticator(decorator)
        .addInterceptor(AuthenticationCacheInterceptor(authCache))
        .build()
}


fun bindImagetest(imgView: ImageView, imgUrl: CharSequence, view: View) {
    if (Regex("""https?://(.*?):(.*?)@.*""").containsMatchIn(imgUrl)) {
        val (camUsername, camPassword) = Regex("""https?://(.*?):(.*?)@.*""")
            .find(imgUrl)!!
            .destructured
        val credentials: String = Credentials.basic(camUsername, camPassword)
        imgUrl.let {
            val imageLoader = ImageLoader.Builder(imgView.context)
                .okHttpClient {
                    buildHttpClient(camUsername,camPassword)
                }
                .build()
            val request = ImageRequest.Builder(imgView.context)
                .data(imgUrl)
                .setHeader("Authorization", credentials)
                .target(imgView)
                .placeholder(R.drawable.loading_animation)
                .error(R.drawable.ic_broken_image)
                .memoryCachePolicy(CachePolicy.DISABLED)
                .listener(onSuccess = {_, _ -> Snackbar.make(view,"Test Successful",Snackbar.LENGTH_LONG).show() },
                    onError = {_, throwable: ErrorResult-> Snackbar.make(view,"Error: ${throwable.throwable}",Snackbar.LENGTH_LONG).show() }
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
                    onError = {_, throwable: ErrorResult -> Snackbar.make(view,"Error: ${throwable.throwable}",Snackbar.LENGTH_LONG).show() }
                )
                .build()
            imageLoader.enqueue(request)
        }
    }
}