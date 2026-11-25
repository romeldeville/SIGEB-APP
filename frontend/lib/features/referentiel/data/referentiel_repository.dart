import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/network/dio_client.dart';
import '../domain/referentiel_models.dart';

class ReferentielRepository {
  final Dio _dio;
  ReferentielRepository(this._dio);

  Future<List<ProvinceItem>> listerProvinces() async {
    final resp = await _dio.get('/referentiel/provinces');
    return (resp.data as List).map((e) => ProvinceItem.fromJson(e as Map<String, dynamic>)).toList();
  }

  Future<List<DioceseItem>> listerDioceses() async {
    final resp = await _dio.get('/referentiel/dioceses');
    return (resp.data as List).map((e) => DioceseItem.fromJson(e as Map<String, dynamic>)).toList();
  }

  Future<DioceseItem> creerDiocese(Map<String, dynamic> payload) async {
    final resp = await _dio.post('/referentiel/dioceses', data: payload);
    return DioceseItem.fromJson(resp.data as Map<String, dynamic>);
  }

  Future<List<ParoisseItem>> listerParoisses() async {
    final resp = await _dio.get('/referentiel/paroisses');
    return (resp.data as List).map((e) => ParoisseItem.fromJson(e as Map<String, dynamic>)).toList();
  }

  Future<ParoisseItem> creerParoisse(Map<String, dynamic> payload) async {
    final resp = await _dio.post('/referentiel/paroisses', data: payload);
    return ParoisseItem.fromJson(resp.data as Map<String, dynamic>);
  }

  Future<List<UtilisateurItem>> listerUtilisateurs() async {
    final resp = await _dio.get('/utilisateurs');
    return (resp.data as List).map((e) => UtilisateurItem.fromJson(e as Map<String, dynamic>)).toList();
  }

  Future<UtilisateurItem> creerUtilisateur(Map<String, dynamic> payload) async {
    final resp = await _dio.post('/utilisateurs', data: payload);
    return UtilisateurItem.fromJson(resp.data as Map<String, dynamic>);
  }

  Future<void> changerStatutUtilisateur(String id, String statut) async {
    await _dio.patch('/utilisateurs/$id/statut', queryParameters: {'statut': statut});
  }
}

final referentielRepositoryProvider = Provider<ReferentielRepository>((ref) {
  return ReferentielRepository(ref.watch(dioProvider));
});
