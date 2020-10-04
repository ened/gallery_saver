package carnegietechnologies.gallery_saver

import android.app.Activity
import android.content.Context
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry.Registrar

class GallerySaverPlugin : FlutterPlugin, ActivityAware, MethodCallHandler {

    private lateinit var gallerySaver: GallerySaver
    private var activityBinding: ActivityPluginBinding? = null

    companion object {
        @JvmStatic
        fun registerWith(registrar: Registrar) {
            val instance = GallerySaverPlugin()
            instance.startListening(registrar.context(), registrar.messenger())

            registrar.addRequestPermissionsResultListener(instance.gallerySaver)

            if (registrar.activeContext() is Activity) {
                instance.gallerySaver.activity = registrar.activity()
            }
        }
    }

    override fun onMethodCall(call: MethodCall, result: Result) {
        when (call.method) {
            "saveImage" -> gallerySaver.checkPermissionAndSaveFile(call, result, MediaType.Image)
            "saveVideo" -> gallerySaver.checkPermissionAndSaveFile(call, result, MediaType.Video)
            "image.check" -> {
                val path = call.arguments<String>()
                val isImage = gallerySaver.isImage(path)

                result.success(isImage)
            }
            else -> result.notImplemented()
        }
    }

    override fun onAttachedToEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        startListening(binding.applicationContext, binding.binaryMessenger)
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        onAttachedToActivity(binding)
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        activityBinding = binding

        binding.addRequestPermissionsResultListener(gallerySaver)
        gallerySaver.activity = binding.activity
    }

    override fun onDetachedFromActivityForConfigChanges() {
        onDetachedFromActivity()
    }

    override fun onDetachedFromActivity() {
        gallerySaver.activity = null

        activityBinding?.removeRequestPermissionsResultListener(gallerySaver)
        activityBinding = null
    }

    private fun startListening(context: Context, messenger: BinaryMessenger) {
        gallerySaver = GallerySaver(context)

        val channel = MethodChannel(messenger, "gallery_saver")
        channel.setMethodCallHandler(this)
    }
}
