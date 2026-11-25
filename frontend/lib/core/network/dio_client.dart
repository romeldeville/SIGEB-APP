import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:uuid/uuid.dart';

import '../../app/l10n/app_strings.dart';
import '../config/api_config.dart';
import '../storage/token_storage.dart';

final dioProvider = Provider<Dio>((ref) {
  final storage = ref.watch(tokenStorageProvider);
  final dio = Dio(BaseOptions(
    baseUrl: ApiConfig.baseUrl,
    connectTimeout: const Duration(seconds: 15),
    receiveTimeout: const Duration(seconds: 25),
    headers: {'Content-Type': 'application/json'},
  ));

  const uuid = Uuid();

  dio.interceptors.add(InterceptorsWrapper(
    onRequest: (options, handler) async {
      final token = await storage.getAccessToken();
      if (token != null && token.isNotEmpty) {
        options.headers['Authorization'] = 'Bearer $token';
      }
      options.headers['Accept-Language'] = ref.read(localeProvider).toLowerCase();

      // Clé d'idempotence automatique sur les créations POST (réseaux ruraux instables)
      if (options.method.toUpperCase() == 'POST' &&
          !options.path.contains('/auth/') &&
          !options.path.contains('/qr/verify') &&
          options.headers['Idempotency-Key'] == null) {
        options.headers['Idempotency-Key'] = uuid.v4();
      }
      handler.next(options);
    },
    onError: (error, handler) async {
      // Tentative de refresh automatique sur 401 (hors /auth/login et /auth/refresh)
      final reqPath = error.requestOptions.path;
      if (error.response?.statusCode == 401 &&
          !reqPath.contains('/auth/login') &&
          !reqPath.contains('/auth/refresh') &&
          error.requestOptions.extra['_retried'] != true) {
        final refreshToken = await storage.getRefreshToken();
        if (refreshToken != null && refreshToken.isNotEmpty) {
          try {
            final refreshDio = Dio(BaseOptions(baseUrl: ApiConfig.baseUrl));
            final resp = await refreshDio.post(
              '/auth/refresh',
              data: {'refreshToken': refreshToken},
            );
            final newAccess = resp.data['accessToken'] as String;
            final newRefresh = resp.data['refreshToken'] as String;
            await storage.saveTokens(accessToken: newAccess, refreshToken: newRefresh);

            final retryOpts = error.requestOptions;
            retryOpts.headers['Authorization'] = 'Bearer $newAccess';
            retryOpts.extra['_retried'] = true;
            final retried = await dio.fetch(retryOpts);
            return handler.resolve(retried);
          } catch (_) {
            await storage.clear();
          }
        }
      }
      handler.next(error);
    },
  ));

  return dio;
});

String extractApiError(Object error) {
  if (error is DioException) {
    final data = error.response?.data;
    if (data is Map && data['detail'] != null) {
      return data['detail'].toString();
    }
    if (error.type == DioExceptionType.connectionError ||
        error.type == DioExceptionType.connectionTimeout) {
      return 'Serveur SIGEB injoignable. Vérifiez votre connexion réseau.';
    }
    return error.message ?? 'Erreur réseau';
  }
  return error.toString();
}
