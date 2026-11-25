import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/network/dio_client.dart';
import '../domain/mention_item.dart';

class MentionRepository {
  final Dio _dio;
  MentionRepository(this._dio);

  Future<List<MentionItem>> boiteReception() async {
    final resp = await _dio.get('/mentions/boite-reception');
    return (resp.data as List).map((e) => MentionItem.fromJson(e as Map<String, dynamic>)).toList();
  }

  Future<MentionItem> declarer(Map<String, dynamic> payload) async {
    final resp = await _dio.post('/mentions', data: payload);
    return MentionItem.fromJson(resp.data as Map<String, dynamic>);
  }

  Future<void> valider(String id, String pin) async {
    await _dio.post('/mentions/$id/valider', data: {'pinSignature': pin});
  }

  Future<void> rejeter(String id, String motif) async {
    await _dio.post('/mentions/$id/rejeter', data: {'motifRejet': motif});
  }
}

final mentionRepositoryProvider = Provider<MentionRepository>((ref) {
  return MentionRepository(ref.watch(dioProvider));
});
