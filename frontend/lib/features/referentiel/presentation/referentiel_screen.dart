import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../app/l10n/app_strings.dart';
import '../../../core/network/dio_client.dart';
import '../../../core/widgets/adaptive_scaffold.dart';

class ReferentielScreen extends ConsumerStatefulWidget {
  const ReferentielScreen({super.key});

  @override
  ConsumerState<ReferentielScreen> createState() => _ReferentielScreenState();
}

class _ReferentielScreenState extends ConsumerState<ReferentielScreen> {
  List<dynamic> _provinces = [];
  List<dynamic> _dioceses = [];
  bool _loading = true;
  String? _error;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    try {
      final dio = ref.read(dioProvider);
      final pResp = await dio.get('/referentiel/provinces');
      final dResp = await dio.get('/referentiel/dioceses');
      setState(() {
        _provinces = pResp.data as List;
        _dioceses = dResp.data as List;
        _loading = false;
      });
    } catch (e) {
      setState(() {
        _error = extractApiError(e);
        _loading = false;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    final strings = ref.watch(stringsProvider);
    return AdaptiveScaffold(
      currentRoute: '/referentiel',
      title: strings.navReferentiel,
      body: _loading
          ? const Center(child: CircularProgressIndicator())
          : _error != null
              ? Center(child: Text(_error!))
              : ListView(
                  padding: const EdgeInsets.all(16),
                  children: [
                    Text('Provinces Ecclésiastiques (${_provinces.length})',
                        style: const TextStyle(fontSize: 17, fontWeight: FontWeight.bold)),
                    const SizedBox(height: 8),
                    ..._provinces.map((p) => Card(
                          child: ListTile(
                            leading: CircleAvatar(child: Text('${p['code']}')),
                            title: Text('${p['nomFr']} / ${p['nomEn']}'),
                          ),
                        )),
                    const SizedBox(height: 16),
                    Text('Diocèses & Archidiocèses (${_dioceses.length})',
                        style: const TextStyle(fontSize: 17, fontWeight: FontWeight.bold)),
                    const SizedBox(height: 8),
                    ..._dioceses.map((d) => Card(
                          child: ListTile(
                            title: Text('${d['nomFr']} (${d['code']})'),
                            subtitle: Text('${d['typeDiocese']} · Siège : ${d['siege']}'),
                          ),
                        )),
                  ],
                ),
    );
  }
}
