package org.jshobbysoft.webcamimagecapture

import android.content.ContentValues
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.graphics.drawable.toBitmap
import androidx.core.os.bundleOf
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import coil.ImageLoader
import coil.imageLoader
import coil.request.CachePolicy
import coil.request.ErrorResult
import coil.request.ImageRequest
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
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Objects
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
        bindImage(viewHolder.imageCapture, imageViewModel.urlFromPrefs)

        viewHolder.imageCapture.setOnClickListener { v ->
            val bundle = bundleOf("url" to imageViewModel.urlFromPrefs)
            v.findNavController().navigate(R.id.action_FirstFragment_to_FullScreenFragment, bundle)
        }

        viewHolder.imageCapture.setOnLongClickListener {
            val builder = AlertDialog.Builder(icContext)
            builder.setMessage("Save image?")
                .setCancelable(false)
                .setPositiveButton("Yes") { _, _ ->
                    val imageBMP = viewHolder.imageCapture.drawable.toBitmap()
//                    saveImageToGallery(imageBMP,icContext)
//                    imageBMP.saveImage(icContext)
                    saveImage(viewHolder.imageCapture.drawable)
                }
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

fun bindImage(imgView: ImageView, imgUrl: CharSequence) {
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
                .memoryCachePolicy(CachePolicy.DISABLED)
                .build()
            imageLoader.enqueue(request)
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

//  https://stackoverflow.com/questions/71308298/how-to-save-image-from-imageview-to-gallery
private fun saveImage(drawable: Drawable) {
    val file = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
    val simpleDateFormat = SimpleDateFormat("yyyyMMddhhmmss")
    val date = simpleDateFormat.format(Date())
    val name = "IMG_$date.jpg"
    val fileName = file.absolutePath + "/" + name
    val newFile = File(fileName)

    try {
        val draw = drawable as BitmapDrawable
        val bitmap = draw.bitmap
//      https://stackoverflow.com/questions/68996975/how-can-an-app-write-and-read-file-in-documents-folder
        val fileOutPutStream = FileOutputStream(newFile)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fileOutPutStream)
//        println("File saved successfully")
        fileOutPutStream.flush()
        fileOutPutStream.close()
    } catch (e: FileNotFoundException) {
        e.printStackTrace()
    } catch (e: IOException) {
        e.printStackTrace()
    }
}

//  https://stackoverflow.com/questions/61541856/android-studio-kotlin-save-a-given-image-in-given-path-in-gallery-2020
private fun Bitmap.saveImage(context: Context): Uri? {

    val simpleDateFormat = SimpleDateFormat("yyyymmsshhmmss")
    val date = simpleDateFormat.format(Date())
    val name = "IMG_$date.jpg"

    if (android.os.Build.VERSION.SDK_INT >= 26) {
        val values = ContentValues()
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
//        values.put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
//        values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis())
        values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures")
        values.put(MediaStore.Images.Media.IS_PENDING, true)
//        values.put(MediaStore.Images.Media.DISPLAY_NAME, "img_${SystemClock.uptimeMillis()}")
        values.put(MediaStore.Images.Media.DISPLAY_NAME, name)

        val uri: Uri? =
            context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        if (uri != null) {
            saveImageToStream(this, context.contentResolver.openOutputStream(uri))
            values.put(MediaStore.Images.Media.IS_PENDING, false)
            context.contentResolver.update(uri, values, null, null)
//            return uri
        }
    } else {
//        val directory =
//            File(
//                context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
//                    .toString() + separator + "test_pictures"
//            )
//        if (!directory.exists()) {
//            directory.mkdirs()
//        }

        val file = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        if (!file.exists() && !file.mkdirs()) {
            file.mkdir()
        }

        val fileName = file.absolutePath + "/" + name
        val newFile = File(fileName)

//        val fileName = "img_${SystemClock.uptimeMillis()}" + ".jpeg"
//        val file = File(directory, fileName)
        saveImageToStream(this, FileOutputStream(newFile))
        val values = ContentValues()
        values.put(MediaStore.Images.Media.DATA, file.absolutePath)
        // .DATA is deprecated in API 29
        context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        return Uri.fromFile(file)
    }
    return null
}


fun saveImageToStream(bitmap: Bitmap, outputStream: OutputStream?) {
    if (outputStream != null) {
        try {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            outputStream.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}


//  https://www.youtube.com/watch?v=uRzl07a5VZk
private fun saveImageToGallery(bitmap: Bitmap, context: Context) {
    val fos: OutputStream

    try {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
            val resolver = context.contentResolver
            val contentValues = ContentValues()
            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME,"image.jpg")
            contentValues.put(MediaStore.MediaColumns.MIME_TYPE,"image/jpg")
            contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH,Environment.DIRECTORY_PICTURES)
            val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,contentValues)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
//            MediaStore.createWriteRequest(context.contentResolver, imageUri)
        }
            fos = resolver.openOutputStream(Objects.requireNonNull(imageUri)!!)!!
            bitmap.compress(Bitmap.CompressFormat.JPEG,100,fos)
            Objects.requireNonNull<OutputStream?>(fos)
//        }
        println("file saved")
    } catch (e: java.lang.Exception) {
        e.printStackTrace()
    }
}