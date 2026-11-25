import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:sigeb_app/main.dart';

void main() {
  testWidgets('Écran de connexion SIGEB affiché et bilingue FR/EN', (tester) async {
    await tester.pumpWidget(const ProviderScope(child: SigebApp()));
    await tester.pumpAndSettle();

    expect(find.text('SIGEB Cameroun'), findsOneWidget);
    expect(find.text('Connexion'), findsOneWidget);
    expect(find.text('Vérifier une carte de baptême (QR)'), findsOneWidget);

    // Bascule en anglais
    await tester.tap(find.text('EN'));
    await tester.pumpAndSettle();
    expect(find.text('Sign In'), findsOneWidget);
  });
}
