import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/network/dio_client.dart';
import '../domain/auth_session.dart';
import '../domain/user_profile.dart';

class AuthRepository {
  final Dio _dio;
  AuthRepository(this._dio);

  Future<AuthSession> login({
    required String identifiant,
    required String motDePasse,
    String? totpCode,
  }) async {
    final resp = await _dio.post('/auth/login', data: {
      'identifiant': identifiant.trim(),
      'motDePasse': motDePasse,
      if (totpCode != null && totpCode.trim().isNotEmpty) 'totpCode': totpCode.trim(),
    });
    return AuthSession.fromJson(resp.data as Map<String, dynamic>);
  }

  Future<UserProfile> getMyProfile() async {
    final resp = await _dio.get('/auth/me');
    return UserProfile.fromJson(resp.data as Map<String, dynamic>);
  }

  Future<UserProfile> updateMyProfile({
    required String nomComplet,
    String? telephone,
    required String languePreferee,
  }) async {
    final resp = await _dio.patch('/auth/me', data: {
      'nomComplet': nomComplet.trim(),
      'telephone': telephone?.trim(),
      'languePreferee': languePreferee.trim().toUpperCase(),
    });
    return UserProfile.fromJson(resp.data as Map<String, dynamic>);
  }

  Future<void> changePin({required String motDePasse, required String pin}) async {
    await _dio.post('/auth/change-pin', data: {'motDePasse': motDePasse, 'pin': pin});
  }

  Future<void> changePassword({required String ancien, required String nouveau}) async {
    await _dio.post('/auth/change-password', data: {
      'ancienMotDePasse': ancien,
      'nouveauMotDePasse': nouveau,
    });
  }

  Future<TotpSetupData> setupTotp() async {
    final resp = await _dio.post('/auth/2fa/setup');
    return TotpSetupData.fromJson(resp.data as Map<String, dynamic>);
  }

  Future<void> confirmTotp(String code) async {
    await _dio.post('/auth/2fa/confirm', data: {'code': code.trim()});
  }
}

final authRepositoryProvider = Provider<AuthRepository>((ref) {
  return AuthRepository(ref.watch(dioProvider));
});
