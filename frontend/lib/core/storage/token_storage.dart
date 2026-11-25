import 'package:flutter/foundation.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';

/// Stockage sécurisé des jetons :
/// - Mobile (Android/iOS) : Keystore / Keychain via flutter_secure_storage
/// - Web : uniquement en mémoire (jamais localStorage, résistance XSS)
class TokenStorage {
  static const _accessKey = 'sigeb_access_token';
  static const _refreshKey = 'sigeb_refresh_token';

  final FlutterSecureStorage _secureStorage = const FlutterSecureStorage();
  String? _memAccess;
  String? _memRefresh;

  Future<void> saveTokens({required String accessToken, required String refreshToken}) async {
    _memAccess = accessToken;
    _memRefresh = refreshToken;
    if (!kIsWeb) {
      await _secureStorage.write(key: _accessKey, value: accessToken);
      await _secureStorage.write(key: _refreshKey, value: refreshToken);
    }
  }

  Future<String?> getAccessToken() async {
    if (kIsWeb) return _memAccess;
    return _memAccess ??= await _secureStorage.read(key: _accessKey);
  }

  Future<String?> getRefreshToken() async {
    if (kIsWeb) return _memRefresh;
    return _memRefresh ??= await _secureStorage.read(key: _refreshKey);
  }

  Future<void> clear() async {
    _memAccess = null;
    _memRefresh = null;
    if (!kIsWeb) {
      await _secureStorage.delete(key: _accessKey);
      await _secureStorage.delete(key: _refreshKey);
    }
  }
}

final tokenStorageProvider = Provider<TokenStorage>((ref) => TokenStorage());
