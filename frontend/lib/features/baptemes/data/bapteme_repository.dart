import 'dart:typed_data';

import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/network/dio_client.dart';
import '../domain/acte_bapteme.dart';

class BaptemeRepository {
  final Dio _dio;
  BaptemeRepository(this._dio);

  Future<List<ActeBapteme>> lister({String? statut}) async {
    final resp = await _dio.get('/actes', queryParameters: {
      if (statut != null && statut.isNotEmpty) 'statut': statut,
    });
    return (resp.data as List).map((e) => ActeBapteme.fromJson(e as Map<String, dynamic>)).toList();
  }

  Future<ActeBapteme> consulter(String id) async {
    final resp = await _dio.get('/actes/$id');
    return ActeBapteme.fromJson(resp.data as Map<String, dynamic>);
  }

  Future<ActeBapteme> creer(Map<String, dynamic> payload) async {
    final resp = await _dio.post('/actes', data: payload);
    return ActeBapteme.fromJson(resp.data as Map<String, dynamic>);
  }

  Future<ActeBapteme> sceller(String id, String pin) async {
    final resp = await _dio.post('/actes/$id/sceller', data: {'pinSignature': pin});
    return ActeBapteme.fromJson(resp.data as Map<String, dynamic>);
  }

  Future<List<CarteEmise>> listerCartes(String acteId) async {
    final resp = await _dio.get('/actes/$acteId/cartes');
    return (resp.data as List).map((e) => CarteEmise.fromJson(e as Map<String, dynamic>)).toList();
  }

  Future<CarteEmise> emettreCarte({
    required String acteId,
    required String formatDocument,
    required String langue,
    required String motifEmission,
    String? motifDetail,
  }) async {
    final resp = await _dio.post('/actes/$acteId/cartes', data: {
      'formatDocument': formatDocument,
      'langue': langue,
      'motifEmission': motifEmission,
      if (motifDetail != null && motifDetail.isNotEmpty) 'motifDetail': motifDetail,
    });
    return CarteEmise.fromJson(resp.data as Map<String, dynamic>);
  }

  Future<Uint8List> telechargerPdf(String carteId) async {
    final resp = await _dio.get<List<int>>(
      '/cartes/$carteId/pdf',
      options: Options(responseType: ResponseType.bytes),
    );
    return Uint8List.fromList(resp.data!);
  }
}

final baptemeRepositoryProvider = Provider<BaptemeRepository>((ref) {
  return BaptemeRepository(ref.watch(dioProvider));
});
