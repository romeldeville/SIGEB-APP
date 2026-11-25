import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:sigeb_app/main.dart';

void main() {
  for (final entry in <String, Size>{
    'Mobile (360x740)': const Size(360, 740),
    'Tablette (768x1024)': const Size(768, 1024),
    'Desktop (1440x900)': const Size(1440, 900),
  }.entries) {
    testWidgets('Login SIGEB responsive sans overflow sur ${entry.key}', (tester) async {
      tester.view.physicalSize = entry.value;
      tester.view.devicePixelRatio = 1.0;
      addTearDown(tester.view.resetPhysicalSize);
      addTearDown(tester.view.resetDevicePixelRatio);

      await tester.pumpWidget(const ProviderScope(child: SigebApp()));
      await tester.pumpAndSettle();

      expect(find.text('SIGEB Cameroun'), findsWidgets);
      expect(find.text('Connexion'), findsOneWidget);
      expect(tester.takeException(), isNull);
    });
  }

  testWidgets('Bascule bilingue FR/EN sur écran de connexion', (tester) async {
    await tester.pumpWidget(const ProviderScope(child: SigebApp()));
    await tester.pumpAndSettle();

    await tester.tap(find.text('EN'));
    await tester.pumpAndSettle();
    expect(find.text('Sign In'), findsOneWidget);
  });
}
