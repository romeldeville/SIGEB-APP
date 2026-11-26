import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/network/dio_client.dart';
import '../domain/audit_entry.dart';

class AuditRepository {
  final Dio _dio;
  AuditRepository(this._dio);

  Future<List<AuditEntry>> consulter({String? iun, String? typeAction, int limite = 50}) async {
    final resp = await _dio.get('/audit', queryParameters: {
      'limite': limite,
      if (iun != null && iun.trim().isNotEmpty) 'iun': iun.trim(),
      if (typeAction != null && typeAction.trim().isNotEmpty) 'typeAction': typeAction.trim(),
    });
    return (resp.data as List).map((e) => AuditEntry.fromJson(e as Map<String, dynamic>)).toList();
  }

  Future<AuditIntegrity> verifierIntegrite() async {
    final resp = await _dio.get('/audit/integrite');
    return AuditIntegrity.fromJson(resp.data as Map<String, dynamic>);
  }
}

final auditRepositoryProvider = Provider<AuditRepository>((ref) {
  return AuditRepository(ref.watch(dioProvider));
});
