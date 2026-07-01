import 'package:flutter/foundation.dart';
import 'package:flutter/gestures.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter/services.dart';
import 'package:flutter/widgets.dart';

typedef FaceAiSdkViewCreatedCallback = void Function(FaceAiSdkController controller);

class FaceAiSdkView extends StatefulWidget {
  final FaceAiSdkViewCreatedCallback? onViewCreated;
  final Map<String, dynamic>? creationParams;

  const FaceAiSdkView({
    Key? key,
    this.onViewCreated,
    this.creationParams,
  }) : super(key: key);

  @override
  State<FaceAiSdkView> createState() => _FaceAiSdkViewState();
}

class _FaceAiSdkViewState extends State<FaceAiSdkView> {
  @override
  Widget build(BuildContext context) {
    const String viewType = 'com.faceaisdk/view';

    if (defaultTargetPlatform == TargetPlatform.android) {
      return PlatformViewLink(
        viewType: viewType,
        surfaceFactory: (context, controller) {
          return AndroidViewSurface(
            controller: controller as AndroidViewController,
            gestureRecognizers: const <Factory<OneSequenceGestureRecognizer>>{},
            hitTestBehavior: PlatformViewHitTestBehavior.opaque,
          );
        },
        onCreatePlatformView: (params) {
          return PlatformViewsService.initSurfaceAndroidView(
            id: params.id,
            viewType: viewType,
            layoutDirection: TextDirection.ltr,
            creationParams: widget.creationParams,
            creationParamsCodec: const StandardMessageCodec(),
            onFocus: () {
              params.onFocusChanged(true);
            },
          )
            ..addOnPlatformViewCreatedListener((int id) {
              params.onPlatformViewCreated(id);
              if (widget.onViewCreated != null) {
                widget.onViewCreated!(FaceAiSdkController(id));
              }
            })
            ..create();
        },
      );
    } else if (defaultTargetPlatform == TargetPlatform.iOS) {
      return UiKitView(
        viewType: viewType,
        onPlatformViewCreated: (int id) {
          if (widget.onViewCreated != null) {
            widget.onViewCreated!(FaceAiSdkController(id));
          }
        },
        creationParams: widget.creationParams,
        creationParamsCodec: const StandardMessageCodec(),
      );
    }
    return Text('$defaultTargetPlatform is not yet supported by the face_aisdk plugin');
  }
}

class FaceAiSdkController {
  final MethodChannel _channel;

  FaceAiSdkController(int id)
      : _channel = MethodChannel('com.faceaisdk/view_$id');

  Future<void> startScan() async {
    return _channel.invokeMethod('startScan');
  }

  Future<void> stopScan() async {
    return _channel.invokeMethod('stopScan');
  }
}
