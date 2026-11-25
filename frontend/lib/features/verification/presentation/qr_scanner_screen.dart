import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:mobile_scanner/mobile_scanner.dart';

import '../../../app/l10n/app_strings.dart';
import '../../../app/theme/app_theme.dart';
import '../../../core/network/dio_client.dart';
import '../../../core/widgets/adaptive_scaffold.dart';
import '../../auth/presentation/auth_controller.dart';
import '../data/verification_repository.dart';
import '../domain/qr_verify_result.dart';

class QrScannerScreen extends ConsumerStatefulWidget {
  const QrScannerScreen({super.key});

  @override
  ConsumerState<QrScannerScreen> createState() => _QrScannerScreenState();
}

class _QrScannerScreenState extends ConsumerState<QrScannerScreen> {
  final _manualCtrl = TextEditingController();
  bool _scanning = false;
  bool _cameraActive = false;
  QrVerifyResult? _result;
  String? _error;

  Future<void> _verify(String payload) async {
    if (_scanning || payload.trim().isEmpty) return;
    setState(() {
      _scanning = true;
      _error = null;
      _cameraActive = false;
    });
    try {
      final res = await ref.read(verificationRepositoryProvider).verifierQr(payload);
      setState(() {
        _result = res;
        _scanning = false;
      });
    } catch (e) {
      setState(() {
        _error = extractApiError(e);
        _scanning = false;
      });
    }
  }

  Color _resultColor(String r) => switch (r) {
        'AUTHENTIQUE_A_JOUR' => AppTheme.success,
        'AUTHENTIQUE_MISE_A_JOUR_DISPONIBLE' => AppTheme.warning,
        _ => AppTheme.danger,
      };

  @override
  Widget build(BuildContext context) {
    final strings = ref.watch(stringsProvider);
    final isAuthenticated = ref.watch(authControllerProvider).isAuthenticated;

    final content = ListView(
      padding: const EdgeInsets.all(20),
      children: [
        if (_cameraActive)
          SizedBox(
            height: 280,
            child: ClipRRect(
              borderRadius: BorderRadius.circular(12),
              child: MobileScanner(
                onDetect: (capture) {
                  final raw = capture.barcodes.firstOrNull?.rawValue;
                  if (raw != null) _verify(raw);
                },
              ),
            ),
          )
        else
          ElevatedButton.icon(
            onPressed: () => setState(() => _cameraActive = true),
            icon: const Icon(Icons.qr_code_scanner),
            label: const Text('Activer la caméra QR Code'),
          ),
        const SizedBox(height: 16),
        Row(
          children: [
            Expanded(
              child: TextField(
                controller: _manualCtrl,
                decoration: const InputDecoration(labelText: 'Ou coller un QR SIGEB:v2:...'),
              ),
            ),
            const SizedBox(width: 8),
            ElevatedButton(
              onPressed: _scanning ? null : () => _verify(_manualCtrl.text),
              child: const Text('Vérifier'),
            ),
          ],
        ),
        if (_scanning) const Padding(padding: EdgeInsets.all(20), child: Center(child: CircularProgressIndicator())),
        if (_error != null)
          Padding(
            padding: const EdgeInsets.only(top: 16),
            child: Text(_error!, style: const TextStyle(color: AppTheme.danger)),
          ),
        if (_result != null) ...[
          const SizedBox(height: 20),
          Card(
            shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(12),
              side: BorderSide(color: _resultColor(_result!.resultat), width: 2),
            ),
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      Icon(
                        _result!.valide ? Icons.verified : Icons.gpp_bad,
                        color: _resultColor(_result!.resultat),
                        size: 32,
                      ),
                      const SizedBox(width: 10),
                      Expanded(
                        child: Text(
                          _result!.resultat,
                          style: TextStyle(
                            fontSize: 16,
                            fontWeight: FontWeight.bold,
                            color: _resultColor(_result!.resultat),
                          ),
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 8),
                  Text(_result!.message),
                  if (_result!.nomAffiche != null) Text('Titulaire : ${_result!.nomAffiche}'),
                  if (_result!.codeCarte != null) Text('Code carte : ${_result!.codeCarte}'),
                  if (_result!.iun != null) Text('IUN : ${_result!.iun}'),
                  if (_result!.paroisseOrigine != null) Text('Paroisse : ${_result!.paroisseOrigine}'),
                  if (_result!.etatLibreMatrimonial != null)
                    Text(
                      'État libre matrimonial : ${_result!.etatLibreMatrimonial! ? "LIBRE" : "NON LIBRE (Mariage / Ordination / Vœux)"}',
                      style: TextStyle(
                        fontWeight: FontWeight.bold,
                        color: _result!.etatLibreMatrimonial! ? AppTheme.success : AppTheme.danger,
                      ),
                    ),
                  if (_result!.remplaceeParCodeCarte != null)
                    Text('Remplacée par duplicata : ${_result!.remplaceeParCodeCarte}',
                        style: const TextStyle(color: AppTheme.danger, fontWeight: FontWeight.bold)),
                  if (_result!.mentionsInscrites.isNotEmpty) ...[
                    const Divider(),
                    const Text('Mentions marginales inscrites :', style: TextStyle(fontWeight: FontWeight.bold)),
                    ..._result!.mentionsInscrites.map((m) => Text('• $m')),
                  ],
                ],
              ),
            ),
          ),
        ],
      ],
    );

    if (!isAuthenticated) {
      return Scaffold(
        appBar: AppBar(
          title: Text(strings.scanPublicQr),
          leading: IconButton(icon: const Icon(Icons.arrow_back), onPressed: () => context.go('/login')),
        ),
        body: content,
      );
    }

    return AdaptiveScaffold(currentRoute: '/verification/qr', title: strings.navScan, body: content);
  }
}
