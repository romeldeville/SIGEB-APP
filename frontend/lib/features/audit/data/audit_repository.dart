import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/network/dio_client.dart';
import '../domain/audit_entry.dart';

class AuditRepository {
  final Dio _dio;
  AuditRepository(this._dio);

  Future<List<AuditEntry>> consulter({int limite = 50}) async {
    final resp = await _dio.get('/audit', queryParameters: {'limite': limite});
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
