import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../app/l10n/app_strings.dart';
import '../../../app/theme/app_theme.dart';
import '../../../core/network/dio_client.dart';
import '../../../core/widgets/adaptive_scaffold.dart';
import '../../auth/presentation/auth_controller.dart';
import '../data/audit_repository.dart';
import '../domain/audit_entry.dart';

class AuditScreen extends ConsumerStatefulWidget {
  const AuditScreen({super.key});

  @override
  ConsumerState<AuditScreen> createState() => _AuditScreenState();
}

class _AuditScreenState extends ConsumerState<AuditScreen> {
  List<AuditEntry> _logs = [];
  bool _loading = true;
  String? _error;
  String? _integriteMsg;
  bool _integriteOk = true;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    try {
      final list = await ref.read(auditRepositoryProvider).consulter();
      setState(() {
        _logs = list;
        _loading = false;
      });
    } catch (e) {
      setState(() {
        _error = extractApiError(e);
        _loading = false;
      });
    }
  }

  Future<void> _verifierIntegrite() async {
    try {
      final res = await ref.read(auditRepositoryProvider).verifierIntegrite();
      setState(() {
        _integriteOk = res.chaineValide;
        _integriteMsg = res.chaineValide
            ? 'Chaîne SHA-256 intacte (${res.nombreEntreesVerifiees} entrées vérifiées)'
            : 'ALERTE : Rupture détectée à l\'ID ${res.premiereRuptureId}';
      });
    } catch (e) {
      if (mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(extractApiError(e))));
    }
  }

  @override
  Widget build(BuildContext context) {
    final strings = ref.watch(stringsProvider);
    final isAdmin = ref.watch(authControllerProvider).session?.isAdminNational == true;

    return AdaptiveScaffold(
      currentRoute: '/audit',
      title: strings.navAudit,
      body: Column(
        children: [
          if (isAdmin)
            Padding(
              padding: const EdgeInsets.all(12),
              child: Wrap(
                spacing: 12,
                runSpacing: 8,
                crossAxisAlignment: WrapCrossAlignment.center,
                children: [
                  ElevatedButton.icon(
                    onPressed: _verifierIntegrite,
                    icon: const Icon(Icons.verified_user),
                    label: Text(strings.verifyIntegrity),
                  ),
                  if (_integriteMsg != null)
                    Text(
                      _integriteMsg!,
                      style: TextStyle(
                        fontWeight: FontWeight.bold,
                        color: _integriteOk ? AppTheme.success : AppTheme.danger,
                      ),
                    ),
                ],
              ),
            ),
          Expanded(
            child: _loading
                ? const Center(child: CircularProgressIndicator())
                : _error != null
                    ? Center(child: Text(_error!))
                    : ListView.builder(
                        padding: const EdgeInsets.all(12),
                        itemCount: _logs.length,
                        itemBuilder: (_, i) {
                          final l = _logs[i];
                          return Card(
                            child: ListTile(
                              dense: true,
                              title: Text('${l.typeAction} · ${l.roleUtilisateur}'),
                              subtitle: Text('${l.iunConcerne ?? "-"} · ${l.motif ?? ""} · ${l.horodatage}'),
                            ),
                          );
                        },
                      ),
          ),
        ],
      ),
    );
  }
}
