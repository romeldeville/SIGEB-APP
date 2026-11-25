import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/network/dio_client.dart';
import '../domain/statistique_globale.dart';

class StatistiqueRepository {
  final Dio _dio;
  StatistiqueRepository(this._dio);

  Future<StatistiqueGlobale> fetchDashboard() async {
    final resp = await _dio.get('/statistiques/tableau-de-bord');
    return StatistiqueGlobale.fromJson(resp.data as Map<String, dynamic>);
  }
}

final statistiqueRepositoryProvider = Provider<StatistiqueRepository>((ref) {
  return StatistiqueRepository(ref.watch(dioProvider));
});

final dashboardStatsProvider = FutureProvider.autoDispose<StatistiqueGlobale>((ref) {
  return ref.watch(statistiqueRepositoryProvider).fetchDashboard();
});
