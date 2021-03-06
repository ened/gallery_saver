package carnegietechnologies.gallery_saver

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import androidx.core.app.ActivityCompat
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.PluginRegistry
import kotlinx.coroutines.*


/**
 * Class holding implementation of saving images and videos
 */
class GallerySaver internal constructor(private val context: Context) :
    PluginRegistry.RequestPermissionsResultListener {

    private var pendingResult: MethodChannel.Result? = null
    private var mediaType: MediaType? = null
    private var filePath: String = ""
    private var albumName: String = ""

    private val job = Job()
    private val uiScope = CoroutineScope(Dispatchers.Main + job)

    internal var activity: Activity? = null

    /**
     * Checks if the file specified by the [path] is an image.
     *
     * The function returns true/false depending on whether the bounds can be read.
     */
    internal fun isImage(path: String): Boolean {
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeFile(path, options)
        return (options.outWidth != -1 && options.outHeight != -1)
    }

    /**
     * Saves image or video to device
     *
     * @param methodCall - method call
     * @param result     - result to be set when saving operation finishes
     * @param mediaType    - media type
     */
    internal fun checkPermissionAndSaveFile(
        methodCall: MethodCall,
        result: MethodChannel.Result,
        mediaType: MediaType
    ) {
        filePath = methodCall.argument<Any>(KEY_PATH)?.toString() ?: ""
        albumName = methodCall.argument<Any>(KEY_ALBUM_NAME)?.toString() ?: ""
        this.mediaType = mediaType
        this.pendingResult = result

        if (isWritePermissionGranted()) {
            saveMediaFile()
        } else if (activity != null){
            ActivityCompat.requestPermissions(
                activity!!,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                REQUEST_EXTERNAL_IMAGE_STORAGE_PERMISSION
            )
        } else {
            result.error("missing-permission", "permissions can not be requested", null)
        }
    }

    private fun isWritePermissionGranted(): Boolean {
        return PackageManager.PERMISSION_GRANTED ==
                ActivityCompat.checkSelfPermission(
                    context, Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
    }

    private fun saveMediaFile() {
        uiScope.launch {
            val success = async(Dispatchers.IO) {
                if (mediaType == MediaType.Video) {
                    FileUtils.insertVideo(context.contentResolver, filePath, albumName)
                } else {
                    FileUtils.insertImage(context.contentResolver, filePath, albumName)
                }
            }
            success.await()
            finishWithSuccess()
        }
    }

    private fun finishWithSuccess() {
        pendingResult!!.success(true)
        pendingResult = null
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ): Boolean {
        val permissionGranted = grantResults.isNotEmpty()
                && grantResults[0] == PackageManager.PERMISSION_GRANTED

        if (requestCode == REQUEST_EXTERNAL_IMAGE_STORAGE_PERMISSION) {
            if (permissionGranted) {
                if (mediaType == MediaType.Video) {
                    FileUtils.insertVideo(context.contentResolver, filePath, albumName)
                } else {
                    FileUtils.insertImage(context.contentResolver, filePath, albumName)
                }
            }
        } else {
            return false
        }
        return true
    }

    companion object {

        private const val REQUEST_EXTERNAL_IMAGE_STORAGE_PERMISSION = 2408

        private const val KEY_PATH = "path"
        private const val KEY_ALBUM_NAME = "albumName"
    }
}