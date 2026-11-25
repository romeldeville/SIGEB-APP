import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/network/dio_client.dart';
import '../domain/qr_verify_result.dart';

class VerificationRepository {
  final Dio _dio;
  VerificationRepository(this._dio);

  Future<QrVerifyResult> verifierQr(String qrPayload) async {
    final resp = await _dio.post('/qr/verify', data: {'qrPayload': qrPayload.trim()});
    return QrVerifyResult.fromJson(resp.data as Map<String, dynamic>);
  }

  Future<List<Map<String, dynamic>>> rechercheNationale({
    String? iun,
    String? nom,
    String? prenoms,
    String? dateNaissance,
    required String motif,
  }) async {
    final resp = await _dio.post('/verification/recherche-nationale', data: {
      if (iun != null && iun.isNotEmpty) 'iun': iun.trim(),
      if (nom != null && nom.isNotEmpty) 'nom': nom.trim(),
      if (prenoms != null && prenoms.isNotEmpty) 'prenoms': prenoms.trim(),
      if (dateNaissance != null && dateNaissance.isNotEmpty) 'dateNaissance': dateNaissance.trim(),
      'motif': motif.trim(),
    });
    return (resp.data as List).cast<Map<String, dynamic>>();
  }
}

final verificationRepositoryProvider = Provider<VerificationRepository>((ref) {
  return VerificationRepository(ref.watch(dioProvider));
});
