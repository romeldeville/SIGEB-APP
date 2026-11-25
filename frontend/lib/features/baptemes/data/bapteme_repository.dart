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

  Future<CarteEmise> revoquerCarte(String carteId, String motif) async {
    final resp = await _dio.post(
      '/cartes/$carteId/revoquer',
      queryParameters: {'motif': motif},
    );
    return CarteEmise.fromJson(resp.data as Map<String, dynamic>);
  }

  Future<List<AvenantRectification>> listerAvenants(String acteId) async {
    final resp = await _dio.get('/actes/$acteId/avenants');
    return (resp.data as List)
        .map((e) => AvenantRectification.fromJson(e as Map<String, dynamic>))
        .toList();
  }

  Future<AvenantRectification> creerAvenant(String acteId, Map<String, dynamic> payload) async {
    final resp = await _dio.post('/actes/$acteId/avenants', data: payload);
    return AvenantRectification.fromJson(resp.data as Map<String, dynamic>);
  }

  Future<AvenantRectification> approuverAvenant(String avenantId, String pin) async {
    final resp = await _dio.post('/avenants/$avenantId/approuver', data: {'pinSignature': pin});
    return AvenantRectification.fromJson(resp.data as Map<String, dynamic>);
  }

  Future<AvenantRectification> rejeterAvenant(String avenantId) async {
    final resp = await _dio.post('/avenants/$avenantId/rejeter');
    return AvenantRectification.fromJson(resp.data as Map<String, dynamic>);
  }
}

final baptemeRepositoryProvider = Provider<BaptemeRepository>((ref) {
  return BaptemeRepository(ref.watch(dioProvider));
});
