import 'dart:async';
import 'dart:io';

import 'package:flutter/services.dart';
import 'package:gallery_saver/files.dart';

class GallerySaver {
  static const String channelName = 'gallery_saver';
  static const String methodSaveImage = 'saveImage';
  static const String methodSaveVideo = 'saveVideo';

  static const String pleaseProvidePath = 'Please provide valid file path.';
  static const String fileIsNotVideo = 'File on path is not a video.';
  static const String fileIsNotImage = 'File on path is not an image.';
  static const String fileIsNotLocal = 'File on path locally available.';
  static const MethodChannel _channel = const MethodChannel(channelName);

  ///saves video from provided temp path and optional album name in gallery
  static Future<bool> saveVideo(String path, {String albumName}) async {
    File tempFile;
    if (path == null || path.isEmpty) {
      throw ArgumentError(pleaseProvidePath);
    }
    if (!isVideo(path)) {
      throw ArgumentError(fileIsNotVideo);
    }
    if (!isLocalFilePath(path)) {
      throw ArgumentError(fileIsNotLocal);
    }
    bool result = await _channel.invokeMethod(
      methodSaveVideo,
      <String, dynamic>{'path': path, 'albumName': albumName},
    );
    if (tempFile != null) {
      tempFile.delete();
    }
    return result;
  }

  ///saves image from provided temp path and optional album name in gallery
  static Future<bool> saveImage(String path, {String albumName}) async {
    File tempFile;
    if (path == null || path.isEmpty) {
      throw ArgumentError(pleaseProvidePath);
    }
    if (!await _checkIfFileIsImage(path)) {
      throw ArgumentError(fileIsNotImage);
    }
    if (!isLocalFilePath(path)) {
      throw ArgumentError(fileIsNotLocal);
    }

    bool result = await _channel.invokeMethod(
      methodSaveImage,
      <String, dynamic>{'path': path, 'albumName': albumName},
    );
    if (tempFile != null) {
      tempFile.delete();
    }

    return result;
  }

  static Future<bool> _checkIfFileIsImage(String path) async {
    return _channel.invokeMethod<bool>('image.check', path);
  }
}
